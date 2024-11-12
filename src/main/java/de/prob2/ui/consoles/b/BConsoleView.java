package de.prob2.ui.consoles.b;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.CSPModel;
import de.prob.model.representation.TLAModel;
import de.prob.model.representation.XTLModel;
import de.prob.statespace.Language;
import de.prob.statespace.Trace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.TranslatableAdapter;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

@FXMLInjected
@Singleton
public final class BConsoleView extends BorderPane {
	private final CurrentTrace currentTrace;
	private final I18n i18n;

	@FXML
	private ExtendedCodeArea consoleHistory;
	@FXML
	private ExtendedCodeArea consoleInput;
	@FXML
	private ComboBox<Language> languageDropdown;
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
		this.languageDropdown.setConverter(this.i18n.translateConverter(TranslatableAdapter.adapter(language -> switch (language) {
			case CLASSICAL_B -> "consoles.b.toolbar.language.classicalB";
			case EVENT_B -> "consoles.b.toolbar.language.eventB";
			case TLA -> "consoles.b.toolbar.language.tla";
			case CSP -> "consoles.b.toolbar.language.csp";
			case XTL -> "consoles.b.toolbar.language.xtl";
			default -> throw new IllegalArgumentException("Unsupported language " + language);
		})));
		this.languageDropdown.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if (to == null) {
				// this.bConsole.setPrompt("consoles.b.prompt.classicalB");
				// this.bConsole.getInterpreter().setBMode(true);
			} else {
				boolean bMode = false;
				String prompt = switch (to) {
					case CLASSICAL_B -> {
						bMode = true;
						yield "consoles.b.prompt.classicalB";
					}
					case EVENT_B -> "consoles.b.prompt.eventB";
					case TLA -> "consoles.b.prompt.tla";
					case CSP -> "consoles.b.prompt.csp";
					case XTL -> "consoles.b.prompt.xtl";
					default -> throw new IllegalArgumentException("Unsupported language " + to);
				};
				// this.bConsole.setPrompt(prompt);
				// this.bConsole.getInterpreter().setBMode(bMode);
			}
		});

		ChangeListener<Trace> traceListener = (o, from, to) -> {
			Language selectedItem = this.languageDropdown.getSelectionModel().getSelectedItem();

			List<Language> languages;
			if (to == null) {
				languages = List.of(Language.CLASSICAL_B);
			} else {
				AbstractModel model = to.getModel();
				if (model instanceof EventBModel) {
					languages = List.of(Language.EVENT_B, Language.CLASSICAL_B);
				} else if (model instanceof CSPModel) {
					languages = List.of(Language.CSP);
				} else if (model instanceof TLAModel) {
					languages = List.of(Language.TLA, Language.CLASSICAL_B);
				} else if (model instanceof XTLModel) {
					languages = List.of(Language.XTL);
				} else {
					languages = List.of(Language.CLASSICAL_B);
				}
			}
			this.languageDropdown.getItems().setAll(languages);

			if (this.languageDropdown.getItems().contains(selectedItem)) {
				this.languageDropdown.getSelectionModel().select(selectedItem);
			} else {
				this.languageDropdown.getSelectionModel().selectFirst();
			}
		};
		this.currentTrace.addListener(traceListener);
		traceListener.changed(null, null, null);

		helpButton.setHelpContent("mainView.bconsole", null);
	}

	@FXML
	private void handleClear() {
		//bConsole.reset();
	}
}
