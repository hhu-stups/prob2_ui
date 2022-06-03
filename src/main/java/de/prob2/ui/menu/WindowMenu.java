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
import de.prob2.ui.error.ExceptionAlert;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.PerspectiveKind;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.persistence.UIState;

import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public class WindowMenu extends Menu {

	private static final Logger LOGGER = LoggerFactory.getLogger(WindowMenu.class);

	private final Injector injector;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;
	private final UIState uiState;

	@Inject
	private WindowMenu(
		final Injector injector,
		final StageManager stageManager,
		final ResourceBundle bundle,
		final FileChooserManager fileChooserManager,
		final UIState uiState,
		@Nullable MenuToolkit menuToolkit
	) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
		this.uiState = uiState;
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
	private void handleLoadDefault() {
		this.switchToPerspective(PerspectiveKind.PRESET, "main.fxml");
	}

	@FXML
	private void handleLoadSeparated() {
		this.switchToPerspective(PerspectiveKind.PRESET, "separatedHistory.fxml");
	}

	@FXML
	private void handleLoadSeparated2() {
		this.switchToPerspective(PerspectiveKind.PRESET, "separatedHistoryAndStatistics.fxml");
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
				fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.fxml", "fxml"));
		Path selectedFile = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.PERSPECTIVES,
				stageManager.getMainStage());
		if (selectedFile != null) {
			this.switchToPerspective(PerspectiveKind.CUSTOM, selectedFile.toUri().toString());
		}
	}

	private void switchToPerspective(final PerspectiveKind kind, final String perspective) {
		injector.getInstance(DetachViewStageController.class).attachAllViews();
		PerspectiveKind prevKind = this.uiState.getPerspectiveKind();
		String prevPerspective = this.uiState.getPerspective();

		try {
			this.uiState.setPerspectiveKind(kind);
			this.uiState.setPerspective(perspective);
			injector.getInstance(MainController.class).reloadMainView();
		} catch(Exception e) {
			this.uiState.setPerspectiveKind(prevKind);
			this.uiState.setPerspective(prevPerspective);
			LOGGER.error("Exception during switching perspective", e);
			stageManager.makeExceptionAlert(e,  "menu.window.switchingPerspective.error").show();
		}
	}
}
