/**
 * Copyright (c) 2003-2017 The Apereo Foundation
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
package org.sakaiproject.gradebookng.tool.panels.importExport;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.tool.component.GbAjaxLink;
import org.sakaiproject.gradebookng.tool.panels.BasePanel;
import org.sakaiproject.service.gradebook.shared.CourseGrade;

public class SguCustomExportPanel extends BasePanel {

	private static final long serialVersionUID = 1L;

	public SguCustomExportPanel(final String id) {
		super(id);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		add(new GbAjaxLink<Void>("downloadSGUGradebook") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(final AjaxRequestTarget target) {
				target.appendJavaScript("$('#sgu-submit-progress').show();$('.gb-import-export-section button').prop('disabled', true);");
				buildFile();
			}
		});

	}

	private File buildFile() {
		File tempFile = new File(buildFileName());

		try {
			//CSV separator is comma unless the comma is the decimal separator, then is ;
			try (OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8.name())){

				CSVWriter csvWriter = new CSVWriter(fstream, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.RFC4180_LINE_END);

				// get the final grades
				final String siteId = this.businessService.getCurrentSiteId();
				final Map<String, CourseGrade> grades = this.businessService.getCourseGrades(siteId);
				Map<String, GbUser> users = this.businessService.getUserEidMap();

				for (Map.Entry<String, CourseGrade> entry : grades.entrySet()) {
					final List<String> line = new ArrayList<>();
					final String userId = entry.getKey();
					final CourseGrade grade = entry.getValue();
					final String userEid = this.businessService.getUser(userId).getDisplayId();
					final GbUser gbUser = users.get(userEid);
					final String studentNumber = gbUser.getStudentNumber();

					line.add(siteId);
					line.add(userEid);
					line.add(studentNumber);
					line.add(FormatHelper.formatGradeForDisplay(grade.getCalculatedGrade()));
					line.add(grade.getDisplayGrade());

					csvWriter.writeNext(line.toArray(new String[] {}));
				}
				csvWriter.close();
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		return tempFile;
	}

	private String buildFileName() {
		final String basePath = this.businessService.getServerConfigService().getString("sgu.custom.gradebook.path", "/var/sftp/sgu_files/gradebook_export/");
		final String gradebookName = this.businessService.getGradebook().getUid();

		return basePath + gradebookName + ".csv";
	}
}
