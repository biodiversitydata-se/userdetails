package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.RoleRecord
import au.org.ala.users.UserPropertyRecord
import au.org.ala.users.UserRecord
import au.org.ala.users.UserRoleRecord
import au.org.ala.ws.security.JwtProperties
import au.org.ala.ws.tokens.TokenService
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AddCustomAttributesRequest
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest
import com.amazonaws.services.cognitoidp.model.AdminDisableUserRequest
import com.amazonaws.services.cognitoidp.model.AdminEnableUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult
import com.amazonaws.services.cognitoidp.model.AdminResetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminSetUserMFAPreferenceRequest
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest
import com.amazonaws.services.cognitoidp.model.AssociateSoftwareTokenRequest
import com.amazonaws.services.cognitoidp.model.AttributeType
import com.amazonaws.services.cognitoidp.model.ConfirmForgotPasswordRequest
import com.amazonaws.services.cognitoidp.model.DescribeUserPoolRequest
import com.amazonaws.services.cognitoidp.model.GetUserRequest
import com.amazonaws.services.cognitoidp.model.GetUserResult
import com.amazonaws.services.cognitoidp.model.ListGroupsRequest
import com.amazonaws.services.cognitoidp.model.ListGroupsResult
import com.amazonaws.services.cognitoidp.model.ListUsersInGroupRequest
import com.amazonaws.services.cognitoidp.model.ListUsersRequest
import com.amazonaws.services.cognitoidp.model.ListUsersResult
import com.amazonaws.services.cognitoidp.model.SchemaAttributeType
import com.amazonaws.services.cognitoidp.model.UpdateUserAttributesRequest
import com.amazonaws.services.cognitoidp.model.UpdateUserAttributesResult
import com.amazonaws.services.cognitoidp.model.SoftwareTokenMfaSettingsType
import com.amazonaws.services.cognitoidp.model.UserNotFoundException
import com.amazonaws.services.cognitoidp.model.UserType
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.amazonaws.services.cognitoidp.model.VerifySoftwareTokenRequest
import grails.converters.JSON
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import org.apache.commons.lang3.NotImplementedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import java.util.stream.Stream

@Slf4j
class CognitoUserService implements IUserService {

    static mainAttrs = ['given_name', 'family_name', 'email', 'username', 'roles'] as Set

    static customAttrs = [ 'organisation', 'city', 'state', 'country' ] as Set

    EmailService emailService
    TokenService tokenService
    def grailsApplication

    AWSCognitoIdentityProvider cognitoIdp
    String poolId
    @Autowired
    JwtProperties jwtProperties

    @Value('${attributes.affiliations.enabled:false}')
    boolean affiliationsEnabled = false
    public static final String TEMP_AUTH_KEY = 'tempAuthKey'

    @Override
    boolean updateUser(String userId, GrailsParameterMap params) {

        UserRecord user = getUserById(userId)
        def isUserLocked = user.locked

        def emailRecipients = [ user.email ]
        if (params.email != user.email) {
            emailRecipients << params.email
        }

        try {
            user.setProperties(params)
//            user.activated = true
//            user.locked = false

//            Collection<AttributeType> userAttributes = user.userProperties.collect { new AttributeType().withName(it.name).withValue(it.value) }
            Collection<AttributeType> userAttributes = new ArrayList<>()

            userAttributes.add(new AttributeType().withName('email').withValue(user.email))
//            userAttributes.add(new AttributeType().withName('userName').withValue(user.userName))
//        userAttributes.add(new AttributeType().withName('userid').withValue(record.id))
            userAttributes.add(new AttributeType().withName('given_name').withValue(user.firstName))
            userAttributes.add(new AttributeType().withName('family_name').withValue(user.lastName))

//            userAttributes.add(new AttributeType().withName('email').withValue(user.email))
//            userAttributes.add(new AttributeType().withName('email_verified').withValue('false'))

            params.findAll {customAttrs.contains(it.key) }
                    .each {userAttributes.add(new AttributeType().withName("custom:${it.key}").withValue(it.value)) }

            AdminUpdateUserAttributesRequest request =
                    new AdminUpdateUserAttributesRequest()
                            .withUserPoolId(poolId)
                            .withUsername(userId)
                            .withUserAttributes(userAttributes)

            cognitoIdp.adminUpdateUserAttributes(request)

            //disable user
            if(params.locked && !isUserLocked){
                disableUser(user)
            }
            else if(!params.locked && isUserLocked) {
                activateAccount(user, params)
            }

            emailService.sendUpdateProfileSuccess(user, emailRecipients)
            return true

        } catch (Exception e) {
            log.error(e.getMessage(), e)
        }

        return false
    }

    @Override
    boolean disableUser(UserRecord user) {
        def response = cognitoIdp.adminDisableUser(new AdminDisableUserRequest().withUsername(user.email).withUserPoolId(poolId))
        return response.sdkHttpMetadata.httpStatusCode == 200
    }

    @Override
    boolean isActive(String email) {
        def user = getUserByEmail(email)
        return user?.getActivated()
    }

    @Override
    boolean isLocked(String email) {
        def user = getUserByEmail(email)
        return user?.getLocked()
    }

    @Override
    boolean isEmailRegistered(String email) {
        return isEmailInUse(email)
    }

    @Override
    boolean isEmailInUse(String email) {

        ListUsersRequest request = new ListUsersRequest()
            .withUserPoolId(poolId)
            .withFilter("email=\"${email}\"")

        ListUsersResult response = cognitoIdp.listUsers(request)
        return response.users
    }

    @Override
    boolean activateAccount(UserRecord user, GrailsParameterMap params) {
        def request = new AdminEnableUserRequest().withUsername(user.email).withUserPoolId(poolId)
        def response = cognitoIdp.adminEnableUser(request)
        //TODO update custom activated field
        return response.getSdkHttpMetadata().httpStatusCode == 200
    }

    @Override
    def listUsers(String query, String paginationToken, int maxResults) {

        ListUsersRequest request = new ListUsersRequest()
                .withUserPoolId(poolId)
                .withPaginationToken(paginationToken)
                .withLimit(maxResults)

        Stream<UserType> users

        if (query) {

            request.withFilter("email ^= \"${query}\"")

            ListUsersResult emailResults = cognitoIdp.listUsers(request)

            request.withFilter("given_name ^= \"${query}\"")

            ListUsersResult givenNameResults = cognitoIdp.listUsers(request)

            request.withFilter("family_name ^= \"${query}\"")

            ListUsersResult familyNameResults = cognitoIdp.listUsers(request)

            users = Stream.concat(
                    emailResults.users.stream(),
                    Stream.concat(givenNameResults.users.stream(), familyNameResults.users.stream()))
                    .distinct()

        } else {

            ListUsersResult results = cognitoIdp.listUsers(request)

            users = results.users.stream()
        }

        users.map { userType ->

            Map<String, String> attributes = userType.attributes.collectEntries { [ (it.name): it.value ] }
            Collection<UserPropertyRecord> userProperties = userType.attributes
                    .findAll {!mainAttrs.contains(it.name) }
                    .collect {
                        new UserPropertyRecord(name: it.name, value: it.value)
                    }

            new UserRecord(
                    userId: userType.username,
                    dateCreated: userType.userCreateDate, lastUpdated: userType.userLastModifiedDate,
                    activated: userType.userStatus == "CONFIRMED", locked: !userType.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: userType.username,
                    userRoles: attributes['custom:role']?.split(','), userProperties: userProperties)
        }
        .toList()
    }

    @Override
    Collection<UserRecord> listUsers() {
        throw new NotImplementedException()
    }

    @Override
    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody) {
        return null
    }

    @Override
    UserRecord registerUser(GrailsParameterMap params) throws Exception {
        def request = new AdminCreateUserRequest()
        //TODO need to change
        request.username = UUID.randomUUID().toString()
        request.userPoolId = poolId
        request.desiredDeliveryMediums = ["EMAIL"]

        Collection<AttributeType> userAttributes = new ArrayList<>()

        userAttributes.add(new AttributeType().withName('email').withValue(params.email))
        userAttributes.add(new AttributeType().withName('given_name').withValue(params.firstName))
        userAttributes.add(new AttributeType().withName('family_name').withValue(params.lastName))
        userAttributes.add(new AttributeType().withName('email_verified').withValue('true'))

        params.findAll {customAttrs.contains(it.key) }
                .each {userAttributes.add(new AttributeType().withName("custom:${it.key}").withValue(it.value as String)) }

        userAttributes.add(new AttributeType().withName('custom:activated').withValue("0"))
        userAttributes.add(new AttributeType().withName('custom:disabled').withValue("0"))
        userAttributes.add(new AttributeType().withName('custom:authority').withValue("ROLE_USER"))
        userAttributes.add(new AttributeType().withName('custom:role').withValue("ROLE_USER"))
        userAttributes.add(new AttributeType().withName('custom:expired').withValue("0"))

        request.userAttributes = userAttributes

        def userResponse = cognitoIdp.adminCreateUser(request)

        if(userResponse.user) {

            Map<String, String> attributes = userResponse.user.attributes.collectEntries { [(it.name): it.value] }
            Collection<UserPropertyRecord> userProperties = attributes
                    .findAll { !mainAttrs.contains(it.key) }
                    .collect {
                        if (it.key.startsWith('custom:')) {
                            new UserPropertyRecord(name: it.key.substring(7), value: it.value)
                        } else {
                            new UserPropertyRecord(name: it.key, value: it.value)
                        }
                    }

            UserRecord user = new UserRecord(
                    dateCreated: userResponse.user.userCreateDate,
                    lastUpdated: userResponse.user.userLastModifiedDate,
                    activated: userResponse.user.userStatus == "CONFIRMED", locked: !userResponse.user.enabled,
                    firstName: attributes.find { it.key == 'given_name' }.value,
                    lastName: attributes.find { it.key == 'family_name' }.value,
                    email: attributes.find { it.key == 'email' }.value,
                    userName: userResponse.user.username,
                    userId: userResponse.user.username,
                    userProperties: userProperties
            )

            Collection<UserRoleRecord> userRoles = attributes
                    .find {it.key == "custom:role" }.value.split(",")
                    .collect {
                        new UserRoleRecord(user: user, role: new RoleRecord(role: it, description: it))
                    }

            user.userRoles = userRoles

            //disable user
            disableUser(user)

            return user
        }
        return null
    }

    @Override
    void resetAndSendTemporaryPassword(UserRecord user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {
        def request = new AdminResetUserPasswordRequest()
        request.username = user.email
        request.userPoolId = poolId

        cognitoIdp.adminResetUserPassword(request)
    }

    @Override
    void clearTempAuthKey(UserRecord user) {
        def request = new AdminUpdateUserAttributesRequest()
                .withUsername(user.userName)
                .withUserPoolId(poolId)
                .withUserAttributes(new AttributeType().withName(TEMP_AUTH_KEY).withValue(null))
        cognitoIdp.adminUpdateUserAttributes(request)
    }

    @Override
    void updateProperties(UserRecord user, GrailsParameterMap params) {
        throw new NotImplementedException()
    }

    @Override
    void deleteUser(UserRecord user) {
        throw new NotImplementedException()
    }

    @Override
    UserRecord getUserById(String userId) {

        def userResponse
        def userAttributes

        if (userId == null || userId == "") {
            // Problem. This might mean an expired cookie, or it might mean that this service is not in the authorised system list
            log.debug("Attempt to get current user returned null. This might indicating that this machine is not the authorised system list")
            return null
        }

        try {
            if (userId.isLong()) {
                ListUsersRequest request = new ListUsersRequest()
                        .withUserPoolId(poolId)
                        .withFilter("name=\"${userId}\"")
                ListUsersResult response = cognitoIdp.listUsers(request)
                userResponse = response.users.first()
                userAttributes = userResponse.attributes
            }

            else {
                userResponse = cognitoIdp.adminGetUser(new AdminGetUserRequest().withUsername(userId).withUserPoolId(poolId))
                userAttributes = userResponse.userAttributes
            }

            Map<String, String> attributes = userAttributes.collectEntries { [ (it.name): it.value ] }
            Collection<UserPropertyRecord> userProperties = userAttributes
                    .findAll {!mainAttrs.contains(it.name) }
                    .collect {
                            new UserPropertyRecord(name: it.name, value: it.value)
                    }
                userProperties.add(new UserPropertyRecord(name: "enableMFA", value: userResponse.getUserMFASettingList()?.size() > 0))


            UserRecord user = new UserRecord(
                    userId: userResponse.username,
                    dateCreated: userResponse.userCreateDate, lastUpdated: userResponse.userLastModifiedDate,
                    activated: userResponse.userStatus == "CONFIRMED", locked: !userResponse.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: userResponse.username,
                    userRoles: attributes['custom:role']?.split(','), userProperties: userProperties
            )

            return user
        }
        catch (UserNotFoundException e) {
            return null
        }
    }

    @Override
    UserRecord getUserByEmail(String email) {
        return getUserById(email)
    }

    @Override
    UserRecord getCurrentUser() {

        try {
            AccessToken accessToken = tokenService.getAuthToken(true)

            if(accessToken == null){
                return null
            }
            GetUserResult userResponse = cognitoIdp.getUser(new GetUserRequest().withAccessToken(accessToken as String))


            Map<String, String> attributes = userResponse.userAttributes.collectEntries { [(it.name): it.value] }
            Collection<UserPropertyRecord> userProperties = userResponse.userAttributes
                    .findAll { !mainAttrs.contains(it.name) }
                    .collect {
                        new UserPropertyRecord(name: it.name, value: it.value)
                    }
            userProperties.add(new UserPropertyRecord(name: "enableMFA", value: userResponse.getUserMFASettingList()?.size() > 0))


            UserRecord user = new UserRecord(
                    userId: userResponse.username,
//                dateCreated: userResponse.userCreateDate, lastUpdated: userResponse.userLastModifiedDate,
//                activated: userResponse.userStatus == "CONFIRMED", locked: !userResponse.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: userResponse.username,
//                userRoles: attributes['custom:roles']?.split(','),
                    userProperties: userProperties
            )

            return user
        }
        catch (Exception e){
            log.error(e.getMessage())
            return null
        }
    }

    @Override
    String getResetPasswordUrl(UserRecord user) {
        return null
    }

    @Override
    Collection<UserRecord> findUsersForExport(List usersInRoles, Object includeInactive) {
        return null
    }

    @Override
    Map getUsersCounts(Locale locale) {
        Map jsonMap = [:]
        DescribeUserPoolRequest request = new DescribeUserPoolRequest().withUserPoolId(poolId)
        def response = cognitoIdp.describeUserPool(request)
        jsonMap.totalUsers = response.userPool.estimatedNumberOfUsers
        log.debug "jsonMap = ${jsonMap as JSON}"
        jsonMap
    }

    @Override
    List<String[]> countByProfileAttribute(String s, Date date, Locale locale) {
        return null
    }

    @Override
    Collection<RoleRecord> listRoles() {
        return []
    }

    @Override
    Collection<RoleRecord> listRoles(String paginationToken, int maxResults) {

        ListGroupsResult result = cognitoIdp.listGroups(new ListGroupsRequest()
                .withUserPoolId(poolId)
                .withNextToken(paginationToken))

        result.groups.stream().map { groupType ->
            new RoleRecord(role: groupType.groupName, description: groupType.description)
        }
        .toList()
    }

//    @Override
//    RoleRecord createRole(GrailsParameterMap params) {
//
//    }

    @Override
    boolean addUserRole(String userId, String roleName) {

        user

        return false
    }

//    @Override
//    boolean removeUserRole(UserRecord user, RoleRecord role) {
//        return false
//    }

    @Override
    void findScrollableUsersByUserName(String username, int maxResults, ResultStreamer resultStreamer) {
        throw new NotImplementedException()
    }

    @Override
    void findScrollableUsersByIdsAndRole(List<String> ids, String roleName, ResultStreamer resultStreamer) {
        throw new NotImplementedException()
    }

    @Override
    void addRoles(Collection<RoleRecord> roleRecords) {
//        throw new NotImplementedException()
        log.warn("CognitoUserService.addRoles() not implemented yet")
    }

    @Override
    List<UserPropertyRecord> findAllAttributesByName(String s) {
        throw new NotImplementedException()
    }

    @Override
    void addOrUpdateProperty(UserRecord userRecord, String name, String value) {
        throw new NotImplementedException()
    }

    @Override
    void removeUserAttributes(UserRecord userRecord, ArrayList<String> attributes) {
        throw new NotImplementedException()
    }

    @Override
    void getUserAttribute(UserRecord userRecord, String attribute) {
        throw new NotImplementedException()
    }

    @Override
    List getAllAvailableProperties() {
        throw new NotImplementedException()
    }

    @Override
    RoleRecord addRole(RoleRecord roleRecord) {
        throw new NotImplementedException()
    }

    @Override
    List<String[]> listNamesAndEmails() {
        throw new NotImplementedException()
    }

    @Override
    List<String[]> listIdsAndNames() {
        throw new NotImplementedException()
    }

    @Override
    List<String[]> listUserDetails() {
        throw new NotImplementedException()
    }

    @Override
    Map findUserRoles(String role, GrailsParameterMap grailsParameterMap) {
        throw new NotImplementedException()
    }

    @Override
    boolean deleteRole(String userId, String roleName) {
        throw new NotImplementedException()
    }

    @Override
    boolean resetPassword(UserRecord user, String newPassword, boolean isPermanent, String confirmationCode) {
        if(!user || !newPassword) {
            return false
        }

        try {
            if (confirmationCode == null) {
                def request = new AdminSetUserPasswordRequest()
                request.username = user.email
                request.userPoolId = poolId
                request.password = newPassword
                request.permanent = isPermanent

                def response = cognitoIdp.adminSetUserPassword(request)
                return response.getSdkHttpMetadata().httpStatusCode == 200
            } else {
                def request = new ConfirmForgotPasswordRequest().withUsername(user.email)
                request.password = newPassword
                request.confirmationCode = confirmationCode
                request.clientId = grailsApplication.config.getProperty('security.oidc.client-id')
                request.secretHash = calculateSecretHash(grailsApplication.config.getProperty('security.oidc.client-id'),
                        grailsApplication.config.getProperty('security.oidc.secret'), user.email)
                def response = cognitoIdp.confirmForgotPassword(request)
                return response.getSdkHttpMetadata().httpStatusCode == 200
            }
        }catch(Exception e){
            return false
        }
    }

    @Override
    String getPasswordResetView() {
        return "passwordResetCognito"
    }

    @Override
    def sendAccountActivation(UserRecord user) {
        //this email can be sent via cognito
        //emailService.sendCognitoAccountActivation(user)
    }

    @Override
    String getSecretForMfa() {
        AccessToken accessToken = tokenService.getAuthToken(true)

        if (accessToken == null) {
            throw new IllegalStateException("No current user available")
        }
        AssociateSoftwareTokenRequest request = new AssociateSoftwareTokenRequest()
        request.accessToken = accessToken.value
        def response = cognitoIdp.associateSoftwareToken(request)
        if (response.secretCode) {
            return response.secretCode
        } else {
            throw new RuntimeException()
        }
    }

    @Override
    boolean verifyUserCode(String userCode) {
        AccessToken accessToken = tokenService.getAuthToken(true)

        if (accessToken == null) {
            throw new IllegalStateException("No current user available")
        }
        VerifySoftwareTokenRequest request = new VerifySoftwareTokenRequest()
        request.accessToken = accessToken.value
        request.userCode = userCode
        def response= cognitoIdp.verifySoftwareToken(request)
        return response.status == "SUCCESS"
    }

    @Override
    void enableMfa(String userId, boolean enable) {
        AdminSetUserMFAPreferenceRequest mfaRequest = new AdminSetUserMFAPreferenceRequest().withUserPoolId(poolId)
                .withUsername(userId)
        mfaRequest.setSoftwareTokenMfaSettings(new SoftwareTokenMfaSettingsType(enabled: enable))
        def response = cognitoIdp.adminSetUserMFAPreference(mfaRequest)
        if (response.sdkHttpMetadata.httpStatusCode != 200) {
            throw new RuntimeException("Couldn't set MFA preference")
        }
    }

    static String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
        try {
            byte[] rawHmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, userPoolClientSecret).hmac("$userName$userPoolClientId")
            return Base64.getEncoder().encodeToString(rawHmac)
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating ")
        }
    }

    @Override
    UserRecord findByUserNameOrEmail(String userName) {
        return listUsers(userName, null, 1)?[0]
    }

    @Override
    def findUsersByRole(String roleName, List numberIds, List userIds, String pageOrToken) {
        ListUsersInGroupRequest request = new ListUsersInGroupRequest().withUserPoolId(poolId).withLimit(10)
        if(pageOrToken) {
            request.nextToken = pageOrToken
        }
        request.groupName = roleName.split(jwtProperties.getRolePrefix())[1].toLowerCase()
        //TODO: filter using userid and email
        //following CustomQueryParameter is not working
        //request.putCustomQueryParameter("name", "43954")
        def response = cognitoIdp.listUsersInGroup(request)
        def users = response.users.stream()

        def results = users.map { userType ->

            Map<String, String> attributes = userType.attributes.collectEntries { [ (it.name): it.value ] }
            Collection<UserPropertyRecord> userProperties = userType.attributes
                    .findAll {!mainAttrs.contains(it.name) }
                    .collect {
                        new UserPropertyRecord(name: it.name, value: it.value)
                    }

            new UserRecord(
                    userId: userType.username,
                    dateCreated: userType.userCreateDate, lastUpdated: userType.userLastModifiedDate,
                    activated: userType.userStatus == "CONFIRMED", locked: !userType.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: userType.username,
                    userRoles: attributes['custom:role']?.split(','), userProperties: userProperties)
        }.toList()

        return [results: results, nextToken: response.nextToken]
    }

    def getUserDetailsFromIdList(List idList){

        List<UserType> users = []

        ListUsersRequest request = new ListUsersRequest()
                .withUserPoolId(poolId)
                .withLimit(1)

        idList.forEach{
            if(it instanceof Number) {
                request.withFilter("name = \"${it.toString()}\"")
            }
            else{
                request.withFilter("username = \"${it.toString()}\"")
            }
            def response = cognitoIdp.listUsers(request)
            users.addAll(response.users)
        }

        return users.stream().map { userType ->

            Map<String, String> attributes = userType.attributes.collectEntries { [ (it.name): it.value ] }
            Collection<UserPropertyRecord> userProperties = userType.attributes
                    .findAll {!mainAttrs.contains(it.name) }
                    .collect {
                        new UserPropertyRecord(name: it.name, value: it.value)
                    }

            new UserRecord(
                    userId: attributes['name'] ?: userType.username,
                    dateCreated: userType.userCreateDate, lastUpdated: userType.userLastModifiedDate,
                    activated: userType.userStatus == "CONFIRMED", locked: !userType.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: userType.username,
                    userRoles: attributes['custom:role']?.split(','), userProperties: userProperties)
        }.toList()
    }

    def searchByUsernameOrEmail(String q, int max){
        return [results: listUsers(q, null, max)]
    }

    def saveCustomUserProperty(UserRecord user, String name, String value) {

        DescribeUserPoolRequest request = new DescribeUserPoolRequest().withUserPoolId(poolId)
        def response = cognitoIdp.describeUserPool(request)
        if (response.userPool.schemaAttributes.find{it.name =='custom:' + name} == null) {

            AddCustomAttributesRequest addAttrRequest = new AddCustomAttributesRequest().withUserPoolId(poolId)

            List<SchemaAttributeType> attList = new ArrayList<>()
            attList.add(new SchemaAttributeType().withAttributeDataType("String")
                    .withMutable(true).withName(name))

            addAttrRequest.customAttributes = attList
            def addAttResponse = cognitoIdp.addCustomAttributes(addAttrRequest)
            if (addAttResponse.sdkHttpMetadata.httpStatusCode == 200) {

                def updateUserResponse = addCustomUserProperty(user, name, value)

                if (updateUserResponse.sdkHttpMetadata.httpStatusCode == 200) {
                    return [property: name, value: value]
                } else {
                    return []
                }
            } else {
                return []
            }
        }
        else{
            def updateUserResponse = addCustomUserProperty(user, name, value)

            if (updateUserResponse.sdkHttpMetadata.httpStatusCode == 200) {
                return [property: name, value: value]
            } else {
                return []
            }
        }
    }

    def addCustomUserProperty(UserRecord user, String name, String value){
        Collection<AttributeType> userAttributes = new ArrayList<>()

        userAttributes.add(new AttributeType().withName('custom:' + name).withValue(value))

        AdminUpdateUserAttributesRequest updateUserRequest =
                new AdminUpdateUserAttributesRequest()
                        .withUserPoolId(poolId)
                        .withUsername(user.userName)
                        .withUserAttributes(userAttributes)

        return cognitoIdp.adminUpdateUserAttributes(updateUserRequest)
    }

    def getCustomUserProperty(UserRecord user, String name){
        return user.userProperties.findAll{it.name == 'custom:'+ name}.collect{ [property:"$name",value: it.value ] }
    }

}
