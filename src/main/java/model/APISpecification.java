package model;

import java.util.ArrayList;
import java.util.List;

public class APISpecification {
	private String responseJson;
	private List<Parameters> parameters = new ArrayList<Parameters>();
	private String responseContentsType;
	private String httpMethod;
	private String path;
	private String apiName;

	public String getApiName() {
		return apiName;
	}

	public void setApiName(String apiName) {
		this.apiName = apiName;
	}

	public String getResponseJson() {
		return responseJson;
	}

	public void setResponseJson(String responseJson) {
		this.responseJson = responseJson;
	}

	public List<Parameters> getParameters() {
		return parameters;
	}

	public void setParameters(List<Parameters> parameters) {
		this.parameters = parameters;
	}

	public String getResponseContentsType() {
		return responseContentsType;
	}

	public void setResponseContentsType(String responseContentsType) {
		this.responseContentsType = responseContentsType;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
