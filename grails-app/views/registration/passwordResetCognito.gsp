<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}"/>
    <meta name="section" content="home"/>
    <title><g:message code="password.reset.title" /></title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<asset:script type="text/javascript">
    $(function(){
        // Used to prevent double clicks from submitting the form twice.  Doing so will result in a confusing
        // message sent back to the user.
        var processingPasswordReset = false;
        var $form = $("form[name='resetPasswordForm']");
        $form.submit(function(event) {

            // Double clicks result in a confusing error being presented to the user.
            if (!processingPasswordReset) {
                processingPasswordReset = true;
                $('#submitResetBtn').attr('disabled','disabled');
                if($('#reenteredPassword').val() != $('#password').val()) {
                    event.preventDefault();
                    processingPasswordReset = false;
                    alert("The supplied passwords do not match!")
                    $('#submitResetBtn').removeAttr('disabled');
                }
            }
            else {
                event.preventDefault();
            }
        });
    });
</asset:script>

<div class="row">
    <h1><g:message code="password.reset.description" /></h1>

    <g:hasErrors>
    <div class="alert alert-danger">
        <g:eachError var="err">
            <p><g:message error="${err}"/></p>
        </g:eachError>
    </div>
    </g:hasErrors>

    <div class="row">

        <g:form useToken="true" name="resetPasswordForm" controller="registration" action="updateCognitoPassword">
            <input id="email" type="hidden" name="email" value="${email}"/>

            <div class="form-group">
                <label for="code">Code sent to your email</label>
                <input id="code" type="number" class="form-control" name="code" value=""/>
            </div>

            <div class="form-group">
                <label for="password">Your new password (Password must contain at least 8 characters and must contain a lower case letter, an upper case letter, a special character and a number)</label>
                <input id="password" type="password" class="form-control" name="password" value=""/>
            </div>

            <div class="form-group">
                <label for="reenteredPassword"><g:message code="password.reset.re.enter.password" /></label>
                <input id="reenteredPassword" type="password" class="form-control" name="reenteredPassword" value=""/>
            </div>

            <button id="submitResetBtn" class="btn btn-primary"><g:message code="password.reset.set.btn" /></button>
        </g:form>
   </div>
</div>
</body>
</html>
