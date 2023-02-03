package au.org.ala.userdetails

import au.org.ala.users.AuthorisedSystem
import grails.core.GrailsApplication
import grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.dao.DataIntegrityViolationException

class ConfigAuthorisedSystemRepository implements IAuthorisedSystemRepository {

    GrailsApplication grailsApplication

    @Override
    Boolean findByHost(String host) {
        return grailsApplication.config.getProperty('authorised.systems', List<String>, []).contains(host)
    }

    @Override
    def list(GrailsParameterMap params) {
        def list = []
        def count = 0
        def query = params.q as String
        if (query) {

        } else {

        }

        [authorisedSystemInstanceList: list, authorisedSystemInstanceTotal: count]
    }

    @Override
    def save(GrailsParameterMap params) {
        def authorisedSystemInstance = new AuthorisedSystem(params)

        return authorisedSystemInstance
    }

    @Override
    def get(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        return authorisedSystemInstance
    }


    @Override
    def update(GrailsParameterMap params) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        return authorisedSystemInstance
    }

    @Override
    def delete(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
    }

    @Override
    def ajaxResolveHostName(GrailsParameterMap params) {

        return [host:host, hostname: hostname, reachable: reachable]
    }
}
