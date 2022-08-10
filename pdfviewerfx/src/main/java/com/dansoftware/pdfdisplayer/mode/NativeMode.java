package com.dansoftware.pdfdisplayer.mode;

class NativeMode implements IMode{
    @Override
    public IWebNode createWebNode() {
        return new NativeWebNode();
    }

    @Override
    public String getPdfViewerWindowScriptCode() {
        return "";
    }
}
