<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <title><g:message code="forgotten.password.title" /></title>
    <asset:stylesheet src="application.css" />
</head>
<body>

<div class="row">
    <h1>Reset my password</h1>
    <div class="row">
        <div class="col-md-6">

            <g:if test="${captchaInvalid}">
            <p class="well text-danger">
                <g:message code="forgotten.password.captcha.fail" />
            </p>
            </g:if>

            <g:form action="startPasswordReset" method="POST" onsubmit="submitResetBtn.disabled = true; return true;">
                <div class="form-group">
                    <label for="email"><g:message code="forgotten.password.email" /></label>
                    <input id="email" name="email" type="text" class="form-control" value="${params.email ?: email}"/>
                </div>

                <img src="${createLink(controller: 'simpleCaptcha', action: 'captcha')}"/>
                <div class="form-group">
                    <label for="captcha"><g:message code="forgotten.password.captcha" /></label>
                    <g:textField name="captcha" class="form-control"/>
                </div>

                <br/>
                <g:submitButton id="submitResetBtn" class="btn btn-primary" name="submit" value="${message(code:'forgotten.password.reset.link')}"/>
            </g:form>
        </div>
        <div class="col-md-6">
            <p class="well">
                <g:message code="forgotten.password.description.send.pass" />
                <br/>
                <g:message code="forgotten.password..valid.hours" />
            </p>
        </div>

   </div>
</div>
</body>
</html>
