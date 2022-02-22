package au.org.ala.userdetails

import au.org.ala.ws.security.JwtAuthenticator
import grails.converters.JSON
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.pac4j.core.config.Config

/**
 * Specification for the UserRoleController
 */
//@TestFor(UserRoleController)
//@TestMixin(InterceptorUnitTestMixin)
//@Mock([AuthorisedSystemService, User, Role, UserRole, UserProperty])
class UserRoleControllerSpec extends UserDetailsSpec implements ControllerUnitTest<UserRoleController>, DataTest {

    private User user

    void setupSpec() {
        mockDomains(User, Role, UserRole, UserProperty)
    }

    void setup() {
        defineBeans {
            config(InstanceFactoryBean, Stub(Config), Config)
            jwtAuthenticator(InstanceFactoryBean, Stub(JwtAuthenticator), JwtAuthenticator)
            authorisedSystemService(UserDetailsSpec.UnAuthorised)
        }
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