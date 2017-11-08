package controller;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import controller.customexceptions.InvalidUrlException;
import javafx.scene.control.Alert;

public final class URLValidator {

	static Logger logger = Logger.getLogger(URLValidator.class);

	/**
	 * This method validate that whether entered url is a valid URL or not.
	 * 
	 * @param linkUrl
	 * @return
	 */
	public static boolean isValidUrl(String linkUrl) throws InvalidUrlException, UnknownHostException, IOException {
		logger.info("Inside URLValidator class to validate the URL.");
		boolean flag = false;
		URL url = new URL(linkUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.connect();
		int responseCode = connection.getResponseCode();
		if (responseCode >= 200 && responseCode < 400)
			flag = true;
		else
			throw new InvalidUrlException("Entered URL is not a valid URL:" + linkUrl);
		return flag;
	}

}
