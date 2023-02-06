package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.IAuthorisedSystemRepository
import au.org.ala.users.AuthorisedSystemRecord
import grails.converters.JSON
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.springframework.dao.DataIntegrityViolationException

@Slf4j
class GormAuthorisedSystemRepository implements IAuthorisedSystemRepository {

    @Override
    Boolean findByHost(String host) {
        return AuthorisedSystem.findByHost(host) != null
    }

    @Override
    def list(GrailsParameterMap params) {
        def list = []
        def count = 0
        def query = params.q as String
        if (query) {
            def c = AuthorisedSystem.createCriteria()
            list = c.list(params) {
                or {
                    ilike('host', "%${query}%")
                    ilike('description', "%${query}%")
                }
            }
            count = list.totalCount
        } else {
            list = AuthorisedSystem.list(params)
            count = AuthorisedSystem.count()
        }

        return [list: list, count: count]
    }

    @Override
    AuthorisedSystemRecord save(GrailsParameterMap params) {
        def authorisedSystemInstance = new AuthorisedSystem(host: params.host, description: params.description)
        if (!authorisedSystemInstance.save(flush: true)) {
            return null
        }
        return authorisedSystemInstance
    }

    @Override
    AuthorisedSystemRecord get(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        return authorisedSystemInstance
    }

    @Override
    AuthorisedSystemRecord update(GrailsParameterMap params) {
        def authorisedSystemInstance = AuthorisedSystem.get(params.id as Long)
        if (!authorisedSystemInstance) {
            return null
        }

        if (params.version != null) {
            if (authorisedSystemInstance.version > params.version as Long) {
                log.error("Another user has updated this AuthorisedSystem while you were editing")
                return null
            }
        }

        authorisedSystemInstance.properties = params

        if (!authorisedSystemInstance.save(flush: true)) {
            return null
        }

        return authorisedSystemInstance
    }

    @Override
    Boolean delete(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        if (!authorisedSystemInstance) {
            return false
        }
        try {
            authorisedSystemInstance.delete(flush: true)
        }
        catch (DataIntegrityViolationException e) {
            return false
        }
        return true
    }
}
