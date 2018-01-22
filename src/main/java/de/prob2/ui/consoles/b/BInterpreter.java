package de.prob2.ui.consoles.b;

import java.util.Collections;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.exceptions.BLexerException;
import de.be4.classicalb.core.parser.exceptions.BParseException;
import de.be4.classicalb.core.parser.exceptions.CheckException;
import de.be4.classicalb.core.parser.exceptions.PreParseException;
import de.be4.classicalb.core.parser.parser.ParserException;
import de.be4.eventbalg.core.parser.EventBLexerException;
import de.be4.eventbalg.core.parser.EventBParseException;

import de.hhu.stups.sablecc.patch.SourcePosition;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EnumerationWarning;
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
	private static final class ParseError {
		private final int line;
		private final int column;
		private final String message;
		
		private ParseError(final int line, final int column, final String message) {
			super();
			
			Objects.requireNonNull(message);
			
			this.line = line;
			this.column = column;
			this.message = message;
		}
		
		private int getLine() {
			return this.line;
		}
		
		private int getColumn() {
			return this.column;
		}
		
		private String getMessage() {
			return this.message;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(BInterpreter.class);
	private static final Pattern MESSAGE_WITH_POSITIONS_PATTERN = Pattern.compile("^\\[(\\d+),(\\d+)\\] (.*)$");
	
	private final CurrentTrace currentTrace;
	private final ResourceBundle bundle;
	private final Trace defaultTrace;
	private final int promptLength;

	@Inject
	public BInterpreter(final MachineLoader machineLoader, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		this.defaultTrace = new Trace(machineLoader.getEmptyStateSpace(Collections.emptyMap()));
		this.promptLength = bundle.getString("consoles.prompt.default").length();
	}
	
	// The exceptions thrown while parsing are not standardized in any way.
	// This method tries to extract line/column info from an exception.
	// First it checks if the exception has programmatically readable position info and uses that if possible.
	// If that fails, it tries to parse SableCC-style position info from the exception message.
	// If that also fails, null is returned.
	private ParseError getParseErrorFromException(final Exception e) {
		if ((
			e instanceof EvaluationException
			|| e instanceof BException
			|| e instanceof de.be4.eventbalg.core.parser.BException
		) && e.getCause() instanceof Exception) {
			// Check for known "wrapper exceptions" and look at their cause instead.
			return this.getParseErrorFromException((Exception)e.getCause());
		} else if (e instanceof BCompoundException && !((BCompoundException)e).getBExceptions().isEmpty()) {
			return this.getParseErrorFromException(((BCompoundException)e).getBExceptions().get(0));
		} else if (e instanceof PreParseException && ((PreParseException)e).getTokens().length > 0) {
			final PreParseException ex = (PreParseException)e;
			return new ParseError(ex.getTokens()[0].getLine(), ex.getTokens()[0].getPos(), e.getMessage());
		} else if (e instanceof BLexerException) {
			final BLexerException ex = (BLexerException)e;
			return new ParseError(ex.getLastLine(), ex.getLastPos(), e.getMessage());
		} else if (e instanceof EventBLexerException) {
			final EventBLexerException ex = (EventBLexerException)e;
			return new ParseError(ex.getLastLine(), ex.getLastPos(), e.getMessage());
		} else if (e instanceof BParseException) {
			final BParseException ex = (BParseException)e;
			return new ParseError(ex.getToken().getLine(), ex.getToken().getPos(), ex.getRealMsg());
		} else if (e instanceof EventBParseException) {
			final EventBParseException ex = (EventBParseException)e;
			return new ParseError(ex.getToken().getLine(), ex.getToken().getPos(), e.getMessage());
		} else if (e instanceof de.be4.classicalb.core.preparser.parser.ParserException) {
			final de.be4.classicalb.core.preparser.parser.ParserException ex =
				(de.be4.classicalb.core.preparser.parser.ParserException)e;
			return new ParseError(ex.getToken().getLine(), ex.getToken().getPos(), ex.getRealMsg());
		} else if (e instanceof ParserException) {
			final ParserException ex = (ParserException)e;
			return new ParseError(ex.getToken().getLine(), ex.getToken().getPos(), ex.getRealMsg());
		} else if (e instanceof de.be4.eventbalg.core.parser.parser.ParserException) {
			final de.be4.eventbalg.core.parser.parser.ParserException ex =
				(de.be4.eventbalg.core.parser.parser.ParserException)e;
			return new ParseError(ex.getToken().getLine(), ex.getToken().getPos(), ex.getRealMsg());
		} else if (e instanceof CheckException && ((CheckException)e).getNodes().length > 0) {
			final SourcePosition pos = ((CheckException)e).getNodes()[0].getStartPos();
			return new ParseError(pos.getLine(), pos.getPos(), e.getMessage()
			);
		} else {
			// The exception doesn't have any accessible position info.
			// Look for SableCC-style position info in the error message and try to parse it.
			final Matcher matcher = MESSAGE_WITH_POSITIONS_PATTERN.matcher(e.getMessage());
			return matcher.find() ? new ParseError(
				Integer.parseInt(matcher.group(1)),
				Integer.parseInt(matcher.group(2)),
				matcher.group(3)
			) : null;
		}
	}
	
	private String formatParseException(final Exception e) {
		final ParseError error = this.getParseErrorFromException(e);
		
		if (error != null) {
			return String.format("%" + (error.getColumn() + this.promptLength) + "s\n%s", '^', error.getMessage());
		} else {
			return String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage());
		}
	}
	
	private String formatResult(final AbstractEvalResult result) {
		Objects.requireNonNull(result);
		final StringBuilder sb = new StringBuilder();
		if (result instanceof EvalResult) {
			sb.append(((EvalResult)result).getValue());
		} else if (result instanceof EvaluationErrorResult) {
			if (result instanceof IdentifierNotInitialised) {
				sb.append(bundle.getString("consoles.b.result.notInitialized"));
			} else if (result instanceof WDError) {
				sb.append(bundle.getString("consoles.b.result.notWellDefined"));
			} else {
				sb.append(bundle.getString("consoles.b.result.evaluationError"));
			}
			for (final String s : ((EvaluationErrorResult)result).getErrors()) {
				sb.append('\n');
				sb.append(s);
			}
		} else if (result instanceof EnumerationWarning) {
			sb.append(bundle.getString("consoles.b.result.enumerationWarning"));
		} else if (result instanceof ComputationNotCompletedResult) {
			sb.append(bundle.getString("consoles.b.result.computationNotCompleted"));
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
		if ("clear".equals(source)) {
			return new ConsoleExecResult("clear","", ConsoleExecResultType.PASSED);
		}
		final Trace trace = currentTrace.exists() ? currentTrace.get() : defaultTrace;
		final IEvalElement formula;
		try {
			formula = trace.getModel().parseFormula(source, FormulaExpand.EXPAND);
		} catch (EvaluationException e) {
			logger.info("Failed to parse B console user input", e);
			return new ConsoleExecResult("", this.formatParseException(e), ConsoleExecResultType.ERROR);
		}
		final AbstractEvalResult res;
		try {
			res = trace.evalCurrent(formula);
		} catch (ProBError e) {
			logger.info("B evaluation failed", e);
			return new ConsoleExecResult("", e.getMessage(), ConsoleExecResultType.ERROR);
		}
		return new ConsoleExecResult("", this.formatResult(res), ConsoleExecResultType.PASSED);
	}
}
