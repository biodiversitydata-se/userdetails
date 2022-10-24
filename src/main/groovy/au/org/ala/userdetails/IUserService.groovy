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
import au.org.ala.users.User
import au.org.ala.userdetails.records.IUserRecord
import au.org.ala.userdetails.records.UserRecord
import grails.web.servlet.mvc.GrailsParameterMap

interface IUserService {

    boolean updateUser(String userId, GrailsParameterMap params)

    boolean disableUser(User user)

    boolean isActive(String email)

    boolean isLocked(String email)

    boolean isEmailRegistered(String email)

    boolean isEmailInUse(String newEmail)

    void activateAccount(User user)

    def listUsers(String query, int offset, int maxResults)

    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody)

    User registerUser(GrailsParameterMap params) throws Exception

    void updateProperties(User user, GrailsParameterMap params)

    void deleteUser(User user)

    void resetAndSendTemporaryPassword(User user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException

    void clearTempAuthKey(User user)

    User getUserById(String id)

    User getUserByEmail(String email)

    /**
     * This service method returns the User object for the current user.
     */
    User getCurrentUser()

    String getResetPasswordUrl(User user)

    Collection<User> findUsersForExport(List usersInRoles, includeInactive)

    /**
     * Calculate the number of active users (not locked and is activated), as well as the number
     * of active users 1 year ago (for comparison).
     *
     * @return Map jsonMap
     */
    Map getUsersCounts(Locale locale)

    List<String[]> countByProfileAttribute(String s, Date date, Locale locale)

    boolean resetPassword(User user, String newPassword, boolean isPermanent)

    String getPasswordResetView()
}
