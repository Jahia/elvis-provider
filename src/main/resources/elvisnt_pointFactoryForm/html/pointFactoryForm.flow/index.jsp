<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="flowRequestContext" type="org.springframework.webflow.execution.RequestContext"--%>
<%--@elvariable id="elvisFactory" type="org.jahia.modules.external.elvis.admin.MountPointFactory"--%>
<template:addResources type="javascript" resources="jquery.min.js"/>
<template:addResources type="javascript" resources="admin/angular.min.js"/>
<template:addResources type="javascript" resources="admin/app/folderPicker.js"/>
<template:addResources type="css" resources="admin/app/folderPicker.css"/>

<template:addResources>
    <script>
        var previewSettings = [];

        function addPreviewSettings() {
            $('#fieldsRequiredErrorMessage').hide();
            if ($('#previewType').val() == ''
                    || $('#previewName').val() == ''
                    || $('#previewMaxWidth').val() == ''
                    || $('#previewMaxHeight').val() == '') {
                $('#fieldsErrorMessage').html('All fields are required!');
                $('#fieldsErrorMessage').show();
                return false;
            }

            console.log(previewSettings);

            for (var i in previewSettings) {
                if (previewSettings[i].name == $('#previewName').val() && previewSettings[i].type == $('#previewType').val()) {
                    $('#fieldsErrorMessage').html('A preview setting for this type already exist with this name!');
                    $('#fieldsErrorMessage').show();
                    return false;
                }
            }

            previewSettings.push({name: $('#previewName').val(),type: $('#previewType').val(),maxWidth: $('#previewMaxWidth').val(),maxHeight: $('#previewMaxHeight').val()});

            console.log(previewSettings);

            console.log(JSON.stringify(previewSettings));
            $('#previewSettings').val(JSON.stringify(previewSettings));
            console.log($('#previewSettings').val());

            $('#previewType').val('');
            $('#previewName').val('');
            $('#previewMaxWidth').val('');
            $('#previewMaxHeight').val('');
        }

        function updatePreviewSettingsTable() {
            $('#previewSettingsBodyTable').html('');
            if (previewSettings.length == 0) {
                $('#emptyPreviewSettingsMessage').show();
            } else {
                for (var i in previewSettings) {
                    $('#previewSettingsBodyTable').append('<tr><td>' + previewSettings[i].type + '</td><td>' + previewSettings[i].name + '</td><td>' + previewSettings[i].maxWidth + '</td><td>' + previewSettings[i].maxHeight + '</td><td><button type="button" onclick="previewSettings.splice('+i+'+1, -1)">Remove</button></td></tr>');
                }
            }
        }

        $(document).ready(function() {
            if ($('#previewSettings').val() != '') {
                $('#emptyPreviewSettingsMessage').hide();
                console.log(JSON.parse($('#previewSettings').val()));
                previewSettings = JSON.parse($('#previewSettings').val());
            }

            if ($('#fileLimit').val() == '') {
                $('#fileLimit').val('-1');
            }
        });
    </script>
</template:addResources>

<h1 class="page-header"><fmt:message key="elvisnt_mountPoint"/></h1>
<div class="folderPickerApp" ng-app="folderPicker">
    <%@ include file="errors.jspf" %>

    <fmt:message var="selectTarget" key="elvisnt_mountPoint.selectTarget"/>
    <c:set var="i18NSelectTarget" value="${functions:escapeJavaScript(selectTarget)}"/>
    <div class="box-1" ng-controller="folderPickerCtrl" ng-init='init(${localFolders}, "${elvisFactory.localPath}", "localPath", true, "${i18NSelectTarget}")'>
        <form:form modelAttribute="elvisFactory" method="post" cssClass="form-horizontal">

            <div class="control-group <c:if test='${fn:contains(messagesSource, "name")}'> error</c:if>">
                <form:label path="name" cssClass="control-label">
                    <fmt:message key="elvisnt_mountPoint.name"/> <span style="color: red">*</span>
                </form:label>
                <div class="controls">
                    <form:input path="name"/>
                    <c:if test="${fn:contains(messagesSource, 'name')}">
                <span class="help-inline">
                    <form:errors path="name"/>
                </span>
                    </c:if>
                </div>
            </div>

            <div class="control-group <c:if test='${fn:contains(messagesSource, "url")}'> error</c:if>">
                <form:label path="url" cssClass="control-label">
                    <fmt:message key="elvisnt_mountPoint.url"/> <span style="color: red">*</span>
                </form:label>
                <div class="controls">
                    <form:input path="url"/>
                    <c:if test="${fn:contains(messagesSource, 'url')}">
                <span class="help-inline">
                    <form:errors path="url"/>
                </span>
                    </c:if>
                </div>
            </div>

            <div class="control-group <c:if test='${fn:contains(messagesSource, "userName")}'> error</c:if>">
                <form:label path="userName" cssClass="control-label">
                    <fmt:message key="elvisnt_mountPoint.userName"/> <span style="color: red">*</span>
                </form:label>
                <div class="controls">
                    <form:input path="userName"/>
                    <c:if test="${fn:contains(messagesSource, 'userName')}">
                <span class="help-inline">
                    <form:errors path="userName"/>
                </span>
                    </c:if>
                </div>
            </div>

            <div class="control-group <c:if test='${fn:contains(messagesSource, "password")}'> error</c:if>">
                <form:label path="password" cssClass="control-label">
                    <fmt:message key="elvisnt_mountPoint.password"/> <span style="color: red">*</span>
                </form:label>
                <div class="controls">
                    <form:password path="password" showPassword="true"/>
                    <c:if test="${fn:contains(messagesSource, 'password')}">
                <span class="help-inline">
                    <form:errors path="password"/>
                </span>
                    </c:if>
                </div>
            </div>

            <div class="control-group <c:if test='${fn:contains(messagesSource, "fileLimit")}'> error</c:if>">
                <form:label path="fileLimit" cssClass="control-label">
                    <fmt:message key="elvisnt_mountPoint.fileLimit"/> <span style="color: red">*</span>
                </form:label>
                <div class="controls">
                    <form:input path="fileLimit" showPassword="true"/>
                    <c:if test="${fn:contains(messagesSource, 'fileLimit')}">
                <span class="help-inline">
                    <form:errors path="fileLimit"/>
                </span>
                    </c:if>
            <span class="help-block">
                <div class="alert alert-info">
                    <fmt:message key="elvisnt_mountPoint.fileLimit.info"/>
                </div>
            </span>
                </div>
            </div>
            <div class="control-group <c:if test='${fn:contains(messagesSource, "usePreview")}'> error</c:if>">
                <div class="controls">
                    <form:label path="usePreview" cssClass="checkbox">
                        <form:checkbox path="usePreview" onchange="$('#previewBuilder').toggle();"/>
                        <fmt:message key="elvisnt_mountPoint.usePreview"/> <span style="color: red">*</span>
                    </form:label>
                    <c:if test="${fn:contains(messagesSource, 'usePreview')}">
                        <span class="help-inline">
                            <form:errors path="usePreview"/>
                        </span>
                    </c:if>
                </div>
                <form:hidden path="previewSettings"/>
                <div id="previewBuilder" style="display: none;border-color: #999;background-color: #fbfbfb;color: #000;" class="alert">

                    <div class="alert alert-info">
                        <p>If you do not add a preview setting for each type we will use default preview settings from elvis to generate previews.</p>
                        <p><em>default image preview: 1600x1600 (width, height)</em></p>
                        <p><em>default video preview: 480x360 (width, height)</em></p>
                    </div>

                    <table class="table table-bordered table-hover table-striped">
                        <thead>
                            <tr>
                                <th>
                                    Type
                                </th>
                                <th>
                                    Preview name
                                </th>
                                <th>
                                    Max width
                                </th>
                                <th>
                                    Max height
                                </th>
                                <th>
                                    Actions
                                </th>
                            </tr>
                        </thead>
                        <tbody id="previewSettingsBodyTable"></tbody>
                    </table>
                    <p id="emptyPreviewSettingsMessage" class="text-center"><em>Not preview settings saved yet!</em></p>
                    <div class="form-inline">
                        <fieldset>
                            <legend>Add a preview settings</legend>
                            <p>You can set multiple preview formats for each type. But Preview name must be unique by type and and all fields are required.</p>

                            <select id="previewType">
                                <option value="">Select type...</option>
                                <option value="video">Video</option>
                                <option value="image">Image</option>
                            </select>

                            <input id="previewName" type="text" placeholder="Preview name">

                            <input id="previewMaxWidth" type="number" class="input-small" placeholder="Max width">

                            <input id="previewMaxHeight" type="number" class="input-small" placeholder="Max height">

                            <button type="button" class="btn btn-primary" style="margin-bottom: 0" onclick="addPreviewSettings()">
                                Add
                            </button>
                            <p id="fieldsErrorMessage" class="text-error" style="display: none;"></p>
                        </fieldset>
                    </div>
                </div>
            </div>

            <div class="control-group">
                <jsp:include page="/modules/external-provider/angular/folderPicker.jsp"/>
            </div>

            <div class="form-actions">
                <button class="btn btn-primary" type="submit" name="_eventId_save">
                    <fmt:message key="label.save"/>
                </button>
                <button class="btn" type="submit" name="_eventId_cancel">
                    <fmt:message key="label.cancel"/>
                </button>
            </div>
        </form:form>
    </div>
</div>