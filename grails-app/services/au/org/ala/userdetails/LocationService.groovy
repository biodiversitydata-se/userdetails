package au.org.ala.userdetails

import grails.converters.JSON
import grails.plugin.cache.Cacheable
import grails.gorm.transactions.NotTransactional
import org.grails.web.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.core.io.Resource

class LocationService {

    @Autowired
    MessageSource messageSource

    @Value('${attributes.states.path}')
    Resource states

    @NotTransactional
    @Cacheable("states")
    JSONObject getStatesAndCountries() {
        return states.inputStream.withReader('UTF-8') { reader ->
            (JSONObject) JSON.parse(reader)
        }
    }

    Map<String,String> affiliationSurvey(Locale locale) {
        // Use ala.affiliations.$key for i18N
        [
                'community': messageSource.getMessage('ala.affiliations.community', null, 'Community based organisation – club, society, landcare', locale),
                'education': messageSource.getMessage('ala.affiliations.education', null, 'Education – primary and secondary schools, TAFE, environmental or wildlife education', locale),
                'firstNationsOrg': messageSource.getMessage('ala.affiliations.firstNationsOrg', null, 'First Nations organisation', locale),
                'government': messageSource.getMessage('ala.affiliations.government', null, 'Government – federal, state and local', locale),
                'industry': messageSource.getMessage('ala.affiliations.industry', null, 'Industry, commercial, business or retail', locale),
                'mri': messageSource.getMessage('ala.affiliations.mri', null, 'Medical Research Institute (MRI)', locale),
                'museum': messageSource.getMessage('ala.affiliations.museum', null, 'Museum, herbarium, library, botanic gardens', locale),
                'nfp': messageSource.getMessage('ala.affiliations.nfp', null, 'Not for profit', locale),
                'other': messageSource.getMessage('ala.affiliations.other', null, 'Other', locale),
                'otherResearch': messageSource.getMessage('ala.affiliations.otherResearch', null, 'Other research organisation, unaffiliated researcher', locale),
                'disinclinedToAcquiesce': messageSource.getMessage('ala.affiliations.disinclinedToAcquiesce', null, 'Prefer not to say', locale),
                'private': messageSource.getMessage('ala.affiliations.private', null, 'Private user', locale),
                'publiclyFunded': messageSource.getMessage('ala.affiliations.publiclyFunded', null, 'Publicly Funded Research Agency (PFRA) e.g. CSIRO, AIMS, DSTO', locale),
                'uniResearch': messageSource.getMessage('ala.affiliations.uniResearch', null, 'University – faculty, researcher', locale),
                'uniGeneral': messageSource.getMessage('ala.affiliations.uniGeneral', null, 'University - general staff, administration, management', locale),
                'uniStudent': messageSource.getMessage('ala.affiliations.uniStudent', null, 'University – student', locale),
                'wildlife': messageSource.getMessage('ala.affiliations.wildlife', null, 'Wildlife park, sanctuary, zoo, aquarium, wildlife rescue', locale),
                'volunteer': messageSource.getMessage('ala.affiliations.volunteer', null, 'Volunteer, citizen scientist', locale)
        ]
    }
}
