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
import au.org.ala.users.User
import grails.converters.JSON
import org.springframework.beans.factory.annotation.Qualifier

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
    def authService
    def passwordService

    @Qualifier('userService')
    IUserService userService
    def locationService
    RecaptchaClient recaptchaClient


    def index() {
        redirect(action: 'createAccount')
    }

    def createAccount() {}

    def editAccount() {
        def user = userService.currentUser
        render(view: 'createAccount', model: [edit: true, user: user, props: user?.propsAsMap()])
    }

    def passwordReset() {
        User user = userService.getUserById(params.userId)
        if (!user) {
            render(view: 'accountError', model: [msg: "User not found with ID ${params.userId}"])
        } else if (user.tempAuthKey == params.authKey) {
            //keys match, so lets reset password
            render(view: 'passwordReset', model: [user: user, authKey: params.authKey])
        } else {
            render(view: 'authKeyExpired')
        }
    }

    def updatePassword(UpdatePasswordCommand cmd) {
        User user = userService.getUserById(cmd.userId as String)
        if (cmd.hasErrors()) {
            render(view: 'passwordReset', model: [user: user, authKey: cmd.authKey, errors:cmd.errors, passwordMatchFail: true])
        }
        else {
            withForm {
                if (user.tempAuthKey == params.authKey) {
                    //update the password
                    try {
                        userService.resetPassword(user, cmd.password, true)
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
        User user = userService.getUserByEmail(cmd.email)
        if (cmd.hasErrors()) {
            render(view: 'passwordResetCognito', model: [email: cmd.email, code: cmd.code, errors:cmd.errors, passwordMatchFail: true])
        }
        else {
            withForm {
                if(!user) {
                    log.error "Invalid email ${cmd.email}"
                    render(view: 'accountError', model: [msg: "Invalid email ${cmd.email}"])
                }
                //update the password
                try {
                    userService.resetPassword(user, cmd.password, true)
                    //userService.clearTempAuthKey(user) //TODO do we need this?
                    redirect(controller: 'registration', action: 'passwordResetSuccess')
                    log.info("Password successfully reset for user: " + cmd.email)
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
        boolean captchaValid = simpleCaptchaService.validateCaptcha(params.captcha)
        if (!captchaValid) {
            //send password reset link
            render(view: 'forgottenPassword', model: [email: params.email, captchaInvalid: true])
        } else {
            log.info("Starting password reset for email address: " + params.email)
            def user = userService.getUserById(params.email)
            if (user) {
                try {
                    userService.resetAndSendTemporaryPassword(user, null, null, null, null)
                    render(view: userService.getPasswordResetView(), model: [email: params.email])
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

            def success = userService.updateUser(authService.userId, params)

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
                        render(view: 'createAccount', model: [edit: false, user: params, props: params])
                        return
                    }
                } else {
                    log.warn("error from recaptcha {}", response)
                    flash.message = 'There was an error with the captcha, please try again'
                    render(view: 'createAccount', model: [edit: false, user: params, props: params])
                    return
                }
            }

            //create user account...
            if (!params.email || userService.isEmailRegistered(params.email)) {
                def inactiveUser = !userService.isActive(params.email)
                def lockedUser = userService.isLocked(params.email)
                render(view: 'createAccount', model: [edit: false, user: params, props: params, alreadyRegistered: true, inactiveUser: inactiveUser, lockedUser: lockedUser])
            } else {

                try {
                    //does a user with the supplied email address exist
                    def user = userService.registerUser(params)

                    if(user) {
                        //store the password
                        try {
                            userService.resetPassword(user, params.password, true)
                            //store the password
                            userService.sendAccountActivation(user)
                            redirect(action: 'accountCreated', id: user.id)
                        } catch (e) {
                            log.error("Couldn't reset password", e)
                            render(view: "accountError", model: [msg: "Failed to reset password"])
                        }
                    }
                    else{
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

    def forgottenPassword() {}

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
        def response = userService.getSecretForMfa(session)
        render(response as JSON)
    }

    def verifyAndActivateMfa() {
        def response = userService.verifyUserCode(session, params.userCode)
        if(response.success) {
            def mfaResponse = userService.enableMfa(params.userId, true)
            render(mfaResponse as JSON)
        }
        else {
            render(response as JSON)
        }
    }

    def disableMfa() {
        userService.enableMfa(params.userId, false)
        redirect(action: 'editAccount')
    }
}
