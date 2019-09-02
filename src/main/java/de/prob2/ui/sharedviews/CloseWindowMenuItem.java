package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@FXMLInjected
public final class CloseWindowMenuItem extends MenuItem {
	private final StageManager stageManager;
	
	@Inject
	private CloseWindowMenuItem(final StageManager stageManager) {
		super();
		
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "close_window_menu_item.fxml");
	}

	@FXML
	private void handleCloseWindow() {
		final Stage stage = this.stageManager.getCurrent();
		if (stage != null) {
			stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
		}
	}
}
