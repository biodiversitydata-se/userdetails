<%@ page import="grails.util.Holders" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <meta name="breadcrumb" content="My API Key Secret" />
    <meta name="breadcrumbParent"
          content="${createLink(action: 'index', controller: 'profile')},${message(code: 'myprofile', default:'My Profile')},${createLink(action: 'show', controller: 'apiKey')},${message(code: 'myapikey', default:'My API Key')}"
    />
    <meta name="breadcrumbs"
          content="${createLink(action: 'show', controller: 'apiKey')},${message(code: 'myapikey', default:'My API Key')}"
    />
    <title><g:message code="userdetails.my.profile" /> | ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div class="row">
    <h1><g:message code="myprofile.hello" args="[user.firstName]" /></h1>

        <g:if test="${regenSuccess}">
            <p>
                This is your API key and generated secret.
                Please store this in a safe place, you will not be able to retrieve it again.
            </p>
            <table class="table">
                <tr>
                    <td>API Key</td><td>${apiKey}</td>
                </tr>
                <tr>
                    <td>API Secret</td><td>${apikeySecret}</td>
                </tr>
            </table>
        </g:if>
        <g:else>
            <p>
               You recently regenerated your secret code.
               You can regenerate in ${regenOkTime} minutes....
            </p>
        </g:else>
    </p>
    </div>
</div>
</body>
</html>
