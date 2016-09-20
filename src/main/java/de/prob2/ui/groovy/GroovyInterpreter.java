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
		
	private final GroovyObjectStage groovyObjectStage;
	

	@Inject
	public GroovyInterpreter(final ScriptEngineProvider sep, final GroovyObjectStage groovyObjectStage) {
		engine = sep.get();
		this.groovyObjectStage = groovyObjectStage;
	}

	public Pair exec(Instruction instruction) {
		String resultString = "";
		StringBuilder console = new StringBuilder();
		logger.trace("Exec");
		try {
			if("inspect".equals(instruction.getInstruction())) {
				groovyObjectStage.showObjects(engine);
				return new Pair("", "result");
			}
			engine.put("__console", console);
			logger.trace("Eval {} on {}", instruction.getInstruction(), engine);
			Object eval = engine.eval(instruction.getInstruction());
			resultString = eval.toString();
			logger.trace("Evaled {} to {}", instruction.getInstruction(), resultString);
			
		} catch (Exception e) {
			logger.debug("Groovy Evaluation failed",e);
			resultString = e.getMessage();
		}

		return new Pair(console.toString(), resultString);
	}
	
	public void closeObjectStage() {
		groovyObjectStage.close();
	}

}
