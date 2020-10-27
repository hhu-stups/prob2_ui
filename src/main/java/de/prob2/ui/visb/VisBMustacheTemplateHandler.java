package de.prob2.ui.visb;

import de.prob2.ui.internal.MustacheTemplateManager;
import de.prob2.ui.visb.visbobjects.VisBEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class VisBMustacheTemplateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisBMustacheTemplateHandler.class);

    public static String getQueryString(VisBEvent visBEvent, StringBuilder enterAction, StringBuilder leaveAction) {
        try {
            URI uri = VisBMustacheTemplateHandler.class.getResource("on_click_event_query.mustache").toURI();
            MustacheTemplateManager templateManager = new MustacheTemplateManager(uri, "model_not_initialised");
            templateManager.put("eventID", visBEvent.getId());
            templateManager.put("eventName", visBEvent.getEvent());
            templateManager.put("enterAction", enterAction.toString());
            templateManager.put("leaveAction", leaveAction.toString());
            return templateManager.apply();
        } catch (URISyntaxException e) {
            LOGGER.error("", e);
            return "";
        }
    }

    public static String getModelNotInitialisedString(File jsonFile) {
        try {
            URI uri = VisBMustacheTemplateHandler.class.getResource("model_not_initialised.mustache").toURI();
            MustacheTemplateManager templateManager = new MustacheTemplateManager(uri, "model_not_initialised");
            templateManager.put("jsonFile", jsonFile);
            return templateManager.apply();
        } catch (URISyntaxException e) {
            LOGGER.error("", e);
            return "";
        }
    }

}
