package de.prob2.ui.beditor;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;

import javafx.util.Duration;
import org.fxmisc.richtext.model.TwoDimensional;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class SearchPane extends AnchorPane {
	private record MatchPosition(int start, int end) { }

	private static final String STYLE_ERROR = "search-error";
	private static final String STYLE_NO_RESULTS = "search-no-results";
	private static final String STYLE_RESULTS = "search-has-results";

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchPane.class);

	private final I18n i18n;
	private BEditorView bEditorView;

	private final ObservableList<MatchPosition> matches = FXCollections.observableArrayList();
	private int currentMatch = 0;
	private final PauseTransition pause = new PauseTransition(Duration.millis(100));
	private final ChangeListener<String> textListener = (obs, ov, nv) -> pause.playFromStart();

	@FXML
	private TextField tfSearch;
	@FXML
	private ToggleButton btMatchCase;
	@FXML
	private ToggleButton btWordsOnly;
	@FXML
	private ToggleButton btRegex;
	@FXML
	private Label lblResults;
	@FXML
	private Button btPrev;
	@FXML
	private Button btNext;

	@Inject
	public SearchPane(StageManager stageManager, I18n i18n) {
		this.i18n = i18n;
		stageManager.loadFXML(this, "search_pane.fxml");
	}

	public void show(BEditorView bEditorView) {
		Objects.requireNonNull(bEditorView);
		if (this.bEditorView != bEditorView) {
			if (this.bEditorView != null) {
				this.bEditorView.getEditor().setSearchResults(null);
			}

			this.bEditorView = bEditorView;
		}

		this.startSearch();
	}

	public void startSearch() {
		this.tfSearch.textProperty().removeListener(textListener);
		this.requestFocus();

		String selected = this.bEditorView.getEditor().getSelectedText();
		if (!selected.isEmpty()) { // do not clear text if selection is empty
			this.tfSearch.setText(selected);
		}
		this.handleFind();
		if (!this.tfSearch.isFocused()) {
			this.tfSearch.requestFocus();
		}
		this.tfSearch.selectAll();
		this.tfSearch.textProperty().addListener(textListener);
	}

	@FXML
	private void initialize() {
		this.lblResults.getStyleClass().clear();

		pause.setOnFinished(e -> this.handleFind());

		btMatchCase.selectedProperty().addListener(getToggleButtonListener(btMatchCase));
		btWordsOnly.selectedProperty().addListener(getToggleButtonListener(btWordsOnly));
		btRegex.selectedProperty().addListener(getToggleButtonListener(btRegex));

		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN), e -> btMatchCase.fire()));
		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.W, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN), e -> btWordsOnly.fire()));
		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.X, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN), e -> btRegex.fire()));
		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), e -> handleGotoNext(false)));
		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), e -> handleGotoPrevious(false)));
	}

	private ChangeListener<Boolean> getToggleButtonListener(ToggleButton button) {
		return (obs, ov, nv) -> {
			if (nv) {
				button.getGraphic().setStyle("-fx-background-color: lightgray; -fx-background-radius: 4;");
			} else {
				button.getGraphic().setStyle("");
			}
			pause.playFromStart();
		};
	}

	@FXML
	private void handleGotoPrevious() {
		handleGotoPrevious(false);
	}

	void handleGotoPrevious(boolean focusEditor) {
		if (matches.isEmpty()) {
			return;
		}
		this.currentMatch--;
		if (currentMatch < 0) {
			this.currentMatch = matches.size() - 1;
		}
		this.gotoMatch(currentMatch, focusEditor);
	}

	@FXML
	private void handleGotoNext() {
		handleGotoNext(false);
	}

	void handleGotoNext(boolean focusEditor) {
		if (matches.isEmpty()) {
			return;
		}
		this.currentMatch++;
		if (currentMatch >= matches.size()) {
			this.currentMatch = 0;
		}
		this.gotoMatch(currentMatch, focusEditor);
	}

	@FXML
	private void handleFind() {
		if (this.bEditorView == null) {
			this.setResultText(STYLE_NO_RESULTS, "beditor.searchPane.resultLabel.noResults");
			return;
		}

		this.bEditorView.getEditor().setSearchResults(null);
		this.matches.clear();

		String searchText = this.tfSearch.getText();
		if (searchText.isEmpty()) {
			this.setResultText(STYLE_RESULTS, "beditor.searchPane.resultLabel.noResults");
			return;
		}

		// normal matching options
		boolean matchCase = this.btMatchCase.isSelected();
		boolean wordsOnly = this.btWordsOnly.isSelected();

		// regex options
		boolean regex = this.btRegex.isSelected();

		// Unicode support always enabled
		int flags = Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.CANON_EQ;
		if (!matchCase) {
			flags |= Pattern.CASE_INSENSITIVE;
		}
		if (!regex) {
			searchText = Pattern.quote(searchText);
		}
		if (wordsOnly) {
			searchText = "\\b" + searchText + "\\b";
		}

		Pattern searchPattern;
		try {
			searchPattern = Pattern.compile(searchText, flags);
		} catch (PatternSyntaxException e) {
			LOGGER.trace("unable to parse search string regex", e);
			this.setResultText(STYLE_ERROR, "beditor.searchPane.resultLabel.error");
			return;
		}

		String text = this.bEditorView.getEditor().getText();
		Matcher m = searchPattern.matcher(text);
		while (m.find()) {
			this.matches.add(new MatchPosition(m.start(), m.end()));
		}

		this.btNext.setDisable(matches.isEmpty());
		this.btPrev.setDisable(matches.isEmpty());

		if (matches.isEmpty()) {
			this.setResultText(STYLE_NO_RESULTS, "beditor.searchPane.resultLabel.noResults");
			return;
		}
		this.bEditorView.getEditor().setSearchResults(matches.stream().map(match -> this.buildSearchResultLocation(match.start(), match.end())).toList());
		this.currentMatch = findSelectedMatch();
		this.gotoMatch(currentMatch, false);
	}

	private void gotoMatch(int matchIdx, boolean focusEditor) {
		MatchPosition match = matches.get(matchIdx);
		int caret = tfSearch.getCaretPosition();
		this.bEditorView.jumpToSearchResult(this.buildSearchResultLocation(match.start(), match.end()));
		this.setResultText(STYLE_RESULTS, "beditor.searchPane.resultLabel.results", currentMatch+1, this.matches.size());
		if (focusEditor) {
			selectCurrentMatchInEditor();
		} else {
			this.tfSearch.requestFocus();
			this.tfSearch.positionCaret(caret);
		}
	}

	private int findSelectedMatch() {
		for (int i=0; i<this.matches.size(); i++) {
			IndexRange selection = bEditorView.getEditor().getSelection();
			MatchPosition match = this.matches.get(i);
			if (selection.getStart() == match.start() && selection.getEnd() == match.end()) {
				return i; // the selected match in the editor from which the search was started
			}
		}
		return 0; // first match
	}

	public void hide() {
		if (this.bEditorView != null) {
			this.bEditorView.getEditor().setSearchResults(null);
			selectCurrentMatchInEditor();
		}

		this.lblResults.setText("");
		this.bEditorView = null;
		this.tfSearch.textProperty().removeListener(textListener);
	}

	private void selectCurrentMatchInEditor() {
		if (this.bEditorView != null) {
			if (!matches.isEmpty() && 0 <= currentMatch && currentMatch < matches.size()) {
				MatchPosition match = matches.get(currentMatch);
				if (match.end() <= this.bEditorView.getEditor().getLength()) { // do not attempt to highlight when triggered by switching to shorter new machine
					this.bEditorView.getEditor().selectRange(match.start(), match.end());
				}
			}
		}
	}

	private ErrorItem.Location buildSearchResultLocation(int start, int end) {
		Path path = this.bEditorView.getPath();
		TwoDimensional.Position startPos = this.bEditorView.getEditor().getContent().offsetToPosition(start, TwoDimensional.Bias.Forward);
		TwoDimensional.Position endPos = startPos.offsetBy(end - start, TwoDimensional.Bias.Backward);
		return new ErrorItem.Location(
			path.toString(),
			startPos.getMajor() + 1,
			startPos.getMinor(),
			endPos.getMajor() + 1,
			endPos.getMinor()
		);
	}

	private void setResultText(String style, String key, Object... args) {
		this.lblResults.setText(i18n.translate(key, args));
		this.lblResults.getStyleClass().setAll(style);
	}
}
