package com.dansoftware.pdfdisplayer;

public enum PdfJSVersion {
    _2_2_228("/pdfjs_2.2.228", "web/viewer.html"),
    _2_7_570("/pdfjs_2.7.570", "web/viewer.html");

    private final String rootPath;
    private final String htmlViewer;

    PdfJSVersion(final String rootPath, final String htmlViewer) {
        this.rootPath = rootPath;
        this.htmlViewer = htmlViewer;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getHtmlViewer() {
        return htmlViewer;
    }

    public static PdfJSVersion latest() {
        return _2_7_570;
    }
}
