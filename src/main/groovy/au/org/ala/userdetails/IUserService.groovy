package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.userdetails.records.IUserRecord
import au.org.ala.userdetails.records.UserRecord
import grails.web.servlet.mvc.GrailsParameterMap

interface IUserService {

    UserRecord getUser(def id)

    UserRecord getUserByEmail(def email)

    def updateUser(UserRecord user, GrailsParameterMap params)

    def disableUser(UserRecord user)

    boolean isActive(String email)

    boolean isLocked(String email)

    boolean isEmailRegistered(String email)

    boolean isEmailInUse(String newEmail, UserRecord user)

    def activateAccount(UserRecord user)

    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody)

    UserRecord registerUser(GrailsParameterMap params) throws Exception

    def updateProperties(UserRecord user, GrailsParameterMap params)

    def deleteUser(UserRecord user)

    def resetAndSendTemporaryPassword(UserRecord user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException

    def clearTempAuthKey(UserRecord user)

    /**
     * This service method returns the User object for the current user.
     */
    UserRecord getCurrentUser()

    String getResetPasswordUrl(UserRecord user)

    def findUsersForExport(List usersInRoles, includeInactive)

    /**
     * Calculate the number of active users (not locked and is activated), as well as the number
     * of active users 1 year ago (for comparison).
     *
     * @return Map jsonMap
     */
    Map getUsersCounts(Locale locale)

    List<String[]> countByProfileAttribute(String s, Date date, Locale locale)
}
