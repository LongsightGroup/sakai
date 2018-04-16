<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t" %>
<%@ taglib uri="http://sakaiproject.org/jsf/sakai" prefix="sakai" %>
<f:view>
  <div class="portletBody">
	<h:form id="gbForm">
		<t:aliasBean alias="#{bean}" value="#{sissGradeExportBean}">
			<%@include file="/inc/appMenu.jspf"%>
		</t:aliasBean>

		<sakai:flowState bean="#{sissGradeExportBean}" />

		<h2><h:outputText value="#{msgs.siss_export_page_title}"/></h2>

		<div class="indnt1">
			<div class="instruction">
				<p><h:outputText value="#{msgs.siss_export_instruction_export1}" escape="false"/></p>
				<p><h:outputText value="#{msgs.siss_export_instruction_export2}" escape="false"/></p>
			</div>
		</div>

		<div class="indnt1">

<!-- Grade Conversion -->
		<%@include file="/inc/globalMessages.jspf"%>
<!-- GRADE MAPPING TABLE -->
		<div class="gbSection" style="display: none;">
		  <h:panelGrid cellpadding="0" cellspacing="0"
			columns="1"
			columnClasses="itemName"
			styleClass="itemSummary">
				<h:panelGroup>
					<h4><h:outputText value="#{msgs.siss_export_threshold_label}"/><f:verbatim>:</f:verbatim></h4>
				</h:panelGroup>
			</h:panelGrid>
			<t:dataTable cellpadding="0" cellspacing="0"
				id="mappingTable"
				value="#{sissGradeExportBean.gradeRows}"
				var="gradeRow"
				columnClasses="shorttext"
				styleClass="listHier narrowTable">
				<h:column>
					<h:outputText value="#{gradeRow.grade}"/>
				</h:column>
				<h:column><f:verbatim>...</f:verbatim></h:column>
				<h:column>
					<h:outputText value="#{gradeRow.bottomValue}">
						<f:convertNumber pattern="##"/>
					</h:outputText>
				</h:column>
				<h:column>
					<f:verbatim>-</f:verbatim>
				</h:column>
				<h:column>
					<h:outputText value="#{gradeRow.topValue}">
						<f:convertNumber pattern="##"/>
					</h:outputText>
				</h:column>
			</t:dataTable>
		</div>
<!--  GRADE TYPE SELECTOR -->
			<div class="gbSection">
				<h4><h:outputText value="#{msgs.siss_export_gradetype_label}"/><f:verbatim>:</f:verbatim></h4>
				<h:selectOneRadio value="#{sissGradeExportBean.selectedGradeType}">
					<f:selectItem itemLabel="#{msgs.siss_export_gradetype_midterm}" itemValue="MID" />
					<f:selectItem itemLabel="#{msgs.siss_export_gradetype_final}" itemValue="FIN" />
				</h:selectOneRadio>
			</div>
		</div> <!-- END INDNT1 -->

		<p class="act">
			<h:commandButton
				id="saveButton"
				styleClass="active"
				value="#{msgs.siss_export_continue}"
				action="#{sissGradeExportBean.showStudentGrades}" >
			</h:commandButton>
		</p>

	</h:form>
  </div>
</f:view>
