package au.org.ala.userdetails

import au.org.ala.cas.encoding.BcryptPasswordEncoder
import grails.gorm.transactions.Transactional
import org.mindrot.jbcrypt.BCrypt
import org.springframework.beans.factory.annotation.Value

@Transactional
class UserApiKeyService {

    def jwtService

    @Value('${bcrypt.strength}')
    Integer bcryptStrength = 10

    def serviceMethod() {}

    def getApiKeyForUser(User user){
        ApiKey apiKey = ApiKey.findByUser(user)
        if (!apiKey){
            apiKey = new ApiKey(user: user,
                    apiKey: UUID.randomUUID().toString()
            )
            apiKey.save(flush:true)
        }
        apiKey
    }

    ApiKey getApiKey(String apiKey){
        ApiKey.findByApiKey(apiKey)
    }

    String resetSecretForApiKey(ApiKey apiKey){
        def encoder = new BcryptPasswordEncoder(bcryptStrength)
        String apiKeySecret = UUID.randomUUID().toString()
        def encodedPassword = encoder.encode(apiKeySecret)
        apiKey.setApiSecret(encodedPassword)
        apiKey.save(flush:true)
        // return the unencoded secret
        apiKeySecret
    }

    /**
     * Check the validity of the key and secret and generate a JWT if good.
     *
     * @param suppliedApiKey
     * @param suppliedApiKeySecret
     * @return
     */
    Map generateTokenApiKeySecret(String suppliedApiKey, String suppliedApiKeySecret){
        ApiKey apiKey = ApiKey.findByApiKey(suppliedApiKey)
        if (apiKey){
            if (BCrypt.checkpw(suppliedApiKeySecret, apiKey.apiSecret)){
                try {
                    def jwt = jwtService.generateJwt(apiKey.user)
                    [statusCode: 200, jwt: jwt]
                } catch (Exception e){
                    [statusCode: 500]
                }
            } else {
                [statusCode: 400]
            }
        } else {
            [statusCode: 400]
        }
    }

    /**
     * Check the validity of the Authorization header and generate a JWT if good.
     *
     * @param suppliedApiKey
     * @param suppliedApiKeySecret
     * @return
     */
    Map generateTokenBasicAuth(String authorizationHeader){

        if (authorizationHeader.startsWith("Basic")){
            String base64encoded = authorizationHeader.substring(6).trim()
            String decoded = new String(Base64.getUrlDecoder().decode(base64encoded))
            int indexOfSep = decoded.indexOf(":")
            if (indexOfSep > 0){
                String[] parts = decoded.split(":")
                User user = User.findByEmail(parts[0])
                if (!user.getActivated()){
                    return [statusCode: 401, message: "Account not activated"]
                }
                if (user.getLocked()){
                    return [statusCode: 401, message: "Account locked"]
                }

                if (user){
                    def pw = Password.findByUser(user)
                    if (pw) {
                        if (BCrypt.checkpw(parts[1], pw.password)) {
                            try {
                                def jwt = jwtService.generateJwt(user)
                                return [statusCode: 200, jwt: jwt]
                            } catch (Exception e){
                                log.error("Problem generating JWT - check keystore - " + e.getMessage())
                                if (log.isDebugEnabled()){
                                    log.debug(e.getMessage(), e)
                                }
                                return [statusCode: 500]
                            }
                        }
                    } else {
                        log.info("Unable to find password for user ${user.id}")
                    }
                } else {
                    log.debug("Unable to find user for ${parts[0]}")
                }
            } else {
                log.debug("Badly formatted Authorization header")
            }
        } else {
            log.debug("Badly formatted Authorization header - should start with Basic")
        }
        [statusCode: 400]
    }
}
