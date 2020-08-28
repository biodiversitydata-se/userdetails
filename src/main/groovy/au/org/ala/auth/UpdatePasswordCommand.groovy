package au.org.ala.auth

import grails.validation.Validateable
/**
 * Data binding and validation for the password update action.
 */
class UpdatePasswordCommand implements Validateable {

    Long userId
    String password
    String reenteredPassword
    String authKey

    static constraints = {
        // note that the password validation is done in the controller actions, not the command
        password blank: false
        reenteredPassword validator: { val, cmd -> val == cmd.password }
        userId nullable: false
        authKey blank: false
    }
}
