package com.isgsolutions.ibridge.authentication;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="AuthenticateTokenResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "authenticateTokenResult" })
@XmlRootElement(name = "AuthenticateTokenResponse")
public class AuthenticateTokenResponse {

	@XmlElement(name = "AuthenticateTokenResult")
	protected String authenticateTokenResult;

	/**
	 * Gets the value of the authenticateTokenResult property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getAuthenticateTokenResult() {
		return authenticateTokenResult;
	}

	/**
	 * Sets the value of the authenticateTokenResult property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setAuthenticateTokenResult(String value) {
		this.authenticateTokenResult = value;
	}

}
