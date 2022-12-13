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

import au.org.ala.auth.PreAuthorise
import au.org.ala.users.AuthorisedSystem
import grails.converters.JSON
import org.springframework.dao.DataIntegrityViolationException

@PreAuthorise
class AuthorisedSystemController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        redirect(action: "list", params: params)
    }

    def list(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def list = []
        def count = 0
        def query = params.q as String
        if (query) {
            def c = AuthorisedSystem.createCriteria()
            list = c.list(params) {
                or {
                    ilike('host', "%${query}%")
                    ilike('description', "%${query}%")
                }
            }
            count = list.totalCount
        } else {
            list = AuthorisedSystem.list(params)
            count = AuthorisedSystem.count()
        }

        [authorisedSystemInstanceList: list, authorisedSystemInstanceTotal: count]
    }

    def create() {
        [authorisedSystemInstance: new AuthorisedSystem(params)]
    }

    def save() {
        def authorisedSystemInstance = new AuthorisedSystem(params)
        if (!authorisedSystemInstance.save(flush: true)) {
            render(view: "create", model: [authorisedSystemInstance: authorisedSystemInstance])
            return
        }

        flash.message = message(code: 'default.created.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), authorisedSystemInstance.id])
        redirect(action: "show", id: authorisedSystemInstance.id)
    }

    def show(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        if (!authorisedSystemInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
            return
        }

        [authorisedSystemInstance: authorisedSystemInstance]
    }

    def edit(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        if (!authorisedSystemInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
            return
        }

        [authorisedSystemInstance: authorisedSystemInstance]
    }

    def update(Long id, Long version) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        if (!authorisedSystemInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
            return
        }

        if (version != null) {
            if (authorisedSystemInstance.version > version) {
                authorisedSystemInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem')] as Object[],
                          "Another user has updated this AuthorisedSystem while you were editing")
                render(view: "edit", model: [authorisedSystemInstance: authorisedSystemInstance])
                return
            }
        }

        authorisedSystemInstance.properties = params

        if (!authorisedSystemInstance.save(flush: true)) {
            render(view: "edit", model: [authorisedSystemInstance: authorisedSystemInstance])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), authorisedSystemInstance.id])
        redirect(action: "show", id: authorisedSystemInstance.id)
    }

    def delete(Long id) {
        def authorisedSystemInstance = AuthorisedSystem.get(id)
        if (!authorisedSystemInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
            return
        }

        try {
            authorisedSystemInstance.delete(flush: true)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'authorisedSystem.label', default: 'AuthorisedSystem'), id])
            redirect(action: "show", id: id)
        }
    }

    def ajaxResolveHostName() {

        def host = params.host as String

        def hostname = "?"
        def reachable = false
        if (host) {
            try {
                InetAddress addr = InetAddress.getByName(host);
                hostname = addr.getHostName();
                reachable = addr.isReachable(2000);
            } catch (Exception ex) {
                ex.printStackTrace()
            }
        }

        render([host:host, hostname: hostname, reachable: reachable] as JSON)
    }
}
