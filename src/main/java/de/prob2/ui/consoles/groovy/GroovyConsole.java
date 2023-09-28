package de.prob2.ui.consoles.groovy;

import java.util.OptionalInt;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.consoles.Console;
import de.prob2.ui.consoles.groovy.codecompletion.GroovyCCItem;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
@Singleton
public class GroovyConsole extends Console {

	private final GroovyInterpreter groovyInterpreter;
	private final CodeCompletion<GroovyCCItem> codeCompletion;

	@Inject
	private GroovyConsole(StageManager stageManager, GroovyInterpreter groovyInterpreter, I18n i18n, Config config) {
		super(i18n, groovyInterpreter, "consoles.groovy.header", "consoles.groovy.prompt");
		this.groovyInterpreter = groovyInterpreter;
		this.codeCompletion = new CodeCompletion<>(
			stageManager,
				new AbstractParentWithEditableText<>() {

					@Override
					public void doReplacement(GroovyCCItem replacement) {
						OptionalInt optInputPosition = GroovyConsole.this.getPositionInInput();
						if (!optInputPosition.isPresent()) {
							// the cursor is not in the input, we dont have an anchor position for completion
							return;
						}

						int inputPosition = optInputPosition.getAsInt();
						GroovyConsole.this.replace(inputPosition - replacement.getOriginalText().length(), inputPosition, replacement.getReplacement());
					}
				},
			this.groovyInterpreter::getSuggestions
		);
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e -> this.triggerCodeCompletion()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.TAB), e -> this.triggerCodeCompletion()));

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.groovyConsoleInstructions != null) {
					setHistory(configData.groovyConsoleInstructions);
				}
			}

			@Override
			public void saveConfig(final ConfigData configData) {
				configData.groovyConsoleInstructions = getHistory();
			}
		});
	}

	@Override
	protected void onEnterSingleLineText(String text) {
		super.onEnterSingleLineText(text);
		if (!isSearching() && ".".equals(text)) {
			triggerCodeCompletion();
		}
	}

	private void triggerCodeCompletion() {
		this.getPositionInInput().ifPresent(pos -> this.codeCompletion.trigger());
	}

	public void closeObjectStage() {
		groovyInterpreter.closeObjectStage();
	}
}
