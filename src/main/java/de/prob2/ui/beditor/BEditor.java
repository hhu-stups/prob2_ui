package de.prob2.ui.beditor;

import java.util.Collection;
import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.brules.RulesModelFactory;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ModelFactory;
import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.consoles.b.codecompletion.BCCItem;
import de.prob2.ui.consoles.b.codecompletion.BCodeCompletion;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
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
	protected StyleSpans<Collection<String>> computeHighlighting(String text) {
		StyleSpans<Collection<String>> styleSpans = super.computeHighlighting(text);
		if (styleSpans == null || text.isEmpty()) {
			return styleSpans;
		}

		StyleSpans<Collection<String>> highlighting = null;
		StyleSpans<Collection<String>> overlay = null;

		Machine machine = currentProject.getCurrentMachine();
		if (machine == null) {
			// Prompt text is a comment
			highlighting = StyleSpans.singleton(Collections.singleton("editor_comment"), text.length());
		} else {
			Class<? extends ModelFactory<?>> modelFactoryClass = machine.getModelFactoryClass();
			if (modelFactoryClass == RulesModelFactory.class) {
				// B-Rules DSL keywords are not recognized by the lexer and are added by an additional regex highlighting
				overlay = RegexSyntaxHighlighting.computeHighlighting(modelFactoryClass, text);
				modelFactoryClass = ClassicalBFactory.class;
			}

			if (modelFactoryClass == ClassicalBFactory.class) {
				highlighting = BLexerSyntaxHighlighting.computeBHighlighting(text);
			} else if (RegexSyntaxHighlighting.canHighlight(modelFactoryClass)) {
				highlighting = RegexSyntaxHighlighting.computeHighlighting(modelFactoryClass, text);
			}
		}

		if (overlay != null) {
			if (highlighting != null) {
				highlighting = highlighting.overlay(overlay, ExtendedCodeArea::combineCollections);
			} else {
				highlighting = overlay;
			}
		}

		if (highlighting != null) {
			return styleSpans.overlay(highlighting, ExtendedCodeArea::combineCollections);
		} else {
			// Do not highlight unknown languages
			return styleSpans;
		}
	}
}
