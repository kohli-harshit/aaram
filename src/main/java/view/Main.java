package view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.nio.file.Paths;

import static javafx.stage.StageStyle.TRANSPARENT;

public class Main extends Application  {

    public static String version="1.2";
    private double xOffset = 0;
    private double yOffset = 0;

    final static Logger logger = Logger.getLogger(Main.class);


    @Override
    public void start(Stage primaryStage) throws Exception{
        try
        {
            String fxmlpath="src/main/resources/app.fxml";
            Parent root = FXMLLoader.load(Paths.get(fxmlpath).toUri().toURL());
            root.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                }
            });
            root.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    primaryStage.setX(event.getScreenX() - xOffset);
                    primaryStage.setY(event.getScreenY() - yOffset);
                }
            });

            primaryStage.setTitle("REST API Automation");
            primaryStage.getIcons().add(new Image("file:src/main/resources/images/swagger-logo-square.png"));
            Scene scene = new Scene(root, 400, 625);
            primaryStage.setScene(scene);
            scene.setFill(null);
            primaryStage.setResizable(false);
            primaryStage.initStyle(TRANSPARENT);
            primaryStage.show();
            logger.info("Main Screen launched");
        }
        catch (Exception e)
        {
            logger.error(ExceptionUtils.getStackTrace(e));
            Platform.exit();
        }
    }
    public static void main(String[] args) {
        PropertyConfigurator.configure("src/main/resources/log4j.properties");
        launch(args);
    }
}