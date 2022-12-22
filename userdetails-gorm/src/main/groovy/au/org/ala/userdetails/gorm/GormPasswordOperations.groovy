package au.org.ala.userdetails.gorm

import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.cas.encoding.BcryptPasswordEncoder
import au.org.ala.cas.encoding.LegacyPasswordEncoder
import au.org.ala.userdetails.EmailService
import au.org.ala.userdetails.IPasswordOperations
import au.org.ala.users.UserRecord
import grails.gorm.transactions.NotTransactional
import org.springframework.beans.factory.annotation.Value

class GormPasswordOperations implements IPasswordOperations {

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

    EmailService emailService

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

    @NotTransactional
    @Override
    String getResetPasswordUrl(UserRecord user) {
        assert user instanceof User
        if(user.tempAuthKey){
            emailService.getServerUrl() + "resetPassword/" +  user.id +  "/"  + user.tempAuthKey
        }
    }

    @Override
    String getPasswordResetView() {
        return "startPasswordReset"
    }

}
