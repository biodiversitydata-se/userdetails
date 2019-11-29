<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="duplicate.submit.title" /></title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1><g:message code="duplicate.submit.results" /></h1>
    <div class="row">
        <div class="alert alert-warning">
            <g:message code="duplicate.submit.description.new.password" />
        </div>

        <strong><g:message code="duplicate.submit.next.steps" /></strong>
        <ul class="userdetails-menu">
            <li>
                <g:message code="duplicate.submit.reset.successfully" args="[grailsApplication.config.security.cas.loginUrl, java.net.URLEncoder.encode(serverUrl, 'UTF-8')]" />
            </li>
            <li>
                If your new password doesn't work, please start the process again <g:link controller="registration" action="forgottenPassword">here</g:link>.
            </li>
            <li>
                <g:message code="duplicate.submit.mailto" args="[grailsApplication.config.supportEmail]" />
            </li>
        </ul>

   </div>
</div>
</body>
</html>
