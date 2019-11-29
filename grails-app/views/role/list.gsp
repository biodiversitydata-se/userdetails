
<%@ page import="au.org.ala.userdetails.Role" %>
<!DOCTYPE html>
<html>
<head>
	<meta name="layout" content="${grailsApplication.config.skin.layout}">
	<g:set var="entityName" value="${message(code: 'role.label', default: 'Role')}" />
	<title><g:message code="default.list.label" args="[entityName]" /></title>
	<meta name="breadcrumbParent" content="${createLink(controller:'admin', action:'index')},Administration" />
</head>
<body>
<div id="list-role" class="content scaffold-list" role="main">
	<div class="row">
		<div class="col-sm-4">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
		</div>
		<div class="col-sm-8">
			<div class="pull-right">
				<g:link class="btn btn-primary" action="create"><i class="fa fa-pencil"></i> <g:message code="default.new.label" args="[entityName]" /></g:link>
			</div>
		</div>
		<div class="col-sm-12">
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table class="table table-bordered table-striped table-condensed">
				<thead>
				<tr>

					<g:sortableColumn property="role" title="${message(code: 'role.role.label', default: 'Role')}" />

					<g:sortableColumn property="description" title="${message(code: 'role.description.label', default: 'Description')}" />

					<th><g:message code="user.list.actions" /></th>

				</tr>
				</thead>
				<tbody>
				<g:each in="${roleInstanceList}" status="i" var="roleInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

						<td>${fieldValue(bean: roleInstance, field: "role")}</td>

						<td>${fieldValue(bean: roleInstance, field: "description")}</td>

						<td><a href="${createLink(controller: 'userRole', action:'list', params:[role:roleInstance.role])}">View users</a></td>
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="text-center">
				<hf:paginate total="${roleInstanceTotal}" params="${params}" />
			</div>
		</div>
	</div>
</div>
</body>
</html>
