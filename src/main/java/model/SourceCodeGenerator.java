package model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jsonschema2pojo.SourceType;
import org.testng.annotations.Test;

import controller.HTMLUtility;
import controller.SwaggerParser;
import controller.customexceptions.BlankURLException;
import controller.customexceptions.InvalidUrlException;

public class SourceCodeGenerator {

	static Logger logger = Logger.getLogger(SourceCodeGenerator.class);
	private String outputDirectory;
	private List<Section> sectionList;
	private String pageName;
	
	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	public List<Section> getSectionList() {
		return sectionList;
	}

	public void setSectionList(List<Section> sectionList) {
		this.sectionList = sectionList;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	private void generateTaskManager(APISpecification apiSpecification, String sectionName, File outputDir)
			throws IOException {
		logger.info("started cretion of TaskManager.java file for api :-" + apiSpecification.getApiName());
		CreateTaskManager createTaskManager = new CreateTaskManager();
		createTaskManager.create(apiSpecification, "com.monotype." + sectionName + "." + apiSpecification.getApiName().toLowerCase(),
				outputDir);
		logger.debug("Taskmanager file created for api" + apiSpecification.getApiName());
	}

	private void generateTasks(APISpecification apiSpecification, String sectionName, File outputDir)
			throws IOException {
		logger.info("Started creation of Task.java files for " + apiSpecification.getApiName());
		CreateTask createTask = new CreateTask();
		ExecuteTask executeTask = new ExecuteTask();
		ValidateTask validateTask = new ValidateTask();
		createTask.create(apiSpecification, "com.monotype." + sectionName + "." + apiSpecification.getApiName().toLowerCase(),
				outputDir);
		logger.debug("CreateRequestTask file created for " + apiSpecification.getApiName());
		executeTask.create(apiSpecification, "com.monotype." + sectionName + "." + apiSpecification.getApiName().toLowerCase(),
				outputDir);
		logger.debug("ExecuteRequestTask file created for " + apiSpecification.getApiName());
		validateTask.create(apiSpecification, "com.monotype." + sectionName + "." + apiSpecification.getApiName().toLowerCase(),
				outputDir);
		logger.debug("ValidateResponseTask file created for " + apiSpecification.getApiName());
		logger.info("Completed creation of Task.java files for " + apiSpecification.getApiName());
	}

	private void generateTestClass(APISpecification apiSpecification, String sectionName, File outputDir)
			throws IOException {
		logger.info("Started creation of Test class for " + apiSpecification.getApiName());
		CreateTest createTest = new CreateTest();
		createTest.createTestClass(apiSpecification, "com.monotype." + sectionName + "." + apiSpecification.getApiName().toLowerCase(),
				outputDir);
		logger.info("Completed creation of Test class for " + apiSpecification.getApiName());
	}

	private void generatePojo(APISpecification apiSpecification, String sectionName, File outputDirectory,
			SourceType sourceType) throws IOException {
		CreatePojo createPojo = new CreatePojo();
		createPojo.generatePojoFromApiSpecification(apiSpecification, outputDirectory,
				"com.monotype." + sectionName + "." + apiSpecification.getApiName().toLowerCase() + ".pojo", sourceType);
		logger.debug("Pojo files created for " + apiSpecification.getApiName());
	}

	public void generateCode() throws InvalidUrlException, BlankURLException, IOException {
		logger.info("Inside generateCode - starting the Pojo.java, Task.java and TaskManager.java file creation. ");
		String sectionName;
		SourceType sourceType = null;
		File outPutDir = new File(getOutputDirectory() + "/src/main/java"+getPageName());
		outPutDir.mkdirs();
		List<Section> sectionList = getSectionList();
		if (SwaggerParser.getSwaggerVersion().equalsIgnoreCase("v1")) {
			sourceType = SourceType.JSON;
		} else if (SwaggerParser.getSwaggerVersion().equalsIgnoreCase("v2")) {
			sourceType = SourceType.JSONSCHEMA;
		}
		for (Section section : sectionList) {
			sectionName = section.getSectionName();
			logger.debug("Starting the Pojo.java, Task.java and TaskManager.java file creation for section :-"
					+ sectionName);
			APIList apiList = section.getApiList();
			List<APISpecification> apiListSpecifications = apiList.getApiList();
			for (APISpecification apiSpecification : apiListSpecifications) {
				generatePojo(apiSpecification, sectionName, outPutDir, sourceType);
				generateTaskManager(apiSpecification, sectionName, outPutDir);
				generateTasks(apiSpecification, sectionName, outPutDir);
				generateTestClass(apiSpecification, sectionName, outPutDir);
			}
			logger.debug("Completed the Pojo.java, Task.java and TaskManager.java file creation for section :-"
					+ sectionName);
		}
		logger.info("Inside generateCode - Completed the Pojo.java, Task.java and TaskManager.java file creation. ");
	}

}
