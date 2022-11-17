%{--
  - Copyright (C) 2022 Atlas of Living Australia
  - All Rights Reserved.
  -
  - The contents of this file are subject to the Mozilla Public
  - License Version 1.1 (the "License"); you may not use this file
  - except in compliance with the License. You may obtain a copy of
  - the License at http://www.mozilla.org/MPL/
  -
  - Software distributed under the License is distributed on an "AS
  - IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
  - implied. See the License for the specific language governing
  - rights and limitations under the License.
  --}%
<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}"/>
    <meta name="section" content="home"/>
    <title>User Administration | ${grailsApplication.config.getProperty('skin.orgNameLong')}</title>
    <meta name="breadcrumbParent" content="${g.createLink(controller: 'profile')},My Profile" />
    <asset:stylesheet src="application.css" />
</head>
<body>
    <div class="row">
        <div class="col-md-12" id="page-body" role="main">
            <h1>User Administration</h1>
            <ul class="userdetails-menu">
                <li><g:link controller="user" action="list">Find a user</g:link></li>
                <li><g:link controller="admin" action="resetPasswordForUser">Reset user password</g:link></li>
                <li><g:link controller="role" action="list">Roles</g:link></li>
                <g:if test="${grailsApplication.config.getProperty('attributes.authorisedSystem.enabled', Boolean, false)}">
                    <li><g:link controller="authorisedSystem" action="list">Authorised systems</g:link></li>
                </g:if>
                <li><g:link controller="alaAdmin" action="index">ALA admin page</g:link></li>
            </ul>
        </div>
    </div>
</body>
</html>
