package au.org.ala.userdetails

import au.org.ala.auth.PasswordResetFailedException

class EmailService {

    def grailsApplication

    static transactional = false

    def sendPasswordReset(user, authKey, emailSubject = null, emailTitle = null, emailBody1 = null, emailBody2 = null, password = null)
            throws PasswordResetFailedException {

        if (!emailSubject) {
            emailSubject = "Reset your password"
        }
        if (!emailTitle) {
            emailTitle = "Reset your password"
        }

        if (!emailBody1) {
            emailBody1 = "We have received a password reset request for the account associated with {}. You can reset your password by clicking the link below.  " +
                    "This will take you to a form where you can provide a new password for your account."
        }
        if (!emailBody2) {
            emailBody2 = "If you did not request a new password, please let us know immediately by replying to this email."
        }
        try {
            sendMail {
              from grailsApplication.config.emailSenderTitle+"<" + grailsApplication.config.emailSender + ">"
              subject emailSubject
              to user.email
              body (view: '/email/resetPassword',
                    plugin:"email-confirmation",
                    model:[userName: user.firstName, link: getServerUrl() + "resetPassword/" +  user.id +  "/"  + authKey, emailTitle: emailTitle, emailBody1: emailBody1, emailBody2: emailBody2, password: password ]
              )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def sendAccountActivation(user, authKey) throws PasswordResetFailedException {
        try {
            sendMail {
                from grailsApplication.config.emailSenderTitle + "<" + grailsApplication.config.emailSender + ">"
                subject "Activate your account"
                to user.email
                body(view: '/email/activateAccount',
                        plugin: "email-confirmation",
                        model: [userName: user.firstName, link: getServerUrl() + "activateAccount/" + user.id + "/" + authKey, orgNameLong: grailsApplication.config.skin.orgNameLong]
                )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def sendAccountActivationSuccess(user, activatedAlerts) throws PasswordResetFailedException {
        try {
            sendMail {
                from grailsApplication.config.emailSenderTitle + "<" + grailsApplication.config.emailSender + ">"
                subject "Account activated successfully"
                to user.email
                body(view: '/email/activateAccountSuccess',
                        plugin: "email-confirmation",
                        model: [userName: user.firstName, activatedAlerts: activatedAlerts, alertsUrl: grailsApplication.config.alerts.url]
                )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def sendUpdateProfileSuccess(user) throws PasswordResetFailedException {
        try {
            sendMail {
                from grailsApplication.config.emailSenderTitle+"<" + grailsApplication.config.emailSender + ">"
                subject "Account updated successfully"
                to user.email
                body (view: '/email/updateAccountSuccess',
                        plugin:"email-confirmation",
                        model:[userName: user.firstName, support: grailsApplication.config.supportEmail]
                )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def sendGeneratedPassword(user, generatedPassword) throws PasswordResetFailedException {
        try {
            sendMail {
              from grailsApplication.config.emailSenderTitle+"<" + grailsApplication.config.emailSender + ">"
              subject "Accessing your account"
              to user.email
              body (view: '/email/accessAccount',
                    plugin:"email-confirmation",
                    model:[userName: user.firstName, link: getLoginUrl(user.email), generatedPassword: generatedPassword]
              )
            }
        } catch (Exception ex) {
            throw new PasswordResetFailedException(ex)
        }
    }

    def getLoginUrl(email){
            grailsApplication.config.security.cas.loginUrl  +
                    "?email=" + email +
                    "&service=" + URLEncoder.encode(getMyProfileUrl(),"UTF-8")
    }

    def getMyProfileUrl(){
            grailsApplication.config.grails.serverURL  +
                    "/myprofile/"
    }

    def getServerUrl(){
        grailsApplication.config.grails.serverURL +
                    "/registration/"
    }
}
