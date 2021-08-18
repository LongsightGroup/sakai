package org.sakaiproject.portal.chat.entity;

import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.UserDirectoryService;

import java.util.Map;

@Slf4j
public class ContentReportTool {

    private UserDirectoryService userDirectoryService;
    private SiteService siteService;
    private EmailService emailService;

    private final static String BAD_SENDER_EMAIL = "BAD_SENDER_EMAIL";
    private final static String BAD_DESCRIPTION = "BAD_DESCRIPTION";
    private final static String SUCCESS = "SUCCESS";
    private final static String ERROR = "ERROR_FROM_CATCH_BLOCK";


    public ContentReportTool() {
        userDirectoryService = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
        siteService = (SiteService) ComponentManager.get(SiteService.class);
        emailService = (EmailService) ComponentManager.get(EmailService.class);
    }

    public String getUserId() {
        String currentUserId = userDirectoryService.getCurrentUser().getDisplayId();
        return currentUserId;
    }

    public String getUserName() {
        String currentUserName = userDirectoryService.getCurrentUser().getDisplayName();
        return currentUserName;
    }

    public String getUserEmail() {
        String currentUserEmail = userDirectoryService.getCurrentUser().getEmail();
        return currentUserEmail;
    }

    public String sendReport(Map<String, Object> params) {

        final String siteTitle = (String) params.get("contentReport__site");
        final String problemBox = (String) params.get("contentReport__problemBox");
        final String senderEmail = getUserEmail();
        final String userId = getUserId();
        final String userAgent = (String) params.get("userAgent");
        final String directUrl = (String) params.get("directUrl");
        final String recipientEmail = "edtech@brocku.ca";
        String subject = (String) params.get("contentReport__title");
        final String userTime = (String) params.get("userTime");
        final String userTimeZone = (String) params.get("userTimeZone");
        final String serverLabel = (String) params.get("serverLabel");
        final String userRole = (String) params.get("role");
        String senderName = getUserName();
        String fromStr="";
        subject = "BCR: "+subject;

        if (problemBox.isEmpty() || problemBox == null) {
            log.error("Empty Description Box. No email will be sent. User will be notified");
            return BAD_DESCRIPTION;
        }

        if (senderName==null) senderName = "Unknown user";

        if (senderEmail == null) {
            log.error("Bad sender email. Sender email is null, No email will be sent");
            return BAD_SENDER_EMAIL;
        }else
            fromStr = senderName+"<"+senderEmail+">";

        StringBuilder content = new StringBuilder();
        content.append("Site title: "+siteTitle+"\n");
        content.append("Direct Url: "+directUrl+"\n");
        content.append("User Agent: "+userAgent+"\n");
        content.append("User name: "+senderName+"\n");
        content.append("User email: "+ senderEmail+"\n");
        content.append("User Timestamp: "+userTime+"\n");
        content.append("User Timezone: "+userTimeZone+"\n");
        content.append("User Role: "+userRole+"\n");
        content.append("Server: "+serverLabel+"\n\n");
        content.append("User problem description: \n\n");
        content.append(problemBox);

        try {
            emailService.send(fromStr, recipientEmail, subject, content.toString(), recipientEmail, null, null);
            return SUCCESS;
        } catch (Exception e) {
            log.error("Failed to send email.", e);
            return ERROR;
        }

    }
}