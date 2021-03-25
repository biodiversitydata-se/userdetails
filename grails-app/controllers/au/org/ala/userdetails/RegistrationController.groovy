package au.org.ala.userdetails

import au.org.ala.auth.UpdatePasswordCommand
import au.org.ala.recaptcha.RecaptchaClient
import grails.converters.JSON
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

import javax.annotation.PostConstruct

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
    def userService
    def locationService
    def cookieService
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
        User user = User.get(params.userId?.toLong())
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
        User user = User.get(cmd.userId)
        if (cmd.hasErrors()) {
            render(view: 'passwordReset', model: [user: user, authKey: cmd.authKey, errors:cmd.errors, passwordMatchFail: true])
        }
        else {
            withForm {
                if (user.tempAuthKey == params.authKey) {
                    //update the password
                    try {
                        passwordService.resetPassword(user, cmd.password)
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

    /**
     * Used only to display the view.
     * This is required given that the view is not rendered directly by #disableAccount but rather a chain
     * of redirects:  userdetails logout, cas logout and finally this view
     */
    def accountDisabled() {
    }

    /** Displayed as a result of a password update with a duplicate form submission. */
    def duplicateSubmit() {
        [serverUrl: grailsApplication.config.grails.serverURL + '/myprofile']
    }

    def passwordResetSuccess() {
        [serverUrl: grailsApplication.config.grails.serverURL + '/myprofile']
    }

    def startPasswordReset() {
        //check for human
        boolean captchaValid = simpleCaptchaService.validateCaptcha(params.captcha)
        if (!captchaValid) {
            //send password reset link
            render(view: 'forgottenPassword', model: [email: params.email, captchaInvalid: true])
        } else {
            log.info("Starting password reset for email address: " + params.email)
            def user = User.findByEmail(params.email)
            if (user) {
                try {
                    userService.resetAndSendTemporaryPassword(user, null, null, null)
                    render(view: 'startPasswordReset', model: [email: params.email])
                } catch (Exception e) {
                    log.error("Problem starting password reset for email address: " + params.email)
                    log.error(e.getMessage(), e)
                    render(view: 'accountError', model: [msg: e.getMessage()])
                }
            } else {
                //invalid email address entered
                log.warn("email address {} is not recognised.", params.email)
            }
        }
    }

    def disableAccount() {
        def user = userService.currentUser

        log.debug("Disabling account for " + user)
        if (user) {
            def success = userService.disableUser(user)

            if (success) {
                redirect(controller: 'logout', action: 'logout', params: [casUrl: grailsApplication.config.security.cas.logoutUrl,
                                                                          appUrl: grailsApplication.config.grails.serverURL + '/registration/accountDisabled'])
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
        boolean emailChanged = false

        if (user) {
            if (params.email != user.email) {
                // email address has changed
                if (userService.isEmailInUse(params.email, user)) {
                    render(view: "accountError", model: [msg: "Failed to update user profile - A user is already registered with the email address"])
                    return
                }
                // and username and email address must be kept in sync
                params.userName = params.email
                emailChanged = true
            }

            def success = userService.updateUser(user, params)
            if (success) {
                if (emailChanged){
                    // update cookie when email address changed https://github.com/AtlasOfLivingAustralia/userdetails/issues/98
                    // cookie config values need to be consistent with values in CAS
                    def cookieName = grailsApplication.config.getProperty('ala.cookie.name', String, null)
                    if (cookieName && cookieService.findCookie(cookieName)) {
                        cookieService.setCookie(cookieName, quoteValue(encodeValue(params.email)), grailsApplication.config.getProperty('ala.cookie.maxAge', Integer, -1),
                                grailsApplication.config.getProperty('ala.cookie.path', String, '/'),
                                grailsApplication.config.getProperty('ala.cookie.domain', String, 'ala.org.au'),
                                grailsApplication.config.getProperty('ala.cookie.secure', Boolean, false),
                                grailsApplication.config.getProperty('ala.cookie.httpOnly', Boolean, true))
                        log.info("New cookie set for user: {}, cookie name: {}, cookie value: {}", user.id, cookieName , quoteValue(encodeValue(params.email)))
                    }
                }
                redirect(controller: 'profile')
                log.info("Account details updated for user: " + user.id + " username: " + user.userName)
            } else {
                render(view: "accountError", model: [msg: "Failed to update user profile - unknown error"])
            }
        } else {
            render(view: "accountError", model: [msg: "The current user details could not be found"])
        }
    }

    String quoteValue(String value) {
        if (grailsApplication.config.getProperty('ala.cookie.quoteCookieValue', Boolean, true)) {
            "\"$value\""
        } else {
            value
        }
    }

    String encodeValue(String value) {
        if (grailsApplication.config.getProperty('ala.cookie.encodeCookieValue', Boolean, false)) {
            URLEncoder.encode(value, "UTF-8")
        } else {
            value
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
                render(view: 'createAccount', model: [edit: false, user: params, props: params, alreadyRegistered: true, inactiveUser: inactiveUser])
            } else {

                try {
                    //does a user with the supplied email address exist
                    def user = userService.registerUser(params)

                    //store the password
                    try {
                        passwordService.resetPassword(user, params.password)
                        //store the password
                        emailService.sendAccountActivation(user, user.tempAuthKey)
                        redirect(action: 'accountCreated', id: user.id)
                    } catch (e) {
                        log.error("Couldn't reset password", e)
                        render(view: "accountError", model: [msg: "Failed to reset password"])
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                    render(view: "accountError", model: [msg: e.getMessage()])
                }
            }
        }
    }

    def accountCreated() {
        def user = User.get(params.id)
        render(view: 'accountCreated', model: [user: user])
    }

    def forgottenPassword() {}

    def activateAccount() {
        def user = User.get(params.userId)
        //check the activation key
        if (user.tempAuthKey == params.authKey) {
            userService.activateAccount(user)
            render(view: 'accountActivatedSuccessful', model: [user: user])
        } else {
            log.error('Auth keys did not match for user : ' + params.userId + ", supplied: " + params.authKey + ", stored: " + user.tempAuthKey)
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
}
