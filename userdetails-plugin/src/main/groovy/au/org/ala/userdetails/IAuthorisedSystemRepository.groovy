package au.org.ala.userdetails

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

    def save(GrailsParameterMap params)

    def get(Long id)

    def update(GrailsParameterMap params)

    def delete(Long id)

    def ajaxResolveHostName(GrailsParameterMap params)

}
