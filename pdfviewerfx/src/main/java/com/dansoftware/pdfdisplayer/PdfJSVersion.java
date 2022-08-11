package com.dansoftware.pdfdisplayer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum PdfJSVersion {
    V_2_2_228("/pdfjs_2.2.228", "web/viewer.html"),
    V_2_7_570("/pdfjs_2.7.570", "web/viewer.html");

    @Getter
    private final String rootPath;

    @Getter
    private final String htmlViewer;

    public static PdfJSVersion latest() {
        return V_2_7_570;
    }
}
