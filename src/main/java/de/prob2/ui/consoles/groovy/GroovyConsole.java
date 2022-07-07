package de.prob2.ui.consoles.groovy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.consoles.Console;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionEvent;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionTriggerAction;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

@FXMLInjected
@Singleton
public class GroovyConsole extends Console {
	private final GroovyInterpreter groovyInterpreter;
	
	@Inject
	private GroovyConsole(GroovyInterpreter groovyInterpreter, I18n i18n, Config config) {
		super(i18n, i18n.translate("consoles.groovy.header"), i18n.translate("consoles.groovy.prompt"), groovyInterpreter);
		this.groovyInterpreter = groovyInterpreter;
		this.groovyInterpreter.setCodeCompletion(this);
		setCodeCompletionEvent();
		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.SPACE, KeyCombination.CONTROL_DOWN), e-> this.triggerCodeCompletion(CodeCompletionTriggerAction.TRIGGER)));
		
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.groovyConsoleInstructions != null) {
					loadInstructions(configData.groovyConsoleInstructions);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.groovyConsoleInstructions = saveInstructions();
			}
		});
	}
	
	@Override
	protected void keyPressed(KeyEvent e) {
		if (".".equals(e.getText())) {
			triggerCodeCompletion(CodeCompletionTriggerAction.POINT);
		}
		super.keyPressed(e);
	}
	
	private void triggerCodeCompletion(CodeCompletionTriggerAction action) {
		if (getCaretPosition() >= this.getInputStart()) {
			int caretPosInLine = getCaretPosition() - getInputStart();
			groovyInterpreter.triggerCodeCompletion(getInput().substring(0, caretPosInLine), action);
		}
	}
	
	private void setCodeCompletionEvent() {
		this.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> groovyInterpreter.triggerCloseCodeCompletion());
		this.addEventHandler(CodeCompletionEvent.CODECOMPLETION, this::handleCodeCompletionEvent);
	}
	
	private void handleCodeCompletionEvent(CodeCompletionEvent e) {
		if (e.getCode() == KeyCode.ENTER || e.getEvent() instanceof MouseEvent || ";".equals(((KeyEvent)e.getEvent()).getText())) {
			handleChooseSuggestion(e);
			requestFollowCaret(); //This forces the text area to scroll to the bottom. Invoking scrollYToPixel does not have the expected effect
		} else if (e.getCode() == KeyCode.SPACE) {
			//handle Space in Code Completion
			keyPressed((KeyEvent)e.getEvent());
			e.consume();
		}
	}
		
	private void handleChooseSuggestion(CodeCompletionEvent e) {
		String choice = e.getChoice();
		String suggestion = e.getCurrentSuggestion();
		int indexSkipped = getIndexSkipped(this.getText(this.getCaretPosition(), this.getLength()), choice, suggestion);
		int indexOfRest = this.getCaretPosition() + indexSkipped;
		int oldLength = this.getLength();
		String addition = choice + this.getText(indexOfRest, this.getLength());
		this.deleteText(this.getCaretPosition() - suggestion.length(), this.getLength());
		this.appendText(addition);
		int diff = this.getLength() - oldLength;
		currentPosInLine += diff + indexSkipped;
		charCounterInLine += diff;
		this.moveTo(indexOfRest + diff);
	}
	
	private int getIndexSkipped(String rest, String choice, String suggestion) {
		String restOfChoice = choice.substring(suggestion.length());
		int result = 0;
		for (int i = 0; i < Math.min(rest.length(), restOfChoice.length()); i++) {
			if (restOfChoice.charAt(i) == rest.charAt(i)) {
				result++;
			} else {
				break;
			}
		}
		return result;
	}
	
	public void closeObjectStage() {
		groovyInterpreter.closeObjectStage();
	}


}
