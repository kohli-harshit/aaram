package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;


public class CommonUIOperations {

    final static Logger logger = Logger.getLogger(Controller.class);

    public static void showAlert(Alert.AlertType alertType, String message,String title)
    {
        Platform.runLater(
                () -> {
                    Alert alert = new Alert(alertType);
                    alert.setTitle(title);
                    alert.setHeaderText(null);
                    alert.setContentText(message);
                    DialogPane dialogPane = alert.getDialogPane();
                    dialogPane.getStylesheets().add("css/dialog.css");
                    dialogPane.getStyleClass().add("myDialog");
                    alert.showAndWait();
                }
        );
    }

    public static void showAlertWithHyperlink(Alert.AlertType alertTypeClick,String message,String linkText)
    {
        Platform.runLater(
                () -> {
                    Alert alertClick = new Alert(alertTypeClick);
                    alertClick.setTitle("Error");
                    alertClick.setHeaderText(null);
                    FlowPane fp = new FlowPane();
                    Label lbl = new Label(message);
                    Hyperlink link = new Hyperlink(linkText);
                    fp.getChildren().addAll(lbl, link);

                    link.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent e) {
                            try {
                                Desktop.getDesktop().browse(new URI((linkText)));
                            } catch (Exception ex) {
                                logger.error(ExceptionUtils.getStackTrace(ex));
                            }
                        }
                    });

                    alertClick.getDialogPane().contentProperty().set(fp);
                    DialogPane dialogPane = alertClick.getDialogPane();
                    dialogPane.getStylesheets().add("css/dialog.css");
                    dialogPane.getStyleClass().add("myDialog");
                    alertClick.showAndWait();
                });
    }

    public static void showToolTip(Node object, String text)
    {
        Platform.runLater(
                () -> {
                    setupCustomTooltipBehavior(100,10000,100);
                    Tooltip tooltip = new Tooltip();
                    tooltip.setText(text);
                    Tooltip.install(object,tooltip);
                }
        );
    }

    /**
     * <p>
     * Tooltip behavior is controlled by a private class javafx.scene.control.Tooltip$TooltipBehavior.
     * All Tooltips share the same TooltipBehavior instance via a static private member BEHAVIOR, which
     * has default values of 1sec for opening, 5secs visible, and 200 ms close delay (if mouse exits from node before 5secs).
     *
     * The hack below constructs a custom instance of TooltipBehavior and replaces private member BEHAVIOR with
     * this custom instance.
     * </p>
     *
     */
    private static void setupCustomTooltipBehavior(int openDelayInMillis, int visibleDurationInMillis, int closeDelayInMillis) {
        try {

            Class TTBehaviourClass = null;
            Class<?>[] declaredClasses = Tooltip.class.getDeclaredClasses();
            for (Class c:declaredClasses) {
                if (c.getCanonicalName().equals("javafx.scene.control.Tooltip.TooltipBehavior")) {
                    TTBehaviourClass = c;
                    break;
                }
            }
            if (TTBehaviourClass == null) {
                // abort
                return;
            }
            Constructor constructor = TTBehaviourClass.getDeclaredConstructor(
                    Duration.class, Duration.class, Duration.class, boolean.class);
            if (constructor == null) {
                // abort
                return;
            }
            constructor.setAccessible(true);
            Object newTTBehaviour = constructor.newInstance(
                    new Duration(openDelayInMillis), new Duration(visibleDurationInMillis),
                    new Duration(closeDelayInMillis), false);
            if (newTTBehaviour == null) {
                // abort
                return;
            }
            Field ttbehaviourField = Tooltip.class.getDeclaredField("BEHAVIOR");
            if (ttbehaviourField == null) {
                // abort
                return;
            }
            ttbehaviourField.setAccessible(true);

            // Cache the default behavior if needed.
            Object defaultTTBehavior = ttbehaviourField.get(Tooltip.class);
            ttbehaviourField.set(Tooltip.class, newTTBehaviour);

        } catch (Exception e) {
            System.out.println("Aborted setup due to error:" + e.getMessage());
        }
    }

}

