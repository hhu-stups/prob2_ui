package de.prob2.ui.groovy;


import javax.script.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;

import de.prob.scripting.Api;
import de.prob.scripting.ScriptEngineProvider;
import de.prob.statespace.AnimationSelector;


public class GroovyInterpreter {
	private final Logger logger = LoggerFactory
			.getLogger(GroovyInterpreter.class);

	private final ScriptEngine engine;
	private final AnimationSelector animations;
	private final Api api;

	@Inject
	public GroovyInterpreter(/*final UUID id,*/ final ScriptEngineProvider sep, final AnimationSelector animations, final Api api) {
		engine = sep.get();
		this.animations = animations;
		this.api = api;
	}

	public String exec(String instruction) {
		String resultString = "";
		logger.trace("Exec");
		try {
			StringBuffer console = new StringBuffer();
			engine.put("__console", console);
			logger.trace("Eval {} on {}", instruction, engine.toString());
			Object eval = engine.eval(instruction);
			resultString = eval.toString();
			logger.trace("Evaled {} to {}", instruction, resultString);
			
		} catch (Exception e) {
			resultString = e.getMessage();
		}
		return resultString;

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
