package de.prob2.ui.consoles.b;

import com.google.inject.Inject;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.statespace.StateSpace;
import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.Executable;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class BInterpreter implements Executable {

	private static final Logger logger = LoggerFactory.getLogger(BInterpreter.class);
	private final StateSpace defaultSS;
	private CurrentTrace currentTrace;

	@Inject
	public BInterpreter(final MachineLoader machineLoader, final CurrentTrace currentTrace) {
		defaultSS = machineLoader.getEmptyStateSpace(Collections.emptyMap());
		this.currentTrace = currentTrace;
	}

	@Override
	public ConsoleExecResult exec(final ConsoleInstruction instruction) {
		String line = instruction.getInstruction();
		AbstractEvalResult res;
		try {
			if ("clear".equals(instruction.getInstruction())) {
				return new ConsoleExecResult("clear","", ConsoleExecResultType.PASSED);
			}
			if (currentTrace.exists()) {
				res = currentTrace.get().evalCurrent(currentTrace.getModel().parseFormula(line));
			} else {
				EvaluationCommand cmd = new ClassicalB(line).getCommand(defaultSS.getRoot());
				defaultSS.execute(cmd);
				res = cmd.getValue();
			}
		} catch (CliError | EvaluationException | ProBError e) {
			logger.info("B evaluation failed", e);
			return new ConsoleExecResult("", "Invalid syntax: " + e.getMessage(), ConsoleExecResultType.ERROR) ;
		}
		// noinspection ObjectToString
		return new ConsoleExecResult("", res.toString(), ConsoleExecResultType.PASSED);
	}
}
