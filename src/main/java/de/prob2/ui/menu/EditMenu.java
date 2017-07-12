package de.prob2.ui.menu;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.scripting.ModelTranslationError;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class EditMenu extends Menu {
	private static final Logger logger = LoggerFactory.getLogger(EditMenu.class);

	@FXML
	private MenuItem reloadMachineItem;
	@FXML
	private MenuItem preferencesItem;
	
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StageManager stageManager;

	@Inject
	private EditMenu(final StageManager stageManager,
			final CurrentTrace currentTrace, final Injector injector) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "editMenu.fxml");
	}

	@FXML
	public void initialize() {
		this.reloadMachineItem.disableProperty().bind(currentTrace.existsProperty().not());
	}

	@FXML
	private void handleReloadMachine() {
		try {
			this.currentTrace.reload(this.currentTrace.get());
		} catch (IOException | ModelTranslationError e) {
			logger.error("Model reload failed", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to reload model:\n" + e).showAndWait();
		}
	}

	@FXML
	private void handlePreferences() {
		final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
		preferencesStage.show();
		preferencesStage.toFront();
	}

	MenuItem getPreferencesItem() {
		return preferencesItem;
	}
}
