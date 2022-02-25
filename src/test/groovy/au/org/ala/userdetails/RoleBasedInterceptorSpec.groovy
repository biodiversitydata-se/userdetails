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

import grails.testing.gorm.DataTest
import grails.testing.web.interceptor.InterceptorUnitTest
import org.apache.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes

/**
 * See the API for {@link grails.test.mixin.web.InterceptorUnitTestMixin} for usage instructions
 */
//@TestFor(RoleBasedInterceptor)
//@TestMixin([InterceptorUnitTestMixin, GrailsUnitTestMixin])
//@Mock([AuthorisedSystemService, User, Role, UserRole, UserProperty])
class RoleBasedInterceptorSpec extends UserDetailsSpec implements InterceptorUnitTest<RoleBasedInterceptor>, DataTest {

    def controller
    private User user

    def setupSpec() {
        mockDomains(User, Role, UserRole, UserProperty)
//        mockDataService(AuthorisedSystemService)
    }

    def setup() {
        controller = new UserRoleController()
        grailsApplication.addArtefact("Controller", UserRoleController)
        user = createUser()
        interceptor.authorisedSystemService = Stub(AuthorisedSystemService)
    }

    void "Unauthorised users should not be able to access the user role UI"() {

        when:
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'userRole')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'list')
        withRequest(controller: 'userRole', action: 'list')

        then:
        interceptor.before() == false
        response.status == HttpStatus.SC_MOVED_TEMPORARILY // Redirect to CAS
    }

    void "Unauthorized systems should not be able to access the user role web service"() {

        setup:
        interceptor.authorisedSystemService.isAuthorisedSystem(_) >> false
        response.format = 'json'

        when:
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'userRole')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'list')
        withRequest(controller: 'userRole', action: 'list')

        then:
        interceptor.before() == false
        response.status == HttpStatus.SC_UNAUTHORIZED
    }

    void "ALA_ADMIN users should be able to access the user role UI"() {

        setup:
        request.addUserRole("ROLE_ADMIN")

        when:
        def model
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'userRole')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'list')
        withRequest(action: 'list')

        then:
        interceptor.before() == true
        response.status == HttpStatus.SC_OK
    }



    void "Authorized systems should be able to access the user role web service"() {

        setup:
        registerMarshallers()
        interceptor.authorisedSystemService.isAuthorisedSystem(_) >> true
        request.format = 'json'
        response.format = 'json'

        when:
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'userRole')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'list')
        withRequest(controller: 'userRole', action: 'list')

        then:
        interceptor.before() == true
        response.status == HttpStatus.SC_OK

    }
}
