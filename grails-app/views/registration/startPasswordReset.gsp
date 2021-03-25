<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="start.password.reset.title" /></title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1><g:message code="start.password.reset.header" /></h1>
    <div class="row">
        <div class="col-md-12">
            <p class="well">
                <g:message code="start.password.reset.description" args="[params.email]" />
                <br/>
                <g:message code="start.password.reset.description.click" />
            </p>
        </div>
   </div>
</div>
</body>
</html>