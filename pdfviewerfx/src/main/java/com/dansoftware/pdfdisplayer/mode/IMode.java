package com.dansoftware.pdfdisplayer.mode;

public interface IMode {

    /**
     * Create a web node containing the pdf viewer
     * @return The web node created
     */
    IWebNode createWebNode();

    /**
     * Provide the code to call to access the PDF viewer element and execute script on it
     * @return The code to access the PDF viewer element
     */
    String getPdfViewerWindowScriptCode();
}
