package au.org.ala.userdetails

import au.org.ala.auth.UpdateCognitoPasswordCommand
import au.org.ala.auth.UpdatePasswordCommand
import au.org.ala.recaptcha.RecaptchaClient
import au.org.ala.users.UserRecord
import org.passay.RuleResult
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.validation.Errors

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED

class PasswordController {

    @Qualifier('userService')
    IUserService userService

    PasswordService passwordService
    RecaptchaClient recaptchaClient
/*
    def updatePassword() {

        UserRecord user = userService.currentUser

        if (!user) {
            log.info('my-profile without a user?')
            render(status: SC_UNAUTHORIZED)
            return
        }

        String currentPassword = params.currentPassword
        String password = params.password
        String reenteredPassword = params.reenteredPassword

        def validationResult = passwordService.validatePassword(user.userName, password)
        buildErrorMessages(validationResult, cmd.errors)

        if (cmd.hasErrors()) {
            render(view: 'updatePassword', model: [user: user, errors: cmd.errors, passwordMatchFail: true, passwordPolicy: passwordService.buildPasswordPolicy()])
        }

        render(view: 'updatePassword', model: [ user: user, passwordPolicy: passwordService.buildPasswordPolicy() ])
    }
*/
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
        def user = userService.getUserByEmail(params.email)
        if (user) {
            try {
                passwordService.resetAndSendTemporaryPassword(user, null, null, null, null)
                render(view: 'passwordReset', model: [ email: params.email, passwordPolicy: passwordService.buildPasswordPolicy() ])
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

    def forgottenPassword() {

        def currentUser = userService.getCurrentUser()
        render(view: 'forgottenPassword', model: [ currentUser: currentUser ])
    }

    def passwordReset() {

        UserRecord user = userService.getUserById(params.userId)

        if (!user) {
            render(view: 'accountError', model: [ msg: "UserRecord not found with ID ${params.userId}"])
        } else if (user.tempAuthKey == params.authKey) {
            // keys match, so lets reset password
            render(view: 'passwordReset', model: [ user: user, authKey: params.authKey, passwordPolicy: passwordService.buildPasswordPolicy() ])
        } else {
            render(view: 'authKeyExpired')
        }
    }

//    def updatePassword(UpdatePasswordCommand cmd) {
    def resetPassword(UpdatePasswordCommand cmd) {

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
            render(view: 'passwordResetCognito', model: [ email: cmd.email, code: cmd.code, errors:cmd.errors, passwordMatchFail: true, passwordPolicy: passwordService.buildPasswordPolicy() ])
        }
        else {
            withForm {
                if (!user) {
                    log.error "Invalid email ${cmd.email}"
                    render(view: 'accountError', model: [msg: "Invalid email ${cmd.email}"])
                }
                // update the password
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
