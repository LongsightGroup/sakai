package org.sakaiproject.tool.gradebook.ui;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.section.api.coursemanagement.CourseSection;
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.gradebook.CourseGrade;
import org.sakaiproject.tool.gradebook.CourseGradeRecord;
import org.sakaiproject.tool.gradebook.GradeMapping;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.tool.gradebook.ui.duke.CsvMappingFileHelper;
import org.sakaiproject.tool.gradebook.ui.helpers.beans.GradeMapDisplayRow;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Backing bean for the SISS Grade Export Wizard
 */
public class SissGradeExportBean extends GradebookDependentBean implements Serializable {
    private static final Log logger = LogFactory.getLog(SissGradeExportBean.class);

	private static final long serialVersionUID = 1L;
	private static final String MID_TERM_GRADE = "MID";
	private static final String GRADED_CODE = "GRD";
	private static final String CSV_EXTENSION = ".csv";
	private static final String CSV_EXPORT_HEADER[] = new String[]{	"Term", "Class Nbr", "Roster Type", "Student ID", "Duke Unique", "Grade", "Name"};
	private static final int CSV_EXPORT_TERM_COLUMN = 0;
	private static final int CSV_EXPORT_CLASS_NBR_COLUMN = 1;
	private static final int CSV_EXPORT_ROSTER_TYPE_COLUMN = 2;
	private static final int CSV_EXPORT_STUDENT_ID_COLUMN = 3;
	private static final int CSV_EXPORT_DUKE_ID_COLUMN = 4;
	private static final int CSV_EXPORT_GRADE_COLUMN = 5;
	private static final int CSV_EXPORT_NAME_COLUMN = 6;
	// Cache a copy of the gradebook object in the request thread, to keep track
	// of all grade mapping changes
	private Gradebook localGradebook;
	private boolean workInProgress;

	private String selectedGradeType;
	private Long selectedGradeMappingId;
	private GradeMapping gradeMapping;
	
	// section and enrollment information
	private List<SectionEnrollmentsRow> sectionEnrollmentList;
	private List<CourseSection> viewableSections;

	/** The list of select box items */
	private List<SelectItem> gradeMappingsSelectItems;

	// View into row-specific data.
	private List<GradeMapDisplayRow> gradeRows;
	
	/**
	 * This is a inner class that works as a value holder for each section.
	 * It contains the already extracted information from the CSV mapping.
	 */
	public class SectionEnrollmentsRow implements Serializable {
		private static final long serialVersionUID = -183249324070402165L;

		private CourseSection section;
		private String courseNumber;
		private List<ScoreRow> scoreRows;
		private boolean graded;
		private String termId;
		
		public SectionEnrollmentsRow() {
		};
		
		public SectionEnrollmentsRow(CourseSection section,List<ScoreRow> scoreRows, 
		  boolean graded, String termId, String courseNumber) {
			this.section = section;
			this.scoreRows = scoreRows;
			this.graded = graded;
			this.termId = termId;
			this.courseNumber = courseNumber;
		};
		
		public CourseSection getSection() {
			return section;
		}
		
		public List<ScoreRow> getScoreRows() {
			return scoreRows;
		}
		
		public String getCourseNumber() {
			return courseNumber;
		}
		
		public String getTermId() {
			return termId;
		}
		
		public boolean isGraded() {
			return graded;
		}
		
	  public void downloadSection() {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpServletResponse response = ( HttpServletResponse ) context.getExternalContext().getResponse();
      response.setContentType("application/x-download");     
      response.setHeader("Content-Disposition", "attachment;filename=\"" +  this.getSection().getTitle() + CSV_EXTENSION + "\"");

      try {
        CSVWriter writer = new CSVWriter(response.getWriter(), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER,
                                  CSVWriter.NO_ESCAPE_CHARACTER);
        
        String termId = this.getTermId();
        String courseNumber = this.getCourseNumber();
        String gradeType = getSelectedGradeType();
        writer.writeNext(CSV_EXPORT_HEADER);
        String[] line = new String[CSV_EXPORT_HEADER.length];
        for (ScoreRow scoreRow: this.scoreRows) {
          line[CSV_EXPORT_TERM_COLUMN] = termId;
          line[CSV_EXPORT_CLASS_NBR_COLUMN] = courseNumber;
          line[CSV_EXPORT_ROSTER_TYPE_COLUMN] = gradeType;
          line[CSV_EXPORT_STUDENT_ID_COLUMN] = "";
          line[CSV_EXPORT_DUKE_ID_COLUMN] = scoreRow.getDukeUserId();
          line[CSV_EXPORT_GRADE_COLUMN] = scoreRow.getCourseGradeRecord().getDisplayGrade();
          line[CSV_EXPORT_NAME_COLUMN] = "\"" +scoreRow.getDukeUserName()+"\"";
          writer.writeNext(line);
        }
        writer.flush();
        writer.close();
      } catch (IOException ioe) {
        logger.error("Failed to write export file ", ioe);
      }
      context.responseComplete();
    }
	}
	
	/**
	 * This is a inner class that works as a value holder for the students grade record.
	 * It contains the already extracted information from the CSV mapping.
	 */
	public class ScoreRow implements Serializable {
		private static final long serialVersionUID = 3948864836632176712L;
		
		private EnrollmentRecord enrollment;
        private String dukeUserId;
        private String dukeUserFirstName;
        private String dukeUserLastName;
        private CourseGradeRecord courseGradeRecord;

		public ScoreRow() {
		}

		public ScoreRow(EnrollmentRecord enrollment, CourseGradeRecord courseGradeRecord, 
		        String dukeUserId, String dukeUserFirstName, String dukeUserLastName) 
		{
            this.enrollment = enrollment;
			this.courseGradeRecord = courseGradeRecord;
			this.dukeUserId = dukeUserId;
			this.dukeUserFirstName = dukeUserFirstName;
			this.dukeUserLastName = dukeUserLastName;
		}
		
		public EnrollmentRecord getEnrollment() {
			return enrollment;
		}
		
		public CourseGradeRecord getCourseGradeRecord() {
			return courseGradeRecord;
		}

		public String getLetterGrade() {
			Double grade = courseGradeRecord.getAutoCalculatedGrade();
	    	String letterGrade = null;
	    	if (grade != null)
	    		letterGrade = gradeMapping.getMappedGrade(courseGradeRecord.getNonNullAutoCalculatedGrade());
	    	return letterGrade;
		}
		
		public String getDukeUserId() {
			return dukeUserId;
		}

		public String getDukeUserName() {
			return dukeUserLastName + ", " + dukeUserFirstName;
		}
	}
	
    @SuppressWarnings("unchecked")
    public List getViewableSections() {
	  if (viewableSections == null) {
	    viewableSections = getGradebookBean().getAuthzService().getViewableSections(getGradebookUid());
	  }	        
		return viewableSections;
	}

	public void setSelectedGradeType(String gradeType) {
		this.selectedGradeType = gradeType;
	}

	public String getSelectedGradeType() {
		return selectedGradeType;
	}

	public void setGradeMapping(GradeMapping gradeMapping) {
		this.gradeMapping = gradeMapping;
	}

	public GradeMapping getGradeMapping() {
		return gradeMapping;
	}

    @SuppressWarnings("unchecked")
    public List<GradeMapDisplayRow> getGradeRows() {
      MemoryService memoryService = (MemoryService) ComponentManager.get("org.sakaiproject.memory.api.MemoryService");
      return null == gradeRows ? (List<GradeMapDisplayRow>) memoryService.newCache("gradebookExportCache" + getGradebookId())
                                 .get("gradeRows") : gradeRows;
	  }

	public Long getSelectedGradeMappingId() {
		return selectedGradeMappingId;
	}

	public List<SelectItem> getGradeMappingsSelectItems() {
		return gradeMappingsSelectItems;
	}

    @SuppressWarnings("unchecked")
    public List<SectionEnrollmentsRow> getSectionEnrollmentList() {
	    MemoryService memoryService = (MemoryService) ComponentManager.get("org.sakaiproject.memory.api.MemoryService");
	    return null == sectionEnrollmentList ? (List<SectionEnrollmentsRow>) memoryService.newCache("gradebookExportCache" + getGradebookId())
	                                           .get("sectionEnrollmentList") : sectionEnrollmentList;
	  }

	private void initGradeRows() {
		// Set up UI table view.
		gradeRows = new ArrayList<GradeMapDisplayRow>();
		Double previousValue = 101.0;
		for (String grade : gradeMapping.getGrades()) {
			Double d = gradeMapping.getDefaultBottomPercents().get(grade);
			if (d != null) {
				gradeRows.add(new GradeMapDisplayRow(grade, d, previousValue - 1.0));
				previousValue = d;
			}
		}
		MemoryService memoryService = (MemoryService) ComponentManager.get("org.sakaiproject.memory.api.MemoryService");
		memoryService.newCache("gradebookExportCache" + getGradebookId()).put("gradeRows", gradeRows);
	}

	protected void init() {
		if (!workInProgress) {
			localGradebook = getGradebookManager()
					.getGradebookWithGradeMappings(getGradebookId());

			// Create the grade type drop-down menu
			gradeMappingsSelectItems = new ArrayList<SelectItem>(localGradebook
					.getGradeMappings().size());
			for (GradeMapping gm : localGradebook.getGradeMappings()) {
				gradeMappingsSelectItems.add(new SelectItem(gm.getId(), gm
						.getName()));
			}

			selectedGradeMappingId = localGradebook.getSelectedGradeMapping()
					.getId();
			gradeMapping = localGradebook.getSelectedGradeMapping();
			selectedGradeType = MID_TERM_GRADE;
			initGradeRows();
			getViewableSections();
			initSectionEnrollmentList();
		}

		// Set the view state.
		workInProgress = true;
	}


	
	
	public String showExportChoices() {
		setPageName("sissGradeExportChoices");
		return "sissGradeExportChoices";
	}

	public String showStudentGrades() {
		if (logger.isDebugEnabled()) {
			logger.debug("Build students grade records (total memory: " + Runtime.getRuntime().totalMemory() + ")");
		}
				
		// Call getViewableSections initializes the list if it needs it
		getViewableSections();
		initSectionEnrollmentList();
		setPageName("sissGradeExportStudentGrades");
		
		if (logger.isDebugEnabled()) {
			logger.debug("Done build students grade records (total memory: " + Runtime.getRuntime().totalMemory() + ")");
		}
		
		return "sissGradeExportStudentGrades";
	}

  private void initSectionEnrollmentList() {
    if (null == getSectionEnrollmentList()) {
      sectionEnrollmentList = new ArrayList<SectionEnrollmentsRow>();
      CourseGrade courseGrade = getGradebookManager().getCourseGrade(getGradebookId());
      if (viewableSections != null) {
        for (CourseSection section : viewableSections) {
          Map enrollmentMap = getGradebookBean().getAuthzService().findMatchingEnrollmentsForViewableCourseGrade(getGradebookUid(),
                              getGradebook().getCategory_type(), null, section.getUuid());

          List<ScoreRow> sectionScoreRows = new ArrayList<ScoreRow>(enrollmentMap.size());
          for (Iterator<EnrollmentRecord> iter = enrollmentMap.keySet().iterator(); iter.hasNext();) {
            EnrollmentRecord record = iter.next();
            CourseGradeRecord gradeRecord = getGradebookManager().getPointsEarnedCourseGradeRecords(courseGrade, 
                    record.getUser().getUserUid());
            if (gradeRecord == null) {
              gradeRecord = new CourseGradeRecord(courseGrade, record.getUser().getUserUid());
            }

            Map<String, String> userData = null;
            ScoreRow scoreRow = null;
            try {
              String userEid = UserDirectoryService.getUserEid(record.getUser().getUserUid());
              if (userEid != null) {
                userData = getCsvMappingFileHelper().getUser(userEid);
              }
              if (userData != null) {
                scoreRow = new ScoreRow(record, gradeRecord, userData.get(CsvMappingFileHelper.DUKEID),
                           userData.get(CsvMappingFileHelper.FIRSTNAME),
                           userData.get(CsvMappingFileHelper.LASTNAME));
              } else {
                logger.warn("User with id [" + record.getUser().getUserUid()
                            + "] not found in CSV Mapping file, skipping course grade record");
                String userName = record.getUser().getDisplayName();
                String nameParts[] = userName.split(",");
                scoreRow = new ScoreRow(record, gradeRecord, userEid, nameParts[0],
                                        nameParts.length > 1 ? nameParts[1] : "");
              }
              sectionScoreRows.add(scoreRow);
            } catch (UserNotDefinedException ue) {
                logger.warn("User not found in CSV Mapping file, skipping course grade record");
              }
          }

          // sorting the score records by user sort name
          Collections.sort(sectionScoreRows, new Comparator<ScoreRow>() {
            public int compare(ScoreRow o1, ScoreRow o2) {
              String s1 = o1 == null ? "" : o1.getDukeUserName();
              String s2 = o2 == null ? "" : o2.getDukeUserName();
              return s1.compareTo(s2);
            }
          });

          String courseId = section.getEid();
          Map<String, String> courseData = getCsvMappingFileHelper().getCourse(courseId);
          if (courseData != null) {
            sectionEnrollmentList.add(new SectionEnrollmentsRow(section, sectionScoreRows, 
                                      GRADED_CODE.equals(courseData.get(CsvMappingFileHelper.GRADED)), 
                                      courseData.get(CsvMappingFileHelper.TERMID), 
                                      courseData.get(CsvMappingFileHelper.COURSENBR)));
            } else {
              logger.warn("Course with id [" + courseId + "] not found in CSV Mapping file, skipping grade records for section.");
              String[] ref = section.getCourse().getUuid().split("/");
              String siteId = null;
              for (int i = 0; i < ref.length; i++) {
                if ("site".equals(ref[i]) && (ref.length > i + 1)) {
                  siteId = ref[i + 1];
                  break;
                }
              }
              try {
                if (siteId != null) {
                  Site site = SiteService.getSite(siteId);
                  String termEid = (String) site.getProperties().get("term_eid");
                  sectionEnrollmentList.add(new SectionEnrollmentsRow(section, sectionScoreRows, false, termEid, ""));
                }
              } catch (IdUnusedException e) {
                logger.warn("Cannot find site with id [" + siteId + "]");
              }
            }
        }
      }
      MemoryService memoryService = (MemoryService) ComponentManager.get("org.sakaiproject.memory.api.MemoryService");
      memoryService.newCache("gradebookExportCache" + getGradebookId()).put("sectionEnrollmentList", sectionEnrollmentList);
    }
  }
}
