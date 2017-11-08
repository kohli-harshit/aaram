package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.WordUtils;
import org.apache.commons.validator.UrlValidator;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.jayway.jsonpath.JsonPath;

import controller.customexceptions.BlankURLException;
import controller.customexceptions.InvalidUrlException;
import model.APIList;
import model.APISpecification;
import model.Parameters;
import model.Section;
import model.Swagger;
import us.codecraft.xsoup.Xsoup;

public class SwaggerParserv2 extends SwaggerParser {
	public static String jsonString;

	public static String getJsonString() {
		return jsonString;
	}

	JSONParser jsonParser = new JSONParser();
	JSONObject swaggerDoc = new JSONObject();
	static Logger logger = Logger.getLogger(SwaggerParserv2.class);

	public SwaggerParserv2(Document document, String url) throws InvalidUrlException, BlankURLException, IOException {
		logger.info("Starting swagger document parsing");
		String mainSectionNameXpath = "//h4[contains(@class,'opblock-tag')]/a/span";
		String swaggerHubDownloadJsonXpath = "//ul[@aria-labelledby='api-download-menu']/li[@role='presentation']/a";
		String swaggerJsonFromDoc = "//div[@id='swagger-options']/text()";
		String swaggerOptionJsonLinkCSS = "div#swagger-options";
		String swaggerJsonLinkCSS = "span.url";
		Elements elements = Xsoup.compile(mainSectionNameXpath).evaluate(document).getElements();
		if (elements.size() < 1) {
			throw new InvalidUrlException(
					"Invalid input URL - Swagger document for input URL doesn't contains any section.");
		}
		logger.info("Starting swagger document parsing");
		if (document.select(swaggerOptionJsonLinkCSS).size() > 0) {
			this.jsonString = Xsoup.compile(swaggerJsonFromDoc).evaluate(document).get();
		} else if (Xsoup.compile(swaggerHubDownloadJsonXpath).evaluate(document).getElements().size() > 0) {
			String href = Xsoup.compile(swaggerHubDownloadJsonXpath).evaluate(document).getElements().attr("href");
			String swaggerJsonUrl = generateSwaggerJsonURL(url, href);
			this.jsonString = HTMLUtility.getSwaggerJsonFromDownloadUrl(swaggerJsonUrl);
		} else if (document.select(swaggerJsonLinkCSS).size() > 0) {
			url = generateSwaggerJsonURL(url, document.select(swaggerJsonLinkCSS).text().trim());
			// this.jsonString = HTMLUtility.getSwaggerJsonFromUrl(url);
			this.jsonString = getPageSource(url);
		} else
			throw new InvalidUrlException("Invalid URL - Swagger document is not supported");
		logger.info("Completed swagger document parsing.");
	}

	public void parseJson() throws Exception {
		logger.info("Starting swagger document parsing");
		JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);
		swaggerDoc = (JSONObject) jsonObject.get("swaggerDoc");
		if (swaggerDoc == null || swaggerDoc.isEmpty())
			swaggerDoc = jsonObject;
		logger.info("Completed swagger doc node.");
	}

	/**
	 * This method return the details of all sections available on swagger
	 * document.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Swagger getSwaggerDetails() throws Exception {
		logger.info("Inside SwaggerParser.java - start fetching swagger details");
		List<Section> sectionList = new ArrayList<Section>();
		Swagger swagger = new Swagger();
		parseJson();
		swagger.setTitle(getDocumentTitle());
		swagger.setSectionList(getSectionDetails());
		System.out.println("completed");
		return swagger;
	}

	/**
	 * This method used to get the details of all sections available on swagger
	 * document.
	 * 
	 * @return
	 * @throws Exception
	 */
	private List<Section> getSectionDetails() throws Exception {

		String apiPath = "$.paths";
		HashMap<String, Section> sections = new HashMap<>();
		// Get all API's
		JSONObject apis = JsonPath.read(swaggerDoc, apiPath);
		Set<String> basePath = apis.keySet();
		for (String path : basePath) {
			JSONObject api = (JSONObject) apis.get(path);
			addApiToSection(sections, api, path);
		}
		return new ArrayList<>(sections.values());
	}

	/**
	 * This method used to add API to APIList of sections available on swagger
	 * document.
	 * 
	 * @return
	 * @throws Exception
	 */
	private boolean addApiToSection(HashMap<String, Section> sections, JSONObject apis, String path) throws Exception {
		Set<String> verbs = apis.keySet();

		for (String verb : verbs) {

			JSONObject properties = (JSONObject) apis.get(verb);
			String sectionName = JsonPath.read(properties, "$.tags[0]").toString().toLowerCase()
					.replaceAll("-|_|\\s|\\{|\\}|/", "");
			APISpecification apiSpecification = getAPISpecification(properties, verb, path);
			Section section;
			// Add section if not present
			if (!sections.containsKey(sectionName)) {
				section = new Section();
				sections.put(sectionName, section);
				APIList apiList = new APIList();
				apiList.setApiList(Arrays.asList(apiSpecification));
				section.setApiList(apiList);
			} else {
				section = sections.get(sectionName);
				List<APISpecification> apiSpecifications = new ArrayList<>(section.getApiList().getApiList());
				apiSpecifications.add(apiSpecification);
				section.getApiList().setApiList(apiSpecifications);
			}
			section.setSectionName(sectionName.replaceAll(" ", ""));
		}
		return true;

	}

	/**
	 * This method return the details of all sections available on swagger
	 * document.
	 * 
	 * @return
	 * @throws Exception
	 */
	private APISpecification getAPISpecification(JSONObject properties, String verb, String basePath) throws Exception {
		APISpecification apiSpecification = new APISpecification();
		apiSpecification.setHttpMethod(verb);
		apiSpecification.setPath(basePath);
		String[] replaceChar = { "/", "api", "\\{", "\\}", "-", "_" };
		String tempBasePath = basePath;
		for (String str : replaceChar)
			tempBasePath = tempBasePath.replaceAll(str, "");
		String tempVerb = WordUtils.capitalize(verb);
		apiSpecification.setApiName((tempVerb.trim() + WordUtils.capitalize(tempBasePath.trim())).replaceAll("/", ""));
		if (properties.containsKey("produces"))
			apiSpecification.setResponseContentsType(properties.get("produces").toString());
		JSONObject definitions = new JSONObject();
		if (swaggerDoc.containsKey("definitions"))
			definitions = JsonPath.read(swaggerDoc, "$.definitions");
		JSONArray parametersFromSwagger;
		if (properties.containsKey("parameters")) {
			parametersFromSwagger = JsonPath.read(properties, "$.parameters");
			Iterator iterator = parametersFromSwagger.iterator();
			List<Parameters> parameters = new ArrayList<>();
			while (iterator.hasNext()) {
				JSONObject jsonObject = (JSONObject) iterator.next();
				if(jsonObject.containsKey("$ref")) {
					jsonObject = (JSONObject) JsonPath.read(swaggerDoc,
							"$" + jsonObject.get("$ref").toString().replace("#", "").replace("/", "."));
				}
				Parameters parameter = new Parameters();
				for (Object keys : jsonObject.keySet()) {
					if (keys.toString().equals("in")) {
						parameter.setParamaterType(jsonObject.get(keys).toString());
					}
					if (keys.toString().equals("type"))
						parameter.setParameterDataType(jsonObject.get("type").toString());
					if (keys.toString().equals("description"))
						parameter.setParameterDescritption(jsonObject.get("description").toString());
					if (keys.toString().equals("name"))
						parameter.setParameterName(jsonObject.get("name").toString());
					if (keys.toString().equalsIgnoreCase("schema")) {
						JSONObject requestSchema = (JSONObject) jsonObject.get("schema");
						if (requestSchema.containsKey("properties")) {
							requestSchema = (JSONObject) requestSchema.get("properties");
						}
						if (requestSchema.containsKey("$ref")) {
							requestSchema = (JSONObject) JsonPath.read(swaggerDoc,
									"$" + requestSchema.get("$ref").toString().replace("#", "").replace("/", "."));
							if (requestSchema.containsKey("properties"))
								requestSchema = (JSONObject) requestSchema.get("properties");
						}
						JSONObject requestModel = new JSONObject();
						requestModel.put("definitions", definitions);
						requestModel.put("properties", requestSchema);
						parameter.setModelExample(requestModel.toJSONString());
					}
					if(keys.toString().contains("ref")){

					}
				}
				parameters.add(parameter);
			}
			apiSpecification.setParameters(parameters);
		}
		if (properties.containsKey("responses")) {
			JSONObject responses = JsonPath.read(properties, "$.responses");
			Set<String> keys = responses.keySet();
			JSONObject responseSchema;
			JSONObject responseModel = new JSONObject();

			for (String key : keys) {
				if (key.startsWith("20") && ((JSONObject) responses.get(key)).containsKey("schema")) {
					responseSchema = (JSONObject) ((JSONObject) responses.get(key)).get("schema");
					if(!(responseSchema.containsKey("type") && responseSchema.get("type").toString().equalsIgnoreCase("string"))) {
                        if (responseSchema.containsKey("properties")) {
                            responseSchema = JsonPath.read(responseSchema, "$.properties");
                        }
                        if (responseSchema.containsKey("$ref")) {
                            responseSchema = (JSONObject) JsonPath.read(swaggerDoc,
                                    "$" + responseSchema.get("$ref").toString().replace("#", "").replace("/", "."));
                            if (responseSchema.containsKey("properties"))
                                responseSchema = (JSONObject) responseSchema.get("properties");
                        }
                        responseModel.put("definitions", definitions);
                        responseModel.put("properties", responseSchema);
                        apiSpecification.setResponseJson(responseModel.toJSONString());
                    }
				} else if (key.startsWith("20") && ((JSONObject) responses.get(key)).containsKey("$ref")) {
					responseSchema = (JSONObject) responses.get(key);
					responseSchema = (JSONObject) JsonPath.read(swaggerDoc,
							"$" + responseSchema.get("$ref").toString().replace("#", "").replace("/", "."));
					if(responseSchema.containsKey("schema"))
						responseSchema = (JSONObject) responseSchema.get("schema");
					if (responseSchema.containsKey("properties"))
						responseSchema = (JSONObject) responseSchema.get("properties");
					responseModel.put("definitions", definitions);
					responseModel.put("properties", responseSchema);
					apiSpecification.setResponseJson(responseModel.toJSONString());
				}
				break;
			}
		}
		return apiSpecification;
	}

	/**
	 * This method return the title of swagger document.
	 * 
	 * @return
	 */
	private String getDocumentTitle() {

		logger.info("Found swagger doc title.");
		String titlePath = "$.info.title";
		String json = JsonPath.read(swaggerDoc, titlePath);
		return json;
	}

	/**
	 * This method return the section name from the give swagger page url.
	 * 
	 * @return
	 * @throws InvalidUrlException
	 * @throws BlankURLException
	 */
	private List<String> getAllSectionNamesFromJson() throws InvalidUrlException, BlankURLException {
		logger.info("fetching section name from swagger document.");
		String sectionPath = "$.paths..tags[0]";
		List<String> sections = JsonPath.read(swaggerDoc, sectionPath);
		Set<String> uniqueSections = new HashSet<>(sections);
		return new ArrayList<>(uniqueSections);
	}

	/**
	 * This method compose a url to get the swagger json file.
	 * 
	 * @param baseUrl
	 * @param relativeUrl
	 * @return
	 * @throws MalformedURLException
	 */
	private String generateSwaggerJsonURL(String baseUrl, String relativeUrl) throws MalformedURLException {
		if(isValidUrl(relativeUrl))
			return relativeUrl;
		URL url = new URL(baseUrl);
		String hostName = url.getHost();
		String protocol = url.getProtocol();
		int port = url.getPort();
		String portNumber = "";
		if (port > 0)
			portNumber = ":" + port;
		String swaggerUrl = protocol + "://" + hostName + portNumber + relativeUrl;
		return swaggerUrl;
	}

	private String getPageSource(String pageUrl) throws IOException {
		URL url = new URL(pageUrl);
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

		StringBuffer pageContent = new StringBuffer();
		String text;
		while ((text = in.readLine()) != null)
			pageContent.append(text);

		in.close();
		return pageContent.toString();

	}
	
	public static boolean isValidUrl(String inputUrl){
		UrlValidator validUrl = new UrlValidator();
		return validUrl.isValid(inputUrl);
	}

}
