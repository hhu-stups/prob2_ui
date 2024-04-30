package de.prob2.ui.beditor;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.brules.RulesModelFactory;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ModelFactory;
import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.consoles.b.codecompletion.BCCItem;
import de.prob2.ui.consoles.b.codecompletion.BCodeCompletion;
import de.prob2.ui.internal.*;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@Singleton
@FXMLInjected
public class BEditor extends ExtendedCodeArea {

	private final CurrentProject currentProject;
	private final CodeCompletion<BCCItem> codeCompletion;

	@Inject
	private BEditor(final StageManager stageManager, final FontSize fontSize, final I18n i18n, final CurrentProject currentProject, final StopActions stopActions, final CurrentTrace currentTrace) {
		super(fontSize, i18n, stopActions);
		this.currentProject = currentProject;

		this.codeCompletion = new CodeCompletion<>(
			stageManager,
			new AbstractParentWithEditableText<>() {

				@Override
				public void doReplacement(BCCItem replacement) {
					if (!BEditor.this.isEditable()) {
						// the text field is not editable, assume no file loaded
						return;
					}

					int caret = BEditor.this.getCaretPosition();
					BEditor.this.replace(caret - replacement.getOriginalText().length(), caret, replacement.getReplacement(), Collections.emptyList());
					BEditor.this.reloadHighlighting();
				}
			},
			text -> BCodeCompletion.doCompletion(currentTrace.getStateSpace(), text)
		);
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e -> this.triggerCodeCompletion()));
	}

	private void triggerCodeCompletion() {
		if (this.isEditable()) {
			this.codeCompletion.trigger();
		}
	}

	@Override
	protected boolean showLineNumbers() {
		return true;
	}

	@Override
	protected Optional<StyleSpans<Collection<String>>> computeHighlighting(String text) {
		Optional<StyleSpans<Collection<String>>> styleSpansOpt = super.computeHighlighting(text);
		if (styleSpansOpt.isEmpty()) {
			return Optional.empty();
		} else if (text.isEmpty()) {
			return styleSpansOpt;
		}

		StyleSpans<Collection<String>> styleSpans = styleSpansOpt.get();
		Machine machine = currentProject.getCurrentMachine();
		if (machine == null) {
			// Prompt text is a comment text
			return Optional.of(styleSpans.overlay(StyleSpans.singleton(Collections.singleton("editor_comment"), text.length()), ExtendedCodeArea::combineCollections));
		}
		Class<? extends ModelFactory<?>> modelFactoryClass = machine.getModelFactoryClass();
		if (modelFactoryClass == ClassicalBFactory.class) {
			return Optional.of(styleSpans.overlay(BLexerSyntaxHighlighting.computeBHighlighting(text), ExtendedCodeArea::combineCollections));
		} else if (modelFactoryClass == RulesModelFactory.class) {
			// B-Rules DSL keywords are not recognized by the lexer and are added by an additional regex highlighting
			return Optional.of(styleSpans.overlay(BLexerSyntaxHighlighting.computeBHighlighting(text), ExtendedCodeArea::combineCollections)
				                   .overlay(RegexSyntaxHighlighting.computeHighlighting(RulesModelFactory.class, text), ExtendedCodeArea::combineCollections));
		} else if (RegexSyntaxHighlighting.canHighlight(modelFactoryClass)) {
			return Optional.of(styleSpans.overlay(RegexSyntaxHighlighting.computeHighlighting(modelFactoryClass, text), ExtendedCodeArea::combineCollections));
		} else {
			// Do not highlight unknown languages.
			return Optional.of(styleSpans);
		}
	}
}
