
package com.isgsolutions.ibridge.dataaccess;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="securityPassword" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="sqlStatement" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "securityPassword",
    "sqlStatement"
})
@XmlRootElement(name = "ExecuteSQLStatement")
public class ExecuteSQLStatement {

    protected String securityPassword;
    protected String sqlStatement;

    /**
     * Gets the value of the securityPassword property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecurityPassword() {
        return securityPassword;
    }

    /**
     * Sets the value of the securityPassword property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecurityPassword(String value) {
        this.securityPassword = value;
    }

    /**
     * Gets the value of the sqlStatement property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSqlStatement() {
        return sqlStatement;
    }

    /**
     * Sets the value of the sqlStatement property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSqlStatement(String value) {
        this.sqlStatement = value;
    }

}
