package au.org.ala.userdetails.marshaller

import au.org.ala.userdetails.User
import grails.converters.JSON

/**
 * Registers two custom marshallers for the User class - one that includes properties, one that doesn't.
 */
class UserMarshaller {

    public static final String WITH_PROPERTIES_CONFIG = 'withProperties'

    private Map toMap(User user) {
        [
                userId: user.id.toString(),
                userName: user.userName,
                firstName: user.firstName,
                lastName: user.lastName,
                email: user.email,
                activated: user.activated,
                locked: user.locked,
                roles: user.getUserRoles()*.toString() ?: []
        ]
    }

    void register(){

        JSON.createNamedConfig(WITH_PROPERTIES_CONFIG) {
            it.registerObjectMarshaller(User) { User user ->
                Map userMap = toMap(user)
                userMap.props = user.propsAsMap()
                userMap
            }
        }
        JSON.registerObjectMarshaller(User) { User user ->
            toMap(user)
        }
    }

}
