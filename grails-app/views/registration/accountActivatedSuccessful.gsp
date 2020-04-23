<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="account.activated.successful.title" /> | ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1>
        <g:message code="account.activated.successful.congratulations" />
    </h1>

    <div class="row">
        <div class="col-md-12">
            <div class="well well-lg">
                <g:message code="account.activated.successful.please.login" args="[grailsApplication.config.security.cas.loginUrl, user.email, grailsApplication.config.redirectAfterFirstLogin]" />
            </div>
        </div>
    </div>
</div>
</body>
</html>