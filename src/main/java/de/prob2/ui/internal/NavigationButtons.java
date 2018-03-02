package de.prob2.ui.internal;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.menu.FileMenu;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public final class NavigationButtons extends HBox {
	@FXML private Button backButton;
	@FXML private Button fastBackButton;
	@FXML private Button forwardButton;
	@FXML private Button fastForwardButton;
	@FXML private Button reloadButton;

	private final CurrentTrace currentTrace;
	private final Injector injector;

	@Inject
	private NavigationButtons(final StageManager stageManager, final CurrentTrace currentTrace, final Injector injector) {
		super();

		this.currentTrace = currentTrace;
		this.injector = injector;

		stageManager.loadFXML(this, "navigation_buttons.fxml");
	}

	@FXML
	private void initialize() {
		backButton.disableProperty().bind(currentTrace.canGoBackProperty().not());
		fastBackButton.disableProperty().bind(currentTrace.canGoBackProperty().not());
		forwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not());
		fastForwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not());
		reloadButton.disableProperty().bind(currentTrace.existsProperty().not());
	}

	@FXML
	private void handleBackButton() {
		if (currentTrace.exists()) {
			currentTrace.set(currentTrace.back());
		}
	}

	@FXML
	private void handleFastBackButton() {
		if (currentTrace.exists()) {
			while(currentTrace.canGoBack()) {
				currentTrace.set(currentTrace.back());
			}
		}
	}

	@FXML
	private void handleForwardButton() {
		if (currentTrace.exists()) {
			currentTrace.set(currentTrace.forward());
		}
	}

	@FXML
	private void handleFastForwardButton() {
		if (currentTrace.exists()) {
			while(currentTrace.canGoForward()) {
				currentTrace.set(currentTrace.forward());
			}
		}
	}

	@FXML
	private void handleReloadButton() {
		injector.getInstance(FileMenu.class).handleReloadMachine();
	}
}
