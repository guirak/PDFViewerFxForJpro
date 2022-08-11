package com.dansoftware.pdfdisplayer.mode;

import com.jpro.webapi.HTMLView;
import com.jpro.webapi.WebAPI;
import javafx.scene.Parent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * Web node used on JavaFX apps running on JPRO server
 */
@Slf4j
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
        return htmlView;
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
        });

        htmlView.setMinHeight(1200);
        htmlView.setMaxHeight(Double.MAX_VALUE);
        htmlView.getStyleClass().add("html-view");
    }

    @Override
    public void setOnLoaded(final Runnable onLoadedTask) {
        // TODO Check how to detect web node is loaded
        log.debug("setOnLoaded : " + onLoadedTask);
        if (onLoadedTask != null) {
            onLoadedTask.run();
        }
//        if (onLoadedTask != null) {
//            jproWebNodeExecutor.schedule(onLoadedTask, 5, TimeUnit.SECONDS);
//        }
    }
}
