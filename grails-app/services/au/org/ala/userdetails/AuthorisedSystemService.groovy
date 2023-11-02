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
import org.pac4j.http.client.direct.DirectBearerAuthClient
import org.pac4j.jee.context.JEEContextFactory
import org.springframework.beans.factory.annotation.Autowired

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthorisedSystemService {

    @Autowired
    JwtProperties jwtProperties
    @Autowired(required = false)
    Config config
    @Autowired(required = false)
    DirectBearerAuthClient directBearerAuthClient

    def isAuthorisedSystem(HttpServletRequest request){
        def host = request.getHeader("x-forwarded-for")
        if(host == null){
            host = request.getRemoteAddr()
        }
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
    def isAuthorisedRequest(HttpServletRequest request, HttpServletResponse response, String role, String scope) {
        def result = false

        if (jwtProperties.enabled) {
            def context = context(request, response)
            ProfileManager profileManager = new ProfileManager(context, config.sessionStore)
            profileManager.setConfig(config)

            def credentials = directBearerAuthClient.getCredentials(context, config.sessionStore)
            if (credentials.isPresent()) {
                def profile = directBearerAuthClient.getUserProfile(credentials.get(), context, config.sessionStore)
                if (profile.isPresent()) {
                    def userProfile = profile.get()
                    profileManager.save(
                            directBearerAuthClient.getSaveProfileInSession(context, userProfile),
                            userProfile,
                            directBearerAuthClient.isMultiProfile(context, userProfile)
                    )

                    result = true
                    if (role) {
                        result = userProfile.roles.contains(role)
                    }

                    if (result && scope) {
                        result = userProfile.permissions.contains(scope) || profileHasScope(userProfile, scope)
                    }
                }
            } else if (jwtProperties.fallbackToLegacyBehaviour) {
                result = isAuthorisedSystem(request)
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
