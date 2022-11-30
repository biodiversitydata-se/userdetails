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

interface IUserService {

    boolean updateUser(GrailsParameterMap params)

    boolean updateUser(String userId, GrailsParameterMap params)

    boolean disableUser(UserRecord user)

    boolean isActive(String email)

    boolean isLocked(String email)

    boolean isEmailRegistered(String email)

    boolean isEmailInUse(String newEmail)

    void activateAccount(UserRecord user)

    def listUsers(String query, String paginationToken, int maxResults)

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

    Collection<RoleRecord> listRoles(String paginationToken, int maxResults)

//    RoleRecord createRole(GrailsParameterMap params)

    boolean addUserRole(String userId, String roleName)

//    boolean removeUserRole(UserRecord user, RoleRecord role)

    // TODO return type and implementation
    void findScrollableUsersByUserName(String username, int maxResults, ResultStreamer resultStreamer)

    // TODO return type and implementation
    void findScrollableUsersByIdsAndRole(List<String> ids, String roleName, ResultStreamer resultStreamer)

    void addRoles(Collection<RoleRecord> roleRecords)

    List<UserPropertyRecord> findAllAttributesByName(String s)

    void addOrUpdateProperty(UserRecord userRecord, String name, String value)

    void removeUserAttributes(UserRecord userRecord, ArrayList<String> attributes)

    void getUserAttribute(UserRecord userRecord, String attribute)

    List getAllAvailableProperties()

    RoleRecord addRole(RoleRecord roleRecord)

    UserRecord findByUserNameOrEmail(String username)

    List<String[]> listNamesAndEmails()

    List<String[]> listIdsAndNames()

    List<String[]> listUserDetails()

    Map findUserRoles(String role, GrailsParameterMap grailsParameterMap)

    boolean deleteRole(String userId, String roleName)
}