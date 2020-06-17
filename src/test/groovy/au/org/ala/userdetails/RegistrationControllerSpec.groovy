package au.org.ala.userdetails

import au.org.ala.recaptcha.RecaptchaClient
import au.org.ala.recaptcha.RecaptchaResponse
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import retrofit2.mock.Calls

//@Mock([User, Role, UserRole, UserProperty])
class RegistrationControllerSpec extends UserDetailsSpec implements ControllerUnitTest<RegistrationController>, DataTest {

    def passwordService = Mock(PasswordService)
    def userService = Mock(UserService)
    def emailService = Mock(EmailService)
    def recaptchaClient = Mock(RecaptchaClient)

    void setup() {
        controller.passwordService = passwordService
        controller.userService = userService
        controller.emailService = emailService
        controller.recaptchaClient = recaptchaClient
    }

    void setupSpec() {
        mockDomains(User, Role, UserRole, UserProperty)
    }

    void "A new password must be supplied"() {
        setup:
        String authKey = UUID.randomUUID().toString()
        User user = createUser(authKey)
        def calculatedUserName = 'test'
        request.method = 'POST'
        params.userId = user.id
        params.authKey = authKey

        when:
        params.password = ""
        params.reenterPassword = ""
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(calculatedUserName, "") >> [valid: false, metadata: null, details: null, entropy: 0]
        0 * _ // no other interactions
        model.errors.getFieldError("password").codes.any { c -> c.contains('.blank.')}
        view == '/registration/passwordReset'
    }

    void "The new password must be at least the minimum required length"() {
        setup:
        String authKey = UUID.randomUUID().toString()
        String password = "12345"
        User user = createUser(authKey)
        def calculatedUserName = 'test'
        request.method = 'POST'
        params.userId = user.id
        params.authKey = authKey

        when:
        params.password = password
        params.reenteredPassword = password
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(calculatedUserName, password) >> [valid: false, metadata: null, details: null, entropy: 10]
        0 * _ // no other interactions
        model.errors.getFieldError("password").codes.any { c -> c.contains('.minSize.notmet.')}
        view == '/registration/passwordReset'
    }

    void "Password is not updated when the re-entered password does not match"() {
        setup:
        String authKey = UUID.randomUUID().toString()
        String password = "123456789"
        User user = createUser(authKey)
        request.method = 'POST'
        params.userId = user.id
        params.authKey = authKey

        when:
        params.password = password
        params.reenteredPassword = "123456543"
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(_, password) >> [valid: true, metadata: null, details: null, entropy: 10]
        0 * _ // no other interactions
        model.errors.getFieldError("reenteredPassword").codes.any { c -> c.contains('.validator.invalid')}
        model.passwordMatchFail
        view == '/registration/passwordReset'
    }

    void "Password is not updated when the password validation fails"() {
        setup:
        String authKey = UUID.randomUUID().toString()
        String password = "AKSdkffhMf"
        User user = createUser(authKey)
        def calculatedUserName = 'test'
        request.method = 'POST'
        params.userId = user.id
        params.authKey = authKey

        when:
        params.password = password
        params.reenteredPassword = password
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(calculatedUserName, password) >> [
                valid  : false, metadata: null, entropy: 10,
                details: [[errorCodes: ['INSUFFICIENT_CHARACTERISTICS'], values: ['2', '3', '4'] as Object[]]]]
        0 * _ // no other interactions
        model.errors.getFieldError("password").codes.any { c -> c.contains('.insufficient_characteristics')}
        model.passwordMatchFail
        view == '/registration/passwordReset'
    }

    void "Duplicate submits of the password form are directed to a page explaining what has happened"() {
        setup:
        String authKey = UUID.randomUUID().toString()
        String password = "password1"
        User user = createUser(authKey)
        def calculatedUserName = 'test'
        request.method = 'POST'
        params.userId = user.id
        params.authKey = authKey

        when:
        params.password = password
        params.reenteredPassword = password

        // Note that duplicate submit error is the default behaviour.
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(calculatedUserName, password) >> [valid: true, metadata: null, details: null, entropy: 10]
        0 * _ // no other interactions
        !model.errors
        response.redirectedUrl == '/registration/duplicateSubmit'
    }

    void "A successful submission will result in the password being reset"() {
        setup:
        String authKey = UUID.randomUUID().toString()
        String password = "password1"
        User user = createUser(authKey)
        def calculatedUserName = 'test'
        request.method = 'POST'
        params.userId = Long.toString(1)
        params.authKey = authKey

        // This is to allow the submitted token to pass validation.  Failure to do this will result in the invalidToken block being used.
        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        when:
        params.password = password
        params.reenteredPassword = password

        // Note that duplicate submit error is the default behaviour.
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(calculatedUserName, password) >> [valid: true, metadata: null, details: null, entropy: 10]
        1 * passwordService.resetPassword(user, password)
        1 * userService.clearTempAuthKey(user)
        0 * _ // no other interactions
        response.redirectedUrl == '/registration/passwordResetSuccess'
    }

    def "Account is registered when a recaptcha response is supplied and recaptcha secret key is defined"() {
        setup:
        def password = 'password'
        def email = 'test@example.org'
        def calculatedUserName = 'test'
        def authKey = '987'
        def recaptchaSecretKey = 'xyz'
        def recaptchaResponseKey = '123'
        def remoteAddressIp = '127.0.0.1'
        grailsApplication.config.recaptcha.secretKey = recaptchaSecretKey

        // This is to allow the submitted token to pass validation.  Failure to do this will result in the invalidToken block being used.
        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        when:
        params.email = email
        params.firstName = 'Test'
        params.lastName = 'Test'
        params['organisation'] = 'Org'
        params.country = 'AU'
        params.state = 'ACT'
        params.city = 'Canberra'
        params.password = password
        params.reenteredPassword = password
        params['g-recaptcha-response'] = recaptchaResponseKey
        request.remoteAddr = remoteAddressIp

        controller.register()

        then:
        1 * recaptchaClient.verify(recaptchaSecretKey, recaptchaResponseKey, remoteAddressIp) >> { Calls.response(new RecaptchaResponse(true, '2019-09-27T16:06:00Z', 'test-host', [])) }
        1 * userService.isEmailRegistered(email) >> false
        1 * passwordService.validatePassword(calculatedUserName, password) >> [valid: true, metadata: null, details: null, entropy: 10]
        1 * userService.registerUser(_) >> { def user = new User(params); user.tempAuthKey = authKey; user }
        1 * passwordService.resetPassword(_, password)
        1 * emailService.sendAccountActivation(_, authKey)
        0 * _ // no other interactions
        response.redirectedUrl == '/registration/accountCreated'
    }

    def "Account is registered when no recaptcha secret key is defined"() {
        setup:
        def password = 'password'
        def email = 'test@example.org'
        def calculatedUserName = 'test'
        def authKey = '987'
        def remoteAddressIp = '127.0.0.1'
        grailsApplication.config.recaptcha.secretKey = ''

        // This is to allow the submitted token to pass validation.  Failure to do this will result in the invalidToken block being used.
        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        when:
        params.email = email
        params.firstName = 'Test'
        params.lastName = 'Test'
        params['organisation'] = 'Org'
        params.country = 'AU'
        params.state = 'ACT'
        params.city = 'Canberra'
        params.password = password
        params.reenteredPassword = password
        request.remoteAddr = remoteAddressIp

        controller.register()

        then:
        0 * recaptchaClient.verify(_, _, _)
        1 * userService.isEmailRegistered(email) >> false
        1 * passwordService.validatePassword(calculatedUserName, password) >> [valid: true, metadata: null, details: null, entropy: 10]
        1 * userService.registerUser(_) >> { def user = new User(params); user.tempAuthKey = authKey; user }
        1 * passwordService.resetPassword(_, password)
        1 * emailService.sendAccountActivation(_, authKey)
        0 * _ // no other interactions
        response.redirectedUrl == '/registration/accountCreated'
    }

    def "Account is not registered when recaptcha secret key is defined and no recaptcha response is present"() {
        setup:
        def secretKey = 'xyz'
        grailsApplication.config.recaptcha.secretKey = secretKey

        // This is to allow the submitted token to pass validation.  Failure to do this will result in the invalidToken block being used.
        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        when:
        params.email = 'test@example.org'
        params.firstName = 'Test'
        params.lastName = 'Test'
        params['organisation'] = 'Org'
        params.country = 'AU'
        params.state = 'ACT'
        params.city = 'Canberra'
        params.password = 'password'
        params.reenteredPassword = 'password'
//        params['g-recaptcha-response'] = '123'
        request.remoteAddr = '127.0.0.1'

        controller.register()

        then:
        1 * recaptchaClient.verify(secretKey, null, '127.0.0.1') >> { Calls.response(new RecaptchaResponse(false, null, null, ['missing-input-response'])) }
        0 * userService.registerUser(_)
        0 * passwordService.resetPassword(_, _)
        0 * emailService.sendAccountActivation(_, _)
        0 * _ // no other interactions
        view == '/registration/createAccount'
        !model.edit
    }

    def "Account is not registered when password fails password policy"() {
        setup:
        def password = 'password'
        def email = 'test@example.org'
        def calculatedUserName = 'test'
        def remoteAddressIp = '127.0.0.1'
        grailsApplication.config.recaptcha.secretKey = ''

        // This is to allow the submitted token to pass validation.  Failure to do this will result in the invalidToken block being used.
        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        when:
        params.email = email
        params.firstName = 'Test'
        params.lastName = 'Test'
        params['organisation'] = 'Org'
        params.country = 'AU'
        params.state = 'ACT'
        params.city = 'Canberra'
        params.password = password
        params.reenteredPassword = password
        request.remoteAddr = remoteAddressIp

        controller.register()

        then:
        0 * recaptchaClient.verify(_, _, _)
        1 * userService.isEmailRegistered(email) >> false
        1 * passwordService.validatePassword(calculatedUserName, password) >> [
                valid  : false, metadata: null, entropy: 10,
                details: [[errorCodes: ['INSUFFICIENT_CHARACTERISTICS'], values: ['2', '3', '4'] as Object[]]]]
        0 * _ // no other interactions
        view == '/registration/createAccount'
        !model.edit
        flash.message.startsWith('The selected password does not meet the password policy.')
    }

    def "Account is updated when the current password is included"() {
        setup:
        def password = "HPVBq46QmEH0YhWo6xek"
        def authKey = "W0E6QMaKUJnzTlqSNQXk"
        User user = createUser(authKey)

        when:
        params.email = 'test@example.org'
        params.firstName = 'Test'
        params.lastName = 'Test'
        params['organisation'] = 'Org'
        params.country = 'AU'
        params.state = 'ACT'
        params.city = 'Canberra'
        params.confirmUserPassword = password
        request.remoteAddr = '127.0.0.1'

        controller.update()

        then:
        1 * userService.currentUser >> user
        1 * passwordService.checkUserPassword(user, password) >> true
        1 * userService.updateUser(user, params) >> true
        0 * _ // no other interactions
        response.redirectedUrl == '/profile'
    }

    def "Account is not updated when wrong password is specified"() {
        setup:
        def wrongPassword = 'O6I8NdjRFLXpwOVhYeWt'
        def authKey = "W0E6QMaKUJnzTlqSNQXk"
        User user = createUser(authKey)

        when:
        params.email = 'test@example.org'
        params.firstName = 'Test'
        params.lastName = 'Test'
        params['organisation'] = 'Org'
        params.country = 'AU'
        params.state = 'ACT'
        params.city = 'Canberra'
        params.confirmUserPassword = wrongPassword
        request.remoteAddr = '127.0.0.1'

        controller.update()

        then:
        1 * userService.currentUser >> user
        1 * passwordService.checkUserPassword(user, wrongPassword) >> false
        0 * _ // no other interactions
        flash.message == 'Incorrect password. Could not update account details. Please try again.'
        model.edit
        model.user == user
        view == '/registration/createAccount'
    }

    def "Account is not updated when the user cannot be found"() {
        setup:
        def password = 'HPVBq46QmEH0YhWo6xek'

        when:
        params.email = 'test@example.org'
        params.firstName = 'Test'
        params.lastName = 'Test'
        params['organisation'] = 'Org'
        params.country = 'AU'
        params.state = 'ACT'
        params.city = 'Canberra'
        params.confirmUserPassword = password
        request.remoteAddr = '127.0.0.1'

        controller.update()

        then:
        1 * userService.currentUser >> null
        0 * _ // no other interactions
        model.msg == "The current user details could not be found"
        view == '/registration/accountError'
    }

    def "Account is not updated when the user details cannot be updated"() {
        setup:
        def password = 'HPVBq46QmEH0YhWo6xek'
        def authKey = "W0E6QMaKUJnzTlqSNQXk"
        User user = createUser(authKey)

        when:
        params.email = 'test@example.org'
        params.firstName = 'Test'
        params.lastName = 'Test'
        params['organisation'] = 'Org'
        params.country = 'AU'
        params.state = 'ACT'
        params.city = 'Canberra'
        params.confirmUserPassword = password
        request.remoteAddr = '127.0.0.1'

        controller.update()

        then:
        1 * userService.currentUser >> user
        1 * passwordService.checkUserPassword(user, password) >> true
        1 * userService.updateUser(user, params) >> false
        0 * _ // no other interactions
        model.msg == "Failed to update user profile - unknown error"
        view == '/registration/accountError'
    }
}
