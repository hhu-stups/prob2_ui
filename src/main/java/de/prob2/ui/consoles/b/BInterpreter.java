package de.prob2.ui.consoles.b;

import java.util.Collection;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
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

	@Inject
	public BInterpreter(final MachineLoader machineLoader, final CurrentTrace currentTrace) {
		this.machineLoader = machineLoader;
		this.currentTrace = currentTrace;
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

	private static boolean isIdentifierStart(char c) {
		return Character.isJavaIdentifierStart(c) && !Character.isIdentifierIgnorable(c);
	}

	private static boolean isIdentifierPart(char c) {
		return Character.isJavaIdentifierPart(c) && !Character.isIdentifierIgnorable(c);
	}

	private static boolean isIdentifierChar(String text, int index) {
		char c = text.charAt(index);

		if (index == 0) {
			return isIdentifierStart(c);
		} else {
			char p = text.charAt(index - 1);
			if (isIdentifierStart(p) || isIdentifierPart(p)) {
				return isIdentifierPart(c);
			} else {
				return isIdentifierStart(c);
			}
		}
	}

	private static String extractPrefix(String text) {
		if (text.isEmpty()) {
			return "";
		}

		int first = text.length();
		while (first > 0 && isIdentifierChar(text, first - 1)) {
			first--;
		}

		return text.substring(first);
	}

	private Trace getDefaultTrace() {
		return new Trace(machineLoader.getActiveStateSpace());
	}

	@Override
	public ConsoleExecResult exec(String source) {
		if (":clear".equals(source)) {
			return new ConsoleExecResult("", "", ConsoleExecResultType.CLEAR);
		}
		if (source.replace(" ", "").isEmpty()) {
			return new ConsoleExecResult("", "", ConsoleExecResultType.PASSED);
		}
		Trace trace = currentTrace.get();
		if (trace == null) {
			trace = this.getDefaultTrace();
		}
		final IEvalElement formula;
		try {
			formula = trace.getModel().parseFormula(source, FormulaExpand.EXPAND);
		} catch (EvaluationException e) {
			LOGGER.info("Failed to parse B console user input", e);
			return new ConsoleExecResult("", formatParseException(source, e), ConsoleExecResultType.ERROR);
		}
		final AbstractEvalResult res;
		try {
			res = trace.evalCurrent(formula);
		} catch (EvaluationException | ProBError e) {
			LOGGER.info("B evaluation failed", e);
			return new ConsoleExecResult("", e.getMessage(), ConsoleExecResultType.ERROR);
		}
		return new ConsoleExecResult("", res.toString(), res instanceof EvalResult ? ConsoleExecResultType.PASSED : ConsoleExecResultType.ERROR);
	}

	public Collection<? extends BCCItem> getSuggestions(String text) {
		Trace trace = this.currentTrace.get();
		if (trace == null) {
			trace = this.getDefaultTrace();
		}

		BCodeCompletion cc = new BCodeCompletion(trace.getModel(), extractPrefix(text));
		cc.find();
		return cc.getSuggestions();
	}
}
