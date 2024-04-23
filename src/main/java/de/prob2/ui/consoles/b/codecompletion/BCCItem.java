package de.prob2.ui.consoles.b.codecompletion;

import de.prob2.ui.codecompletion.CodeCompletionItem;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class BCCItem implements CodeCompletionItem {

	private static final int MAX_WIDTH = 455, MAX_WRAPPING_WIDTH_REPLACEMENT = 250, MAX_WRAPPING_WIDTH_TYPE = 205;

	private final String originalText;
	private final String replacement;
	private final String type;

	public BCCItem(String originalText, String replacement, String type) {
		this.originalText = originalText;
		this.replacement = replacement;
		this.type = type;
	}

	@Override
	public String getOriginalText() {
		return this.originalText;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public String getReplacement() {
		return this.replacement;
	}

	@Override
	public Node getListNode() {
		if (!type.isEmpty()) {
			HBox hBox = new HBox();

			Text replaceText = new Text(replacement);
			double replaceWidth = Math.min(replaceText.getLayoutBounds().getWidth(), MAX_WRAPPING_WIDTH_REPLACEMENT);
			replaceText.setWrappingWidth(replaceWidth);
			hBox.getChildren().add(replaceText);

			Text typeText = new Text(type);
			double typeWidth = Math.min(MAX_WIDTH - replaceWidth, MAX_WRAPPING_WIDTH_TYPE);
			typeText.setFont(new Font(replaceText.getFont().getSize() - 3));
			typeText.setFill(Paint.valueOf(Color.GRAY.toString()));
			typeText.setTextAlignment(TextAlignment.RIGHT);
			typeText.setWrappingWidth(typeWidth);
			hBox.getChildren().add(typeText);

			hBox.setMaxWidth(MAX_WIDTH);
			hBox.setAlignment(Pos.CENTER_LEFT);
			hBox.setSpacing(MAX_WIDTH - typeWidth - replaceWidth);
			return hBox;
		} else {
			return new Text(this.replacement);
		}
	}

	@Override
	public String toString() {
		return this.getReplacement();
	}
}
