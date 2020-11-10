package de.prob2.ui.consoles.b;

import java.util.Objects;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.animator.domainobjects.WDError;
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
	
	private final MachineLoader machineLoader;
	private final CurrentTrace currentTrace;
	private final ResourceBundle bundle;
	private Trace defaultTrace;

	@Inject
	public BInterpreter(final MachineLoader machineLoader, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		this.machineLoader = machineLoader;
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		this.defaultTrace = null;
	}
	
	private Trace getDefaultTrace() {
		if (defaultTrace == null) {
			defaultTrace = new Trace(machineLoader.getEmptyStateSpace());
		}
		return defaultTrace;
	}
	
	private static ErrorItem getParseErrorFromException(final Exception e) {
		if (!(e instanceof EvaluationException) || !(e.getCause() instanceof BCompoundException)) {
			return null;
		}
		final ProBError convertedError = new ProBError((BCompoundException)e.getCause());
		if (convertedError.getErrors().isEmpty()) {
			return null;
		}
		final ErrorItem firstError = convertedError.getErrors().get(0);
		if (firstError.getLocations().isEmpty()) {
			return null;
		}
		return firstError;
	}
	
	private static String formatParseException(final String source, final Exception e) {
		final ErrorItem error = getParseErrorFromException(e);
		
		if (error != null) {
			assert !error.getLocations().isEmpty();
			final int startColumn = error.getLocations().get(0).getStartColumn();
			return String.format("%s\n%" + (startColumn + 1) + "s\n%s", source, '^', error.getMessage());
		} else {
			return String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage());
		}
	}
	
	private String formatResult(final AbstractEvalResult result) {
		Objects.requireNonNull(result);
		final StringBuilder sb = new StringBuilder();
		if (result instanceof EvalResult) {
			sb.append(result);
		} else if (result instanceof EvaluationErrorResult) {
			if (result instanceof IdentifierNotInitialised) {
				sb.append(bundle.getString("consoles.b.interpreter.result.notInitialized"));
			} else if (result instanceof WDError) {
				sb.append(bundle.getString("consoles.b.interpreter.result.notWellDefined"));
			} else {
				sb.append(bundle.getString("consoles.b.interpreter.result.evaluationError"));
			}
			for (final String s : ((EvaluationErrorResult)result).getErrors()) {
				sb.append('\n');
				sb.append(s);
			}
		} else if (result instanceof EnumerationWarning) {
			sb.append(bundle.getString("consoles.b.interpreter.result.enumerationWarning"));
		} else if (result instanceof ComputationNotCompletedResult) {
			sb.append(bundle.getString("consoles.b.interpreter.result.computationNotCompleted"));
			final String reason = ((ComputationNotCompletedResult)result).getReason();
			if (!reason.isEmpty()) {
				sb.append('\n');
				sb.append(reason);
			}
		} else {
			throw new IllegalArgumentException("Don't know how to show the value of a " + result.getClass() + " instance");
		}
		return sb.toString();
	}

	@Override
	public ConsoleExecResult exec(final ConsoleInstruction instruction) {
		final String source = instruction.getInstruction();
		if (":clear".equals(source)) {
			return new ConsoleExecResult("","", ConsoleExecResultType.CLEAR);
		}
		if(source.replace(" ", "").isEmpty()) {
			return new ConsoleExecResult("","", ConsoleExecResultType.PASSED);
		}
		Trace trace = currentTrace.get();
		if (trace == null) {
			trace = this.getDefaultTrace();
		}
		final IEvalElement formula;
		try {
			formula = trace.getModel().parseFormula(source, FormulaExpand.EXPAND);
		} catch (EvaluationException e) {
			logger.info("Failed to parse B console user input", e);
			return new ConsoleExecResult("", formatParseException(source, e), ConsoleExecResultType.ERROR);
		}
		final AbstractEvalResult res;
		try {
			res = trace.evalCurrent(formula);
		} catch (EvaluationException | ProBError e) {
			logger.info("B evaluation failed", e);
			return new ConsoleExecResult("", e.getMessage(), ConsoleExecResultType.ERROR);
		}
		if(res instanceof EvaluationErrorResult || res instanceof ComputationNotCompletedResult) {
			return new ConsoleExecResult("", this.formatResult(res), ConsoleExecResultType.ERROR);
		} else {
			return new ConsoleExecResult("", this.formatResult(res), ConsoleExecResultType.PASSED);
		}
	}
}
