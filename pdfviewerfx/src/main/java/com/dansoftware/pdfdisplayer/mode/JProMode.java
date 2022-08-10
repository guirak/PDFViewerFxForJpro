package com.dansoftware.pdfdisplayer.mode;

class JProMode implements IMode{

    @Override
    public IWebNode createWebNode() {
        return new JProWebNode();
    }

    @Override
    public String getPdfViewerWindowScriptCode() {
        return "document.getElementById('" + JProWebNode.PDF_VIEWER_FRAME_ID + "').contentWindow.";
    }
}
