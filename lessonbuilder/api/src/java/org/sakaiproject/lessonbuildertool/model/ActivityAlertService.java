package org.sakaiproject.lessonbuildertool.model;

import org.sakaiproject.api.app.scheduler.ScheduledInvocationCommand;
import org.sakaiproject.lessonbuildertool.ActivityAlert;

public interface ActivityAlertService extends ScheduledInvocationCommand{

	public void scheduleActivityAlert(ActivityAlert alert);
	
	public void clearActivityAlert(ActivityAlert alert);
}
