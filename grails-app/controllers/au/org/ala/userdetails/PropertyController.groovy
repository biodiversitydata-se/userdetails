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

class PropertyController extends BaseController {

    def profileService
    def authorisedSystemService

    static allowedMethods = [getProperty: "GET", saveProperty: "POST"]

    def index() {}

    /**
     * get a property value for a user
     * @return
     */
    def getProperty() {
        if (authorisedSystemService.isAuthorisedSystem(request)) {
            String name = params.name;
            Long alaId = Long.parseLong(params.alaId);
            if (!name || !alaId) {
                badRequest "name and alaId must be provided";
            } else {
                User user = User.findById(alaId);
                List props
                if (user) {
                    props = profileService.getUserProperty(user, name);
                    render text: props as JSON, contentType: 'application/json'
                } else {
                    notFound "Could not find user for id: ${alaId}";
                }
            }
        } else {
            response.sendError(403)
        }
    }

    /**
     * save a property value to a user
     * @return
     */
    def saveProperty(){
        if (authorisedSystemService.isAuthorisedSystem(request)) {
            String name = params.name;
            String value = params.value;
            Long alaId = Long.parseLong(params.alaId);
            if (!name || !alaId) {
                badRequest "name and alaId must be provided";
            } else {
                User user = User.findById(alaId);
                UserProperty property
                if (user) {
                    property = profileService.saveUserProperty(user, name, value);
                    if (property.hasErrors()) {
                        saveFailed()
                    } else {
                        render text: property as JSON, contentType: 'application/json'
                    }
                } else {
                    notFound "Could not find user for id: ${alaId}";
                }
            }
        } else {
            response.sendError(403)
        }
    }
}
