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
        // Use ala.affiliations.$key for i18N
        return [
                'uniResearch': g.message(code:'ala.affiliations.uniResearch', default:'University – faculty, researcher'),
                'uniStudent': g.message(code:'ala.affiliations.uniStudent', default:'University – student'),
                'uniGeneral': g.message(code:'ala.affiliations.uniGeneral', default:'University - general staff, library, administration, management'),
                'publiclyFunded': g.message(code:'ala.affiliations.publiclyFunded', default:'Publicly Funded Research Agency (PFRA)'),
                'mri': g.message(code:'ala.affiliations.mri', default:'Medical Research Institute (MRI)'),
                'museum': g.message(code:'ala.affiliations.museum', default:'Museum, herbarium, library or other collecting organisation'),
                'otherResearch': g.message(code:'ala.affiliations.otherResearch', default:'Other research organisation'),
                'industry': g.message(code:'ala.affiliations.industry', default:'Industry / commercial organisation'),
                'govLocal': g.message(code:'ala.affiliations.govLocal', default:'Government - local'),
                'govState': g.message(code:'ala.affiliations.govState', default:'Government - state'),
                'govFederal': g.message(code:'ala.affiliations.govFederal', default:'Government - federal'),
                'volunteer': g.message(code:'ala.affiliations.volunteer', default:'Volunteer including citizen scientists'),
                'education': g.message(code:'ala.affiliations.education', default:'Education – primary and secondary'),
                'nfp': g.message(code:'ala.affiliations.nfp', default:'Not for profit'),
                'other': g.message(code:'ala.affiliations.other', default:'Other / unaffiliated'),
                'disinclinedToAcquiesce': g.message(code:'ala.affiliations.disinclinedToAcquiesce', default:'Prefer not to say')
        ]
    }
}
