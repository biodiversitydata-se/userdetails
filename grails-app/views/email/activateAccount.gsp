<%@ page contentType="text/html"%>
<html>
  <head><title><g:message code="activate.account.title" /></title></head>
  <body>
    <h3><g:message code="activate.account.header" /></h3>
    <p><g:message code="email.greeting" /> ${userName}</p>
    <p>
        <g:message code="activate.account.description" args="[orgNameLong]" />
    </p>
    <p>
       <a href="${link}">${link}</a>
    </p>
  </body>
</html>