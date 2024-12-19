package de.prob2.ui.consoles.b;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
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
import de.prob2.ui.consoles.AsyncExecutable;
import de.prob2.ui.consoles.ConsoleExecResult;
import de.prob2.ui.consoles.ConsoleExecResultType;
import de.prob2.ui.consoles.b.codecompletion.BCCItem;
import de.prob2.ui.consoles.b.codecompletion.BCodeCompletion;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BInterpreter implements AsyncExecutable {
	private static final Logger LOGGER = LoggerFactory.getLogger(BInterpreter.class);

	private final MachineLoader machineLoader;
	private final CurrentTrace currentTrace;
	private final CliTaskExecutor cliExecutor;

	private boolean bMode;

	@Inject
	public BInterpreter(final MachineLoader machineLoader, final CurrentTrace currentTrace, CliTaskExecutor cliExecutor) {
		this.machineLoader = machineLoader;
		this.currentTrace = currentTrace;
		this.cliExecutor = cliExecutor;
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
	public CompletableFuture<ConsoleExecResult> exec(String source) {
		if (source == null || source.isBlank()) {
			return CompletableFuture.completedFuture(new ConsoleExecResult("", "", ConsoleExecResultType.PASSED));
		} else if (":clear".equals(source.trim())) {
			return CompletableFuture.completedFuture(new ConsoleExecResult("", "", ConsoleExecResultType.CLEAR));
		}

		final Trace trace;
		if (this.currentTrace.get() != null) {
			trace = this.currentTrace.get();
		} else {
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
			return CompletableFuture.completedFuture(new ConsoleExecResult("", formatParseException(source, e), ConsoleExecResultType.ERROR));
		}

		return this.cliExecutor
				       .submit(() -> trace.evalCurrent(formula))
				       .handle((res, t) -> {
					       if (t instanceof CancellationException) {
						       return new ConsoleExecResult("", "cancelled", ConsoleExecResultType.ERROR);
					       } else if (t != null || !(res instanceof EvalResult)) {
						       LOGGER.warn("B evaluation failed", t);
						       return new ConsoleExecResult("", t != null ? t.getMessage() : Objects.toString(res), ConsoleExecResultType.ERROR);
					       } else {
						       return new ConsoleExecResult("", Objects.toString(res), ConsoleExecResultType.PASSED);
					       }
				       });
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
