package de.prob2.ui.codecompletion;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import de.prob2.ui.internal.StageManager;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.util.Callback;

public class CodeCompletion<T extends CodeCompletionItem> extends Popup {

	private final ParentWithEditableText<? super T> parent;
	private final ChangeListener<Optional<Point2D>> caretBoundsChangeListener;
	private final Callback<String, Collection<? extends T>> codeCompletionProvider;

	@FXML
	private ListView<T> lvSuggestions;

	public CodeCompletion(StageManager stageManager, ParentWithEditableText<? super T> parent, Callback<String, Collection<? extends T>> codeCompletionProvider) {
		super();
		this.parent = parent;
		this.codeCompletionProvider = codeCompletionProvider;

		this.caretBoundsChangeListener = (observable, from, to) -> to.ifPresent(pos -> {
			this.setAnchorX(pos.getX());
			this.setAnchorY(pos.getY());
		});
		this.parent.getCaretPosition().addListener(this.caretBoundsChangeListener);
		this.parent.getTextBeforeCaret().addListener((observable, from, to) -> this.update());

		this.setAutoFix(true);
		this.setAutoHide(true);
		this.setHideOnEscape(true);

		stageManager.loadFXML(this, "codecompletion_popup.fxml");
	}

	@FXML
	private void initialize() {
		lvSuggestions.setOnKeyPressed(this::onKeyPressed);
		lvSuggestions.setOnMouseClicked(this::onMouseClicked);
	}

	private void recalculateAnchorPosition() {
		this.caretBoundsChangeListener.changed(this.parent.getCaretPosition(), Optional.empty(), this.parent.getCaretPosition().getValue());
	}

	public void trigger() {
		if (this.isShowing()) {
			return;
		}

		this.recalculateAnchorPosition();
		this.show(this.parent.getWindow());

		this.update();
		this.lvSuggestions.getSelectionModel().selectFirst();
	}

	private void update() {
		if (!this.isShowing()) {
			return;
		}

		// TODO: update in another thread and use callbacks
		Optional<String> text = this.parent.getTextBeforeCaret().getValue();
		if (text.isPresent()) {
			Collection<? extends T> suggestions = this.codeCompletionProvider.call(text.get());
			List<? extends T> sortedSuggestions = suggestions.stream()
				                                      .sorted(Comparator.comparing(Objects::toString, String.CASE_INSENSITIVE_ORDER))
				                                      .collect(Collectors.toList());
			this.lvSuggestions.getItems().setAll(sortedSuggestions);
		} else {
			this.lvSuggestions.getItems().clear();
		}

		if (this.lvSuggestions.getItems().isEmpty()) {
			this.hide();
		}
	}

	@Override
	public void hide() {
		super.hide();
		this.lvSuggestions.getItems().clear();
	}

	private void onKeyPressed(KeyEvent event) {
		switch (event.getCode()) {
			case TAB:
			case ENTER:
				this.doCompletion(this.lvSuggestions.getSelectionModel().getSelectedItem());
				break;
			case ESCAPE:
				if (this.isHideOnEscape()) {
					this.hide();
				}
				break;
		}
	}

	private void onMouseClicked(MouseEvent event) {
		if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
			this.doCompletion(this.lvSuggestions.getSelectionModel().getSelectedItem());
		}
	}

	private void doCompletion(T selectedItem) {
		if (selectedItem == null) {
			return;
		}

		this.parent.doReplacement(selectedItem);
		this.hide();
	}
}
