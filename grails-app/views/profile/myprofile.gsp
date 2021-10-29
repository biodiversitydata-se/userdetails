<%@ page import="grails.util.Holders" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="breadcrumb" content="${message(code:"my.profile")}" />
    <title><g:message code="userdetails.my.profile" /> | ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:stylesheet src="application.css" />
</head>
<body>
    <div id="content">
        <div>
            <div class="row">
                <div class="col-lg-12">
                    <h1><g:message code="myprofile.hello" args="[user.firstName]" /></h1>
                </div>
            </div>
            <div class="row">
                <!-- Column 1 -->
                <div class="col-lg-4">
                    <div class="d-flex">
                        <div class="image">
                            <i class="glyphicon glyphicon-user"></i>
                        </div>
                        <div class="content">
                            <h4 id="update-your-details">
                                <g:link controller="registration" action="editAccount">
                                    <g:message code="myprofile.update" />
                                </g:link>
                              </h4>
                            <p><g:message code="myprofile.update.desc" /></p>
                        </div>
                    </div>
                    <div class="d-flex">
                        <div class="image">
                            <i class="glyphicon glyphicon-lock"></i>
                        </div>
                        <div class="content">
                            <h4 id="reset-your-password">
                                <g:link controller="registration" action="forgottenPassword">
                                    <g:message code="userdetails.index.reset.password" />
                                </g:link>
                            </h4>
                            <p><g:message code="userdetails.index.reset.password.desc" /></p>
                        </div>
                    </div>
                    <div class="d-flex">
                        <div class="image">
                            <img src="${grailsApplication.config.logo.api}"/>
                        </div>
                        <div class="content">
                            <h4 id="your-api-key">
                                <g:link controller="apiKey">
                                    <g:message code="myprofile.apikey" default="My API key" />
                                </g:link>
                            </h4>
                            <p><g:message code="myprofile.apikey.desc" />.</p>
                        </div>
                    </div>

                    <g:if test="${isAdmin}">
                        <div class="d-flex">
                            <div class="image">
                                <i class="glyphicon glyphicon-cog"></i>
                            </div>
                            <div class="content">
                                <h4 id="admin-tools">
                                    <g:link controller="admin">
                                        <g:message code="myprofile.admin.tools" />
                                    </g:link>
                                </h4>
                                <p><g:message code="myprofile.admin.tools.desc" /></p>
                            </div>
                        </div>
                    </g:if>
                </div>

                <!-- Column 2 -->
                <div class="col-lg-4">
                    <div class="d-flex">
                        <div class="image">
                                <img src="${grailsApplication.config.logo.downloads}" alt="">
                        </div>
                        <div class="content">
                            <h4 id="your-doi">
                                <a href="${grailsApplication.config.biocache.myDownloads.url}">
                                    <g:message code="myprofile.your.downloads" />
                                </a>
                            </h4>
                            <p><g:message code="myprofile.your.downloads.desc" /></p>
                        </div>
                    </div>
                    <div class="d-flex">
                        <div class="image">
                            <img src="${grailsApplication.config.logo.specieslists}" alt="">
                        </div>
                        <div class="content">
                            <h4 id="species-lists">
                                <a href="${grailsApplication.config.lists.url}">
                                    <g:message code="myprofile.uploaded.species.lists" />
                                </a>
                            </h4>
                            <p><g:message code="myprofile.uploaded.species.lists.desc" /></p>
                        </div>
                    </div>
                    <div class="d-flex">
                        <div class="image">
                            <i class="glyphicon glyphicon-pencil"></i>
                        </div>
                        <div class="content">
                            <h4 id="records-annotated">
                                <a href="${grailsApplication.config.biocache.search.url}%22${user.id}%22">
                                    <g:message code="myprofile.view.records.you.annotated" />
                                </a>
                            </h4>
                            <p><g:message code="myprofile.view.records.you.annotated.desc" /></p>
                        </div>
                    </div>
                    <div class="d-flex">
                        <div class="image">
                            <img src="${grailsApplication.config.logo.sandbox}" alt="">
                        </div>
                        <div class="content">
                            <h4 id="records-uploaded">
                                <a href="${grailsApplication.config.myData.url}">
                                    <g:message code="myprofile.your.datasets" />
                                </a>
                            </h4>
                            <p><g:message code="myprofile.your.datasets.desc" /></p>
                        </div>
                    </div>
                </div>

                <!-- Column 3 -->
                <div class="col-lg-4">
                    <div class="d-flex">
                        <div class="image">
                            <i class="glyphicon glyphicon-envelope"></i>
                        </div>
                        <div class="content">
                            <h4 id="my-alerts">
                                <a href="${grailsApplication.config.alerts.url}">
                                    <g:message code="myprofile.your.alerts" />
                                </a>
                            </h4>
                            <p><g:message code="myprofile.your.alerts.desc" /></p>
                        </div>
                    </div>
                    <div class="d-flex">
                        <div class="image">
                            <img src="${grailsApplication.config.logo.biocollect}" alt="">
                        </div>
                        <div class="content">
                            <h4 id="my-biocollect">
                                <a href="${grailsApplication.config.biocollect.url}">
                                    <g:message code="myprofile.biocollect" />
                                </a>
                            </h4>
                            <p><g:message code="myprofile.biocollect.desc" /></p>
                        </div>
                    </div>
                    <div class="d-flex">
                        <div class="image">
                            <img src="${grailsApplication.config.logo.digivol}" alt="">
                        </div>
                        <div class="content">
                            <h4 id="record-a-sighting">
                                <a href="${grailsApplication.config.volunteer.url}">
                                    <g:message code="myprofile.tasks.digivol" />
                                </a>
                            </h4>
                            <p><g:message code="myprofile.tasks.digivol.desc" /></p>
                        </div>
                    </div>
                    <div class="d-flex">
                        <div class="image">
                            <img src="${grailsApplication.config.logo.spatialportal}" alt="">
                        </div>
                        <div class="content">
                            <h4 id="spatial-portal">
                                <a href="${grailsApplication.config.spatial.url}">
                                    <g:message code="myprofile.spatial.portal" />
                                </a>
                            </h4>
                            <p><g:message code="myprofile.spatial.portal.desc" /></p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <h3><g:message code="myprofile.external.site.linkages" /></h3>

        <div id="external-linkages" class="row">

            <g:if test="${Holders.config.getProperty('oauth.providers.inaturalist.enabled', Boolean, false)}">
                <div class="col-lg-6">
                    <div class=" well well-small">
                        <div class="d-flex">
                            <div class="image">
                                <img src="${grailsApplication.config.logo.inaturalist}">
                            </div>
                            <div class="content">
                                <h4>${grailsApplication.config.inaturalist.name}</h4>
                                <g:if test="${props.inaturalistId}">
                                    <strong><g:message code="myprofile.inat.you.have.connected.with.user" args="[grailsApplication.config.inaturalist.name]" />
                                    <u:link baseProperty="inaturalist.baseUrl" paths="['people', props.inaturalistId]">${props.inaturalistUsername}</u:link>
                                    </strong>
                                    <ul>
                                        <li><u:link baseProperty="biocache.search.baseUrl" params='[q: grailsApplication.config.inaturalist.searchQuery, fq: "alau_user_id:${props.inaturalistUsername}"]'>View my iNaturalist observations in ${grailsApplication.config.skin.orgNameShort}</u:link></li>
                                        <li><u:link baseProperty="biocache.search.baseUrl" params='[q: grailsApplication.config.inaturalist.searchQuery + " OR " + grailsApplication.config.inaturalist.sightingsSearchQuery, fq: "alau_user_id:${props.inaturalistUsername} OR alau_user_id:\"${user.id}\""]'>View my iNaturalist observations and my ${grailsApplication.config.skin.orgNameShort} Sightings in ${grailsApplication.config.skin.orgNameShort}</u:link></li>
                                    </ul>
                                    <g:link controller="profile" class="btn btn-default" action="removeLink" params="[provider: 'inaturalist']"><g:message code="myprofile.remove.link.to.inaturalist" /></g:link>
                                </g:if>
                                <g:else>
                                    <p>
                                        <g:message code="myprofile.inaturalists.link.description" args="[grailsApplication.config.skin.orgNameShort, grailsApplication.config.skin.orgNameShort]" />
                                    </p>

                                    <span class="btn btn-default">
                                        <oauth:connect provider="inaturalist"><g:message code="myprofile.link.to.my.inaturalist" /></oauth:connect>
                                    </span>
                                </g:else>
                            </div>
                        </div>
                    </div>
                </div>
            </g:if>

            <g:if test="${Holders.config.getProperty('oauth.providers.flickr.enabled', Boolean, true)}">
                <div class="col-lg-6">
                    <div class=" well well-small">
                        <div class="d-flex">
                            <div class="image">
                                <img src="${grailsApplication.config.logo.flickr}">
                            </div>
                            <div class="content">
                                <h4><g:message code="myprofile.flickr.title" /></h4>
                                <g:if test="${props.flickrUsername}">
                                    <strong>
                                        <g:message code="myprofile.flickr.connected" args="[props.flickrId, props.flickrUsername]" />
                                    </strong>
                                    <p>
                                        <g:message code="myprofile.linking.with.flickr.enables.images" />
                                        <a href="http://www.flickr.com/groups/encyclopedia_of_life/"><g:message code="myprofile.flickr.eol.group" /></a>
                                        <g:message code="myprofile.to.be.linked.to.your.atlas" />
                                    </p>
                                    <g:link controller="profile" class="btn btn-default" action="removeLink" params="[provider: 'flickr']"><g:message code="myprofile.remove.link.to.flickr.account" /></g:link>
                                </g:if>
                                <g:else>
                                    <p>
                                        <g:message code="myprofile.flicker.link.description" />
                                    </p>
                                    <span class="btn btn-default">
                                        <oauth:connect provider="flickr"><g:message code="myprofile.link.to.my.flickr.account" /></oauth:connect>
                                    </span>
                                </g:else>
                            </div>
                        </div>
                    </div>
                </div>
            </g:if>
        </div>
    </div>
</body>
</html>
