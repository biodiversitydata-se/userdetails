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

import au.org.ala.cas.encoding.BcryptPasswordEncoder
import au.org.ala.cas.encoding.LegacyPasswordEncoder
import au.org.ala.users.Password
import au.org.ala.users.UserRecord
import grails.gorm.transactions.Transactional
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value

@Transactional
class PasswordService {

    static final String BCRYPT_ENCODER_TYPE = 'bcrypt'
    static final String LEGACY_ENCODER_TYPE = 'legacy'

    @Value('${password.encoder}')
    String passwordEncoderType = 'bcrypt'
    @Value('${bcrypt.strength}')
    Integer bcryptStrength = 10
    @Value('${encoding.algorithm}')
    String legacyAlgorithm
    @Value('${encoding.salt}')
    String legacySalt

    /**
     * Trigger a password reset
     *
     * @param user
     * @param newPassword
     * @return
     */
    void resetPassword(UserRecord user, String newPassword) {
       //update the password
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
    }

    String generatePassword(UserRecord user) {
       //generate a new password
       def newPassword = RandomStringUtils.randomAlphanumeric(10)

       resetPassword(user, newPassword)
       return newPassword
    }
}
