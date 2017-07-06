package com.isgsolutions.ibridge.authentication;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java
 * element interface generated in the com.isgsolutions.ibridge.authentication
 * package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the
 * Java representation for XML content. The Java representation of XML content
 * can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory
 * methods for each of these are provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

	private final static QName _String_QNAME = new QName(
			"http://ibridge.isgsolutions.com/Authentication/", "string");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of
	 * schema derived classes for package:
	 * com.isgsolutions.ibridge.authentication
	 * 
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link AuthenticateCookie }
	 * 
	 */
	public AuthenticateCookie createAuthenticateCookie() {
		return new AuthenticateCookie();
	}

	/**
	 * Create an instance of {@link AuthenticateCookieResponse }
	 * 
	 */
	public AuthenticateCookieResponse createAuthenticateCookieResponse() {
		return new AuthenticateCookieResponse();
	}

	/**
	 * Create an instance of {@link DeleteUserSessionResponse }
	 * 
	 */
	public DeleteUserSessionResponse createDeleteUserSessionResponse() {
		return new DeleteUserSessionResponse();
	}

	/**
	 * Create an instance of {@link LoginResponse }
	 * 
	 */
	public LoginResponse createLoginResponse() {
		return new LoginResponse();
	}

	/**
	 * Create an instance of {@link AuthenticateTokenResponse }
	 * 
	 */
	public AuthenticateTokenResponse createAuthenticateTokenResponse() {
		return new AuthenticateTokenResponse();
	}

	/**
	 * Create an instance of {@link AuthenticateToken }
	 * 
	 */
	public AuthenticateToken createAuthenticateToken() {
		return new AuthenticateToken();
	}

	/**
	 * Create an instance of {@link DeleteUserSession }
	 * 
	 */
	public DeleteUserSession createDeleteUserSession() {
		return new DeleteUserSession();
	}

	/**
	 * Create an instance of {@link AuthenticateUser }
	 * 
	 */
	public AuthenticateUser createAuthenticateUser() {
		return new AuthenticateUser();
	}

	/**
	 * Create an instance of {@link AuthenticateUserResponse }
	 * 
	 */
	public AuthenticateUserResponse createAuthenticateUserResponse() {
		return new AuthenticateUserResponse();
	}

	/**
	 * Create an instance of {@link Login }
	 * 
	 */
	public Login createLogin() {
		return new Login();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
	 * 
	 */
	@XmlElementDecl(namespace = "http://ibridge.isgsolutions.com/Authentication/", name = "string")
	public JAXBElement<String> createString(String value) {
		return new JAXBElement<String>(_String_QNAME, String.class, null, value);
	}

}
