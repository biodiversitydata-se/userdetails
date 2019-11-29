<%@ page import="au.org.ala.userdetails.User" %>

<div class="row">
    <div class="col-md-6">
        <div class="form-group fieldcontain ${hasErrors(bean: userInstance, field: 'firstName', 'error')} ">
            <label for="fi rstName">
                <g:message code="user.firstName.label" default="First Name"/>
            </label>
            <g:textField name="firstName" class="form-control" value="${userInstance?.firstName}"/>
        </div>

        <div class="form-group fieldcontain ${hasErrors(bean: userInstance, field: 'lastName', 'error')} ">
            <label for="lastName">
                <g:message code="user.lastName.label" default="Last Name"/>
            </label>
            <g:textField name="lastName" class="form-control" value="${userInstance?.lastName}"/>
        </div>

        <div class="form-group fieldcontain ${hasErrors(bean: userInstance, field: 'email', 'error')} ">
            <label for="email">
                <g:message code="user.email.label" default="Email"/>
            </label>
            <g:textField name="email" class="form-control" value="${userInstance?.email}"/>
        </div>

        <div class="form-group">
            <label for="organisation"><g:message code="create.account.organisation" /></label>
            <input id="organisation" name="organisation" type="text" class="form-control" value="${props?.organisation}"/>
        </div>

        <div class="form-group">
            <label for="country"><g:message code="create.account.country" /></label>
            <g:select id="country" name="country"
                      class="form-control chosen-select"
                      value="${props?.country ?: 'AU'}"
                      keys="${l.countries()*.isoCode}"
                      from="${l.countries()*.name}"
                      noSelection="${['': message(code:'create.account.choose.your.country')]}"
                      valueMessagePrefix="ala.country."
            />
        </div>

        <div class="form-group">
            <label for="state"><g:message code="create.account.state.province" /></label>
            <g:select id="state" name="state"
                      class="form-control chosen-select"
                      value="${props?.state}"
                      keys="${l.states(country: props?.country ?: 'AU')*.isoCode}"
                      from="${l.states(country: props?.country ?: 'AU')*.name}"
                      noSelection="${['': message(code:'create.account.choose.your.state')]}"
                      valueMessagePrefix="ala.state."
            />
        </div>

        <div class="form-group">
            <label for="city"><g:message code="create.account.city" /></label>
            <input id="city" name="city" type="text" class="form-control" value="${props?.city}" />
        </div>

</div>
<div class="col-md-6 well well-lg">
    <div class="fieldcontain ${hasErrors(bean: userInstance, field: 'activated', 'error')} ">
        <div class="checkbox">
            <label>
                <g:checkBox name="activated" value="${userInstance?.activated}"/> <g:message code="user.form.activated" />
            </label>
        </div>
    </div>

<div class="fieldcontain ${hasErrors(bean: userInstance, field: 'locked', 'error')} ">
    <div class="checkbox">
        <label>
            <g:checkBox name="locked" value="${userInstance?.locked}"/> <g:message code="user.form.locked" />
        </label>
    </div>
</div>

<hr/>

<div class="fieldcontain ${hasErrors(bean: userInstance, field: 'userRoles', 'error')} ">
    <label for="userRoles">
        <g:message code="user.userRoles.label" default=" Roles"/>

    </label>

    <table class="table">
        <g:each in="${userInstance?.userRoles ?}" var="u">
            <tr>
                <td><g:link controller="userRole" action="list" params="[role:u?.encodeAsHTML()]">${u?.encodeAsHTML()}</g:link></td>
                <td><g:link controller="userRole"
                            action="deleteRole"
                            class="btn btn-warning btn-xs"
                            params="[userId:u.user.id,role:u.role.role]"
                            onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');">
                        ${message(code: 'default.button.delete.label', default: 'Delete')}
                    </g:link>
                </td>
            </tr>
        </g:each>
    </table>

    <g:link controller="userRole" action="create"
            class="btn btn-default"
                    params="['user.id': userInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'userRole.label', default: 'UserRole')])}</g:link>

</div>

</div>

</div>