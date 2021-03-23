<%@ page contentType="text/html"%>
<html>
  <head><title>${emailTitle}</title></head>
  <body>
    <h3>${emailTitle}</h3>
    <p><g:message code="email.greeting" /> ${userName}</p>
    <p>
        <markdown:renderHtml text="${emailBody}"/>
    </p>
    <g:if test="${password}">
        <g:message code="reset.password.description" args="[password]" />
    </g:if>
    <p>
       <a href="${link}">${link}</a>
    </p>
  </body>
</html>