package de.prob2.ui.verifications.ltl.formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.ClassicalBParser;
import de.be4.classicalb.core.parser.IDefinitions;
import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.LTL;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLChecker;
import de.prob.check.LTLError;
import de.prob.exception.ProBError;
import de.prob.ltl.parser.LtlParser;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.LTLMarker;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class LTLFormulaChecker {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaChecker.class);
				
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final ListProperty<Thread> currentJobThreads;
	
	private final LTLResultHandler resultHandler;
	
	@Inject
	private LTLFormulaChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
			final LTLResultHandler resultHandler) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
	}
	
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			for (LTLFormulaItem item : machine.getLTLFormulas()) {
				this.checkFormula(item, machine);
				if(Thread.currentThread().isInterrupted()) {
					break;
				}
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public void checkFormula(LTLFormulaItem item, Machine machine) {
		if(!item.selected()) {
			return;
		}
		LtlParser parser = new LtlParser(item.getCode());
		parser.setPatternManager(machine.getPatternManager());
		List<LTLMarker> errorMarkers = new ArrayList<>();
		Object result = getResult(parser, errorMarkers, item);
		resultHandler.handleFormulaResult(item, errorMarkers, result, currentTrace.getStateSpace());
	}
	
	public void checkFormula(LTLFormulaItem item) {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			checkFormula(item, machine);
			if(item.getCounterExample() != null) {
				currentTrace.set(item.getCounterExample());
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}

	public void checkFormula(LTLFormulaItem item, LTLFormulaStage formulaStage) {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			checkFormula(item, machine);
			Platform.runLater(() -> formulaStage.showErrors(item.getResultItem()));
			if(item.getCounterExample() != null) {
				currentTrace.set(item.getCounterExample());
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	private Object getResult(LtlParser parser, List<LTLMarker> errorMarkers, LTLFormulaItem item) {
		LTLParseListener parseListener = parseFormula(parser);
		errorMarkers.addAll(parseListener.getErrorMarkers());
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
			}
			final LTLChecker checker = new LTLChecker(currentTrace.getStateSpace(), formula);
			final IModelCheckingResult res = checker.call();
			if(res instanceof LTLError) {
				//TODO
				//LTLError error = (LTLError) res;
				//errorMarkers.add(new LTLMarker("error", res.getTokenLine(), parseError.getTokenColumn(), parseError.getMessage().length(), error.getMessage()));
				errorMarkers.add(new LTLMarker("error", 0, 0, res.getMessage().length(), res.getMessage()));
			}
			return res;
		} catch (ProBError error) {
			logger.error("Could not parse LTL formula: ", error);
			if(error.getErrors() == null) {
				errorMarkers.add(new LTLMarker("error", 0, 0, error.getMessage().length(), error.getMessage()));
			} else {
				error.getErrors().stream()
						.flatMap(err -> err.getLocations().stream())
						.map(location -> new LTLMarker("error", location.getStartLine(), location.getStartColumn(), location.getEndColumn() - location.getStartColumn(), error.getMessage()))
						.collect(Collectors.toCollection(() -> errorMarkers));
			}
			return error;
		} catch (LtlParseException error) {
			logger.error("Could not parse LTL formula: ", error);
			errorMarkers.add(new LTLMarker("error", error.getTokenLine(), error.getTokenColumn(), error.toString().length(), error.toString()));
			return error;
		}
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
		currentJobThreads.forEach(Thread::interrupt);
		currentTrace.getStateSpace().sendInterrupt();
	}
	
	public BooleanExpression runningProperty() {
		return currentJobThreads.emptyProperty().not();
	}
	
	public boolean isRunning() {
		return this.runningProperty().get();
	}
}
