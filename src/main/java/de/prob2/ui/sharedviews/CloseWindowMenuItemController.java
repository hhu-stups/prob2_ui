package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public final class CloseWindowMenuItemController {
	private final StageManager stageManager;
	
	@Inject
	private CloseWindowMenuItemController(final StageManager stageManager) {
		super();
		
		this.stageManager = stageManager;
	}

	@FXML
	private void handleCloseWindow() {
		final Stage stage = this.stageManager.getCurrent();
		if (stage != null) {
			stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
		}
	}
}
