<%@ page contentType="text/html"%>
<html>
  <head><title><g:message code="accessing.account.title" /></title></head>
  <body>
    <h3><g:message code="accessing.account.header" /></h3>
    <p>Dear ${userName}</p>
    <p>
        <g:message code="accessing.account.description" />
    </p>
    <p>
      ${generatedPassword}
    </p>
    <p>
       <g:message code="accessing.account.link" args="[link]" />
    </p>
  </body>
</html>