package au.org.ala.userdetails

import au.org.ala.cas.encoding.BcryptPasswordEncoder
import au.org.ala.cas.encoding.LegacyPasswordEncoder
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
    void resetPassword(User user, String newPassword) {
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

    String generatePassword(User user) {
       //generate a new password
       def newPassword = RandomStringUtils.randomAlphanumeric(10)

       resetPassword(user, newPassword)
       return newPassword
    }
}
