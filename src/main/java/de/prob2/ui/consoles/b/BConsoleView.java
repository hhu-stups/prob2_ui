package de.prob2.ui.consoles.b;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.CSPModel;
import de.prob.model.representation.TLAModel;
import de.prob.model.representation.XTLModel;
import de.prob.statespace.Language;
import de.prob.statespace.Trace;
import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.consoles.b.codecompletion.BCCItem;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
@Singleton
public final class BConsoleView extends BorderPane {

	private final StageManager stageManager;
	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private final BInterpreter interpreter;

	private CodeCompletion<BCCItem> codeCompletion;

	@FXML
	private CodeArea consoleHistory;
	@FXML
	private ExtendedCodeArea consoleInput;
	@FXML
	private ComboBox<Language> languageDropdown;
	@FXML
	private HelpButton helpButton;

	@Inject
	private BConsoleView(StageManager stageManager, I18n i18n, CurrentTrace currentTrace, BInterpreter interpreter) {
		super();
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.interpreter = interpreter;

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

		this.consoleHistory.getStyleClass().add("console");
		this.consoleHistory.setUndoManager(null);
		this.initializeHistoryContextMenu();
		this.consoleHistory.setEditable(false);
		this.consoleHistory.setWrapText(true);
		Nodes.addInputMap(this.consoleInput, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), e -> this.consoleInput.insertText(this.consoleInput.getCaretPosition(), "\n")));
		Nodes.addInputMap(this.consoleInput, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), e -> this.trigger()));

		// code completion
		this.codeCompletion = new CodeCompletion<>(
				stageManager,
				this.consoleInput.new AbstractParentWithEditableText<>() {

					@Override
					public void doReplacement(BCCItem replacement) {
						if (!BConsoleView.this.consoleInput.isEditable()) {
							return;
						}

						int caret = BConsoleView.this.consoleInput.getCaretPosition();
						BConsoleView.this.consoleInput.replace(caret - replacement.getOriginalText().length(), caret, replacement.getReplacement(), Set.of());
					}
				},
				interpreter::getSuggestions
		);
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e -> this.triggerCodeCompletion()));
	}

	private void initializeHistoryContextMenu() {
		ContextMenu contextMenu = new ContextMenu();

		MenuItem copyItem = new MenuItem(this.i18n.translate("common.contextMenu.copy"));
		copyItem.setOnAction(e -> this.consoleHistory.copy());
		contextMenu.getItems().add(copyItem);

		MenuItem clearItem = new MenuItem(this.i18n.translate("common.contextMenu.clear"));
		clearItem.setOnAction(e -> this.handleClear());
		contextMenu.getItems().add(clearItem);

		MenuItem selectAllItem = new MenuItem(this.i18n.translate("common.contextMenu.selectAll"));
		selectAllItem.setOnAction(e -> this.consoleHistory.selectAll());
		contextMenu.getItems().add(selectAllItem);

		this.consoleHistory.setContextMenu(contextMenu);
	}

	private void trigger() {
		String input = this.consoleInput.getText();
		this.consoleInput.clear();

		String inputWithPrompt = "B> " + input; // TODO: add prompt
		this.consoleHistory.append(inputWithPrompt + "\n", Set.of("console", "input"));

		ConsoleExecResult result = this.interpreter.exec(input);
		if (result.getResultType() == ConsoleExecResultType.CLEAR) {
			this.handleClear();
			return;
		}

		Collection<String> style = result.getResultType() == ConsoleExecResultType.ERROR ? Set.of("console", "error", "output") : Set.of("console", "output");
		this.consoleHistory.append(result.getResult() + "\n", style);

		this.consoleHistory.requestFollowCaret();
	}

	@FXML
	private void handleClear() {
		this.consoleHistory.clear();
	}

	private void triggerCodeCompletion() {
		if (this.consoleInput.isEditable()) {
			this.codeCompletion.trigger();
		}
	}
}
