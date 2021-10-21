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
    <h1>Flickr success</h1>
    <table class="table">
        <tr>
            <td>Flickr user ID</td>
            <td><a href="http://www.flickr.com/photos/${user_nsid}">${user_nsid}</a>    </td>
        </tr>
        <tr>
            <td>Flickr images</td>
            <td>http://www.flickr.com/photos/${user_nsid}</td>
        </tr>
        <tr>
            <td>Flickr user name</td>
            <td>${username}</td>
        </tr>
    </table>
</div>
</body>
</html>
