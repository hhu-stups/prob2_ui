package de.prob2.ui.groovy;

import javax.script.Bindings;
import javax.script.ScriptContext;
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

	public Pair exec(Instruction instruction) {
		String resultString = "";
		StringBuffer console = new StringBuffer();
		logger.trace("Exec");
		try {
			
			engine.put("__console", console);
			logger.trace("Eval {} on {}", instruction.getInstruction(), engine.toString());
			Object eval = engine.eval(instruction.getInstruction());
			resultString = eval.toString();
			logger.trace("Evaled {} to {}", instruction.getInstruction(), resultString);
			
		} catch (Exception e) {
			resultString = e.getMessage();
		}

		Bindings binding = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		for(int i = 0; i < binding.keySet().size(); i++) {
			System.out.println(binding.keySet().toArray()[i] + ": " + binding.values().toArray()[i]);
		}
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		Bindings binding1 = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
		for(int i = 0; i < binding1.keySet().size(); i++) {
			System.out.println(binding1.keySet().toArray()[i] + ": " + binding1.values().toArray()[i]);
		}
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
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
