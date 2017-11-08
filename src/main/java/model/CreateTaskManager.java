package model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

import org.apache.log4j.Logger;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;


public class CreateTaskManager {

	static Logger logger = Logger.getLogger(CreateTaskManager.class);
	String apiBasePath = "";
	String apiPath = "";

	/**
	 * This method generate a <ApiName>TaskManager.java file.
	 * @param apiSpecification
	 *            -api instance of the given api
	 * @param packageName
	 *            - packageName for the java file
	 * @param outputDirectory
	 *            - file path to store the generated .java file
	 * @throws IOException
	 */
	public void create(APISpecification apiSpecification, String packageName, File outputDirectory) throws IOException {
		logger.info("Start creating TaskManager.java file for api: " + apiSpecification.getApiName());
		apiBasePath = apiSpecification.getPath();
		Map<String, String> pathParameterList = new HashMap<String, String>();
		Map<String, String> queryParameterList = new HashMap<String, String>();
		if (apiBasePath.contains("{")) {
			apiPath = apiBasePath.substring(apiBasePath.indexOf("/{") + 1, apiBasePath.length());
			apiBasePath = apiBasePath.substring(0, apiBasePath.indexOf("/{") + 1);
			String[] split = apiPath.split("\\{");
			for (String str : split)
				if (str.contains("}"))
					pathParameterList.put(str.substring(0, str.indexOf("}")), null);
		}
		Builder classBuilder = TypeSpec.classBuilder(apiSpecification.getApiName() + "TaskManager")
				.addModifiers(Modifier.PUBLIC)
				.superclass(ParameterizedTypeName.get(ClassName.get("com.monotype.api_utils.core", "TaskManager"),
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager")))
				.addJavadoc("This class provide TaskManager for given api."
						+ "\n apiSpecification instance provide details about the api.\n")
				.addField(String.class, "baseUri", Modifier.PRIVATE)
				.addField(FieldSpec.builder(String.class, "apiBasePath").addModifiers(Modifier.PUBLIC)
						.initializer("$S", apiBasePath).build())
				.addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String.class), "headers", Modifier.PRIVATE).initializer("new $T()", ParameterizedTypeName.get(HashMap.class, String.class, String.class)).build())
				.addField(int.class, "expectedStatusCode")
				.addField(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger",
						Modifier.PRIVATE)
				.addMethod(constructor).addMethod(getBaseUri()).addMethod(getHeaders())// .addMethod(getBasePath()).addMethod(setBasePath())
				.addMethod(setBaseUri())// .addMethod(setInputData())
				.addMethod(getApiBasePath()).addMethod(setHeaders())// .addMethod(setApiBasePath())
				.addMethod(getExpectedStatusCode())// .addMethod(getMtLogger()).addMethod(setMtLogger())
				.addMethod(setExpectedStatusCode(200));
		// .addField(ParameterizedTypeName.get(Map.class, Object.class,
		// Object.class), "inputData", Modifier.PRIVATE)
		// .addField(String.class, "basePath", Modifier.PRIVATE)

		List<Parameters> parameters = apiSpecification.getParameters();
		boolean createApiPath = false;
		for (Parameters param : parameters) {
			if (param.getParamaterType() != null && param.getParamaterType().equals("path"))
				createApiPath = true;
			if (param.getParamaterType() != null && param.getParamaterType().equals("query")) {
				queryParameterList.put(param.getParameterName(), null);
			}
		}
		if (createApiPath) {
			classBuilder.addField(FieldSpec.builder(String.class, "apiPath").addModifiers(Modifier.PUBLIC)
					.initializer("$S", apiPath).build());
			classBuilder.addMethod(getApiPath());
			classBuilder.addMethod(setApiPath());
		}

		if (pathParameterList.size() > 0) {
			//classBuilder.addField(ParameterizedTypeName.get(Map.class, String.class, String.class), "pathParameterList", Modifier.PRIVATE);
			classBuilder.addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String.class), "pathParameterList", Modifier.PRIVATE).initializer("new $T()", ParameterizedTypeName.get(HashMap.class, String.class, String.class)).build());
			classBuilder.addMethod(getPathParameterList());
			classBuilder.addMethod(setPathParameterList());
		}

		if (queryParameterList.size() > 0) {
			//classBuilder.addField(ParameterizedTypeName.get(Map.class, String.class, String.class), "queryParameterList", Modifier.PRIVATE);
			classBuilder.addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String.class), "queryParameterList", Modifier.PRIVATE).initializer("new $T()", ParameterizedTypeName.get(HashMap.class, String.class, String.class)).build());
			classBuilder.addMethod(getQueryParameterList());
			classBuilder.addMethod(setQueryParameterList());
		}

		for (Parameters parameter : parameters) {
			if (parameter.getParamaterType() != null && parameter.getParamaterType().equals("body")) {
				String pojoLocation = outputDirectory + "/" + packageName + "/pojo/request/";
				File requestPojoFile = new File(pojoLocation.replaceAll("\\.", "/"),
						apiSpecification.getApiName() + "Request.java");
				if (requestPojoFile.exists()) {
					classBuilder.addField(FieldSpec.builder(Object.class, "request", Modifier.PRIVATE).build());
					classBuilder.addMethod(getRequestPojo());
					classBuilder.addMethod(setRequestPojo());
					break;
				}
			}
		}

		/*
		 * if (apiSpecification.getResponseJson() != null) { String pojoLocation
		 * = outputDirectory + "/" + packageName + "/pojo/response/"; File
		 * responsePojoFile = new File(pojoLocation.replaceAll("\\.", "/"),
		 * apiSpecification.getApiName() + "Response.java"); if
		 * (responsePojoFile.exists()) {
		 * classBuilder.addField(FieldSpec.builder( ClassName.get(packageName +
		 * ".pojo.response", apiSpecification.getApiName() + "Response"),
		 * "responsePojo", Modifier.PRIVATE).build()); classBuilder.addMethod(
		 * getResponsePojo(packageName + ".pojo.response",
		 * apiSpecification.getApiName() + "Response")); classBuilder.addMethod(
		 * setResponsePojo(packageName + ".pojo.response",
		 * apiSpecification.getApiName() + "Response")); } }
		 */

		/*
		 * for (Parameters parameter : parameters) { if
		 * (parameter.getParamaterType() != null &&
		 * parameter.getParamaterType().equals("body")) { String pojoLocation =
		 * outputDirectory + "/" + packageName + "/pojo/request/"; File
		 * requestPojoFile = new File(pojoLocation.replaceAll("\\.", "/"),
		 * apiSpecification.getApiName() + "Request.java"); if
		 * (requestPojoFile.exists()) { classBuilder.addField(FieldSpec
		 * .builder(ClassName.get(packageName + ".pojo.request",
		 * apiSpecification.getApiName() + "Request"), "requestPojo",
		 * Modifier.PRIVATE) .build()); classBuilder.addMethod(
		 * getRequestPojo(packageName + ".pojo.request",
		 * apiSpecification.getApiName() + "Request")); classBuilder.addMethod(
		 * setRequestPojo(packageName + ".pojo.request",
		 * apiSpecification.getApiName() + "Request")); break; } } }
		 */

		TypeSpec build = classBuilder.addMethod(perform(apiSpecification))
				.addMethod(performWithoutValidation(apiSpecification)).build();
		JavaFile javaFile = JavaFile.builder(packageName, build).build();
		javaFile.writeTo(outputDirectory);
		logger.info("TaskManager.java file created for api: " + apiSpecification.getApiName());
	}

	FieldSpec basePath = FieldSpec.builder(String.class, "apiBasePath").addModifiers(Modifier.PRIVATE)
			.initializer("$S", apiBasePath).build();

	MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
			.addParameter(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger")
			.addStatement("this.$N = $N", "mtLogger", "mtLogger").build();

	/**
	 * This method generate the performWithoutValidation() method for <ApiName>TaskManager to
	 * execute the api without validating the api response.
	 * @param apiSpecification
	 * @return
	 */
	private MethodSpec performWithoutValidation(APISpecification apiSpecification) {
		logger.info("writing executeWithoutValidation method inside TaskManager.java file for api: "
				+ apiSpecification.getApiName());
		return MethodSpec.methodBuilder("performWithoutValidation")
				.addJavadoc("This method execute the api without validating the api resonse.\n\n")
				.addModifiers(Modifier.PUBLIC).returns(Boolean.class)
				//.addParameter(ParameterizedTypeName.get(Map.class, Object.class, Object.class), "inputData")
				//.addParameter(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger")
				//.addStatement("setInputData($N)", "inputData").addStatement("setMtLogger($N)", "mtLogger")
				.addStatement("addTask(new $N($N))", apiSpecification.getApiName() + "CreateRequestTask", "mtLogger")
				.addStatement("addTask(new $N($N))", apiSpecification.getApiName() + "ExecuteRequestTask", "mtLogger")
				.addStatement("execute(this)").addStatement("return true").build();
	}

	/**
	 * This method generate the perform() method for the <ApiName>TaskManager to
	 * execute the api and validating the api response.
	 * @param apiSpecification
	 * @return
	 */
	private MethodSpec perform(APISpecification apiSpecification) {
		logger.info("writing execute method inside TaskManager.java file for api: " + apiSpecification.getApiName());
		return MethodSpec.methodBuilder("perform")
				.addJavadoc("This method execute the api by validating the api resonse.\n\n")
				.addModifiers(Modifier.PUBLIC).returns(Boolean.class)
				//.addParameter(ParameterizedTypeName.get(Map.class, Object.class, Object.class), "inputData")
				//.addParameter(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger")
				//.addStatement("setInputData($N)", "inputData").addStatement("setMtLogger($N)", "mtLogger")
				.addStatement("addTask(new $N($N))", apiSpecification.getApiName() + "CreateRequestTask", "mtLogger")
				.addStatement("addTask(new $N($N))", apiSpecification.getApiName() + "ExecuteRequestTask", "mtLogger")
				.addStatement("addTask(new $N($N))", apiSpecification.getApiName() + "ValidateResponseTask", "mtLogger")
				.addStatement("execute(this)").addStatement("return true").build();
	}

/*	private MethodSpec getResponsePojo(String packageName, String apiName) {
		return MethodSpec.methodBuilder("getResponsePojo").addModifiers(Modifier.PUBLIC)
				.returns(ClassName.get(packageName, apiName)).addStatement("return responsePojo").build();
	}

	private MethodSpec setResponsePojo(String packageName, String apiName) {
		return MethodSpec.methodBuilder("setResponsePojo").addModifiers(Modifier.PUBLIC)
				.addParameter(ClassName.get(packageName, apiName), "responsePojo").returns(void.class)
				.addStatement("this.$N = $N", "responsePojo", "responsePojo").build();
	}*/

	private MethodSpec getRequestPojo() {
		return MethodSpec.methodBuilder("getRequest").addModifiers(Modifier.PUBLIC).returns(Object.class)
				.addStatement("return request").build();
	}

	private MethodSpec setRequestPojo() {
		return MethodSpec.methodBuilder("setRequest").addModifiers(Modifier.PUBLIC)
				.addParameter(Object.class, "request").returns(void.class)
				.addStatement("this.$N = $N", "request", "request").build();
	}

	private MethodSpec getApiBasePath() {
		return MethodSpec.methodBuilder("getApiBasePath").addModifiers(Modifier.PUBLIC).returns(String.class)
				.addStatement("return $N", "apiBasePath").build();
	}

/*	private MethodSpec setApiBasePath() {
		return MethodSpec.methodBuilder("setApiBasePath").addModifiers(Modifier.PUBLIC)
				.addParameter(String.class, "apiBasePath").returns(void.class)
				.addStatement("this.$N = $N", "apiBasePath", "apiBasePath").build();
	}*/

	private MethodSpec getApiPath() {
		return MethodSpec.methodBuilder("getApiPath").addModifiers(Modifier.PUBLIC).returns(String.class)
				.addStatement("return $N", "apiPath").build();
	}

	private MethodSpec setApiPath() {
		return MethodSpec.methodBuilder("setApiPath").addModifiers(Modifier.PUBLIC).returns(void.class)
				.addParameter(String.class, "apiPath").addStatement("this.$N = $N", "apiPath", "apiPath").build();
	}

	private MethodSpec getExpectedStatusCode() {
		return MethodSpec.methodBuilder("getExpectedStatusCode").addModifiers(Modifier.PUBLIC).returns(int.class)
				.addStatement("return $N", "expectedStatusCode").build();
	}

	private MethodSpec getPathParameterList() {
		return MethodSpec.methodBuilder("getPathParameterList").addModifiers(Modifier.PUBLIC)
				.returns(ParameterizedTypeName.get(Map.class, String.class, String.class))
				.addStatement("return $N", "pathParameterList").build();
	}

	private MethodSpec setPathParameterList() {
		return MethodSpec.methodBuilder("setPathParameterList").addModifiers(Modifier.PUBLIC).returns(void.class)
				.addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "pathParameterList")
				.addStatement("this.$N=$N", "pathParameterList", "pathParameterList").build();
	}

	private MethodSpec getQueryParameterList() {
		return MethodSpec.methodBuilder("getQueryParameterList").addModifiers(Modifier.PUBLIC)
				.returns(ParameterizedTypeName.get(Map.class, String.class, String.class))
				.addStatement("return $N", "queryParameterList").build();
	}

	private MethodSpec setQueryParameterList() {
		return MethodSpec.methodBuilder("setQueryParameterList").addModifiers(Modifier.PUBLIC).returns(void.class)
				.addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "queryParameterList")
				.addStatement("this.$N=$N", "queryParameterList", "queryParameterList").build();
	}

	private MethodSpec setExpectedStatusCode(int expectedStatusCode) {
		return MethodSpec.methodBuilder("setExpectedStatusCode").addModifiers(Modifier.PUBLIC)
				.addParameter(int.class, "expectedStatusCode").returns(void.class)
				.addStatement("this.$N = $N", "expectedStatusCode", "expectedStatusCode").build();
	}

/*	private MethodSpec getSoftAssert() {
		return MethodSpec.methodBuilder("getSoftAssert").addModifiers(Modifier.PUBLIC).returns(SoftAssert.class)
				.addStatement("return $N", "softAssert").build();
	}*/

/*	private MethodSpec getMtLogger() {
		return MethodSpec.methodBuilder("getMtLogger").addModifiers(Modifier.PUBLIC)
				.returns(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"))
				.addStatement("return $N", "mtLogger").build();
	}*/

/*	private MethodSpec setSoftAssert() {
		return MethodSpec.methodBuilder("setSoftAssert").addParameter(SoftAssert.class, "softAssert")
				.addModifiers(Modifier.PUBLIC).returns(void.class)
				.addStatement("this.$N=$N", "softAssert", "softAssert").build();
	}*/
/*	private MethodSpec setMtLogger() {
		return MethodSpec.methodBuilder("setMtLogger")
				.addParameter(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger")
				.addModifiers(Modifier.PUBLIC).returns(void.class).addStatement("this.$N=$N", "mtLogger", "mtLogger")
				.build();
	}*/

/*	private MethodSpec getInputData() {
		return MethodSpec.methodBuilder("getInputData").addModifiers(Modifier.PUBLIC)
				.returns(ParameterizedTypeName.get(Map.class, Object.class, Object.class))
				.addStatement("return $N", "inputData").build();
	}*/

	private MethodSpec getBaseUri() {
		return MethodSpec.methodBuilder("getBaseUri").addModifiers(Modifier.PUBLIC).returns(String.class)
				.addStatement("return $N", "baseUri").build();
	}

/*	private MethodSpec getBasePath() {
		return MethodSpec.methodBuilder("getBasePath").addModifiers(Modifier.PUBLIC).returns(String.class)
				.addStatement("return $N", "basePath").build();
	}*/

	private MethodSpec getHeaders() {
		return MethodSpec.methodBuilder("getHeaders").addModifiers(Modifier.PUBLIC)
				.returns(ParameterizedTypeName.get(Map.class, String.class, String.class))
				.addStatement("return $N", "headers").build();
	}

/*	private MethodSpec setInputData() {
		return MethodSpec.methodBuilder("setInputData").addModifiers(Modifier.PUBLIC).returns(void.class)
				.addParameter(ParameterizedTypeName.get(Map.class, Object.class, Object.class), "inputdata")
				.addStatement("this.$N=$N", "inputData", "inputData").build();
	}*/

	private MethodSpec setBaseUri() {
		return MethodSpec.methodBuilder("setBaseUri").addModifiers(Modifier.PUBLIC).returns(void.class)
				.addParameter(String.class, "baseUri").addStatement("this.$N=$N", "baseUri", "baseUri").build();
	}

/*	private MethodSpec setBasePath() {
		return MethodSpec.methodBuilder("setBasePath").addModifiers(Modifier.PUBLIC).returns(void.class)
				.addParameter(String.class, "basePath").addStatement("this.$N=$N", "basePath", "basePath").build();
	}*/

	private MethodSpec setHeaders() {
		return MethodSpec.methodBuilder("setHeaders").addModifiers(Modifier.PUBLIC).returns(void.class)
				.addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "headers")
				.addStatement("this.$N=$N", "headers", "headers").build();
	}

}
