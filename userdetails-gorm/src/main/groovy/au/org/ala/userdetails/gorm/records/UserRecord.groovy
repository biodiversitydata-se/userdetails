package au.org.ala.userdetails.records

import grails.plugin.cache.Cacheable
import grails.web.databinding.WebDataBinding

@Cacheable
class UserRecord implements Serializable, WebDataBinding {


    String firstName
    String lastName

    String userName
    String email

    Date dateCreated
    Date lastUpdated

    Date lastLogin

    Boolean activated
    Boolean locked

    String tempAuthKey

    String displayName

    Collection<UserRoleRecord> userRoles

    Collection<UserPropertyRecord> userProperties

}