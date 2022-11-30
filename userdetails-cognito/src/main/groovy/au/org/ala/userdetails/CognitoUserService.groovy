package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.RoleRecord
import au.org.ala.users.UserPropertyRecord
import au.org.ala.users.UserRecord
import au.org.ala.ws.tokens.TokenService
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest
import com.amazonaws.services.cognitoidp.model.AttributeType
import com.amazonaws.services.cognitoidp.model.GetUserRequest
import com.amazonaws.services.cognitoidp.model.GetUserResult
import com.amazonaws.services.cognitoidp.model.ListGroupsRequest
import com.amazonaws.services.cognitoidp.model.ListGroupsResult
import com.amazonaws.services.cognitoidp.model.ListUsersRequest
import com.amazonaws.services.cognitoidp.model.ListUsersResult
import com.amazonaws.services.cognitoidp.model.UpdateUserAttributesRequest
import com.amazonaws.services.cognitoidp.model.UpdateUserAttributesResult
import com.amazonaws.services.cognitoidp.model.UserType
import com.nimbusds.oauth2.sdk.token.AccessToken
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.NotImplementedException
import org.springframework.beans.factory.annotation.Value

import java.util.stream.Stream

@Slf4j
class CognitoUserService implements IUserService {

    static mainAttrs = ['given_name', 'family_name', 'email', 'username', 'roles'] as Set

    static customAttrs = [ 'organisation', 'city', 'state', 'country' ] as Set

    EmailService emailService
    TokenService tokenService

    AWSCognitoIdentityProvider cognitoIdp
    String poolId

    @Value('${attributes.affiliations.enabled:false}')
    boolean affiliationsEnabled = false

    @Override
    boolean updateUser(GrailsParameterMap params) {

        AccessToken accessToken = tokenService.getAuthToken(true)


//        def emailRecipients = [ user.email ]
//        if (params.email != user.email) {
//            emailRecipients << params.email
//        }

        try {
//            user.setProperties(params)
//            user.activated = true
//            user.locked = false

//            Collection<AttributeType> userAttributes = user.userProperties.collect { new AttributeType().withName(it.name).withValue(it.value) }
            Collection<AttributeType> userAttributes = new ArrayList<>()

//            userAttributes.add(new AttributeType().withName('email').withValue(user.email))
////            userAttributes.add(new AttributeType().withName('userName').withValue(user.userName))
////        userAttributes.add(new AttributeType().withName('userid').withValue(record.id))
//            userAttributes.add(new AttributeType().withName('given_name').withValue(user.firstName))
//            userAttributes.add(new AttributeType().withName('family_name').withValue(user.lastName))

//            userAttributes.add(new AttributeType().withName('email').withValue(user.email))
//            userAttributes.add(new AttributeType().withName('email_verified').withValue('false'))

            userAttributes.add(new AttributeType().withName('email').withValue(params.email))
            userAttributes.add(new AttributeType().withName('email_verified').withValue('false'))
            userAttributes.add(new AttributeType().withName('given_name').withValue(params.firstName))
            userAttributes.add(new AttributeType().withName('family_name').withValue(params.lastName))

//            params.findAll {customAttrs.contains(it.key) }
//                .each {userAttributes.add(new AttributeType().withName("custom:${it.key}").withValue(it.value)) }

            UpdateUserAttributesResult result = cognitoIdp.updateUserAttributes(new UpdateUserAttributesRequest()
                    .withAccessToken(accessToken as String)
                    .withUserAttributes(userAttributes))

//            emailService.sendUpdateProfileSuccess(user, emailRecipients)
            return true

        } catch (Exception e) {
            log.error(e.getMessage(), e)
        }

        return false
    }

    @Override
    boolean updateUser(String userId, GrailsParameterMap params) {

        UserRecord user = getUserById(userId)

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

//            params.findAll {customAttrs.contains(it.key) }
//                    .each {userAttributes.add(new AttributeType().withName("custom:${it.key}").withValue(it.value)) }

            AdminUpdateUserAttributesRequest request =
                    new AdminUpdateUserAttributesRequest()
                            .withUserPoolId(poolId)
                            .withUsername(userId)
                            .withUserAttributes(userAttributes)

            cognitoIdp.adminUpdateUserAttributes(request)

            emailService.sendUpdateProfileSuccess(user, emailRecipients)
            return true

        } catch (Exception e) {
            log.error(e.getMessage(), e)
        }

        return false
    }

    private AdminUpdateUserAttributesRequest updateUserAttributesRequest(UserRecord record) {


        return request
    }

    @Override
    boolean disableUser(UserRecord user) {
        return false
    }

    @Override
    boolean isActive(String email) {
        return false
    }

    @Override
    boolean isLocked(String email) {
        return false
    }

    @Override
    boolean isEmailRegistered(String email) {
        return false
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
    void activateAccount(UserRecord user) {

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
                    userRoles: attributes['custom:roles']?.split(','), userProperties: userProperties)
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
        return null
    }

    @Override
    void updateProperties(UserRecord user, GrailsParameterMap params) {

    }

    @Override
    void deleteUser(UserRecord user) {

    }

    @Override
    void resetAndSendTemporaryPassword(UserRecord user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {

    }

    @Override
    void clearTempAuthKey(UserRecord user) {

    }

    @Override
    UserRecord getUserById(String userId) {

        if (userId == null) {
            // Problem. This might mean an expired cookie, or it might mean that this service is not in the authorised system list
            log.debug("Attempt to get current user returned null. This might indicating that this machine is not the authorised system list")
            return null
        }

        AdminGetUserResult userResponse = cognitoIdp.adminGetUser(new AdminGetUserRequest().withUsername(userId).withUserPoolId(poolId))

        Map<String, String> attributes = userResponse.userAttributes.collectEntries { [ (it.name): it.value ] }
        Collection<UserPropertyRecord> userProperties = userResponse.userAttributes
                .findAll {!mainAttrs.contains(it.name) }
                .collect {
                        new UserPropertyRecord(name: it.name, value: it.value)
                }


        UserRecord user = new UserRecord(
                userId: userResponse.username,
                dateCreated: userResponse.userCreateDate, lastUpdated: userResponse.userLastModifiedDate,
                activated: userResponse.userStatus == "CONFIRMED", locked: !userResponse.enabled,
                firstName: attributes['given_name'], lastName: attributes['family_name'],
                email: attributes['email'], userName: userResponse.username,
                userRoles: attributes['custom:roles']?.split(','), userProperties: userProperties
        )

        return user
    }

    @Override
    UserRecord getUserByEmail(String email) {
        return null
    }

    @Override
    UserRecord getCurrentUser() {

        AccessToken accessToken = tokenService.getAuthToken(true)

        GetUserResult userResponse = cognitoIdp.getUser(new GetUserRequest().withAccessToken(accessToken as String))

        Map<String, String> attributes = userResponse.userAttributes.collectEntries { [ (it.name): it.value ] }
        Collection<UserPropertyRecord> userProperties = userResponse.userAttributes
                .findAll {!mainAttrs.contains(it.name) }
                .collect {
                    new UserPropertyRecord(name: it.name, value: it.value)
                }


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

        return getUserById(authService.userId)
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
        return null
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
        throw new NotImplementedException()
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
    Map findUserRoles(String role, GrailsParameterMap grailsParameterMap) {
        throw new NotImplementedException()
    }

    @Override
    boolean deleteRole(String userId, String roleName) {
        throw new NotImplementedException()
    }
}
