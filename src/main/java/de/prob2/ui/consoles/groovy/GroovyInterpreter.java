package de.prob2.ui.consoles.groovy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.google.inject.Inject;

import de.prob.scripting.ScriptEngineProvider;
import de.prob2.ui.consoles.AsyncExecutable;
import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.consoles.groovy.codecompletion.GroovyCCItem;
import de.prob2.ui.consoles.groovy.codecompletion.GroovyCodeCompletion;
import de.prob2.ui.consoles.groovy.objects.GroovyObjectStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GroovyInterpreter implements AsyncExecutable {

	private static final Logger LOGGER = LoggerFactory.getLogger(GroovyInterpreter.class);

	private final ScriptEngine engine;
	private final GroovyCodeCompletion codeCompletion;
	private final GroovyObjectStage groovyObjectStage;

	@Inject
	private GroovyInterpreter(final ScriptEngineProvider sep, final GroovyObjectStage groovyObjectStage) {
		engine = sep.get();
		this.groovyObjectStage = groovyObjectStage;
		this.codeCompletion = new GroovyCodeCompletion(engine);
	}

	@Override
	public CompletableFuture<ConsoleExecResult> exec(String instruction) {
		if ("inspect".equals(instruction)) {
			groovyObjectStage.showObjects(engine);
			return CompletableFuture.completedFuture(new ConsoleExecResult("", "", ConsoleExecResultType.PASSED));
		} else if ("clear".equals(instruction)) {
			return CompletableFuture.completedFuture(new ConsoleExecResult("", "", ConsoleExecResultType.CLEAR));
		} else {
			final StringBuilder console = new StringBuilder();
			engine.put("__console", console);
			final Object evalResult;
			try {
				evalResult = engine.eval(instruction);
			} catch (ScriptException e) {
				LOGGER.debug("Groovy console user code threw an exception", e);
				return CompletableFuture.completedFuture(new ConsoleExecResult(console.toString(), e.getCause().toString(), ConsoleExecResultType.ERROR));
			} catch (Error e) {
				LOGGER.warn("Groovy console user code threw an Error", e);
				return CompletableFuture.completedFuture(new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR));
			} catch (Throwable e) {
				// Groovy does not enforce declaration of checked Exceptions/Throwables like Java does,
				// which means that the eval call can throw a checked Throwable even though it has no checked exceptions declared.
				// The Groovy script engine wraps Exceptions (including checked ones) in a ScriptException, but not Throwables,
				// so we need to manually catch them here.
				LOGGER.warn("Groovy console user code threw an unexpected Throwable", e);
				return CompletableFuture.completedFuture(new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR));
			}
			final String resultString;
			try {
				resultString = evalResult.toString();
			} catch (RuntimeException e) {
				LOGGER.debug("Groovy console result toString threw an exception", e);
				return CompletableFuture.completedFuture(new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR));
			} catch (Error e) {
				LOGGER.warn("Groovy console result toString threw an Error", e);
				return CompletableFuture.completedFuture(new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR));
			} catch (Throwable e) {
				// Same as above, Groovy code can throw checked Exceptions/Throwables without declaring them.
				LOGGER.warn("Groovy console result toString threw an unexpected Throwable", e);
				return CompletableFuture.completedFuture(new ConsoleExecResult(console.toString(), e.toString(), ConsoleExecResultType.ERROR));
			}
			return CompletableFuture.completedFuture(new ConsoleExecResult(console.toString(), resultString, ConsoleExecResultType.PASSED));
		}
	}

	public void closeObjectStage() {
		groovyObjectStage.close();
	}

	public List<? extends GroovyCCItem> getSuggestions(String text) {
		return codeCompletion.getSuggestions(text);
	}
}
