package de.prob2.ui.visb;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by
 * @author Michelle Werth
 * @since 21.03.2019
 * @version 0.1.0
 *
 * The VisBConnector is used as a connection between the JavaFX Webview and the JavaScript Globals.
 * */

@Singleton
public class VisBConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisBConnector.class);
    private Injector injector;

    @Inject
    public VisBConnector(Injector injector){
        this.injector = injector;
    }

    /**
     * Whenever a svg item, that has an event in the JSON / VisB file is clicked, this method redirects the click towards the {@link VisBController}
     * @param id of the svg item, that is clicked
     */
    public void click(String id){
        LOGGER.debug("SVG Element with ID "+id+" was clicked.");
        this.injector.getInstance(VisBController.class).executeEvent(id);
    }
}
