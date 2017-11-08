package model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import javax.lang.model.element.Modifier;

import org.apache.log4j.Logger;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

public class CreateTask {

	static Logger logger = Logger.getLogger(CreateTask.class);
	String outputDirectory = "";

	/**
	 * This method generate the <ApiName>CreateRequestTask.java file.
	 * @param apiSpecification
	 *            - apiSpecification instance for api details.
	 * @param packageName
	 *            - package of returned java file.
	 * @param outputDirectory
	 *            - file path to write the .java file.
	 * @throws IOException
	 */
	public void create(APISpecification apiSpecification, String packageName, File outputDirectory) throws IOException {
		logger.info("Start creating CreateRequestTask.java file for api: " + apiSpecification.getApiName());
		this.outputDirectory = outputDirectory.toString();
		TypeSpec build = TypeSpec.classBuilder(apiSpecification.getApiName() + "CreateRequestTask")
				.addJavadoc("This class creates the request for given api.\n").addModifiers(Modifier.PUBLIC)
				.superclass(ParameterizedTypeName.get(ClassName.get("com.monotype.api_utils.core", "Task"),
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager")))
				.addField(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger",
						Modifier.PRIVATE)
				.addMethod(constructor).addMethod(createPerform(apiSpecification, packageName)).build();
		JavaFile javaFile = JavaFile.builder(packageName, build).build();
		javaFile.writeTo(outputDirectory);
		logger.info("CreateRequestTask.java file created for api: " + apiSpecification.getApiName());

	}

	MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
			.addParameter(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger")
			.addStatement("this.$N = $N", "mtLogger", "mtLogger").build();

	/**
	 * This method generates the perform() method of <ApiName>CreateRequestTask class.
	 * @param apiSpecification
	 *            - apiSpecification instance for api details.
	 * @param packageName
	 *            - package of returned java file.
	 * @return
	 */
	private MethodSpec createPerform(APISpecification apiSpecification, String packageName) {
		String apiVerb = apiSpecification.getHttpMethod().toUpperCase();
		logger.info(
				"writing perform method inside CreateRequestTask.java file for api: " + apiSpecification.getApiName());
		Builder builder = MethodSpec.methodBuilder("perform")
				.addJavadoc("This method perform the createRequestTask action. \n"
						+ "Some attirbutes for request are pre-defined like RequestContentType, RequestType, BaseUri, BasePath are set.\n"
						+ "User has to set the other attibutes for request like request body, if needed.\n\n"
						+ "@param taskManager - apiTaskManager instance of given api.\n")
				.returns(void.class).addModifiers(Modifier.PUBLIC)
				.addParameter(ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"), "taskManager")
				.addAnnotation(Override.class)
				.addStatement(
						"taskManager.getRestDriver().getobjMTRequestBuilder().setContentType($T.RequestContentType.JSON)",
						ClassName.get("com.monotype.api_utils.core.restman", "MTRequestBuilder"))
				.addStatement("taskManager.getRestDriver().getobjMTRequestBuilder().setRequestType($T.$N)",
						ClassName.get("com.monotype.api_utils.core.requestexecutor", "RequestType"), apiVerb)
				.addStatement("taskManager.getRestDriver().getobjMTRequestBuilder().setBaseURI($N)",
						"taskManager.getBaseUri()")
				.addStatement("taskManager.getRestDriver().getobjMTRequestBuilder().setBasePath($N)",
						"taskManager.getApiBasePath()");
		List<Parameters> parameters = apiSpecification.getParameters();
		boolean pathParameterExist = false, queryParameterExist = false, headerExist = false;
		for (Parameters param : parameters) {
			if (param.getParamaterType() != null && param.getParamaterType().equals("path"))
				pathParameterExist = true;
			if (param.getParamaterType() != null && param.getParamaterType().equals("query"))
				queryParameterExist = true;
			if (param.getParamaterType() != null && param.getParamaterType().equals("header"))
				headerExist = true;
		}

//		if (headerExist) {
			builder.beginControlFlow("for($T header : $N",
					ParameterizedTypeName.get(Entry.class, String.class, String.class),
					"taskManager.getHeaders().entrySet())");
			builder.addStatement("taskManager.getRestDriver().getobjMTRequestBuilder().setHeader($N, $N)",
					"header.getKey()", "header.getValue()");
			builder.endControlFlow();
	//	}

		if (pathParameterExist) {
			builder.beginControlFlow("for($T pathParameter : $N",
					ParameterizedTypeName.get(Entry.class, String.class, String.class),
					"taskManager.getPathParameterList().entrySet())");
			builder.addStatement("taskManager.getRestDriver().getobjMTRequestBuilder().setPathParameters($N, $N)",
					"pathParameter.getKey()", "pathParameter.getValue()");
			builder.endControlFlow();
		}

		if (queryParameterExist) {
			builder.beginControlFlow("for($T queryParameter : $N",
					ParameterizedTypeName.get(Entry.class, String.class, String.class),
					"taskManager.getQueryParameterList().entrySet())");
			builder.addStatement("taskManager.getRestDriver().getobjMTRequestBuilder().setQueryParameters($N, $N)",
					"queryParameter.getKey()", "queryParameter.getValue()");
			builder.endControlFlow();
		}

		for (Parameters parameter : parameters) {
			if (parameter.getParamaterType() != null && parameter.getParamaterType().equals("body")) {
				String pojoLocation = outputDirectory + "/" + packageName + "/pojo/request/";
				File requestPojoFile = new File(pojoLocation.replaceAll("\\.", "/"),
						apiSpecification.getApiName() + "Request.java");
				if (requestPojoFile.exists()) {
					builder.addStatement("taskManager.getRestDriver().getobjMTRequestBuilder().setReqBody($N)",
							"taskManager.getRequest()");
					break;
				}
			}
		}

		builder.addStatement(
				"taskManager.getRestDriver().createRequest(taskManager.getRestDriver().getobjMTRequestBuilder())");

		return builder.build();
	}

}
