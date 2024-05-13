package de.prob2.ui.consoles.b;

import java.io.File;
import java.util.OptionalInt;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.consoles.Console;
import de.prob2.ui.consoles.b.codecompletion.BCCItem;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
@Singleton
public final class BConsole extends Console {

	private final CodeCompletion<BCCItem> codeCompletion;

	@Inject
	private BConsole(StageManager stageManager, BInterpreter bInterpreter, I18n i18n, CurrentTrace currentTrace, Config config) {
		super(i18n, bInterpreter, "consoles.b.header", "consoles.b.prompt.classicalB");
		currentTrace.stateSpaceProperty().addListener((o, from, to) -> {
			if (to != null) {
				final File modelFile = to.getModel().getModelFile();
				final String name = modelFile == null ? to.getMainComponent().toString() : modelFile.getName();
				final String message = i18n.translate("consoles.b.message.modelLoaded", name);
				this.addParagraph(message, Set.of("message"), false);
			}
		});

		this.codeCompletion = new CodeCompletion<>(
			stageManager,
				new AbstractParentWithEditableText<>() {

					@Override
					public void doReplacement(BCCItem replacement) {
						OptionalInt optInputPosition = BConsole.this.getPositionInInput();
						if (optInputPosition.isEmpty()) {
							// the cursor is not in the input, we dont have an anchor position for completion
							return;
						}

						int inputPosition = optInputPosition.getAsInt();
						BConsole.this.replace(inputPosition - replacement.getOriginalText().length(), inputPosition, replacement.getReplacement());
					}
				},
			bInterpreter::getSuggestions
		);
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e -> this.triggerCodeCompletion()));
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.TAB), e -> this.triggerCodeCompletion()));

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.bConsoleInstructions != null) {
					setHistory(configData.bConsoleInstructions);
				}
			}

			@Override
			public void saveConfig(final ConfigData configData) {
				configData.bConsoleInstructions = getHistory();
			}
		});
	}

	private void triggerCodeCompletion() {
		this.getPositionInInput().ifPresent(pos -> this.codeCompletion.trigger());
	}
}
