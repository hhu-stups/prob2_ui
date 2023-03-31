package de.prob2.ui.codecompletion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StringHelper;

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

	private final ParentWithEditableText parent;
	private final ChangeListener<Optional<Point2D>> caretBoundsChangeListener;
	private final Callback<String, Collection<? extends T>> codeCompletionProvider;
	private final Consumer<T> codeCompletionCallback;

	@FXML
	private ListView<T> lvSuggestions;

	public CodeCompletion(StageManager stageManager, ParentWithEditableText parent, Callback<String, Collection<? extends T>> codeCompletionProvider, Consumer<T> codeCompletionCallback) {
		super();
		this.parent = parent;
		this.codeCompletionProvider = codeCompletionProvider;
		this.codeCompletionCallback = codeCompletionCallback;

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
		System.out.println("CodeCompletion.trigger");
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
		System.out.println("CodeCompletion.hide");
		super.hide();
		this.lvSuggestions.getItems().clear();
	}

	private static String keyEventToString(KeyEvent e) {
		List<String> components = new ArrayList<>();
		if (e.isConsumed()) {
			components.add("consumed");
		}

		if (e.getCharacter() != null && !e.getCharacter().isEmpty()) {
			components.add("character=" + StringHelper.escapeNonAscii(e.getCharacter()));
		}

		if (e.getText() != null && !e.getText().isEmpty()) {
			components.add("text=" + StringHelper.escapeNonAscii(e.getText()));
		}

		if (e.getCode() != null) {
			components.add("code=" + e.getCode());
		}

		if (e.isShiftDown()) {
			components.add("shift");
		}

		if (e.isControlDown()) {
			components.add("ctrl");
		}

		if (e.isAltDown()) {
			components.add("alt");
		}

		if (e.isMetaDown()) {
			components.add("meta");
		}

		if (e.isShortcutDown()) {
			components.add("shortcut");
		}

		return e.getClass().getSimpleName() + '[' + e.getEventType() + ']' + '{' + String.join(",", components) + '}';
	}

	private void onKeyPressed(KeyEvent event) {
		System.out.println("CodeCompletion.ListView.onKeyPressed: " + keyEventToString(event));
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
		System.out.println("CodeCompletion.ListView.onMouseClicked");
		if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
			this.doCompletion(this.lvSuggestions.getSelectionModel().getSelectedItem());
		}
	}

	private void doCompletion(T selectedItem) {
		System.out.println("CodeCompletion.doCompletion: " + selectedItem);
		if (selectedItem == null) {
			return;
		}

		this.codeCompletionCallback.accept(selectedItem);
		this.hide();
	}
}
