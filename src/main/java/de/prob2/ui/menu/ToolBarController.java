package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob2.ui.config.Config;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.preferences.PreferencesStage;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class ToolBarController {

	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	private final I18n i18n;
	@FXML
	private final FontSize fontSize;


	@Inject
	public ToolBarController(Injector injector, StageManager stageManager, UIState uiState, I18n i18n, FontSize fontsize) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.uiState = uiState;
		this.i18n = i18n;
		this.fontSize = fontsize;
	}

	@FXML
	private void handlePreferences() {
		final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
		preferencesStage.show();
		preferencesStage.toFront();
	}

	@FXML
	private void zoomIn() {
		fontSize.setFontSize(fontSize.getFontSize() + 1);
	}

	@FXML
	private void zoomOut() {
		fontSize.setFontSize(fontSize.getFontSize() - 1);
	}
}
