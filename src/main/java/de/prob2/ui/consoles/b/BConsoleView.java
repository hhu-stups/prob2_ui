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
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.TranslatableAdapter;
import de.prob2.ui.internal.executor.FxThreadExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
@Singleton
public final class BConsoleView extends BorderPane {

	private final I18n i18n;
	private final FxThreadExecutor fxExecutor;
	private final CurrentTrace currentTrace;
	private final ObservableList<String> history;

	@FXML
	private SplitPane consoleContainer;
	@FXML
	private Region inputContainer;
	@FXML
	private CodeArea consoleHistory;
	@FXML
	private BConsoleInput consoleInput;
	@FXML
	private ComboBox<String> historyDropdown;
	@FXML
	private ComboBox<Language> languageDropdown;
	@FXML
	private Label promptLabel;
	@FXML
	private HelpButton helpButton;

	private String savedText;
	private int historyPos;
	/**
	 * Keep as field!
	 * Stops the listeners from being garbage collected.
	 */
	private DoubleBinding inputPrefHeight;

	@Inject
	private BConsoleView(StageManager stageManager, I18n i18n, CurrentTrace currentTrace, Config config, FxThreadExecutor fxExecutor) {
		super();
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.fxExecutor = fxExecutor;
		this.history = FXCollections.observableArrayList();

		this.savedText = "";
		this.historyPos = -1;

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(ConfigData configData) {
				if (configData.bConsoleInstructions != null) {
					List<String> history = new ArrayList<>(configData.bConsoleInstructions);
					Collections.reverse(history);
					BConsoleView.this.history.setAll(history);
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
				.subscribe(this.consoleInput.getBInterpreter()::setBMode);

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
		Nodes.addInputMap(this.consoleHistory, InputMap.consume(EventPattern.keyTyped(), e -> {
			this.consoleInput.requestFocus();
			this.consoleInput.fireEvent(e);
		}));
		Nodes.addInputMap(this.consoleInput, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), e -> this.consoleInput.insertText(this.consoleInput.getCaretPosition(), "\n")));
		Nodes.addInputMap(this.consoleInput, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), e -> this.trigger()));

		// history with arrow keys
		Nodes.addInputMap(this.consoleInput, InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.UP), () -> this.consoleInput.getCurrentParagraph() <= 0, e -> {
			this.setHistoryPos(this.historyPos + 1);
		}));
		Nodes.addInputMap(this.consoleInput, InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.DOWN), () -> this.consoleInput.getCurrentParagraph() >= this.consoleInput.getParagraphs().size() - 1, e -> {
			this.setHistoryPos(this.historyPos - 1);
		}));

		// history dropdown
		this.historyDropdown.promptTextProperty().bind(this.i18n.translateBinding("consoles.b.history"));
		Bindings.bindContent(this.historyDropdown.getItems(), this.history);
		this.historyDropdown.getSelectionModel().selectedIndexProperty().subscribe(selected -> {
			if (selected == null) {
				return;
			}

			int s = (int) selected;
			if (s != -1) {
				this.consoleInput.requestFocus();
				this.setHistoryPos(s);
				this.consoleInput.requestFollowCaret();
				Platform.runLater(() -> {
					this.historyDropdown.getSelectionModel().clearSelection();
					// on my linux machine the "requestFocus" above causes text in the consoleHistory to become selected?!
					// this is caused by the history text area somehow receiving mouse dragged events,
					// even though the dropdown is shown and the focus is requested by the input text area
					Platform.runLater(() -> this.consoleHistory.deselect());
				});
			}
		});

		// required so the textarea calculates its prefHeight based on the text content
		this.consoleInput.setAutoHeight(true);
		this.inputPrefHeight = Bindings.createDoubleBinding(
				() -> this.consoleInput.prefHeight(-1),
				this.consoleInput.textProperty(), this.consoleInput.autoHeightProperty(), this.consoleInput.prefHeightProperty(), this.consoleInput.widthProperty(), this.consoleInput.prefWidthProperty()
		);
		this.inputPrefHeight.subscribe(prefHeight -> this.updateSplitPane());
		this.consoleContainer.heightProperty().subscribe(this::updateSplitPane);
	}

	private void updateSplitPane() {
		var prefHeight = this.inputPrefHeight.get();
		var containerHeight = this.consoleContainer.getHeight();
		if (prefHeight >= 0 && containerHeight > 0) {
			var height = clampHeight(this.inputContainer, prefHeight + 28); // add buffer so there is no scrollbar
			var divider = Math.max(0.5, 1.0 - height / containerHeight);
			if (this.consoleInput.getLength() == 0 || divider < this.consoleContainer.getDividerPositions()[0]) {
				this.consoleContainer.setDividerPositions(divider);
			}
		}
	}

	private static double clampHeight(Region r, double height) {
		if (r.getMinHeight() >= 0 && height < r.getMinHeight()) {
			height = r.getMinHeight();
		}
		if (r.getMaxHeight() >= 0 && height > r.getMaxHeight()) {
			height = r.getMaxHeight();
		}

		return height;
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

	private void setHistoryPos(int newHistoryPos) {
		if (this.historyPos == -1) {
			this.savedText = this.consoleInput.getText();
		}

		this.historyPos = Math.max(-1, Math.min(this.history.size(), newHistoryPos));
		if (this.historyPos == -1) {
			this.consoleInput.replaceText(this.savedText);
		} else {
			this.consoleInput.replaceText(this.history.get(this.historyPos));
		}
	}

	private void trigger() {
		String input = this.consoleInput.getText();
		this.consoleInput.clear();

		if (input.isBlank()) {
			return;
		}

		this.consoleHistory.append(this.promptLabel.getText(), Set.of("console", "input"));

		int pos = this.consoleHistory.getCaretPosition();
		this.consoleHistory.append(input, Set.of());
		var styleSpans = this.consoleInput.computeCodeHighlighting(input);
		this.consoleHistory.setStyleSpans(pos, styleSpans);
		this.consoleHistory.append("\n", Set.of());

		this.savedText = "";
		this.historyPos = -1;
		this.history.add(0, input);

		this.consoleInput.setEditable(false);
		this.consoleInput.getBInterpreter().exec(input)
				.handleAsync((result, t) -> {
					if (t != null) {
						return null;
					}

					if (result.resultType() == ConsoleExecResultType.CLEAR) {
						this.handleClear();
						return null;
					}

					Collection<String> style = result.resultType() == ConsoleExecResultType.ERROR ? Set.of("console", "error", "output") : Set.of("console", "output");
					this.appendHistory(result.result(), style);

					this.consoleHistory.requestFollowCaret();
					return null;
				}, this.fxExecutor)
				.thenRunAsync(() -> this.consoleInput.setEditable(true), this.fxExecutor);
	}

	@FXML
	private void handleClear() {
		this.savedText = "";
		this.historyPos = -1;
		this.consoleHistory.clear();
		this.appendHistory(this.i18n.translate("consoles.b.header"), Set.of("console", "header"));
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
