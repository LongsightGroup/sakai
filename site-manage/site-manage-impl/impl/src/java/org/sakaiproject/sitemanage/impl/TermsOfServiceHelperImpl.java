package org.sakaiproject.sitemanage.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.sitemanage.api.TermsOfServiceHelper;
import org.springframework.util.ResourceUtils;

public class TermsOfServiceHelperImpl implements TermsOfServiceHelper {
	private static Log M_log = LogFactory.getLog(TermsOfServiceHelperImpl.class);
	
	private String tosFileLocation;
	private String primaryUseFileLocation; 
	private ContentHostingService contentHostingService;
	
	
	public void setTosFileLocation(String tosFileLocation) {
		this.tosFileLocation = tosFileLocation;
	}
	
	public void setPrimaryUseFileLocation(String primaryUseResource) {
		this.primaryUseFileLocation = primaryUseResource;
	}
	
	public void setContentHostingService(ContentHostingService contentHostingService) {
		this.contentHostingService = contentHostingService; 
	}
	
	public List<String> getPrimaryUseList() {
		InputStream tosFile = null;
		BufferedReader br = null;
		try {
			if (primaryUseFileLocation.startsWith("contentResource:")) {
				ContentResource resource = contentHostingService.getResource(
						primaryUseFileLocation.substring("contentResource:".length()));
				br = new BufferedReader(new InputStreamReader(resource.streamContent()));
			} else {
				File resource = ResourceUtils.getFile(primaryUseFileLocation);
				br = new BufferedReader(new FileReader(resource));
			}
			List<String> primaryUseList = new ArrayList<String>();
			String line;
			while (null != (line = br.readLine())) {
				primaryUseList.add(line);
			}
			return primaryUseList;
		} catch (FileNotFoundException fe) {
			M_log.error("Primary Use file not found.", fe);
		} catch (IOException fe) {
			M_log.error("Error reading Primary Use file.", fe);
		} catch (PermissionException e) {
			M_log.error("Permission exception accessing file.", e);
		} catch (IdUnusedException e) {
			M_log.error("File resource id invalid.", e);
		} catch (TypeException e) {
			M_log.error("File resource type invalid.", e);
		} catch (ServerOverloadException e) {
			M_log.error("File resource cannot be read.", e);
		} finally {
			if (br != null) {
				try { br.close(); } catch (IOException ignore) {};
			}
			if (tosFile != null) {
				try { tosFile.close(); } catch (IOException ignore) {};
			}
		}
		return null;
	}

	public String getTosText() {
		InputStream tosFile = null;
		BufferedReader br = null;
		try {
			if (tosFileLocation.startsWith("contentResource:")) {
				ContentResource resource = contentHostingService.getResource(
						tosFileLocation.substring("contentResource:".length()));
				br = new BufferedReader(new InputStreamReader(resource.streamContent()));
			} else {
				File resource = ResourceUtils.getFile(tosFileLocation);
				br = new BufferedReader(new FileReader(resource));
			}
			StringBuffer bf = new StringBuffer();
			String line;
			while (null != (line = br.readLine())) {
				bf.append(line);
				bf.append("\n");
			}
			return bf.toString();
		} catch (FileNotFoundException fe) {
			M_log.error("Terms of Service file not found.", fe);
		} catch (IOException fe) {
			M_log.error("Error reading Terms of Service file.", fe);
		} catch (PermissionException e) {
			M_log.error("Permission exception accessing file.", e);
		} catch (IdUnusedException e) {
			M_log.error("File resource id invalid.", e);
		} catch (TypeException e) {
			M_log.error("File resource type invalid.", e);
		} catch (ServerOverloadException e) {
			M_log.error("File resource cannot be read.", e);
		} finally {
			if (br != null) {
				try { br.close(); } catch (IOException ignore) {};
			}
			if (tosFile != null) {
				try { tosFile.close(); } catch (IOException ignore) {};
			}
		}
		return null;
	}

}
