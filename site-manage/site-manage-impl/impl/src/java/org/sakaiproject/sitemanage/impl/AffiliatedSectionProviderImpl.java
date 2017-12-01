/**********************************************************************************
 * $URL:  $
 * $Id:  $
 ***********************************************************************************
 *
 * Copyright (c) 2007, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.sitemanage.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService.SortType;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.sitemanage.api.AffiliatedSectionProvider;

/**
 * @author zqian
 *
 */
@Slf4j
public class AffiliatedSectionProviderImpl implements AffiliatedSectionProvider {

	public List getAffiliatedSectionEids(String userEid, String academicSessionEid)
	{
                List<Site> dukeSites = SiteService.getSites(org.sakaiproject.site.api.SiteService.SelectionType.UPDATE, null, null, null, SortType.TITLE_ASC, null);
                if (dukeSites.size() > 0) {
                        List<String> l = new ArrayList<String>();

                        for (Site s : dukeSites) {
                                l.add (s.getId());
                        }

                        return l;
                }
                else {
                        return null;
                }
	}
	
	public void init() {
	}

	public void destroy() {
	}
}
