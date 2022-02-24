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

import javax.servlet.http.HttpServletRequest

class AuthorisedSystemService {

    def isAuthorisedSystem(HttpServletRequest request){
        def host = request.getHeader("x-forwarded-for")
        if(host == null){
          host = request.getRemoteHost()
        }
        log.debug("RemoteHost: " + request.getRemoteHost())
        log.debug("RemoteAddr: " + request.getRemoteAddr())
        log.debug("host using: " + host)

        return host != null && AuthorisedSystem.findByHost(host)
    }
}
