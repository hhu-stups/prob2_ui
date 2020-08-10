package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

@FXMLInjected
public final class NavigationButtons extends HBox {
	@FXML private Button backButton;
	@FXML private Button fastBackButton;
	@FXML private Button forwardButton;
	@FXML private Button fastForwardButton;
	@FXML private Button reloadButton;

	private final CurrentTrace currentTrace;

	@Inject
	private NavigationButtons(final StageManager stageManager, final CurrentTrace currentTrace) {
		super();
		this.currentTrace = currentTrace;

		stageManager.loadFXML(this, "navigation_buttons.fxml");
	}

	@FXML
	private void initialize() {
		backButton.disableProperty().bind(currentTrace.canGoBackProperty().not());
		fastBackButton.disableProperty().bind(currentTrace.canGoBackProperty().not());
		forwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not());
		fastForwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not());
	}

	@FXML
	private void handleBackButton() {
		final Trace trace = currentTrace.get();
		if (trace != null) {
			currentTrace.set(trace.back());
		}
	}

	@FXML
	private void handleFastBackButton() {
		final Trace trace = currentTrace.get();
		if (trace != null) {
			currentTrace.set(trace.gotoPosition(-1));
		}
	}

	@FXML
	private void handleForwardButton() {
		final Trace trace = currentTrace.get();
		if (trace != null) {
			currentTrace.set(trace.forward());
		}
	}

	@FXML
	private void handleFastForwardButton() {
		final Trace trace = currentTrace.get();
		if (trace != null) {
			currentTrace.set(trace.gotoPosition(trace.size()-1));
		}
	}
}
