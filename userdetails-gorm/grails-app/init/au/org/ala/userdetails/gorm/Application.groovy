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

package au.org.ala.userdetails.gorm

import au.org.ala.userdetails.EmailService
import au.org.ala.userdetails.IAuthorisedSystemRepository
import au.org.ala.userdetails.IPasswordOperations
import au.org.ala.userdetails.IUserService
import au.org.ala.userdetails.LocationService
import au.org.ala.userdetails.PasswordService
import au.org.ala.web.AuthService
import au.org.ala.ws.service.WebService
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean

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

    @Bean('userService')
    IUserService userService(GrailsApplication grailsApplication,
                             EmailService emailService,
                             PasswordService passwordService,
                             AuthService authService,
                             LocationService locationService,
                             MessageSource messageSource,
                             WebService webService
                             ) {

//        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, UserRecord)
//        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, UserPropertyRecord)
//        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, UserRoleRecord)

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

    @Bean('authorisedSystemRepository')
    IAuthorisedSystemRepository authorisedSystemRepository(MessageSource messageSource) {
        new GormAuthorisedSystemRepository(messageSource)
    }

    @Bean('passwordOperations')
    IPasswordOperations passwordOperations(EmailService emailService) {
        new GormPasswordOperations(emailService: emailService)
    }
}