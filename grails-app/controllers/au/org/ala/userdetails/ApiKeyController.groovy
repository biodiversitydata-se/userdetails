package au.org.ala.userdetails

import grails.converters.JSON
import org.apache.http.HttpHeaders
import org.springframework.beans.factory.annotation.Value

class ApiKeyController {

    public static final String X_API_KEY_HEADER = "x-api-key"
    public static final String X_API_SECRET = "x-api-secret"
    def userService
    def emailService
    def apiKeyService

    @Value('${apiKey.secretRegenerationTimeOutInMins}')
    Integer secretRegenerationTimeOutInMins

    def index() {

        // show the API key for a user
        def user = userService.currentUser
        if (user) {
            ApiKey apiKey = apiKeyService.getApiKeyForUser(user)
            use (groovy.time.TimeCategory) {
                def props = user.propsAsMap()
                boolean generateLinkEnabled = apiKey.apiSecret == null || apiKey.lastUpdated < (new Date() - secretRegenerationTimeOutInMins.minutes)
                def regenTime = apiKey.lastUpdated + secretRegenerationTimeOutInMins.minutes - new Date()
                render(view: 'show', model: [user: user, props: props, apiKey: apiKey.apiKey,
                                             generateLinkEnabled: generateLinkEnabled,
                                             regenTime: regenTime
                ])
            }
        } else {
            String baseUrl = grailsApplication.config.security.cas.loginUrl
            def separator = baseUrl.contains("?") ? "&" : "?"
            def loginUrl = "${baseUrl}${separator}service=" + URLEncoder.encode(emailService.getMyProfileUrl(), "UTF-8")
            redirect(url: loginUrl)
        }
    }

    def secret(){

        // generate secret, persist and display to user
        // show the API key for a user
        def user = userService.currentUser

        if (user) {
            def props = user.propsAsMap()
            // generate the secret, encrypt, persist it, render it
            ApiKey apiKey = apiKeyService.getApiKeyForUser(user)
            if (!apiKey){
                render ([response:'Regenerated recently....'] as JSON)
            } else {
                use (groovy.time.TimeCategory) {
                    if (apiKey.apiSecret && apiKey.lastUpdated > (new Date() - secretRegenerationTimeOutInMins.minutes)) {

                        def difference = apiKey.lastUpdated + secretRegenerationTimeOutInMins.minutes - new Date()
                        render(view: 'secret', model: [
                                user: user,
                                props: props,
                                regenSuccess: false,
                                lastUpdated : apiKey.lastUpdated,
                                regenOkTime : difference.minutes]
                        )
                        render ([response:'Regenerated recently....'] as JSON)
                    } else {
                        String apikeySecret = apiKeyService.resetSecretForApiKey(apiKey)
                        render(view: 'secret', model: [
                                user: user,
                                props: props,
                                regenSuccess: true,
                                apiKey: apiKey.apiKey,
                                apikeySecret: apikeySecret]
                        )

                        render ([response:apikeySecret] as JSON)
                    }
                }
            }
        } else {
            render ([response:'Regenerated recently....'] as JSON)
        }
    }

    def jwt() {

        Map result = [:]

        def authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authorizationHeader){
            result = apiKeyService.generateTokenBasicAuth(authorizationHeader)
        } else {
            def apiKey = request.getHeader(X_API_KEY_HEADER)
            def apiKeySecret = request.getHeader(X_API_SECRET)
            if (apiKey && apiKeySecret) {
                result = apiKeyService.generateTokenApiKeySecret(apiKey, apiKeySecret)
            }
        }
        if (result) {
            if (result.jwt) {
                render(["token": result.jwt] as JSON)
            } else {
                response.sendError(result.statusCode, "Authentication not successful")
            }
        } else {
            response.sendError(500, "Unable to create JWT")
        }
    }
}
