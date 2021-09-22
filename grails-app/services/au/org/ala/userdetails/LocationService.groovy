package au.org.ala.userdetails

import grails.converters.JSON
import grails.plugin.cache.Cacheable
import grails.gorm.transactions.NotTransactional
import org.grails.web.json.JSONObject

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class LocationService {

    @Value('${attributes.states.path}')
    Resource states

    @NotTransactional
    @Cacheable("states")
    JSONObject getStatesAndCountries() {
        return states.inputStream.withReader('UTF-8') { reader ->
            (JSONObject) JSON.parse(reader)
        }
    }
}
