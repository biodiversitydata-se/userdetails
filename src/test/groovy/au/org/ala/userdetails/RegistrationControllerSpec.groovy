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
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.passay.RuleResult
import org.passay.RuleResultDetail
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
        def authKey = UUID.randomUUID().toString()
        def user = createUser(authKey)
        def username = user.userName ?: user.email ?: ''

        when:
        params.userId = user.id
        params.password = ''
        params.reenteredPassword = ''
        params.authKey = authKey
        request.method = 'POST'
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(username, "") >> new RuleResult(
                false,
                new RuleResultDetail('TOO_SHORT', [minimumLength: 8, maximumLength: 64])
        )
        1 * passwordService.buildPasswordPolicy()
        0 * _ // no other interactions
        model.errors.getFieldError("password").codes.any { c -> c.contains('.blank.') }
        view == '/registration/passwordReset'
    }

    void "The new password must be at least the minimum required length"() {
        setup:
        def authKey = UUID.randomUUID().toString()
        def password = "12345"
        def user = createUser(authKey)

        when:
        params.userId = user.id
        params.password = password
        params.reenteredPassword = password
        params.authKey = authKey
        request.method = 'POST'
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(user.email, password) >> new RuleResult(
                false,
                new RuleResultDetail('TOO_SHORT', [minimumLength: 8, maximumLength: 64])
        )
        1 * passwordService.buildPasswordPolicy()
        0 * _ // no other interactions
        model.errors.getFieldError("password").codes.any { c -> c.contains('.too_short.') }
        view == '/registration/passwordReset'
    }

    void "Password is not updated when the re-entered password does not match"() {
        setup:
        def authKey = UUID.randomUUID().toString()
        def password = "123456789"
        def user = createUser(authKey)
        def reenteredPassword = "123456543"

        when:
        params.userId = user.id
        params.password = password
        params.reenteredPassword = reenteredPassword
        params.authKey = authKey
        request.method = 'POST'
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(user.email, password) >> new RuleResult(true)
        1 * passwordService.buildPasswordPolicy()
        0 * _ // no other interactions
        model.errors.getFieldError("reenteredPassword").codes.any { c -> c.contains('.validator.invalid') }
        model.passwordMatchFail
        view == '/registration/passwordReset'
    }

    void "Password is not updated when the password validation fails"() {
        setup:
        def authKey = UUID.randomUUID().toString()
        def password = "AKSdkffhMf"
        def user = createUser(authKey)
        def reenteredPassword = password

        when:
        params.userId = user.id
        params.password = password
        params.reenteredPassword = reenteredPassword
        params.authKey = authKey
        request.method = 'POST'
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(user.email, password) >> new RuleResult(
                false,
                new RuleResultDetail('INSUFFICIENT_CHARACTERISTICS', [successCount: '2', minimumRequired: '3', ruleCount: '4'])
        )
        1 * passwordService.buildPasswordPolicy()
        0 * _ // no other interactions
        model.errors.getFieldError("password").codes.any { c -> c.contains('.insufficient_characteristics') }
        model.passwordMatchFail
        view == '/registration/passwordReset'
    }

    void "Duplicate submits of the password form are directed to a page explaining what has happened"() {
        setup:
        def authKey = UUID.randomUUID().toString()
        def password = "password1"
        def user = createUser(authKey)

        when:
        params.userId = user.id
        params.password = password
        params.reenteredPassword = password
        params.authKey = authKey
        request.method = 'POST'
        // Note that duplicate submit error is the default behaviour.
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(user.email, password) >> new RuleResult(true)
        0 * _ // no other interactions
        !model.errors
        response.redirectedUrl == '/registration/duplicateSubmit'
    }

    void "A successful submission will result in the password being reset"() {
        setup:
        String authKey = UUID.randomUUID().toString()
        String password = "password1"
        User user = createUser(authKey)
        def userId = Long.toString(1)?.toLong()

        // This is to allow the submitted token to pass validation.  Failure to do this will result in the invalidToken block being used.
        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        when:
        params.userId = userId
        params.password = password
        params.reenteredPassword = password
        params.authKey = authKey
        request.method = 'POST'
        // Note that duplicate submit error is the default behaviour.
        controller.updatePassword()

        then:
        1 * passwordService.validatePassword(user.email, password) >> new RuleResult(true)
        1 * passwordService.resetPassword(user, password)
        1 * userService.clearTempAuthKey(user)
        0 * _ // no other interactions
        response.redirectedUrl == '/registration/passwordResetSuccess'
    }

    def "Account is registered when a recaptcha response is supplied and recaptcha secret key is defined"() {
        setup:
        def password = 'password'
        def email = 'test@example.org'
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
        1 * passwordService.validatePassword(email, password) >> new RuleResult(true)
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
        1 * passwordService.validatePassword(email, password) >> new RuleResult(true)
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
        1 * passwordService.buildPasswordPolicy()
        0 * _ // no other interactions
        view == '/registration/createAccount'
        !model.edit
    }

    def "Account is not registered when password fails password policy"() {
        setup:
        def password = 'password'
        def email = 'test@example.org'
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
        1 * passwordService.validatePassword(email, password) >> new RuleResult(
                false,
                new RuleResultDetail('INSUFFICIENT_CHARACTERISTICS', [successCount: '2', minimumRequired: '3', ruleCount: '4'])
        )
        1 * passwordService.buildPasswordPolicy()
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
        1 * userService.isEmailInUse('test@example.org', user) >> false
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
        1 * userService.isEmailInUse('test@example.org', user) >> false
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
        1 * userService.isEmailInUse('test@example.org', user) >> false
        1 * passwordService.checkUserPassword(user, password) >> true
        1 * userService.updateUser(user, params) >> false
        0 * _ // no other interactions
        model.msg == "Failed to update user profile - unknown error"
        view == '/registration/accountError'
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
        1 * userService.isEmailInUse(params.email, currentUser) >> true
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
        params.confirmUserPassword = 'password'

        when:
        controller.update()

        then:
        1 * userService.updateUser(_, _) >> true
        1 * userService.isEmailInUse(params.email, currentUser) >> false
        1 * passwordService.checkUserPassword(_, 'password') >> true
        response.redirectedUrl == '/profile'
    }

}
