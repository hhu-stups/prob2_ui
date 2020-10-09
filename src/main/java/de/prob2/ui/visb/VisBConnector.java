package de.prob2.ui.visb;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The VisBConnector is used as a connection between the JavaFX Webview and the JavaScript Globals.
 */
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
	public void click(String id, int pageX, int pageY, boolean shiftKey, boolean metaKey){
	    // probably pageX,pageY is the one to use as they do not change when scrolling and are relative to the SVG
		LOGGER.debug("\nSVG Element with ID "+id+" was clicked at page position " + pageX + "," + pageY 
		                + " with shift "+shiftKey + " cmd/meta " + metaKey); // 1=left, 2=middle, 3=right
		this.injector.getInstance(VisBController.class).executeEvent(id,pageX,pageY,shiftKey,metaKey);
	}
}
