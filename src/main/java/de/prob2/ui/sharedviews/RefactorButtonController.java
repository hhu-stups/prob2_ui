package de.prob2.ui.sharedviews;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.animation.tracereplay.refactoring.RefactorSetupView;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public final class RefactorButtonController {
	private final Injector injector;

	@FXML
	private Button button;

	@Inject
	private RefactorButtonController(Injector injector) {
		super();
		this.injector = injector;
	}

	@FXML
	private void initialize() {
		button.setOnAction(event -> injector.getInstance(RefactorSetupView.class).showAndPerformAction());
	}
}
