package au.org.ala.userdetails

import au.org.ala.auth.EnglishCustomCharacterData
import au.org.ala.cas.encoding.BcryptPasswordEncoder
import au.org.ala.cas.encoding.LegacyPasswordEncoder
import grails.gorm.transactions.Transactional
import org.passay.*
import org.passay.dictionary.WordListDictionary
import org.passay.dictionary.WordLists
import org.passay.dictionary.sort.ArraysSort
import org.springframework.beans.factory.annotation.Value

@Transactional
class PasswordService {

    static final String BCRYPT_ENCODER_TYPE = 'bcrypt'
    static final String LEGACY_ENCODER_TYPE = 'legacy'

    static final String STATUS_CURRENT = 'CURRENT'

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
        if (!newPassword || newPassword.length() < 1) {
            throw new IllegalArgumentException("The new password must not be empty.")
        }
        if (user == null) {
            throw new IllegalArgumentException("Must provide the user to reset a password.")
        }

        //update the password
        Password.findAllByUser(user).each {
            it.delete()
        }

        def encodedPassword = encodePassword(newPassword)

        //reuse object if old password
        def password = new Password()
        password.user = user
        password.password = encodedPassword
        password.type = getPasswordType()
        password.created = new Date().toTimestamp()
        password.expiry = null
        password.status = STATUS_CURRENT
        password.save(failOnError: true)
    }

    /**
     * Generate a new password with length 10 and use it to reset a user's password.
     * @param user The user that will get the new password.
     * @return The new password.
     */
    String generatePassword(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Must provide the user to generate a password.")
        }

        //generate a new password
        def newPassword = generateNewPassword(user.userName)

        resetPassword(user, newPassword)
        return newPassword
    }

    /**
     * Check that a password matches a user's existing password.
     * @param user The user.
     * @param password The password to match.
     * @return True if the password matches the existing password, otherwise false.
     */
    boolean checkUserPassword(User user, String password) {
        if (!password || password.length() < 1) {
            throw new IllegalArgumentException("The password must not be empty.")
        }
        if (user == null) {
            throw new IllegalArgumentException("Must provide the user to compare a password.")
        }

        def existingPasswords = Password.findAllByUser(user)
        def passwordType = getPasswordType()
        def dateTimeNow = new Date()
        def matchingPassword = existingPasswords.find { item ->
            comparePasswords(password, item.password) &&
                    item.type == passwordType &&
                    (item.expiry == null || item.expiry > dateTimeNow) &&
                    item.status == STATUS_CURRENT
        }

        if (matchingPassword) {
            // delete any other passwords - there should only be one password for one user
            existingPasswords.each {
                if (it != matchingPassword) {
                    it.delete()
                }
            }
            return true
        } else {
            return false
        }
    }

    /**
     * Encode a password ready to store it.
     * @param password The password to encode.
     * @return The encoded password.
     */
    String encodePassword(String password) {
        if (!password || password.length() < 1) {
            throw new IllegalArgumentException("Must supply a password to be encoded.")
        }

        boolean isBcrypt = isBcrypt()
        def encoder = isBcrypt ?
                new BcryptPasswordEncoder(bcryptStrength) :
                new LegacyPasswordEncoder(legacySalt, legacyAlgorithm, true)
        def encodedPassword = encoder.encode(password)
        return encodedPassword
    }

    /**
     * Compare a plain-text password to an encoded password.
     * @param plainPassword The plain-text password.
     * @param hashedPassword The encoded password.
     * @return True if the passwords match, otherwise false.
     */
    Boolean comparePasswords(String plainPassword, String hashedPassword) {
        if (!plainPassword || plainPassword.length() < 1 || !hashedPassword || hashedPassword.length() < 1) {
            throw new IllegalArgumentException("Must supply a plain text password and a hashed password to be compared.")
        }

        boolean isBcrypt = isBcrypt()
        def encoder = isBcrypt ?
                new BcryptPasswordEncoder(bcryptStrength) :
                new LegacyPasswordEncoder(legacySalt, legacyAlgorithm, true)
        def encodedPassword = encoder.matches(plainPassword, hashedPassword)
        return encodedPassword
    }

    /**
     * Validate a password against a password policy.
     * @param username
     * @param password
     * @return
     */
    Map<String, Object> validatePassword(String username, String password) {
        // policy is:
        // - length of 8 - 64 characters
        // - at least 3 of these:
        //      - upper case letter, e.g. A, B, C
        //      - lower case e.g. a, b, c
        //      - numerals e.g. 1, 2, 55
        //      - non-alphanumeric e.g. $, *, #, &
        // - must not be a commonly used password (which also accounts for some passwords that are entirely dictionary words)
        // - must not include the part of the email before the '@', as email addresses are used as usernames
        // - must not contain a sequence of 5 or more letters that appear next to each other on a US stand keyboard

        def passwordData = new PasswordData(username, password, PasswordData.Origin.User)

        // reference: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#implement-proper-password-strength-controls
        def ruleLength = new LengthRule(8, 64)

        // ensure password does not contain the user name
        def ruleUsername = new UsernameRule(false, true, MatchBehavior.Contains)

        // don't allow sequences of characters that are next to each other on a standard US qwerty keyboard
        def ruleSequence = new IllegalSequenceRule(EnglishSequenceData.USQwerty)

        // at least x of y
        def ruleGroup = new CharacterCharacteristicsRule()

        // at least 3 of the rules must be met
        ruleGroup.setNumberOfCharacteristics(3)

        // The rules that need to be met
        ruleGroup.getRules().addAll(getPasswordCharacterRules())

        // password must not be in the list of common passwords
        // source https://raw.githubusercontent.com/danielmiessler/SecLists/5e1dc9cc79aac54b373349e2a97bbb22f1b63bb3/Passwords/Common-Credentials/10-million-password-list-top-100000.txt
        def commonPassInputStream = this.class.classLoader.getResourceAsStream("common-passwords.txt")
        def commonPassInputStreamReaders = [new InputStreamReader(commonPassInputStream)] as Reader[]
        def caseSensitive = false
        def sorting = new ArraysSort()
        def commonPasswordsList = WordLists.createFromReader(commonPassInputStreamReaders, caseSensitive, sorting)
        def commonPasswordsDict = new WordListDictionary(commonPasswordsList)
        def ruleCommonPasswords = new DictionaryRule(commonPasswordsDict)

        def validator = new PasswordValidator(ruleLength, ruleUsername, ruleSequence, ruleGroup, ruleCommonPasswords)

        // at least 20 bits of entropy
        // not enforcing this because this measures the entropy of the password generation process, which we don't control
        // reference https://csrc.nist.gov/publications/detail/sp/800-63b/final
        double entropy
        try {
            entropy = validator.estimateEntropy(passwordData)
        } catch (Exception ignored) {
            entropy = 0
        }

        def result = validator.validate(passwordData)
        return [
                valid   : result.valid,
                metadata: result.metadata,
                details : result.details,
                entropy : entropy
        ]
    }

    /**
     * Generate a new password that meets the password policy.
     * @return A new password.
     */
    String generateNewPassword(String username) {
        def defaultPasswordLength = 10
        def maxCount = 10
        def generator = new PasswordGenerator()

        String newPassword = null
        def isValid = false
        def count = 0
        def passwordRules = getPasswordCharacterRules()
        while (!isValid && count < maxCount) {
            newPassword = generator.generatePassword(defaultPasswordLength, passwordRules)
            isValid = validatePassword(username, newPassword)
            count += 1
        }
        if (!isValid) {
            def msg = "Tried ${maxCount} times to generate a new password for user name '${username}', but none of the passwords satisfied the rules."
            log.error(msg)
            throw new IllegalStateException(msg)
        } else {
            log.warn("Generated a new password with length ${defaultPasswordLength} for user name '${username}'.")
        }
        return newPassword
    }

    /**
     * Get the list of password character rules.
     * @return The password character rules.
     */
    List<CharacterRule> getPasswordCharacterRules() {
        return [
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCustomCharacterData.Special, 1),
        ]
    }

    private boolean isBcrypt() {
        return passwordEncoderType.equalsIgnoreCase(BCRYPT_ENCODER_TYPE)
    }

    private getPasswordType() {
        return isBcrypt() ? BCRYPT_ENCODER_TYPE : LEGACY_ENCODER_TYPE
    }
}
