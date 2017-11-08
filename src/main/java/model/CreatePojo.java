package model;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.jsonschema2pojo.SourceType;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CreatePojo extends JsonToPojo {

	static Logger logger = Logger.getLogger(CreatePojo.class);

	public void createResponsePojo(String jsonString, File outputDirectory, String packageName, String className,
			SourceType sourceType) throws IOException {
		createPojoFile(jsonString, outputDirectory, packageName + ".response", className + "Response", sourceType);
		logger.debug("Resonse pojo file created :- " + className);
	}

	public void createRequestPojo(String jsonString, File outputDirectory, String packageName, String className,
			SourceType sourceType) throws IOException {

		createPojoFile(jsonString, outputDirectory, packageName + ".request", className + "Request", sourceType);
		logger.debug("Request pojo file created :- " + className);
	}

	/**
	 * This method will create the POJO file by parsing the given apiSpecifiction object.
	 * @param apiSpecification
	 * @throws IOException
	 */
	public void generatePojoFromApiSpecification(APISpecification apiSpecification, File outputDirectory,
			String packageName, SourceType sourceType) throws IOException {
		String apiName = apiSpecification.getPath();
		String verb = WordUtils.capitalize(apiSpecification.getHttpMethod());
		String[] replaceChar = {"/", "api", "\\{", "\\}", "_", "-"};
		for (String str : replaceChar)
			apiName = apiName.replaceAll(str, "");
		apiName = WordUtils.capitalize(apiName);
		logger.info("Started pojo creation for api :-" + apiName);
		String responseJsonString = apiSpecification.getResponseJson();
		if (responseJsonString != null && !responseJsonString.trim().isEmpty()) {
			if (isJSONValid(responseJsonString)) {
				createResponsePojo(responseJsonString, outputDirectory, packageName, verb + apiName, sourceType);
			}

		}
		List<Parameters> parameters = apiSpecification.getParameters();
		for (Parameters parameter : parameters) {
			if ((parameter.getParameterDataType() != null && parameter.getParameterDataType().contains("Model"))
					|| (parameter.getParamaterType() != null && parameter.getParamaterType().contains("body"))) {
				if (parameter.getModelExample() != null && isJSONValid(parameter.getModelExample())) {
					createRequestPojo(parameter.getModelExample(), outputDirectory, packageName, verb + apiName,
							sourceType);
				}
			}

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

}
