package com.dansoftware.pdfdisplayer.mode;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.Parent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.function.Consumer;

/**
 * Web node used on native JavaFX apps
 */
class NativeWebNode implements IWebNode {

    /**
     * Web view
     */
    private final WebView webView = new WebView();

    /**
     * Loading state listener
     */
    private ChangeListener<Worker.State> loadingStateListener;

    /**
     * Constructor
     */
    public NativeWebNode() {
        webView.setContextMenuEnabled(false);
        webView.getEngine().setJavaScriptEnabled(true);
    }

    @Override
    public void executeScript(final String code, final Consumer<Object> resultConsumer) {
        final Object result = webView.getEngine().executeScript(code);

        if (resultConsumer != null) {
            resultConsumer.accept(result);
        }
    }

    @Override
    public void executeScript(final String code) {
        executeScript(code, null);
    }

    @Override
    public String getDocumentFromScript() {
        return "document";
    }

    @Override
    public Parent toNode() {
        return webView;
    }

    @Override
    public void load(final String url) {
        webView.getEngine().load(url);
    }

    @Override
    public void setOnLoaded(final Runnable onLoadedTask) {
        if (loadingStateListener != null) {
            webView.getEngine().getLoadWorker().stateProperty().removeListener(loadingStateListener);
            loadingStateListener = null;
        }

        if (onLoadedTask != null) {
            loadingStateListener = (observable, oldValue, newValue) -> executeScript("window", o -> {
                JSObject window = (JSObject) o;
                window.setMember("java", new JSLogListener());
                executeScript("console.log = function(message){ try {java.log(message);} catch(e) {} };");

                if (newValue == Worker.State.SUCCEEDED) {
                    onLoadedTask.run();
                }
            });

            webView.getEngine().getLoadWorker()
                    .stateProperty()
                    .addListener(loadingStateListener);
        }
    }
}
