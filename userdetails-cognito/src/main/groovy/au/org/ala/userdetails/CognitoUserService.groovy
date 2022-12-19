package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.RoleRecord
import au.org.ala.users.UserPropertyRecord
import au.org.ala.users.UserRecord
import au.org.ala.users.UserRoleRecord
import au.org.ala.ws.tokens.TokenService
import com.amazonaws.AmazonWebServiceResult
import com.amazonaws.ResponseMetadata
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AdminAddUserToGroupRequest
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest
import com.amazonaws.services.cognitoidp.model.AdminDisableUserRequest
import com.amazonaws.services.cognitoidp.model.AdminEnableUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult
import com.amazonaws.services.cognitoidp.model.AdminListGroupsForUserRequest
import com.amazonaws.services.cognitoidp.model.AdminRemoveUserFromGroupRequest
import com.amazonaws.services.cognitoidp.model.AdminRemoveUserFromGroupResult
import com.amazonaws.services.cognitoidp.model.AdminResetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminSetUserMFAPreferenceRequest
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest
import com.amazonaws.services.cognitoidp.model.AssociateSoftwareTokenRequest
import com.amazonaws.services.cognitoidp.model.AttributeType
import com.amazonaws.services.cognitoidp.model.ConfirmForgotPasswordRequest
import com.amazonaws.services.cognitoidp.model.CreateGroupRequest
import com.amazonaws.services.cognitoidp.model.GetGroupRequest
import com.amazonaws.services.cognitoidp.model.GetUserRequest
import com.amazonaws.services.cognitoidp.model.GetUserResult
import com.amazonaws.services.cognitoidp.model.GroupType
import com.amazonaws.services.cognitoidp.model.ListGroupsRequest
import com.amazonaws.services.cognitoidp.model.ListGroupsResult
import com.amazonaws.services.cognitoidp.model.ListUsersInGroupRequest
import com.amazonaws.services.cognitoidp.model.ListUsersRequest
import com.amazonaws.services.cognitoidp.model.ListUsersResult
import com.amazonaws.services.cognitoidp.model.SoftwareTokenMfaSettingsType
import com.amazonaws.services.cognitoidp.model.UserNotFoundException
import com.amazonaws.services.cognitoidp.model.UserType
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.amazonaws.services.cognitoidp.model.VerifySoftwareTokenRequest
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import org.apache.commons.lang3.NotImplementedException
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
    List<UserRecord> listUsers(String query, String paginationToken, int maxResults) {

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

        return users.map { userType ->
            cognitoUserTypeToUserRecord(userType)
        }.toList()
    }

    private UserRecord cognitoUserTypeToUserRecord(UserType userType, boolean findRoles = false) {
        def (Map<String, String> attributes, List<UserPropertyRecord> userProperties) =
            cognitoAttrsToUserPropertyRecords(userType.attributes, []) // TODO userType doesn't userMFASettingsList (yet?)

        def user = new UserRecord<String>(
                id: attributes['name'] ?: userType.username,
                userId: userType.username,
                dateCreated: userType.userCreateDate, lastUpdated: userType.userLastModifiedDate,
                activated: userType.userStatus == "CONFIRMED", locked: !userType.enabled,
                firstName: attributes['given_name'], lastName: attributes['family_name'],
                email: attributes['email'], userName: userType.username,
                userRoles: [],
                userProperties: userProperties)
        if (findRoles) {
            user.userRoles = rolesForUser(userType.username).collect { new UserRoleRecord(user: user, role: it) }
        }
        return user
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

        if (userId == null || userId == "") {
            // Problem. This might mean an expired cookie, or it might mean that this service is not in the authorised system list
            log.debug("Attempt to get current user returned null. This might indicating that this machine is not the authorised system list")
            return null
        }

        try {
            AdminGetUserResult userResponse = cognitoIdp.adminGetUser(
                    new AdminGetUserRequest()
                            .withUsername(userId)
                            .withUserPoolId(poolId)
            )

            def (Map<String, String> attributes, List<UserPropertyRecord> userProperties) =
                cognitoAttrsToUserPropertyRecords(userResponse.userAttributes, userResponse.userMFASettingList)

            UserRecord user = new UserRecord<String>(
                    id: attributes['name'] ?: userResponse.username,
                    userId: userResponse.username,
                    dateCreated: userResponse.userCreateDate, lastUpdated: userResponse.userLastModifiedDate,
                    activated: userResponse.userStatus == "CONFIRMED", locked: !userResponse.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: userResponse.username,
                    userRoles: [], //attributes['custom:roles']?.split(','),
                    userProperties: userProperties
            )

            user.userRoles = rolesForUser(userResponse.username).collect { new UserRoleRecord(role: it, user: user) }

            return user
        } catch (UserNotFoundException e) {
            return null
        }
    }

    private List cognitoAttrsToUserPropertyRecords(List<AttributeType> userAttributes, List<String> mfaSettings) {
        Map<String, String> attributes = userAttributes.collectEntries { [(it.name): it.value] }
        Collection<UserPropertyRecord> userProperties = userAttributes
                .findAll { !mainAttrs.contains(it.name) }
                .collect {
                    new UserPropertyRecord(name: it.name, value: it.value)
                }
        userProperties.add(new UserPropertyRecord(name: "enableMFA", value: mfaSettings?.size() > 0))
        return [attributes, userProperties]
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

            def (Map<String, String> attributes, List<UserPropertyRecord> userProperties) =
                cognitoAttrsToUserPropertyRecords(userResponse.userAttributes, userResponse.userMFASettingList)

            UserRecord user = new UserRecord<String>(
                    id: attributes['name'] ?: userResponse.username,
                    userId: userResponse.username,
//                dateCreated: userResponse.userCreateDate, lastUpdated: userResponse.userLastModifiedDate,
//                activated: userResponse.userStatus == "CONFIRMED", locked: !userResponse.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: userResponse.username,
                    userProperties: userProperties
            )
            user.userRoles = rolesForUser(userResponse.username).collect { new UserRoleRecord(user: user, role: it) }

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
        return null
    }

    @Override
    List<String[]> countByProfileAttribute(String s, Date date, Locale locale) {
        return null
    }

    @Override
    Collection<RoleRecord> listRoles() {
        ListGroupsResult result = cognitoIdp.listGroups(
            new ListGroupsRequest()
                .withUserPoolId(poolId)
        )

        return result.groups.collect { groupType ->
            new RoleRecord(role: groupType.groupName, description: groupType.description)
        }
    }

    @Override
    PagedResult<RoleRecord> listRoles(GrailsParameterMap params) {

        ListGroupsResult result = cognitoIdp.listGroups(new ListGroupsRequest()
                .withUserPoolId(poolId)
                .withNextToken(params.token))

        def roles = result.groups.collect { groupType ->
            new RoleRecord(role: groupType.groupName, description: groupType.description)
        }

        return new PagedResult<RoleRecord>(list: roles, count: null, nextPageToken: result.nextToken)
    }

    private List<RoleRecord> rolesForUser(String username) {
        def groupsResult = cognitoIdp.adminListGroupsForUser(
                new AdminListGroupsForUserRequest()
                        .withUsername(username)
                        .withUserPoolId(poolId)
        )

        return groupsResult.groups.collect { new RoleRecord(role: it.groupName, description: it.description) }
    }

//    @Override
//    RoleRecord createRole(GrailsParameterMap params) {
//
//    }

    @Override
    boolean addUserRole(String userId, String roleName) {

        if (checkGroupExists(roleName)) {
            def addUserToGroupResult = cognitoIdp.adminAddUserToGroup(
                new AdminAddUserToGroupRequest()
                    .withUsername(userId)
                    .withGroupName(roleName)
                    .withUserPoolId(poolId)
            )

            return isSuccessful(addUserToGroupResult)
        }

        return false
    }

    @Override
    boolean removeUserRole(String userId, String roleName) {
        if (checkGroupExists(roleName)) {
            def removeUserFromGroupResult = cognitoIdp.adminRemoveUserFromGroup(
                    new AdminRemoveUserFromGroupRequest()
                            .withUsername(userId)
                            .withGroupName(roleName)
                            .withUserPoolId(poolId)
            )

            return isSuccessful(removeUserFromGroupResult)
        }
        return false
    }

    private GroupType getCognitoGroup(String roleName) {
        def getGroupResult = cognitoIdp.getGroup(
                new GetGroupRequest()
                        .withGroupName(roleName)
                        .withUserPoolId(poolId)
        )
        return isSuccessful(getGroupResult) ? getGroupResult.group : null
    }

    private boolean checkGroupExists(String roleName) {
        def group = getCognitoGroup(roleName)
        return group?.groupName == roleName
    }

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
        roleRecords.each { addRole(it) }
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
        def group = getCognitoGroup(roleRecord.role)
        if (!checkGroupExists(roleRecord.role)) {
            def createGroupResult = cognitoIdp.createGroup(
                    new CreateGroupRequest()
                            .withGroupName(roleRecord.role)
                            .withDescription(roleRecord.description)
//                        .withRoleArn()
                            .withUserPoolId(poolId)
            )
            if (createGroupResult.group) {
                return roleRecord
            } else {
                throw new RuntimeException("Couldn't create group")
            }
        } else {
            throw new RuntimeException("${roleRecord.role} already exists!")
        }
    }

    @Override
    UserRecord findByUserNameOrEmail(String username) {
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
    PagedResult<UserRoleRecord> findUserRoles(String role, GrailsParameterMap params) {
        def max = Math.min(params.int('max', 100), 1000)
        if (role) {
            def group = getCognitoGroup(role)
            if (group) {
                def listUsersInGroupResult = cognitoIdp.listUsersInGroup(
                        new ListUsersInGroupRequest()
                                .withGroupName(role)
                                .withLimit(max)
                                .withNextToken(params.token)
                                .withUserPoolId(poolId)
                )
                if (isSuccessful(listUsersInGroupResult)) {

                    def roleRecord = new RoleRecord(role: group.groupName, description: group.description)
                    def userRoleInstanceList = listUsersInGroupResult.users.collect {
                        new UserRoleRecord(user: cognitoUserTypeToUserRecord(it), role: roleRecord)
                    }

                    return new PagedResult<UserRoleRecord>(list: userRoleInstanceList, count: null, nextPageToken: listUsersInGroupResult.nextToken)
                }
            } else {
                log.warn("$role does not exist, can't find users for it")
                return new PagedResult<UserRoleRecord>(list: [], count: 0, nextPageToken: null)
            }
        } else {
            throw new NotImplementedException("You must supply a role for Cognito")
        }
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

    private boolean isSuccessful(AmazonWebServiceResult<? extends ResponseMetadata> result) {
        def code = result.sdkHttpMetadata.httpStatusCode
        return code >= 200 && code < 300
    }

}
