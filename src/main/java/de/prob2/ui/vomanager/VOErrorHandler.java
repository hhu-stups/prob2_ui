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
		VOParseException.ErrorType errorType = exception.getErrorType();
		String headerKey = "vomanager.error.parsing.header";
		String contentKey;
		switch (errorType) {
			case PARSING:
				contentKey = "vomanager.error.parsing.content";
				break;
			case SCOPING:
				contentKey = "vomanager.error.scoping";
				break;
			case TYPECHECKING:
				contentKey = "vomanager.error.typechecking";
				break;
			default:
				throw new RuntimeException("VO parsing error type unknown: " + errorType);
		}
		final Alert alert = stageManager.makeExceptionAlert(exception, headerKey, contentKey);
		alert.initOwner(window);
		alert.show();
	}

}
