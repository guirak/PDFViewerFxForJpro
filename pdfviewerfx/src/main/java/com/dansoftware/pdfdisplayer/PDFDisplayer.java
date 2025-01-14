package com.dansoftware.pdfdisplayer;

import com.dansoftware.pdfdisplayer.mode.IMode;
import com.dansoftware.pdfdisplayer.mode.IWebNode;
import com.dansoftware.pdfdisplayer.mode.ModeFactory;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@Log4j2
public class PDFDisplayer {


    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        return t;
    });

    private final ObjectProperty<Consumer<Task<String>>> onLoaderTaskPresentProperty =
            new SimpleObjectProperty<>();

    private final PdfJSVersion version;

    /**
     * The current mode of the app
     */
    private final IMode mode = ModeFactory.create();

    private boolean pdfJsLoaded;

    private String loadScript;

    private String toExecuteWhenPDFJSLoaded = "";

    /**
     * Node containing the web content
     */
    private IWebNode webNode;

    public PDFDisplayer(final PdfJSVersion version) {
        this.version = version;
    }

    public PDFDisplayer() {
        this(PdfJSVersion.latest());
    }

    public PDFDisplayer(PdfJSVersion version, File file) throws IOException {
        this(version);
        loadPDF(file);
    }

    public PDFDisplayer(File file) throws IOException {
        this(PdfJSVersion.latest(), file);
    }

    public PDFDisplayer(PdfJSVersion version, URL url) throws IOException {
        this(version);
        loadPDF(url);
    }

    public PDFDisplayer(URL url) throws IOException {
        this(PdfJSVersion.latest(), url);
    }

    public PDFDisplayer(PdfJSVersion version, InputStream inputStream) {
        this(version);
        loadPDF(inputStream);
    }

    public PDFDisplayer(InputStream inputStream) {
        this(PdfJSVersion.latest(), inputStream);
    }

    @Deprecated
    public void displayPdf(File file) throws IOException {
        loadPDF(file);
    }

    @Deprecated
    public void displayPdf(URL url) throws IOException {
        loadPDF(url);
    }

    public void loadPDF(File file) throws IOException {
        loadPDF(new BufferedInputStream(new FileInputStream(file)));
    }

    public void loadPDF(URL url) throws IOException {
        loadPDF(new BufferedInputStream(url.openConnection().getInputStream()));
    }

    public void loadPDF(InputStream inputStream) {
        if (inputStream == null)
            return;

        Task<String> task = buildLoadingTask(inputStream);

        final Consumer<Task<String>> onLoaderTaskPresent = this.onLoaderTaskPresentProperty.get();
        if (onLoaderTaskPresent != null) {
            Platform.runLater(() -> onLoaderTaskPresent.accept(task));
        }
        THREAD_POOL.submit(task);
    }

    /**
     * @deprecated Use {@link #loadPDF(InputStream)} instead
     */
    @Deprecated
    public void displayPdf(final InputStream inputStream) {
        loadPDF(inputStream);
    }

    private Task<String> buildLoadingTask(final InputStream inputStream) {
        final Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    long contentSize = inputStream.available();
                    long onePercent = contentSize / 100;

                    int allReadBytesCount = 0;

                    byte[] buf = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buf)) >= 0) {
                        allReadBytesCount += bytesRead;
                        outputStream.write(buf, 0, bytesRead);

                        if (onePercent > 0) {
                            double percent = allReadBytesCount / (double) onePercent;
                            updateProgress(percent, 100d);
                        }

                        if (this.isCancelled()) {
                            return null;
                        }
                    }

                    byte[] data = outputStream.toByteArray();
                    String base64 = Base64.getEncoder().encodeToString(data);

                    //JS Function declaration
                    return mode.getPdfViewerWindowScriptCode() + "openFileFromBase64('" + base64 + "');";
                } finally {
                    inputStream.close();
                }
            }
        };
        task.valueProperty().addListener((observable, oldValue, js) -> {
            if (js != null) {
                try {
                    webNode.executeScript(js);
                } catch (Exception ex) {
                    if (!pdfJsLoaded) loadScript = js;
                }
            }
        });
        return task;
    }

    @SuppressWarnings("all")
    public void setSecondaryToolbarToggleVisibility(boolean value) {
        setVisibilityOf("secondaryToolbarToggle", value);

        final String pdfViewerWindowAccessor = mode.getPdfViewerWindowScriptCode();

        String js;
        if (value) {
            js = new StringBuilder()
                    .append("var element = " + pdfViewerWindowAccessor + "document.getElementsByClassName('verticalToolbarSeparator')[0];")
                    .append("element.style.display = 'inherit';")
                    .append("element.style.visibility = 'inherit';")
                    .toString();
        }
        else {
            js = new StringBuilder()
                    .append("var element = " + pdfViewerWindowAccessor + "document.getElementsByClassName('verticalToolbarSeparator')[0];")
                    .append("element.style.display = 'none';")
                    .append("element.style.visibility = 'hidden';")
                    .toString();
        }

        try {
            webNode.executeScript(js);
        } catch (Exception ex) {
            if (!pdfJsLoaded) toExecuteWhenPDFJSLoaded += js;
        }
    }

    @SuppressWarnings("all")
    public void setVisibilityOf(final String id, final boolean value) {
        String css;
        final String pdfViewerWindowAccessor = mode.getPdfViewerWindowScriptCode();
        if (value) {
            css = new StringBuilder()
                    .append(pdfViewerWindowAccessor + "document.getElementById('" + id + "').style.display = 'inherit';")
                    .append(pdfViewerWindowAccessor + "document.getElementById('" + id + "').style.visibility = 'inherit';")
                    .toString();
        }
        else {
            css = new StringBuilder()
                    .append(pdfViewerWindowAccessor + "document.getElementById('" + id + "').style.display = 'none';")
                    .append(pdfViewerWindowAccessor + "document.getElementById('" + id + "').style.visibility = 'hidden';")
                    .toString();
        }

        log.info("Mode = " + mode + ", CSS : " + css);

        try {
            webNode.executeScript(css);
        } catch (Exception ex) {
            if (!pdfJsLoaded) this.toExecuteWhenPDFJSLoaded += css;
        }
    }

    public void getActualPageNumber(final IntConsumer pageNumberConsumer) {
        webNode.executeScript("PDFViewerApplication.page;", o -> pageNumberConsumer.accept((int) o));
    }

    public void getTotalPageCount(final IntConsumer pageNumberConsumer) {
        webNode.executeScript("PDFViewerApplication.pagesCount;", o -> pageNumberConsumer.accept((int) o));
    }

    public void navigateByPage(int pageNum) {
        String jsCommand = mode.getPdfViewerWindowScriptCode() + "goToPage(" + pageNum + ");";
        try {
            webNode.executeScript(jsCommand);
        } catch (Exception ex) {
            if (!pdfJsLoaded) toExecuteWhenPDFJSLoaded += jsCommand;
        }
    }

    public void executeScript(String js) {
        try {
            this.webNode.executeScript(js);
        } catch (Exception ex) {
            if (!pdfJsLoaded) toExecuteWhenPDFJSLoaded += String.format("%s;", js);
        }
    }

    /**
     * Create the node with the web content
     * @return The node with the web content
     */
    private IWebNode createWebNode() {
        // Create the web node
        final IWebNode newWebNode = mode.createWebNode();

        // Load the pdf viewer
        newWebNode.loadPdfViewer(version.getRootPath(), version.getHtmlViewer());

        // Define a task to execute when the web content is successfully loaded
        newWebNode.setOnLoaded(() -> {
            try {
                pdfJsLoaded = true;

                if (loadScript != null) {
                    log.debug("PDF already loaded - Load " + loadScript);
                    newWebNode.executeScript(loadScript);
                }


                newWebNode.executeScript(toExecuteWhenPDFJSLoaded);
                toExecuteWhenPDFJSLoaded = null;
                newWebNode.setOnLoaded(null);
            } catch (Exception e) {
                log.error("Error while executing scripts on PDF Viewer start : ", e);
            }
        });


        return newWebNode;
    }

    /**
     * Provide the node which display the web content
     * @return The node which display the web content
     */
    public Parent toNode() {
        if (webNode == null) {
            webNode = createWebNode();
        }
        return webNode.toNode();
    }

    public Consumer<Task<String>> getOnLoaderTaskPresentProperty() {
        return onLoaderTaskPresentProperty.get();
    }

    public void setOnLoaderTaskPresentProperty(Consumer<Task<String>> onLoaderTaskPresentProperty) {
        this.onLoaderTaskPresentProperty.set(onLoaderTaskPresentProperty);
    }

    public ObjectProperty<Consumer<Task<String>>> onLoaderTaskPresentProperty() {
        return onLoaderTaskPresentProperty;
    }
}
