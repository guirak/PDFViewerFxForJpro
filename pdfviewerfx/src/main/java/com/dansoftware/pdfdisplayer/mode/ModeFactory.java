package com.dansoftware.pdfdisplayer.mode;

import com.jpro.webapi.WebAPI;

/**
 * Factory to get the mode features for JPro or native JavaFX apps
 */
public class ModeFactory {

    /**
     * Private constructor
     */
    private ModeFactory(){

    }

    /**
     * Create the mode depending on where is running the app
     * @return The mode created
     */
    public static IMode create(){
        IMode mode;
        if (WebAPI.isBrowser()) {
            mode = new JProMode();
        }
        else {
            mode = new NativeMode();
        }
        return mode;
    }
}
