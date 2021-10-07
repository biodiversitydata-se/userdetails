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
        return [
                'community': g.message(code:'ala.affiliations.community', default: 'Community based organisation – club, society, landcare'),
                'education': g.message(code:'ala.affiliations.education', default: 'Education – primary and secondary schools, TAFE, environmental or wildlife education'),
                'firstNationsOrg': g.message(code:'ala.affiliations.firstNationsOrg', default: 'First Nations organisation'),
                'government': g.message(code:'ala.affiliations.government', default: 'Government – federal, state and local'),
                'industry': g.message(code:'ala.affiliations.industry', default: 'Industry, commercial, business or retail'),
                'mri': g.message(code:'ala.affiliations.mri', default: 'Medical Research Institute (MRI)'),
                'museum': g.message(code:'ala.affiliations.museum', default: 'Museum, herbarium, library, botanic gardens'),
                'nfp': g.message(code:'ala.affiliations.nfp', default: 'Not for profit'),
                'other': g.message(code:'ala.affiliations.other', default: 'Other'),
                'otherResearch': g.message(code:'ala.affiliations.otherResearch', default: 'Other research organisation, unaffiliated researcher'),
                'disinclinedToAcquiesce': g.message(code:'ala.affiliations.disinclinedToAcquiesce', default: 'Prefer not to say'),
                'private': g.message(code:'ala.affiliations.private', default: 'Private user'),
                'publiclyFunded': g.message(code:'ala.affiliations.publiclyFunded', default: 'Publicly Funded Research Agency (PFRA) e.g. CSIRO, AIMS, DSTO'),
                'uniResearch': g.message(code:'ala.affiliations.uniResearch', default: 'University – faculty, researcher'),
                'uniGeneral': g.message(code:'ala.affiliations.uniGeneral', default: 'University - general staff, administration, management'),
                'uniStudent': g.message(code:'ala.affiliations.uniStudent', default: 'University – student'),
                'wildlife': g.message(code:'ala.affiliations.wildlife', default: 'Wildlife park, sanctuary, zoo, aquarium, wildlife rescue'),
                'volunteer': g.message(code:'ala.affiliations.volunteer', default: 'Volunteer, citizen scientist')
        ]
        // Use ala.affiliations.$key for i18N
    }
}
