package de.prob2.ui.visb;

import de.prob2.ui.internal.MustacheTemplateManager;
import de.prob2.ui.visb.visbobjects.VisBEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

public class VisBMustacheTemplateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisBMustacheTemplateHandler.class);

    public static String getQueryString(VisBEvent visBEvent) {
        String enterAction = visBEvent.getHovers().stream()
                .map(hover -> getChangeAttributeString(hover.getHoverId(), hover.getHoverAttr(), hover.getHoverEnterVal()))
                .collect(Collectors.joining("\n"));
        String leaveAction = visBEvent.getHovers().stream()
                .map(hover -> getChangeAttributeString(hover.getHoverId(), hover.getHoverAttr(), hover.getHoverLeaveVal()))
                .collect(Collectors.joining("\n"));
        try {
            URI uri = VisBMustacheTemplateHandler.class.getResource("on_click_event_query.mustache").toURI();
            MustacheTemplateManager templateManager = new MustacheTemplateManager(uri, "on_click_event_query");
            templateManager.put("eventID", visBEvent.getId());
            templateManager.put("eventName", visBEvent.getEvent());
            templateManager.put("enterAction", enterAction);
            templateManager.put("leaveAction", leaveAction);
            return templateManager.apply();
        } catch (URISyntaxException e) {
            LOGGER.error("", e);
            return "";
        }
    }

    public static String getChangeAttributeString(String id, String attr, String value) {
        try {
            URI uri = VisBMustacheTemplateHandler.class.getResource("change_attribute.mustache").toURI();
            MustacheTemplateManager templateManager = new MustacheTemplateManager(uri, "change_attribute");
            templateManager.put("id", id);
            templateManager.put("attr", attr);
            templateManager.put("value", value);
            return templateManager.apply();
        } catch (URISyntaxException e) {
            LOGGER.error("", e);
            return "";
        }
    }

    public static String generateHTMLFileWithSVG(String jqueryLink, String clickEvents, File jsonFile, String svgContent) {
        try {
            URI uri = VisBMustacheTemplateHandler.class.getResource("visb_html_view.mustache").toURI();
            MustacheTemplateManager templateManager = new MustacheTemplateManager(uri, "visb_html_view");
            templateManager.put("jqueryLink", jqueryLink);
            templateManager.put("clickEvents", clickEvents);
            templateManager.put("jsonFile", jsonFile);
            templateManager.put("svgContent", svgContent);
            return templateManager.apply();
        } catch (URISyntaxException e) {
            LOGGER.error("", e);
            return "";
        }
    }

}
