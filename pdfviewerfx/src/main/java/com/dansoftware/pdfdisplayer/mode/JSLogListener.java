package com.dansoftware.pdfdisplayer.mode;

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@NoArgsConstructor
@Log4j2
public class JSLogListener {
    public void log(final String message) throws IOException {
        if (message != null) {
            log.error(message);
        }
    }
}
