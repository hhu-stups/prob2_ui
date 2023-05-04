package de.prob2.ui.consoles.groovy;

import java.util.Optional;
import java.util.OptionalInt;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.codecompletion.CodeCompletion;
import de.prob2.ui.codecompletion.GroovyCCItem;
import de.prob2.ui.codecompletion.ParentWithEditableText;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.consoles.Console;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.stage.Window;

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

		ObservableValue<Optional<Point2D>> caretPos = Bindings.createObjectBinding(
				() -> this.caretBoundsProperty().getValue()
						      .map(bounds -> new Point2D(
								      (bounds.getMinX() + bounds.getMaxX()) / 2.0,
								      bounds.getMaxY()
						      )),
				this.caretBoundsProperty()
		);
		ObservableValue<Optional<String>> textBeforeCaret = Bindings.createObjectBinding(() -> {
			OptionalInt positionInInput = this.getPositionInInput();
			if (positionInInput.isPresent()) {
				return Optional.of(this.getInput().substring(0, positionInInput.getAsInt()));
			} else {
				return Optional.empty();
			}
		}, this.inputProperty(), this.caretPositionProperty());
		this.codeCompletion = new CodeCompletion<>(
				stageManager,
				new ParentWithEditableText<GroovyCCItem>() {

					@Override
					public Window getWindow() {
						return GroovyConsole.this.getScene().getWindow();
					}

					@Override
					public ObservableValue<Optional<Point2D>> getCaretPosition() {
						return caretPos;
					}

					@Override
					public ObservableValue<Optional<String>> getTextBeforeCaret() {
						return textBeforeCaret;
					}

					@Override
					public void doReplacement(GroovyCCItem replacement) {
						OptionalInt optInputPosition = GroovyConsole.this.getPositionInInput();
						if (!optInputPosition.isPresent()) {
							// the cursor is not in the input, we dont have an anchor position for completion
							return;
						}

						int inputPosition = optInputPosition.getAsInt();
						String prefix = GroovyConsole.this.getInput().substring(0, Math.max(0, inputPosition - replacement.getOriginalText().length()));
						String suffix = GroovyConsole.this.getInput().substring(inputPosition);
						String text = replacement.getReplacement();
						GroovyConsole.this.setInput(prefix + text + suffix);
						GroovyConsole.this.moveCaretToPosInInput(prefix.length() + text.length());
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
