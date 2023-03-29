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
    <title><g:message code="myprofile.myClientAndApikey" /></title>
    <asset:stylesheet src="userdetails.css" />
</head>
<body>

<div id="myClientAndApikey">
    <h1><g:message code="myprofile.myClientAndApikey" /></h1>
    <g:if test="${errors}">
        <div class="alert alert-danger">
            <g:each in="${errors}" var="err">
                <p><g:message error="${err}"/></p>
            </g:each>
        </div>
    </g:if>
    <g:if test="${grailsApplication.config.getProperty('oauth.support.dynamic.client.registration', Boolean, false)}">
        <div class="row">
            <div class="col-md-4">
                <h3><g:message code="myprofile.my.client" /></h3>
                <g:form name="generateApikeyForm" controller="profile" action="generateClient">
                    <p><g:message code="myclient.desc" /></p>
                    <g:if test="${clientId}">
                        <g:message code="my.client.id" /><code>${clientId}</code>
                        <div>
                            <br/>
                            <a href="${grailsApplication.config.getProperty('tokenApp.tokenGeneration.url')}&client_id=${clientId}" target="_blank">Click here to generate an access token</a>
                        </div>
                    </g:if>
                    <g:else>
                        <br/>
                        <h4><g:message code="myprofile.my.client.create" /></h4>
%{--                        <div class="form-group">--}%
%{--                            <label for="callbackURLs"><g:message code="myclient.callbackURLs" /></label>--}%
%{--                            <input id="callbackURLs" name="callbackURLs" type="text" class="form-control"/>--}%
%{--                        </div>--}%
                        <div class="form-group">
                            <div class="checkbox">
                                <label>
                                    <g:checkBox name="forGalah"/> Is this for Galah?
                                </label>
                            </div>
                        </div>
                        <button id="generateClient" class="btn btn-primary"><g:message code="myprofile.generate.client" /></button>
                    </g:else>
                </g:form>
            </div>
        </div>
        <br/>
    </g:if>
    <div class="row">
        <div class="col-md-12">
            <h3><g:message code="myprofile.my.apikey" /></h3>
            <h4><g:message code="myprofile.my.apikey.desc" /></h4>
            <g:form name="generateApikeyForm" controller="profile" action="generateApikey" params="[application:'galah']">
                <br/>
                <p><g:message code="generate.apikey.desc.1" /></p>
                <p><g:message code="generate.apikey.desc.2" /></p>
                <br/>
                <g:if test="${apikeys}">
                    <g:message code="my.apikey" /><code>${apikeys}</code>
                </g:if>
                <g:else>
                    <button id="generateApikey" class="btn btn-primary"><g:message code="myprofile.generate.apikey" /></button>
                </g:else>
            </g:form>
            <br/>
        </div>
    </div>
</div>
</body>
</html>