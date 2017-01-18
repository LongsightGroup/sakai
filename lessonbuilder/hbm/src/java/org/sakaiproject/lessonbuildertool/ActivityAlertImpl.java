package org.sakaiproject.lessonbuildertool;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class ActivityAlertImpl implements ActivityAlert, Serializable{

	private static final long serialVersionUID = 2045356417886560929L;
	private String tool;
	private String siteId;
	private String reference;
	private String studentMessage;
	private String nonStudentMessage;
	private Integer recurrence;
	private Date beginDate;
	private Date endDate;
	private String studentRecipients;
	private String nonStudentRecipients;
	
	public String getStudentMessage() {
		return studentMessage;
	}
	public void setStudentMessage(String studentMessage) {
		this.studentMessage = studentMessage;
	}
	public String getNonStudentMessage() {
		return nonStudentMessage;
	}
	public void setNonStudentMessage(String nonStudentMessage) {
		this.nonStudentMessage = nonStudentMessage;
	}
	public Integer getRecurrence() {
		return recurrence;
	}
	public void setRecurrence(Integer recurrence) {
		this.recurrence = recurrence;
	}
	public Date getBeginDate() {
		return beginDate;
	}
	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public String getStudentRecipients() {
		return studentRecipients;
	}
	public void setStudentRecipients(String studentRecipients) {
		this.studentRecipients = studentRecipients;
	}
	public String getNonStudentRecipients() {
		return nonStudentRecipients;
	}
	public void setNonStudentRecipients(String nonStudentRecipients) {
		this.nonStudentRecipients = nonStudentRecipients;
	}
	public String getTool() {
		return tool;
	}
	public void setTool(String tool) {
		this.tool = tool;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getSiteId() {
		return siteId;
	}
	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}
	@Override
	public Set<String> getStudentRecipientsType(String type) {
		return getRecipientsType(studentRecipients, type);
	}
	@Override
	public Set<String> getNonStudentRecipientsType(String type) {
		return getRecipientsType(nonStudentRecipients, type);
	}
	
	private Set<String> getRecipientsType(String recipients, String type){
		Set<String> recipientsTypeList = new HashSet<String>();
		if(StringUtils.isNotBlank(recipients)){
			for(String recipient : recipients.split(RECIPIENT_DELIMITER)){
				if(recipient.startsWith(type)){
					recipient = recipient.substring(type.length());
					recipientsTypeList.add(recipient);
				}						
			}
		}
		return recipientsTypeList;
	}
}
