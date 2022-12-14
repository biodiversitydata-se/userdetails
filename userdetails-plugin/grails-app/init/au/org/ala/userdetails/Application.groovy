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
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.PluginSource
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import org.springframework.boot.actuate.mongo.MongoHealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Slf4j
@PluginSource
class Application extends GrailsAutoConfiguration {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
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

    @Bean('authorisedSystemRepository')
    @ConditionalOnMissingBean(name = 'authorisedSystemRepository')
    IAuthorisedSystemRepository authorisedSystemRepository() {
        new ConfigAuthorisedSystemRepository(grailsApplication: grailsApplication)
    }

}