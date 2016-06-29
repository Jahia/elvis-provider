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

<fmt:message key="elvis.label.select" var="labelSelect"/>
<fmt:message key="elvis.label.remove" var="labelRemove"/>
<fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.maxWidth" var="labelMaxWidth"/>
<fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.maxHeight" var="labelMaxHeight"/>
<fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.ppiDpi" var="labelPpiDpi"/>
<fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.extension" var="labelExtension"/>
<fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.error.fieldRequired" var="errorFieldRequired"/>
<fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.error.numberOnly" var="errorNumberOnly"/>
<fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.error.nameAlreadyTaken" var="errorNameAlreadyTaken"/>
<fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.error.nameCharacters" var="errorNameCharacters"/>

<template:addResources>
    <script>
        var mapI18nMPF = {
            labelSelect: '${functions:escapeJavaScript(labelSelect)}',
            labelRemove: '${functions:escapeJavaScript(labelRemove)}',
            labelMaxWidth: '${functions:escapeJavaScript(labelMaxWidth)}',
            labelMaxHeight: '${functions:escapeJavaScript(labelMaxHeight)}',
            labelPpiDpi: '${functions:escapeJavaScript(labelPpiDpi)}',
            labelExtension: '${functions:escapeJavaScript(labelExtension)}',
            errorFieldRequired: '${functions:escapeJavaScript(errorFieldRequired)}',
            errorNumberOnly: '${functions:escapeJavaScript(errorNumberOnly)}',
            errorNameAlreadyTaken: '${functions:escapeJavaScript(errorNameAlreadyTaken)}',
            errorNameCharacters: '${functions:escapeJavaScript(errorNameCharacters)}'
        };

        var managePreviewSettings = {
            settings: {video:[], image:[]},
            init: function() {
                if ($('#usePreview1').is(":checked")) {
                    managePreviewSettings.togglePanel();
                }

                if ($('#previewSettings').val() != '') {
                    $('#emptyPreviewSettingsMessage').hide();
                    managePreviewSettings.settings = JSON.parse($('#previewSettings').val());
                    managePreviewSettings.updateTable();
                }
            },
            add: function() {
                if ($('#previewType').val() == 'image') {
                    managePreviewSettings.settings.image.push({
                        name: $('#previewName').val(),
                        type: $('#previewType').val(),
                        maxWidth: $('#previewMaxWidth').val(),
                        maxHeight: $('#previewMaxHeight').val(),
                        ppi: $('#previewPpi').val(),
                        extension: $('#previewExtension').val()
                    });
                } else {
                    managePreviewSettings.settings.video.push({
                        name: $('#previewName').val(),
                        type: $('#previewType').val(),
                        maxWidth: $('#previewMaxWidth').val(),
                        maxHeight: $('#previewMaxHeight').val(),
                        extension: $('#previewExtension').val()
                    });
                }

                managePreviewSettings.updateTable();
                managePreviewSettings.updateInput();
            },
            clearErrorMessage: function() {
                $('.previewError').hide();
                $('.previewError').html('');
                $('.previewError').parents('.control-group').removeClass('error');
            },
            clearForm: function() {
                $('#previewType').val('');
                $('#previewName').val('');
                $('#previewMaxWidth').val('');
                $('#previewMaxHeight').val('');
                $('#previewPpi').parents('.control-group').hide();
                $('#previewPpi').val('');
                $('#previewExtension').parents('.control-group').hide();
            },
            remove: function(table, index) {
                var array = (table == 'image')?managePreviewSettings.settings.image:managePreviewSettings.settings.video;
                array.splice(index, 1);
                managePreviewSettings.updateTable();
                managePreviewSettings.updateInput();
            },
            showErrorMessage: function($input, message) {
                $input.parents('.control-group').addClass('error');
                var $helpBlock = $input.siblings('.previewError');
                $helpBlock.html(message);
                $helpBlock.show();
            },
            togglePanel: function() {
                $('#previewBuilder').toggle();
            },
            updateForm: function() {
                $('#previewPpi').parents('.control-group').hide();
                $('#previewPpi').val('');
                $('#previewExtension').parents('.control-group').hide();

                if ($('#previewType').val() == 'video') {
                    $('#previewExtension').html('<option value="">' + mapI18nMPF.labelSelect + '</option>' +
                                                '<option value="mp4">mp4</option>' +
                                                '<option value="flv">flv</option>');
                    $('#previewExtension').parents('.control-group').show();
                }

                if ($('#previewType').val() == 'image') {
                    $('#previewPpi').parents('.control-group').show();
                    $('#previewExtension').html('<option value="">' + mapI18nMPF.labelSelect + '</option>' +
                                                '<option value="jpg">jpg</option>' +
                                                '<option value="png">png</option>' +
                                                '<option value="tiff">tiff</option>');
                    $('#previewExtension').parents('.control-group').show();
                }
            },
            updateInput: function() {
                $('#previewSettings').val(JSON.stringify(managePreviewSettings.settings));
            },
            updateTable: function() {
                $('#previewSettingsBodyTable').html('');
                if (managePreviewSettings.settings.length == 0) {
                    $('#emptyPreviewSettingsMessage').show();
                } else {
                    for (var i in managePreviewSettings.settings.image) {
                        $('#previewSettingsBodyTable').append('<tr>' +
                                '<td>' + managePreviewSettings.settings.image[i].type + '</td>' +
                                '<td>' + managePreviewSettings.settings.image[i].name + '</td>' +
                                '<td><p>' + mapI18nMPF.labelMaxWidth + ': ' + managePreviewSettings.settings.image[i].maxWidth + '</p>' +
                                '<p>' + mapI18nMPF.labelMaxHeight + ': ' + managePreviewSettings.settings.image[i].maxHeight + '</p>' +
                                '<p>' + mapI18nMPF.labelPpiDpi + ': ' + managePreviewSettings.settings.image[i].ppi + '</p>' +
                                '<p>' + mapI18nMPF.labelExtension + ': ' + managePreviewSettings.settings.image[i].extension + '</p></td>' +
                                '<td><button type="button" class="btn btn-danger" onclick="managePreviewSettings.remove(\'image\', ' + i + ')">' + mapI18nMPF.labelRemove + '</button></td></tr>');
                    }
                    for (var i in managePreviewSettings.settings.video) {
                        $('#previewSettingsBodyTable').append('<tr>' +
                                '<td>' + managePreviewSettings.settings.video[i].type + '</td>' +
                                '<td>' + managePreviewSettings.settings.video[i].name + '</td>' +
                                '<td><p>' + mapI18nMPF.labelMaxWidth + ': ' + managePreviewSettings.settings.video[i].maxWidth + '</p>' +
                                '<p>' + mapI18nMPF.labelMaxHeight + ': ' + managePreviewSettings.settings.video[i].maxHeight + '</p>' +
                                '<p>' + mapI18nMPF.labelExtension + ': ' + managePreviewSettings.settings.video[i].extension + '</p></td>' +
                                '<td><button type="button" class="btn btn-danger" onclick="managePreviewSettings.remove(\'video\', ' + i + ')">' + mapI18nMPF.labelRemove + '</button></td></tr>');
                    }
                }
            },
            validateForm: function() {
                var isValid = true;
                managePreviewSettings.clearErrorMessage();

                if ($('#previewType').val() == '') {
                    managePreviewSettings.showErrorMessage($('#previewType'), mapI18nMPF.errorFieldRequired);
                    isValid = false;
                } else {
                    if ($('#previewExtension').val() == '') {
                        managePreviewSettings.showErrorMessage($('#previewExtension'), mapI18nMPF.errorFieldRequired);
                        isValid = false;
                    }

                    if ($('#previewType').val() == 'image') {
                        if ($('#previewPpi').val() != '' && !/^\d+$/.test($('#previewPpi').val())) {
                            managePreviewSettings.showErrorMessage($('#previewPpi'), mapI18nMPF.errorNumberOnly);
                            isValid = false;
                        }
                    }
                }

                if ($('#previewName').val() == '') {
                    managePreviewSettings.showErrorMessage($('#previewName'), mapI18nMPF.errorFieldRequired);
                    isValid = false;
                } else {
                    if (!/^([A-Za-z0-9\-\_]+)$/.test($('#previewName').val())) {
                        managePreviewSettings.showErrorMessage($('#previewName'), mapI18nMPF.errorNameCharacters);
                        isValid = false;
                    }
                }

                if ($('#previewType').val() != '' && $('#previewName').val() != '') {
                    var array = ($('#previewType').val() == 'image')?managePreviewSettings.settings.image:managePreviewSettings.settings.video;
                    for (var i in array) {
                        if (array[i].name == $('#previewName').val() && array[i].type == $('#previewType').val()) {
                            managePreviewSettings.showErrorMessage($('#previewName'), mapI18nMPF.errorNameAlreadyTaken);
                            isValid = false;
                        }
                    }
                }

                if ($('#previewMaxWidth').val() == '') {
                    managePreviewSettings.showErrorMessage($('#previewMaxWidth'), mapI18nMPF.errorFieldRequired);
                    isValid = false;
                } else {
                    if (!/^\d+$/.test($('#previewMaxWidth').val())) {
                        managePreviewSettings.showErrorMessage($('#previewMaxWidth'), mapI18nMPF.errorNumberOnly);
                        isValid = false;
                    }
                }

                if ($('#previewMaxHeight').val() == '') {
                    managePreviewSettings.showErrorMessage($('#previewMaxHeight'), mapI18nMPF.errorFieldRequired);
                    isValid = false;
                } else {
                    if (!/^\d+$/.test($('#previewMaxHeight').val())) {
                        managePreviewSettings.showErrorMessage($('#previewMaxHeight'), mapI18nMPF.errorNumberOnly);
                        isValid = false;
                    }
                }

                if (isValid) {
                    $('#modalPreviewSettings').modal('hide');
                    managePreviewSettings.add();
                    managePreviewSettings.clearForm();
                    managePreviewSettings.clearErrorMessage();
                } else {
                    return isValid;
                }
            }
        };

        function validateMountPointForm() {
            if ($('#writeUsageInElvis1').is(":checked") && $('#fieldToWriteUsage').val() == '') {
                $('#fieldToWriteUsageDiv').addClass('error');
                $('#fieldToWriteUsageDiv .controls').append('<span class="help-inline">' + mapI18nMPF.errorFieldRequired + '</span>');
                return false;
            } else {
                $('#elvisFactory').append('<input type="hidden" name="_eventId" value="save">');
                $('#elvisFactory').submit();
            }
        }

        $(document).ready(function() {
            managePreviewSettings.init();

            if ($('#writeUsageInElvis1').is(":checked")) {
                $('#fieldToWriteUsageDiv').toggle();
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
                    <form:input path="fileLimit"/>
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

            <div class="control-group <c:if test='${fn:contains(messagesSource, "writeUsageInElvis")}'> error</c:if>">
                <div class="controls">
                    <form:label path="writeUsageInElvis" for="writeUsageInElvis1" class="checkbox">
                        <form:checkbox path="writeUsageInElvis" onchange="$('#fieldToWriteUsageDiv').toggle();"/>
                        <fmt:message key="elvisnt_mountPoint.writeUsageInElvis"/>
                    </form:label>
                    <c:if test="${fn:contains(messagesSource, 'writeUsageInElvis')}">
                        <span class="help-inline">
                            <form:errors path="writeUsageInElvis"/>
                        </span>
                    </c:if>
                    <span class="help-block">
                        <div class="alert alert-info">
                            <fmt:message key="elvisnt_mountPoint.writeUsageInElvis.info"/>
                        </div>
                    </span>
                </div>
            </div>

            <div class="control-group <c:if test='${fn:contains(messagesSource, "fieldToWriteUsage")}'> error</c:if>"
                 id="fieldToWriteUsageDiv" style="display: none;">
                <form:label path="fieldToWriteUsage" cssClass="control-label">
                    <fmt:message key="elvisnt_mountPoint.fieldToWriteUsage"/> <span style="color: red">*</span>
                </form:label>
                <div class="controls">
                    <form:input path="fieldToWriteUsage"/>
                    <c:if test="${fn:contains(messagesSource, 'fieldToWriteUsage')}">
                        <span class="help-inline">
                            <form:errors path="fieldToWriteUsage"/>
                        </span>
                    </c:if>
                </div>
            </div>


            <div class="control-group <c:if test='${fn:contains(messagesSource, "usePreview")}'> error</c:if>">
                <div class="controls">
                    <form:label path="usePreview" for="usePreview1" class="checkbox">
                        <form:checkbox path="usePreview" onchange="managePreviewSettings.togglePanel();"/>
                        <fmt:message key="elvisnt_mountPoint.usePreview"/>
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
                        <p><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.info.previewSettings1"/></p>
                        <p><em><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.info.previewSettings2"/></em></p>
                        <p><em><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.info.previewSettings3"/></em></p>
                    </div>

                    <table class="table table-bordered table-hover table-striped">
                        <thead>
                            <tr>
                                <th>
                                    <fmt:message key="elvis.label.type"/>
                                </th>
                                <th>
                                    <fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.previewName"/>
                                </th>
                                <th>
                                    <fmt:message key="elvis.label.options"/>
                                </th>
                                <th>
                                    <fmt:message key="elvis.label.actions"/>
                                </th>
                            </tr>
                        </thead>
                        <tbody id="previewSettingsBodyTable"></tbody>
                    </table>
                    <p id="emptyPreviewSettingsMessage" class="text-center"><em><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.table.noPreviewSettings"/></em></p>
                    <div class="row-fluid">
                        <button class="btn btn-primary pull-right" type="button" data-toggle="modal" data-target="#modalPreviewSettings">
                            <fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.title.addPreviewSettings"/>
                        </button>
                    </div>
                </div>
            </div>

            <div class="control-group">
                <jsp:include page="/modules/external-provider/angular/folderPicker.jsp"/>
            </div>

            <div class="form-actions">
                <button type="button" class="btn btn-primary" onclick="validateMountPointForm();">
                    <fmt:message key="label.save"/>
                </button>
                <button type="submit" name="_eventId_cancel" class="btn">
                    <fmt:message key="label.cancel"/>
                </button>
            </div>
        </form:form>
    </div>
</div>

<div id="modalPreviewSettings" class="modal hide fade" role="dialog" aria-labelledby="addPreviewSetting" aria-hidden="true">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
        <h3 id="addPreviewSetting"><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.title.addPreviewSettings"/></h3>
    </div>
    <div class="modal-body">
        <div class="alert alert-info"><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.info.addPreviewSettings"/></div>

        <form class="form-horizontal" novalidate>
            <div class="control-group">
                <label class="control-label"><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.previewType"/></label>
                <div class="controls">
                    <select id="previewType" onchange="managePreviewSettings.updateForm()">
                        <option value=""><fmt:message key="elvis.label.select"/></option>
                        <option value="video"><fmt:message key="elvis.label.video"/></option>
                        <option value="image"><fmt:message key="elvis.label.image"/></option>
                    </select>
                    <span class="help-block previewError hide"></span>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label"><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.previewName"/></label>
                <div class="controls">
                    <input id="previewName" type="text">
                    <span class="help-block previewError hide"></span>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label"><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.maxWidth"/></label>
                <div class="controls">
                    <input id="previewMaxWidth" type="number">
                    <span class="help-block previewError hide"></span>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label"><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.maxHeight"/></label>
                <div class="controls">
                    <input id="previewMaxHeight" type="number">
                    <span class="help-block previewError hide"></span>
                </div>
            </div>

            <div class="control-group hide">
                <label class="control-label"><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.ppiDpi"/></label>
                <div class="controls">
                    <input id="previewPpi" type="number">
                    <span class="help-block previewError hide"></span>
                </div>
            </div>

            <div class="control-group hide">
                <label class="control-label"><fmt:message key="elvisnt_pointFactoryForm.managePreviewSettings.label.extension"/></label>
                <div class="controls">
                    <select id="previewExtension"></select>
                    <span class="help-block previewError hide"></span>
                </div>
            </div>
        </form>
    </div>
    <div class="modal-footer">
        <button type="button" class="btn" data-dismiss="modal" aria-hidden="true"
                onclick="managePreviewSettings.clearForm();managePreviewSettings.clearErrorMessage();">
            <fmt:message key="elvis.label.cancel"/>
        </button>
        <button type="button" class="btn btn-primary" onclick="managePreviewSettings.validateForm();">
            <fmt:message key="elvis.label.add"/>
        </button>
    </div>
</div>