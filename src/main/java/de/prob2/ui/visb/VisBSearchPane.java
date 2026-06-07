package de.prob2.ui.visb;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@FXMLInjected
@Singleton
public final class VisBSearchPane extends AnchorPane {

	private static final String STYLE_NO_RESULTS = "search-no-results";
	private static final String STYLE_RESULTS = "search-has-results";

	private static final Logger LOGGER = LoggerFactory.getLogger(VisBSearchPane.class);

	private final I18n i18n;
	private WebView webView;

	private final PauseTransition pause = new PauseTransition(Duration.millis(100));
	private final ChangeListener<String> textListener = (obs, ov, nv) -> pause.playFromStart();

	@FXML
	private TextField tfSearch;
	@FXML
	private ToggleButton btMatchCase;
	@FXML
	private ToggleButton btWordsOnly;
	@FXML
	private ToggleButton btSearchIds;
	@FXML
	private Label lblResults;
	@FXML
	private Button btPrev;
	@FXML
	private Button btNext;

	@Inject
	public VisBSearchPane(StageManager stageManager, I18n i18n) {
		this.i18n = i18n;
		stageManager.loadFXML(this, "visb_search_pane.fxml");
	}

	@FXML
	private void initialize() {
		this.lblResults.getStyleClass().clear();

		pause.setOnFinished(e -> this.find(false, true));

		btMatchCase.selectedProperty().addListener(getToggleButtonListener(btMatchCase));
		btWordsOnly.selectedProperty().addListener(getToggleButtonListener(btWordsOnly));
		btSearchIds.selectedProperty().addListener(getToggleButtonListener(btSearchIds));
		btSearchIds.selectedProperty().addListener((o, ov, nv) -> {
			// ID search is currently only supported for a precise match, TODO: improve
			btMatchCase.setSelected(nv);
			btMatchCase.setDisable(nv);
			btWordsOnly.setSelected(nv);
			btWordsOnly.setDisable(nv);
		});

		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN), e -> btMatchCase.fire()));
		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.W, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN), e -> btWordsOnly.fire()));
		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.I, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN), e -> btSearchIds.fire()));
		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), e -> find(false)));
		Nodes.addInputMap(this.tfSearch, InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.SHIFT_DOWN), e -> find(true)));
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

	public void show(WebView webView) {
		Objects.requireNonNull(webView);
		if (this.webView != webView) {
			if (this.webView != null) {
				webView.getEngine().executeScript("window.getSelection().removeAllRanges(); window.search('');");
			}
			this.webView = webView;
		}
		this.startSearch();
	}

	public void startSearch() {
		this.tfSearch.textProperty().removeListener(textListener);
		this.requestFocus();

		Object jsText = this.webView.getEngine().executeScript("window.getSelection().toString();");
		String selected = jsText != null ? jsText.toString() : "";
		if (!selected.isEmpty()) { // do not clear text if selection is empty
			this.tfSearch.setText(selected);
		}
		this.find(false,true);
		if (!this.tfSearch.isFocused()) {
			this.tfSearch.requestFocus();
		}
		this.tfSearch.selectAll();
		this.tfSearch.textProperty().addListener(textListener);
	}

	public void hide() {
		this.lblResults.setText("");
		this.removeHighlighting();
		this.webView = null;
		this.tfSearch.textProperty().removeListener(textListener);
	}

	private void removeHighlighting() {
		this.webView.getEngine().executeScript("window.getSelection().removeAllRanges();");
	}

	@FXML
	private void handleGotoPrevious() {
		this.find(true);
	}

	@FXML
	private void handleGotoNext() {
		this.find(false);
	}

	void find(boolean backwards) {
		find(backwards, false);
	}

	private void find(boolean backwards, boolean keepPosition) {
		if (this.webView == null) {
			this.setResultText(STYLE_NO_RESULTS, "visb.searchPane.resultLabel.notFound");
			return;
		}

		String searchText = this.tfSearch.getText();
		if (searchText.isEmpty()) {
			this.setResultText(STYLE_RESULTS, "visb.searchPane.resultLabel.notFound");
			this.removeHighlighting();
			return;
		}

		boolean found;
		if (btSearchIds.isSelected()) { // search for SVG IDs
			found = searchForId(searchText);
		} else { // standard text search
			if (keepPosition) {
				// stay on the same search result by searching backwards first, then forwards
				search(searchText, true);
				found = search(searchText, false);
			} else {
				// search in the direction specified by 'backwards'
				found = search(searchText, backwards);
			}
		}

		if (found) {
			this.setResultText(STYLE_RESULTS, "visb.searchPane.resultLabel.found");
			return;
		}
		this.setResultText(STYLE_NO_RESULTS, "visb.searchPane.resultLabel.notFound");
		this.removeHighlighting();
	}

	private boolean search(String text, boolean backwards) {
		String js = "var found = " + findWithArguments(text, backwards) + ";"
				+ "if (found) {"
				+   "if (found.rangeCount > 0) {"
				+       "var rect = found.getRangeAt(0).getBoundingClientRect();"
				+       "window.scrollTo({top: rect.top + window.scrollY - (window.innerHeight / 2)});"
				+   "}"
				+ "}; "
				+ "found;";
		return (Boolean) webView.getEngine().executeScript(js);
	}

	private String findWithArguments(String searchText, boolean backwards) {
		// find(string, caseSensitive, backwards, wrapAround, wholeWord, searchInFrames, showDialog)
		return "window.find('" + searchText + "',"
				+ this.btMatchCase.isSelected() + ","
				+ backwards + ","
				+ "true,"
				+ this.btWordsOnly.isSelected() + ","
				+ "true,false)";
	}

	private boolean searchForId(String id) {
		String elem = "elem_" + (id.hashCode() & Integer.MAX_VALUE); // to allow removal of blink effect of previous search result after timeout
		String escId = id.replace("\\", "\\\\").replace("'", "\\'");
		String js = "var "+ elem + " = document.getElementById('" + escId + "');"
				+ "if ("+ elem + ") {"
				+ "  var rect = " + elem + ".getBoundingClientRect();"
				+ "  window.scrollTo({top: rect.top + window.scrollY - (window.innerHeight / 2),"
				+ "                   left: rect.left + window.scrollX - (window.innerWidth / 2)});"
				+ "  " + elem + ".classList.add('search-highlight');"
				+ "  setTimeout(() => " + elem + ".classList.remove('search-highlight'), 3000);"
				+ "}"
				+ elem + " != null;";
		return (Boolean) webView.getEngine().executeScript(js);
	}

	private void setResultText(String style, String key, Object... args) {
		this.lblResults.setText(i18n.translate(key, args));
		this.lblResults.getStyleClass().setAll(style);
	}
}
