<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="userdetails.index.accounts" args="[grailsApplication.config.skin.orgNameShort]" /></title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <div class="col-md-12" id="page-body" role="main">

        <g:if test="${flash.errorMessage || flash.message}">
            <div class="col-md-12">
                <div class="alert alert-danger">
                    <button class="close" onclick="$('.alert').fadeOut();" href="#">×</button>
                    ${flash.errorMessage?:flash.message}
                </div>
            </div>
        </g:if>

        <h1><g:message code="userdetails.index.accounts" args="[grailsApplication.config.skin.orgNameShort]" /></h1>
        <ul class="userdetails-menu">
            <li><g:link controller="registration" action="createAccount"><g:message code="userdetails.index.create.new.account" /></g:link></li>
            <li><g:link controller="registration" action="forgottenPassword"><g:message code="userdetails.index.reset.password" /></g:link></li>
            <li><g:link controller="profile"><g:message code="userdetails.index.my.profile" /></g:link></li>
        </ul>

    </div>
    <auth:ifAllGranted roles="ROLE_ADMIN">
        <div style="color:white;" class="pull-right">
            <g:link style="color:#DDDDDD; font-weight:bold;" controller="admin"><g:message code="userdetails.index.admin.tools" args="[grailsApplication.config.skin.orgNameShort]" /></g:link>
        </div>
    </auth:ifAllGranted>
</div>
</body>
</html>
