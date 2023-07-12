%{--
   - Copyright (C) 2023 Atlas of Living Australia
   - All Rights Reserved.
   -
   - The contents of this file are subject to the Mozilla Public
   - License Version 1.1 (the "License"); you may not use this file
   - except in compliance with the License. You may obtain a copy of
   - the License at http://www.mozilla.org/MPL/
   -
   - Software distributed under the License is distributed on an "AS
   - IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
   - implied. See the License for the specific language governing
   - rights and limitations under the License.
   --}%
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.getProperty('skin.layout')}"/>
    <g:set var="entityName" value="${message(code: 'application.label', default: 'Application')}"/>
    <meta name="section" content="home"/>
    <meta name="breadcrumbParent" content="${g.createLink(controller: 'profile')},My Profile" />
    <title><g:message code="myprofile.my.applications" /></title>
    <asset:stylesheet src="userdetails.css" />
</head>
<body>

<div id="my-applications" role="main">

    <div class="page-header">
        <h1><g:message code="myprofile.my.applications" /></h1>
    </div>

    <!-- Nav tabs -->
    <ul class="nav nav-tabs" role="tablist">
        <li role="presentation" class="active"><a href="#apikey" aria-controls="apikey" role="tab" data-toggle="tab"><g:message code="myprofile.apikey"/></a></li>
        <li role="presentation"><a href="#applications" aria-controls="applications" role="tab" data-toggle="tab"><g:message code="myprofile.applications" default="Applications" /></a></li>
%{--        <li role="presentation"><a href="#messages" aria-controls="messages" role="tab" data-toggle="tab">Messages</a></li>--}%
%{--        <li role="presentation"><a href="#settings" aria-controls="settings" role="tab" data-toggle="tab">Settings</a></li>--}%
    </ul>

    <!-- Tab panes -->
    <div class="tab-content">
        <div role="tabpanel" class="tab-pane active" id="apikey">

            <div class="row">
                <div class="col-md-12">
                    <h3><g:message code="myprofile.my.apikey" /></h3>
                    <h4><g:message code="myprofile.my.apikey.desc" /></h4>
                    <g:form name="generateApikeyForm" controller="profile" action="generateApikey" params="[application:'galah']">
                        <br/>
                        <p><g:message code="generate.apikey.desc.1" /></p>
                        <p><g:message code="generate.apikey.desc.2" /></p>
                        <br/>
                        <g:if test="${apikeys}">
                            <g:message code="my.apikey" /><code>${apikeys}</code>
                        </g:if>
                        <g:else>
                            <button id="generateApikey" class="btn btn-primary"><g:message code="myprofile.generate.apikey" /></button>
                        </g:else>
                    </g:form>
                    <br/>
                </div>
            </div>

        </div>
        <div role="tabpanel" class="tab-pane" id="applications">
            <div class="row">
                <div class="col-md-4">
%{--                    <div class="page-header">--}%
%{--                        <h1><g:message code="myprofile.my.applications" /></h1>--}%
%{--                    </div>--}%
                </div>
                <div class="col-md-8">
                    <div class="pull-right">
                        <button type="button" id="btn-create-app" class="btn btn-primary"><i class="fa fa-pencil"></i> <g:message code="default.new.label" args="[entityName]" /></button>
%{--                        <g:link class="btn btn-primary" action="createApplication"><i class="fa fa-pencil"></i> <g:message code="default.new.label" args="[entityName]" /></g:link>--}%
                    </div>
                </div>
                <div class="col-md-12">
                    <g:if test="${errors}">
                        <div class="alert alert-danger">
                            <g:each in="${errors}" var="err">
                                <p><g:message error="${err}"/></p>
                            </g:each>
                        </div>
                    </g:if>

                    <g:if test="${flash.message}">
                        <div class="message" role="status">${flash.message}</div>
                    </g:if>
                    <table id="apps-table" class="table table-bordered table-striped table-condensed">
                        <thead>
                        <tr>
                            <g:sortableColumn property="name" title="${message(code: 'application.name.label', default: 'Name')}" />

                            <g:sortableColumn property="clientId" title="${message(code: 'application.clientId.label', default: 'Client ID')}" />

                            <td></td>

                            %{--            <g:sortableColumn property="firstName"--}%
                            %{--                              title="${message(code: 'user.firstName.label', default: 'First Name')}" mapping="adminUserList"/>--}%

                            %{--            <g:sortableColumn property="lastName"--}%
                            %{--                              title="${message(code: 'user.lastName.label', default: 'Last Name')}" mapping="adminUserList"/>--}%

                            %{--            <g:sortableColumn property="activated"--}%
                            %{--                              title="${message(code: 'user.activated.label', default: 'Activated')}" mapping="adminUserList"/>--}%

                            %{--            <g:sortableColumn property="locked" title="${message(code: 'user.locked.label', default: 'Locked')}" mapping="adminUserList"/>--}%

                            %{--            <g:sortableColumn property="dateCreated" title="${message(code: 'user.dateCreated.label', default: 'Created')}" mapping="adminUserList"/>--}%

                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${applicationList}" status="i" var="application">
                            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                                <td><g:link action="show"
                                            id="${application.clientId}">${application.name}</g:link></td>

                                <td><g:link action="show"
                                            id="${application.clientId}">${fieldValue(bean: application, field: "clientId")}</g:link></td>

                                <td>
%{--                                    <button class="app-enable" data-id="${application.clientId}">Enable</button>--}%
                                    <button class="app-edit" aria-label="View/Edit" data-id="${application.clientId}"><i class="fa fa-eye"></i></button>
                                    <button class="app-delete" aria-label="Delete" data-id="${application.clientId}"><i class="fa fa-trash"></i></button>
                                </td>

                                %{--                <td>${fieldValue(bean: userInstance, field: "firstName")}</td>--}%

                                %{--                <td>${fieldValue(bean: userInstance, field: "lastName")}</td>--}%

                                %{--                <td><g:formatBoolean boolean="${userInstance.activated}"/></td>--}%

                                %{--                <td><g:formatBoolean boolean="${userInstance.locked}"/></td>--}%

                                %{--                <td>${fieldValue(bean: userInstance, field: "dateCreated")}</td>--}%

                            </tr>
                        </g:each>
                        </tbody>
                    </table>

                    <div class="text-center">
                        <ud:paginate action="list" total="${applicationList}" nextToken="${nextToken}" params="${params}"/>
                    </div>
                </div>
            </div>
        </div>
%{--        <div role="tabpanel" class="tab-pane" id="messages">...</div>--}%
%{--        <div role="tabpanel" class="tab-pane" id="settings">...</div>--}%
    </div>
    <div id="client-modal" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Modal title</h4>
                </div>
                <div class="modal-body">
                    <g:form name="modal-save-form" action="saveApplication">
                        <fieldset class="form">
                            <div class="form-group">
                                <label for="clientId">
                                    <g:message code="application.clientId.label" default="Client ID" />
                                </label>
                                <!-- not read by server -->
                                <input id="clientId" name="clientId" type="text" form="none" readonly class="form-control" />
                            </div>
                            <div class="form-group">
                                <label for="clientSecret">
                                    <g:message code="application.clientSecret.label" default="Client Secret" />
                                </label>
                                <div class="input-group">
                                    <input id="clientSecret" name="clientSecret" form="none" type="password" readonly class="form-control" />
                                    <span class="input-group-btn">
                                        <button id="show-secret" class="btn btn-default"><span class="fa fa-eye" aria-label="Show secret" aria-label-open="Show Secret" aria-label-closed="Hide Secret"></span></button>
                                    </span>
                                </div><!-- /input-group -->


                            </div>
                            <g:render template="applicationForm"/>
                        </fieldset>
                    </g:form>
                </div>
                <div class="modal-footer">
                    <button id="btn-close-app" type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                    <button id="btn-save-app" type="submit" form="modal-save-form" class="btn btn-primary">Save changes</button>
                </div>
            </div><!-- /.modal-content -->
        </div><!-- /.modal-dialog -->
    </div><!-- /.modal -->
<asset:script type="text/javascript">
    function refreshAppTable() {
        let url = '<g:createLink controller="profile" action="listApplications" />';
        $.get(url)
        .done(function(data) {
            let $appsTableBody = $('#apps-table tbody');
            $appsTableBody.children().remove();
            for (var i = 0; i < data.length; ++i) {
                let $tr = $('<tr></tr>', {class: (i % 2) === 0 ? 'even': 'odd'});
                $tr.append($('<td></td>', {text: app.name}));
                $tr.append($('<td></td>', {text: app.clientId}));
                let $buttonsTd = $('<td></td>');
                $buttonsTd.append($('<button>', {class: 'app-edit', 'aria-label': 'View/Edit', 'data-id': app.clientId}).append('<i></i>', {class: 'fa fa-eye'}));
                $buttonsTd.append($('<button>', {class: 'app-delete', 'aria-label': 'Delete', 'data-id': app.clientId}).append('<i></i>', {class: 'fa fa-trash'}));
                $tr.append($buttonsTd);
                $appsTableBody.append($tr);
            }
        });

    }

    function showEditModal(data) {
        $('#client-modal').modal({});
        $('#name').val(data.name);
        $('#type').val(data.type.name).trigger('change');
        $('#clientId').val(data.clientId).parents('.form-group').show();
        let $clientSecret = $('#clientSecret');
        $clientSecret.val(data.clientSecret);
        if (data.clientSecret) {
            $clientSecret.parents('.form-group').show();
        } else {
            $clientSecret.parents('.form-group').hide();
        }

        let callbacks = data.callbacks ? data.callbacks : [];

        let $callbacks = $('#callback-list')
        $callbacks.children().remove();

        for (var i = 0; i < callbacks.length; ++i) {
            addCallbackToForm($callbacks, callbacks, i);
        }

        $('input.callbacks').val('');

        let url = '<g:createLink controller="profile" action="updateClient" />';
        $("#modal-save-form").off('submit').on('submit', function (e) {
            e.preventDefault();
            let $this = $(this);
            let $saveButton = $this.find('btn-save-app');
            let $saveButtonContent = $saveButton.content;
            $saveButton.html('<i class="fa fa-spinner"></i>');
            setModalButtonsDisabled(true);
            $.post(
                url,
                $(this).serialize()
            ).done(function(data) {
                showEditModal(data);
                refreshAppTable();
            }).always(function() {
                $saveButton.html($saveButtonContent);
                setModalButtonsDisabled(false);
            });
        });
    }

    function showCreateModal() {
        $('#client-modal').modal({});
        $('#name').val('');
        $('#type').val('GALAH').trigger('change');
        $('#clientId').val('').parents('.form-group').hide();
        $('#clientSecret').val('').parents('.form-group').hide();
        // $('#callback').val(); // TODO
        let url = '<g:createLink controller="profile" action="generateClient" />';
        $("#modal-save-form").off('submit').on('submit', function (e) {
            e.preventDefault();
            let $this = $(this);
            let $saveButton = $this.find('btn-save-app');
            let $saveButtonContent = $saveButton.content;
            $saveButton.html('<i class="fa fa-spinner"></i>');
            setModalButtonsDisabled(true);
            $.post(
                url,
                $(this).serialize()
            ).done(function(data) {
                showEditModal(data);
                refreshAppTable();
            }).always(function() {
                $saveButton.html($saveButtonContent);
                setModalButtonsDisabled(false);
            });
        });
    }

    function addCallbackToForm($callbacks, callbacks, index) {
        let value = callbacks[index];
        let span = $('<span></span>', {class: 'tag label label-default', 'data-index': index});
        let innerSpan = $('<span></span>', {text: value});
        let button = $('<a></a>', {'data-index': index, role: 'button', class: 'btn btn-danger delete'}).append('<i class="fa fa-trash"></i>');
        let input = $('<input></input>', {value: value, 'data-index': index, type: 'hidden', name: 'callbacks'});

        span.append(innerSpan);
        span.append(button);
        $callbacks.append(span);
        // $callbacks.append(button);
        $callbacks.append(input);
    }

    function setModalButtonsDisabled(disabled) {
        $('#btn-close-app').prop('disabled', disabled);
        $('#btn-save-app').prop('disabled', disabled);
    }

    $(function() {
        $('#btn-create-app').on('click', function() {
            let $this = $(this);
            showCreateModal();
        });
        $('.app-enable').on('click', function() {

        });
        $('#apps-table tbody').on('click', '.app-edit', function() {
            let $this = $(this);
            let $i = $this.find('i');
            $i.toggleClass('fa-spinner').toggleClass('fa-eye');
            let id = $this.data('id');
            let url = "<g:createLink controller="profile" action="application" id="asdfasdf"/>".replace('asdfasdf', id);
            $.get({url: url, dataType: "json"})
                .always(function() {
                    $i.toggleClass('fa-spinner').toggleClass('fa-eye');
                }).done(function(data) {
                    showEditModal(data);
                }).fail(function() {
                    alert("Error loading application"); // TODO
                });
        }).on('click', '.app-delete', function() {
            let confirmDelete = confirm("Are you sure?");
            if (confirmDelete) {
                $.delete("<g:createLink controller="profile" action="application" id="###"/>".replace('###', $this.data('id')))
                .done(function(data) {
                    refreshAppTable();
                }).fail(function() {
                  alert("Error deleting application"); // TODO
                });
            }
        });
        $('#show-secret').on('click', function(e) {
            e.preventDefault()
            let $this = $(this);
            let $icon = $this.find('span');
            let $secretInput = $('#client-secret')
            if ($secretInput.is('[type=password]')) {
                $secretInput.attr('type', 'text');
                $icon.removeClass('fa-eye');
                $icon.addClass('fa-eye-slash');
                $this.attr('aria-label', $this.attr('aria-label-open'));
            } else {
                $secretInput.attr('type', 'password');
                $icon.removeClass('fa-eye-slash');
                $icon.addClass('fa-eye');
                $this.attr('aria-label', $this.attr('aria-label-closed'));
            }
        });
    });
</asset:script>


</div>
</body>
</html>