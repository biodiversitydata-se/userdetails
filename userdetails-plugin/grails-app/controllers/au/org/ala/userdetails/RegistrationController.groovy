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

import au.org.ala.auth.UpdateCognitoPasswordCommand
import au.org.ala.auth.UpdatePasswordCommand
import au.org.ala.recaptcha.RecaptchaClient
import au.org.ala.users.UserRecord
import au.org.ala.ws.service.WebService
import grails.converters.JSON

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.passay.RuleResult
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.validation.Errors

/**
 * Controller that handles the interactions with general public.
 * Supports:
 *
 * 1. Account creation
 * 2. Account editing
 * 3. Password reset
 * 4. Account activation
 */
class RegistrationController {

    def simpleCaptchaService
    def emailService
    def passwordService

    @Qualifier('userService')
    IUserService userService
    def locationService
    RecaptchaClient recaptchaClient
    WebService webService
    def messageSource

    @Value('${userdetails.features.requirePasswordForUserUpdate:true}')
    boolean requirePasswordForUserUpdate

    def index() {
        redirect(action: 'createAccount')
    }

    def createAccount() {
        render(view: 'createAccount', model: [
                passwordPolicy: passwordService.buildPasswordPolicy(),
        ])
    }

    def editAccount() {
        def user = userService.currentUser
        render(view: 'createAccount', model: [edit: true, user: user, props: user?.propsAsMap(), passwordPolicy: passwordService.buildPasswordPolicy()])
    }

    def passwordReset() {
        UserRecord user = userService.getUserById(params.userId)
        if (!user) {
            render(view: 'accountError', model: [msg: "UserRecord not found with ID ${params.userId}"])
        } else if (user.tempAuthKey == params.authKey) {
            //keys match, so lets reset password
            render(view: 'passwordReset', model: [user: user, authKey: params.authKey, passwordPolicy: passwordService.buildPasswordPolicy()])
        } else {
            render(view: 'authKeyExpired')
        }
    }

    def updatePassword(UpdatePasswordCommand cmd) {
        UserRecord user = userService.getUserById(cmd.userId as String)

        // since the email address is the user name, use the part before the @ as the username
        def username = user?.userName ?: user?.email ?: ''
        def validationResult = passwordService.validatePassword(username, cmd?.password)
        buildErrorMessages(validationResult, cmd.errors)

        if (cmd.hasErrors()) {
            render(view: 'passwordReset', model: [user: user, authKey: cmd.authKey, errors:cmd.errors, passwordMatchFail: true, passwordPolicy: passwordService.buildPasswordPolicy()])
        }
        else {
            withForm {
                if (user.tempAuthKey == cmd.authKey) {
                    //update the password
                    try {
                        passwordService.resetPassword(user, cmd.password, true, null)
                        userService.clearTempAuthKey(user)
                        redirect(controller: 'registration', action: 'passwordResetSuccess')
                        log.info("Password successfully reset for user: " + cmd.userId)
                    } catch (e) {
                        log.error("Couldn't reset password", e)
                        render(view: 'accountError', model: [msg: "Failed to reset password"])
                    }
                } else {
                    log.error "Password was not reset as AUTH_KEY did not match -- ${user.tempAuthKey} vs ${cmd.authKey}"
                    render(view: 'accountError', model: [msg: "Password was not reset as AUTH_KEY did not match"])
                }
            }
            .invalidToken {
                redirect(action: 'duplicateSubmit', model: [msg: ""])
            }
        }

    }

    def updateCognitoPassword(UpdateCognitoPasswordCommand cmd) {
        UserRecord user = userService.getUserByEmail(cmd.email)
        if (cmd.hasErrors()) {
            render(view: 'passwordResetCognito', model: [email: cmd.email, code: cmd.code, errors:cmd.errors, passwordMatchFail: true, passwordPolicy: passwordService.buildPasswordPolicy()])
        }
        else {
            withForm {
                if(!user) {
                    log.error "Invalid email ${cmd.email}"
                    render(view: 'accountError', model: [msg: "Invalid email ${cmd.email}"])
                }
                //update the password
                try {
                    def success = passwordService.resetPassword(user, cmd.password, true, cmd.code)
                    if(success) {
                        Map resp = webService.get("${grailsApplication.config.getProperty('grails.serverURL')}/logout?")
                        redirect(controller: 'registration', action: 'passwordResetSuccess')
                        log.info("Password successfully reset for user: " + cmd.email)
                    }
                    else{
                        render(view: 'accountError', model: [msg: "Failed to reset password"])
                    }
                } catch (e) {
                    log.error("Couldn't reset password", e)
                    render(view: 'accountError', model: [msg: "Failed to reset password"])
                }
            }
                    .invalidToken {
                        redirect(action: 'duplicateSubmit', model: [msg: ""])
                    }
        }

    }

    /**
     * Used only to display the view.
     * This is required given that the view is not rendered directly by #disableAccount but rather a chain
     * of redirects:  userdetails logout, cas logout and finally this view
     */
    def accountDisabled() {
    }

    /** Displayed as a result of a password update with a duplicate form submission. */
    def duplicateSubmit() {
        [serverUrl: grailsApplication.config.getProperty('grails.serverURL') + '/myprofile']
    }

    def passwordResetSuccess() {
        [serverUrl: grailsApplication.config.getProperty('grails.serverURL') + '/myprofile']
    }

    def startPasswordReset() {
        //check for human
        def recaptchaKey = grailsApplication.config.getProperty('recaptcha.secretKey')
        if (recaptchaKey) {
            def recaptchaResponse = params['g-recaptcha-response']
            def call = recaptchaClient.verify(recaptchaKey, recaptchaResponse, request.remoteAddr)
            def response = call.execute()
            if (response.isSuccessful()) {
                def verifyResponse = response.body()
                if (!verifyResponse.success) {
                    log.warn('Recaptcha verify reported an error: {}', verifyResponse)
                    flash.message = 'There was an error with the captcha, please try again'
                    render(view: 'forgottenPassword', model: [email: params.email, captchaInvalid: true])
                    return
                }
            } else {
                //send password reset link
                render(view: 'forgottenPassword', model: [email: params.email, captchaInvalid: true])
                return
            }
        }

        log.info("Starting password reset for email address: " + params.email)
        def user = userService.getUserById(params.email)
        if (user) {
            try {
                passwordService.resetAndSendTemporaryPassword(user, null, null, null, null)
                render(view: passwordService.getPasswordResetView(), model: [email: params.email, passwordPolicy: passwordService.buildPasswordPolicy()])
            } catch (Exception e) {
                log.error("Problem starting password reset for email address: " + params.email)
                log.error(e.getMessage(), e)
                render(view: 'accountError', model: [msg: e.getMessage()])
            }
        } else {
            //invalid email address entered
            log.warn("email address {} is not recognised.", params.email)
            render(view: 'forgottenPassword', model: [email: params.email, emailInvalid: true])
        }

    }

    def disableAccount() {
        def user = userService.currentUser

        log.debug("Disabling account for " + user)
        if (user) {
            def success = userService.disableUser(user)

            if (success) {
                redirect(controller: 'logout', action: 'logout', params: [appUrl: grailsApplication.config.getProperty('grails.serverURL') + '/registration/accountDisabled'])
            } else {
                render(view: "accountError", model: [msg: "Failed to disable user profile - unknown error"])
            }
        } else {
            render(view: "accountError", model: [msg: "The current user details could not be found"])
        }
    }

    def update() {

        def user = userService.currentUser

        log.debug("Updating account for " + user)

        if (user) {
            if (params.email != user.email) {
                // email address has changed
                if (userService.isEmailInUse(params.email)) {
                    def msg = message(code: "update.account.failure.msg", default: "Failed to update user profile - A user is already registered with the email address.")
                    render(view: "accountError", model: [msg: msg])
                    return
                }
                // and username and email address must be kept in sync
//                params.userName = params.email
            }

            // TODO might need to remove this for delegated auth?
            if (requirePasswordForUserUpdate) {
                def isCorrectPassword = passwordService.checkUserPassword(user, params.confirmUserPassword)
                if (!isCorrectPassword) {
                    flash.message = 'Incorrect password. Could not update account details. Please try again.'
                    render(view: 'createAccount', model: [edit: true, user: user, props: user?.propsAsMap(), passwordPolicy: passwordService.buildPasswordPolicy()])
                    return
                }
            }

            def success = userService.updateUser(user.userId, params)

            if (success) {
                redirect(controller: 'profile')
                log.info("Account details updated for user: " + user.id + " username: " + user.userName)
            } else {
                render(view: "accountError", model: [msg: "Failed to update user profile - unknown error"])
            }
        } else {
            render(view: "accountError", model: [msg: "The current user details could not be found"])
        }
    }

    def register() {
        def paramsEmail = params?.email?.toString()
        def paramsPassword = params?.password?.toString()
        withForm {

            def recaptchaKey = grailsApplication.config.getProperty('recaptcha.secretKey')
            if (recaptchaKey) {
                def recaptchaResponse = params['g-recaptcha-response']
                def call = recaptchaClient.verify(recaptchaKey, recaptchaResponse, request.remoteAddr)
                def response = call.execute()
                if (response.isSuccessful()) {
                    def verifyResponse = response.body()
                    if (!verifyResponse.success) {
                        log.warn('Recaptcha verify reported an error: {}', verifyResponse)
                        flash.message = 'There was an error with the captcha, please try again'
                        render(view: 'createAccount', model: [edit: false, user: params, props: params, passwordPolicy: passwordService.buildPasswordPolicy()])
                        return
                    }
                } else {
                    log.warn("error from recaptcha {}", response)
                    flash.message = 'There was an error with the captcha, please try again'
                    render(view: 'createAccount', model: [edit: false, user: params, props: params, passwordPolicy: passwordService.buildPasswordPolicy()])
                    return
                }
            }

            //create user account...
            if (!paramsEmail || userService.isEmailInUse(paramsEmail)) {
                def inactiveUser = !userService.isActive(paramsEmail)
                def lockedUser = userService.isLocked(paramsEmail)
                render(view: 'createAccount', model: [edit: false, user: params, props: params, alreadyRegistered: true, inactiveUser: inactiveUser, lockedUser: lockedUser, passwordPolicy: passwordService.buildPasswordPolicy()])
            } else {

                def passwordValidation = passwordService.validatePassword(paramsEmail, paramsPassword)
                if (!passwordValidation.valid) {
                    log.warn("The password for user name '${paramsEmail}' did not meet the validation criteria '${passwordValidation}'")
                    flash.message = "The selected password does not meet the password policy. Please try again with a different password. ${buildErrorMessages(passwordValidation)}"
                    render(view: 'createAccount', model: [edit: false, user: params, props: params, passwordPolicy: passwordService.buildPasswordPolicy()])
                    return
                }

                try {
                    //does a user with the supplied email address exist
                    def user = userService.registerUser(params)

                    if (user) {
                        //store the password
                        try {
                            passwordService.resetPassword(user, params.password, true, null)
                            //store the password
                            userService.sendAccountActivation(user)
                            redirect(action: 'accountCreated', id: user.id)
                        } catch (e) {
                            log.error("Couldn't reset password", e)
                            render(view: "accountError", model: [msg: "Failed to reset password"])
                        }
                    } else {
                        log.error('Couldn\'t create user')
                        render(view: "accountError", model: [msg: 'Couldn\'t create user'])
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                    render(view: "accountError", model: [msg: e.getMessage()])
                }
            }
        }.invalidToken {
            redirect action: 'createAccount'
        }
    }

    def accountCreated() {
        def user= userService.getUserById(params.id)
        render(view: 'accountCreated', model: [user: user])
    }

    def forgottenPassword() {
        def currentUser = userService.getCurrentUser()
        render(view: 'forgottenPassword', model: [currentUser: currentUser])
    }

    def activateAccount() {
        def user= userService.getUserById(params.userId)
        boolean isSuccess = userService.activateAccount(user, params)

        if (isSuccess) {
            render(view: 'accountActivatedSuccessful', model: [user: user])
        } else {
            render(view: "accountError")
        }
    }

    def countries() {
        Map locations = locationService.getStatesAndCountries()
        respond locations.countries
    }

    def states(String country) {
        Map locations = locationService.getStatesAndCountries()
        if (country)
            respond locations.states[country] ?: []
        else
            respond locations.states
    }

    def getSecretForMfa() {
        def mfaResponse
        try {
            mfaResponse = [success: true, code: userService.getSecretForMfa()]
        } catch (e) {
            mfaResponse = [success: false, error: e.message]
        }
        render(mfaResponse as JSON)
    }

    def verifyAndActivateMfa() {
        try  {
            def success = userService.verifyUserCode(params.userCode)
            if (success) {
                userService.enableMfa(params.userId, true)
                render([success: true] as JSON)
            }
            else {
                render([success: false] as JSON)
            }
        } catch (e) {
            def result = [success: false, error: e.message]
            render result as JSON
        }

    }

    def disableMfa() {
        userService.enableMfa(params.userId, false)
        redirect(action: 'editAccount')
    }

    private String buildErrorMessages(RuleResult validationResult, Errors errors = null) {
        if (validationResult.valid) {
            return null
        }
        def results = []
        if (!validationResult.valid) {
            def details = validationResult.details
            for (def detail in details) {
                for (String errorCode in detail.errorCodes) {
                    def fullErrorCode = "user.password.error.${errorCode?.toLowerCase()}"
                    def errorValues = detail.values as Object[]
                    if (errors) {
                        errors.rejectValue('password', fullErrorCode, errorValues, "Invalid password.")
                    }
                    results.add(messageSource.getMessage(fullErrorCode, errorValues, "Invalid password.", LocaleContextHolder.locale))
                }
            }
        }
        return results.unique().sort().join(' ')
    }
}
