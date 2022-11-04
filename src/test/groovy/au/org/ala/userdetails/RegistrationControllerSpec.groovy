/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.userdetails

import au.org.ala.recaptcha.RecaptchaClient
import au.org.ala.recaptcha.RecaptchaResponse
import au.org.ala.users.Role
import au.org.ala.users.User
import au.org.ala.users.UserProperty
import au.org.ala.users.UserRole
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import retrofit2.mock.Calls


//@Mock([User, Role, UserRole, UserProperty])
class RegistrationControllerSpec extends UserDetailsSpec implements ControllerUnitTest<RegistrationController>, DataTest {

    def passwordService = Mock(PasswordService)
    def userService = Mock(GormUserService)
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
        request.method = 'POST'
        params.userId = Long.toString(1)
        params.authKey = "test"

        when:
        params.password = ""
        params.reenterPassword = ""
        controller.updatePassword()

        then:
        model.errors.getFieldError("password") != null
        view == '/registration/passwordReset'
    }

    void "The new password must be at least 6 characters long"() {
        setup:
        request.method = 'POST'
        params.userId = Long.toString(1)
        params.authKey = "test"

        when:
        params.password = "12345"
        controller.updatePassword()

        then:
        model.errors.getFieldError("password") != null
        view == '/registration/passwordReset'
    }

    void "The new re-entered password must be the same as the original"() {
        setup:
        request.method = 'POST'
        params.userId = Long.toString(1)
        params.authKey = "test"

        when:
        params.password = "123456"
        params.reenteredPassword = "12345"
        controller.updatePassword()

        then:
        model.errors.getFieldError("reenteredPassword") != null
        view == '/registration/passwordReset'
    }

    void "Duplicate submits of the password form are directed to a page explaining what has happened"() {
        setup:
        request.method = 'POST'
        params.userId = Long.toString(1)
        params.authKey = "test"

        when:
        params.password = "123456"
        params.reenteredPassword = "123456"

        // Note that duplicate submit error is the default behaviour.
        controller.updatePassword()

        then:
        response.redirectedUrl == '/registration/duplicateSubmit'
    }

    void "A successful submission will result in the users password being reset"() {
        setup:
        String authKey = "test"
        String password = "password"
        User user = createUser(authKey)
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
        1 * passwordService.resetPassword(user, password)
        1 * userService.clearTempAuthKey(user)
        response.redirectedUrl == '/registration/passwordResetSuccess'
    }

    def "Account is registered when a recaptcha response is supplied and recaptcha secret key is defined"() {
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
        params['g-recaptcha-response'] = '123'
        request.remoteAddr = '127.0.0.1'

        controller.register()

        then:
        1 * recaptchaClient.verify(secretKey, '123', '127.0.0.1') >> { Calls.response(new RecaptchaResponse(true, '2019-09-27T16:06:00Z', 'test-host', [])) }
        1 * userService.registerUser(_) >> { def user = new User(params); user.tempAuthKey = '123'; user }
        1 * passwordService.resetPassword(_, 'password')
        1 * emailService.sendAccountActivation(_, '123')
        response.redirectedUrl == '/registration/accountCreated'
    }

    def "Account is registered when no recaptcha secret key is defined"() {
        setup:
        grailsApplication.config.recaptcha.secretKey = ''

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
        request.remoteAddr = '127.0.0.1'

        controller.register()

        then:
        0 * recaptchaClient.verify(_, _, _)
        1 * userService.registerUser(_) >> { def user = new User(params); user.tempAuthKey = '123'; user }
        1 * passwordService.resetPassword(_, 'password')
        1 * emailService.sendAccountActivation(_, '123')
        response.redirectedUrl == '/registration/accountCreated'
    }

    def "Account is not register when recaptcha secret key is defined and no recaptcha response is present"() {
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
    }

    void "A new email address must not be in use by others"() {
        setup:
        User currentUser = new User()
        currentUser.email = 'currentUser@example.org'
        userService.currentUser >> currentUser
        params.email = 'in.use@example.org'

        when:
        controller.update()

        then:
        1 * userService.isEmailInUse(params.email) >> true
        model.msg.indexOf("A user is already registered") != -1
        view == '/registration/accountError'
    }

    void "Account is updated when a valid new email address is supplied"() {
        setup:
        User currentUser = new User()
        currentUser.email = 'currentUser@example.org'
        userService.currentUser >> currentUser

        params.email = 'test@example.org'
        params.firstName = 'Test'
        params.lastName = 'Test'
        params['organisation'] = 'Org'
        params.country = 'AU'
        params.state = 'ACT'
        params.city = 'Canberra'
        params.password = 'password'
        params.reenteredPassword = 'password'

        when:
        controller.update()

        then:
        1 * userService.updateUser(_, _) >> true
        1 * userService.isEmailInUse(params.email) >> false
        response.redirectedUrl == '/profile'
    }

}
