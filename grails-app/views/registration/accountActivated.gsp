<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="account.activated.account.created" /> | ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1>
        <g:message code="account.activated.please.check.your.email" />
    </h1>

    <div class="row">
        <div class="col-md-12">
            <div class="well well-lg">
                <g:message code="account.activated.thank.you.for.registering" args="[grailsApplication.config.skin.orgNameLong]" />
                <br/>
                <g:message code="account.activated.if.you.have.any.problems" args="[grailsApplication.config.supportEmail]" />
            </div>
        </div>
   </div>
</div>
</body>
</html>
