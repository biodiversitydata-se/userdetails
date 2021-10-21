package au.org.ala.userdetails

class LocationsTagLib {

    static namespace = "l"

    static defaultEncodeAs = [taglib:'html']
    static encodeAsForTags = [states: [taglib:'none'], countries: [taglib:'none'], affiliations: [taglib:'none']]
    static returnObjectForTags = ['states', 'countries', 'affiliations']

    def locationService

    def states = { attrs, body ->
        def country = attrs.remove('country')
        locationService.getStatesAndCountries().states[country] ?: []
    }

    def countries = { attrs, body ->
        locationService.getStatesAndCountries().countries
    }

    def affiliations = { attrs, body ->
        return locationService.affiliationSurvey(request.locale)
    }
}
