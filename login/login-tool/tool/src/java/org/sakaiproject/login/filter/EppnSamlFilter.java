package org.sakaiproject.login.filter;

import org.apache.commons.lang.StringUtils;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

public class EppnSamlFilter implements SAMLUserDetailsService {
        @Override
        public Object loadUserBySAML(SAMLCredential cred) throws UsernameNotFoundException {
                // https://www.incommon.org/federation/attributesummary.html
                String eppn = cred.getAttributeAsString("urn:oid:1.3.6.1.4.1.5923.1.1.1.6");
                return StringUtils.lowerCase(StringUtils.substringBefore(eppn, "@"));
        }
}
