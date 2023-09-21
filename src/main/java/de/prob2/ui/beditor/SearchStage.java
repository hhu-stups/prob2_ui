package de.prob2.ui.beditor;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.fxmisc.richtext.model.TwoDimensional;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SearchStage extends Stage {

	private static final String STYLE_ERROR = "search-error";
	private static final String STYLE_NO_RESULTS = "search-no-results";
	private static final String STYLE_RESULTS = "search-has-results";

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchStage.class);

	private final I18n i18n;
	private BEditorView bEditorView;

	@FXML
	private TextField tfSearch;
	@FXML
	private CheckBox cbMatchCase;
	@FXML
	private CheckBox cbWordsOnly;
	@FXML
	private CheckBox cbRegex;
	/*@FXML
	private CheckBox cbDotAll;
	@FXML
	private CheckBox cbMultiline;*/
	@FXML
	private Label lblResults;
	@FXML
	private Button findButton;
	@FXML
	private Button gotoStartButton;
	@FXML
	private Button closeButton;

	@Inject
	public SearchStage(StageManager stageManager, I18n i18n) {
		this.i18n = i18n;
		stageManager.loadFXML(this, "search_stage.fxml");
	}

	public void open(BEditorView bEditorView) {
		Objects.requireNonNull(bEditorView);
		if (this.bEditorView != bEditorView) {
			if (this.bEditorView != null) {
				this.bEditorView.getEditor().setSearchResult(null);
			}

			this.bEditorView = bEditorView;

			this.gotoStartButton.disableProperty().bind(Bindings.createBooleanBinding(
				() -> Objects.equals(this.bEditorView.getEditor().caretPositionProperty().getValue(), 0),
				this.bEditorView.getEditor().caretPositionProperty()
			));
		}

		if (this.isShowing()) {
			this.requestFocus();
		} else {
			this.show();
		}
	}

	@FXML
	private void initialize() {
		this.setAlwaysOnTop(true);

		// this.cbDotAll.disableProperty().bind(this.cbRegex.selectedProperty().not());
		// this.cbMultiline.disableProperty().bind(this.cbRegex.selectedProperty().not());

		this.lblResults.getStyleClass().clear();

		this.findButton.disableProperty().bind(this.tfSearch.textProperty().isEmpty());

		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), e -> handleFind()));
	}

	@FXML
	private void handleFind() {
		// TODO: instead of popup window use a solution integrated into the top bar
		// TODO: search while typing/find all matches
		// TODO: wraparound search

		if (this.bEditorView == null || this.findButton.isDisabled()) {
			return;
		}

		this.bEditorView.getEditor().setSearchResult(null);

		String searchText = this.tfSearch.getText();

		// normal matching options
		boolean matchCase = this.cbMatchCase.isSelected();
		boolean wordsOnly = this.cbWordsOnly.isSelected();

		// regex options
		boolean regex = this.cbRegex.isSelected();
		boolean dotAll = false; // this.cbDotAll.isSelected();
		boolean multiline = false; // this.cbMultiline.isSelected();

		// unicode support always enabled
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
		if (regex && dotAll) {
			flags |= Pattern.DOTALL;
		}
		if (regex && multiline) {
			flags |= Pattern.MULTILINE;
		}

		Pattern searchPattern;
		try {
			searchPattern = Pattern.compile(searchText, flags);
		} catch (PatternSyntaxException e) {
			LOGGER.warn("unable to parse search string", e);
			this.setResultText(STYLE_ERROR, "beditor.searchStage.resultLabel.error");
			return;
		}

		String text = this.bEditorView.getEditor().getText();
		Matcher m = searchPattern.matcher(text);
		int startPosition = this.bEditorView.getEditor().getCaretPosition();
		boolean found = m.find(Math.max(0, Math.min(text.length(), startPosition)));
		if (found) {
			this.setResultText(STYLE_RESULTS, "beditor.searchStage.resultLabel.results");
			int start = m.start();
			int end = m.end();
			this.bEditorView.jumpToSearchResult(this.buildSearchResultLocation(start, end));
		} else {
			if (startPosition > 0) {
				this.setResultText(STYLE_NO_RESULTS, "beditor.searchStage.resultLabel.noResultsAfterCursor");
			} else {
				this.setResultText(STYLE_NO_RESULTS, "beditor.searchStage.resultLabel.noResults");
			}
		}
	}

	@FXML
	private void handleGotoStart() {
		if (this.bEditorView == null || this.gotoStartButton.isDisabled()) {
			return;
		}

		this.bEditorView.jumpToPosition(0);
	}

	@FXML
	private void handleClose() {
		if (this.bEditorView != null) {
			this.bEditorView.getEditor().setSearchResult(null);
		}

		this.lblResults.setText("");
		this.gotoStartButton.disableProperty().unbind();
		this.bEditorView = null;
		this.close();
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
