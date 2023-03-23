package de.prob2.ui.consoles.groovy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.consoles.Console;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
@Singleton
public class GroovyConsole extends Console {

	private final GroovyInterpreter groovyInterpreter;

	@Inject
	private GroovyConsole(GroovyInterpreter groovyInterpreter, I18n i18n, Config config) {
		super(i18n, groovyInterpreter, "consoles.groovy.header", "consoles.groovy.prompt");
		this.groovyInterpreter = groovyInterpreter;
		this.groovyInterpreter.setCodeCompletion(this);
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e -> this.triggerCodeCompletion()));

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
		this.getPositionInInput().ifPresent(pos -> groovyInterpreter.triggerCodeCompletion(getInput().substring(0, pos)));
	}

	public void closeObjectStage() {
		groovyInterpreter.closeObjectStage();
	}
}
