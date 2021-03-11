<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="account.activated.created.title" /></title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1>
        <g:message code="account.activated.created.please.check.your.email" />
    </h1>

    <div class="row">
        <div class="col-md-12">
            <div class="well well-lg">
                <p><g:message code="account.activated.created.thank.you.for.registering" args="[grailsApplication.config.skin.orgNameLong]" /></p>

                <p><g:message code="account.activated.created.check.your.email" /></p>

                <p><g:message code="account.activated.created.problems.please.email" args="[grailsApplication.config.supportEmail]" /></p>
            </div>
        </div>
    </div>
</div>
</body>
</html>
