<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="password.reset.success.title" /></title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1><g:message code="password.reset.success.header" /></h1>
    <div class="row">
        <div class="col-md-12">
            <p class="well">
                <g:message code="password.reset.success.description" />
            </p>
            <a href="${grailsApplication.config.security.cas.loginUrl}?service=${java.net.URLEncoder.encode(serverUrl, 'UTF-8')}" class="btn btn-primary"><g:message code="password.reset.success.btn" /></a>
        </div>
   </div>
</div>
</body>
</html>
