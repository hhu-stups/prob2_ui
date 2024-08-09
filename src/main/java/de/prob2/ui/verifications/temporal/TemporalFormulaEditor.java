package de.prob2.ui.verifications.temporal;

import java.util.Collections;

import com.google.inject.Inject;

import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.consoles.b.codecompletion.BCCItem;
import de.prob2.ui.consoles.b.codecompletion.BCodeCompletion;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
public final class TemporalFormulaEditor extends ExtendedCodeArea {
	private final CodeCompletion<BCCItem> codeCompletion;

	@Inject
	public TemporalFormulaEditor(StageManager stageManager, FontSize fontSize, I18n i18n, StopActions stopActions, CurrentTrace currentTrace) {
		super(fontSize, i18n, stopActions);

		this.codeCompletion = new CodeCompletion<>(
			stageManager,
				new AbstractParentWithEditableText<>() {

					@Override
					public void doReplacement(BCCItem replacement) {
						if (!TemporalFormulaEditor.this.isEditable()) {
							// the text field is not editable, assume no file loaded
							return;
						}

						int caret = TemporalFormulaEditor.this.getCaretPosition();
						TemporalFormulaEditor.this.replace(caret - replacement.getOriginalText().length(), caret, replacement.getReplacement(), Collections.emptyList());
					}
				},
			text -> BCodeCompletion.doCompletion(currentTrace.getStateSpace(), text, false)
		);
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e -> this.triggerCodeCompletion()));
	}

	private void triggerCodeCompletion() {
		if (this.isEditable()) {
			this.codeCompletion.trigger();
		}
	}
}
