package model;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.apache.log4j.Logger;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

public class ExecuteTask {

	static Logger logger = Logger.getLogger(ExecuteTask.class);
	/**
	 * This method generate a <ApiName>ExecuteRequestTask.java file.
	 * @param apiSpecification
	 *            - apiSpecification instance with api details
	 * @param packageName
	 *            - package name for the created .java file
	 * @param outputDirecctory
	 *            - file path to write the .java file
	 * @throws IOException
	 */
	public void create(APISpecification apiSpecification, String packageName, File outputDirecctory)
			throws IOException {
		logger.info("Start creating ExecuteRequestTask.java file for api: " + apiSpecification.getApiName());
		TypeSpec build = TypeSpec.classBuilder(apiSpecification.getApiName() + "ExecuteRequestTask")
				.superclass(ParameterizedTypeName.get(ClassName.get("com.monotype.api_utils.core", "Task"),
						ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager")))
				.addJavadoc("This class creates the exceute api request for given api.\n").addModifiers(Modifier.PUBLIC)
				.addField(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger",
						Modifier.PRIVATE)
				.addMethod(constructor).addMethod(createPerform(apiSpecification, packageName)).build();
		JavaFile javaFile = JavaFile.builder(packageName, build).build();
		javaFile.writeTo(outputDirecctory);
		logger.info("ExecuteRequestTask.java file created for api: " + apiSpecification.getApiName());

	}

	MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
			.addParameter(ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "MTLogger"), "mtLogger")
			.addStatement("this.$N = $N", "mtLogger", "mtLogger").build();

	/**
	 * This method generates perform(<ApiName>TaskManager taskManager) which will be performing api execution action.
	 * @param apiSpecification
	 *            - apiSpecification instance with api details
	 * @param packageName
	 *            - package name for the created .java file
	 * @return
	 */
	private MethodSpec createPerform(APISpecification apiSpecification, String packageName) {
		logger.info("writing perform method inside ExecuteRequestTask.java file for api: "+apiSpecification.getApiName());
		Builder builder = MethodSpec.methodBuilder("perform").returns(void.class)
				.addJavadoc("This method perform action to executes the api request. \n\n"
						+ "@param taskManager - taskManager instance of given api.\n")
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ClassName.get(packageName, apiSpecification.getApiName() + "TaskManager"), "taskManager")
				.addAnnotation(Override.class);
		List<Parameters> parameters = apiSpecification.getParameters();
		boolean flag = false;
		for (Parameters param : parameters) {
			if (param.getParamaterType()!=null && param.getParamaterType().equals("path"))
				flag = true;
		}
		if (flag)
			builder.addStatement(
					"taskManager.getRestDriver().getRequestObj().executeRequest(taskManager.getRestDriver(), $N)",
					"taskManager.getApiPath()");
		else
			builder.addStatement(
					"taskManager.getRestDriver().getRequestObj().executeRequest(taskManager.getRestDriver())");
		builder.addStatement(
				"$N.log($T.INFO,\"API response code is :\"+taskManager.getRestDriver().getStatusCode())",
				"mtLogger",ClassName.get("com.monotype.api_utils.logger.extentreportlogger", "LogStatus"));

		return builder.build();
	}

}
