package de.prob2.ui.consoles.b;

import java.util.Collections;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;

import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.consoles.ConsoleInstruction;
import de.prob2.ui.consoles.Executable;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BInterpreter implements Executable {

	private static final Logger logger = LoggerFactory.getLogger(BInterpreter.class);
	private final CurrentTrace currentTrace;
	private final ResourceBundle bundle;
	private final Trace defaultTrace;

	@Inject
	public BInterpreter(final MachineLoader machineLoader, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		this.defaultTrace = new Trace(machineLoader.getEmptyStateSpace(Collections.emptyMap()));
	}

	@Override
	public ConsoleExecResult exec(final ConsoleInstruction instruction) {
		if ("clear".equals(instruction.getInstruction())) {
			return new ConsoleExecResult("clear","", ConsoleExecResultType.PASSED);
		}
		final Trace trace = currentTrace.exists() ? currentTrace.get() : defaultTrace;
		final IEvalElement formula;
		try {
			formula = trace.getModel().parseFormula(instruction.getInstruction(), FormulaExpand.EXPAND);
		} catch (CliError | EvaluationException | ProBError e) {
			logger.info("Failed to parse B console user input", e);
			return new ConsoleExecResult("", String.format(bundle.getString("consoles.b.parsingFailed"), e.getMessage()), ConsoleExecResultType.ERROR);
		}
		try {
			AbstractEvalResult res = trace.evalCurrent(formula);
			// noinspection ObjectToString
			return new ConsoleExecResult("", res.toString(), ConsoleExecResultType.PASSED);
		} catch (CliError | EvaluationException | ProBError  e) {
			logger.info("B evaluation failed", e);
			return new ConsoleExecResult("", String.format(bundle.getString("consoles.b.evalFailed"), e.getMessage()), ConsoleExecResultType.ERROR);
		}
	}
}
