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

package au.org.ala.userdetails.cognito

import au.org.ala.userdetails.*
import au.org.ala.ws.tokens.TokenService
import com.amazonaws.auth.*
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Bean

@Slf4j
class Application extends GrailsAutoConfiguration {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    AWSCredentialsProvider awsCredentialsProvider() {

        String accessKey = grailsApplication.config.getProperty('cognito.accessKey')
        String secretKey = grailsApplication.config.getProperty('cognito.secretKey')
        String sessionToken = grailsApplication.config.getProperty('cognito.sessionToken')

        AWSCredentialsProvider credentialsProvider
        if (accessKey && secretKey) {
            AWSCredentials credentials
            if (sessionToken) {
                credentials = new BasicSessionCredentials(accessKey, secretKey, sessionToken)
            } else {
                credentials = new BasicAWSCredentials(accessKey, secretKey)
            }
            credentialsProvider = new AWSStaticCredentialsProvider(credentials)
        } else {
            credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance()
        }
        return credentialsProvider
    }

    @Bean
    AWSCognitoIdentityProvider cognitoIdpClient(AWSCredentialsProvider awsCredentialsProvider) {
        def region = grailsApplication.config.getProperty('cognito.region')

        AWSCognitoIdentityProvider cognitoIdp = AWSCognitoIdentityProviderClientBuilder.standard()
                .withRegion(region)
                .withCredentials(awsCredentialsProvider)
                .build()

        return cognitoIdp
    }

    @Bean('userService')
    IUserService userService(TokenService tokenService, EmailService emailService, AWSCognitoIdentityProvider cognitoIdp) {

        CognitoUserService userService = new CognitoUserService()
        userService.cognitoIdp = cognitoIdp
        userService.poolId = grailsApplication.config.getProperty('cognito.poolId')

        userService.emailService = emailService
        userService.tokenService = tokenService

        userService.affiliationsEnabled = grailsApplication.config.getProperty('attributes.affiliations.enabled', Boolean, false)

        return userService
    }

    @Bean('passwordOperations')
    IPasswordOperations passwordOperations(AWSCognitoIdentityProvider cognitoIdp) {
        CognitoPasswordOperations cognitoPasswordOperations = new CognitoPasswordOperations()

        cognitoPasswordOperations.cognitoIdp = cognitoIdp
        cognitoPasswordOperations.poolId = grailsApplication.config.getProperty('cognito.poolId')
        cognitoPasswordOperations.grailsApplication = grailsApplication

        return cognitoPasswordOperations
    }
}