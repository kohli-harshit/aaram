package controller;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;

import controller.customexceptions.BlankURLException;
import controller.customexceptions.InvalidUrlException;
import model.Swagger;

public abstract class SwaggerParser {
	private static String swaggerVersion;

	public static String getSwaggerVersion() {
		return swaggerVersion;
	}

	public static void setSwaggerVersion(String swaggerVersion) {
		SwaggerParser.swaggerVersion = swaggerVersion;
	}

	static Logger logger = Logger.getLogger(SwaggerParser.class);

	public abstract Swagger getSwaggerDetails() throws Exception;

	public static SwaggerParser getSwaggerParser(String url, String pageName)
			throws InvalidUrlException, BlankURLException, IOException {
		SwaggerParser swaggerParser = null;
		Document htmlDocument = HTMLUtility.getHTMLDocument(url, pageName);
		String version = HTMLUtility.getSwaggerVersion(htmlDocument);
		setSwaggerVersion(version);
		if (version.equalsIgnoreCase("v1") || version.startsWith("1")) {
			swaggerParser = new SwaggerParserv1(htmlDocument);
		} else if (version.equalsIgnoreCase("v2") || version.equalsIgnoreCase("swaggerHub")
				|| version.startsWith("2")) {
			swaggerParser = new SwaggerParserv2(htmlDocument, url);
		} else {
			logger.fatal("Swagger version  not supported");
			throw new InvalidUrlException("InvalidURLException - Swagger version  not supported.");
		}
		return swaggerParser;
	}

}
