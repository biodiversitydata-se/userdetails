package au.org.ala.userdetails

import au.org.ala.users.AuthorisedSystem
import au.org.ala.ws.security.JwtProperties
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.pac4j.core.config.Config
import au.org.ala.ws.security.client.AlaAuthClient
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

class AuthorisedSystemServiceSpec extends Specification implements ServiceUnitTest<AuthorisedSystemService>, DataTest {

    void setupSpec() {
//        mockDomains(AuthorisedSystem)
    }

    def setup() {
        defineBeans {
            authorisedSystemRepository(InstanceFactoryBean, Mock(IAuthorisedSystemRepository), IAuthorisedSystemRepository)
            pac4jConfig(InstanceFactoryBean, Stub(Config), Config)
            alaAuthClient(InstanceFactoryBean, Stub(AlaAuthClient), AlaAuthClient)
            jwtProperties(JwtProperties) {
                enabled = true
                fallbackToLegacyBehaviour = true
            }
        }
    }

    def "test isAuthorisedRequest legacy"(String remoteAddr, boolean result) {
        given:
        service.jwtProperties.enabled = false
        service.config = null
        service.alaAuthClient = null
        def request = new MockHttpServletRequest("GET", "/userdetails/getUserDetails")
        request.remoteAddr = remoteAddr
        request.remoteHost = 'example.org'
        def response = new MockHttpServletResponse()
        when:
        def authorised = service.isAuthorisedRequest(request, response, null, null)
        then:
        1 * service.authorisedSystemRepository.findByHost(remoteAddr) >> result
        authorised == result

        where:
        remoteAddr | result
        '123.123.123.124' | false
        '123.123.123.123' | true
    }

    def "test isAuthorisedRequest legacy fallback"(String remoteAddr, boolean result) {
        given:
        def request = new MockHttpServletRequest("GET", "/userdetails/getUserDetails")
        request.remoteAddr = remoteAddr
        request.remoteHost = 'example.org'
        def response = new MockHttpServletResponse()
        when:
        def authorised = service.isAuthorisedRequest(request, response, null, null)
        then:
        1 * service.authorisedSystemRepository.findByHost(remoteAddr) >> result
        authorised == result

        where:
        remoteAddr | result
        '123.123.123.124' | false
        '123.123.123.123' | true
    }
}
