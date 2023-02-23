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
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}"/>
    <meta name="section" content="home"/>
    <meta name="breadcrumbParent" content="${g.createLink(controller: 'profile')},My Profile" />
    <title><g:message code="myprofile.myGalah" /></title>
    <asset:stylesheet src="userdetails.css" />
</head>
<body>

<div id="my-galah">
    <h1><g:message code="myprofile.myGalah" /></h1>
    <g:hasErrors>
        <div class="alert alert-danger">
            <g:eachError var="err">
                <p><g:message error="${err}"/></p>
            </g:eachError>
        </div>
    </g:hasErrors>
    <div class="row">
        <div class="col-md-2">
            <p><img src="https://raw.githubusercontent.com/AtlasOfLivingAustralia/ala-labs/main/images/hex/galah_logo.png" width="55%"></p>
        </div>
        <div class="col-md-10">
            <p><g:message code="myGalah.desc" /></p>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <h2><g:message code="myprofile.generate.apikey" /></h2>
            <g:form name="generateApikeyForm" controller="profile" action="generateApikey" params="[application:'galah']">
                <br/>
                <p><g:message code="generate.apikey.desc.1" /></p>
                <p><g:message code="generate.apikey.desc.2" /></p>
                <br/>
                <g:if test="${apikeys}">
                    <g:message code="my.apikey" /><code>${apikeys}</code>
                </g:if>
                <g:else>
                    <button id="submitResetBtn" class="btn btn-primary"><g:message code="myprofile.generate.apikey" /></button>
                </g:else>
            </g:form>
            <br/>
        </div>
    </div>
</div>
</body>
</html>