package edu.amc.sakai.user;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sakaiproject.user.api.UserEdit;
import org.sakaiproject.user.api.UserFactory;

/**
 * JLDAPDirectoryProvider that only does authentication.
 * 
 * @author Earle Nietzel (earle.nietzel@gmail.com)
 *
 */
public class JLDAPAuthOnlyDirectoryProvider extends JLDAPDirectoryProvider {

	public JLDAPAuthOnlyDirectoryProvider() {
		super();
	}

	@Override
	public Collection<UserEdit> findUsersByEmail(String email, UserFactory factory) {
		return Collections.emptyList();
	}

	@Override
	public List<UserEdit> searchExternalUsers(String criteria, int first, int last, UserFactory factory) {
		return Collections.emptyList();
	}

	@Override
	public boolean authenticateUser(String eid, UserEdit edit, String password) {
		return super.authenticateUser(eid, edit, password);
	}

	@Override
	public boolean authenticateWithProviderFirst(String eid) {
		return false;
	}

	@Override
	public boolean findUserByEmail(UserEdit edit, String email) {
		return false;
	}

	@Override
	public boolean getUser(UserEdit edit) {
		return false;
	}

	@Override
	public void getUsers(Collection<UserEdit> users) {
	}

}
