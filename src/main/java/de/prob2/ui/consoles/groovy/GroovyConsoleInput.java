package de.prob2.ui.consoles.groovy;

import java.util.Collection;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.consoles.groovy.codecompletion.GroovyCCItem;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputHandler;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
@Singleton
public final class GroovyConsoleInput extends ExtendedCodeArea {

	private final GroovyInterpreter groovyInterpreter;
	private final CodeCompletion<GroovyCCItem> codeCompletion;

	@Inject
	public GroovyConsoleInput(FontSize fontSize, I18n i18n, StopActions stopActions, GroovyInterpreter groovyInterpreter, StageManager stageManager) {
		super(fontSize, i18n, stopActions);
		this.groovyInterpreter = groovyInterpreter;

		this.codeCompletion = new CodeCompletion<>(
				stageManager,
				new AbstractParentWithEditableText<>() {

					@Override
					public void doReplacement(GroovyCCItem replacement) {
						if (!GroovyConsoleInput.this.isEditable()) {
							return;
						}

						int caret = GroovyConsoleInput.this.getCaretPosition();
						GroovyConsoleInput.this.replace(caret - replacement.getOriginalText().length(), caret, replacement.getReplacement(), Set.of());
					}
				},
				this.groovyInterpreter::getSuggestions
		);
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e -> this.triggerCodeCompletion()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.TAB), e -> this.triggerCodeCompletion()));
		Nodes.addInputMap(this, InputMap.process(EventPattern.keyTyped("."), e -> {
			// need a delay so this runs after the dot has actually been typed
			Platform.runLater(this::triggerCodeCompletion);
			return InputHandler.Result.PROCEED;
		}));

		this.getStyleClass().add("console");
	}

	private void triggerCodeCompletion() {
		if (this.isEditable()) {
			this.codeCompletion.trigger();
		}
	}

	@Override
	protected StyleSpans<Collection<String>> computeHighlighting(String text) {
		StyleSpans<Collection<String>> styleSpans = super.computeHighlighting(text);
		if (styleSpans == null || text.isEmpty()) {
			return styleSpans;
		}

		return styleSpans.overlay(this.computeCodeHighlighting(text), ExtendedCodeArea::combineStyleSpans);
	}

	public StyleSpans<Collection<String>> computeCodeHighlighting(String text) {
		// could add some syntax highlighting here
		return new StyleSpansBuilder<Collection<String>>().add(Set.of(), text.length()).create();
	}

	public GroovyInterpreter getGroovyInterpreter() {
		return this.groovyInterpreter;
	}

	public void closeObjectStage() {
		this.groovyInterpreter.closeObjectStage();
	}
}
