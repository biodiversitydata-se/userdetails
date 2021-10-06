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
                'uniResearch': g.message(code:'ala.affiliations.uniResearch', default: 'University – faculty, researcher'),
                'uniGeneral': g.message(code:'ala.affiliations.uniGeneral', default: 'University - general staff, administration, management'),
                'uniStudent': g.message(code:'ala.affiliations.uniStudent', default: 'University – student'),
                'education': g.message(code:'ala.affiliations.education', default: 'Education – primary and secondary schools, TAFE, environmental or wildlife education'),
                'firstNationsOrg': g.message(code:'ala.affiliations.firstNationsOrg', default: 'First Nations organisation'),
                'government': g.message(code:'ala.affiliations.government', default: 'Government – federal, state and local'),
                'industry': g.message(code:'ala.affiliations.industry', default: 'Industry, commercial, business or retail'),
                'museum': g.message(code:'ala.affiliations.museum', default: 'Museum, herbarium, library, botanic gardens'),
                'wildlife': g.message(code:'ala.affiliations.wildlife', default: 'Wildlife park, sanctuary, zoo, aquarium, wildlife rescue'),
                'publiclyFunded': g.message(code:'ala.affiliations.publiclyFunded', default: 'Publicly Funded Research Agency (PFRA) includes CSIRO, ANSTO, AIMS, AAO, AIATSIS, AAD, DSTO, GA, BoM'),
                'mri': g.message(code:'ala.affiliations.mri', default: 'Medical Research Institute (MRI)'),
                'otherResearch': g.message(code:'ala.affiliations.otherResearch', default: 'Other research organisation, unaffiliated researcher'),
                'nfp': g.message(code:'ala.affiliations.nfp', default: 'Not for profit'),
                'community': g.message(code:'ala.affiliations.community', default: 'Community based organisation – club, society, landcare'),
                'volunteer': g.message(code:'ala.affiliations.volunteer', default: 'Volunteer, citizen scientist'),
                'private': g.message(code:'ala.affiliations.private', default: 'Private user'),
                'other': g.message(code:'ala.affiliations.other', default: 'Other'),
                'disinclinedToAcquiesce': g.message(code:'ala.affiliations.disinclinedToAcquiesce', default: 'Prefer not to say')
        ]
    }
}
