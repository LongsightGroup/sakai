/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.provider.user;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.user.api.DisplayAdvisorUDP;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryProvider;
import org.sakaiproject.user.api.UserEdit;
import org.sakaiproject.user.api.UserFactory;
import org.sakaiproject.user.api.UsersShareEmailUDP;

import org.sakaiproject.provider.user.GetConstInfo;
import org.sakaiproject.provider.user.GetConstInfoSoap;



/**
 * <p>
 * SampleUserDirectoryProvider is a samaple UserDirectoryProvider.
 * </p>
 */
public class ICOUserDirectoryProvider implements UserDirectoryProvider, UsersShareEmailUDP, DisplayAdvisorUDP
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(ICOUserDirectoryProvider.class);

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies and their setter methods
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected String sharedSecret = "password";
    protected String wsdlUrl = "http://localhost/";
    protected String nsUrl = "http://localhost/";

    public void setSharedSecret(String password) {
      sharedSecret = password;
    }
    public void setWsdlUrl(String url) {
      wsdlUrl = url;
    }
    public void setNsUrl(String url) {
      nsUrl = url;
    }
    

	public void init()
	{
		M_log.info("Initing the ICO Blackbaud OCC User Directory Provider!");
	}
	
	/**
	 * Returns to uninitialized state. You can use this method to release resources thet your Service allocated when Turbine shuts down.
	 */
	public void destroy()
	{

		M_log.info("destroy()");

	} // destroy

	/**********************************************************************************************************************************************************************************************************************************************************
	 * UserDirectoryProvider implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Construct.
	 */
	public ICOUserDirectoryProvider()
	{
	}

	/**
	 * See if a user by this id exists.
	 * 
	 * @param userId
	 *        The user id string.
	 * @return true if a user by this id exists, false if not.
	 */
	protected boolean userExists(String userId)
	{
		M_log.debug("userExists is being called! "+userId);
		if (userId == null) return false;

        String results = null;
        try {
            GetConstInfo service = new GetConstInfo();
            GetConstInfoSoap soap = service.getGetConstInfoSoap();
            if (soap == null) {
                M_log.debug("Soap web service is null!");
            }
            results = soap.getConstituent(userId, sharedSecret);
        } catch (Exception e) {
            System.out.println(e.getClass().getName() + " : " + e.getMessage());
            return false;
        }

        if (results == null || results == "") {
            return false;
        }

		return true;

	} // userExists

	/**
	 * Access a user object. Update the object with the information found.
	 * 
	 * @param edit
	 *        The user object (id is set) to fill in.
	 * @return true if the user object was found and information updated, false if not.
	 */
	public boolean getUser(UserEdit edit)
	{
		M_log.debug("getUser is being called! "+edit.getEid());
		if (edit == null) return false;
		if (!userExists(edit.getEid())) return false;
		
		String results = null;
		try {
            GetConstInfo service = new GetConstInfo();
            GetConstInfoSoap soap = service.getGetConstInfoSoap();
            if (soap == null) {
                M_log.debug("Soap web service is null!");
            }
            
            results = soap.getConstituent(edit.getEid(), sharedSecret);

		} catch (Exception e) {
		    System.out.println(e.getClass().getName() + " : " + e.getMessage());
		    return false;
		}
		
		if (results == null || results == "") {
		    edit.setFirstName(edit.getEid());
		    edit.setLastName(edit.getEid());
			edit.setEmail(edit.getEid());
			edit.setPassword(edit.getEid());
			edit.setType("registered");
		} else {
		    M_log.debug(results);
		    String[] resultsArray = results.split(",");
		    edit.setFirstName(resultsArray[1]);
		    edit.setLastName(resultsArray[2]);
		    edit.setEmail(resultsArray[3]);
		    edit.setPassword("c6Swuchadadr23e"); //TODO - does this have to be set correctly? Does it matter since authenticateUser will be consulted anyway?
		    edit.setType("student");
		}

		return true;

	} // getUser

	/**
	 * Access a collection of UserEdit objects; if the user is found, update the information, otherwise remove the UserEdit object from the collection.
	 * 
	 * @param users
	 *        The UserEdit objects (with id set) to fill in or remove.
	 */
	public void getUsers(Collection users)
	{
		for (Iterator i = users.iterator(); i.hasNext();)
		{
			UserEdit user = (UserEdit) i.next();
			if (!getUser(user))
			{
				i.remove();
			}
		}
	}

	/**
	 * Find a user object who has this email address. Update the object with the information found. <br />
	 * Note: this method won't be used, because we are a UsersShareEmailUPD.<br />
	 * This is the sort of method to provide if your external source has only a single user for any email address.
	 * 
	 * @param email
	 *        The email address string.
	 * @return true if the user object was found and information updated, false if not.
	 */
	public boolean findUserByEmail(UserEdit edit, String email)
	{
		if ((edit == null) || (email == null)) return false;

		// assume a "@local.host"
		int pos = email.indexOf("@local.host");
		if (pos != -1)
		{
			String id = email.substring(0, pos);
			edit.setEid(id);
			return getUser(edit);
		}

		return false;

	} // findUserByEmail

	/**
	 * Find all user objects which have this email address.
	 * 
	 * @param email
	 *        The email address string.
	 * @param factory
	 *        Use this factory's newUser() method to create all the UserEdit objects you populate and return in the return collection.
	 * @return Collection (UserEdit) of user objects that have this email address, or an empty Collection if there are none.
	 */
	public Collection findUsersByEmail(String email, UserFactory factory)
	{
		Collection rv = new Vector();

		// get a UserEdit to populate
		UserEdit edit = factory.newUser();

		// assume a "@local.host"
		int pos = email.indexOf("@local.host");
		if (pos != -1)
		{
			String id = email.substring(0, pos);
			edit.setEid(id);
			if (getUser(edit)) rv.add(edit);
		}

		return rv;
	}

	/**
	 * Authenticate a user / password. If the user edit exists it may be modified, and will be stored if...
	 * 
	 * @param id
	 *        The user id.
	 * @param edit
	 *        The UserEdit matching the id to be authenticated (and updated) if we have one.
	 * @param password
	 *        The password.
	 * @return true if authenticated, false if not.
	 */
	public boolean authenticateUser(String userId, UserEdit edit, String password)
	{
		if ((userId == null) || (password == null)) return false;
		
		// students will not authenticate via this interface
		
		return false;

	} // authenticateUser

	/**
	 * {@inheritDoc}
	 */
	public boolean authenticateWithProviderFirst(String id)
	{
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean createUserRecord(String id)
	{
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDisplayId(User user)
	{
		return user.getEid();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDisplayName(User user)
	{
		// punt
		return null;
	}
}

