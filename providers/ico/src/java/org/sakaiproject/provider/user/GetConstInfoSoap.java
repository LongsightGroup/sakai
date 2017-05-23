package org.sakaiproject.provider.user;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 *  * This class was generated by the JAX-WS RI. JAX-WS RI 2.1.3-hudson-390-
 *   * Generated source version: 2.0
 *    * 
 *     */
@WebService(name = "GetConstInfoSoap", targetNamespace = "http://ico.edu/")
public interface GetConstInfoSoap {

	/**
 * 	 * 
 * 	 	 * @param userName
 * 	 	 	 * @return returns java.lang.String
 * 	 	 	 	 */
	@WebMethod(operationName = "GetConstituent", action = "http://ico.edu/GetConstituent")
	@WebResult(name = "GetConstituentResult", targetNamespace = "http://ico.edu/")
	@RequestWrapper(localName = "GetConstituent", targetNamespace = "http://ico.edu/", className = "org.sakaiproject.provider.user.GetConstituent")
	@ResponseWrapper(localName = "GetConstituentResponse", targetNamespace = "http://ico.edu/", className = "org.sakaiproject.provider.user.GetConstituentResponse")
	public String getConstituent(
			@WebParam(name = "userName", targetNamespace = "http://ico.edu/") String userName,
			@WebParam(name = "sharedKey", targetNamespace = "http://ico.edu/") String sharedKey
			);
			
}

