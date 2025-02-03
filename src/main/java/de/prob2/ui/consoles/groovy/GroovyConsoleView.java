package de.prob2.ui.consoles.groovy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.FxThreadExecutor;

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
import javafx.scene.layout.VBox;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
@Singleton
public final class GroovyConsoleView extends BorderPane {

	private final I18n i18n;
	private final FxThreadExecutor fxExecutor;
	private final ObservableList<String> history;

	@FXML
	private VBox consoleContainer;
	@FXML
	private CodeArea consoleHistory;
	@FXML
	private GroovyConsoleInput consoleInput;
	@FXML
	private ComboBox<String> historyDropdown;
	@FXML
	private Label promptLabel;

	private String savedText;
	private int historyPos;

	@Inject
	private GroovyConsoleView(StageManager stageManager, I18n i18n, Config config, FxThreadExecutor fxExecutor) {
		super();
		this.i18n = i18n;
		this.fxExecutor = fxExecutor;
		this.history = FXCollections.observableArrayList();

		this.savedText = "";
		this.historyPos = -1;

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(ConfigData configData) {
				if (configData.groovyConsoleInstructions != null) {
					List<String> history = new ArrayList<>(configData.groovyConsoleInstructions);
					Collections.reverse(history);
					GroovyConsoleView.this.history.setAll(history);
				}
			}

			@Override
			public void saveConfig(ConfigData configData) {
				List<String> history = new ArrayList<>(GroovyConsoleView.this.history);
				Collections.reverse(history);
				configData.groovyConsoleInstructions = history;
			}
		});
		stageManager.loadFXML(this, "groovy_console_view.fxml");
	}

	@FXML
	private void initialize() {
		this.promptLabel.textProperty().bind(this.i18n.translateBinding("consoles.groovy.prompt"));
		this.handleClear();

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
		this.historyDropdown.promptTextProperty().bind(this.i18n.translateBinding("consoles.groovy.history"));
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

		this.consoleInput.setAutoHeight(true);
		this.consoleInput.maxHeightProperty().bind(this.consoleContainer.heightProperty().divide(2.0));
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
		this.consoleInput.getGroovyInterpreter().exec(input)
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
		this.appendHistory(this.i18n.translate("consoles.groovy.header"), Set.of("console", "header"));
	}

	private void appendHistory(String paragraph, Collection<String> style) {
		this.consoleHistory.append(paragraph + "\n", style);
	}

	public void closeObjectStage() {
		this.consoleInput.closeObjectStage();
	}
}
