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
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus

/**
 * Specification for the UserRoleController
 */
//@TestFor(UserRoleController)
//@TestMixin(InterceptorUnitTestMixin)
//@Mock([AuthorisedSystemService, User, Role, UserRole, UserProperty])
class UserRoleControllerSpec extends UserDetailsSpec implements ControllerUnitTest<UserRoleController>, DataTest {

    Closure doWithSpring(){{ ->
        authorisedSystemService(UserDetailsSpec.UnAuthorised)
    }}

    private User user

    void setupSpec() {
        mockDomains(User, Role, UserRole, UserProperty)
    }

    void setup() {
        user = createUser()
    }

    void "user role list should return a model when HTML requested"() {

        setup:
        request.addUserRole("ROLE_ADMIN")

        when:
        def model = controller.list()
        then:
        response.status == HttpStatus.SC_OK
        model.userRoleInstanceTotal == 1
    }

    void "user role web service should return valid JSON"() {

        setup:
        registerMarshallers()
        request.format = 'json'
        response.format = 'json'

        when:
        controller.list()

        then:

        response.status == HttpStatus.SC_OK
        Map deserializedJson = JSON.parse(response.text)
        deserializedJson.count == 1
        deserializedJson.users[0].userId == Long.toString(user.id)
    }
}