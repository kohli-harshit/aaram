package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import controller.customexceptions.BlankURLException;
import controller.customexceptions.InvalidUrlException;
import model.APIList;
import model.APISpecification;
import model.Parameters;
import model.Section;
import model.Swagger;
import us.codecraft.xsoup.Xsoup;

public class SwaggerParserv1 extends SwaggerParser {
	private Document document;
	static Logger logger = Logger.getLogger(SwaggerParserv1.class);

	public SwaggerParserv1(Document document) throws InvalidUrlException, BlankURLException {
		logger.info("Starting swagger document parsing");
		String mainSectionNameXpath = "//ul[@id='resources']//div[@class='heading']/h2/a";
		Elements elements = Xsoup.compile(mainSectionNameXpath).evaluate(document).getElements();
		if (elements.size() < 1)
			throw new InvalidUrlException(
					"Invalid input URL - Swagger document for input URL doesn't contains any section.");

		this.document = document;
		logger.info("Completed swagger document parsing.");
	}

	/**
	 * This method return the section name from the give confluence page url.
	 * @return
	 * @throws IOException
	 * @throws NullPointerException
	 * @throws InvalidUrlException
	 * @throws BlankURLException
	 */
	private List<String> getAllSectionNamesFromURL() throws  BlankURLException {
		logger.info("fetching section name from swagger document.");
		String mainSectionNameXpath = "//ul[@id='resources']//div[@class='heading']/h2/a";
		List<String> sectionNames = new ArrayList<String>();
		Elements elements = Xsoup.compile(mainSectionNameXpath).evaluate(document).getElements();
		for (Element element : elements) {
			sectionNames.add(element.text());
		}
		return sectionNames;
	}

	/**
	 * This method reqtuen the details of all sections available on swagger
	 * document.
	 * @return
	 * @throws InvalidUrlException
	 * @throws BlankURLException
	 */
	public Swagger getSwaggerDetails() throws InvalidUrlException, BlankURLException {
		logger.info("Inside SwaggerParserv1.java - start fetching swagger details");
		List<Section> sectionList = new ArrayList<Section>();
		Swagger swagger = new Swagger();
		swagger.setTitle(getDocumentTitle());
		List<String> sectionNamesFromURL = getAllSectionNamesFromURL();
		for (String sectionName : sectionNamesFromURL) {
			sectionList.add(getSectionDetails(sectionName.toLowerCase()));
		}
		swagger.setSectionList(sectionList);
		return swagger;

	}

	/**
	 * This method return the title of swagger document.
	 * @return
	 */
	private String getDocumentTitle() {
		logger.info("Found swagger doc title.");
		String docTitleXpath = "//div[@class='info_title']";
		return Xsoup.compile(docTitleXpath).evaluate(document).getElements().text();
	}

	/**
	 * This method will return the detail for given section name.
	 * @param sectionName
	 * @return
	 */
	private Section getSectionDetails(String sectionName) {
		logger.info("start fetching details for section:- " + sectionName);
		String entireSectionxpath = "//li[@class='resource']";
		String onlySectionNameTextXpath = "//div/h2/a/text()";
		String apiDom = null;
		Elements xElements = Xsoup.compile(entireSectionxpath).evaluate(document).getElements();
		for (Element element : xElements) {
			String name = Xsoup.compile(onlySectionNameTextXpath).evaluate(element).get();
			if (name.trim().equalsIgnoreCase(sectionName)) {
				apiDom = element.toString();
				break;
			}
		}
		Section section = new Section();
		section.setSectionName(sectionName.replaceAll(" ", ""));
		section.setSectionDetail(apiDom);
		APIList setAPIList = setAPIList(section.getSectionDetail(), section);
		section.setApiList(setAPIList);
		logger.info("Found all details for section:- " + sectionName);
		return section;
	}

	/**
	 * This method fetch the api details for given section name.
	 * @param element
	 * @param sections
	 * @return
	 */
	private APIList setAPIList(String element, Section sections) {
		String apiListInsideSectionXpath = "//ul[@class='endpoints']/li[@class='endpoint']";
		logger.info("Start fetching details about all apis available inside section.");
		List<APISpecification> apiSpecifications = new ArrayList<APISpecification>();
		APIList apiList = new APIList();
		Document parse = Jsoup.parse(element);
		List<String> list = Xsoup.compile(apiListInsideSectionXpath).evaluate(parse).list();
		for (String api : list) {
			apiSpecifications.add(setAPISpecifications(api));
		}
		apiList.setApiList(apiSpecifications);
		logger.info("fetched details about all apis available inside section.");
		return apiList;
	}

	/**
	 * This method return the api details about given apiElement.
	 * @param apiElement
	 *            - parsed html object for given api.
	 * @return
	 */
	private APISpecification setAPISpecifications(String apiElement) {
		String jsonElementText = null;
		String apiVerbXpath = "//span[@class='http_method']/a/text()";
		String apiPathXpath = "//span[@class='path']/a/text()";
		String apiResponseJsonXpath = "//div[@class='response-class']//div[@class='snippet_json']/pre/code";

		Document element = Jsoup.parse(apiElement, "", Parser.xmlParser());
		APISpecification apiSpecification = new APISpecification();
		String apiVerb = WordUtils.capitalize(Xsoup.compile(apiVerbXpath).evaluate(element).get());
		String apiPath = WordUtils.capitalize(Xsoup.compile(apiPathXpath).evaluate(element).get());
		apiSpecification.setHttpMethod(apiVerb);
		apiSpecification.setPath(apiPath);
		String complexJson = Xsoup.compile(apiResponseJsonXpath).evaluate(element).get();
		if (null != complexJson) {
			jsonElementText = complexJson.replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", "");
		}
		apiSpecification.setResponseJson(jsonElementText);
		apiSpecification.setParameters(setParameterList(apiElement));

		String[] replaceChar = {"/", "api", "\\{", "\\}"};
		for (String str : replaceChar)
			apiPath = apiPath.replaceAll(str, "");
		apiSpecification.setApiName(apiVerb.trim() + apiPath.trim());
		logger.info("fetched all deatils about api : " + apiSpecification.getApiName());
		return apiSpecification;

	}

	/**
	 * This method return the parameter details about an apiElement.
	 * @param apiElement
	 *            - parsed html object for given api
	 * @return
	 */
	private List<Parameters> setParameterList(String apiElement) {
		String parameterListXpath = "//table[@class='fullwidth parameters']/tbody/tr";
		String parameterNameXpath = "//td[1]/label";
		String parameterValueXpath = "//td[2]/input/@value";
		String parameterTypeXpath = "//td[4]/text()";
		String paramterDataTypeXpath = "//td[5]/span/text()";
		String parameterRequestJsonXpath = "//td//div[@class='snippet_json']/pre/code";
		String jsonElemText = null;

		List<Parameters> parameterList = new ArrayList<Parameters>();
		Document element = Jsoup.parse(apiElement);
		List<String> paramList = Xsoup.compile(parameterListXpath).evaluate(element).list();
		Parameters parameters;
		for (String param : paramList) {
			parameters = new Parameters();
			Document paramElm = Jsoup.parse(param, "", Parser.xmlParser());
			parameters.setParameterName(Xsoup.compile(parameterNameXpath).evaluate(paramElm).getElements().text());
			parameters.setParameterValue(Xsoup.compile(parameterValueXpath).evaluate(paramElm).get());
			parameters.setParamaterType(Xsoup.compile(parameterTypeXpath).evaluate(paramElm).get());
			String paramDataType = Xsoup.compile(paramterDataTypeXpath).evaluate(paramElm).get();
			if (paramDataType != null && !paramDataType.trim().isEmpty()) {
				parameters.setParameterDataType(Xsoup.compile(paramterDataTypeXpath).evaluate(paramElm).get());
			} else {
				parameters.setParameterDataType("Model");
				String complexJson = Xsoup.compile(parameterRequestJsonXpath).evaluate(paramElm).get();
				if (null != complexJson) {
				}
				jsonElemText = complexJson.replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", "");
				parameters.setModelExample(jsonElemText);
			}

			parameterList.add(parameters);
		}
		logger.info("Parsed all parameter details about api.");
		return parameterList;
	}

	/**
	 * Parse the swagger document for given section name and generate the POJO
	 * files for the api.
	 * @param sectionName
	 * @throws IOException
	 * @throws FailingHttpStatusCodeException
	 * @throws InvalidUrlException
	 */
	public void parseSection(SwaggerParserv1 swaggerParserv1, String sectionName, String outputDirectory,
			String packageName) throws InvalidUrlException, IOException {
		swaggerParserv1.getSectionDetails(sectionName);
	}
}
