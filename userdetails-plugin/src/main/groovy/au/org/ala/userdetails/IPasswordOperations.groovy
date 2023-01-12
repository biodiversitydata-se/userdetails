package au.org.ala.userdetails

import au.org.ala.auth.PasswordResetFailedException
import au.org.ala.users.UserRecord

/**
 * Extracts operations used by the PasswordService into a provider
 * specific class
 */
interface IPasswordOperations {

    /**
     * Resets a user's password
     * @param user The user
     * @param newPassword The new password
     * @param isPermanent Whether this is a permanent or temporary reset
     * @param confirmationCode The confirmation code provided by the user
     * @return True if the password was reset, false otherwise
     */
    boolean resetPassword(UserRecord user, String newPassword, boolean isPermanent, String confirmationCode)

    void resetAndSendTemporaryPassword(UserRecord user, String emailSubject, String emailTitle, String emailBody, String password) throws PasswordResetFailedException

    String getResetPasswordUrl(UserRecord user)

    String getPasswordResetView()

    /**
     * Check that a plain-text password matches a user's existing password.
     * @param user The user.
     * @param password The plain-text password to match.
     * @return True if the password matches the existing password, otherwise false.
     */
    boolean checkUserPassword(UserRecord user, String password)
}
