package com.azzimov.search.controller;

import com.azzimov.search.controller.SearchHandler;
import org.springframework.stereotype.Component;

/**
 * Created by RahulGupta on 2017-11-14.
 */

@Component
public class AccentSearchHandler implements SearchHandler {
    @Override
    public String getMessage() {
        return "Accent Search Handler";
    }
}
