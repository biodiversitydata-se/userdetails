/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.userdetails.gorm

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.cas.encoding.BcryptPasswordEncoder
import au.org.ala.cas.encoding.LegacyPasswordEncoder
import au.org.ala.userdetails.EmailService
import au.org.ala.userdetails.IUserService
import au.org.ala.userdetails.LocationService
import au.org.ala.userdetails.PasswordService
import au.org.ala.userdetails.ResultStreamer
import au.org.ala.users.RoleRecord
import au.org.ala.users.UserPropertyRecord
import au.org.ala.users.UserRecord
import au.org.ala.users.UserRoleRecord
import au.org.ala.web.AuthService
import au.org.ala.ws.service.WebService
import grails.converters.JSON
import grails.core.GrailsApplication
import grails.plugin.cache.Cacheable
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import grails.util.Environment
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.grails.datastore.mapping.core.Session
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.hibernate.ScrollableResults
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource

import javax.servlet.http.HttpSession

@Slf4j
@Transactional
class GormUserService implements IUserService {

    static final String BCRYPT_ENCODER_TYPE = 'bcrypt'
    static final String LEGACY_ENCODER_TYPE = 'legacy'

    EmailService emailService
    PasswordService passwordService
    AuthService authService
    GrailsApplication grailsApplication
    LocationService locationService
    MessageSource messageSource
    WebService webService

    @Value('${password.encoder}')
    String passwordEncoderType = 'bcrypt'
    @Value('${bcrypt.strength}')
    Integer bcryptStrength = 10
    @Value('${encoding.algorithm}')
    String legacyAlgorithm
    @Value('${encoding.salt}')
    String legacySalt

    @Value('${attributes.affiliations.enabled:false}')
    boolean affiliationsEnabled = false

    boolean updateUser(String userId, GrailsParameterMap params) {

        User user = getUserById(userId)

        def emailRecipients = [user.email]
        if (params.email != user.email) {
            emailRecipients << params.email
        }

        try {
            // and username and email address must be kept in sync
            params.userName = params.email

            user.setProperties(params)
            user.activated = true
            user.locked = false
            user.save(failOnError: true, flush:true)
            updateProperties(user, params)
            emailService.sendUpdateProfileSuccess(user, emailRecipients)
            true
        } catch (Exception e){
            log.error(e.getMessage(), e)
            false
        }
    }

    boolean disableUser(UserRecord user) {
        assert user instanceof User
        try {
            user.activated = false
            user.save(failOnError: true, flush: true)
            Map resp = webService.post("${grailsApplication.config.getProperty('alerts.url')}/api/alerts/user/${user.id}/unsubscribe", [:])
            if (resp.statusCode != HttpStatus.SC_OK) {
                log.error("Alerts returned ${resp} when trying to disable the user's alerts. " +
                        "The user has been disabled, but their alerts are still active.")
            }
            true
        } catch (Exception e){
            log.error(e.getMessage(), e)
            false
        }
    }

    @Transactional(readOnly = true)
    boolean isActive(String email) {
        def user = User.findByEmailOrUserName(email?.toLowerCase(), email?.toLowerCase())
        return user?.activated ?: false
    }

    @Transactional(readOnly = true)
    boolean isLocked(String email) {
        def user = User.findByEmailOrUserName(email?.toLowerCase(), email?.toLowerCase())
        return user?.locked ?: false
    }

    @Transactional(readOnly = true)
    boolean isEmailRegistered(String email) {
        return User.findByEmailOrUserName(email?.toLowerCase(), email?.toLowerCase()) != null
    }

    @Transactional(readOnly = true)
    boolean isEmailInUse(String newEmail) {
        return User.findByEmailOrUserName(newEmail?.toLowerCase(), newEmail?.toLowerCase())
    }

    @Transactional
    boolean activateAccount(UserRecord user, GrailsParameterMap params) {
        assert user instanceof User
        //check the activation key
        if (user.tempAuthKey == params.authKey) {

            Map resp = webService.post("${grailsApplication.config.getProperty('alerts.url')}/api/alerts/user/createAlerts", [:], [userId: user.id, email: user.email, firstName: user.firstName, lastName: user.lastName])
            if (resp.statusCode == HttpStatus.SC_CREATED) {
                emailService.sendAccountActivationSuccess(user, resp.resp)
            } else if (resp.statusCode != HttpStatus.SC_OK) {
                log.error("Alerts returned ${resp} when trying to create user alerts for " + user.id + " with email: " + user.email)
            }

            user.activated = true
            user.save(flush:true)
            return true
        } else {
            log.error('Auth keys did not match for user : ' + params.userId + ", supplied: " + params.authKey + ", stored: " + user.tempAuthKey)
            return false
        }
    }

    @Override
    def listUsers(String query, String paginationToken, int maxResults) {

        if (query) {

            String q = "%${query}%"

            return User.findAllByEmailLikeOrLastNameLikeOrFirstNameLike(q, q, q, [offset: paginationToken as int, max: maxResults ])
        }

        return User.list([offset: paginationToken as int, max: maxResults ])
    }

    @Override
    Collection<UserRecord> listUsers() {
        return User.list()
    }

    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody) {

        def results = new BulkUserLoadResults()

        if (!stream) {
            results.message = "No data specified!"
            return results
        }

        int lineNumber = 0
        int expectedColumns = 4

        def roleUser = Role.findByRole("ROLE_USER")

        stream.eachCsvLine { tokens ->
            // email_address,first_name,surname,roles
            if (++lineNumber == 1 && firstRowContainsFieldNames) {
                // ignore...
            } else {

                if (tokens.size() == 1 && !tokens[0]?.trim()?.length()) {
                    return // skip empty lines
                }

                if (tokens.size() != expectedColumns) {
                    results.failedRecords << [lineNumber: lineNumber, tokens: tokens, reason: "Incorrect number of columns - expected ${expectedColumns}, got ${tokens.size()}"]
                    return
                }

                def emailAddress = tokens[0]
                // Check to see if this email address is already in use...
                def userInstance = User.findByEmail(emailAddress)
                def isNewUser = true

                def existingRoles = [] as Set
                if (userInstance) {
                    isNewUser = false
                    // keep track of their current roles
                    existingRoles.addAll(UserRole.findAllByUser(userInstance)*.role.collect { GrailsHibernateUtil.unwrapIfProxy(it) })
                } else {
                    userInstance = new User(email: emailAddress, userName: emailAddress, firstName: tokens[1], lastName: tokens[2])
                    userInstance.activated = true
                    userInstance.locked = false
                }

                // Now add roles
                def roles = []
                if (!existingRoles.contains(roleUser)) {
                    roles << roleUser
                }
                if (tokens[3]?.trim()) {
                    def roleArray = tokens[3].trim()?.split(" ")
                    for (String roleName : roleArray) {
                        def role = Role.findByRole(roleName)
                        if (!role) {
                            results.failedRecords << [lineNumber: lineNumber, tokens: tokens, reason: "Specified role '${roleName} does not exist"]
                            return
                        } else {
                            if (!roles.contains(role) && !existingRoles.contains(role)) {
                                roles << role
                            }
                        }
                    }
                }

                String password = null

                if (isNewUser) {
                    userInstance.save(failOnError: true)
                    password = passwordService.generatePassword(userInstance)
                    results.userAccountsCreated++
                }

                roles?.each { role ->
                    def userRole = new UserRole(user: userInstance, role: role)
                    userRole.save(failOnError: true)
                }

                if (!isNewUser) {
                    results.warnings << [lineNumber: lineNumber, tokens: tokens, reason: "Email address already exists in database. Added roles ${roles}"]
                } else {
                    // UserRecord Properties
                    def userProps = [:]

                    userProps['affiliation'] = affiliation ?: 'disinclinedToAcquiesce'
                    userProps['bulkCreatedOn'] = new Date().format("yyyy-MM-dd HH:mm:ss")
                    setUserPropertiesFromMap(userInstance, userProps)

                    // Now send a temporary password to the user...
                    try {
                        resetAndSendTemporaryPassword(userInstance, emailSubject, emailTitle, emailBody, password)
                        passwordService
                    } catch (PasswordResetFailedException ex) {
                        // Catching the checked exception should prevent the transaction from failing
                        log.error("Failed to send temporary password via email!", ex)
                        results.warnings << [lineNumber: lineNumber, tokens: tokens, reason: "Failed to send password reset email. Check mail configuration"]
                    }
                }


            }
        }

        results.success = true

        return results
    }

    private setUserPropertiesFromMap(UserRecord user, Map properties) {

        properties.keySet().each { String propName ->
            def propValue = properties[propName] ?: ''
            setUserProperty(user, propName, propValue)
        }
    }

    private setUserProperty(UserRecord user, String propName, String propValue) {
        def existingProperty = UserProperty.findByUserAndName(user, propName)
        if (existingProperty) {
            existingProperty.value = propValue
            existingProperty.save()
        } else {
            def newProperty = new UserProperty(user: user, name: propName, value: propValue)
            newProperty.save(failOnError: true)
        }
    }

    UserRecord registerUser(GrailsParameterMap params) throws Exception {

        //does a user with the supplied email address exist
        def user = new User(params)
        user.userName = params.email
        user.activated = false
        user.locked = false
        user.tempAuthKey = UUID.randomUUID().toString()
        def createdUser = user.save(flush: true, failOnError: true)
        updateProperties(createdUser, params)

        //add a role of user
        def roleUser = Role.findByRole("ROLE_USER")
        new UserRole(user:user, role:roleUser).save(flush:true, failOnError: true)

        log.info("Newly created user: " + createdUser.id)
        createdUser
    }

    void updateProperties(UserRecord user, GrailsParameterMap params) {

        ['city', 'organisation', 'state', 'country'].each { propName ->
            setUserProperty(user, propName, params.get(propName, ''))
        }
        if (affiliationsEnabled) {
            setUserProperty(user, 'affiliation', params.get('affiliation', ''))
        }
    }

    void deleteUser(UserRecord user) {
        assert user instanceof User
        if (user) {
            // First need to delete any user properties
            def userProperties = UserProperty.findAllByUser(user)
            userProperties.each { userProp ->
                userProp.delete()
            }
            // Then delete any roles
            def userRoles = UserRole.findAllByUser(user)
            userRoles.each { userRole ->
                userRole.delete()
            }

            // Delete password
            def passwords = Password.findAllByUser(user)
            passwords.each { password ->
                password.delete()
            }

            // and finally delete the use object
            user.delete()
        }

    }

    @Override
    void resetAndSendTemporaryPassword(UserRecord user, String emailSubject, String emailTitle, String emailBody, String password = null) throws PasswordResetFailedException {
        assert user instanceof User
        if (user) {
            //set the temp auth key
            user.tempAuthKey = UUID.randomUUID().toString()
            user.save(flush: true)
            //send the email
            emailService.sendPasswordReset(user, user.tempAuthKey, emailSubject, emailTitle, emailBody, password)
        }
    }

    @Override
    void clearTempAuthKey(UserRecord user) {
        assert user instanceof User
        if (user) {
            //set the temp auth key
            user.tempAuthKey = null
            user.save(flush: true)
        }
    }

    @Override
    User getUserById(String userId) {
        return User.get(userId as Long)
    }

    @Override
    User getUserByEmail(String email) {
        return User.findByEmail(email)
    }
/**
     * This service method returns the UserRecord object for the current user.
     */
    @Transactional(readOnly = true)
    UserRecord getCurrentUser() {

        def userId = authService.getUserId()
        if (userId == null) {
            // Problem. This might mean an expired cookie, or it might mean that this service is not in the authorised system list
            log.debug("Attempt to get current user returned null. This might indicating that this machine is not the authorised system list")
            return null
        }

        User user = null
        if(userId.toString().isLong()){
            user = User.get(userId.toLong())
            if (user == null && Environment.current != Environment.PRODUCTION) {
                // try looking up by email, as this may be a dev session, and the id's might not line up because this service is talking to the local database
                def email = authService.getEmail()
                if (email) {
                    user = User.findByEmail(email)
                }
            }
        } else {
            user = User.findByEmail(authService.getEmail())
        }

        return user
    }

    @NotTransactional
    @Override
    String getResetPasswordUrl(UserRecord user) {
        assert user instanceof User
        if(user.tempAuthKey){
            emailService.getServerUrl() + "resetPassword/" +  user.id +  "/"  + user.tempAuthKey
        }
    }

    @Transactional(readOnly = true)
    Collection<User> findUsersForExport(List usersInRoles, includeInactive) {
        def roles = usersInRoles? Role.findAllByRoleInList(usersInRoles) : []
        def criteria = User.createCriteria()
        def results
            results = criteria.listDistinct {
                and {
                    if(roles) {
                        userRoles {
                            'in'('role', roles)
                        }
                    }
                    if(!includeInactive) {
                        eq('activated', true)
                    }
                }
            }
        results
    }

    /**
     * Calculate the number of active users (not locked and is activated), as well as the number
     * of active users 1 year ago (for comparison).
     *
     * @return Map jsonMap
     */
    @Cacheable('dailyCache')
    @Transactional(readOnly = true)
    Map getUsersCounts(Locale locale) {
        Map jsonMap = [description: messageSource.getMessage("getUserCounts.description", null, locale?:Locale.default)]
        jsonMap.totalUsers = User.countByLockedAndActivated(false, true)
        // calculate number of users 1 year ago
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1) // minus 1 year
        Date oneYearAgoDate = cal.getTime()
        jsonMap.totalUsersOneYearAgo = User.countByLockedAndActivatedAndDateCreatedLessThan(false, true, oneYearAgoDate)
        log.debug "jsonMap = ${jsonMap as JSON}"
        jsonMap
    }

    List<String[]> countByProfileAttribute(String s, Date date, Locale locale) {
        def results = UserProperty.withCriteria {
            if (date) {
                user {
                    gt 'lastLogin', date
                }
            }
            eq 'name', s

            projections {
                groupProperty "value"
                count 'name', 'count'
            }
            order('count')
        }
        def affiliations = locationService.affiliationSurvey(locale)
        return results.collect {
            [affiliations[it[0]] ?: it[0], it[1].toString()].toArray(new String[0])
        }
    }

    @Override
    Collection<RoleRecord> listRoles() {
        Role.list()
    }

    @Override
    Collection<RoleRecord> listRoles(String paginationToken, int maxResults) {
        Role.list([offset: paginationToken as int, max: maxResults ])
    }

//    Role createRole(GrailsParameterMap params) {
//
//
//    }

    @Override
    boolean addUserRole(String userId, String roleId) {

        def user = getUserById(userId)
        def role = Role.findByRole(roleId)

         new UserRole(user: user, role: role).save()
    }

//    @Override
//    boolean removeUserRole(UserRecord user, RoleRecord role) {
//        return false
//    }

    @Override
    void findScrollableUsersByUserName(String username, int max, ResultStreamer resultStreamer) {
        User.withStatelessSession { Session session ->
            def c = User.createCriteria()
            ScrollableResults results = c.scroll {
                or {
                    ilike('userName', "%$username%")
                    ilike('email', "%$username%")
                    ilike('displayName', "%$username%")
                }
                maxResults(max)
            }

            streamUserResults(resultStreamer, results, session)
        }
    }

    @Override
    void findScrollableUsersByIdsAndRole(List<String> ids, String roleName, ResultStreamer resultStreamer) {

        def things = ids.groupBy { it.isLong() }
        def userIds = things[false]
        def numberIds = things[true]

        User.withStatelessSession { Session session ->
            RoleRecord role = Role.findByRole(roleName)

            def c = User.createCriteria()
            ScrollableResults results = c.scroll {
                or {
                    if (numberIds) {
                        inList('id', numberIds*.toLong())
                    }
                    if (userIds) {
                        inList('userName', userIds)
                        inList('email', userIds)
                    }
                }
                userRoles {
                    eq("role", role)
                }
            }

            streamUserResults(resultStreamer, results, session)
        }
    }

    @Override
    void addRoles(Collection<RoleRecord> roleRecords) {
        Role.saveAll(roleRecords.collect { new Role(it.role, it.description) })

    }

    @Override
    List<UserPropertyRecord> findAllAttributesByName(String s) {
        UserProperty.findAllByName("flickrId")
    }

    @Override
    void addOrUpdateProperty(UserRecord userRecord, String name, String value) {
        assert userRecord instanceof User
        UserProperty.addOrUpdateProperty(userRecord, name, value)
    }

    @Override
    void removeUserAttributes(UserRecord user, ArrayList<String> attrs) {
        def props = UserProperty.findAllByUserAndNameInList(user, attrs)
        if (props) UserProperty.deleteAll(props)
    }

    @Override
    void getUserAttribute(UserRecord userRecord, String attribute) {
        UserProperty.findAllByUserAndName(userRecord, attribute)
    }

    @Override
    List<UserProperty> getAllAvailableProperties() {
        UserProperty.withCriteria {
            projections {
                distinct("name")
            }
            order("name")
        }
    }

    @Override
    RoleRecord addRole(RoleRecord roleRecord) {
        return new Role(role: roleRecord.role).save(flush: true)
    }

    @Override
    User findByUserNameOrEmail(String username) {
        return User.findByUserNameOrEmail(username, username)
    }

    @Override
    List<String[]> listNamesAndEmails() {
        return User.findNameAndEmailWhereEmailIsNotNull()
    }

    @Override
    List<String[]> listIdsAndNames() {
        return User.findIdFirstAndLastName()
    }

    @Override
    List<String[]> listUserDetails() {
        return User.findUserDetails()
    }

//    @Override
//    RoleRecord findRole(String role) {
//        return Role.findByRole(role)
//    }

    @Override
    Map findUserRoles(String roleName, GrailsParameterMap params) {
        params.max = Math.min(params.int('max', 100), 1000)
        if (roleName) {
            def role = Role.findByRole(roleName)
            if(role) {
                def list = UserRole.findAllByRole(role, params)
                return [userRoleInstanceList: list, userRoleInstanceTotal: UserRole.findAllByRole(role).size()]
            } else {
                return [userRoleInstanceList: [], userRoleInstanceTotal: 0]
            }
        } else {
            return [userRoleInstanceList: UserRole.list(params), userRoleInstanceTotal: UserRole.count()]
        }
    }

    @Override
    boolean deleteRole(String userId, String roleName) {
        def user = UserRecord.get(userId.toLong())
        def role = RoleRecord.get(roleName)

        UserRole.withNewTransaction {
            def userRoleInstance = UserRole.findByUserAndRole(user, role)
            if (!userRoleInstance) {
                return false
            }
            userRoleInstance.delete(flush: true)
            return true
        }
    }

    private void streamUserResults(ResultStreamer resultStreamer, ScrollableResults results, session) {
        resultStreamer.init()
        try {
            int count = 0

            while (results.next()) {
                UserRecord user = ((UserRecord) results.get()[0])

                resultStreamer.offer(user)

                if (count++ % 50 == 0) {
                    session.flush()
                    session.clear()
                }
            }
        } finally {
            resultStreamer.finalise()
        }
        resultStreamer.complete()
    }

    @Override
    boolean resetPassword(UserRecord user, String newPassword, boolean isPermanent, String confirmationCode) {
        assert user instanceof User
        Password.findAllByUser(user).each {
            it.delete()
        }

        boolean isBcrypt = passwordEncoderType.equalsIgnoreCase(BCRYPT_ENCODER_TYPE)

        def encoder = isBcrypt ? new BcryptPasswordEncoder(bcryptStrength) : new LegacyPasswordEncoder(legacySalt, legacyAlgorithm, true)
        def encodedPassword = encoder.encode(newPassword)

        //reuse object if old password
        def password = new Password()
        password.user = user
        password.password = encodedPassword
        password.type = isBcrypt ? BCRYPT_ENCODER_TYPE : LEGACY_ENCODER_TYPE
        password.created = new Date().toTimestamp()
        password.expiry = null
        password.status = "CURRENT"
        password.save(failOnError: true)
        return true
    }

    @Override
    String getPasswordResetView() {
        return "startPasswordReset"
    }

    @Override
    def sendAccountActivation(UserRecord user) {
        emailService.sendAccountActivation(user, user.tempAuthKey)
    }

    @Override
    String getSecretForMfa() {}

    @Override
    boolean verifyUserCode(String userCode){}

    @Override
    void enableMfa(String userId, boolean enable){}
}
