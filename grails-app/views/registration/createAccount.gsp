<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="section" content="home"/>
    <g:if test="${!alreadyRegistered && edit}">
        <g:set var="title"><g:message code="create.account.edit.account" /></g:set>
        <meta name="breadcrumbParent" content="${g.createLink(controller: 'profile')},My Profile" />
    </g:if>
    <g:else>
        <g:set var="title"><g:message code="create.account.title" /></g:set>
    </g:else>
    <title>${title}</title>
    <asset:stylesheet src="application.css" />
    <asset:stylesheet src="createAccount.css" />
    <g:if test="${grailsApplication.config.recaptcha.siteKey}">
        <script src="https://www.google.com/recaptcha/api.js" async defer></script>
    </g:if>
</head>
<body>

<div class="row">
    <h1>${title}</h1>
    <g:if test="${flash.message}">
        <div class="alert alert-warning">
            ${flash.message}
        </div>
    </g:if>
    <g:if test="${inactiveUser}">
        <div class="row">
            <div class="col-sm-12">
                <div class="well">
                    <p class="text-danger"><g:message code="create.account.already-reg" args="[params.email]" />
                    </p>

                    <p>
                        <g:message code="create.account.if.error" args="[grailsApplication.config.supportEmail]" />
                    </p>
                </div>
            </div>
        </div>
    </g:if>
    <g:elseif test="${alreadyRegistered}">
        <div class="row">
            <div class="col-sm-12">
                <div class="well">
                    <p class="text-danger"><g:message code="create.account.already.registered" /> <strong>${params.email}</strong>.</p>

                    <p>
                        <g:message code="create.account.login.with.username" /> <a
                            href="${grailsApplication.config.security.cas.loginUrl}"><g:message code="create.account.click.here" /></a>.<br/>
                        <g:message code="create.account.resetting.your.password" /> <g:link controller="registration"
                                                                                 action="forgottenPassword"
                                                                                 params="${[email: params.email]}"><g:message code="create.account.click.here" /></g:link>.
                    </p>
                </div>
            </div>
        </div>
    </g:elseif>

    <div class="row">
        <div class="col-md-8 col-md-push-4">
            <div class="well">
                <g:if test="${!edit}">
                    <h2><g:message code="create.account.do.i.need.account" /></h2>

                    <p><g:message code="create.account.motivation.intro" />

                    <ul>
                    <li><g:message code="create.account.motivation.1" />
                    <li><g:message code="create.account.motivation.2" />
                    <li><g:message code="create.account.motivation.3" />
                    <li><g:message code="create.account.motivation.4" />
                    </ul>
                    <p><g:message code="create.account.motivation.footer" />
                </g:if>
                <h2><g:message code="create.account.your.account.title" /></h2>
                <p>
                    <g:message code="create.account.your.email.will.be.your.account.login" args="[grailsApplication.config.skin.orgNameShort]" />
                </p>
                <g:if test="${!edit}">
                    <p><g:message code="create.account.activation.description" /></p>
                </g:if>
                <h2><g:message code="create.account.policy.title" /></h2>
                <p>
                    <g:message code="create.account.privacy.title" />
                    <a href="${grailsApplication.config.privacyPolicy}"><g:message code="create.account.privacy.link" /></a>.
                </p>
                <h2><g:message code="create.account.tos.title" /></h2>
                <p>
                    <g:message code="create.account.tos.description" args="[grailsApplication.config.skin.orgNameShort, grailsApplication.config.termsOfUse]" />
                </p>
            </div>
        </div>
        <div class="col-md-4 col-md-pull-8">
            <div>
            <g:form name="updateAccountForm" method="POST" action="${edit ? 'update' : 'register'}" controller="registration" useToken="true" onsubmit="updateAccountSubmit.disabled = true; return true;">
                <div class="form-group">
                    <label for="firstName"><g:message code="create.account.first.name" /></label>
                    <input id="firstName" name="firstName" type="text" class="form-control" value="${user?.firstName}" data-validation-engine="validate[required]"/>
                </div>
                <div class="form-group">
                    <label for="lastName"><g:message code="create.account.last.name" /></label>
                    <input id="lastName" name="lastName" type="text" class="form-control" value="${user?.lastName}"  data-validation-engine="validate[required]"/>
                </div>
                <div class="form-group">
                    <label for="email"><g:message code="create.account.email.address" /></label>
                    <input id="email" name="email" type="text" class="form-control" value="${user?.email}"
                           data-validation-engine="validate[required,custom[email]]"
                           data-errormessage-value-missing="${message(code:'create.account.email.is.required')}"
                    />
                </div>

                <g:if test="${!edit}">
                    <div class="form-group">
                    <label for="password"><g:message code="create.account.password" /></label>
                    <input id="password"
                           name="password"
                           class="form-control"
                           value=""
                           data-validation-engine="validate[required, minSize[8]]"
                           data-errormessage-value-missing="${message(code:'create.account.password.is.required')}"
                           type="password"
                    />
                    </div>
                    <div class="form-group">
                    <label for="reenteredPassword"><g:message code="create.account.reentered.password" /></label>
                    <input id="reenteredPassword"
                           name="reenteredPassword"
                           class="form-control"
                           value=""
                           data-validation-engine="validate[required, minSize[8]]"
                           data-errormessage-value-missing="${message(code:'create.account.password.is.required')}"
                           type="password"
                    />
                    </div>
                </g:if>
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
                <g:if test="${edit}">
                    <button id="updateAccountSubmit" class="btn btn-primary"><g:message code="create.account.update.account" /></button>
                    <button id="disableAccountSubmit" class="btn btn-danger"><g:message code="create.account.disable.account" /></button>
                </g:if>
                <g:else>
                    <g:if test="${grailsApplication.config.recaptcha.siteKey}">
                        <div class="g-recaptcha" data-sitekey="${grailsApplication.config.recaptcha.siteKey}"></div>
                        <br/>
                    </g:if>
                    <button id="updateAccountSubmit" class="btn btn-primary"><g:message code="create.account.btn" /></button>
                </g:else>
            </g:form>
            </div>
            <g:if test="${flash.invalidToken}">
                <g:message code="create.account.button.twice" />
            </g:if>
        </div>
   </div>
</div>
</body>
<asset:javascript src="createAccount.js" asset-defer="" />
<asset:script type="text/javascript">
    $(function() {
        userdetails.initCountrySelect('.chosen-select', '#country', '#state', "${g.createLink(uri: '/ws/registration/states')}");

        $('#updateAccountForm').validationEngine('attach', { scroll: false });
        $("#updateAccountSubmit").click(function(e) {

            $("#updateAccountSubmit").attr('disabled','disabled');

            var pm = $('#password').val() == $('#reenteredPassword').val();
            if(!pm){
                alert("The supplied passwords do not match!");
            }

            var valid = $('#updateAccountForm').validationEngine('validate');

            if (valid && pm) {
                $("form[name='updateAccountForm']").submit();
            } else {
                $('#updateAccountSubmit').removeAttr('disabled');
                e.preventDefault();
            }
        });

        $("#disableAccountSubmit").click(function(e) {

            $("#disableAccountSubmit").attr('disabled','disabled');

            var valid = confirm("${message(code: 'default.button.delete.user.confirm.message', default: 'Are you sure want to disable your account? You won\'t be able to login again. You will have to contact us in the future if you want to reactivate your account.')}");

            if (valid) {
                $('#updateAccountForm').validationEngine('detach');
                $("form[name='updateAccountForm']").attr('action','disableAccount');
                $("form[name='updateAccountForm']").submit();
            } else {
                $('#disableAccountSubmit').removeAttr('disabled');
                e.preventDefault();
            }
        });



    });
</asset:script>
</html>
