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
import au.org.ala.web.AuthService
import au.org.ala.web.IAuthService
import au.org.ala.ws.service.WebService
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.session.data.mongo.JdkMongoSessionConverter
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

import javax.sql.DataSource
import java.time.Duration
import java.util.stream.Collectors

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

//    @Configuration
//    static class CognitoConfiguration {

        // TODO replace with rotatable keys
        @Value('${cognito.accessKey}')
        String cognitoAccessKey
        @Value('${cognito.secretKey}')
        String cognitoSecretKey
        @Value('${cognito.poolId}')
        String poolId

    @Profile('cognito')
    @Bean
    AWSCognitoIdentityProviderClient cognitoIdpClient() {
        def backupAccessKey = grailsApplication.config.getProperty('cognito.accessKey')
        def backupSecretKey = grailsApplication.config.getProperty('cognito.secretKey')
        def sessionToken = grailsApplication.config.getProperty('cognito.sessionToken')
        def region = grailsApplication.config.getProperty('cognito.region')

        def accessKey = cognitoAccessKey ?: backupAccessKey
        def secretKey = cognitoSecretKey ?: backupSecretKey

        // TODO Talk to Joe/Matt about Credentials
        AWSCredentials credentials
        if (sessionToken) {
            credentials = new BasicSessionCredentials(accessKey, secretKey, sessionToken)
        } else {
            credentials = new BasicAWSCredentials(accessKey, secretKey)
        }
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials)


        def cognitoIdp = AWSCognitoIdentityProviderClientBuilder.standard()
                .withRegion(region)
                .withCredentials(credentialsProvider)
                .build()
        return cognitoIdp
    }

        @Profile('cognito')
        @Qualifier('userServiceImpl')
        @Bean
        IUserService cognitoUserService(AuthService authService, AWSCognitoIdentityProviderClient cognitoIdpClient) {
            def backupPoolId = grailsApplication.config.getProperty('cognito.poolId')
            new CognitoUserService(authService, cognitoIdpClient, poolId ?: backupPoolId)
        }

        @Profile('mysql')
        @Qualifier('userServiceImpl')
        @Bean
        IUserService mysqlUserService(EmailService emailService, PasswordService passwordService, AuthService authService,
                                      GrailsApplication grailsApplication, LocationService locationService, MessageSource messageSource,
                                      WebService webService) {
            new DbUserService(emailService, passwordService, authService,
                    grailsApplication, locationService, messageSource,
                    webService)
        }
//    }



}