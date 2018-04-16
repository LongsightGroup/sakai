package org.sakaiproject.tool.gradebook.ui.duke;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;

import au.com.bytecode.opencsv.CSVReader;

public class CsvMappingFileHelper {

	private static final String CACHE_ENTRY_KEY = "csvMapRead";
	private static final String CACHE_NAME = "csvMapCache";

	private static final Log M_log = LogFactory
			.getLog(CsvMappingFileHelper.class);
	public static final String NETID = "netid";
	public static final String DUKEID = "dukeid";
	public static final String FIRSTNAME = "fname";
	public static final String LASTNAME = "lname";
	public static final String COURSEID = "courseid";
	public static final String TERMID = "termid";
	public static final String COURSENBR = "coursenbr";
	public static final String GRADED = "graded";

	public static final int NETID_INDEX = 0;
	public static final int DUKEID_INDEX = 1;
	public static final int FIRSTNAME_INDEX = 3;
	public static final int LASTNAME_INDEX = 2;
	public static final int COURSEID_INDEX = 0;
	public static final int TERMID_INDEX = 1;
	public static final int COURSENBR_INDEX = 2;
	public static final int GRADED_INDEX = 3;

	Map<String, Map<String, String>> userCache;
	Map<String, Map<String, String>> courseCache;
	private MemoryService memoryService;
	private Cache cache;

	private String batchFilePath;
	private String csvUserMappingFileName = "user-id-mapping.csv";
	private String csvCourseMappingFileName = "course-num-mapping.csv";
	private boolean hasHeader = false;
	private boolean isInitialized;

	public void setHasHeader(boolean hasHeader) {
		this.hasHeader = hasHeader;
	}

	public boolean isInitialzied() {
		return isInitialized;
	}
	
	public synchronized void init() {
		memoryService = (MemoryService) ComponentManager.get("org.sakaiproject.memory.api.MemoryService");
		cache = memoryService.newCache(CACHE_NAME);
		/* Initialize the grade mapping CSV files directory
		 * Configure using: gradebook.csv.mapping.dir=/full/path/to/dir
		 * OR fallback to {sakora-csv 'net.unicon.sakora.csv.batchUploadDir'} + '/mapping'
		 * OR DEFAULT to {SakaiHomePath} + '/sakora-csv/mapping'
		 */
		if (batchFilePath == null || "".equals(batchFilePath)) {
		    ServerConfigurationService scs = (ServerConfigurationService) ComponentManager.get("org.sakaiproject.component.api.ServerConfigurationService");
		    String basePath = scs.getString("gradebook.csv.mapping.dir");
		    if (basePath == null || "".equals(basePath)) {
		        // NOTE: this will default to using the sakora-csv path because that is easier for Duke -AZ
		        basePath = scs.getString("net.unicon.sakora.csv.batchUploadDir");
		        if (basePath == null || "".equals(basePath)) {
		            basePath = scs.getSakaiHomePath() + File.separator + "sakora-csv";
		            M_log.warn("No path configured for grade mapping csv file under 'gradebook.csv.mapping.dir' or 'net.unicon.sakora.csv.batchUploadDir', using the default fallback: "+basePath);
		        }
		        // add the mapping subfolder to the sakora path
		        basePath = new File(basePath, "mapping").getAbsolutePath();
		    }
		    File dir = new File(basePath);
		    if ( !(dir.exists()) ) {
		        if ( !(dir.mkdirs()) ) {
		            M_log.error("Unable to create grade mapping csv directory at: " + basePath);
		        }
		    } else if ( !(dir.isDirectory()) ) {
		        M_log.error("The configured grade mapping csv path is not a directory: " + basePath);
		    } else if ( !(dir.canRead()) ) {
		        M_log.error("The configured grade mapping csv directory is not readable: " + basePath);
		    } else {
		        M_log.info("grade mapping csv directory initialized: "+basePath);
		    }
		    batchFilePath = new File(basePath).getAbsolutePath(); // this will cleanup the path
		}
		M_log.info("Duke Gradebook grade mapping csv file path initialized at: "+batchFilePath);

		readCache();
	}
	
	public synchronized void destroy() {
		if (M_log.isDebugEnabled()) {
			M_log.debug("Start clearing user map and course map (total memory: "
					+ Runtime.getRuntime().totalMemory());
		}
		courseCache.clear();
		courseCache = null;
		userCache.clear();
		userCache = null;

		if (M_log.isDebugEnabled()) {
			M_log.debug("Done clearing user map and course map (total memory: "
					+ Runtime.getRuntime().totalMemory());
		}

	}
	
	
	protected synchronized void readCache() {
		if (M_log.isDebugEnabled()) {
			M_log.debug("Cache status [" + cache.get(CACHE_ENTRY_KEY) + "]");
		}
		
		if (cache.get(CACHE_ENTRY_KEY) == null) {
			if (M_log.isDebugEnabled()) {
				M_log.debug("Start initializing user map and course map ( "
						+ this + " free memory: "
						+ Runtime.getRuntime().freeMemory() + ")");
			}

			courseCache = new HashMap<String, Map<String, String>>();
			userCache = new HashMap<String, Map<String, String>>();
			readMapping(csvUserMappingFileName, new RecordReadCallback() {
				public void doWithRecord(String[] line) {
					Map<String, String> user = new HashMap<String, String>();
					if (StringUtils.isNotBlank(line[NETID_INDEX])) {
						user.put(NETID, line[NETID_INDEX]);
						user.put(DUKEID, line[DUKEID_INDEX]);
						user.put(FIRSTNAME, line[FIRSTNAME_INDEX]);
						user.put(LASTNAME, line[LASTNAME_INDEX]);
						userCache.put(line[NETID_INDEX], user);
					}
				}
			});
			readMapping(csvCourseMappingFileName, new RecordReadCallback() {
				public void doWithRecord(String[] line) {
					Map<String, String> course = new HashMap<String, String>();
					if (StringUtils.isNotBlank(line[COURSEID_INDEX])) {
						course.put(COURSEID, line[COURSEID_INDEX]);
						course.put(TERMID, line[TERMID_INDEX]);
						course.put(COURSENBR, line[COURSENBR_INDEX]);
						if (GRADED_INDEX < line.length) {
							course.put(GRADED, line[GRADED_INDEX]);
						}
						courseCache.put(line[0], course);
					}
				}
			});

			cache.put(CACHE_ENTRY_KEY, "someValue");
			if (M_log.isDebugEnabled()) {
				M_log.debug("Done initializing user map and course map ( "
						+ this + " free memory: "
						+ Runtime.getRuntime().freeMemory() + ")");
			}

		}
	}

	protected void readMapping(String csvFileName, RecordReadCallback callback) {
		BufferedReader br = null;
		CSVReader csvr = null;
		String[] line = null;

		File csvFile = new File(batchFilePath, csvFileName);
		try {
			br = new BufferedReader(new FileReader(csvFile));
			csvr = new CSVReader(br);

			// if the csv files have headers, skip them
			if (hasHeader) {
				csvr.readNext();
			}

			while (null != (line = csvr.readNext())) {
				callback.doWithRecord(line);
			}
			isInitialized = true;
		} catch (FileNotFoundException ffe) {
			M_log.error(
					"CSV reader failed to locate file ["
							+ csvFile.getAbsolutePath() + "]", ffe);
			isInitialized = false;
		} catch (IOException ioe) {
			M_log.error(
					"CSV reader failed to read from file ["
							+ csvFile.getAbsolutePath() + "]", ioe);
			isInitialized = false;
		} finally {
			if (csvr != null) {
				try {
					csvr.close();
				} catch (IOException ioe) {
					M_log.error("Unable to close CSV reader", ioe);
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException ioe) {
					M_log.error(
							"Unable to close the BufferedReader for CSV Reader",
							ioe);
				}
			}
		}
	}

	public Map<String, String> getUser(String eid) {
		readCache();
		if (isInitialized && userCache != null) {
			return userCache.get(eid);
		} else {
			return null;
		}
	}

	public Map<String, String> getCourse(String courseId) {
		readCache();
		if (isInitialized && courseCache != null) {
			return courseCache.get(courseId);
		} else {
			return null;
		}
	}
	
	protected static interface RecordReadCallback {
		public void doWithRecord(String[] line);
	}
}
