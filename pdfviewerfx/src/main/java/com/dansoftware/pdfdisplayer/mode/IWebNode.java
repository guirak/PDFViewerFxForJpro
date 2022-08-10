package com.dansoftware.pdfdisplayer.mode;

import javafx.scene.Parent;

import java.util.function.Consumer;

/**
 * Interface for web nodes. </br>
 * Different web nodes are implemented to permit the display of the pdf viewer in different configurations :
 * - Native JavaFX apps.
 * - JPro apps.
 */
public interface IWebNode {

    /**
     * Executes JavaScript code in the currently hosting browser.
     * @param code – The code to be executed.
     * @param resultConsumer - Consumer to execute when the request response has been received
     * @return Result of the script execution
     */
    void executeScript(String code, Consumer<Object> resultConsumer);

    /**
     * Executes JavaScript code in the currently hosting browser.
     * @param code – The code to be executed.
     * @return Result of the script execution
     */
    void executeScript(String code);

    /**
     * Provide the code for accessing the pdfviewer document by script
     * @return
     */
    String getDocumentFromScript();

    /**
     * Provide the node to display the web content
     * @return Node to display the web content
     */
    Parent toNode();

    /**
     * Load an url
     * @param url Url to load
     */
    void load(String url);

    /**
     * Define a task to execyte when the runnable is loaded
     * @param onLoadedTask The task to execute
     */
    void setOnLoaded(final Runnable onLoadedTask);
}
