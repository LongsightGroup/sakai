/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008, 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.tool.assessment.contentpackaging;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.tool.assessment.data.dao.assessment.Answer;
import org.sakaiproject.tool.assessment.data.dao.assessment.AnswerFeedback;
import org.sakaiproject.tool.assessment.data.dao.assessment.AssessmentAttachment;
import org.sakaiproject.tool.assessment.data.dao.assessment.AssessmentData;
import org.sakaiproject.tool.assessment.data.dao.assessment.ItemAttachment;
import org.sakaiproject.tool.assessment.data.dao.assessment.ItemData;
import org.sakaiproject.tool.assessment.data.dao.assessment.ItemFeedback;
import org.sakaiproject.tool.assessment.data.dao.assessment.ItemText;
import org.sakaiproject.tool.assessment.data.dao.assessment.SectionAttachment;
import org.sakaiproject.tool.assessment.data.dao.assessment.SectionData;
import org.sakaiproject.tool.assessment.data.dao.questionpool.QuestionPoolData;
import org.sakaiproject.tool.assessment.data.ifc.shared.TypeIfc;
import org.sakaiproject.tool.assessment.facade.AgentFacade;
import org.sakaiproject.tool.assessment.facade.AssessmentFacade;
import org.sakaiproject.tool.assessment.facade.ItemFacade;
import org.sakaiproject.tool.assessment.facade.QuestionPoolFacade;
import org.sakaiproject.tool.assessment.facade.SectionFacade;
import org.sakaiproject.tool.assessment.qti.util.XmlUtil;
import org.sakaiproject.tool.assessment.services.QuestionPoolService;
import org.sakaiproject.tool.assessment.services.assessment.AssessmentService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Copyright: Copyright (c) 2007 Sakai
 * </p>
 * 
 * @version $Id$
 */

@Slf4j
public class ManifestGenerator {

	/**default namespace and metadata namespace*/
	private static final String DEFAULT_NAMESPACE_URI = "http://www.imsglobal.org/xsd/imscp_v1p1";
	private static final String DEFAULT_NAMESPACE_SCHEMA_LOCATION = "http://www.imsglobal.org/xsd/imscp_v1p1.xsd";
	private static final String IMSMD_NAMESPACE_URI = "http://www.imsglobal.org/xsd/imsmd_v1p2";
	private static final String IMSMD_NAMESPACE_SCHEMA_LOCATION = "http://www.imsglobal.org/xsd/imsmd_v1p2.xsd";

	private static final String EXPORT_ASSESSMENT = "exportAssessment";
	private static final String EXPORT_ASSESSMENT_XML = EXPORT_ASSESSMENT + ".xml";
	private Document document;
	private String assessmentId;
	private Long questionPoolId;
	private HashMap contentMap = new HashMap();
	private ContentHostingService contentHostingService;

	public ManifestGenerator(String assessmentId) {
		this.assessmentId = assessmentId;
	}

	public ManifestGenerator(Long questionPoolId) {
		this.questionPoolId = questionPoolId;
	}

	public String getManifest() {
		document = readXMLDocument();
		
		if (document == null) {
			log.info("document == null");
			return "";
		}
		String xmlString = XmlUtil.getDOMString(document);
		String newXmlString = xmlString;
		if (xmlString.startsWith("<?xml version")) {
			newXmlString = xmlString.replaceFirst("version=\"1.0\"",
					"version=\"1.0\"  encoding=\"UTF-8\"");
		}
		log.debug(newXmlString);
		return newXmlString;
	}

	public HashMap getContentMap() {
		return contentMap;
	}

	private Document readXMLDocument() {
		document = getDocument();
		
		// Need to create upper level metadata elements
		Element manifestElement = createManifestElement();
		document.appendChild(manifestElement);
		
		// create manifest metadata
		Element metadata = createDefaultNSElement("metadata");
		appendLOMMetadataToElement(EXPORT_ASSESSMENT, metadata);
		manifestElement.appendChild(metadata);
		
		// Need organizations element
		Element organizationsElement = createOrganizationsElement();
		manifestElement.appendChild(organizationsElement);

		// resources
		Element resourcesElement = createDefaultNSElement("resources");
		// resource
		Element resourceElement = createResourceElement();
		Element xmlFileElement = createFileElement(EXPORT_ASSESSMENT_XML);
		resourceElement.appendChild(xmlFileElement);
		
		setContentHostingService();
		getAttachments();
		getFCKAttachments();
		Iterator iter = contentMap.keySet().iterator();
		String filename = null;
		Element fileElement = null;
		while (iter.hasNext()) {
			filename = ((String) iter.next()).replaceAll(" ", "");
			fileElement = createFileElement(filename);
			resourceElement.appendChild(fileElement);
		}
		resourcesElement.appendChild(resourceElement);
		manifestElement.appendChild(resourcesElement);

		return document;
	}

	private void getAttachments() {
		try {
			if (StringUtils.isNotBlank(assessmentId)) {
				AssessmentService assessmentService = new AssessmentService();
				AssessmentFacade assessment = assessmentService
						.getAssessment(assessmentId);

				// Assessment attachment
				AssessmentData assessmentData = (AssessmentData) assessment
						.getData();
				Set assessmentAttachmentSet = assessmentData
						.getAssessmentAttachmentSet();
				Iterator assessmentAttachmentIter = assessmentAttachmentSet
						.iterator();
				byte[] content = null;
				String resourceId = null; // resoureId is also the filename (whole
				// path) in the zip file
				while (assessmentAttachmentIter.hasNext()) {
					AssessmentAttachment assessmentAttachment = (AssessmentAttachment) assessmentAttachmentIter
							.next();
					resourceId = assessmentAttachment.getResourceId();
					content = contentHostingService.getResource(resourceId)
							.getContent();
					contentMap.put(resourceId.replace(" ", ""), content);
				}

				// Section attachment
				Set<SectionFacade> sectionSet = assessment.getSectionSet();
				for (SectionFacade sectionFacade : sectionSet) {
					SectionData sectionData = (SectionData) sectionFacade.getData();
					Set<SectionAttachment> sectionAttachmentSet = sectionData.getSectionAttachmentSet();
					for (SectionAttachment sectionAttachment : sectionAttachmentSet) {
						contentMap.put(sectionAttachment.getResourceId().replace(" ", ""), 
							contentHostingService.getResource(sectionAttachment.getResourceId()).getContent());
					}
					for (ItemData itemData : (Set<ItemData>) sectionData.getItemSet()) {
						Set<ItemAttachment> attachments = itemData.getItemAttachmentSet().stream()
								.map(attachment -> (ItemAttachment) attachment)
								.collect(Collectors.toSet());
						for (ItemAttachment itemAttachment : attachments) {
							contentMap.put(itemAttachment.getResourceId().replace(" ", ""), 
								contentHostingService.getResource(itemAttachment.getResourceId()).getContent());
						}
					}
				}
			} else if (StringUtils.isNotBlank(questionPoolId.toString())) {
				QuestionPoolService questionPoolService = new QuestionPoolService();

				// Question pool attachment
				List<ItemFacade> items = questionPoolService.getAllItems(questionPoolId);
				for (ItemFacade itemFacade : items) {
					for (ItemAttachment itemAttachment : (Set<ItemAttachment> ) itemFacade.getItemAttachmentSet()) {
						contentMap.put(itemAttachment.getResourceId().replace(" ", ""), 
							contentHostingService.getResource(itemAttachment.getResourceId()).getContent());
					}
					Set<ItemAttachment> attachments = itemFacade.getData().getItemAttachmentSet().stream()
							.map(attachment -> (ItemAttachment) attachment)
							.collect(Collectors.toSet());
					for (ItemAttachment itemAttachment : attachments) {
						contentMap.put(itemAttachment.getResourceId().replace(" ", ""), 
							contentHostingService.getResource(itemAttachment.getResourceId()).getContent());
					}
				}
			}

		} catch (PermissionException e) {
			log.error(e.getMessage());
		} catch (IdUnusedException e) {
			log.error(e.getMessage());
		} catch (TypeException e) {
			log.error(e.getMessage());
		} catch (ServerOverloadException e) {
			log.error(e.getMessage());
		}
	}
	
	private void getFCKAttachments() {
		if (StringUtils.isNotBlank(assessmentId)) {
			AssessmentService assessmentService = new AssessmentService();
			AssessmentFacade assessment = assessmentService
					.getAssessment(assessmentId);

			// Assessment FCK attachment
			AssessmentData assessmentData = (AssessmentData) assessment.getData();
			processDescription(assessmentData.getDescription());
			processDescription(assessmentData.getAssessmentAccessControl().getSubmissionMessage());

			// Section FCK attachment
			Set<SectionFacade> sectionSet = assessment.getSectionSet();
			for (SectionFacade section : sectionSet) {
				SectionData sectionData = (SectionData) section.getData();
				processDescription(sectionData.getDescription());

				Set itemSet = sectionData.getItemSet();
				for (ItemData itemData : (Set<ItemData>) itemSet) {
					processItemData(itemData);
				}
			}
		} else if (StringUtils.isNotBlank(questionPoolId.toString())) {
			QuestionPoolService questionPoolService = new QuestionPoolService();
			QuestionPoolFacade questionPool = questionPoolService.getPool(questionPoolId, AgentFacade.getAgentString());

			// Question pool FCK attachment
			QuestionPoolData questionPoolData = (QuestionPoolData) questionPool.getData();
			processDescription(questionPoolData.getDescription());

			List<ItemFacade> items = questionPoolService.getAllItems(questionPoolId);
			Set<ItemFacade> itemSetFacade = new HashSet<>(items);

			for (ItemFacade itemFacade : itemSetFacade) {
				ItemData itemData = (ItemData) itemFacade.getData();
				processItemData(itemData);
			}
		}
	}

	private void processItemData (ItemData itemData) {

		// Question Text
		if (itemData.getTypeId().equals(TypeIfc.MATCHING) || itemData.getTypeId().equals(TypeIfc.CALCULATED_QUESTION)) {
			processDescription(itemData.getInstruction());
		}

		Set itemTextSet = itemData.getItemTextSet();
		for (ItemText itemText : (Set<ItemText>) itemTextSet) {
			processDescription(itemText.getText());

			// Answer
			Set answerSet = itemText.getAnswerSet();
			for (Answer answer : (Set<Answer>)answerSet) {
				processDescription(answer.getText());

				// Answer Feedback
				Set answerFeedbackSet = answer.getAnswerFeedbackSet();
				for (AnswerFeedback answerFeedback : (Set<AnswerFeedback>) answerFeedbackSet) {
					processDescription(answerFeedback.getText());
				}
			}
		}

		// Feedback
		Set itemFeedbackSet = itemData.getItemFeedbackSet();
		for (ItemFeedback itemFeedback : (Set<ItemFeedback>) itemFeedbackSet) {
			processDescription(itemFeedback.getText());
		}
	}

	private void processDescription(String description) {
		String prependString = ServerConfigurationService.getAccessUrl()
				+ contentHostingService.REFERENCE_ROOT;

		// Hardcode here for now because I cannot find the API to get them
		// Also, it is hardcoded in BaseContentService.java
		String siteCollection = "/group/";
		String userCollection = "/user/";
		String attachment = "/attachment/";
		User user = UserDirectoryService.getCurrentUser();
		String userId = user.getId();
		String eid = user.getEid();

		try {
			if (description != null && (description.indexOf("<img ") > -1 || description.indexOf("<a ")  > -1 || description.indexOf("<a\n")  > -1)) {	
				byte[] content = null;
				int srcStartIndex = 0;
				int srcEndIndex = 0;
				String src = null;
				String resourceId = null;
				String eidResourceId = null;

				String[] splittedString = description.split("<img ");
				for (int i = 0; i < splittedString.length; i++) {
					log.debug("splittedString[" + i + "] = "
							+ splittedString[i]);
					String[] splittedRefString = splittedString[i].split("<a[\n|\\s]+?");
					for (int j = 0; j < splittedRefString.length; j++) {
						if (splittedRefString[j].indexOf(prependString) > -1) {
							int offset = 5;
							srcStartIndex = splittedRefString[j].indexOf("src=\"");
							if(srcStartIndex == -1){
								srcStartIndex = splittedRefString[j].indexOf("href=\"");
								offset++;
							}
							srcEndIndex = splittedRefString[j].indexOf("\"",
									srcStartIndex + offset);
							src = splittedRefString[j].substring(srcStartIndex + offset,
									srcEndIndex);

							try {
								src = URLDecoder.decode(src, "UTF-8");
							} catch (UnsupportedEncodingException e) {
								log.error(e.getMessage());
							}

							if (src.indexOf(siteCollection) > -1) {
								resourceId = src.replace(prependString, "");
								content = contentHostingService.getResource(resourceId).getContent();
								if (content != null) {
									contentMap.put(resourceId.replace(" ", ""), content);
								}
							}
							else if (src.indexOf(userCollection) > -1) {
								eidResourceId = src.replace(prependString, "");
								resourceId = eidResourceId.replace(eid, userId);
								content = contentHostingService.getResource(resourceId).getContent();
								if (content != null) {
									contentMap.put(eidResourceId.replace(" ", ""), content);
								}
							}
							else if (src.indexOf(attachment) > -1) {
								resourceId = src.replace(prependString, "");
								content = contentHostingService.getResource(resourceId).getContent();
								if (content != null) {
									contentMap.put(resourceId.replace(" ", ""), content);
								}
							}
							else {
								log.error("Neither group nor user");
							}
						}
					}
				}
			}
		} catch (PermissionException e) {
			log.error(e.getMessage());
		} catch (IdUnusedException e) {
			log.error(e.getMessage());
		} catch (TypeException e) {
			log.error(e.getMessage());
		} catch (ServerOverloadException e) {
			log.error(e.getMessage());
		}
	}
	
	public void setContentHostingService(){
		if (contentHostingService == null) {
			this.contentHostingService = AssessmentService.getContentHostingService();
		}
	}
	
	private Element createManifestElement()
	{
		Element manifest = createDefaultNSElement("manifest");
		manifest.setAttribute("identifier", "MANIFEST1");
		manifest.setAttribute("xmlns", DEFAULT_NAMESPACE_URI);
		manifest.setAttribute("xmlns:imsmd", IMSMD_NAMESPACE_URI);
		manifest.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		manifest.setAttribute("xsi:schemaLocation",
				DEFAULT_NAMESPACE_URI + " " + DEFAULT_NAMESPACE_SCHEMA_LOCATION + " "
				+ IMSMD_NAMESPACE_URI  + " " + IMSMD_NAMESPACE_SCHEMA_LOCATION);
				
		return manifest;
	}

	public void appendLOMMetadataToElement(String title, Element parent)
	{
		// imsmd:lom
		Element imsmdLom = createLOMElement("lom");
		// imsmd:general
		Element imsmdGeneral = createLOMElement("general");
		// imsmd:identifier
		String identifier = Long.toHexString((new Date()).getTime());
		Element imsmdIdentifier = createLOMElementWithLangstring("identifier", identifier);
		imsmdGeneral.appendChild(imsmdIdentifier);
		// imsmd:title
		Element imsmdTitle = createLOMElementWithLangstring("title", title);
		imsmdGeneral.appendChild(imsmdTitle);
		imsmdLom.appendChild(imsmdGeneral);
		parent.appendChild(imsmdLom);
	}

	private Element createOrganizationsElement()
	{
		Element organizations = createDefaultNSElement("organizations");
		return organizations;
	}

	public Element createResourceElement() 
	{
		Element resourceElement = createDefaultNSElement("resource");
		resourceElement.setAttributeNS(DEFAULT_NAMESPACE_URI, "identifier", "RESOURCE1");
		resourceElement.setAttributeNS(DEFAULT_NAMESPACE_URI, "type","imsqti_xmlv1p1");
		resourceElement.setAttributeNS(DEFAULT_NAMESPACE_URI, "href", EXPORT_ASSESSMENT_XML);
		
		return resourceElement;
	}

	public Element createFileElement(String href) 
	{
		Element fileElement = createDefaultNSElement("file");
		fileElement.setAttributeNS(DEFAULT_NAMESPACE_URI, "href", href);
		
		return fileElement;
	}
	
	private Element createDefaultNSElement(String elename) {

		return getDocument().createElementNS(DEFAULT_NAMESPACE_URI, elename);
	}

	private Element createLOMElement(String elename) {

		Element imsmdlom = getDocument().createElementNS(IMSMD_NAMESPACE_URI, elename);
		imsmdlom.setPrefix("imsmd");

		return imsmdlom;
	}
	
	
	private Element createLOMElementWithLangstring(String elementName, String text) {
		
		Element element = createLOMElement(elementName);
		//imsmd:langstring
		Element imsmdlangstring = createLOMElement("langstring");
		imsmdlangstring.setAttribute("xml:lang", "en-US");
		setNodeValue(imsmdlangstring, text);
		element.appendChild(imsmdlangstring);

		return element;
	}
	
	private void setNodeValue( Element parent, String data ) {
		Text textNode = getDocument().createTextNode(data);
		parent.appendChild( textNode );
	}

	private Document getDocument() {
		if (document != null)
			return document;
		else
			document = XmlUtil.createDocument();
		return document;
	}

}
