Revision XXXXXX contains the Terms Of Service Functionality.

When the user tries to create a new project site, he has to accept
the "Terms of Service" and select the 'Primary Intended Use" of the site.
The 'Primary Intended Use" of the site can be changed later on Site Info page.

By default data will be loaded from the files TermsOfService.txt and PrimaryUse.txt in Recourses of the Administration root folder (/user/admin). 

The location of these file can be overwritten in local.properties or sakai.properties with the default Sakai property overwrite of the following attributes:

tosFileLocation@org.sakaiproject.sitemanage.api.TermsOfServiceHelper=<new location>
primaryUseFileLocation@org.sakaiproject.sitemanage.api.TermsOfServiceHelper=<new location>

The <new location> can be a default Spring resource URI or start with "contentResource:" to specify a location
within the Resources of Sakai.




 