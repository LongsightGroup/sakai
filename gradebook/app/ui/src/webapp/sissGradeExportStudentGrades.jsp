<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t" %>
<%@ taglib uri="http://sakaiproject.org/jsf/sakai" prefix="sakai" %>
<%@ taglib uri="http://sakaiproject.org/jsf/gradebook" prefix="gbx"%>
<f:view>
  <div class="portletBody">
	<h:form id="gbForm">
		<t:aliasBean alias="#{bean}" value="#{sissGradeExportBean}">
			<%@include file="/inc/appMenu.jspf"%>
		</t:aliasBean>
		<sakai:flowState bean="#{sissGradeExportBean}" />
		<t:jsValueSet name="mainWindow" value=""/>

		<h2><h:outputText value="#{msgs.siss_export_page_title}"/></h2>

		<div class="instruction">
			<p><h:outputText value="#{msgs.siss_export_instruction_header}" escape="false"/></p>
			<p><h:outputText value="#{msgs.siss_export_instruction_inst}" escape="false"/></p>
			<p>
			  <ol>
				<li><h:outputText value="#{msgs.siss_export_instruction_inst1}" escape="false"/></li>
				<li><h:outputText value="#{msgs.siss_export_instruction_inst2}" escape="false"/></li>
				<li><h:outputText value="#{msgs.siss_export_instruction_inst3}" escape="false"/></li>
			  </ol>
			</p>
		</div>
			<div class="gbSection">
				<h:outputText value="#{msgs.siss_export_gradetype_for_message}"/><f:verbatim> </f:verbatim>
				<h:outputText value="#{msgs.siss_export_gradetype_midterm}" rendered="#{sissGradeExportBean.selectedGradeType eq 'MID'}" />
				<h:outputText value="#{msgs.siss_export_gradetype_final}" rendered="#{sissGradeExportBean.selectedGradeType eq 'FIN'}" />
			</div>

			<div class="gbSection">
				<t:dataTable
					styleClass="ssisExportTable"
					value="#{sissGradeExportBean.sectionEnrollmentList}" var="sectionEnrollment"
					rowIndexVar="rowIndex" preserveRowStates="true">
					<t:column>
					    <t:div styleClass="ssisExportSectionHeader">
							<t:panelGroup >
								<h:graphicImage value="/images/collapse.gif"
										onclick="javascript:showHideDiv1(this, 'gradeData', '/sakai-gradebook-tool', 'expand', 'collapse', 'Expand Section', 'Collapse Section'); resize()"/>
								<f:verbatim> </f:verbatim>
								<h:outputText value="#{sectionEnrollment.section.title}"/>
								<f:verbatim> - </f:verbatim>
								<h:outputText value="#{msgs.siss_export_section_graded}" rendered="#{sectionEnrollment.graded}"/>
								<h:outputText value="#{msgs.siss_export_section_ungraded}" rendered="#{!sectionEnrollment.graded}"/>
							</t:panelGroup>
							<t:panelGroup styleClass="ssisExportSectionDownload">
								<h:commandLink action="#{sectionEnrollment.downloadSection}">
								 	<f:param value="#{rowIndex}" name="currentDownloadSectionId"/>
								 	<h:outputText value="#{msgs.siss_export_section_download}"/>
								</h:commandLink>
							</t:panelGroup>
						</t:div>
						<t:dataTable id="gradeData" preserveRowStates="true"
							styleClass="ssisExportGradeTable"
							style="display: none;"
							value="#{sectionEnrollment.scoreRows}" var="scoreRow">
							<t:column styleClass="ssisExportUserName">>
								<h:outputText value="#{scoreRow.dukeUserName}"/>
								<f:verbatim> .... </f:verbatim>
							</t:column>
							<t:column styleClass="ssisExportGrade">
									<h:outputText value="#{scoreRow.courseGradeRecord.displayGrade}"/>
							</t:column>
						</t:dataTable>
					</t:column>
				</t:dataTable>
		</div>
<!-- Grade Conversion -->
		<%@include file="/inc/globalMessages.jspf"%>
		<p class="act">
			<h:commandButton
				id="backButton"
				styleClass="active"
				value="#{msgs.siss_export_back}"
				action="#{sissGradeExportBean.showExportChoices}" >
			</h:commandButton>
		</p>

		<%
			String thisId = request.getParameter("panel");
			if (thisId == null) {
			    thisId = "Main" + org.sakaiproject.tool.cover.ToolManager.getCurrentPlacement().getId();
			}
		%>
		<script type="text/javascript">
			function resize(){
				mySetMainFrameHeight('<%= org.sakaiproject.util.Web.escapeJavascript(thisId)%>');
			}
		</script>
	</h:form>
  </div>
</f:view>
