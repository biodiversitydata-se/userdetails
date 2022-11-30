package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.Role
import au.org.ala.users.User
import au.org.ala.users.UserProperty
import au.org.ala.ws.tokens.TokenService
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest
import com.amazonaws.services.cognitoidp.model.AttributeType
import com.amazonaws.services.cognitoidp.model.DescribeUserPoolRequest
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
import grails.converters.JSON
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
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

        User user = getUserById(userId)

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

    private AdminUpdateUserAttributesRequest updateUserAttributesRequest(User record) {


        return request
    }

    @Override
    boolean disableUser(User user) {
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
    void activateAccount(User user) {

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
            Collection<UserProperty> userProperties = userType.attributes
                    .findAll {!mainAttrs.contains(it.name) }
                    .collect {
                        new UserProperty(name: it.name, value: it.value)
                    }

            new User(
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
    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody) {
        return null
    }

    @Override
    User registerUser(GrailsParameterMap params) throws Exception {
        return null
    }

    @Override
    void updateProperties(User user, GrailsParameterMap params) {

    }

    @Override
    void deleteUser(User user) {

    }

    @Override
    void resetAndSendTemporaryPassword(User user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {

    }

    @Override
    void clearTempAuthKey(User user) {

    }

    @Override
    User getUserById(String userId) {

        if (userId == null) {
            // Problem. This might mean an expired cookie, or it might mean that this service is not in the authorised system list
            log.debug("Attempt to get current user returned null. This might indicating that this machine is not the authorised system list")
            return null
        }

        AdminGetUserResult userResponse = cognitoIdp.adminGetUser(new AdminGetUserRequest().withUsername(userId).withUserPoolId(poolId))

        Map<String, String> attributes = userResponse.userAttributes.collectEntries { [ (it.name): it.value ] }
        Collection<UserProperty> userProperties = userResponse.userAttributes
                .findAll {!mainAttrs.contains(it.name) }
                .collect {
                        new UserProperty(name: it.name, value: it.value)
                }


        User user = new User(
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
    User getUserByEmail(String email) {
        return null
    }

    @Override
    User getCurrentUser() {

        AccessToken accessToken = tokenService.getAuthToken(true)

        GetUserResult userResponse = cognitoIdp.getUser(new GetUserRequest().withAccessToken(accessToken as String))

        Map<String, String> attributes = userResponse.userAttributes.collectEntries { [ (it.name): it.value ] }
        Collection<UserProperty> userProperties = userResponse.userAttributes
                .findAll {!mainAttrs.contains(it.name) }
                .collect {
                    new UserProperty(name: it.name, value: it.value)
                }


        User user = new User(
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
    String getResetPasswordUrl(User user) {
        return null
    }

    @Override
    Collection<User> findUsersForExport(List usersInRoles, Object includeInactive) {
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
    Collection<Role> listRoles(String paginationToken, int maxResults) {

        ListGroupsResult result = cognitoIdp.listGroups(new ListGroupsRequest()
                .withUserPoolId(poolId)
                .withNextToken(paginationToken))

        result.groups.stream().map { groupType ->
            new Role(role: groupType.groupName, description: groupType.description)
        }
        .toList()
    }

    @Override
    Role createRole(GrailsParameterMap params) {

    }

    @Override
    boolean addUserRole(User user, Role role) {

        user

        return false
    }

    @Override
    boolean removeUserRole(User user, Role role) {
        return false
    }
}
