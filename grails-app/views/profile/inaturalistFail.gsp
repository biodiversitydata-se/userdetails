<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="userdetails.my.profile" /> | ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1><g:message code="inaturalist.fail.link" /></h1>
    <p class="well text-danger">
        <g:message code="inaturalist.fail.description" args="[grailsApplication.config.supportEmail]" />
    </p>
</div>
</body>
</html>
