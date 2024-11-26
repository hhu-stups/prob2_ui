package de.prob2.ui.consoles.b;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.CSPModel;
import de.prob.model.representation.TLAModel;
import de.prob.model.representation.XTLModel;
import de.prob.statespace.Language;
import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;

import org.controlsfx.control.SearchableComboBox;
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
	private final ObservableList<String> history;

	private CodeCompletion<BCCItem> codeCompletion;

	@FXML
	private CodeArea consoleHistory;
	@FXML
	private ExtendedCodeArea consoleInput;
	@FXML
	private SearchableComboBox<String> historyDropdown;
	@FXML
	private ComboBox<Language> languageDropdown;
	@FXML
	private Label promptLabel;
	@FXML
	private HelpButton helpButton;

	@Inject
	private BConsoleView(StageManager stageManager, I18n i18n, CurrentTrace currentTrace, BInterpreter interpreter, Config config) {
		super();
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.interpreter = interpreter;
		this.history = FXCollections.observableArrayList();

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(ConfigData configData) {
				if (configData.bConsoleInstructions != null) {
					List<String> bConsoleInstructions = new ArrayList<>(configData.bConsoleInstructions);
					Collections.reverse(bConsoleInstructions);
					BConsoleView.this.history.setAll(bConsoleInstructions);
				}
			}

			@Override
			public void saveConfig(ConfigData configData) {
				List<String> history = new ArrayList<>(BConsoleView.this.history);
				Collections.reverse(history);
				configData.bConsoleInstructions = history;
			}
		});
		stageManager.loadFXML(this, "b_console_view.fxml");
	}

	@FXML
	private void initialize() {
		this.languageDropdown.setConverter(this.i18n.translateConverter(TranslatableAdapter.adapter(l -> switch (l) {
			case CLASSICAL_B -> "consoles.b.toolbar.language.classicalB";
			case EVENT_B -> "consoles.b.toolbar.language.eventB";
			case TLA -> "consoles.b.toolbar.language.tla";
			case CSP -> "consoles.b.toolbar.language.csp";
			case XTL -> "consoles.b.toolbar.language.xtl";
			default -> throw new IllegalArgumentException("Unsupported language " + l);
		})));
		var prompt = this.languageDropdown.getSelectionModel().selectedItemProperty()
				.map(l -> switch (l) {
					case CLASSICAL_B -> "consoles.b.prompt.classicalB";
					case EVENT_B -> "consoles.b.prompt.eventB";
					case TLA -> "consoles.b.prompt.tla";
					case CSP -> "consoles.b.prompt.csp";
					case XTL -> "consoles.b.prompt.xtl";
					default -> throw new IllegalArgumentException("Unsupported language " + l);
				})
				.orElse("consoles.b.prompt.classicalB");
		this.promptLabel.textProperty().bind(this.i18n.translateBinding(prompt));
		this.languageDropdown.getSelectionModel().selectedItemProperty()
				.map(Language.CLASSICAL_B::equals)
				.orElse(true)
				.subscribe(this.interpreter::setBMode);

		this.currentTrace.stateSpaceProperty().subscribe(ss -> {
			Language selectedItem = this.languageDropdown.getSelectionModel().getSelectedItem();

			List<Language> languages;
			if (ss == null) {
				languages = List.of(Language.CLASSICAL_B);
			} else {
				AbstractModel model = ss.getModel();
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

				if (model != null) {
					String name;
					AbstractElement mainComponent = model.getMainComponent();
					if (mainComponent != null) {
						name = mainComponent.toString();
					} else {
						File modelFile = model.getModelFile();
						if (modelFile != null) {
							name = modelFile.getName();
						} else {
							name = this.i18n.translate("common.notAvailable");
						}
					}
					this.appendHistoryWithoutDuplicates(this.i18n.translate("consoles.b.message.modelLoaded", name), Set.of("console", "message"));
				}
			}
			this.languageDropdown.getItems().setAll(languages);

			if (this.languageDropdown.getItems().contains(selectedItem)) {
				this.languageDropdown.getSelectionModel().select(selectedItem);
			} else {
				this.languageDropdown.getSelectionModel().selectFirst();
			}
		});
		this.handleClear();

		this.helpButton.setHelpContent("mainView.bconsole", null);

		this.consoleHistory.getStyleClass().add("console");
		this.consoleHistory.setUndoManager(null);
		this.initializeHistoryContextMenu();
		this.consoleHistory.setEditable(false);
		this.consoleHistory.setWrapText(true);
		Nodes.addInputMap(this.consoleInput, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), e -> this.consoleInput.insertText(this.consoleInput.getCaretPosition(), "\n")));
		Nodes.addInputMap(this.consoleInput, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), e -> this.trigger()));

		// code completion
		this.codeCompletion = new CodeCompletion<>(
				this.stageManager,
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
				this.interpreter::getSuggestions
		);
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e -> this.triggerCodeCompletion()));

		// history
		Bindings.bindContent(this.historyDropdown.getItems(), this.history);
		this.historyDropdown.getSelectionModel().selectedItemProperty().subscribe(s -> {
			if (s != null) {
				this.consoleInput.replaceText(s);
				Platform.runLater(() -> {
					this.historyDropdown.getSelectionModel().clearSelection();
					this.consoleInput.requestFocus();
				});
			}
		});
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

		if (input.isBlank()) {
			return;
		}

		this.appendHistory(this.promptLabel.getText() + input, Set.of("console", "input"));
		this.history.add(0, input);

		ConsoleExecResult result = this.interpreter.exec(input);
		if (result.getResultType() == ConsoleExecResultType.CLEAR) {
			this.handleClear();
			return;
		}

		Collection<String> style = result.getResultType() == ConsoleExecResultType.ERROR ? Set.of("console", "error", "output") : Set.of("console", "output");
		this.appendHistory(result.getResult(), style);

		this.consoleHistory.requestFollowCaret();
	}

	@FXML
	private void handleClear() {
		this.consoleHistory.clear();
		this.appendHistory(this.i18n.translate("consoles.b.header"), Set.of("console", "header"));
	}

	private void triggerCodeCompletion() {
		if (this.consoleInput.isEditable()) {
			this.codeCompletion.trigger();
		}
	}

	private void appendHistory(String paragraph, Collection<String> style) {
		this.consoleHistory.append(paragraph + "\n", style);
	}

	private void appendHistoryWithoutDuplicates(String paragraph, Collection<String> style) {
		var paragraphs = this.consoleHistory.getParagraphs();
		int i = paragraphs.size() - 1;
		while (i >= 0) {
			String lastParagraph = paragraphs.get(i).getText();
			if (paragraph.equals(lastParagraph)) {
				return;
			}

			if (!lastParagraph.isBlank()) {
				break;
			}

			i--;
		}

		this.appendHistory(paragraph, style);
	}
}
