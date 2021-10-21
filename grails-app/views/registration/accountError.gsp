<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="account.error.title" args="[grailsApplication.config.skin.orgNameShort]" /></title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1>Account Error</h1>
    <div class="row">
        <p>
            <g:message code="account.error.there.was.problem" /><br>
            <g:message code="account.error.contact.mailto" args="[grailsApplication.config.supportEmail]" />
        </p>
        <g:if test="${msg}"><p><h4>Error:</h4> <pre>${msg}</pre></p></g:if>
   </div>
</div>
</body>
</html>
