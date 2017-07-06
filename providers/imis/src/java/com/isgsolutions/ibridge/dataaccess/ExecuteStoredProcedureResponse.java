
package com.isgsolutions.ibridge.dataaccess;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
 *         &lt;element name="ExecuteStoredProcedureResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "executeStoredProcedureResult"
})
@XmlRootElement(name = "ExecuteStoredProcedureResponse")
public class ExecuteStoredProcedureResponse {

    @XmlElement(name = "ExecuteStoredProcedureResult")
    protected String executeStoredProcedureResult;

    /**
     * Gets the value of the executeStoredProcedureResult property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExecuteStoredProcedureResult() {
        return executeStoredProcedureResult;
    }

    /**
     * Sets the value of the executeStoredProcedureResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExecuteStoredProcedureResult(String value) {
        this.executeStoredProcedureResult = value;
    }

}
