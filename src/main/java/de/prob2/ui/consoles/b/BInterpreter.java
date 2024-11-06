package de.prob2.ui.consoles.b;

import java.util.List;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.TLAModel;
import de.prob.statespace.Trace;
import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.consoles.Executable;
import de.prob2.ui.consoles.b.codecompletion.BCCItem;
import de.prob2.ui.consoles.b.codecompletion.BCodeCompletion;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BInterpreter implements Executable {
	private static final Logger LOGGER = LoggerFactory.getLogger(BInterpreter.class);

	private final MachineLoader machineLoader;
	private final CurrentTrace currentTrace;

	private boolean bMode;

	@Inject
	public BInterpreter(final MachineLoader machineLoader, final CurrentTrace currentTrace) {
		this.machineLoader = machineLoader;
		this.currentTrace = currentTrace;
		this.bMode = false;
	}

	private static ErrorItem getParseErrorFromException(final Exception e) {
		if (!(e instanceof EvaluationException) || !(e.getCause() instanceof BCompoundException)) {
			return null;
		}
		final ProBError convertedError = new ProBError((BCompoundException) e.getCause());
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

	private Trace getDefaultTrace() {
		return new Trace(machineLoader.getActiveStateSpace());
	}

	@Override
	public ConsoleExecResult exec(String source) {
		if (source == null || source.isBlank()) {
			return new ConsoleExecResult("", "", ConsoleExecResultType.PASSED);
		} else if (":clear".equals(source)) {
			return new ConsoleExecResult("", "", ConsoleExecResultType.CLEAR);
		}

		Trace trace = this.currentTrace.get();
		if (trace == null) {
			trace = this.getDefaultTrace();
		}

		IEvalElement formula;
		try {
			AbstractModel model = trace.getModel();
			if (model instanceof TLAModel tlaModel && this.bMode) {
				formula = tlaModel.parseFormulaAsClassicalB(source, FormulaExpand.EXPAND);
			} else if (model instanceof EventBModel eventBModel && this.bMode) {
				formula = eventBModel.parseFormulaAsClassicalB(source, FormulaExpand.EXPAND);
			} else {
				formula = model.parseFormula(source, FormulaExpand.EXPAND);
			}

			// force parsing of the string representation because the eventb implementation is lazy
			// this also helps with error messages because eventb formulas swallow parse errors
			// when trying to print them as a prolog term
			if (formula instanceof IBEvalElement bFormula) {
				bFormula.getAst();
			}
		} catch (Exception e) {
			LOGGER.info("Failed to parse B console user input", e);
			return new ConsoleExecResult("", formatParseException(source, e), ConsoleExecResultType.ERROR);
		}

		AbstractEvalResult res;
		try {
			res = trace.evalCurrent(formula);
		} catch (Exception e) {
			LOGGER.info("B evaluation failed", e);
			return new ConsoleExecResult("", e.getMessage(), ConsoleExecResultType.ERROR);
		}

		return new ConsoleExecResult("", res.toString(), res instanceof EvalResult ? ConsoleExecResultType.PASSED : ConsoleExecResultType.ERROR);
	}

	public List<? extends BCCItem> getSuggestions(String text) {
		// TODO: we need to use the currently selected language instead of the global model language
		return BCodeCompletion.doCompletion(this.currentTrace.getStateSpace(), text, false);
	}

	public boolean isBMode() {
		return this.bMode;
	}

	public void setBMode(boolean bMode) {
		this.bMode = bMode;
	}
}
