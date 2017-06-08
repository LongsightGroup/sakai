package org.sakaiproject.webservices;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.user.api.User;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC, use = SOAPBinding.Use.LITERAL)
public class SALM_ESB_Receiver extends AbstractWebService {

    private static final Logger LOG = LoggerFactory.getLogger(SALM_ESB_Receiver.class);
    private static final Pattern ExtractSiteCode = Pattern.compile("^(.*)/portal/site/~{0,1}(\\p{XDigit}{8,8}-\\p{XDigit}{4,4}-\\p{XDigit}{4,4}-\\p{XDigit}{4,4}-\\p{XDigit}{12,12})(.*)$");
    private SqlService sqlService;

    /**
  	 * Login with the supplied credentials and  call a <i>protected</i> method to join DECUsername to sakaiSiteURL if not already joined and set inactive.
  	 * <b>Uses <i>protected</i> methods in this class.</b> <br /><b>Reproduced from SakaiScript.jws and SakaiLogin.jws</b>
  	 * @param loginId		Sakai Username used to process the request,
  	 * @param loginPw		Sakai Password used to process the request
  	 * @param DECUsername	DEC Username to be joined to a Site in Sakai
  	 * @param sakaiSiteURL	Site URL in Sakai for DEC Username to be joined to (this is currently a String)
  	 * @param action		enrol or suspend, which marks student Active or Inactive in the Site Membership in Sakai
  	 * @param groupName		Name of group to add the DEC Username as a member
  	 * @param trackingId 	UUID to audit/track message
  	 * @return				"ok" if successful
  	 **/
    @WebMethod
    @Path("/Process_SALM_ESB_Request")
    @Produces("text/plain")
    @GET
    public String Process_SALM_ESB_Request(
            @WebParam(name = "loginId", partName = "loginId") @QueryParam("loginId") String loginId,
            @WebParam(name = "loginPw", partName = "loginPw") @QueryParam("loginPw") String loginPw,
            @WebParam(name = "DECUsername", partName = "DECUsername") @QueryParam("DECUsername") String DECUsername,
            @WebParam(name = "sakaiSiteURL", partName = "sakaiSiteURL") @QueryParam("sakaiSiteURL") String sakaiSiteURL,
            @WebParam(name = "action", partName = "action") @QueryParam("action") String action,
            @WebParam(name = "groupName", partName = "groupName") @QueryParam("groupName") String groupName,
            @WebParam(name = "trackingId", partName = "trackingId") @QueryParam("trackingId") String trackingId) {

              Connection conn = null;
          		String userid=null;
          		Statement stmt = null;
          		ResultSet rs = null;
          		String sErrorMessage = null;
          		String baction;
          		Session s = null;

          		// Perform basic check that SOAP request received is complete
          		String sResult=this.verifySALM_ESB_Request(loginId, loginPw, DECUsername, sakaiSiteURL, action, groupName, trackingId);
          		if (sResult != null) {
          			sErrorMessage = "[Process_SALM_ESB_Request] " + sResult;
          		}

          		// Logins into Sakai
          		if (sErrorMessage == null) {
          			String sId = login(loginId, loginPw);

          			Pattern RegExGUID = Pattern.compile("^(\\p{XDigit}{8,8}-\\p{XDigit}{4,4}-\\p{XDigit}{4,4}-\\p{XDigit}{4,4}-\\p{XDigit}{12,12})$");

          			Matcher m = RegExGUID.matcher(sId);
          			if (!m.find()) {
          				sErrorMessage= "[Process_SALM_ESB_Request] login returned " + sId;
          			}

          			if (action.equals("enrol")) {
          				baction = "1";
          			} else {
          				baction = "0";
          			}

          			s = sessionManager.getSession(sId);
          		}

          		String sStatus = (sErrorMessage == null) ? "Not Processed" : "Error";
          		String sStatusDescription = sErrorMessage;

          		String query = "INSERT INTO ebssoapmessages (trackingId, DECUsername, sakaiSiteURL, action, groupName, status, statusDescription, dateReceived) "
          			+ " VALUES ( ?, ?, ?, '" + action + "', ?, '" + sStatus + "', ?, NOW() ) ";

          		try {
          			conn = sqlService.borrowConnection();

          			PreparedStatement preparedStmt = conn.prepareStatement(query);

          			preparedStmt.setString (1, trackingId);
          			preparedStmt.setString (2, DECUsername);
          			preparedStmt.setString (3, sakaiSiteURL);
          			preparedStmt.setString (4, groupName);
          			preparedStmt.setString (5, sStatusDescription);

          			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

          			preparedStmt.execute();
                conn.commit();

          			if (sErrorMessage != null) {
          				// AxisFault af = new AxisFault("trackingId [" + trackingId + "] " + sErrorMessage);
          				// LOG.error("Description=" + sErrorMessage+" for message with trackingId of " + trackingId);
          				// af.setFaultActor(faultActor);
          				// throw af;
          				return "ok";
          			}
          		} catch (Exception e) {
          			LOG.error ( "Error inserting message into ebssoapmessages: trackingId=" + trackingId + ", DECUsername=" + DECUsername + ",sakaiSiteURL=" + sakaiSiteURL + ",action=" + action + ",groupName=" + groupName);
          			LOG.error ( e.getClass().getName() + " : " + e.getMessage());
          			return "ok";
          		} finally {
          			if (rs != null) {
          					try {
          						rs.close();
          					} catch (SQLException e) {}
          			}

          			if (stmt != null) {
          					try {
          						stmt.close();
          					} catch (SQLException e) {}
          			}

                sqlService.returnConnection(conn);
          		}
          		return "ok";

    }

    /**
     *	Checks that all properties in the SOAP request are not empty
     *
     * @author Shane Norton
     * @param loginId		Sakai Username used to process the request,
     * @param loginPw		Sakai Password used to process the request
     * @param DECUsername	DEC Username to be joined to a Site in Sakai
     * @param sakaiSiteURL	Site URL in Sakai for DEC Username to be joined to (this is currently a String)
     * @param action		    enrol or suspend, which triggers whether to mark the student Inactive in the Site Membership in Sakai
     *
     * @return null if ok or error message if there is something invalid found.
     **/
    protected String verifySALM_ESB_Request(String loginId, String loginPw, String DECUsername, String sakaiSiteURL, String action, String groupName, String trackingId) {
      //Check that any parameters are not null
      // loginId, loginPw, DECUsername, sakaiSiteURL, action, groupName, trackingId
      if ((loginId == null) || (loginPw == null) || (DECUsername == null) || (sakaiSiteURL == null) || (action == null)) {
        return "Message is missing one or more mandatory fields";
      }
      if ((loginId.trim().equals("")) || (loginPw.trim().equals("")) || (DECUsername.trim().equals("")) || (sakaiSiteURL.trim().equals("")) || (action.trim().equals(""))) {
        return "Message is missing one or more mandatory fields";
      }
      // Verify action parameter is 'enrol' or 'suspend'
      String myaction = action.toLowerCase(); // allows upper case variants.
      if (!((myaction.equals("enrol")) || (myaction.equals("suspend")))) {
        return "Invalid action";
      }

      // Verify sakaiSiteURL is in a format where we can extract the site_id
      if (!(siteExists(sakaiSiteURL))) {
        return "Invalid SakaiSiteURL";
      }

      if (!(isValidSiteType(sakaiSiteURL, new String[] {"course","project"}))) {
        return "Invalid Site Type";
      }

      return null;
    }

    protected boolean isValidSiteType(String siteURL, String[] validSiteTypes) {
      //Extract Site Code using Regex
      Matcher m = ExtractSiteCode.matcher(siteURL);
      if (m.find()) { // only if it matches
        String siteId = m.group(2).toString();
                          // LOG.error("Description=SiteId is" + siteId);
        try {
          Site site = siteService.getSite(siteId); // check if siteId Exists
          String siteType=site.getType();
          // Loop through validSiteTypes if there is a match return true
          for(int i =0; i < validSiteTypes.length; i++) {
            if ((siteType).equals(validSiteTypes[i])) {
              return true; //Valid Site Type
            }
          }
        } catch (Exception e) {	}	//no specific action for exceptions
      }

      return false; //Invalid Site Type
    }

    /**
     *
     *	Verifies the absolute URL matches a regular expression "^(.*)/portal/site/(.+)$"
     *  WARNING: Doesn't follow the unused short/alias feature of Sakai
     *
     *	@param 	URL 	The URL address (String) to extract the site_id from
     *	@author Shane Norton
     *  @return true if siteURL exists, or false if it does not
     *
     **/
    protected Boolean siteExists(String siteURL) {
      //Extract Site Code using Regex
      Matcher m = ExtractSiteCode.matcher(siteURL);
      if (m.find()) { // only if it matches
        String siteId = m.group(2).toString();
        try {
          Site site = siteService.getSite(siteId); // check if siteId Exists
          if (site != null) {
            return true;
          }
        } catch (Exception e) {	}

      }

      return false; //failed to match regex
    }

    /*	protected static boolean IsGUID(String expression) {
      Pattern RegExGUID = Pattern.compile("^(\\p{XDigit}{8,8}-\\p{XDigit}{4,4}-\\p{XDigit}{4,4}-\\p{XDigit}{4,4}-\\p{XDigit}{12,12})$");

      Matcher m = RegExGUID.matcher(expression);
      if (expression != null) {
        return m.find();
      }
      return false;
    }
    */

      /**
       * Login with the supplied credentials and return the session string which can be used in subsequent web service calls, ie via SakaiScript
       * @param id	eid, eg jsmith26
       * @param pw	password for the user
       * @return		session string or
     * "WS Disabled" or "WS UserNotDefinedException" or "WS SessionFailedToBeCreated" or "WS Access Denied" or "WS Authentication failed"
       */
      protected String login(String id,String pw)  {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        String ipAddress = request.getRemoteAddr();

        boolean allowLogin = serverConfigurationService.getBoolean("webservices.allowlogin", false);

        if (!allowLogin) {
            throw new RuntimeException("Web Services Login Disabled");
        }

        User user = userDirectoryService.authenticate(id, pw);
        if (user != null) {
            Session s = sessionManager.startSession();
            sessionManager.setCurrentSession(s);
            if (s == null) {
                LOG.warn("Web Services Login failed to establish session for id=" + id + " ip=" + ipAddress);
                throw new RuntimeException("Unable to establish session");
            } else {

                // We do not care too much on the off-chance that this fails - folks simply won't show up in presense
                // and events won't be trackable back to people / IP Addresses - but if it fails - there is nothing
                // we can do anyways.

                usageSessionService.login(user.getId(), id, ipAddress, "SakaiLogin.jws", UsageSessionService.EVENT_LOGIN_WS);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Sakai Web Services Login id=" + id + " ip=" + ipAddress + " session=" + s.getId());
                }
                return s.getId();
            }
        }
        LOG.warn("Failed Web Services Login id=" + id + " ip=" + ipAddress);
        throw new RuntimeException("Unable to login");
    }

    @WebMethod(exclude = true)
    public void setSqlService(SqlService sqlService) {
        this.sqlService = sqlService;
    }
}
