package au.org.ala.userdetails

import au.org.ala.auth.PreAuthorise
import au.org.ala.ws.security.JwtAuthenticator
import org.pac4j.core.config.Config
import org.pac4j.core.context.JEEContextFactory
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.extractor.BearerAuthExtractor
import org.pac4j.core.profile.ProfileManager
import org.pac4j.core.profile.UserProfile
import org.pac4j.core.util.FindBest
import org.springframework.beans.factory.annotation.Autowired

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthorisedSystemService {

    @Autowired
    Config config
    @Autowired
    JwtAuthenticator jwtAuthenticator

    def isAuthorisedSystem(HttpServletRequest request){
        def host = request.getRemoteAddr()
        log.debug("RemoteHost: " + request.getRemoteHost())
        log.debug("RemoteAddr: " + request.getRemoteAddr())
        log.debug("host using: " + host)

        return host != null && AuthorisedSystem.findByHost(host)
    }

    /**
     * Validate a JWT Bearer token instead of the API key.
     * @param fallbackToLegacy Whether to fall back to legacy authorised systems if the JWT is not present.
     * @param role The user role required to continue
     * @param scope The JWT scope required for the request to be authorized
     * @return true
     */
    def isAuthorisedRequest(HttpServletRequest request, HttpServletResponse response, boolean fallbackToLegacy, String role, String scope) {
        def result = false
        def context = context(request, response)
        def bearerAuthExtractor = new BearerAuthExtractor()
        def credentials = bearerAuthExtractor.extract(context, config.sessionStore)
        if (credentials.isPresent()) {
            ProfileManager profileManager = new ProfileManager(context, config.sessionStore)
            profileManager.setConfig(config)
            def creds = credentials.get()
            try {
                jwtAuthenticator.validate(creds, context, config.sessionStore)

                def userProfile = creds.userProfile

                if (userProfile) {
                    profileManager.save(false, creds.userProfile, false)
                }

                result = true
                if (role) {
                    result = userProfile.roles.contains(role)
                }

                if (result && scope) {
                    result = userProfile.permissions.contains(scope) || profileHasScope(userProfile, scope)
                }

            } catch (e) {
                if (log.isDebugEnabled()) {
                    log.debug("Couldn't validate JWT", e)
                } else {
                    log.info("Couldn't validate JWT: {}", e.message)
                }
                result = false
            }
        } else if (fallbackToLegacy) {
            result = isAuthorisedSystem(request)
        }
        return result
    }

    private boolean profileHasScope(UserProfile userProfile, String scope) {
        def scopes = userProfile.attributes['scope']
        def result = false
        if (scopes != null) {
            if (scopes instanceof String) {
                result = scopes.tokenize(',').contains(scope)
            } else if (scopes.class.isArray()) {
                result =scopes.any { it?.toString() == scope }
            } else if (scopes instanceof Collection) {
                result =scopes.any { it?.toString() == scope }
            }
        }
        return result
    }

    private WebContext context(request, response) {
        final WebContext context = FindBest.webContextFactory(null, config, JEEContextFactory.INSTANCE).newContext(request, response)
        return context
    }
}
