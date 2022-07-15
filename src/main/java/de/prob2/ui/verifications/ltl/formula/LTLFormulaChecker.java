package de.prob2.ui.verifications.ltl.formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.ClassicalBParser;
import de.be4.classicalb.core.parser.IDefinitions;
import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.LTL;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLChecker;
import de.prob.check.LTLError;
import de.prob.exception.ProBError;
import de.prob.ltl.parser.LtlParser;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob2.ui.internal.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLMarker;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;

import javafx.beans.binding.BooleanExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LTLFormulaChecker {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaChecker.class);
	
	private final CliTaskExecutor cliExecutor;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final LTLResultHandler resultHandler;
	
	@Inject
	private LTLFormulaChecker(final CliTaskExecutor cliExecutor, final CurrentTrace currentTrace, final CurrentProject currentProject,
			final LTLResultHandler resultHandler) {
		this.cliExecutor = cliExecutor;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
	}
	
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		this.cliExecutor.submit(() -> {
			for (LTLFormulaItem item : machine.getLTLFormulas()) {
				this.checkFormula(item, machine);
				if(Thread.currentThread().isInterrupted()) {
					break;
				}
			}
		});
	}
	
	public void checkFormula(LTLFormulaItem item, Machine machine) {
		if(!item.selected()) {
			return;
		}
		LtlParser parser = new LtlParser(item.getCode());
		parser.setPatternManager(machine.getPatternManager());
		final LTLParseListener parseListener = parseFormula(parser);
		try {
			final LTL formula;
			BParser bParser = new BParser();
			if (currentTrace.get().getModel() instanceof ClassicalBModel) {
				IDefinitions definitions = ((ClassicalBModel) currentTrace.get().getModel()).getDefinitions();
				bParser.setDefinitions(definitions);
			}
			if(!parseListener.getErrorMarkers().isEmpty()) {
				formula = new LTL(item.getCode(), new ClassicalBParser(bParser));
			} else {
				formula = new LTL(item.getCode(), new ClassicalBParser(bParser), parser);
				if(!parseListener.getErrorMarkers().isEmpty()) {
					resultHandler.handleFormulaParseErrors(item, parseListener.getErrorMarkers());
					return;
				}
			}
			final LTLChecker checker = new LTLChecker(currentTrace.getStateSpace(), formula);
			final IModelCheckingResult result = checker.call();
			if (result instanceof LTLError) {
				resultHandler.handleFormulaParseErrors(item, ltlMarkersFromErrorItems(((LTLError)result).getErrors()));
			} else {
				resultHandler.handleFormulaResult(item, result);
			}
		} catch (ProBError error) {
			logger.error("Could not parse LTL formula: ", error);
			final List<LTLMarker> errorMarkers = new ArrayList<>(parseListener.getErrorMarkers());
			if(error.getErrors() == null) {
				errorMarkers.add(new LTLMarker("error", 1, 0, 1, error.getMessage()));
			} else {
				errorMarkers.addAll(ltlMarkersFromErrorItems(error.getErrors()));
			}
			resultHandler.handleFormulaParseErrors(item, errorMarkers);
		} catch (LtlParseException error) {
			logger.error("Could not parse LTL formula: ", error);
			final List<LTLMarker> errorMarkers = new ArrayList<>(parseListener.getErrorMarkers());
			if (error.getTokenString() == null) {
				errorMarkers.add(new LTLMarker("error", 1, 0, 1, error.getMessage()));
			} else {
				errorMarkers.add(new LTLMarker("error", error.getTokenLine(), error.getTokenColumn(), error.getTokenString().length(), error.getMessage()));
			}
			resultHandler.handleFormulaParseErrors(item, errorMarkers);
		}
	}
	
	public CompletableFuture<LTLFormulaItem> checkFormula(LTLFormulaItem item) {
		Machine machine = currentProject.getCurrentMachine();
		return this.cliExecutor.submit(() -> {
			checkFormula(item, machine);
			if(item.getCounterExample() != null) {
				currentTrace.set(item.getCounterExample());
			}
			return item;
		});
	}

	private static String ltlErrorTypeFromProB(final ErrorItem.Type type) {
		switch (type) {
			case MESSAGE:
			case WARNING:
				return "warning";
			
			case ERROR:
			case INTERNAL_ERROR:
			default:
				return "error";
		}
	}
	
	private static ErrorItem.Type proBErrorTypeFromLtl(final String type) {
		switch (type) {
			case "warning":
				return ErrorItem.Type.WARNING;
			
			case "error":
				return ErrorItem.Type.ERROR;
			
			default:
				logger.warn("Unhandled LTL error type: {}", type);
				return ErrorItem.Type.INTERNAL_ERROR;
		}
	}
	
	private static List<LTLMarker> ltlMarkersFromErrorItems(final List<ErrorItem> errors) {
		final List<LTLMarker> markers = new ArrayList<>();
		for (final ErrorItem error : errors) {
			final String type = ltlErrorTypeFromProB(error.getType());
			if (error.getLocations().isEmpty()) {
				markers.add(new LTLMarker(type, 1, 0, 1, error.getMessage()));
			} else {
				for (final ErrorItem.Location location : error.getLocations()) {
					final int length;
					if (location.getStartLine() == location.getEndLine()) {
						length = location.getEndColumn() - location.getStartColumn();
					} else {
						// Don't have the original LTL formula here to calculate the length of multi-line spans...
						length = 1;
					}
					markers.add(new LTLMarker(type, location.getStartLine(), location.getStartColumn(), length, error.getMessage()));
				}
			}
		}
		return markers;
	}
	
	private static List<ErrorItem> errorItemsFromLtlMarkers(final List<LTLMarker> markers) {
		return markers.stream()
			.map(marker -> new ErrorItem(marker.getMsg(), proBErrorTypeFromLtl(marker.getType()), Collections.emptyList()))
			.collect(Collectors.toList());
	}
	
	private LTLParseListener parseFormula(LtlParser parser) {
		LTLParseListener parseListener = new LTLParseListener();
		parser.removeErrorListeners();
		parser.addErrorListener(parseListener);
		parser.addWarningListener(parseListener);
		parser.parse();
		return parseListener;
	}
	
	public void cancel() {
		cliExecutor.interruptAll();
		currentTrace.getStateSpace().sendInterrupt();
	}
	
	public BooleanExpression runningProperty() {
		return cliExecutor.runningProperty();
	}
	
	public boolean isRunning() {
		return this.runningProperty().get();
	}
}
