import com.dansoftware.pdfdisplayer.PDFDisplayer;
import com.dansoftware.pdfdisplayer.PdfJSVersion;
import com.jpro.webapi.JProApplication;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class JProDemo extends JProApplication {
    @Override
    public void start(Stage primaryStage) throws Exception {
        PDFDisplayer displayer = new PDFDisplayer(PdfJSVersion.V_2_2_228);

        Stage alertStage = new Stage();
        alertStage.setScene(new Scene(new StackPane(new Label("Processing"))));
        alertStage.setAlwaysOnTop(true);
        alertStage.show();

        displayer.setOnLoaderTaskPresentProperty(task -> {
            task.setOnRunning(e -> alertStage.show());
            task.setOnSucceeded(e -> alertStage.close());
            task.setOnFailed(e -> alertStage.close());
        });

        Button btn = new Button("Load");
        btn.setOnAction(e -> loadPDF(displayer));

        Text javaInfo = new Text(
                String.format("Java version: %s, JavaFX version: %s",
                        System.getProperty("java.version"),
                        System.getProperty("javafx.version"))
        );

        primaryStage.setScene(new Scene(new VBox(btn, displayer.toNode(), javaInfo)));
        primaryStage.show();

        displayer.setSecondaryToolbarToggleVisibility(true);
       // displayer.toNode().getStylesheets().add("style.css");

        //displayer.executeScript("document.getElementById('secondaryToolbarToggle').style.backgroundColor = 'blue';");

        ///JSLogListener.setOutputStream(System.err);
    }

    private void loadPDF(PDFDisplayer pdfDisplayer) {
        try {
            pdfDisplayer.loadPDF(new URL("https://www.tutorialspoint.com/jdbc/jdbc_tutorial.pdf"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
