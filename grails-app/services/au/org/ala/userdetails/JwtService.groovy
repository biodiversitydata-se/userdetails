package au.org.ala.userdetails

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Value

import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAMultiPrimePrivateCrtKeySpec
import java.security.spec.RSAPrivateKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit

@Transactional
class JwtService {

    @Value('${jwk.filePath}')
    String jwkFilePath

    @Value('${jwk.url}')
    String jwkUrl

    @Value('${jwk.keyId}')
    String keyId

    @Value('${jwt.iss}')
    String iss

    @Value('${jwt.aud}')
    String aud

    @Value('${jwt.expiryInMins}')
    Integer expiryInMins

    @Value('${jwt.notBeforeProcessingInMins}')
    Integer notBeforeProcessingInMins

    def serviceMethod() {}

    /**
     * Example...
         {
             "jti": "ST-26-Rklj-CSPRLUlg5tuX9iXN2-4Fj8nectar-auth-dev.ala.org.au",
             "nonce": "7LMmljh1PWrNxfMuxzDgfaI9r056_h3PHkD6ehimSbQ",
             "at_hash": "GRtIZKvHLv4W88oupfdP_Q",
             "iss": "https://nectar-auth-dev.ala.org.au/cas/oidc",
             "aud": "oidc-test-client-id",
             "sub": "david.martin@csiro.au",
             "state": "ACT",
             "authority": "ROLE_ADMIN,ROLE_USER",
             "city": "Canberra",
             "country": "AU",
             "email": "david.martin@csiro.au",
             "family_name": "Martin",
             "given_name": "David",
             "organisation": "CSIRO",
             "role": [
             "ROLE_ADMIN",
             "ROLE_USER"
             ],
             "preferred_username": "oidc-test-client-id"
         }
     */
    String generateJwt(User user) throws Exception {
        try {
            JwkProvider provider = new UrlJwkProvider(new URL(jwkUrl));
            Jwk jwk = provider.get(keyId)

            Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Instant expiration = issuedAt.plus(expiryInMins, ChronoUnit.MINUTES);
            Instant nbf = issuedAt.plus(notBeforeProcessingInMins, ChronoUnit.MINUTES);

            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), getPrivateKey());
            String token = JWT.create()
                    .withIssuer(iss)
                    .withKeyId(keyId)
                    .withClaim("iat", Date.from(issuedAt))
                    .withClaim("exp", Date.from(expiration))
                    .withClaim("nbf", Date.from(nbf))
                    .withClaim("email", user.email)
                    .withClaim("sub", user.email)
                    .withClaim("authority", user.userRoles.collect {it.role.role}.join(","))
                    .withClaim("aud", aud) // this corresponds to AppClient in Cognito or Service in CAS
                    .withClaim("city", user.propsAsMap().get("city"))
                    .withClaim("state", user.propsAsMap().get("state"))
                    .withClaim("country", user.propsAsMap().get("country"))
                    .withClaim("organisation", user.propsAsMap().get("organisation"))
                    .withClaim("family_name", user.lastName)
                    .withClaim("given_name", user.firstName)
                    .withClaim("preferred_username", user.userName)
                    .withClaim("userid", user.id)
                    .withClaim("role", user.userRoles.collect {it.role.role})
                    .sign(algorithm);

            token
        } catch (JWTCreationException exception){
            //Invalid Signing configuration / Couldn't convert Claims.
            throw new Exception(exception)
        }
    }

    /**
     * For details of magic strings below see: https://tools.ietf.org/html/rfc7518#section-6.3
     * @return
     */
    RSAPrivateKey getPrivateKey(){

        try {

            String jwk = new File(jwkFilePath).text

            def json = new JsonSlurper().parseText(jwk)
            Map<String, Object> additionalAttributes = json.keys[0];
            KeyFactory kf = KeyFactory.getInstance("RSA");

            BigInteger modules = getValues(additionalAttributes, "n");
            BigInteger publicExponent = getValues(additionalAttributes, "e");
            BigInteger privateExponent = getValues(additionalAttributes, "d");
            BigInteger primeP = getValues(additionalAttributes, "p");
            BigInteger primeQ = getValues(additionalAttributes, "q");
            BigInteger primeExponentP = getValues(additionalAttributes, "dp");
            BigInteger primeExponentQ = getValues(additionalAttributes, "dq");
            BigInteger crtCoefficient = getValues(additionalAttributes, "qi");

            RSAPrivateKeySpec privateKeySpec = new RSAMultiPrimePrivateCrtKeySpec(
                    modules,
                    publicExponent,
                    privateExponent,
                    primeP,
                    primeQ,
                    primeExponentP,
                    primeExponentQ,
                    crtCoefficient,
                    null
            );

            return (RSAPrivateKey) kf.generatePrivate(privateKeySpec);

        } catch (JwkException e) {
            // The only way to get here is if jwkProvider.get(null) throws an exception, i.e. there are no JWKs in the provider.
            // At this point we (in my company) have already verified that this is not the case.
            throw new RuntimeException("Unreachable");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    private BigInteger getValues(Map<String, Object> additionalAttributes, String key) {
        return new BigInteger(1, Base64.getUrlDecoder().decode((String) additionalAttributes.get(key)));
    }
}
