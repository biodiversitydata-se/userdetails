package au.org.ala.userdetails

import au.org.ala.cas.encoding.BcryptPasswordEncoder
import grails.gorm.transactions.Transactional
import org.mindrot.jbcrypt.BCrypt
import org.springframework.beans.factory.annotation.Value

@Transactional
class ApiKeyService {

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
    String generateToken(String suppliedApiKey, String suppliedApiKeySecret){
        ApiKey apiKey = ApiKey.findByApiKey(suppliedApiKey)
        if (apiKey){
            def encoder = new BcryptPasswordEncoder(bcryptStrength)
            def encodedSecret = encoder.encode(suppliedApiKeySecret)
            if (BCrypt.checkpw(suppliedApiKeySecret, apiKey.apiSecret)){
                jwtService.generateJwt(apiKey.user)
            } else {
                null
            }
        } else {
            null
        }
    }
}
