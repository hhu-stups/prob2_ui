package de.prob2.ui.consoles.groovy;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.google.inject.Inject;

import de.prob.scripting.ScriptEngineProvider;

import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.Executable;
import de.prob2.ui.consoles.groovy.codecompletion.CodeCompletionTriggerAction;
import de.prob2.ui.consoles.groovy.codecompletion.GroovyCodeCompletion;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectStage;
import de.prob2.ui.internal.StageManager;

import org.codehaus.groovy.GroovyBugError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroovyInterpreter implements Executable {
	private static final Logger logger = LoggerFactory.getLogger(GroovyInterpreter.class);
	private final ScriptEngine engine;
	private final GroovyCodeCompletion codeCompletion;
	private final GroovyObjectStage groovyObjectStage;

	@Inject
	private GroovyInterpreter(final StageManager stageManager, final ScriptEngineProvider sep, final GroovyObjectStage groovyObjectStage) {
		engine = sep.get();
		this.groovyObjectStage = groovyObjectStage;
		this.codeCompletion = new GroovyCodeCompletion(stageManager, engine);
	}
	
	@Override
	public ConsoleExecResult exec(final ConsoleInstruction instruction) {
		logger.trace("Exec");
		if ("inspect".equals(instruction.getInstruction())) {
			groovyObjectStage.showObjects(engine);
			return new ConsoleExecResult("", "", ConsoleExecResultType.PASSED);
		} else if("clear".equals(instruction.getInstruction())) {
			return new ConsoleExecResult("clear","", ConsoleExecResultType.PASSED);
		} else {
			String resultString;
			StringBuilder console = new StringBuilder();
			ConsoleExecResultType resultType = ConsoleExecResultType.PASSED;
			engine.put("__console", console);
			logger.trace("Eval {} on {}", instruction.getInstruction(), engine);
			try {
				Object eval = engine.eval(instruction.getInstruction());
				resultString = eval.toString();
				logger.trace("Evaled {} to {}", instruction.getInstruction(), resultString);
			} catch (ScriptException|GroovyBugError e) {
				logger.debug("Groovy Evaluation failed", e);
				resultString = e.toString();
				resultType = ConsoleExecResultType.ERROR;
			}
			return new ConsoleExecResult(console.toString(), resultString, resultType);
		}
	}
	
	public void setCodeCompletion(GroovyConsole parent) {
		codeCompletion.setParent(parent);
	}
	
	public void triggerCodeCompletion(String currentLine, CodeCompletionTriggerAction action) {
		if(!codeCompletion.isVisible()) {
			codeCompletion.activate(currentLine, action);
		}
	}
	
	public void triggerCloseCodeCompletion() {
		codeCompletion.deactivate();
	}
	
	public void closeObjectStage() {
		groovyObjectStage.close();
	}
}
