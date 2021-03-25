<%@ page contentType="text/html"%>
<html>
  <head>
      <title>${emailTitle}</title>
      <asset:stylesheet src="application.css" />
      <style>
      body {
          font-family: 'Roboto';
      }
      </style>
  </head>

  <body>
    <a href="${grailsApplication.config.getProperty('homeUrl', String, 'https://www.ala.org.au')}" title="<g:message code='email.logo.title' />">
        <img src="${grailsApplication.config.getProperty('homeLogoUrl', String, 'https://www.ala.org.au/app/uploads/2020/06/ALA_Logo_Inline_RGB-300x63.png')}"
             alt="<g:message code='email.logo.alt' />" >
    </a>

    <div class="email-body">
        <h3>${emailTitle}</h3>
        <p><g:message code="email.greeting" /> ${userName}</p>
        <p><markdown:renderHtml text="${emailBody1}"/></p>
        <g:if test="${password}">
            <g:message code="reset.password.description" args="[password]" />
        </g:if>
        <p><a href="${link}">${link}</a></p>
        <p><markdown:renderHtml text="${emailBody2}"/></p>
    </div>
  </body>
</html>