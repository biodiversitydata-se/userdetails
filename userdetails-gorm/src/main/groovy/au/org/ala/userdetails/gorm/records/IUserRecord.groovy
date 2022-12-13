package au.org.ala.userdetails.records

interface IUserRecord {

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

    Map<String,String> propsAsMap()

}
