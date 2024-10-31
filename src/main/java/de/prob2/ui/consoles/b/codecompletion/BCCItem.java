package de.prob2.ui.consoles.b.codecompletion;

import de.prob2.ui.codecompletion.CodeCompletionItem;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class BCCItem implements CodeCompletionItem {

	private static final int MAX_WIDTH = 455;
	private static final int MAX_WRAPPING_WIDTH_REPLACEMENT = 240;
	private static final int MAX_WRAPPING_WIDTH_TYPE = 200;
	private static final int PADDING = 15;

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
	public Node getListNode(boolean selected) {
		if (!type.isEmpty()) {
			return selected ? getReplacementTypeBox(Color.WHITE, Color.WHITE) : getReplacementTypeBox(Color.BLACK, Color.GRAY);
		} else {
			Text text = new Text(replacement);
			text.setFill(selected ? Color.WHITE : Color.BLACK);
			return text;
		}
	}

	@Override
	public String toString() {
		return this.getReplacement();
	}

	private HBox getReplacementTypeBox(Color replacementColor, Color typeColor) {
		Text replaceText = new Text(replacement);
		replaceText.setFill(replacementColor);

		Text typeText = new Text(type);
		typeText.setFont(new Font(replaceText.getFont().getSize() - 2));
		typeText.setFill(typeColor);
		typeText.setTextAlignment(TextAlignment.RIGHT);

		HBox right = new HBox(typeText);
		right.setAlignment(Pos.CENTER_RIGHT);
		HBox.setHgrow(right, Priority.ALWAYS);

		if (replacement.length() < type.length()) {
			// limit the replacement text to its max. length (or to its actual length, if shorter) and
			// use the rest of the space for the (longer) type text
			double replaceWidth = Math.min(replaceText.getLayoutBounds().getWidth(), MAX_WRAPPING_WIDTH_REPLACEMENT) + PADDING;
			replaceText.setWrappingWidth(replaceWidth);
			typeText.setWrappingWidth(MAX_WIDTH - replaceWidth);
		} else {
			// limit the type text to its max. length (or to its actual length, if shorter) and
			// use the rest of the space for the (longer) replacement text
			double typeWidth = Math.min(typeText.getLayoutBounds().getWidth(), MAX_WRAPPING_WIDTH_TYPE) + PADDING;
			typeText.setWrappingWidth(typeWidth);
			replaceText.setWrappingWidth(MAX_WIDTH - typeWidth);
		}

		return new HBox(replaceText, right);
	}
}
