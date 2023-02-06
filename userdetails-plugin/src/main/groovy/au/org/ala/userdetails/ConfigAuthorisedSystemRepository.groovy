package au.org.ala.userdetails

import au.org.ala.users.AuthorisedSystemRecord
import grails.core.GrailsApplication
import grails.web.servlet.mvc.GrailsParameterMap
import org.apache.commons.lang3.NotImplementedException

class ConfigAuthorisedSystemRepository implements IAuthorisedSystemRepository {

    GrailsApplication grailsApplication

    @Override
    Boolean findByHost(String host) {
        def list = grailsApplication.config.getProperty('authorised.systems', List, [])
                .collect{ it as AuthorisedSystemRecord }
        return list.count{it.host ==  host } > 0
    }

    @Override
    def list(GrailsParameterMap params) {
        def list = []
        def count = 0
        def query = params.q as String
        if (query) {
            def records = grailsApplication.config.getProperty('authorised.systems', List, [])
                    .collect{ it as AuthorisedSystemRecord }
            list = records.findAll{ it.host.contains(query) || it.description.contains(query)}
            count = records.size()

        } else {
            list = grailsApplication.config.getProperty('authorised.systems', List, [])
                    .collect{ it as AuthorisedSystemRecord }
            count = list.size()
        }

        return [list: list, count: count]
    }

    @Override
    AuthorisedSystemRecord save(GrailsParameterMap params) {
        throw new NotImplementedException()
    }

    @Override
    AuthorisedSystemRecord get(Long id) {
        def records = grailsApplication.config.getProperty('authorised.systems', List, [])
                .collect{ it as AuthorisedSystemRecord }
        return records.find{ it.id == id }
    }


    @Override
    AuthorisedSystemRecord update(GrailsParameterMap params) {
        throw new NotImplementedException()
    }

    @Override
    Boolean delete(Long id) {
        throw new NotImplementedException()
    }
}
