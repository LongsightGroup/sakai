package org.sakaiproject.lessonbuildertool.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.app.scheduler.DelayedInvocation;
import org.sakaiproject.api.app.scheduler.ScheduledInvocationManager;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.email.cover.EmailService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.lessonbuildertool.ActivityAlert;
import org.sakaiproject.lessonbuildertool.SimplePage;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;

public class ActivityAlertServiceImpl implements ActivityAlertService {
	private static Log log = LogFactory.getLog(ActivityAlertServiceImpl.class);
	private ScheduledInvocationManager scheduledInvocationManager;
	private TimeService timeService;
	private SimplePageToolDao simplePageToolDao;
	private static final String ID_DELIMITER = ";";
	private static final String MULTIPART_BOUNDARY = "======sakai-multi-part-boundary======";
	private static final String BOUNDARY_LINE = "\n\n--"+MULTIPART_BOUNDARY+"\n";
	private static final String TERMINATION_LINE = "\n\n--"+MULTIPART_BOUNDARY+"--\n\n";
	private static final String MIME_ADVISORY = "This message is for MIME-compliant mail readers.";
	private static final String PLAIN_TEXT_HEADERS= "Content-Type: text/plain\n\n";
	private static final String HTML_HEADERS = "Content-Type: text/html; charset=ISO-8859-1\n\n";
	private static final String HTML_END = "\n  </body>\n</html>\n";

	@Override
	public void scheduleActivityAlert(ActivityAlert alert) {
		log.debug("ActivityAlertServiceImpl.scheduleActivityAlert");
		
		if(alert != null 
				&& StringUtils.isNotBlank(alert.getSiteId()) 
				&& StringUtils.isNotBlank(alert.getTool())
				&& StringUtils.isNotBlank(alert.getReference())) {

			//first clear out any existing scheduled alerts:
			clearActivityAlert(alert);
		
			if(alert.getBeginDate() != null && alert.getRecurrence() != null
					&& (StringUtils.isNotBlank(alert.getStudentRecipients()) || StringUtils.isNotBlank(alert.getNonStudentRecipients()))) {
				Date scheduleDate = alert.getBeginDate();
				Calendar c = Calendar.getInstance();
				Date now = c.getTime();
				
				if(ActivityAlert.RECURRENCCE_ONCE != alert.getRecurrence().intValue()) {
					while(scheduleDate.before(now)) {
						c.setTime(scheduleDate);
						//find the next DATE 
						if(ActivityAlert.RECURRENCCE_DAILY == alert.getRecurrence()) {
							c.add(Calendar.DAY_OF_YEAR, 1);
						} else if(ActivityAlert.RECURRENCCE_WEEKLY == alert.getRecurrence()) {
							c.add(Calendar.DAY_OF_YEAR, 7);
						} else {
							break;
						}
						scheduleDate = c.getTime();
					}
					if(alert.getEndDate() == null) {
						alert.setEndDate(alert.getBeginDate());
					}
					if(scheduleDate.after(alert.getEndDate())) {
						scheduleDate = null;
					}
				}
				if(scheduleDate != null && scheduleDate.after(now)) {
					log.debug("Scheduling alert at: " + scheduleDate);
					Time scheduleTime = timeService.newTime(scheduleDate.getTime());
					scheduledInvocationManager.createDelayedInvocation(scheduleTime, "org.sakaiproject.lessonbuildertool.model.ActivityAlertService", getActivitySchedulerId(alert));
				}
			}		
		}	
	}

	@Override
	public void clearActivityAlert(ActivityAlert alert) {
		log.debug("ActivityAlertServiceImpl.clearActivityAlert");
		// Remove any existing notifications for this area
		DelayedInvocation[] fdi = scheduledInvocationManager.findDelayedInvocations("org.sakaiproject.lessonbuildertool.model.ActivityAlertService", getActivitySchedulerId(alert));
		if (fdi != null && fdi.length > 0)
		{
			for (DelayedInvocation d : fdi)
			{
				scheduledInvocationManager.deleteDelayedInvocation(d.uuid);
			}
		}
	}
	
	private String getActivitySchedulerId(ActivityAlert alert) {
		return alert.getSiteId() + ID_DELIMITER + alert.getTool() + ID_DELIMITER + alert.getReference();
	}
	
	@Override
	public void execute(String id) {
		log.debug("ActivityAlertServiceImpl.execute");

		String[] idSplit = id.split(ID_DELIMITER);
		if(idSplit.length == 3){
			ActivityAlert alert = simplePageToolDao.findActivityAlert(idSplit[0], idSplit[1], idSplit[2]);
			if(alert != null){
				Integer pageId = null;
				try{
					pageId = Integer.parseInt(alert.getReference());
				}catch(Exception e){
					log.error(e.getMessage(), e);
				}
				if(pageId != null){
					SimplePage lessonPage = simplePageToolDao.getPage(pageId);
					if(lessonPage != null){
						try {
							Site alertSite = SiteService.getSite(alert.getSiteId());
							if(alertSite != null){			
								//get list of students:
								Set<String> inactiveStudentIds = new HashSet<String>();
								Set<String> studentRecipientRoles = alert.getStudentRecipientsType(ActivityAlert.RECIPIENT_TYPE_ROLE);
								if(!studentRecipientRoles.isEmpty()){
									for(String role : studentRecipientRoles){
										inactiveStudentIds.addAll(alertSite.getUsersHasRole(role));
									}
								}
								//get list of non students:
								Set<String> inactiveNonStudentIds = new HashSet<String>();
								Set<String> nonStudentRecipientRoles = alert.getNonStudentRecipientsType(ActivityAlert.RECIPIENT_TYPE_ROLE);
								if(!nonStudentRecipientRoles.isEmpty()){
									for(String role : nonStudentRecipientRoles){
										inactiveNonStudentIds.addAll(alertSite.getUsersHasRole(role));
									}
								}

								Set<String> checkUsersIds = new HashSet<String>(inactiveStudentIds);
								checkUsersIds.addAll(inactiveNonStudentIds);

								//Look up User's emails and email them if possible:
								List<User> inactiveUsers = new ArrayList<User>();
								Set<User> failedEmailUsers = new HashSet<User>();
								Set<User> alertNonStudentUsers = new HashSet<User>();
								Set<User> alertStudentUsers = new HashSet<User>();

								//now check the checkUsersIds users have any activity, filter out the ones that don't:
								for(String userId : checkUsersIds){
									User user = null;
									try {
										user = UserDirectoryService.getUser(userId);
										
										if (!simplePageToolDao.hasActivityForPage(userId, pageId)) {
											// user is inactive for this page
											inactiveUsers.add(user);
											if(StringUtils.isNotBlank(user.getEmail())){
												//send email:
												if(inactiveNonStudentIds.contains(userId)){
													alertNonStudentUsers.add(user);
												}else{
													alertStudentUsers.add(user);
												}											
											}else{
												failedEmailUsers.add(user);
											}
										}
									} catch (UserNotDefinedException e) {
										log.error(e.getMessage(), e);
										failedEmailUsers.add(user);
									}
								}
								String defaultAlertMessage = "No activity has been reported for your account in " + alertSite.getTitle() + ":" + lessonPage.getTitle();
								String defaultAlertSubject = "[" + alertSite.getTitle() + "] inactivity for page " + lessonPage.getTitle();
								if(alertStudentUsers.size() > 0){
									String alertMessage = defaultAlertMessage;
									if(StringUtils.isNotBlank(alert.getStudentMessage())){
										alertMessage = alert.getStudentMessage();
									}
									EmailService.sendToUsers(alertStudentUsers, getHeaders(defaultAlertSubject), formatMessage(defaultAlertSubject, alertMessage));
								}
								if(alertNonStudentUsers.size() > 0){
									String alertMessage = defaultAlertMessage;
									if(StringUtils.isNotBlank(alert.getNonStudentMessage())){
										alertMessage = alert.getNonStudentMessage();
									}
									EmailService.sendToUsers(alertNonStudentUsers, getHeaders(defaultAlertSubject), formatMessage(defaultAlertSubject, alertMessage));
								}

								// Get roles to send inactivity report to
								// lessons.inactivity.notify.roles should contain roles matching SAKAI_REALM_ROLE.ROLE_NAME
								String[] rolesToNotify = ServerConfigurationService.getStrings("lessons.inactivity.notify.roles");
								if (rolesToNotify == null || rolesToNotify.length == 0) {
									rolesToNotify = new String[] { alertSite.getMaintainRole() };
								}
								List<String> listRolesToNotify = Arrays.asList(rolesToNotify);
								Set<String> userIdsToNotify = alertSite.getRoles().stream()
										.filter(r -> listRolesToNotify.contains(r.getId()))
										.flatMap(r -> alertSite.getUsersHasRole(r.getId()).stream())
										.collect(Collectors.toSet());
								List<User> notifyUsers = UserDirectoryService.getUsers(userIdsToNotify);
								Set<User> maintainerUsers = new HashSet<>();
								for (User user: notifyUsers) {
									if (StringUtils.isNotBlank(user.getEmail())) {
										maintainerUsers.add(user);
									} else {
										failedEmailUsers.add(user);
									}
								}
								if(maintainerUsers.size() > 0){
									StringBuilder maintainerMessage = new StringBuilder();
									String maintainerSubject = "[" + alertSite.getTitle() + "] Inactivity Report for page " + lessonPage.getTitle();
									if(inactiveUsers.size() == 0){
										maintainerMessage.append("All users have activity logged in " + alertSite.getTitle() + ":" + lessonPage.getTitle());
									}else{
										Collections.sort(inactiveUsers, new Comparator<User>(){
											@Override
											public int compare(User o1, User o2) {
												return o1.getDisplayName().compareTo(o2.getDisplayName());
											}											
										});
										maintainerMessage.append("The following users have not logged any activity in " + alertSite.getTitle() + ":" + lessonPage.getTitle() + "\n\n");
										for(User user : inactiveUsers){
											maintainerMessage.append(user.getDisplayName() + "\n");
										}										
										if(failedEmailUsers.size() > 0){
											maintainerMessage.append("\n");
											maintainerMessage.append("Alert: The following users do not have an email associated with their account and haven't been notified:\n\n");
											for(User user : failedEmailUsers){
												maintainerMessage.append(user.getDisplayName() + "\n");
											}
										}
									}
									EmailService.sendToUsers(maintainerUsers, getHeaders(maintainerSubject), formatMessage(maintainerSubject, maintainerMessage.toString()));
								}
							}
						} catch (IdUnusedException e) {
							log.error(e.getMessage(), e);
						}
					}
				}
				//Now see if you there needs to be a recurrence or not:
				scheduleActivityAlert(alert);
			}
		}
	}
	
	private List<String> getHeaders(String subject)
    {
            List<String> rv = new ArrayList<String>();

            rv.add("MIME-Version: 1.0");
            rv.add("Content-Type: multipart/alternative; boundary=\""+MULTIPART_BOUNDARY+"\"");
            // set the subject
            rv.add(formatSubject(subject));
            // from
            rv.add("From: " + "\"" + ServerConfigurationService.getString("ui.service", "Sakai") + "\" <"+ ServerConfigurationService.getString("setup.request","no-reply@"+ ServerConfigurationService.getServerName()) + ">");

            return rv;
    }
	
	private String formatSubject(String subject) {
		return "Subject: " + subject;
	}
	
	/** helper methods for formatting the message */
	private String formatMessage(String subject, String message) {
		StringBuilder sb = new StringBuilder();
		sb.append(MIME_ADVISORY);
		sb.append(BOUNDARY_LINE);
		sb.append(PLAIN_TEXT_HEADERS);
		sb.append(StringEscapeUtils.escapeHtml(message));
		sb.append(BOUNDARY_LINE);
		sb.append(HTML_HEADERS);
		sb.append(htmlPreamble(subject));
		sb.append(message);
		sb.append(HTML_END);
		sb.append(TERMINATION_LINE);
		
		return sb.toString();
	}
	
	private String htmlPreamble(String subject) {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n");
		sb.append("\"http://www.w3.org/TR/html4/loose.dtd\">\n");
		sb.append("<html>\n");
		sb.append("<head><title>");
		sb.append(subject);
		sb.append("</title></head>\n");
		sb.append("<body>\n");
		
		return sb.toString();
	}
	
	public ScheduledInvocationManager getScheduledInvocationManager() {
		return scheduledInvocationManager;
	}

	public void setScheduledInvocationManager(
			ScheduledInvocationManager scheduledInvocationManager) {
		this.scheduledInvocationManager = scheduledInvocationManager;
	}

	public TimeService getTimeService() {
		return timeService;
	}

	public void setTimeService(TimeService timeService) {
		this.timeService = timeService;
	}

	public SimplePageToolDao getSimplePageToolDao() {
		return simplePageToolDao;
	}

	public void setSimplePageToolDao(SimplePageToolDao simplePageToolDao) {
		this.simplePageToolDao = simplePageToolDao;
	}

}
