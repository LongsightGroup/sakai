/**
 * Copyright (c) 2003-2016 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.login.filter;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;

@Slf4j
public class EppnSamlFilter implements SAMLUserDetailsService {
    private MemoryService memoryService;
    private Cache cache;

    /**
     * Initialize the servlet.
     * 
     * @param config
     *        The servlet config.
     * @throws ServletException
     */
    public void init() {
        log.debug("init()");
        memoryService = (MemoryService) ComponentManager.get(MemoryService.class);
    }

        @Override
        public Object loadUserBySAML(SAMLCredential cred) throws UsernameNotFoundException {
                // get the cache
                if (memoryService == null) memoryService = (MemoryService) ComponentManager.get(MemoryService.class);
                if (cache == null) cache = memoryService.getCache("edu.duke.dukeRole");

                // https://www.incommon.org/federation/attributesummary.html
                String eid = cred.getAttributeAsString("urn:oid:1.3.6.1.4.1.5923.1.1.1.6");
                String dukeRole = cred.getAttributeAsString("urn:oid:1.3.6.1.4.1.5923.1.5.1.1");
                cache.put(eid, dukeRole);

                return eid;
        }
}
