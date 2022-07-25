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
                <li><g:link controller="authorisedSystem" action="list">Authorised systems</g:link></li>
                <li><g:link controller="admin" action="bulkUploadUsers">Bulk create user accounts</g:link></li>
                <li><g:link controller="admin" action="exportUsers">Export users to CSV file</g:link></li>
                <g:if test="${grailsApplication.config.getProperty('attributes.affiliations.enabled', Boolean, false)}">
                    <li><g:link controller="admin" action="surveyResults">Get user survey results</g:link></li>
                </g:if>
                <li><g:link controller="alaAdmin" action="index">ALA admin page</g:link></li>
            </ul>

            <h2>Web services (HTTP POST)</h2>
            <ul class="userdetails-menu">
                <li>${createLink(controller:'userDetails', action: 'getUserDetails')}?userName=&lt;email_or_userid&gt;[&amp;includeProps=true] - return a JSON object containing id, email and display name for a given user, use includeProps=true to get additional information such as organisation</li>
                <li>${createLink(controller:'userDetails', action: 'getUserList')} - return a JSON map of user email address to display name</li>
                <li>${createLink(controller:'userDetails', action: 'getUserListWithIds')} - return a JSON map of user ids to display name</li>
                <li>${createLink(controller:'userDetails', action: 'getUserListFull')} - return all users as a JSON array</li>
                <li>${createLink(controller:'userDetails', action: 'getUserDetailsFromIdList')} - return the details for a list of user ids.  POST a JSON body like this:
                    <pre>{
  "userIds": ["1","2"], // list of numeric user ids to retrieve
  "includeProps": true // optional, set to true to include organisation, primary usage, etc
}</pre>
                    This will return a JSON doc like this:
                    <pre>{
  "users": {
    "1":{
      "userId":"1",
      "userName":"user@email.address",
      "firstName":"User Given Name",
      "lastName":"User Surname",
      "email":"user@email.address",
      "props":{
        "organisation":"User Organisation",
        "telephone":"555-123456",
        "city":"User City",
        "state":"User State",
        "country": "AU"
      }
    }
  },
  "invalidIds":[2],
  "success":true
}</pre>
                </li>
            </ul>
            <h2>Web services (HTTP GET)</h2>
            <ul class="userdetails-menu">
                <li><a href="${createLink(uri:'/ws/getUserStats')}">${createLink(uri:'/ws/getUserStats')}</a> - a public web service that returns a JSON object containing a 'description' of the service, 'totalUsers' and 'totalUsersOneYearAgo' counts.</li>
            </ul>
        </div>
    </div>
</body>
</html>
