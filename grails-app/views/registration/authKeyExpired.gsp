<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="auth.key.expired.title" /></title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1><g:message code="auth.key.expired.header" /></h1>
    <div class="row">
        <p>
            <g:message code="auth.key.expired.initial.description" /><br/>
            If you have completed the <g:link controller="registration" action="forgottenPassword">Reset your password</g:link> form recently, please check your email for a new email with a link that will take your through to page where you can provide a new password.
            <br/>
            Alternatively you can start the process again <g:link controller="registration" action="forgottenPassword">here</g:link>.

            <br/>
            <g:message code="auth.key.expired.mailto" args="[grailsApplication.config.supportEmail]" />
        </p>
   </div>
</div>
</body>
</html>
