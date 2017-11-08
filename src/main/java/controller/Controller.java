package controller;

import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;
import controller.customexceptions.BlankURLException;
import controller.customexceptions.InvalidUrlException;
import controller.db.AppVersion;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import model.Section;
import model.SourceCodeGenerator;
import model.Swagger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.controlsfx.control.textfield.TextFields;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import view.Main;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;

public class Controller extends AnchorPane implements Initializable {

    @FXML
    private ImageView create;
    @FXML
    private ImageView download;
    @FXML
    private ImageView info;
    @FXML
    private ImageView power;
    @FXML
    private AnchorPane mainwindow;
    @FXML
    private AnchorPane inspectwindow;
    @FXML
    private AnchorPane generatewindow;
    @FXML
    private ImageView imgSwagger;
    @FXML
    private TextField txtSwagger;
    @FXML
    private Button btnInspect;
    @FXML
    private VBox swaggersections;
    @FXML
    private Button btnGenerateCode;
    @FXML
    private TextField txtCodeLocation;
    @FXML
    private ScrollPane generatescrollPane;
    @FXML
    private Label lblCodeLocation;
    @FXML
    private ImageView generateprogress;
    @FXML
    private ImageView generatecodeprogress;
    @FXML
    private Label lblGenerateStatus;
    @FXML
    private Label lblGenerateCodeStatus;
    @FXML
    private AnchorPane downloadwindow;
    @FXML
    private AnchorPane nodownloadwindow;
    @FXML
    private ImageView create_noDownload;
    @FXML
    private AnchorPane resultwindow;
    @FXML
    private Hyperlink linksuccess;
    @FXML
    private ImageView imgsuccess;
    @FXML
    private AnchorPane splashwindow;
    @FXML
    private AnchorPane topbar;
    @FXML
    private AnchorPane helpwindow;
    @FXML
    private Label lblLoading;
    @FXML
    private Hyperlink documentationlink;
    @FXML private ComboBox comboBoxPagesList;
    @FXML private Label multiplePagesLabel;
    private String selection;
    private String pageName;
    private int noOfPages;
    private static int clickOnCreateButtonCounter=0;
    public static List<Section> sections = new ArrayList<Section>();

    public static Boolean prerequisitesSatisfied=false;


    private static String functionalityName;
    final static Logger logger = Logger.getLogger(Controller.class);


    @FXML
    private void handlebutton(Event event)
    {
        if(event.getTarget()==create || event.getTarget()==create_noDownload)
        {
            generatewindow.setVisible(false);
            downloadwindow.setVisible(false);
            helpwindow.setVisible(false);
            if(inspectwindow.isVisible()){
                inspectwindow.setVisible(false);
            }
            else
            {
                inspectwindow.setVisible(true);
                updateCreateImageToGreen(event);
                updateGenerateImageToDefault(event);
                updateInfoImageToDefault(event);
                txtSwagger.setText("");
            }
        }
        else if(event.getTarget()==download)
        {
            inspectwindow.setVisible(false);
            generatewindow.setVisible(false);
            helpwindow.setVisible(false);
            if(downloadwindow.isVisible())
            {
                downloadwindow.setVisible(false);
            }
            else
            {
                downloadwindow.setVisible(true);
                updateGenerateImageToGreen(event);
                updateCreateImageToDefault(event);
                updateCreateDownloadImageToDefault(event);
                updateInfoImageToDefault(event);
            }
        }
        else if(event.getTarget()==info){
            inspectwindow.setVisible(false);
            generatewindow.setVisible(false);
            downloadwindow.setVisible(false);
            if(helpwindow.isVisible()){
                helpwindow.setVisible(false);
            }

            else {
                helpwindow.setVisible(true);
                updateInfoImageToGreen(event);
                updateCreateImageToDefault(event);
                updateGenerateImageToDefault(event);
            }
        }
        else if(event.getTarget()==power){
            Platform.exit();
        }
    }


    public void gotoGenerate()
    {
        try
        {
            Service service = new Service() {
                @Override
                protected Task createTask() {
                    return new Task() {
                        @Override
                        protected Object call() throws Exception {
                            imgSwagger.setImage(new Image("file:src/main/resources/images/swagger-logo-loading.gif"));
                            btnInspect.setDisable(true);
                            txtSwagger.setDisable(true);
                            logger.info("Started Inspecting...");
                            try {
                                SwaggerParser swaggerParser;
                                Swagger swagger = null;

                                String URL=txtSwagger.getText();
                                logger.debug("URL Entered by User - " + URL);
                                URLValidator.isValidUrl(URL);
                                List<String> pageNames = HTMLUtility.getPagesFromApiDoc(URL);
                                logger.info("fetching list of pages");
                                if(clickOnCreateButtonCounter>0) {
                                    multiplePagesLabel.setDisable(false);
                                    comboBoxPagesList.setDisable(false);
                                }
                                noOfPages=pageNames.size();
                                if(noOfPages>0) {
                                    multiplePagesLabel.setVisible(true);
                                    comboBoxPagesList.setVisible(true);

                                    comboBoxPagesList.setPromptText("");
                                    comboBoxPagesList.getItems().addAll(pageNames);


                                    selection=null;
                                    while(selection==null) {
                                        selection=(String)comboBoxPagesList.getValue();
                                    }
                                    clickOnCreateButtonCounter++;
                                    comboBoxPagesList.getItems().clear();

                                    multiplePagesLabel.setDisable(true);
                                    comboBoxPagesList.setDisable(true);

                                    pageName=selection;
                                    swaggerParser = SwaggerParser.getSwaggerParser(URL, selection);
                                    swagger = swaggerParser.getSwaggerDetails();
                                    sections = swagger.getSectionList();
                                }
                                else {
                                    swaggerParser = SwaggerParser.getSwaggerParser(URL, "");
                                    swagger = swaggerParser.getSwaggerDetails();
                                    sections = swagger.getSectionList();
                                }

                                functionalityName = swagger.getTitle().replaceAll("[^a-zA-Z0-9]", "");
                                Platform.runLater(
                                        () -> {
                                            inspectwindow.setVisible(false);
                                            multiplePagesLabel.setVisible(false);
                                            comboBoxPagesList.setVisible(false);
                                            generatewindow.setVisible(true);
                                            txtCodeLocation.setText(System.getProperty("user.home")+ System.getProperty("file.separator") + "Desktop");
                                            generatewindow.toFront();
                                            try {
                                                Cache.writeToFile(txtSwagger.getText());
                                                updateAutoComplete();
                                            } catch (Exception e) {
                                                logger.error(ExceptionUtils.getStackTrace(e));
                                            }
                                            swaggersections.getChildren().clear();
                                            CheckBox checkBoxSelect = new CheckBox("Select All");
                                            checkBoxSelect.setStyle(" -fx-text-fill:white;-fx-font-size: 13;");
                                            checkBoxSelect.setSelected(true);
                                            swaggersections.getChildren().addAll(checkBoxSelect);
                                            for(Section section:sections)
                                            {
                                                logger.debug("Section Fetched - " + section.getSectionName());
                                                CheckBox checkBox = new CheckBox(section.getSectionName());
                                                checkBox.setSelected(true);
                                                checkBox.setStyle(" -fx-text-fill:white;-fx-font-size: 13;");
                                                swaggersections.getChildren().addAll(checkBox);
                                                checkBox.setOnMouseClicked(new EventHandler<MouseEvent>() {
                                                    public void handle(MouseEvent me) {
                                                        if(!checkBox.isSelected())
                                                        {
                                                            if(checkBoxSelect.isSelected())
                                                                checkBoxSelect.setSelected(false);
                                                        }
                                                    }
                                                });
                                            }

                                            checkBoxSelect.setOnMouseClicked(new EventHandler<MouseEvent>() {
                                                public void handle(MouseEvent me) {
                                                    for(Node checkBox :  swaggersections.getChildren())
                                                    {
                                                        CheckBox childcheckBox =  ((CheckBox)checkBox);
                                                        childcheckBox.setSelected(checkBoxSelect.isSelected());
                                                    }
                                                }
                                            });

                                            logger.info("Done Inspecting!");
                                        }
                                );
                            }
                            catch (BlankURLException e)
                            {
                                logger.error(ExceptionUtils.getStackTrace(e));
                                CommonUIOperations.showAlert(Alert.AlertType.ERROR,"Please enter a Swagger URL in the Text Box","Error while processing request");
                            }
                            catch (InvalidUrlException e)
                            {
                                logger.error(ExceptionUtils.getStackTrace(e));
                                CommonUIOperations.showAlert(Alert.AlertType.ERROR,"Please enter a proper Swagger URL in the Text Box","Error while processing request");
                            }
                            catch(UnknownHostException e) {
                    			logger.error(ExceptionUtils.getStackTrace(e));
                    			CommonUIOperations.showAlert(Alert.AlertType.ERROR,"Entered URL is not hosted - Please enter a valid url.", "Unknown Host Exception");
                            }
                            catch (Exception e)
                            {
                                logger.error(ExceptionUtils.getStackTrace(e));
                                CommonUIOperations.showAlert(Alert.AlertType.ERROR,"Unknown Error Occurred - " + e.getMessage(),"Error while processing request");
                            }
                            finally
                            {
                                imgSwagger.setImage(new Image("file:src/main/resources/images/swagger-logo.png"));
                                btnInspect.setDisable(false);
                                txtSwagger.setDisable(false);
                            }
                            return null;
                        }
                    };
                }
            };
            service.start();
        }
        catch (Exception e)
        {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public void inspect(Event event) throws Exception
    {
        gotoGenerate();
    }
    public void imgSwaggerShowToolTip(Event event)
    {
        CommonUIOperations.showToolTip(imgSwagger,"Enter Swagger Link in the Text box below");
    }

    public void txtSwaggerMouseEntered(Event event)
    {
        CommonUIOperations.showToolTip(txtSwagger,"Enter Swagger Link for your API in this Text box");
    }

    public void btnInspectMouseEntered(Event event)
    {
        btnInspect.setStyle("-fx-background-color:#58D68D");
        CommonUIOperations.showToolTip(btnInspect,"Click to check out the Swagger link entered in above Text box");
    }

    public void btnInspectMouseExited(Event event)
    {
        btnInspect.setStyle(null);
    }

    public void updateCreateImageToGreen(Event event)
    {
        create.setImage(new Image("file:src/main/resources/images/create_green.png"));
        CommonUIOperations.showToolTip(create,"Click here to go to Swagger Inspect screen");
    }

    public void updateCreateDownloadImageToGreen(Event event)
    {
        create_noDownload.setImage(new Image("file:src/main/resources/images/create_green.png"));
        CommonUIOperations.showToolTip(create_noDownload,"Click here to go to Swagger Inspect screen");
    }

    public void btnGenerateMouseEntered(Event event)
    {
        btnGenerateCode.setStyle("-fx-background-color:#58D68D");
        CommonUIOperations.showToolTip(btnGenerateCode,"Click to generate the ApiART code for the selected sections");
    }

    public void btnGenerateMouseExited(Event event)
    {
        btnGenerateCode.setStyle(null);
    }

    public void updateCreateImageToDefault(Event event)
    {
        if(!inspectwindow.isVisible() && !generatewindow.isVisible())
        {
            create.setImage(new Image("file:src/main/resources/images/create_gray.png"));
        }
    }

    public void updateCreateDownloadImageToDefault(Event event)
    {
        if(!inspectwindow.isVisible() && !generatewindow.isVisible())
        {
            create_noDownload.setImage(new Image("file:src/main/resources/images/create_gray.png"));
        }
    }

    public void updateGenerateImageToGreen(Event event)
    {
        download.setImage(new Image("file:src/main/resources/images/download_green.png"));
        CommonUIOperations.showToolTip(download,"Click here to go to Download screen");
    }

    public void updateGenerateImageToDefault(Event event)
    {
        if( !downloadwindow.isVisible())
        {
            download.setImage(new Image("file:src/main/resources/images/download_gray.png"));
        }
    }

    public void updateInfoImageToGreen(Event event)
    {
        info.setImage(new Image("file:src/main/resources/images/info_green.png"));
        CommonUIOperations.showToolTip(info,"Click here to go to Help Screen");
    }

    public void updateInfoImageToDefault(Event event)
    {
        if( !helpwindow.isVisible()) {
            info.setImage(new Image("file:src/main/resources/images/info_gray.png"));
        }
    }

    public void updatePowerImageToGreen(Event event)
    {
        power.setImage(new Image("file:src/main/resources/images/shutdown_green.png"));
        CommonUIOperations.showToolTip(power,"Exit");
    }

    public void updatePowerImageToDefault(Event event)
    {
        power.setImage(new Image("file:src/main/resources/images/shutdown_gray.png"));
    }

    public void onEnterTextSwagger(ActionEvent e)
    {
        gotoGenerate();
    }


    public void showGenerateWindow(KeyEvent evn) {
        if (evn.getCode() == KeyCode.ENTER) {
            gotoGenerate();
        }
    }

    public void generateCode(ActionEvent event)
    {
        String codeLocation = txtCodeLocation.getText();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to Generate code at " + codeLocation + "?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add("src/main/resources/css/dialog.css");
        dialogPane.getStyleClass().add("myDialog");
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            clone(event,codeLocation + "/" + functionalityName);
        }
    }

    public void clone(ActionEvent event,String codeLocation)
    {

        Service service = new Service() {
            @Override
            protected Task createTask() {
                return new Task() {
                    @Override
                    protected Object call() throws Exception {
                        try {
                            btnGenerateCode.setDisable(true);
                            generatescrollPane.setDisable(true);
                            lblCodeLocation.setDisable(true);
                            txtCodeLocation.setDisable(true);
                            generateprogress.setVisible(true);
                            lblGenerateStatus.setVisible(true);
                            CommonUIOperations.showToolTip(generateprogress, "Getting Template from GitHub and creating your Automation Project");
                            GitOperations.cloneApiART(codeLocation);
                            lblGenerateStatus.setVisible(false);
                            generateprogress.setVisible(false);
                            generatecodeprogress.setVisible(true);
                            lblGenerateCodeStatus.setVisible(true);
                            CommonUIOperations.showToolTip(generatecodeprogress, "Creating Java files for your Automation project");


                            SourceCodeGenerator sourceCodeGenerator = new SourceCodeGenerator();
                            if(noOfPages>0) {
                                sourceCodeGenerator.setPageName("/"+pageName.replaceAll("\\s|-|_|\\{|\\}|/", ""));
                            }
                            else{
                                sourceCodeGenerator.setPageName("");
                            }
                            sourceCodeGenerator.setOutputDirectory(GitOperations.cloneDir);
                            sourceCodeGenerator.setSectionList(filterSections());
                            sourceCodeGenerator.generateCode();
                            updateProject(GitOperations.cloneDir, functionalityName);
                            nodownloadwindow.setVisible(false);
                            generatewindow.setVisible(false);
                            downloadwindow.setVisible(true);
                            updateCreateImageToDefault(event);
                            updateGenerateImageToGreen(event);
                            resultwindow.setVisible(true);
                            CommonUIOperations.showToolTip(generatecodeprogress, "Automation Project is created successfully at - " + GitOperations.cloneDir);
                            EventHandler<Event> openDir =  new EventHandler<Event>() {
                                @Override public void handle(Event e){
                                    try {
                                        Desktop.getDesktop().open(new java.io.File(GitOperations.cloneDir));
                                    }
                                    catch (Exception ex)
                                    {
                                        logger.error(ex);
                                        CommonUIOperations.showAlert(Alert.AlertType.ERROR,"Error Occured in opening folder - " + GitOperations.cloneDir,"Error");
                                    }
                                }
                            };
                            linksuccess.setOnMouseClicked(openDir);
                            imgsuccess.setOnMouseClicked(openDir);
                        }
                        catch (Exception e)
                        {
                            logger.error(ExceptionUtils.getStackTrace(e));
                            CommonUIOperations.showAlert(Alert.AlertType.ERROR, e.getMessage(), "Error while processing request");
                        }
                        finally {
                            lblGenerateStatus.setVisible(false);
                            lblGenerateCodeStatus.setVisible(false);
                            generateprogress.setVisible(false);
                            generatecodeprogress.setVisible(false);
                            btnGenerateCode.setDisable(false);
                            generatescrollPane.setDisable(false);
                            lblCodeLocation.setDisable(false);
                            txtCodeLocation.setDisable(false);
                        }
                        return null;
                    }
                };
            }
        };

        service.start();
    }

    /**
     * This methodd will update the projectName in downloaded pom.xml by replacing the artifetcId by titleName.
     * @param codeLocation - file location of downloaded project
     * @param titleName - project name for downloaded project
     * @throws IOException
     */
    private void updateProject(String codeLocation, String titleName) throws IOException{
        String content = new Scanner(new File(codeLocation+"/pom.xml")).useDelimiter("\\Z").next();
        content=content.replace("<artifactId>apiART</artifactId>", "<artifactId>"+titleName+"</artifactId>");
        FileWriter fw = new FileWriter(codeLocation + "/pom.xml");
        fw.write(content);
        fw.close();

        //Deleting unneccesary files
//        FileUtils.forceDelete(new File(codeLocation + "/src/test/java"));
//        FileUtils.forceDelete(new File(codeLocation + "/src/main/java/api"));
//        FileUtils.forceDelete(new File(codeLocation + "/src/main/java/helper"));
//        FileUtils.forceDelete(new File(codeLocation + "/src/main/java/pojo"));
        FileUtils.forceDelete(new File(codeLocation + "/test-data"));
        FileUtils.forceDelete(new File(codeLocation + "/target"));
        FileUtils.forceDelete(new File(codeLocation + "/.classpath"));
        FileUtils.forceDelete(new File(codeLocation + "/.settings"));
        FileUtils.forceDelete(new File(codeLocation + "/.project"));
    }

    public List<Section> filterSections()
    {
        List<Section> filteredSections = new ArrayList<Section>();
        for(int sectionCounter=0;sectionCounter<sections.size();sectionCounter++) {
            CheckBox correspondingCheckBox= (CheckBox)swaggersections.getChildren().get(sectionCounter+1);
            if(correspondingCheckBox.isSelected())
            {
                filteredSections.add(sections.get(sectionCounter));
            }
        }
        return filteredSections;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try {
            mainwindow.setStyle("-fx-background-color: rgba(0, 0, 0, 0);");
            setup();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public void setup() throws Exception
    {
        Service service = new Service() {
            @Override
            protected Task createTask() {
                return new Task() {
                    @Override
                    protected Object call() throws Exception {
                        setLoadingText("      Thinking really hard...      ");
                        String osName = System.getProperty("os.name").toLowerCase();
                        try {
                            AppVersion.checkVersion(Main.version);
                            setLoadingText("      Doing The Impossible...      ");
                            logger.info("Application version - " + Main.version + " is up-to date");
                            String cacheFilePath = null;
                            if(osName.contains("windows"))
                                cacheFilePath = System.getenv("LOCALAPPDATA") + "/AARAM/cache.json";
                            else if(osName.contains("mac"))
                                cacheFilePath = System.getProperty("user.home")+ "/Library/Application Support/AARAM/cache.json";
                            File cacheFile = new File(cacheFilePath);
                            Boolean fileExists = cacheFile.exists();
                            logger.info("Cache File Found = " + fileExists);
                            if (!fileExists) {
                                cacheFile.getParentFile().mkdirs();
                                cacheFile.createNewFile();
                                logger.info("File Created at = " + cacheFilePath);
                            }
                            setLoadingText("Does Anyone Actually Read This?");
                        }
                        catch (MongoSocketOpenException|MongoTimeoutException e)
                        {
                            CommonUIOperations.showAlert(Alert.AlertType.ERROR,"Not able to connect to the Database to fetch the current supported version. Don't worry you can still use the tool!","Error while connecting to DB");
                        }
                        catch (Exception e)
                        {
                            if(e.getMessage().contains("not on the latest"))
                            {
                                CommonUIOperations.showAlertWithHyperlink(Alert.AlertType.ERROR,"You are not on the latest version. Get the current one @ ","https://someurl");
                            }
                            else
                            {
                                CommonUIOperations.showAlert(Alert.AlertType.ERROR,e.getMessage(),"Error while processing request");
                            }
                        }
                        finally {
                            topbar.setVisible(true);
                            splashwindow.setVisible(false);
                            updateAutoComplete();
                        }
                        return null;
                    }
                };
            }
        };
        service.start();
    }

    public void setLoadingText(String text) throws InterruptedException {
        int maxTime=1000;
        DateTime startTime = DateTime.now();
        Platform.runLater(
                () -> {
                    lblLoading.setText(text);
                });
        DateTime endTime = DateTime.now();
        Period timeTaken =  new Period(startTime, endTime, PeriodType.millis());
        if(timeTaken.getValue(0)<maxTime)
        {
            Thread.sleep(maxTime-timeTaken.getValue(0));
        }
    }

    public void updateAutoComplete()
    {
        List urlsList = null;
        try {
            urlsList = Cache.readingFile();
            if(urlsList!=null) {
                TextFields.bindAutoCompletion(txtSwagger, (String[])urlsList.toArray(new String[urlsList.size()]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openDocumentation(Event event)
    {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/kohli-harshit/aaram/blob/master/README.md"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
    }
}
