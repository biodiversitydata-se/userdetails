package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.User
import au.org.ala.users.UserProperty
import au.org.ala.users.UserRole
import au.org.ala.web.AuthService
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest
import com.amazonaws.services.cognitoidp.model.AdminDisableUserRequest
import com.amazonaws.services.cognitoidp.model.AdminEnableUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult
import com.amazonaws.services.cognitoidp.model.AdminResetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest
import com.amazonaws.services.cognitoidp.model.AttributeType
import com.amazonaws.services.cognitoidp.model.GetUserRequest
import com.amazonaws.services.cognitoidp.model.ListUsersRequest
import com.amazonaws.services.cognitoidp.model.ListUsersResult
import com.amazonaws.services.cognitoidp.model.UserNotFoundException
import com.amazonaws.services.cognitoidp.model.UserType
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value

import java.util.stream.Collectors
import java.util.stream.Stream

@Slf4j
class CognitoUserService implements IUserService {

    static mainAttrs = ['given_name', 'family_name', 'email', 'username', 'roles'] as Set

    static customAttrs = [ 'organisation', 'city', 'state', 'country' ] as Set

    EmailService emailService
    AuthService authService

    AWSCognitoIdentityProvider cognitoIdp
    String poolId

    @Value('${attributes.affiliations.enabled:false}')
    boolean affiliationsEnabled = false
    public static final String TEMP_AUTH_KEY = 'tempAuthKey'

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

//            userAttributes.add(new AttributeType().withName('email').withValue(user.email))
////            userAttributes.add(new AttributeType().withName('userName').withValue(user.userName))
////        userAttributes.add(new AttributeType().withName('userid').withValue(record.id))
//            userAttributes.add(new AttributeType().withName('given_name').withValue(user.firstName))
//            userAttributes.add(new AttributeType().withName('family_name').withValue(user.lastName))

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
    boolean activateAccount(User user, GrailsParameterMap params) {
        def request = new AdminEnableUserRequest().withUsername(user.email).withUserPoolId(poolId)
        def response = cognitoIdp.adminEnableUser(request)
        //TODO update custom activated field
        return response.getSdkHttpMetadata().httpStatusCode == 200
    }

    @Override
    def listUsers(String query, int offset, int maxResults) {

        ListUsersRequest request = new ListUsersRequest()
                .withUserPoolId(poolId)
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
//                    id: userType.username,
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
        def request = new AdminCreateUserRequest()
        request.username = params.email
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
            Collection<UserProperty> userProperties = attributes
                    .findAll { !mainAttrs.contains(it.key) }
                    .collect {
                        if (it.key.startsWith('custom:')) {
                            new UserProperty(name: it.key.substring(7), value: it.value)
                        } else {
                            new UserProperty(name: it.key, value: it.value)
                        }
                    }

            User user = new User(
                    dateCreated: userResponse.user.userCreateDate,
                    lastUpdated: userResponse.user.userLastModifiedDate,
                    activated: userProperties.find { it.name == 'activated' }.value == "1",
                    locked: userProperties.find { it.name == 'disabled' }.value == "1",
                    firstName: attributes.find { it.key == 'given_name' }.value,
                    lastName: attributes.find { it.key == 'family_name' }.value,
                    email: attributes.find { it.key == 'email' }.value,
                    userName: userResponse.user.username,
                    userProperties: userProperties
            )

            Collection<UserRole> userRoles = attributes
                    .find {it.key == "custom:role" }.value.split(",")
                    .collect {
                        new UserRole(user: user, role: new Role(role: it, description: it))
                    }

            user.userRoles = userRoles

            //disable user
            disableUser(user)

            return user
        }
        return null
    }

    @Override
    void resetAndSendTemporaryPassword(User user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {
        def request = new AdminResetUserPasswordRequest()
        request.username = user.email
        request.userPoolId = poolId

        cognitoIdp.adminResetUserPassword(request)
    }

    @Override
    void clearTempAuthKey(User user) {
        def request = new AdminUpdateUserAttributesRequest()
                .withUsername(user.userName)
                .withUserPoolId(poolId)
                .withUserAttributes(new AttributeType().withName(TEMP_AUTH_KEY).withValue(null))
        cognitoIdp.adminUpdateUserAttributes(request)
    }

    @Override
    void updateProperties(User user, GrailsParameterMap params) {

    }

    @Override
    void deleteUser(User user) {

    }

    @Override
    User getUserById(String userId) {

        if (userId == null || userId == "") {
            // Problem. This might mean an expired cookie, or it might mean that this service is not in the authorised system list
            log.debug("Attempt to get current user returned null. This might indicating that this machine is not the authorised system list")
            return null
        }

        // TODO Get access token from user login?
        def accessToken = ''

        AdminGetUserResult userResponse

        try {
            if (accessToken) {
                userResponse = cognitoIdp.getUser(new GetUserRequest().withAccessToken(accessToken))
            } else {
//            final mainAttrs = ['given_name', 'family_name', 'email', 'username', 'roles'] as Set
                userResponse = cognitoIdp.adminGetUser(new AdminGetUserRequest().withUsername(userId).withUserPoolId(poolId))
            }


            Map<String, String> attributes = userResponse.userAttributes.collectEntries { [(it.name): it.value] }
            Collection<UserProperty> userProperties = userResponse.userAttributes
                    .findAll { !mainAttrs.contains(it.name) }
                    .collect {
//                    if (it.name.startsWith('custom:')) {
//                        new UserProperty(name: it.name.substring(7), value: it.value)
//                    } else {
                        new UserProperty(name: it.name, value: it.value)
//                    }
                    }


            User user = new User(
                    dateCreated: userResponse.userCreateDate, lastUpdated: userResponse.userLastModifiedDate,
                    activated: userResponse.userStatus == "CONFIRMED", locked: !userResponse.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: userResponse.username,
                    userRoles: attributes['custom:roles']?.split(','), userProperties: userProperties
            )

            return user
        }
        catch (UserNotFoundException e) {
            return null
        }
    }

    @Override
    User getUserByEmail(String email) {
        return getUserById(email)
    }

    @Override
    User getCurrentUser() {
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
        return null
    }

    @Override
    List<String[]> countByProfileAttribute(String s, Date date, Locale locale) {
        return null
    }

    @Override
    boolean resetPassword(User user, String newPassword, boolean isPermanent) {
        if(!user || !newPassword) {
            return false
        }
        def request = new AdminSetUserPasswordRequest()
        request.username = user.email
        request.userPoolId = poolId
        request.password = newPassword
        request.permanent = isPermanent

        def response = cognitoIdp.adminSetUserPassword(request)
        return response.getSdkHttpMetadata().httpStatusCode == 200
    }

    @Override
    String getPasswordResetView() {
        return "passwordResetCognito"
    }

    @Override
    def sendAccountActivation(User user) {
        emailService.sendCognitoAccountActivation(user)
    }
}
