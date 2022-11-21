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
import au.org.ala.users.User
import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException

@PreAuthorise
class UserController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    @Autowired
    @Qualifier('userService')
    IUserService userService

    def index() {
        redirect(action: "list", params: params)
    }

    def list(Integer max) {

        String paginationToken = params.paginationToken
        int maxResults = Math.min(max ?: 20, 5000)

        if (params.q) {

            def userList = userService.listUsers(params.q, paginationToken, maxResults)
//            def userList = User.findAllByEmailLikeOrLastNameLikeOrFirstNameLike(q,q,q)
            [ userInstanceList: userList, userInstanceTotal: userList.size(), q: params.q ]

        } else {

            def userList = userService.listUsers(null, paginationToken, maxResults)

            [ userInstanceList: userList, userInstanceTotal: userList.size() ]
//            [ userInstanceList: User.list(params), userInstanceTotal: User.count() ]
        }
    }

    def create() {
        [userInstance: new User()]
    }

    @Transactional
    def save() {
        User user = userService.registerUser(params)

        if (!user) {
            render(view: "create", model: [userInstance: new User()])
            return
        }
        userService.sendAccountActivation(user)

        flash.message = message(code: 'default.created.message', args: [message(code: 'user.label', default: 'User'), user.id])
        redirect(action: "show", id: user.id)
    }

    def show(String id) {

        def userInstance = userService.getUserById(id)

        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), id])
            redirect(action: "list")
            return
        }

        String resetPasswordUrl = userService.getResetPasswordUrl(userInstance)

        [userInstance: userInstance, resetPasswordUrl: resetPasswordUrl]
    }

    def edit(String id) {
        def userInstance = userService.getUserById(id)
        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), id])
            redirect(action: "list")
            return
        }
        [userInstance: userInstance, props:userInstance.propsAsMap()]
    }

    def update(String id, Long version) {

        def userInstance= userService.getUserById(id)

        if (!userInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), id])
            redirect(action: "list")
            return
        }

        // TODO: deal with optimistic locking
//        if (version != null) {
//            if (userInstance.version > version) {
//                userInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
//                          [message(code: 'user.label', default: 'User')] as Object[],
//                          "Another user has updated this User while you were editing")
//                render(view: "edit", model: [userInstance: userInstance])
//                return
//            }
//        }
//
//        if (userInstance.email != params.email) {
//            params.userName = params.email
//        }

        def success = userService.updateUser(id, params)

        if (!success) {
            render(view: "edit", model: [ userInstance: userInstance ])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'user.label', default: 'User'), userInstance.id])
        redirect(action: "show", id: userInstance.id)
    }

    def delete(Long id) {

        def userInstance = userService.getUserById(id as String)

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

    def disableMfa() {
        userService.enableMfa(params.userId, false)
        redirect(action: "edit", id: params.userId)
    }
}
