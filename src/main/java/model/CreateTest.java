package model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

public class CreateTest {
	static Logger logger = Logger.getLogger(CreateTest.class);
	
	public void createTestClass(APISpecification apiSpecification, String packageName, File outputDirectory)
			throws IOException {
		logger.info("Generating CreateTest.java file - creating "+generateApiName(apiSpecification) + "Test.java"+" file.");
		Builder classBuilder = TypeSpec.classBuilder(generateApiName(apiSpecification) + "Test")
				.addModifiers(Modifier.PUBLIC).superclass(ClassName.get("com.monotype.api_utils.core.test", "BaseTest"))
				.addMethod(createTestCase(apiSpecification, packageName, outputDirectory))
				.addMethod(createDataDrivenTestCase(apiSpecification, packageName, outputDirectory))
				.addMethod(createDifferentialsTestCase(apiSpecification, packageName, outputDirectory));
		TypeSpec build = classBuilder.build();
		JavaFile javaFile = JavaFile.builder(packageName, build).build();
		javaFile.writeTo(new File(outputDirectory.getAbsolutePath().replace(outputDirectory.separator, "/")
				.replaceAll("src/main/java", "src/test/java")));
		logger.info("Test file created for : "+generateApiName(apiSpecification));
	}

	/**
	 * This method creates the test case for an api with given apiSpecification.
	 * 
	 * @param apiSpecification
	 * @param packageName
	 * @return
	 */
	private MethodSpec createTestCase(APISpecification apiSpecification, String packageName, File outputDirectory) {
		logger.info("Generating testCase() for api: "+apiSpecification.getApiName());
		
		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("testCase")
				.addModifiers(Modifier.PUBLIC).returns(void.class)
				.addAnnotation(AnnotationSpec.builder(ClassName.get("org.testng.annotations", "Test"))
						.addMember("groups", "{\"Regression\"}").build())
				.addAnnotation(AnnotationSpec
						.builder(ClassName.get("com.monotype.mt_common_utils.testRailUtils", "TestRailId"))
						.addMember("ids", "$S", "").build())
				.addJavadoc("This is auto generated test case for an api with given apiSpecification.\n")
				.addStatement("$T $N = new $T(MTLogger)",
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"), "taskManager",
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"))
				.beginControlFlow("try").addStatement("taskManager.setBaseUri($T.getProperty(\"baseURI\"))",
						ClassName.get("com.monotype.api_utils.settings", "PropertiesManager"));

		List<Parameters> parameters = apiSpecification.getParameters();
		for (Parameters parameter : parameters) {
			if ((parameter.getParameterDataType() != null && parameter.getParameterDataType().contains("Model"))
					|| (parameter.getParamaterType() != null && parameter.getParamaterType().contains("body"))) {
				if (parameter.getModelExample() != null && isJSONValid(parameter.getModelExample())) {
					methodBuilder.addStatement("$T $N = new $T()",
							ClassName.get(packageName + ".pojo.request", generateApiName(apiSpecification) + "Request"),
							"requestInput",
							ClassName.get(packageName + ".pojo.request", generateApiName(apiSpecification) + "Request"));
					methodBuilder.addComment(
							"TODO Currently 'requestInput' object is empty. Populate this object with request body input data.");
					methodBuilder.addStatement("$N.setRequest($N)", "taskManager", "requestInput");
				}
			}
		}

		Map<String, String> pathParameterList = getPathParametersFromApiSpecification(apiSpecification);

		addPath_Header_QueryParameter(methodBuilder, pathParameterList, parameters);

		methodBuilder.addStatement("$N.perform()", "taskManager");
		String pojoLocation = outputDirectory + "/" + packageName + "/pojo/response/";
		File responsePojoFile = new File(pojoLocation.replaceAll("\\.", "/"),
				apiSpecification.getApiName() + "Response.java");
		boolean responseFileExist = false;
		boolean responseArray=false;
		if (responsePojoFile.exists()) {
			responseFileExist = true;
			String responseBodyContent = apiSpecification.getResponseJson();
			if (responseBodyContent.charAt(0) == '[') {
				responseArray=true;
				methodBuilder.addStatement("$T []apiResponse = $N.getRestDriver().getAPIResponseAsPOJO($T[].class)",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"),
						"taskManager",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			} else {
				methodBuilder.addStatement("$T apiResponse = $N.getRestDriver().getAPIResponseAsPOJO($T.class)",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"),
						"taskManager",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			}
		} else {
			methodBuilder.addStatement("$T apiResponse = $N.getRestDriver().getAPIResponseAsString()",
					ClassName.get(String.class), "taskManager");
		}
		methodBuilder.addStatement(
				"MTLogger.log($T.INFO, \"Api response is : \" + $N.getRestDriver().getAPIResponseAsString())",
				ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"), "taskManager");
		if (responseFileExist) {
			methodBuilder
			.addComment("TODO Replace 'expectedResponse' with api's expected response object for comparison.");
			if(responseArray)
				methodBuilder.addStatement("$T []expectedResponse = null",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			else
			methodBuilder.addStatement("$T expectedResponse = null",
					ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			methodBuilder.addStatement("$T.AreEqual(\"Assertion message goes here\", expectedResponse, apiResponse)",
					ClassName.get("com.monotype.mt_common_utils.assertions", "MTAssertion"));
		} else {
			methodBuilder.addComment("TODO Enter 'expected apiResponse' below for comparison.");
			methodBuilder.addStatement(
					"$T.AreEqual(\"Assertion message goes here\", \"Enter expected apiResponse here\", apiResponse)",
					ClassName.get("com.monotype.mt_common_utils.assertions", "MTAssertion"));
		}
		methodBuilder.endControlFlow();
		addCatchBlock(methodBuilder, "");
		methodBuilder.beginControlFlow("finally");
		addFinallyBlock(methodBuilder, "taskManager");
		methodBuilder.endControlFlow();
		return methodBuilder.build();
	}

	/**
	 * This method create the test case using data driven approach for an api
	 * with given apiSpecification.
	 * 
	 * @param apiSpecification
	 * @param packageName
	 * @return
	 */
	private MethodSpec createDataDrivenTestCase(APISpecification apiSpecification, String packageName,
			File outputDirectory) {
		logger.info("Generating testCaseDataDriven() for api: "+apiSpecification.getApiName());
		
		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("testCaseDataDriven")
				.addModifiers(Modifier.PUBLIC).returns(void.class)
				.addAnnotation(AnnotationSpec.builder(ClassName.get("org.testng.annotations", "Test"))
						.addMember("groups", "{\"Regression\"}")
						.addMember("dataProvider", "$S", "dataproviderForTestCase")
						.addMember("dataProviderClass", "$T.class",
								ClassName.get("com.monotype.mt_common_utils.datadriver", "GenericDataProvider"))
						.build())
				.addAnnotation(AnnotationSpec
						.builder(ClassName.get("com.monotype.mt_common_utils.annotations.CSVAnnotation",
								"CSVFileParameters"))
						.addMember("path", "$S", "test-data\\api\\" + apiSpecification.getApiName() + "DataDriven.csv")
						.addMember("delimiter", "$S", "###").build())
				.addParameter(ParameterSpec.builder(int.class, "rowNumber").build())
				.addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Map.class),
						ClassName.get(String.class), ClassName.get(String.class)), "testData").build())
				.addComment("TODO Please create a "+apiSpecification.getApiName() + "DataDriven.csv file which contains test data. ")
				.addJavadoc(
						"This is auto-generated data driven test case for an api with given apiSpecification details.\n")
				.addStatement("MTLogger.log($T.INFO, \"Test data from CSV - \"+ testData.toString())",
						ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"))
				.addStatement("$T $N = new $T(MTLogger)",
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"), "taskManager",
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"))
				.beginControlFlow("try").addStatement("taskManager" + ".setBaseUri($T.getProperty(\"baseURI\"))",
						ClassName.get("com.monotype.api_utils.settings", "PropertiesManager"));

		List<Parameters> parameters = apiSpecification.getParameters();
		for (Parameters parameter : parameters) {
			if ((parameter.getParameterDataType() != null && parameter.getParameterDataType().contains("Model"))
					|| (parameter.getParamaterType() != null && parameter.getParamaterType().contains("body"))) {
				if (parameter.getModelExample() != null && isJSONValid(parameter.getModelExample())) {

					methodBuilder.addStatement("$T jsonSerializer = new $T()",
							ClassName.get("com.monotype.api_utils.serializer", "JSONSerializer"),
							ClassName.get("com.monotype.api_utils.serializer", "JSONSerializer"));
					methodBuilder.addStatement("$T $N = jsonSerializer.stringToPOJO(testData.get(\"json\"), $T.class)",
							ClassName.get(packageName + ".pojo.request", generateApiName(apiSpecification) + "Request"),
							"requestInput",
							ClassName.get(packageName + ".pojo.request", generateApiName(apiSpecification) + "Request"));
					methodBuilder.addComment(
							"TODO Currently 'requestInput' object is empty. Populate 'requestInput' object with request input data.");
					methodBuilder.addStatement("$N.setRequest($N)", "taskManager", "requestInput");
				}
			}
		}

		Map<String, String> pathParameterList = getPathParametersFromApiSpecification(apiSpecification);

		addPath_Header_QueryParameter(methodBuilder, pathParameterList, parameters);

		methodBuilder.addStatement("$N.perform()", "taskManager");
		String pojoLocation = outputDirectory + "/" + packageName + "/pojo/response/";
		File responsePojoFile = new File(pojoLocation.replaceAll("\\.", "/"),
				apiSpecification.getApiName() + "Response.java");
		boolean responseFileExist = false;
		boolean doesResponseAsArray = false;
		if (responsePojoFile.exists()) {
			responseFileExist = true;
			String responseBodyContent = apiSpecification.getResponseJson();
			if (responseBodyContent.charAt(0) == '[') {
				doesResponseAsArray = true;
				methodBuilder.addStatement("$T []apiResponse = $N.getRestDriver().getAPIResponseAsPOJO($T[].class)",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"),
						"taskManager",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			} else {
				methodBuilder.addStatement("$T apiResponse = $N.getRestDriver().getAPIResponseAsPOJO($T.class)",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"),
						"taskManager",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			}
		} else {
			methodBuilder.addStatement("$T apiResponse = $N.getRestDriver().getAPIResponseAsString()",
					ClassName.get(String.class), "taskManager");
		}

		methodBuilder.addStatement(
				"MTLogger.log($T.INFO, \"Api response is : \" + $N.getRestDriver().getAPIResponseAsString())",
				ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"), "taskManager");
		if (responseFileExist) {
			methodBuilder
					.addComment("TODO Replace 'expectedResponse' with api's expected response object for comparison.");
			if (doesResponseAsArray)
				methodBuilder.addStatement("$T []expectedResponse = null",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			else
				methodBuilder.addStatement("$T expectedResponse = null",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			methodBuilder.addStatement("$T.AreEqual(\"Assertion message goes here\", expectedResponse, apiResponse)",
					ClassName.get("com.monotype.mt_common_utils.assertions", "MTAssertion"));
		} else {
			methodBuilder.addComment("TODO Enter below 'expected apiResponse' for comparison.");
			methodBuilder.addStatement(
					"$T.AreEqual(\"Assertion message goes here\", \"enter expected apiResponse here\", apiResponse)",
					ClassName.get("com.monotype.mt_common_utils.assertions", "MTAssertion"));
		}
		methodBuilder.endControlFlow();
		addCatchBlock(methodBuilder, "data-driven");
		methodBuilder.beginControlFlow("finally");
		addFinallyBlock(methodBuilder, "taskManager");
		methodBuilder.endControlFlow();
		return methodBuilder.build();

	}
	
	/**
	 * This method create the test case using data driven approach for an api
	 * with given apiSpecification.
	 * 
	 * @param apiSpecification
	 * @param packageName
	 * @return
	 */
	private MethodSpec createDifferentialsTestCase(APISpecification apiSpecification, String packageName,
			File outputDirectory) {
		logger.info("Generating testCaseDifferential() for api: "+apiSpecification.getApiName());
		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("testCaseDifferential")
				.addModifiers(Modifier.PUBLIC).returns(void.class)
				.addAnnotation(AnnotationSpec.builder(ClassName.get("org.testng.annotations", "Test"))
						.addMember("groups", "{\"Regression\"}")
						.addMember("dataProvider", "$S", "dataproviderForTestCase")
						.addMember("dataProviderClass", "$T.class",
								ClassName.get("com.monotype.mt_common_utils.datadriver", "GenericDataProvider"))
						.build())
				.addAnnotation(AnnotationSpec
						.builder(ClassName.get("com.monotype.mt_common_utils.annotations.CSVAnnotation",
								"CSVFileParameters"))
						.addMember("path", "$S",
								"test-data\\api\\" + apiSpecification.getApiName() + "Differential.csv")
						.addMember("delimiter", "$S", "###").build())
				.addParameter(ParameterSpec.builder(int.class, "rowNumber").build())
				.addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Map.class),
						ClassName.get(String.class), ClassName.get(String.class)), "testData").build())
				.addJavadoc(
						"This is auto-generated differential test case for an api with given apiSpecification details.")
				.addComment("TODO Please create a "+apiSpecification.getApiName() + "Differential.csv file which contains test data. ")
				.addStatement("MTLogger.log($T.INFO, \"Test data from CSV - \"+ testData.toString())",
						ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"))
				.addStatement("$T $N = new $T(MTLogger)",
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"),
						"taskManagerOriginal",
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"))
				.addStatement("$T $N = new $T(MTLogger)",
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"),
						"taskManagerRefactored",
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"))
				.beginControlFlow("try")
				.addStatement("taskManagerOriginal.setBaseUri($T.getProperty(\"baseURI\"))",
						ClassName.get("com.monotype.api_utils.settings", "PropertiesManager"))
				.addStatement("taskManagerRefactored" + ".setBaseUri($T.getProperty(\"refactoredBaseURI\"))",
						ClassName.get("com.monotype.api_utils.settings", "PropertiesManager"));

		List<Parameters> parameters = apiSpecification.getParameters();
		for (Parameters parameter : parameters) {
			if ((parameter.getParameterDataType() != null && parameter.getParameterDataType().contains("Model"))
					|| (parameter.getParamaterType() != null && parameter.getParamaterType().contains("body"))) {
				if (parameter.getModelExample() != null && isJSONValid(parameter.getModelExample())) {

					methodBuilder.addStatement("$T jsonSerializer = new $T()",
							ClassName.get("com.monotype.api_utils.serializer", "JSONSerializer"),
							ClassName.get("com.monotype.api_utils.serializer", "JSONSerializer"));
					methodBuilder.addStatement("$T $N = jsonSerializer.stringToPOJO(testData.get(\"json\"), $T.class)",
							ClassName.get(packageName + ".pojo.request", generateApiName(apiSpecification) + "Request"),
							"requestInputOriginal",
							ClassName.get(packageName + ".pojo.request", generateApiName(apiSpecification) + "Request"));
					methodBuilder.addStatement("$T $N = jsonSerializer.stringToPOJO(testData.get(\"json\"), $T.class)",
							ClassName.get(packageName + ".pojo.request", generateApiName(apiSpecification) + "Request"),
							"requestInputRefactored",
							ClassName.get(packageName + ".pojo.request", generateApiName(apiSpecification) + "Request"));
					methodBuilder.addComment(
							"TODO Currently 'taskManagerOriginal' and 'taskManagerRefactored' object are empty. Populate these objects with request input body data.");
					methodBuilder.addStatement("$N.setRequest($N)", "taskManagerOriginal", "requestInputOriginal");
					methodBuilder.addStatement("$N.setRequest($N)", "taskManagerRefactored", "requestInputRefactored");
				}
			}
		}

		Map<String, String> pathParameterList = getPathParametersFromApiSpecification(apiSpecification);
		if (pathParameterList.size() > 0) {
			methodBuilder.addStatement("$T pathParameters = new $T()",
					ParameterizedTypeName.get(Map.class, String.class, String.class),
					ParameterizedTypeName.get(HashMap.class, String.class, String.class));
			for (Map.Entry<String, String> entry : pathParameterList.entrySet()) {
				methodBuilder.addComment("TODO Enter " + entry.getKey() + " parameter value below.");
				methodBuilder.addStatement("pathParameters.put(\"$N\",\"enter " + entry.getKey() + " value here\")",
						entry.getKey());
			}
			methodBuilder.addStatement("$N.setPathParameterList(pathParameters)", "taskManagerOriginal");
			methodBuilder.addStatement("$N.setPathParameterList(pathParameters)", "taskManagerRefactored");
		}

		Map<String, String> queryParameterList = new HashMap<String, String>();
		Map<String, String> headerList = new HashMap<String, String>();
		for (Parameters parameter : parameters) {
			if (parameter.getParamaterType() != null && parameter.getParamaterType().equals("query")) {
				queryParameterList.put(parameter.getParameterName(), null);
			}
			if (parameter.getParamaterType() != null && parameter.getParamaterType().equals("header"))
				headerList.put(parameter.getParameterName(), null);
		}

		if (headerList.size() > 0) {
			methodBuilder.addStatement("$T headers = new $T()", ParameterizedTypeName.get(ClassName.get(Map.class),
					ClassName.get(String.class), ClassName.get(String.class)), ClassName.get(HashMap.class));
			for (Map.Entry<String, String> header : headerList.entrySet()) {
				methodBuilder.addComment("TODO Enter " + header.getKey() + " header value below.");
				methodBuilder.addStatement("headers.put(\"$N\", \"enter " + header.getKey() + " value here.\")",
						header.getKey());
			}
			methodBuilder.addStatement("$N.setHeaders(headers)", "taskManagerOriginal");
			methodBuilder.addStatement("$N.setHeaders(headers)", "taskManagerRefactored");
		} else {
			methodBuilder.addComment("TODO If needed then please uncomment below lines and enter valid header key-value or delete these lines in case you don't need it.");
			methodBuilder.addComment("$T headers = new $T();", ParameterizedTypeName.get(ClassName.get(Map.class),
					ClassName.get(String.class), ClassName.get(String.class)), ClassName.get(HashMap.class));
			methodBuilder.addComment("headers.put(\"Enter header key\", \"Enter header value\");");
			methodBuilder.addComment("$N.setHeaders(headers);", "taskManagerOriginal");
			methodBuilder.addComment("$N.setHeaders(headers);", "taskManagerRefactored");
		}

		if (queryParameterList.size() > 0) {
			methodBuilder.addStatement("$T queryParameters = new $T()",
					ParameterizedTypeName.get(Map.class, String.class, String.class),
					ParameterizedTypeName.get(HashMap.class, String.class, String.class));
			for (Map.Entry<String, String> entry : queryParameterList.entrySet()) {
				methodBuilder.addComment("TODO Enter " + entry.getKey() + " query parameter value below.");
				methodBuilder.addStatement("queryParameters.put(\"$N\", \"enter " + entry.getKey() + " value here.\")",
						entry.getKey());
			}
			methodBuilder.addStatement("$N.setQueryParameterList(queryParameters)", "taskManagerOriginal");
			methodBuilder.addStatement("$N.setQueryParameterList(queryParameters)", "taskManagerRefactored");
		}

		methodBuilder.addStatement("$N.perform()", "taskManagerOriginal");
		methodBuilder.addStatement("$N.perform()", "taskManagerRefactored");
		String pojoLocation = outputDirectory + "/" + packageName + "/pojo/response/";
		File responsePojoFile = new File(pojoLocation.replaceAll("\\.", "/"),
				apiSpecification.getApiName() + "Response.java");
		if (responsePojoFile.exists()) {
			String responseBodyContent = apiSpecification.getResponseJson();
			if (responseBodyContent.charAt(0) == '[') {
				methodBuilder.addStatement(
						"$T []apiResponseOriginal = $N.getRestDriver().getAPIResponseAsPOJO($T[].class)",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"),
						"taskManagerOriginal",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
				methodBuilder.addStatement(
						"$T []apiResponseRefactored = $N.getRestDriver().getAPIResponseAsPOJO($T[].class)",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"),
						"taskManagerRefactored",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			} else {
				methodBuilder.addStatement("$T apiResponseOriginal = $N.getRestDriver().getAPIResponseAsPOJO($T.class)",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"),
						"taskManagerOriginal",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
				methodBuilder.addStatement(
						"$T apiResponseRefactored = $N.getRestDriver().getAPIResponseAsPOJO($T.class)",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"),
						"taskManagerRefactored",
						ClassName.get(packageName + ".pojo.response", generateApiName(apiSpecification) + "Response"));
			}
		} else {
			methodBuilder.addStatement("$T apiResponseOriginal = $N.getRestDriver().getAPIResponseAsString()",
					ClassName.get(String.class), "taskManagerOriginal");
			methodBuilder.addStatement("$T apiResponseRefactored = $N.getRestDriver().getAPIResponseAsString()",
					ClassName.get(String.class), "taskManagerRefactored");
		}

		methodBuilder.addStatement(
				"MTLogger.log($T.INFO, \"Api response for original version is : \" + $N.getRestDriver().getAPIResponseAsString())",
				ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"), "taskManagerOriginal");
		methodBuilder.addStatement(
				"MTLogger.log($T.INFO, \"Api response for Refactoreded version is : \" + $N.getRestDriver().getAPIResponseAsString())",
				ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"),
				"taskManagerRefactored");

		methodBuilder.addStatement(
				"$T.AreEqual(\"Assertion message goes here\", apiResponseOriginal, apiResponseRefactored)",
				ClassName.get("com.monotype.mt_common_utils.assertions", "MTAssertion"));
		methodBuilder.endControlFlow();
		addCatchBlock(methodBuilder, "differential");
		methodBuilder.beginControlFlow("finally");
		addFinallyBlock(methodBuilder, "taskManagerOriginal");
		addFinallyBlock(methodBuilder, "taskManagerRefactored");
		methodBuilder.endControlFlow();

		return methodBuilder.build();

	}

	/**
	 * This method return the existing path parameters from apiPath available
	 * inside given apiSpecification.
	 * 
	 * @param apiSpecification
	 * @return
	 */
	private Map<String, String> getPathParametersFromApiSpecification(APISpecification apiSpecification) {
		String apiBasePath = apiSpecification.getPath();
		String apiPath = "";
		Map<String, String> pathParameterList = new HashMap<String, String>();
		if (apiBasePath.contains("{")) {
			apiPath = apiBasePath.substring(apiBasePath.indexOf("/{") + 1, apiBasePath.length());
			apiBasePath = apiBasePath.substring(0, apiBasePath.indexOf("/{") + 1);
			String[] split = apiPath.split("\\{");
			for (String str : split)
				if (str.contains("}"))
					pathParameterList.put(str.substring(0, str.indexOf("}")), null);
		}

		return pathParameterList;
	}

	public void addCatchBlock(MethodSpec.Builder methodBuilder, String testType) {
		methodBuilder.beginControlFlow("catch($T exception)", ClassName.get(Exception.class));
		methodBuilder.addStatement("exception.printStackTrace()");
		methodBuilder.addStatement(
				"MTLogger.log($T.FAIL, \"Error in "+testType+" test case. Message = \"+exception.getMessage())",
				ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"));
		methodBuilder.addStatement("$T.fail(exception.getMessage())",
				ClassName.get("com.monotype.mt_common_utils.assertions", "MTAssertion"));
		methodBuilder.endControlFlow();
	}
	
	/**
	 * This method generates the catch and finally block in each test case
	 * 
	 * @param methodBuilder
	 */
	public void addFinallyBlock(MethodSpec.Builder methodBuilder, String taskManagerName) {
		methodBuilder.addStatement("MTLogger.log($T.INFO, \"$N task Status :-\"+$N.getStatus())",
				ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"), taskManagerName,
				taskManagerName);
		methodBuilder.addStatement("MTLogger.log($T.INFO, \"$N task timing :- \\n \"+$N.getTaskTimings())",
				ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"), taskManagerName,
				taskManagerName);
	}
	
	/**
	 * This method adds the header and query parameters in taskmanager.
	 * @param methodBuilder
	 * @param parameters
	 */
	private void addPath_Header_QueryParameter(MethodSpec.Builder methodBuilder, Map<String, String> pathParameterList, List<Parameters> parameters) {
		if (pathParameterList.size() > 0) {
			methodBuilder.addStatement("$T pathParameters = new $T()",
					ParameterizedTypeName.get(Map.class, String.class, String.class),
					ParameterizedTypeName.get(HashMap.class, String.class, String.class));
			for (Map.Entry<String, String> entry : pathParameterList.entrySet()) {
				methodBuilder.addComment("TODO Enter " + entry.getKey() + " parameter value below.");
				methodBuilder.addStatement("pathParameters.put(\"$N\",\"enter " + entry.getKey() + " value here\")",
						entry.getKey());
			}
			methodBuilder.addStatement("$N.setPathParameterList(pathParameters)", "taskManager");
		}

		Map<String, String> queryParameterList = new HashMap<String, String>();
		Map<String, String> headerList = new HashMap<String, String>();
		for (Parameters parameter : parameters) {
			if (parameter.getParamaterType() != null && parameter.getParamaterType().equals("query")) {
				queryParameterList.put(parameter.getParameterName(), null);
			}
			if (parameter.getParamaterType() != null && parameter.getParamaterType().equals("header"))
				headerList.put(parameter.getParameterName(), null);
		}

		if (headerList.size() > 0) {
			methodBuilder.addStatement("$T headers = new $T()", ParameterizedTypeName.get(ClassName.get(Map.class),
					ClassName.get(String.class), ClassName.get(String.class)), ClassName.get(HashMap.class));
			for (Map.Entry<String, String> header : headerList.entrySet()) {
				methodBuilder.addComment("TODO Enter " + header.getKey() + " header value below.");
				methodBuilder.addStatement("headers.put(\"$N\", \"enter " + header.getKey() + " value here.\")",
						header.getKey());
			}
			methodBuilder.addStatement("$N.setHeaders(headers)", "taskManager");
		}  else {
			methodBuilder.addComment("TODO If needed then please uncomment below lines and enter valid header key-value or delete these lines if not required.");
			methodBuilder.addComment("$T headers = new $T();", ParameterizedTypeName.get(ClassName.get(Map.class),
					ClassName.get(String.class), ClassName.get(String.class)), ClassName.get(HashMap.class));
			methodBuilder.addComment("headers.put(\"Enter header key\", \"Enter header value\");");
			methodBuilder.addComment("$N.setHeaders(headers);", "taskManager");
		}

		if (queryParameterList.size() > 0) {
			methodBuilder.addStatement("$T queryParameters = new $T()",
					ParameterizedTypeName.get(Map.class, String.class, String.class),
					ParameterizedTypeName.get(HashMap.class, String.class, String.class));
			for (Map.Entry<String, String> entry : queryParameterList.entrySet()) {
				methodBuilder.addComment("TODO Enter " + entry.getKey() + " query parameter value below.");
				methodBuilder.addStatement("queryParameters.put(\"$N\", \"enter " + entry.getKey() + " value here.\")",
						entry.getKey());
			}
			methodBuilder.addStatement("$N.setQueryParameterList(queryParameters)", "taskManager");
		}
	}


	public boolean isJSONValid(String jsonInString) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(jsonInString);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * This method computes the api name which interns used 
	 * @param apiSpecification
	 * @return
	 */
	private String generateApiName(APISpecification apiSpecification) {
		String apiName = WordUtils.capitalize(apiSpecification.getPath().replaceAll("/|api|\\{|\\}|_|-", ""));
		return WordUtils.capitalize(apiSpecification.getHttpMethod())+ apiName;
	}
	
	
}
