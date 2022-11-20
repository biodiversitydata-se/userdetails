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

import au.org.ala.auth.PreAuthorise
import au.org.ala.users.Role
import au.org.ala.users.User
import com.opencsv.CSVWriterBuilder
import com.opencsv.RFC4180ParserBuilder
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

@PreAuthorise
class AdminController {

    def passwordService
    def emailService
    def userService
    def exportService
    def profileService
    def authorisedSystemService

    def index() {}

    def resetPasswordForUser(){
    }

    def sendPasswordResetEmail(){

        def user = userService.getUserById(params.email)
        if (user) {
            def password = passwordService.generatePassword(user)
            //email to user
            emailService.sendGeneratedPassword(user, password)
            render(view:'userPasswordResetSuccess', model:[email:params.email, password: password])
       } else {
           render(view:'resetPasswordForUser', model:[email:params.email, emailNotRecognised:true])
       }
    }

    def surveyResults() {
        def results = userService.countByProfileAttribute('affiliation', null, request.locale)
        def csvWriter = new CSVWriterBuilder(response.writer)
                .withParser(new RFC4180ParserBuilder().build())
                .build()
        response.status = 200
        response.contentType = 'text/csv'
        response.setHeader('Content-Disposition', "attachment; filename=user-survey-${new Date()}.csv")
        csvWriter.writeAll(results)
        csvWriter.flush()
    }

}
