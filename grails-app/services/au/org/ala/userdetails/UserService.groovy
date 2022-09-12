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
import au.org.ala.userdetails.records.IUserRecord
import au.org.ala.userdetails.records.UserRecord
import grails.converters.JSON
import grails.plugin.cache.Cacheable
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import grails.util.Environment
import grails.web.servlet.mvc.GrailsParameterMap
import org.apache.http.HttpStatus
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value

@Transactional
class UserService implements IUserService {

    @Autowired
    @Qualifier('userServiceImpl')
    IUserService delegate

    @Override
    UserRecord getUser(Object id) {
        return delegate.getUser(id)
    }

    @Override
    UserRecord getUserByEmail(Object email) {
        return delegate.getUserByEmail(email)
    }

    @Override
    def updateUser(UserRecord user, GrailsParameterMap params) {
        return delegate.updateUser(user, params)
    }

    @Override
    def disableUser(UserRecord user) {
        return delegate.disableUser(user)
    }

    @Override
    boolean isActive(String email) {
        return delegate.isActive(email)
    }

    @Override
    boolean isLocked(String email) {
        return delegate.isLocked(email)
    }

    @Override
    boolean isEmailRegistered(String email) {
        return delegate.isEmailRegistered(email)
    }

    @Override
    boolean isEmailInUse(String newEmail, UserRecord user) {
        return delegate.isEmailInUse(newEmail, user)
    }

    @Override
    def activateAccount(UserRecord user) {
        return delegate.activateAccount(user)
    }

    @Override
    BulkUserLoadResults bulkRegisterUsersFromFile(InputStream stream, Boolean firstRowContainsFieldNames, String affiliation, String emailSubject, String emailTitle, String emailBody) {
        return delegate.bulkRegisterUsersFromFile(stream, firstRowContainsFieldNames, affiliation, emailSubject, emailTitle, emailBody)
    }

    @Override
    UserRecord registerUser(GrailsParameterMap params) throws Exception {
        return delegate.registerUser(params)
    }

    @Override
    def updateProperties(UserRecord user, GrailsParameterMap params) {
        return delegate.updateProperties(user, params)
    }

    @Override
    def deleteUser(UserRecord user) {
        return delegate.deleteUser(user)
    }

    @Override
    def resetAndSendTemporaryPassword(UserRecord user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException {
        return delegate.resetAndSendTemporaryPassword(user, emailSubject, emailTitle, emailBody, password)
    }

    @Override
    def clearTempAuthKey(UserRecord user) {
        return delegate.clearTempAuthKey(user)
    }

    @Override
    UserRecord getCurrentUser() {
        return delegate.getCurrentUser()
    }

    @Override
    String getResetPasswordUrl(UserRecord user) {
        return delegate.getResetPasswordUrl(user)
    }

    @Override
    def findUsersForExport(List usersInRoles, Object includeInactive) {
        return delegate.findUsersForExport(usersInRoles, includeInactive)
    }

    @Override
    Map getUsersCounts(Locale locale) {
        return delegate.getUsersCounts(locale)
    }

    @Override
    List<String[]> countByProfileAttribute(String s, Date date, Locale locale) {
        return delegate.countByProfileAttribute(s, date, locale)
    }
}
