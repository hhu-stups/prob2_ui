package de.prob2.ui.vomanager;

import com.google.inject.Inject;

import de.prob.voparser.VOParseException;
import de.prob2.ui.internal.StageManager;

import javafx.scene.control.Alert;
import javafx.stage.Window;

public class VOErrorHandler {

	private final StageManager stageManager;

	@Inject
	public VOErrorHandler(final StageManager stageManager) {
		this.stageManager = stageManager;
	}

	public void handleError(Window window, VOParseException exception) {
		final Alert alert = stageManager.makeExceptionAlert(exception, "vomanager.error.parsing.header", "vomanager.error.parsing.content");
		alert.initOwner(window);
		alert.show();
	}

}
