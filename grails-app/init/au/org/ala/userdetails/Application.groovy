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

import au.org.ala.recaptcha.RecaptchaClient
import au.org.ala.users.User
import au.org.ala.users.UserProperty
import au.org.ala.users.UserRole
import au.org.ala.web.AuthService
import au.org.ala.ws.service.WebService
import au.org.ala.ws.tokens.TokenService
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import org.grails.core.artefact.DomainClassArtefactHandler
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator
import org.springframework.boot.actuate.mongo.MongoHealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

import javax.sql.DataSource

@Slf4j
class Application extends GrailsAutoConfiguration {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource) {
        new DataSourceHealthIndicator(dataSource)
    }

    @Bean
    RecaptchaClient recaptchaClient() {
        def baseUrl = grailsApplication.config.getProperty('recaptcha.baseUrl', 'https://www.google.com/recaptcha/api/')
        return new Retrofit.Builder().baseUrl(baseUrl).client(new OkHttpClient()).addConverterFactory(MoshiConverterFactory.create()).build().create(RecaptchaClient)
    }

    @Configuration
    @ConditionalOnProperty(value = "spring.session.enabled", havingValue = "true", matchIfMissing = false)
    @Import(MongoAutoConfiguration) // unsure if this is disabled by grails?
    @EnableMongoHttpSession
    @EnableConfigurationProperties(MongoProperties)
    static class MongoSessionConfig {

//        @Bean
//        JdkMongoSessionConverter jdkMongoSessionConverter() {
//            return new JdkMongoSessionConverter(Duration.ofMinutes(15L));
//        }
        @Bean // TODO is this necessary?
        MongoHealthIndicator mongoHealthIndicator(MongoTemplate mongoTemplate) {
            new MongoHealthIndicator(mongoTemplate)
        }
    }


    @Profile('cognito')
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

    @Profile('cognito')
    @Bean
    AWSCognitoIdentityProviderClient cognitoIdpClient(AWSCredentialsProvider awsCredentialsProvider) {
        def region = grailsApplication.config.getProperty('cognito.region')
        log.info(region)
        println(region)
        AWSCognitoIdentityProvider cognitoIdp = AWSCognitoIdentityProviderClientBuilder.standard()
                .withRegion(region)
                .withCredentials(awsCredentialsProvider)
                .build()

        return cognitoIdp
    }

    @Profile('cognito')
    @Bean('userService')
    IUserService cognitoUserService(TokenService tokenService, EmailService emailService, AWSCognitoIdentityProvider cognitoIdp) {

        CognitoUserService userService = new CognitoUserService()
        userService.cognitoIdp = cognitoIdp
        userService.poolId = grailsApplication.config.getProperty('cognito.poolId')

        userService.emailService = emailService
        userService.tokenService = tokenService
        userService.grailsApplication = grailsApplication

        userService.affiliationsEnabled = grailsApplication.config.getProperty('attributes.affiliations.enabled', Boolean, false)

        return userService
    }

    @Profile('gorm')
    @Bean('userService')
    IUserService gormUserService(GrailsApplication grailsApplication,
                                     EmailService emailService,
                                     PasswordService passwordService,
                                     AuthService authService,
                                     LocationService locationService,
                                     MessageSource messageSource,
                                     WebService webService) {

        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, User)
        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, UserProperty)
        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, UserRole)

        GormUserService userService = new GormUserService()
        userService.emailService = emailService
        userService.passwordService = passwordService
        userService.authService = authService
        userService.locationService = locationService
        userService.webService = webService

        userService.grailsApplication = grailsApplication
        userService.messageSource = messageSource

        userService.affiliationsEnabled = grailsApplication.config.getProperty('attributes.affiliations.enabled', Boolean, false)

        return userService
    }

}