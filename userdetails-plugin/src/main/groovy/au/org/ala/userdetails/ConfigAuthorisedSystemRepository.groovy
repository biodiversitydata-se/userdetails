package au.org.ala.userdetails

import grails.core.GrailsApplication

class ConfigAuthorisedSystemRepository implements IAuthorisedSystemRepository {

    GrailsApplication grailsApplication

    @Override
    Boolean findByHost(String host) {
        return grailsApplication.config.getProperty('authorised.systems', List<String>, []).contains(host)
    }
}
