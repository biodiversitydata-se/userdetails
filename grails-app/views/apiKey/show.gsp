<%@ page import="grails.util.Holders" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <meta name="breadcrumb" content="${message(code: 'myapikey', default:'My API Key')}" />
    <meta name="breadcrumbParent"
          content="${createLink(action: 'index', controller: 'profile')},${message(code: 'myprofile', default:'My Profile')}"
    />
    <title><g:message code="userdetails.my.profile" /> | ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:stylesheet src="application.css" />
</head>
<body>
<div id="content">
    <div class="row">
        <div class="col-lg-8">
            <h1>
                <g:message code="myprofile.yourapikey" default="Your API Key" />
            </h1>
            <p>
            <g:message code="myprofile.yourapikey.desc1" />
            </p>
            <p>
                <g:message code="myprofile.yourapikey.desc2" />
            </p>
            <table class="table " style="font-size: 20px; margin-top:40px; ">
                <tr>
                    <td>Your API key</td>
                    <td id="apiKey">${apiKey}</td>
                </tr>
                <tr>
                    <td>Your API secret</td>
                    <td id="generatedSecret">
                        <g:if test="${generateLinkEnabled}">
                            <button id="generatedSecretBtn" class="btn btn-danger">
                                <i class="glyphicon glyphicon-cog"></i> Generate secret key
                            </button>
                        </g:if>
                        <g:else>
                            You recently generated the API Secret. You can regenerate in ${regenTime} minutes.
                        </g:else>
                    </td>
                </tr>
            </table>
        </div>
        <div class="col-lg-4 ">
            <div class="well">
            <h2>What can I do with this key ?</h2>
            <p>
                With this key you can obtain a JSON web token.
                With a JSON Web token  you can:
                <ul>
                    <li>Create a data resource</li>
                    <li>Register an IPT</li>
                    <li>Upload a dataset into the Atlas</li>
                    <li>More to come...</li>
                </ul>
            </p>
            </div>
        </div>
    </div>
    <div class="row" id="how-do-i" style="margin-top: 20px;">
        <div class="col-lg-12">
            <h2>How do I use a JSON Web Token (JWT) ?</h2>
            <p>
                With a JWT and API key, you can submit <code>HTTP GET, POST</code> requests to give you access to
                services that require access permissions.
                <br/>
                Once you have a JWT, it can be re-used for several requests.
                It will <b>expire after ${grailsApplication.config.jwt.expiryInMins} minutes</b>.
            </p>

            <h3 style="margin-top: 20px; ">Example usage</h3>

            1. Request a JSON Web Token (JWT) like so:
            <p class="well code-block" >
                <code>
                    curl -L https://gateway-test.ala.org.au/token   \ <br/>
                    &nbsp;&nbsp;-H "x-api-key: YOUR-API-KEY"   \ <br/>
                    &nbsp;&nbsp;-H "x-api-secret: YOUR-API-SECRET"
                </code>
            </p>

            2. The JSON response will look something like this:
            <p class="well code-block">
                <code>
                   { <br/>
                      &nbsp;&nbsp;"token" : "eyJraWQiOiJuZWN0YXItYXV0aC1kZXYuYWxhLm9yZy5h....."
                      <br/>
                    &nbsp;}
                </code>
            </p>

            3. Make the request to Atlas API, your request need to include the JWT in the <code>Authorization</code> header
            along with the <code>apiKey</code> header like so:
            <p class="well code-block">
                <code>
                    curl -X POST https://gateway-test.ala.org.au/register/dataResource  \ <br/>
                        &nbsp;&nbsp;-H "x-api-key: YOUR-API-KEY"   \ <br/>
                        &nbsp;&nbsp;-H "Authorization: Bearer eyJraWQiOiJuZWN0YXItYXV0aC1kZXYuYWxhLm9yZy5h....."
                </code>
            </p>
        </div>
    </div>

    <div class="row">
        <div class="col-lg-12">
            <h2>Services requiring further permissions</h2>
            <p>Other services require permissions to be added to your user account.
            If you wish to request access, please email support@ala.org.au.
            </p>
            Example APIs requiring additional permissions:
            <ul>
                <li>Access sensitive data</li>
                <li>Upload images into the Atlas</li>
            </ul>
        </div>
    </div>

</div>
<asset:script>
    $('#generatedSecretBtn').click(function() {
        $.ajax({
            type: 'GET',
            url: '${raw(g.createLink(controller: 'apiKey', action: 'secret'))}',
            dataType: 'json',
            success: function (data) {
                $('#generatedSecret').html('');
                $('#generatedSecret').html(data.response);
            }
        });
    });
</asset:script>
</body>
</html>
