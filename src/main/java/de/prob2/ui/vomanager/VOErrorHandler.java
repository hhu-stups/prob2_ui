package de.prob2.ui.vomanager;

import com.google.inject.Inject;

import de.prob.voparser.VOException;
import de.prob.voparser.VOParseException;
import de.prob.voparser.VOTypeCheckException;
import de.prob2.ui.internal.StageManager;

import javafx.scene.control.Alert;
import javafx.stage.Window;

public class VOErrorHandler {

	private final StageManager stageManager;

	@Inject
	public VOErrorHandler(final StageManager stageManager) {
		this.stageManager = stageManager;
	}

	public void handleError(Window window, VOException exception) {
		String headerKey = "vomanager.error.parsing.header";
		String contentKey;
		if (exception instanceof VOParseException) {
			contentKey = "vomanager.error.parsing.content";
		} else if (exception instanceof VOTypeCheckException) {
			contentKey = "vomanager.error.typechecking";
		} else {
			contentKey = "vomanager.error.generic";
		}
		final Alert alert = stageManager.makeExceptionAlert(exception, headerKey, contentKey);
		alert.initOwner(window);
		alert.show();
	}

}
