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
import grails.gorm.transactions.Transactional
import org.springframework.dao.DataIntegrityViolationException

@PreAuthorise
class UserController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
    def userService

    def index() {
        redirect(action: "list", params: params)
    }

    @PreAuthorise(requiredRoles = ["ROLE_ADMIN", "ROLE_BIOSECURITY_ADMIN"])
    def list(Integer max) {
        if (params.q) {
            def q = "%"+ params.q + "%"
            def userList = User.findAllByEmailLikeOrLastNameLikeOrFirstNameLike(q,q,q)
            [userInstanceList: userList, userInstanceTotal: userList.size(), q:params.q]
        } else {
            params.max = Math.min(max ?: 20, 5000)
            [userInstanceList: User.list(params), userInstanceTotal: User.count()]
        }
    }

    @PreAuthorise(requiredRoles = ["ROLE_ADMIN", "ROLE_BIOSECURITY_ADMIN"])
    def create() {
        def isBiosecurityAdmin = request.isUserInRole("ROLE_BIOSECURITY_ADMIN")
        [userInstance: new User(params), isBiosecurityAdmin: isBiosecurityAdmin]
    }

    @PreAuthorise(requiredRoles = ["ROLE_ADMIN", "ROLE_BIOSECURITY_ADMIN"])
    @Transactional
    def save() {
        def userInstance = new User(params)
        if (params.locked == null) userInstance.locked = false
        if (params.activated == null) userInstance.activated = false
        // Keep the username and email address in sync
        userInstance.userName = userInstance.email

        if (!userInstance.save(flush: true)) {
            render(view: "create", model: [userInstance: userInstance])
            return
        }

        userService.updateProperties(userInstance, params)
        userService.addUserRole(userInstance, "ROLE_USER")

        flash.message = message(code: 'default.created.message', args: [message(code: 'user.label', default: 'User'), userInstance.id])
        redirect(action: "show", id: userInstance.id)
    }

    @PreAuthorise(requiredRoles = ["ROLE_ADMIN", "ROLE_BIOSECURITY_ADMIN"])
    def show(Long id) {
        def userInstance = User.get(id)
        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), id])
            redirect(action: "list")
            return
        }

        String resetPasswordUrl = userService.getResetPasswordUrl(userInstance)
        def isBiosecurityAdmin = request.isUserInRole("ROLE_BIOSECURITY_ADMIN")

        [userInstance: userInstance, resetPasswordUrl: resetPasswordUrl, isBiosecurityAdmin: isBiosecurityAdmin]
    }

    def edit(Long id) {
        def userInstance = User.get(id)
        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), id])
            redirect(action: "list")
            return
        }
        [userInstance: userInstance, props:userInstance.propsAsMap()]
    }

    @Transactional
    def update(Long id, Long version) {
        def userInstance = User.get(id)
        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), id])
            redirect(action: "list")
            return
        }

        if (version != null) {
            if (userInstance.version > version) {
                userInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'user.label', default: 'User')] as Object[],
                          "Another user has updated this User while you were editing")
                render(view: "edit", model: [userInstance: userInstance])
                return
            }
        }

        if (userInstance.email != params.email) {
            params.userName = params.email
        }

        userInstance.properties = params

        if (!userInstance.save(flush: true)) {
            render(view: "edit", model: [userInstance: userInstance])
            return
        }

        userService.updateProperties(userInstance, params)

        flash.message = message(code: 'default.updated.message', args: [message(code: 'user.label', default: 'User'), userInstance.id])
        redirect(action: "show", id: userInstance.id)
    }

    def delete(Long id) {

        def userInstance = User.get(id)

        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), id])
            redirect(action: "list")
            return
        }

        try {
            log.info("${request.userPrincipal?.name} is attempting to delete user ${userInstance.userName}")
            userService.deleteUser(userInstance)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'user.label', default: 'User'), id])
            redirect(action: "list")
        } catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'user.label', default: 'User'), id])
            redirect(action: "show", id: id)
        }

    }
}
