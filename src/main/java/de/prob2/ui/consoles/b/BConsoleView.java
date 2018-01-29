package de.prob2.ui.consoles.b;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.CSPModel;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

@Singleton
public class BConsoleView extends VBox {
	private final CurrentTrace currentTrace;
	
	@FXML private ResourceBundle bundle;
	@FXML private BConsole bConsole;
	@FXML private Label languageLabel;
	
	@Inject
	private BConsoleView(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace) {
		super();
		
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		
		stageManager.loadFXML(this, "b_console_view.fxml");
	}
	
	@FXML
	private void initialize() {
		this.currentTrace.addListener((o, from, to) -> {
			final String text;
			if (to == null) {
				text = this.bundle.getString("consoles.b.toolbar.language.classicalB");
			} else {
				final AbstractModel model = to.getModel();
				if (model instanceof EventBModel) {
					text = this.bundle.getString("consoles.b.toolbar.language.eventB");
				} else if (model instanceof CSPModel) {
					text = this.bundle.getString("consoles.b.toolbar.language.csp");
				} else {
					text = this.bundle.getString("consoles.b.toolbar.language.classicalB");
				}
			}
			Platform.runLater(() -> this.languageLabel.setText(text));
		});
	}
	
	@FXML
	private void handleClear() {
		bConsole.reset();
	}
}
