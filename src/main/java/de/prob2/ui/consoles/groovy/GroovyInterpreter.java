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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GroovyInterpreter implements Executable {
	private static final Logger LOGGER = LoggerFactory.getLogger(GroovyInterpreter.class);
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
		if ("inspect".equals(instruction.getInstruction())) {
			groovyObjectStage.showObjects(engine);
			return new ConsoleExecResult("", "", ConsoleExecResultType.PASSED);
		} else if ("clear".equals(instruction.getInstruction())) {
			return new ConsoleExecResult("", "", ConsoleExecResultType.CLEAR);
		} else {
			final StringBuilder console = new StringBuilder();
			engine.put("__console", console);
			final Object evalResult;
			try {
				evalResult = engine.eval(instruction.getInstruction());
			} catch (ScriptException e) {
				LOGGER.debug("Groovy console user code threw an exception", e);
				return new ConsoleExecResult(console.toString(), e.getCause().toString(), ConsoleExecResultType.ERROR);
			} catch (Error e) {
				LOGGER.warn("Groovy console user code threw an Error", e);
				return new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR);
			} catch (Throwable e) {
				// Groovy does not enforce declaration of checked Exceptions/Throwables like Java does,
				// which means that the eval call can throw a checked Throwable even though it has no checked exceptions declared.
				// The Groovy script engine wraps Exceptions (including checked ones) in a ScriptException, but not Throwables,
				// so we need to manually catch them here.
				LOGGER.warn("Groovy console user code threw an unexpected Throwable", e);
				return new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR);
			}
			final String resultString;
			try {
				resultString = evalResult.toString();
			} catch (RuntimeException e) {
				LOGGER.debug("Groovy console result toString threw an exception", e);
				return new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR);
			} catch (Error e) {
				LOGGER.warn("Groovy console result toString threw an Error", e);
				return new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR);
			} catch (Throwable e) {
				// Same as above, Groovy code can throw checked Exceptions/Throwables without declaring them.
				LOGGER.warn("Groovy console result toString threw an unexpected Throwable", e);
				return new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR);
			}
			return new ConsoleExecResult(console.toString(), resultString, ConsoleExecResultType.PASSED);
		}
	}

	public void setCodeCompletion(GroovyConsole parent) {
		codeCompletion.setParent(parent);
	}

	public void triggerCodeCompletion(String currentLine, CodeCompletionTriggerAction action) {
		if (!codeCompletion.isVisible()) {
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
