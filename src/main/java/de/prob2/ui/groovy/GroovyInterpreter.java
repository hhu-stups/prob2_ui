package de.prob2.ui.groovy;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.google.inject.Inject;

import de.prob.scripting.ScriptEngineProvider;
import javafx.fxml.FXMLLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroovyInterpreter {
	private final Logger logger = LoggerFactory.getLogger(GroovyInterpreter.class);
	private final ScriptEngine engine;
	private final GroovyCodeCompletion codeCompletion;
	private final GroovyObjectStage groovyObjectStage;

	@Inject
	public GroovyInterpreter(FXMLLoader loader, final ScriptEngineProvider sep, final GroovyObjectStage groovyObjectStage) {
		engine = sep.get();
		this.groovyObjectStage = groovyObjectStage;
		this.codeCompletion = new GroovyCodeCompletion(loader, engine);
	}
	
	public ExecResult exec(Instruction instruction) {
		logger.trace("Exec");
		
		if ("inspect".equals(instruction.getInstruction())) {
			groovyObjectStage.showObjects(engine);
			return new ExecResult("", "");
		} else {
			String resultString;
			StringBuilder console = new StringBuilder();
			engine.put("__console", console);
			logger.trace("Eval {} on {}", instruction.getInstruction(), engine);
			try {
				Object eval = engine.eval(instruction.getInstruction());
				resultString = eval.toString();
				logger.trace("Evaled {} to {}", instruction.getInstruction(), resultString);
			} catch (ScriptException e) {
				logger.debug("Groovy Evaluation failed", e);
				resultString = e.toString();
			}
	
			return new ExecResult(console.toString(), resultString);
		}
	}
	
	public void triggerCodeCompletion(GroovyConsole console, String currentLine) {
		if(!codeCompletion.isVisible()) {
			codeCompletion.activate(console, currentLine);
		} else {
			codeCompletion.deactivate();
		}
	}
	
	public void closeObjectStage() {
		groovyObjectStage.close();
	}
}
