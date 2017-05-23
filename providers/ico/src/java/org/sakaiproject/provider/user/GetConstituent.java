package org.sakaiproject.provider.user;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="userName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * must have http://ico.edu/ set for userName
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "userName", "sharedKey" }, namespace="http://ico.edu/")
@XmlRootElement(name = "GetConstituent", namespace ="http://ico.edu/")
public class GetConstituent {

	protected String userName;
	protected String sharedKey;

	/**
 * 	 * Gets the value of the userName property.
 * 	 	 * 
 * 	 	 	 * @return possible object is {@link String }
 * 	 	 	 	 * 
 * 	 	 	 	 	 */
	public String getUserName() {
		return userName;
	}
	public String getSharedKey() {
		return sharedKey;
	}

	/**
 * 	 * Sets the value of the userName property.
 * 	 	 * 
 * 	 	 	 * @param value
 * 	 	 	 	 *            allowed object is {@link String }
 * 	 	 	 	 	 * 
 * 	 	 	 	 	 	 */
	public void setUserName(String value) {
		this.userName = value;
	}
	public void setSharedKey(String value) {
		this.sharedKey = value;
	}

}

