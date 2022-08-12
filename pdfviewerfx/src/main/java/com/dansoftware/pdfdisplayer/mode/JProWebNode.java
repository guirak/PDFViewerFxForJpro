package com.dansoftware.pdfdisplayer.mode;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Web node used on JavaFX apps running on JPRO server
 */
@Log4j2
class JProWebNode implements IWebNode {

    /**
     * Id for the frame containing the viewer
     */
    static final String PDF_VIEWER_FRAME_ID = "pdfviewerFrame";

    /**
     * Executor to schedule scripts executions
     */
    private static final ScheduledExecutorService jproWebNodeExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Node displaying the web content
     */
    private final HTMLView htmlView = new HTMLView();

    /**
     * Root pane for the web node
     */
    private final StackPane rootPane = new StackPane();

    /**
     * Pane displayed until the viewer finish to load
     */
    private final VBox loadingPane = new VBox();

    /**
     * Task to call when the pdf viewer is loaded
     */
    private Runnable onLoadedTask;

    /**
     * Method called when the viewer is loaded
     */
    private boolean viewerLoaded = false;

    /**
     * List that contain all pending scripts execution that are waiting the load of the viewer
     */
    private final List<PendingScriptExecution> pendingScriptExecutions = new ArrayList<>();

    /**
     * Constructor
     */
    public JProWebNode() {
        htmlView.setFocusTraversable(true);

        // Adding a default stylesheets for the pane
        rootPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("JProWebNode.css")).toExternalForm());

        // Adding the html view
        rootPane.getChildren().add(htmlView);

        // Adding the loading pane
        loadingPane.getStyleClass().add("loadingPane");
        final ProgressIndicator progressIndicator = new ProgressBar();
        final Label label = new Label("Loading...");
        loadingPane.getChildren().addAll(progressIndicator, label);
        rootPane.getChildren().add(loadingPane);

        // Configuring the html view to match its parent
        htmlView.minHeightProperty().bind(rootPane.heightProperty());
        htmlView.maxHeightProperty().bind(rootPane.heightProperty());
        htmlView.getStyleClass().add("html-view");
    }

    /**
     * Execute the script
     * @param webAPI         Web API
     * @param code           Code to execute
     * @param resultConsumer Consumer of the result
     */
    private void internalExecuteScript(final WebAPI webAPI, final String code, final Consumer<Object> resultConsumer) {
        if (!viewerLoaded) {
            synchronized (pendingScriptExecutions) {
                pendingScriptExecutions.add(PendingScriptExecution.builder()
                        .code(code)
                        .resultConsumer(resultConsumer)
                        .build());
            }
        }
        else {
            jproWebNodeExecutor.execute(() -> {
                try {
                    log.info("Executing code : " + code);
                    final String result;

                    result = webAPI.executeScriptWithReturn(code);
                    log.info("Execution result : " + result);
                    if (resultConsumer != null) {
                        resultConsumer.accept(result);
                    }
                } catch (final Exception e) {
                    log.error("Can't execute script : ", e);
                }
            });
        }
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
    public Parent toNode() {
        return rootPane;
    }

    @Override
    public void loadPdfViewer(final String rootPath, final String htmlViewerPath) {
        WebAPI.getWebAPI(htmlView, webAPI -> {
            final String publicUrl = webAPI.getServerName() + "pdfjs" + rootPath + "/" + htmlViewerPath;
            log.debug("publicUrl : " + publicUrl);
            final String content =
                    "<iframe id=\"" + PDF_VIEWER_FRAME_ID + "\" frameborder=\"0\" style=\"width: 100%; height: 100%;\" src=\""
                            + publicUrl
                            + "\"> </iframe>";
            htmlView.setContent(content);

            log.info("PdfViewer loaded");
            viewerLoaded = true;
            onLoaded();
        });
    }

    /**
     * Method called when the viewer is loaded
     */
    private void onLoaded() {
        // TODO : The task is scheduled with a timer. I haven't found how to detect the full loading of the viewer
        // in the browser
        jproWebNodeExecutor.schedule(() -> {
            WebAPI.getWebAPI(htmlView, webAPI -> {
                synchronized (pendingScriptExecutions) {
                    pendingScriptExecutions.forEach(pendingScriptExecution ->
                            internalExecuteScript(webAPI, pendingScriptExecution.getCode(),
                                    pendingScriptExecution.getResultConsumer()));

                    pendingScriptExecutions.clear();
                }
            });

            if (onLoadedTask != null) {
                onLoadedTask.run();
                this.onLoadedTask = null;
            }

            Platform.runLater(() -> {
                loadingPane.setVisible(false);
                loadingPane.setManaged(false);
            });
        }, 1, TimeUnit.SECONDS);

    }

    @Override
    public void setOnLoaded(final Runnable onLoadedTask) {
        this.onLoadedTask = onLoadedTask;

        // If yet loaded, we load
        if (viewerLoaded) {
            onLoaded();
        }
    }

    /**
     * Class to store a pending script execution
     */
    @Builder
    @Getter
    private static class PendingScriptExecution {
        /**
         * Code to execute
         */
        private final String code;

        /**
         * Consumer of the result
         */
        private final Consumer<Object> resultConsumer;
    }
}
