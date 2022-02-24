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

import grails.converters.JSON

class ExternalSiteController {

    def userService

    def index() {}

    def flickr() {

        def flickrIds = UserProperty.findAllByName("flickrId")
        render(contentType: "application/json") {
            flickrUsers(flickrIds) { UserProperty flickrId ->
                id flickrId.user.id.toString()
                externalId flickrId.value
                externalUsername flickrId.user.propsAsMap().flickrUsername
                externalUrl 'http://www.flickr.com/photos/' + flickrId.value
            }
        }
    }

    def getUserStats() {
        def stats = userService.getUsersCounts(request.locale)
        render(stats as JSON, contentType: "application/json")  // getUsersCounts is cached
    }
}
