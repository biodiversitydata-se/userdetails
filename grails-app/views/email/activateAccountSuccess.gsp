<%@ page contentType="text/html"%>
<html>
<head><title><g:message code="activate.account.success.title" /></title></head>
<body>
<h1><g:message code="activate.account.success.header" /></h1>
<p>
    <g:message code="activate.account.success.alerts" />

    ${activatedAlerts}
</p>
<p>
    <g:message code="activate.account.success.alerts.config" args="[alertsUrl]" />
</p>
</body>
</html>