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

import au.org.ala.oauth.apis.InaturalistApi
import au.org.ala.users.UserPropertyRecord
import au.org.ala.users.UserRecord
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.apis.FlickrApi
import com.github.scribejava.core.exceptions.OAuthException
import com.github.scribejava.core.model.*
import com.github.scribejava.core.oauth.OAuth20Service
import com.github.scribejava.core.oauth.OAuthService
import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import uk.co.desirableobjects.oauth.scribe.OauthProvider

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED

class ProfileController {

    static final String FLICKR_ID = 'flickrId'
    static final String FLICKR_USERNAME = 'flickrUsername'
    static final String INATURALIST_TOKEN = 'inaturalistToken'
    static final String INATURALIST_ID = 'inaturalistId'
    static final String INATURALIST_USERNAME = 'inaturalistUsername'

    static final List<String> FLICKR_ATTRS = [FLICKR_ID, FLICKR_USERNAME]
    static final List<String> INATURALIST_ATTRS = [INATURALIST_TOKEN, INATURALIST_ID, INATURALIST_USERNAME]

    def oauthService

    @Autowired
    @Qualifier('userService')
    IUserService userService

    def index() {

        def user = userService.currentUser

        if (user) {
            def props = user.propsAsMap()
            def isAdmin = request.isUserInRole("ROLE_ADMIN")
            render(view: "myprofile", model: [user: user, props: props, isAdmin: isAdmin])
        } else {
            log.info('my-profile without a user?')
            render(status: SC_UNAUTHORIZED)
        }
    }

    def inaturalistCallback() {

        // TODO use the OAuthController callback action and success redirect?
        String providerName = 'inaturalist'
        OauthProvider provider = oauthService.findProviderConfiguration(providerName)

        String code = params['code']

        if (!code) {
            redirect(uri: provider.failureUri)
            return
        }

        def service = (OAuth20Service) provider.service

        OAuth2AccessToken accessToken
        try {
            accessToken = service.getAccessToken(code)
        } catch(OAuthException ex) {
            log.error("Cannot authenticate with oauth", ex)
            return redirect(uri: provider.failureUri)
        }

        session[oauthService.findSessionKeyForAccessToken(providerName)] = accessToken
        session.removeAttribute(oauthService.findSessionKeyForRequestToken(providerName))

        OAuthRequest request = new OAuthRequest(Verb.GET, "${InaturalistApi.baseUrl}users/edit")
        request.addHeader('Accept', 'application/json')
        service.signRequest(accessToken, request)

        Response response = service.execute(request)

        if (!response.isSuccessful()) {
            log.error("Got error response calling iNaturalist user API: {}, body: {}", response.code, response.body)
            return redirect(uri: provider.failureUri)
        }
        def body = response.body
        def inaturalistUser = JSON.parse(body)

        UserRecord user = userService.currentUser

        if (user) {
            if (accessToken.expiresIn == null) {
                userService.addOrUpdateProperty(user, INATURALIST_TOKEN, accessToken.accessToken)
            }
            userService.addOrUpdateProperty(user, INATURALIST_ID, inaturalistUser.id)
            userService.addOrUpdateProperty(user, INATURALIST_USERNAME, inaturalistUser.login)
        } else {
            flash.message = "Failed to retrieve user details!"
        }

        redirect(controller: 'profile')
    }

    def flickrCallback() {

        FlickrApi flickrApi = FlickrApi.instance()
        OAuth1RequestToken token = session.getAt("flickr:oasRequestToken")
        OAuthService service = new ServiceBuilder().
                apiKey(grailsApplication.config.getProperty('oauth.providers.flickr.key')).
                apiSecret(grailsApplication.config.getProperty('oauth.providers.flickr.secret')).build(flickrApi)

        def accessToken = service.getAccessToken(token, params.oauth_verifier)

        // Now let's go and ask for a protected resource!
        OAuthRequest request = new OAuthRequest(Verb.GET, flickrApi.accessTokenEndpoint)
        service.signRequest(accessToken, request)
        Response response = service.execute(request)
        if (response.code == 301) {
            request = new OAuthRequest(Verb.GET, response.headers['Location'])
            response = service.execute(request)
        }
        def model = [:]
        def body = response.body
        body.split("&").each {
            def property = it.substring(0, it.indexOf("="))
            def value = it.substring(it.indexOf("=") + 1)
            model.put(property, value)
        }

        //store the user's flickr ID.
        UserRecord user = userService.currentUser

        if (user) {
            //store flickrID & flickrUsername
            userService.addOrUpdateProperty(user, FLICKR_ID, URLDecoder.decode(model.get("user_nsid"), "UTF-8"))
            userService.addOrUpdateProperty(user, FLICKR_USERNAME, model.get("username"))
        } else {
            flash.message = "Failed to retrieve user details!"
        }

        redirect(controller: 'profile')
    }

    def flickrSuccess() {}

    def flickrFail() {}

    def inaturalistFail() {}

    def removeLink() {
        String provider = params['provider']
        UserRecord user = userService.currentUser
        List<String> attrs
        switch (provider) {
            case 'flickr': attrs = FLICKR_ATTRS
                break
            case 'inaturalist': attrs = INATURALIST_ATTRS
                break
            default:
                flash.message = "Provider $provider not found!"
                return redirect(controller: 'profile')
        }

        if (user) {
            userService.removeUserAttributes(user, attrs)
        } else {
            flash.message = 'Failed to retrieve user details!'
        }
        redirect(controller: 'profile')
    }
}
