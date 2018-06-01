package org.sakaiproject.lessonbuildertool;

import java.util.Date;
import java.util.Set;

public interface ActivityAlert {
	
	public static final String RECIPIENT_DELIMITER = ";";
	public static final String RECIPIENT_TYPE_ROLE = "role:";
	public static final int RECURRENCCE_ONCE = 0;
	public static final int RECURRENCCE_DAILY = 1;
	public static final int RECURRENCCE_WEEKLY = 2;
	
	public String getTool();
	public void setTool(String tool);
	
	public String getReference();
	public void setReference(String reference);
	
	public String getStudentMessage();
	public void setStudentMessage(String studentMessage);
	
	public String getNonStudentMessage();
	public void setNonStudentMessage(String nonStudentMessage);
	
	public Integer getRecurrence();
	public void setRecurrence(Integer recurrence);
	
	public Date getBeginDate();
	public void setBeginDate(Date beginDate);
	
	public Date getEndDate();
	public void setEndDate(Date endDate);
	
	public String getStudentRecipients();
	public void setStudentRecipients(String studentRecipients);
	
	public String getNonStudentRecipients();
	public void setNonStudentRecipients(String nonStudentRecipients);
	
	public String getSiteId();
	public void setSiteId(String siteId);
	
	public Set<String> getStudentRecipientsType(String type);
	public Set<String> getNonStudentRecipientsType(String type);
}
