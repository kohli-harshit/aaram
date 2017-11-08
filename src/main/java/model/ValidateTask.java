package model;

import java.io.File;
import java.io.IOException;

import javax.lang.model.element.Modifier;

import org.apache.log4j.Logger;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

public class ValidateTask {

	static Logger logger = Logger.getLogger(ValidateTask.class);

	/**
	 * This method generates a <ApiName>ValidateResponseTask.java file required
	 * to validate the api response, where <ApiName> is name of api passed in
	 * apiSpecification object.
	 * @param apiSpecification
	 *            - apiSpecification object for api details,
	 * @param packageName
	 *            - package name for the java file
	 * @param outputDirectory
	 *            - file path to store the output
	 *            <ApiName>ValidateResponseTask.java file
	 * @throws IOException
	 */
	public void create(APISpecification apiSpecification, String packageName, File outputDirectory) throws IOException {
		logger.info("Start creating ValidateResponseTask.java file for api: " + apiSpecification.getApiName());
		TypeSpec build = TypeSpec.classBuilder(apiSpecification.getApiName() + "ValidateResponseTask")
				.superclass(ParameterizedTypeName.get(ClassName.get("com.monotype.api_utils.core", "Task"),
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager")))
				.addField(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger",
						Modifier.PRIVATE)
				.addMethod(constructor).addModifiers(Modifier.PUBLIC)
				.addJavadoc("This class validates the api response for given api.\n")
				.addMethod(createPerform(apiSpecification, packageName)).build();
		JavaFile javaFile = JavaFile.builder(packageName, build).build();
		javaFile.writeTo(outputDirectory);
		logger.info("ValidateResponseTask.java file created for api: " + apiSpecification.getApiName());

	}

	MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
			.addParameter(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger")
			.addStatement("this.$N = $N", "mtLogger", "mtLogger").build();

	/**
	 * This method generateds the perform(<TaskManager> taskManager) method
	 * required for <ApiName>ValidateResponseTask.java file.
	 * @param apiSpecification
	 *            - apiSpecification object for api details,
	 * @param packageName
	 *            - package name for the java file
	 * @return
	 */
	private MethodSpec createPerform(APISpecification apiSpecification, String packageName) {
		logger.info("writing perform method inside ValidateResponseTask.java file for api: "
				+ apiSpecification.getApiName());
		return MethodSpec.methodBuilder("perform")
				.addJavadoc("This method perform the validateRequestTask action. \n"
						+ "User have to update the status code, if needed.\n\n"
						+ "@param taskManager - apiTaskManager instance of given api.\n")
				.returns(void.class).addModifiers(Modifier.PUBLIC)
				.addParameter(ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"), "taskManager")
				.addAnnotation(Override.class)
				.addStatement("taskManager.getRestDriver().getobjMTResponseBuilder().setStatusCode($N)",
						"taskManager.getExpectedStatusCode()")
				.addStatement(
						"taskManager.getRestDriver().createExpectedResponse(taskManager.getRestDriver().getobjMTResponseBuilder())")
				.addStatement("taskManager.getRestDriver().validateResponse()")
				.addStatement("$N.log($T.INFO,\"API response is validated successfully\")",
						"mtLogger", ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"))
				.build();
	}

}
