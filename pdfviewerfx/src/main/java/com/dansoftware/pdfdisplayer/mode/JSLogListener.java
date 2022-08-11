package com.dansoftware.pdfdisplayer.mode;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@NoArgsConstructor
@Slf4j
public class JSLogListener {
    public void log(final String message) throws IOException {
        if (message != null) {
            log.error(message);
        }
    }
}
