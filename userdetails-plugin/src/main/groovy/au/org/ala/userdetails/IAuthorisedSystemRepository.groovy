package au.org.ala.userdetails

import au.org.ala.users.AuthorisedSystemRecord
import grails.web.servlet.mvc.GrailsParameterMap

/**
 * Abstract checking whether a host is allowed
 */
interface IAuthorisedSystemRepository {

    /**
     * Return true if the given host parameter is authorised
     * @param host
     * @return
     */
    Boolean findByHost(String host)

    def list(GrailsParameterMap params)

    AuthorisedSystemRecord save(GrailsParameterMap params)

    AuthorisedSystemRecord get(Long id)

    AuthorisedSystemRecord update(GrailsParameterMap params)

    Boolean delete(Long id)
}
