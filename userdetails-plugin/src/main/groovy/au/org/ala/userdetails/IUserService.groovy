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

package au.org.ala.userdetails

import au.org.ala.auth.BulkUserLoadResults
import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.RoleRecord
import au.org.ala.users.UserPropertyRecord
import au.org.ala.users.UserRecord
import au.org.ala.users.UserRoleRecord
import grails.web.servlet.mvc.GrailsParameterMap

import javax.servlet.http.HttpSession

interface IUserService {

    boolean updateUser(String userId, GrailsParameterMap params)

    boolean disableUser(UserRecord user)

    boolean isActive(String email)

    boolean isLocked(String email)

    boolean isEmailRegistered(String email)

    boolean isEmailInUse(String newEmail)

    boolean activateAccount(UserRecord user, GrailsParameterMap params)

    List<UserRecord> listUsers(String query, String paginationToken, int maxResults)

    Collection<UserRecord> listUsers()

    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody)

    UserRecord registerUser(GrailsParameterMap params) throws Exception

    void updateProperties(UserRecord user, GrailsParameterMap params)

    void deleteUser(UserRecord user)

    void resetAndSendTemporaryPassword(UserRecord user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException

    void clearTempAuthKey(UserRecord user)

    UserRecord getUserById(String userId)

    UserRecord getUserByEmail(String email)

    /**
     * This service method returns the UserRecord object for the current user.
     */
    UserRecord getCurrentUser()

    String getResetPasswordUrl(UserRecord user)

    Collection<UserRecord> findUsersForExport(List usersInRoles, includeInactive)

    /**
     * Calculate the number of active users (not locked and is activated), as well as the number
     * of active users 1 year ago (for comparison).
     *
     * @return Map jsonMap
     */
    Map getUsersCounts(Locale locale)

    List<String[]> countByProfileAttribute(String s, Date date, Locale locale)

    Collection<RoleRecord> listRoles()

    /**
     * Retrieve a list of roles, paged by the params in the argument.
     *
     * The following paging params are always supported:
     *  - max: Maximum number of items to retrieve at a time
     * The following paging params are supported in GORM:
     *  - offset: The number of items to skip forward
     *  - sort: the field to sort results by
     *  - order: whether to sort 'asc'ending or 'desc'ending
     * The following paging params are supported in Cognito:
     *  - token: The paging token provided by the back end API
     *
     * @param params The parameters that may be used in the search
     * @return A result with the list of roles and either the total count or the next page token.
     */
    PagedResult<RoleRecord> listRoles(GrailsParameterMap params)

    /**
     * Add a role with `rolename` to user identified by `userid`
     *
     * @param userId The users id
     * @param roleName The name of the role to add
     * @return True if the operation succeeded or false otherwise
     */
    boolean addUserRole(String userId, String roleName)

    /**
     * Remove a role with `rolename` from user identified by `userid`
     *
     * @param userId The users id
     * @param roleName The name of the role to add
     * @return True if the operation succeeded or false otherwise
     */
    boolean removeUserRole(String userId, String roleName)

    /**
     * Add a list of roles to the system.
     *
     * @param roleRecords The list of RoleRecords to add
     */
    void addRoles(Collection<RoleRecord> roleRecords)

    /**
     * Add a single role to the system.
     *
     * @param roleRecord The RoleRecord to add
     */

    RoleRecord addRole(RoleRecord roleRecord)

    /**
     * Find a list of users for a given role.
     *
     * The following paging params are always supported:
     *  - max: Maximum number of items to retrieve at a time
     * The following paging params are supported in GORM:
     *  - offset: The number of items to skip forward
     *  - sort: the field to sort results by
     *  - order: whether to sort 'asc'ending or 'desc'ending
     * The following paging params are supported in Cognito:
     *  - token: The paging token provided by the back end API
     *
     * @param role The role name to get the list of users for
     * @param params The paging parameters for the request
     */
    PagedResult<UserRoleRecord> findUserRoles(String role, GrailsParameterMap params)

    // TODO return type and implementation
    void findScrollableUsersByUserName(String username, int maxResults, ResultStreamer resultStreamer)

    // TODO return type and implementation
    void findScrollableUsersByIdsAndRole(List<String> ids, String roleName, ResultStreamer resultStreamer)

    List<UserPropertyRecord> findAllAttributesByName(String s)

    void addOrUpdateProperty(UserRecord userRecord, String name, String value)

    void removeUserAttributes(UserRecord userRecord, ArrayList<String> attributes)

    void getUserAttribute(UserRecord userRecord, String attribute)

    List getAllAvailableProperties()

    UserRecord findByUserNameOrEmail(String username)

    List<String[]> listNamesAndEmails()

    List<String[]> listIdsAndNames()

    List<String[]> listUserDetails()

    boolean resetPassword(UserRecord user, String newPassword, boolean isPermanent, String confirmationCode)

    String getPasswordResetView()

    def sendAccountActivation(UserRecord user)

    String getSecretForMfa()

    boolean verifyUserCode(String userCode)

    void enableMfa(String userId, boolean enable)
}