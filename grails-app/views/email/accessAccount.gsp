<%@ page contentType="text/html"%>
<html>
  <head><title><g:message code="accessing.account.title" /></title></head>
  <body>
    <h1><g:message code="accessing.account.header" /></h1>
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