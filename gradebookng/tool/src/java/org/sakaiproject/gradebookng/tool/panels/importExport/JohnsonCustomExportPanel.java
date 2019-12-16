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
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.tool.panels.BasePanel;
import org.sakaiproject.grading.api.CourseGradeTransferBean;

public class JohnsonCustomExportPanel extends BasePanel {

	private static final long serialVersionUID = 1L;
	
	private final Model<String> gradeTypeModel = Model.of("final");

	public JohnsonCustomExportPanel(final String id) {
		super(id);
	}

	@Override
	public void onInitialize() {
		super.onInitialize();

		// Create form for grade type selection
		Form<Void> gradeTypeForm = new Form<Void>("gradeTypeForm");
		
		RadioGroup<String> gradeTypeGroup = new RadioGroup<String>("gradeType", gradeTypeModel);
		
		Radio<String> midtermRadio = new Radio<>("gradeTypeMidterm", new Model<>("midterm"));
		Radio<String> finalRadio = new Radio<>("gradeTypeFinal", new Model<>("final"));
		
		gradeTypeGroup.add(midtermRadio);
		gradeTypeGroup.add(finalRadio);
		gradeTypeForm.add(gradeTypeGroup);
		
		// Use an AjaxButton instead of AjaxLink to properly handle form submission
		AjaxButton downloadButton = new AjaxButton("downloadJohnsonGradebook", gradeTypeForm) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit(final AjaxRequestTarget target) {
				target.appendJavaScript("$('#sgu-submit-progress').show();$('.gb-import-export-section button').prop('disabled', true);");
				buildFile();
			}
		};
		gradeTypeForm.add(downloadButton);
		
		add(gradeTypeForm);

	}

	private void buildFile() {
		try {
			final String gradeType = gradeTypeModel.getObject();
			final String gradeTypeIndicator = "midterm".equals(gradeType) ? "0" : "1";
			
			final File tempFile = new File(buildFileName(currentSiteId, gradeType));

			//CSV separator is comma unless the comma is the decimal separator, then is ;
			try (OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {

				final CSVWriter csvWriter = new CSVWriter(fstream, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER,
						CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

				// Add header
				csvWriter.writeNext(new String[] { "Email", "Course", "Grade", "Grade_Type" });

				final List<String> studentUuids = this.businessService.getGradeableUsers(currentGradebookUid, currentSiteId, null);
				final Map<String, String> userEids = this.businessService.getGbUsers(currentSiteId, studentUuids).stream()
						.collect(Collectors.toMap(GbUser::getUserUuid, GbUser::getDisplayId));
				final Map<String, CourseGradeTransferBean> grades = this.businessService.getCourseGrades(currentGradebookUid, currentSiteId,
						studentUuids, null);

				for (Map.Entry<String, CourseGradeTransferBean> entry : grades.entrySet()) {
					final List<String> line = new ArrayList<>();
					final String userId = entry.getKey();
					final CourseGradeTransferBean grade = entry.getValue();
					final String userEid = userEids.get(userId);
					if (userEid == null) {
						continue;
					}

					line.add(userEid);
					line.add(currentSiteId);
					line.add(grade.getDisplayGrade());
					line.add(gradeTypeIndicator);

					csvWriter.writeNext(line.toArray(new String[] {}));
				}
				csvWriter.close();
				tempFile.setReadable(true, false);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String buildFileName(final String gbName, final String gradeType) {
		final String basePath = this.businessService.getServerConfigService().getString("johnson.custom.gradebook.path", "sakai/gradebook_export/");
		File directory = new File(basePath);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		return basePath + gbName + ".csv";
	}
}
