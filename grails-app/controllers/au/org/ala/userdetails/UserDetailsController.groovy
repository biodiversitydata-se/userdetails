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

import au.org.ala.cas.encoding.CloseShieldWriter
import au.org.ala.userdetails.marshaller.UserMarshaller
import grails.converters.JSON
import org.hibernate.ScrollableResults

class UserDetailsController {

    static allowedMethods = [getUserDetails: "POST", getUserList: "POST", getUserListWithIds: "POST", getUserListFull: "POST", getUserDetailsFromIdList: "POST"]

    def index() {}

    def search() {
        def q = params['q']
        def max = params.int('max', 10)
        User.withStatelessSession { session ->
            def c = User.createCriteria()
            ScrollableResults results = c.scroll {
                or {
                    ilike('userName', "%$q%")
                    ilike('email', "%$q%")
                    ilike('displayName', "%$q%")
                }
                maxResults(max)
            }
            streamResults(session, results, UserMarshaller.WITH_PROPERTIES_CONFIG)
        }
    }

    def byRole() {
        def ids = params.list('id')
        def roleName = params.get('role', 'ROLE_USER')
        def includeProps = params.boolean('includeProps', false)

        def things = ids.groupBy { it.isLong() }
        def userIds = things[false]
        def numberIds = things[true]

        // stream the results just in case someone requests ROLE_USER or something
        User.withStatelessSession { session ->
            Role role = Role.findByRole(roleName)
            if (!role) {
                response.sendError(404, "Role not found")
                return
            }

            def c = User.createCriteria()
            ScrollableResults results = c.scroll {
                or {
                    if (numberIds) {
                        inList('id', numberIds*.toLong())
                    }
                    if (userIds) {
                        inList('userName', userIds)
                        inList('email', userIds)
                    }
                }
                userRoles {
                    eq("role", role)
                }
            }

            streamResults(session, results, includeProps ? UserMarshaller.WITH_PROPERTIES_CONFIG : 'default')
        }
    }

    private streamResults(session, ScrollableResults results, String jsonConfig) {
        response.contentType = 'application/json'
        response.characterEncoding = 'UTF-8'
        def csw = new CloseShieldWriter(new BufferedWriter(response.writer))
        def first = true
        csw.print('[')
        if (jsonConfig) JSON.use(jsonConfig)
        else JSON.use('default')
        try {
            int count = 0

            while (results.next()) {
                if (!first) {
                    csw.print(',')
                } else {
                    first = false
                }
                User user = ((User)results.get()[0])

                (user as JSON).render(csw)

                if (count++ % 50 == 0) {
                    session.flush()
                    session.clear()
                }

            }
        } finally {
            JSON.use('default')
        }
        csw.print(']')
        csw.flush()
        response.flushBuffer()
    }

    def getUserDetails() {

        def user
        String userName = params.userName as String
        def includeProps = params.getBoolean('includeProps', false)

        if (userName) {
            if (userName.isLong()) {
                user = User.findById(userName.toLong())
            } else {
                user = User.findByUserNameOrEmail(userName, userName)
            }
        } else {
            render status:400, text: "Missing parameter: userName"
            return
        }

        if (user == null) {
            render status:404, text: "No user found for: ${userName}"
        } else {
            String jsonConfig = includeProps ? UserMarshaller.WITH_PROPERTIES_CONFIG : null
            try {
                JSON.use(jsonConfig)
                render user as JSON
            }
            finally {
                JSON.use(null) // resets to default config
            }
        }

    }

    def getUserList() {
        def users = User.findNameAndEmailWhereEmailIsNotNull()
        def map = users.collectEntries { [(it[0].toLowerCase()): "${it[1]?:""} ${it[2]?:""}"]}
        render(map as JSON, contentType: "application/json")
    }

    def getUserListWithIds() {
        def users = User.findIdFirstAndLastName()
        def map = users.collectEntries { [(it[0]), "${it[1]?:""} ${it[2]?:""}"] }
        render(map as JSON, contentType: "application/json")
    }

    def getUserListFull() {
        def details = User.findUserDetails().collect {
            [id: it[0], firstName: it[1]?:"", lastName: it[2]?:"", userName: it[3]?:"", email: it[4]?:""]
        }
        render(details as JSON, contentType: "application/json")
    }

    def getUserDetailsFromIdList() {

        def req = request.JSON
        def includeProps = req?.includeProps ?: params.getBoolean('includeProps', false)

        if (req && req.userIds) {

            try {
                List<Long> idList = req.userIds.collect { userId -> userId as long }

                def c = User.createCriteria()
                def results = c.list() {
                    'in'("id", idList)
                }
                String jsonConfig = includeProps ? UserMarshaller.WITH_PROPERTIES_CONFIG : null
                try {

                    JSON.use(jsonConfig)

                    def resultsMap = [users:[:], invalidIds:[], success: true]
                    results.each { user ->
                        resultsMap.users[user.id] = user
                    }

                    idList.each {
                        if (!resultsMap.users[it]) {
                            resultsMap.invalidIds << it
                        }
                    }

                    render resultsMap as JSON
                }
                finally {
                    JSON.use(null) // Reset to default
                }
            } catch (Exception ex) {
                render([success: false, message: "Exception: ${ex.toString()}"] as JSON, contentType: "application/json")
            }
        } else {
            render([success: false, message: "Body must contain JSON map payload with 'userIds' key that contains a list of user ids"] as JSON, contentType: "application/json")
        }

    }
}