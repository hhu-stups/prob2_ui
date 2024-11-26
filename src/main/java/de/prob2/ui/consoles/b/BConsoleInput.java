package de.prob2.ui.consoles.b;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.consoles.b.codecompletion.BCCItem;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
@Singleton
public final class BConsoleInput extends ExtendedCodeArea {

	private final BInterpreter bInterpreter;
	private final CodeCompletion<BCCItem> codeCompletion;

	@Inject
	public BConsoleInput(FontSize fontSize, I18n i18n, StopActions stopActions, BInterpreter bInterpreter, StageManager stageManager) {
		super(fontSize, i18n, stopActions);
		this.bInterpreter = bInterpreter;

		this.codeCompletion = new CodeCompletion<>(
				stageManager,
				new AbstractParentWithEditableText<>() {

					@Override
					public void doReplacement(BCCItem replacement) {
						if (!BConsoleInput.this.isEditable()) {
							return;
						}

						int caret = BConsoleInput.this.getCaretPosition();
						BConsoleInput.this.replace(caret - replacement.getOriginalText().length(), caret, replacement.getReplacement(), Set.of());
					}
				},
				this.bInterpreter::getSuggestions
		);
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e -> this.triggerCodeCompletion()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.TAB), e -> this.triggerCodeCompletion()));

		this.getStyleClass().add("console");
	}

	private void triggerCodeCompletion() {
		if (this.isEditable()) {
			this.codeCompletion.trigger();
		}
	}
}
