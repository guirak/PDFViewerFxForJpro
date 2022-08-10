package com.dansoftware.pdfdisplayer.mode;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Web node used on JavaFX apps running on JPRO server
 */
class JProWebNode implements IWebNode {

    /**
     * Id for the frame containing the viewer
     */
    static final String PDF_VIEWER_FRAME_ID = "pdfviewerFrame";

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(JProWebNode.class);

    /**
     * Executor to schedule scripts executions
     */
    private static final ScheduledExecutorService jproWebNodeExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Node displaying the web content
     */
    private final HTMLView htmlView = new HTMLView();

    /**
     * Constructor
     */
    public JProWebNode() {
        htmlView.setFocusTraversable(true);
    }

    /**
     * Execute the script
     * @param webAPI         Web API
     * @param code           Code to execute
     * @param resultConsumer Consumer of the result
     */
    private void internalExecuteScript(final WebAPI webAPI, final String code, final Consumer<Object> resultConsumer) {
        jproWebNodeExecutor.execute(() -> {
            System.out.println("Executing code : " + code);
            final String result;
            try {
                result = webAPI.executeScriptWithReturn(code);
                System.out.println("  >> Result = " + result);
                if (resultConsumer != null) {
                    resultConsumer.accept(result);
                }
            } catch (final Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void executeScript(final String code, final Consumer<Object> resultConsumer) {
        if (htmlView.getScene() != null) {
            internalExecuteScript(WebAPI.getWebAPI(htmlView.getScene()), code, resultConsumer);
        }
        else {
            WebAPI.getWebAPI(htmlView, webAPI -> internalExecuteScript(webAPI, code, resultConsumer));
        }
    }

    @Override
    public void executeScript(final String code) {
        executeScript(code, null);
    }

    @Override
    public String getDocumentFromScript() {
        return "document.getElementById('" + PDF_VIEWER_FRAME_ID + "').contentDocument";
    }

    @Override
    public Parent toNode() {
        return htmlView;
    }

    private void makePdfJsPublic() {

    }

    @Override
    public void loadPdfViewer(final String rootPath, final String htmlViewerPath) throws IOException {
        final ArrayList<URL> allFilesUrl = new ArrayList<>();
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(
                            getClass().getResourceAsStream(rootPath + "/filesList.txt"))))) {
                String line;
                while ((line = br.readLine()) != null) {
                    final String strUrl = Objects.requireNonNull(
                                    getClass().getResource(rootPath + "/" + line))
                            .toExternalForm();
                    allFilesUrl.add(new URL(strUrl));
                }
            }
        } catch (IOException e) {
            logger.error("Can't load the pdf viewer", e);
        }

        WebAPI.getWebAPI(htmlView, webAPI -> {
            allFilesUrl.forEach(webAPI::createPublicFile);

            final URL htmlPageUrl = Objects.requireNonNull(
                    getClass().getResource(rootPath + "/" + htmlViewerPath));
            final String publicUrl = WebAPI.getWebAPI(htmlView.getScene()).createPublicFile(htmlPageUrl);
            final String content =
                    "<iframe id=\"" + PDF_VIEWER_FRAME_ID + "\" frameborder=\"0\" style=\"width: 100%; height: 100%;\" src=\""
                            + publicUrl
                            + "\"> </iframe>";
            htmlView.setContent(content);
        });

        htmlView.setMinHeight(1200);
        htmlView.setMaxHeight(Double.MAX_VALUE);
        htmlView.getStyleClass().add("html-view");
    }

    @Override
    public void setOnLoaded(final Runnable onLoadedTask) {
        if (onLoadedTask != null) {
            onLoadedTask.run();
        }
//        if (onLoadedTask != null) {
//            jproWebNodeExecutor.schedule(onLoadedTask, 10, TimeUnit.SECONDS);
//        }
    }
}
