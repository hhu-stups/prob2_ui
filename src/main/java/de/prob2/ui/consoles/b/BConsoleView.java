package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.CSPModel;
import de.prob.model.representation.XTLModel;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

@FXMLInjected
@Singleton
public class BConsoleView extends VBox {

	private final CurrentTrace currentTrace;
	private final I18n i18n;

	@FXML
	private BConsole bConsole;
	@FXML
	private Label languageLabel;
	@FXML
	private HelpButton helpButton;

	@Inject
	private BConsoleView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace) {
		super();
		this.i18n = i18n;
		this.currentTrace = currentTrace;

		stageManager.loadFXML(this, "b_console_view.fxml");
	}

	@FXML
	private void initialize() {
		this.currentTrace.addListener((o, from, to) -> {
			final String lang;
			final String prompt;
			if (to == null) {
				lang = "consoles.b.toolbar.language.classicalB";
				prompt = "consoles.b.prompt.classicalB";
			} else {
				final AbstractModel model = to.getModel();
				if (model instanceof EventBModel) {
					lang = "consoles.b.toolbar.language.eventB";
					prompt = "consoles.b.prompt.eventB";
				} else if (model instanceof CSPModel) {
					lang = "consoles.b.toolbar.language.csp";
					prompt = "consoles.b.prompt.csp";
				} else if (model instanceof XTLModel) {
					lang = "consoles.b.toolbar.language.xtl";
					prompt = "consoles.b.prompt.xtl";
				} else {
					lang = "consoles.b.toolbar.language.classicalB";
					prompt = "consoles.b.prompt.classicalB";
				}
			}

			Platform.runLater(() -> {
				this.languageLabel.setText(i18n.translate(lang));
				this.bConsole.setPrompt(prompt);
			});
		});
		helpButton.setHelpContent("mainView.bconsole", null);
	}

	@FXML
	private void handleClear() {
		bConsole.reset();
	}
}
