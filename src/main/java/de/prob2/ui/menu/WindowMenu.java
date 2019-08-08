package de.prob2.ui.menu;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.codecentric.centerdevice.MenuToolkit;
import de.codecentric.centerdevice.util.StageUtils;
import de.prob2.ui.MainController;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIState;

import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@FXMLInjected
public class WindowMenu extends Menu {
	private final Injector injector;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;

	@FXML
	private Menu presetPerspectivesMenu;
	@FXML
	private MenuItem detatchedMenuItem;

	@Inject
	private WindowMenu(final StageManager stageManager, final Injector injector, final ResourceBundle bundle,
			final FileChooserManager fileChooserManager, @Nullable MenuToolkit menuToolkit) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
		stageManager.loadFXML(this, "windowMenu.fxml");

		if (menuToolkit != null) {
			MenuItem zoomMenuItem = menuToolkit.createZoomMenuItem();
			zoomMenuItem.setOnAction(
					event -> StageUtils.getFocusedStage().ifPresent(stage -> {
						if(!stage.isMaximized()) {
							stage.setMaximized(true);
						} else {
							stage.sizeToScene();
							stage.setMaximized(false);
							stage.centerOnScreen();
						}
					}));

			this.getItems().addAll(0, Arrays.asList(menuToolkit.createMinimizeMenuItem(), zoomMenuItem,
					menuToolkit.createCycleWindowsItem(), new SeparatorMenuItem()));
			this.getItems().addAll(new SeparatorMenuItem(), menuToolkit.createBringAllToFrontItem(),
					new SeparatorMenuItem());
			menuToolkit.autoAddWindowMenuItems(this);
		}
	}

	@FXML
	private void handleCloseWindow() {
		final Stage stage = this.stageManager.getCurrent();
		if (stage != null) {
			stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
		}
	}

	@FXML
	private void handleLoadDefault() {
		reset();
		loadPreset("main.fxml");
	}

	@FXML
	private void handleLoadSeparated() {
		reset();
		loadPreset("separatedHistory.fxml");
	}

	@FXML
	private void handleLoadSeparated2() {
		reset();
		loadPreset("separatedHistoryAndStatistics.fxml");
	}

	@FXML
	private void handleLoadDetached() {
		injector.getInstance(DetachViewStageController.class).showAndWait();
	}

	@FXML
	private void handleLoadPerspective() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("common.fileChooser.open.title"));
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter(bundle.getString("common.fileChooser.fileTypes.fxml"), "*.fxml"));
		Path selectedFile = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.PERSPECTIVES,
				stageManager.getMainStage());
		if (selectedFile != null) {
			reset();
			loadPreset("custom " + selectedFile.toUri());
		}
	}

	private void reset() {
		injector.getInstance(DetachViewStageController.class).attachAllViews();
	}

	public void loadPreset(String location) {
		injector.getInstance(UIState.class).setGuiState(location);
		injector.getInstance(MainController.class).refresh();
	}

	public void enablePerspectivesAndDetatched() {
		presetPerspectivesMenu.setDisable(false);
		presetPerspectivesMenu.setVisible(true);
		detatchedMenuItem.setDisable(false);
		detatchedMenuItem.setVisible(true);
	}
}
