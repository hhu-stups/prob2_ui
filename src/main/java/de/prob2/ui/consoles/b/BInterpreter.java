package de.prob2.ui.consoles.b;

import com.google.inject.Inject;

import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.FormalismType;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.Executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BInterpreter implements IAnimationChangeListener, Executable {

	private static final Logger logger = LoggerFactory.getLogger(BInterpreter.class);
	private final StateSpace defaultSS;
	private String modelName;
	private Trace currentTrace;

	@Inject
	public BInterpreter(final ClassicalBFactory bfactory, final AnimationSelector animations) {
		StateSpace s = null;
		try {
			s = bfactory.create("MACHINE Empty END").load();
		} catch (CliError | ModelTranslationError | ProBError e) {
			logger.error("loading a model into ProB failed!", e);
		}
		defaultSS = s;
		animations.registerAnimationChangeListener(this);
	}

	@Override
	public ConsoleExecResult exec(final ConsoleInstruction instruction) {
		String line = instruction.getInstruction();
		AbstractEvalResult res;
		try {
			if ("clear".equals(instruction.getInstruction())) {
				return new ConsoleExecResult("clear","", ConsoleExecResultType.PASSED);
			}
			IEvalElement parsed = parse(line);
			if (currentTrace == null) {
				EvaluationCommand cmd = parsed.getCommand(defaultSS.getRoot());
				defaultSS.execute(cmd);
				res = cmd.getValue();
			} else {
				res = currentTrace.evalCurrent(parsed);
			}
		} catch (CliError | EvaluationException | ProBError e) {
			logger.info("B evaluation failed", e);
			return new ConsoleExecResult("", "Invalid syntax: " + e.getMessage(), ConsoleExecResultType.ERROR) ;
		}
		// noinspection ObjectToString
		return new ConsoleExecResult("", res.toString(), ConsoleExecResultType.PASSED);
	}
	
	public IEvalElement parse(final String line) {
		if (currentTrace == null) {
			return new ClassicalB(line);
		}
		return currentTrace.getModel().parseFormula(line);
	}
		
	@Override
	public void traceChange(final Trace currentTrace, final boolean currentAnimationChanged) {
		if (currentAnimationChanged) {
			if (currentTrace == null) {
				modelName = null;
			} else if (currentTrace.getModel().getFormalismType() == FormalismType.B) {
				// ignore models that are not B models
				String mainModelName = currentTrace.getStateSpace().getMainComponent().toString();
				if (!mainModelName.equals(this.modelName)) {
					this.modelName = mainModelName;
				}
			}
			this.currentTrace = currentTrace;
		}
	}

	@Override
	public void animatorStatus(final boolean busy) {
		// Not used
	}
}
