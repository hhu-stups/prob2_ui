package de.prob2.ui.groovy;

import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.scripting.ScriptEngineProvider;


public class GroovyInterpreter {
	private final Logger logger = LoggerFactory
			.getLogger(GroovyInterpreter.class);

	private final ScriptEngine engine;

	@Inject
	public GroovyInterpreter(final ScriptEngineProvider sep) {
		engine = sep.get();
	}

	public Pair exec(String instruction) {
		String resultString = "";
		StringBuffer console = new StringBuffer();
		logger.trace("Exec");
		try {
			
			engine.put("__console", console);
			logger.trace("Eval {} on {}", instruction, engine.toString());
			Object eval = engine.eval(instruction);
			resultString = eval.toString();
			logger.trace("Evaled {} to {}", instruction, resultString);
			
		} catch (Exception e) {
			resultString = e.getMessage();
		}
		return new Pair(console.toString(), resultString);
	}

	private String extractTrace(final StackTraceElement[] stackTrace) {
		if (stackTrace.length == 0) {
			return "";
		}
		if (stackTrace.length == 1) {
			return "at " + stackTrace[0].toString();
		}
		return "at " + stackTrace[0].toString()
				+ System.getProperty("line.separator") + "...";
	}

	/*@Override
	public void reload(String client, int lastinfo, AsyncContext context) {
		sendInitMessage(context);
	}*/
}
