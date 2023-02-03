package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.IAuthorisedSystemRepository
import grails.converters.JSON
import grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.dao.DataIntegrityViolationException

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

        return [authorisedSystemInstanceList: list, authorisedSystemInstanceTotal: count]
    }

    @Override
    def save(GrailsParameterMap params) {
        def authorisedSystemInstance = new AuthorisedSystem(params)
        if (!authorisedSystemInstance.save(flush: true)) {
            return null
        }
        return authorisedSystemInstance
    }

    @Override
    def get(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        return authorisedSystemInstance
    }

    @Override
    def update(GrailsParameterMap params) {
        def authorisedSystemInstance = AuthorisedSystem.get(params.id)
        if (!authorisedSystemInstance) {
            return null
        }

        if (params.version != null) {
            if (authorisedSystemInstance.version > params.version) {
                authorisedSystemInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                        [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem')] as Object[],
                        "Another user has updated this AuthorisedSystem while you were editing")
                return authorisedSystemInstance
            }
        }

        authorisedSystemInstance.properties = params

        if (!authorisedSystemInstance.save(flush: true)) {
            return null
        }

        return authorisedSystemInstance
    }

    @Override
    def delete(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        if (!authorisedSystemInstance) {
            return null
        }
        try {
            authorisedSystemInstance.delete(flush: true)
        }
        catch (DataIntegrityViolationException e) {
            return null
        }
        return
    }

    @Override
    def ajaxResolveHostName(GrailsParameterMap params) {

        def host = params.host as String

        def hostname = "?"
        def reachable = false
        if (host) {
            try {
                InetAddress addr = InetAddress.getByName(host);
                hostname = addr.getHostName();
                reachable = addr.isReachable(2000);
            } catch (Exception ex) {
                ex.printStackTrace()
            }
        }

        return [host:host, hostname: hostname, reachable: reachable]
    }

}
