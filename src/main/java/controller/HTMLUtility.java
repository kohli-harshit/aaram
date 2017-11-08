package controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;

import controller.customexceptions.BlankURLException;
import controller.customexceptions.InvalidUrlException;
import edu.umd.cs.findbugs.util.HTML;
import us.codecraft.xsoup.Xsoup;

public final class HTMLUtility {

	public static Document htmlDocument;

	static Logger logger = Logger.getLogger(HTMLUtility.class);

	private HTMLUtility() {
		// TODO Auto-generated constructor stub
	}

	public static Document getHTMLDocument(String url, String pageName) throws BlankURLException {
		if (url.trim().equals("")) {
			logger.error("given swagger document Url is blank");
			throw new BlankURLException("Input URL is blank.");
		}
		String titlePartialMatchXpath = "//*[@class='info_title' or @class='title' and contains(text(), '";
		String titleXpath = "//*[@class='info_title' or @class='title']";
		String swaggerHubDownloadButtonXpath = "//i[@class='glyphicon glyphicon-download-alt']";
		String selectPageDropdownXpath ="//select[@id='input_baseUrl' or @id='select']";
		String exploreButtonXpath = "//a[@id='explore']";
		
		WebDriver driver = null;
		try {
			String osName = System.getProperty("os.name").toLowerCase();
			if(osName.contains("mac"))
				System.setProperty("phantomjs.binary.path", "src/main/resources/mac/phantomjs");
			else
				System.setProperty("phantomjs.binary.path", "src/main/resources/phantomjs.exe");

			driver = new PhantomJSDriver();
			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			
			driver.get(url);
			HTMLUtility.waitForElemenentPresence(driver, By.xpath(titleXpath));
			if(driver.findElements(By.xpath("//img[@class='topbar-logo-img'][@alt='Swagger Hub']")).size()>0)
				HTMLUtility.waitForElemenentPresence(driver, By.xpath(swaggerHubDownloadButtonXpath));
			Document parse = null;
			if(!(pageName.isEmpty() || pageName == null))
			{
					new Select(driver.findElement(By.xpath(selectPageDropdownXpath))).selectByVisibleText(pageName);
					if(isElementPresent(driver, By.xpath(exploreButtonXpath), 5))
						driver.findElement(By.xpath(exploreButtonXpath)).click();
					//HTMLUtility.waitForElemenentPresence(driver, By.xpath(titlePartialMatchXpath+pageName+"')]"));
					HTMLUtility.waitForElemenentPresence(driver, By.xpath(titleXpath));
					parse = Jsoup.parse(driver.getPageSource());
			}
			else if(pageName.isEmpty())
				parse = Jsoup.parse(driver.getPageSource());

			return parse;
		} finally {
			driver.close();
		}
	}

	/**
	 * This method return list of multiple pageNames(if entered swagger url contains pages init). 
	 * @param apiDocUrl
	 * @return
	 * @throws BlankURLException
	 * @throws InvalidUrlException 
	 */
	public static List<String> getPagesFromApiDoc(String apiDocUrl) throws BlankURLException, InvalidUrlException {
		if (apiDocUrl.trim().equals("")) {
			logger.error("given swagger document Url is blank");
			throw new BlankURLException("Input URL is blank.");
		}
		List<String> pagesName = new ArrayList<>();
		String pagesDropDownXpath = "//select[@id='input_baseUrl' or @id='select']/option";
		String pageTitleXpath = "//*[@class='info_title' or @class='title']";
		WebDriver driver = null;
		try {
			String osName = System.getProperty("os.name").toLowerCase();
			if(osName.contains("mac"))
				System.setProperty("phantomjs.binary.path", "src/main/resources/mac/phantomjs");
			else
				System.setProperty("phantomjs.binary.path", "src/main/resources/phantomjs.exe");

			driver = new PhantomJSDriver();
			driver.get(apiDocUrl);
			HTMLUtility.waitForElemenentPresence(driver, By.xpath(pageTitleXpath));
			List<WebElement> findElements = driver.findElements(By.xpath(pagesDropDownXpath));
			for(WebElement element : findElements)
				pagesName.add(element.getText().trim());
			
			return pagesName;
		}catch(Exception e) {
			logger.error("Failed to detect the document title of entered URL. "+e.getStackTrace());
			throw new InvalidUrlException("Invalid swagger URL: Failed to detect the document title.");
		}
		finally {
			driver.close();
		}
	}

	public static String getSwaggerVersion(Document document) throws InvalidUrlException {
		String v1mainsectionXpath = "//ul[@id='resources']//div[@class='heading']/h2/a";
		String v2mainsectionXpath = "//h4[contains(@class,'opblock-tag')]/a/span";
		String swaggerHubDownloadIconXapath ="//i[@class='glyphicon glyphicon-download-alt']";
		
		Elements v1elms = Xsoup.compile(v1mainsectionXpath).evaluate(document).getElements();
		if (v1elms.size() > 0) {
			return "v1";
		} else if (Xsoup.compile(swaggerHubDownloadIconXapath).evaluate(document).getElements().size()>0) {
			return "swaggerHub";
		} else if (Xsoup.compile(v2mainsectionXpath).evaluate(document).getElements().size() > 0) {
			return "v2";
		}
		else
			throw new InvalidUrlException("Invalid url - Un-supported swagger URL.");
	}

	/**
	 * This method return the swagger json from given swagger URL.
	 * @param url
	 *            - swagger url(for eg : http://<hostname>/swagger.json)
	 * @return
	 */
	public static String getSwaggerJsonFromUrl(String url) {
		WebDriver driver = null;
		String jsonXpath = "//body//text()";
		try {
			System.setProperty("phantomjs.binary.path", "src/main/resources/phantomjs.exe");
			driver = new PhantomJSDriver();
			driver.get(url);
			Document document = Jsoup.parse(driver.getPageSource());
			return Xsoup.compile(jsonXpath).evaluate(document).get();
		} finally {
			driver.close();
		}
	}
	
	/**
	 * This method fetch the json string from swaggerJsonUrl. 
	 * @param swaggerJsonUrl - url generated from swaggerhub.
	 * @return
	 * @throws IOException
	 */
	public static String getSwaggerJsonFromDownloadUrl(String swaggerJsonUrl) throws IOException {
		URL url = new URL(swaggerJsonUrl);
		InputStream openStream = url.openStream();
		return IOUtils.toString(openStream, "UTF-8");
	}
	
	/**
	 * This method explicitly waits for element presence.
	 * @param driver
	 * @param locator
	 */
	private static void waitForElemenentPresence(WebDriver driver, By locator){
		WebDriverWait wait = new WebDriverWait(driver, 30);
		wait.until(ExpectedConditions.presenceOfElementLocated(locator));
	}
	

	/**
	 * This method verify the presence of an element in dom.
	 * @param _driver
	 * @param by
	 * @return
	 */
	private static boolean isElementPresent(final WebDriver _driver, By by, int timeout) {
		boolean webElementLoaded = false;
		try {
			while (webElementLoaded == false) {
				Wait<WebDriver> wait = new FluentWait<WebDriver>(_driver).withTimeout(timeout, TimeUnit.SECONDS)
						.pollingEvery(1, TimeUnit.SECONDS).ignoring(NoSuchElementException.class);
				wait.until(ExpectedConditions.presenceOfElementLocated(by));
				webElementLoaded = true;
			}
		} catch (Exception e) {
			System.out.println("Web Element not present");
		}
		return webElementLoaded;
	}

}
