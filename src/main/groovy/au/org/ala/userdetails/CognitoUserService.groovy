package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.userdetails.records.IUserRecord
import au.org.ala.userdetails.records.UserPropertyRecord
import au.org.ala.userdetails.records.UserRecord
import au.org.ala.web.AuthService
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest
import com.amazonaws.services.cognitoidp.model.AdminDeleteUserRequest
import com.amazonaws.services.cognitoidp.model.AdminDisableUserRequest
import com.amazonaws.services.cognitoidp.model.AdminEnableUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest
import com.amazonaws.services.cognitoidp.model.AdminResetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest
import com.amazonaws.services.cognitoidp.model.AttributeType
import com.amazonaws.services.cognitoidp.model.GetUserRequest
import com.amazonaws.services.cognitoidp.model.UserType
import com.amazonaws.services.cognitosync.AmazonCognitoSync

import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.NotImplementedException

@Slf4j
class CognitoUserService implements IUserService {

    static mainAttrs = ['given_name', 'family_name', 'email', 'username', 'roles'] as Set

    static boolean affiliationsEnabled
    public static final String TEMP_AUTH_KEY = 'tempAuthKey'

    EmailService emailService

    AWSCognitoIdentityProvider cognitoIdp

    AuthService authService

    final String poolId

    CognitoUserService(AuthService authService, AWSCognitoIdentityProvider cognitoIdp, String poolId) {
        this.authService = authService

        this.cognitoIdp = cognitoIdp

        this.poolId = poolId
    }

    @Override
    UserRecord getUser(Object id) {
        return null
    }

    @Override
    UserRecord getUserByEmail(Object email) {
        return null
    }

    @Override
    def updateUser(UserRecord user, GrailsParameterMap params) {
        def emailRecipients = [user.email]
        if (params.email != user.email) {
            emailRecipients << params.email
        }
        try {
            user.setProperties(params)
            user.activated = true
            user.locked = false
//            user.save(failOnError: true, flush:true)
            updateProperties(user, params)

            def request = updateUserAttributesRequest(user)

            def result = cognitoIdp.adminUpdateUserAttributes(request)

            emailService.sendUpdateProfileSuccess(user, emailRecipients)
            true
        } catch (Exception e){
            log.error(e.getMessage(), e)
            false
        }
    }

    private AdminUpdateUserAttributesRequest updateUserAttributesRequest(UserRecord record) {
        def request = new AdminUpdateUserAttributesRequest()
        request.withUserPoolId(poolId)
        request.withUsername(record.userName)
        def attributes= record.userProperties.collect { new AttributeType().withName(it.name).withValue(it.value)}
        request.withUserAttributes()
        def userAttributes = record.userProperties.collect { new AttributeType().withName(it.name).withValue(it.value) }
        userAttributes.add(new AttributeType().withName('email').withValue(record.email))
        userAttributes.add(new AttributeType().withName('userName').withValue(record.userName))
//        userAttributes.add(new AttributeType().withName('userid').withValue(record.id))
        userAttributes.add(new AttributeType().withName('given_name').withValue(record.firstName))
        userAttributes.add(new AttributeType().withName('family_name').withValue(record.lastName))

        return request
    }

    @Override
    def disableUser(UserRecord user) {
        cognitoIdp.adminDisableUser(new AdminDisableUserRequest().withUsername(user.userName).withUserPoolId(poolId))
//        return null
    }

    @Override
    boolean isActive(String email) {
        return currentUser.activated
    }

    @Override
    boolean isLocked(String email) {
        return currentUser.locked
    }

    @Override
    boolean isEmailRegistered(String email) {
        return false
    }

    @Override
    boolean isEmailInUse(String newEmail, UserRecord user) {
        return false
    }

    @Override
    def activateAccount(UserRecord user) {
        cognitoIdp.adminEnableUser(new AdminEnableUserRequest().withUsername(user.userName).withUserPoolId(poolId))
        return null
    }

    @Override
    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody) {
        throw new NotImplementedException()
    }

    @Override
    UserRecord registerUser(GrailsParameterMap params) throws Exception {

//        //does a user with the supplied email address exist
//        def user = new User(params)
//        user.userName = params.email
//        user.activated = false
//        user.locked = false
//        user.tempAuthKey = UUID.randomUUID().toString()
//        def createdUser = user.save(flush: true, failOnError: true)
//        updateProperties(createdUser, params)
//
//        //add a role of user
//        def roleUser = Role.findByRole("ROLE_USER")
//        new UserRole(user:user, role:roleUser).save(flush:true, failOnError: true)
//
//        log.info("Newly created user: " + createdUser.id)
//        createdUser

//        cognitoIdp.signUp()

//        cognitoIdp.group

        // TODO SignUp request instead
        def request = new AdminCreateUserRequest()
        request.withUserPoolId(poolId)

        request.withUsername(params['email'])
        def userAttributes = []
        userAttributes.add(new AttributeType().withName('email').withValue(params['email']))
//        userAttributes.add(new AttributeType().withName('userName').withValue(record.userName))
//        userAttributes.add(new AttributeType().withName('userid').withValue(record.id))
        userAttributes.add(new AttributeType().withName('given_name').withValue(params['firstName']))
        userAttributes.add(new AttributeType().withName('family_name').withValue(params['lastName']))
        userAttributes.add(new AttributeType().withName('country').withValue(params['country']))
        userAttributes.add(new AttributeType().withName('state').withValue(params['state']))
        userAttributes.add(new AttributeType().withName('ala:roles').withValue(params['ala:roles']))
        userAttributes.add(new AttributeType().withName('affiliation').withValue(params['affiliation']))
        userAttributes.add(new AttributeType().withName(TEMP_AUTH_KEY).withValue(UUID.randomUUID().toString()))
        request.withUserAttributes(userAttributes)
        def userResult = cognitoIdp.adminCreateUser(request)

        def passwordResult = cognitoIdp.adminSetUserPassword(
                new AdminSetUserPasswordRequest()
                        .withUserPoolId(poolId)
                        .withUsername(userResult.user.username)
                        .withPassword(params['password'])
                        .withPermanent(true)
        )

//        cognitoIdp.verifyUserAttribute()

        return userTypeToUserRecord(userResult.user)
    }

    private userTypeToUserRecord(UserType user) {

        def attributes = user.attributes.collectEntries { [(it.name): it.value]}
        def otherAttributes =
                attributes.findAll { !mainAttrs.contains(it.key) }
                        .collect { new UserPropertyRecord(name: it.key, value: it.value) }
        def userRecord = new UserRecord(
                dateCreated: user.userCreateDate, lastUpdated: user.userLastModifiedDate,
                activated: user.userStatus == "CONFIRMED", locked: !user.enabled,
                firstName: attributes['given_name'], lastName: attributes['family_name'],
                email: attributes['email'], userName: attributes['username'],
                userRoles: attributes['roles'], userProperties:  otherAttributes
        )
        return userRecord
    }

    @Override
    def updateProperties(UserRecord user, GrailsParameterMap params) {
        ['city', 'organisation', 'state', 'country'].each { propName ->
            setUserProperty(user, propName, params.get(propName, ''))
        }
        if (affiliationsEnabled) {
            setUserProperty(user, 'affiliation', params.get('affiliation', ''))
        }
    }

    private UserPropertyRecord setUserProperty(UserRecord user, String propName, String value) {
        def prop = user.getUserProperties().find { it.name == propName }
        if (prop) {
            prop.value = value
        } else {
            user.getUserProperties().add(new UserPropertyRecord(user: user, name: propName, value: value))

        }
    }

    @Override
    def deleteUser(UserRecord user) {
        def request = new AdminDeleteUserRequest()
        request.username = user.userName
        request.userPoolId = poolId
        def result = cognitoIdp.adminDeleteUser(request)
//        result.
        return null
    }

    @Override
    def resetAndSendTemporaryPassword(UserRecord user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {
        def request = new AdminResetUserPasswordRequest()
        request.username = user.userName
        request.userPoolId = poolId

        cognitoIdp.adminResetUserPassword(request)
        return null
    }

    @Override
    def clearTempAuthKey(UserRecord user) {
        def request = new AdminUpdateUserAttributesRequest()
                .withUsername(user.userName)
                .withUserPoolId(poolId)
                .withUserAttributes(new AttributeType().withName(TEMP_AUTH_KEY).withValue(null))
        cognitoIdp.adminUpdateUserAttributes(request)
        return null
    }

    @Override
    UserRecord getCurrentUser() {
        def userId = authService.getUserId()
        if (userId == null) {
            // Problem. This might mean an expired cookie, or it might mean that this service is not in the authorised system list
            log.debug("Attempt to get current user returned null. This might indicating that this machine is not the authorised system list")
            return null
        }

        // TODO Get access token from user login?
        def accessToken = ''
        UserRecord userRecord
        if (accessToken) {
            def userResponse = cognitoIdp.getUser(new GetUserRequest().withAccessToken(accessToken))
            def attributes = userResponse.userAttributes.collectEntries { [(it.name): it.value]}
            def otherAttributes =
                    attributes.findAll { !mainAttrs.contains(it.key) }
                            .collect { new UserPropertyRecord(name: it.key, value: it.value) }
            userRecord = new UserRecord(
                    dateCreated: userResponse.userCreateDate, lastUpdated: userResponse.userLastModifiedDate,
                    activated: userResponse.userStatus == "CONFIRMED", locked: !userResponse.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: attributes['username'],
                    userRoles: attributes['roles'], userProperties:  otherAttributes
            )
        } else {
//            final mainAttrs = ['given_name', 'family_name', 'email', 'username', 'roles'] as Set
            def userResponse = cognitoIdp.adminGetUser(new AdminGetUserRequest().withUsername(userId).withUserPoolId(poolId))
            def attributes = userResponse.userAttributes.collectEntries { [(it.name): it.value]}
            def otherAttributes =
                    attributes.findAll { !mainAttrs.contains(it.key) }
                              .collect { new UserPropertyRecord(name: it.key, value: it.value) }
            userRecord = new UserRecord(
                    dateCreated: userResponse.userCreateDate, lastUpdated: userResponse.userLastModifiedDate,
                    activated: userResponse.userStatus == "CONFIRMED", locked: !userResponse.enabled,
                    firstName: attributes['given_name'], lastName: attributes['family_name'],
                    email: attributes['email'], userName: attributes['username'],
                    userRoles: attributes['roles'], userProperties:  otherAttributes
            )
        }
        return userRecord
    }

    @Override
    String getResetPasswordUrl(UserRecord user) {
        return null
    }

    @Override
    def findUsersForExport(List usersInRoles, Object includeInactive) {
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
}
