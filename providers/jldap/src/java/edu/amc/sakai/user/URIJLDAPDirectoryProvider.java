package edu.amc.sakai.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.user.api.AuthenticatedUserProvider;
import org.sakaiproject.user.api.UserEdit;
import org.sakaiproject.user.api.UserFactory;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;


public class URIJLDAPDirectoryProvider extends JLDAPDirectoryProvider implements
        AuthenticatedUserProvider {

    /** Class-specific logger */
    private static Log M_log = LogFactory.getLog(URIJLDAPDirectoryProvider.class);
    
    private UserFactory sakaiUserFactory;

    // default constructor
    public URIJLDAPDirectoryProvider() {
        if ( M_log.isDebugEnabled() ) {
            M_log.debug("instantating URIJLDAPDirectoryProvider");
        }
    }
    
    public UserEdit getAuthenticatedUser(String loginId, String password)
    {
        if ( M_log.isDebugEnabled() ) {
            M_log.debug("getAuthenticatedUser(): [loginId = " + loginId + "]");
        }
        
        boolean isPassword = (password != null) && (password.trim().length() > 0);
        if ( !(isPassword) )
        {
            if ( M_log.isDebugEnabled() ) {
                M_log.debug("getAuthenticatedUser(): returning false, blank password");
            }
            return null;
        }

        UserEdit userResult = sakaiUserFactory.newUser(); 
        LDAPConnection conn = null;

        try
        {
            // conn is implicitly bound as manager, if necessary
            if ( M_log.isDebugEnabled() ) {
                M_log.debug("getAuthenticatedUser(): allocating connection for login [loginId = " + loginId + "]");
            }

            conn = ldapConnectionManager.getConnection();

            // look up the end-user's DN, which could be nested at some 
            // arbitrary depth below getBasePath().
            String endUserDN = uriLookupUserBindDn(loginId, conn);

            if ( endUserDN == null ) {
                if ( M_log.isDebugEnabled() ) {
                    M_log.debug("getAuthenticatedUser(): failed to find bind dn for login [loginId = " + loginId + "], returning null");
                }
                return null;
            } else if ( M_log.isDebugEnabled() ) {
                M_log.debug("getAuthenticatedUser(): endUserDN = " + endUserDN + "]");
            }

            if ( M_log.isDebugEnabled() ) {
                M_log.debug("getAuthenticatedUser(): returning connection to pool [loginId = " + loginId + "]");
            }
            ldapConnectionManager.returnConnection(conn);
            conn = null;
            if ( M_log.isDebugEnabled() ) {
                M_log.debug("getAuthenticatedUser(): attempting to allocate bound connection [loginId = " + 
                        loginId + "][bind dn [" + endUserDN + "]");
            }
            conn = ldapConnectionManager.getBoundConnection(endUserDN, password);
           
            if ( M_log.isDebugEnabled() ) {
                M_log.debug("getAuthenticatedUser(): successfully allocated bound connection [loginId = " + 
                        loginId + "][bind dn [" + endUserDN + "]");
            }


            conn = ldapConnectionManager.getConnection();
            String filter = getFindUserByOpridFilter(loginId);
            LdapUserData foundUserData = (LdapUserData)searchDirectoryForSingleEntry(filter, 
                                            conn, null, null, null);

            // if the oprid search has failed, fall back to trying the emplid search using the JLDAP provider methods
            if (foundUserData == null) {
                foundUserData = getUserByEid(loginId, conn);
            }
                
            if ( M_log.isDebugEnabled() ) {
                if (foundUserData == null) {
                    M_log.debug("getAuthenticatedUser(): foundUserData == null");
                }
                    else {
                        M_log.debug("getAuthenticatedUser(): User data for login " + loginId + ":");
                        M_log.debug("getAuthenticatedUser(): Eid = " + foundUserData.getEid());
                        M_log.debug("getAuthenticatedUser(): First Name = " + foundUserData.getFirstName());
                        M_log.debug("getAuthenticatedUser(): Last Name = " + foundUserData.getLastName());
                        M_log.debug("getAuthenticatedUser(): Email = " + foundUserData.getEmail());
                        M_log.debug("getAuthenticatedUser(): Type = " + foundUserData.getType());
                    }
            }
 
            // assign the appropriate values to the UserEdit object to be returned
            if (foundUserData != null) {
                userResult.setEid(foundUserData.getEid());
                userResult.setFirstName(foundUserData.getFirstName());
                userResult.setLastName(foundUserData.getLastName());
                userResult.setEmail(foundUserData.getEmail());
                userResult.setType(foundUserData.getType());
            } else {
                return null;
            }
        }
        catch (LDAPException e)
        {
            if (e.getResultCode() == LDAPException.INVALID_CREDENTIALS) {
                if ( M_log.isWarnEnabled() ) {
                    M_log.warn("getAuthenticatedUser(): invalid credentials [loginId = "
                            + loginId + "]");
                }
                return null;
            } else {
                throw new RuntimeException(
                        "getAuthenticatedUser(): LDAPException during authentication attempt [loginId = "
                        + loginId + "][result code = " + e.resultCodeToString() + 
                        "][error message = "+ e.getLDAPErrorMessage() + "]", e);
            }
        } catch ( Exception e ) {
            throw new RuntimeException(
                    "getAuthenticatedUser(): Exception during authentication attempt [loginId = "
                    + loginId + "]", e);
        } finally {
            if ( conn != null ) {
                if ( M_log.isDebugEnabled() ) {
                    M_log.debug("getAuthenticatedUser(): returning connection to connection manager");
                }
                ldapConnectionManager.returnConnection(conn);
            }
        }

        //return null;
        return userResult;
    }

    /**
     * Search the directory for a DN corresponding to a user's
     * oprid. Typically, this is the same as DN of the object
     * from which the user's attributes are retrieved, but
     * that need not necessarily be the case.
     * 
     * @see #getUserByEid(String, LDAPConnection)
     * @see LdapAttributeMapper#getUserBindDn(LdapUserData)
     * @param loginId the user's login ID
     * @param conn an optional {@link LDAPConnection}
     * @return the user's bindable DN or null if no matching directory entry
     * @throws LDAPException if the directory query exits with an error
     */
    protected String uriLookupUserBindDn(String loginId, LDAPConnection conn) 
    throws LDAPException {

        LdapUserData foundUserData;
        
        if ( M_log.isDebugEnabled() ) {
            M_log.debug("uriLookupUserBindDn(): [loginId = " + loginId + 
                    "][reusing conn = " + (conn != null) + "]");
        }
        
        foundUserData = getUserByOprid(loginId, conn);

        if ( foundUserData == null ) {
            if ( M_log.isDebugEnabled() ) {
                M_log.debug("uriLookupUserBindDn(): no directory entry found [loginId = " + 
                        loginId + "]");
            }
            return null;
        }
        return ldapAttributeMapper.getUserBindDn(foundUserData);

    }
   
    /**
     * Finds a user record using an <code>eid</code> as an index.
     * 
     * @param oprid the PeopleSoft oprid to search on
     * @param conn an optional {@link LDAPConnection}
     * @return object representing the found LDAP entry, or null if no results
     * @throws LDAPException if the search returns with a directory access error
     */
    protected LdapUserData getUserByOprid(String oprid, LDAPConnection conn) 
    throws LDAPException {
        if ( M_log.isDebugEnabled() ) {
            M_log.debug("getUserByOprid(): [oprid = " + oprid + "]");
        }

        LdapUserData cachedUserData = getCachedUserEntry(oprid);
        boolean foundCachedUserData = cachedUserData != null;

        if ( foundCachedUserData ) {
            if ( M_log.isDebugEnabled() ) {
                M_log.debug("getUserByOprid(): found cached user [oprid = " + oprid + "]");
            }
            return cachedUserData;
        }

        String filter = getFindUserByOpridFilter(oprid);
        if ( M_log.isDebugEnabled() ) {
            M_log.debug("getUserByOprid(): [filter = " + filter + "]");
        }
       

        // takes care of caching and everything
        return (LdapUserData)searchDirectoryForSingleEntry(filter, 
                conn, null, null, null);

    }

    /**
     * Called by Spring Framework based on jldap-beans.xml
     * 
     * @param sakaiUserFactory
     */
    public void setSakaiUserFactory(UserFactory sakaiUserFactory) {
        this.sakaiUserFactory = sakaiUserFactory;
    }

    /**
     * Builds a filter of the form &lt;login-attr&gt;=&lt;<code>oprid</code>&gt;
     */
    public String getFindUserByOpridFilter(String oprid) {
        String opridAttr =
            AttributeMappingConstants.URIOPRID_ATTR_MAPPING_KEY;
        return opridAttr + "=" + ldapAttributeMapper.escapeSearchFilterTerm(oprid);
    }

}
