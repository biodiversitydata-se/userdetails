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

import au.org.ala.ws.security.JwtProperties
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.ProfileManager
import org.pac4j.core.profile.UserProfile
import org.pac4j.core.util.FindBest
import au.org.ala.ws.security.client.AlaAuthClient
import org.pac4j.jee.context.JEEContextFactory
import org.pac4j.oidc.credentials.OidcCredentials
import org.springframework.beans.factory.annotation.Autowired

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthorisedSystemService {

    @Autowired
    JwtProperties jwtProperties
    @Autowired(required = false)
    Config config
    @Autowired(required = false)
    AlaAuthClient alaAuthClient
    @Autowired
    IAuthorisedSystemRepository authorisedSystemRepository

    def isAuthorisedSystem(HttpServletRequest request){
        def host = request.getRemoteAddr()
        log.debug("RemoteHost: " + request.getRemoteHost())
        log.debug("RemoteAddr: " + request.getRemoteAddr())
        log.debug("host using: " + host)

        return authorisedSystemRepository.findByHost(host)
//        return host != null && authorisedSystemRepository.findByHost(host)
    }

    /**
     * Validate a JWT Bearer token instead of the API key.
     * @param fallbackToLegacy Whether to fall back to legacy authorised systems if the JWT is not present.
     * @param role The user role required to continue
     * @param scope The JWT scope required for the request to be authorized
     * @return true
     */
    def isAuthorisedRequest(HttpServletRequest request, HttpServletResponse response, String role, String scope) {
        def result = false

        if (jwtProperties.enabled) {
            def context = context(request, response)
            ProfileManager profileManager = new ProfileManager(context, config.sessionStore)
            profileManager.setConfig(config)

//            def credentials = alaAuthClient.getCredentials(context, config.sessionStore)
            result = alaAuthClient.getCredentials(context, config.sessionStore).map { credentials ->
                boolean matchesScope
                if (scope) {

                    if (credentials instanceof OidcCredentials) {

                        OidcCredentials oidcCredentials = credentials

                        matchesScope = oidcCredentials.accessToken.scope.contains(scope)

                        if (!matchesScope) {
                            log.debug "access_token scopes '${oidcCredentials.accessToken.scope}' is missing required scopes ${scope}"
                        }
                    } else {
                        matchesScope = false
                        log.debug("$credentials are not OidcCredentials, so can't get access_token")
                    }
                } else {
                    matchesScope = true
                }

                boolean matchesRole
                if (role) {

                    matchesRole = alaAuthClient.getUserProfile(credentials, context, config.sessionStore).map { userProfile ->

                        profileManager.save(
                                alaAuthClient.getSaveProfileInSession(context, userProfile),
                                userProfile,
                                alaAuthClient.isMultiProfile(context, userProfile)
                        )

                        def userProfileContainsRole = userProfile.roles.contains(role)

                        if (!userProfileContainsRole) {
                            log.debug "user profile roles '${userProfile.roles}' is missing required role ${role}"
                        }
                        return userProfileContainsRole
                    }.orElseGet {
                        log.debug "rejecting request because role $role is required but no user profile is available"
                        false
                    }
                } else {
                    matchesRole = true
                }

                return matchesScope && matchesRole
            }.orElseGet {
                log.debug "no access token present"
                jwtProperties.fallbackToLegacyBehaviour && isAuthorisedSystem(request)
            }
        } else {
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
