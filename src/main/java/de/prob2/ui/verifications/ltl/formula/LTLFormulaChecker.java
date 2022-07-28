package de.prob2.ui.verifications.ltl.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.parserbase.ProBParserBase;
import de.prob2.ui.internal.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
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
	
	public static LTL parseFormula(final String code, final ProBParserBase languageSpecificParser, final PatternManager patternManager) {
		LtlParser parser = new LtlParser(code);
		parser.setPatternManager(patternManager);
		parser.removeErrorListeners();
		LTLParseListener parseListener = new LTLParseListener();
		parser.addErrorListener(parseListener);
		parser.parse();
		
		if (parseListener.getErrorMarkers().isEmpty()) {
			final LTL formula = new LTL(code, languageSpecificParser, parser);
			if (!parseListener.getErrorMarkers().isEmpty()) {
				throw new ProBError(parseListener.getErrorMarkers());
			}
			return formula;
		} else {
			logger.warn("Failed to parse LTL formula using ANTLR-based parser! Retrying using SableCC-based parser without pattern support. Formula: {}", code);
			try {
				return new LTL(code, languageSpecificParser);
			} catch (LtlParseException error) {
				final List<ErrorItem> errorMarkers = new ArrayList<>(parseListener.getErrorMarkers());
				final List<ErrorItem.Location> locations = new ArrayList<>();
				if (error.getTokenString() != null) {
					locations.add(new ErrorItem.Location("", error.getTokenLine(), error.getTokenColumn(), error.getTokenLine(), error.getTokenColumn() + error.getTokenString().length()));
				}
				errorMarkers.add(new ErrorItem(error.getMessage(), ErrorItem.Type.ERROR, locations));
				throw new ProBError(error.getMessage(), errorMarkers, error);
			}
		}
	}
	
	public void checkFormula(LTLFormulaItem item, Machine machine) {
		if(!item.selected()) {
			return;
		}
		BParser bParser = new BParser();
		if (currentTrace.get().getModel() instanceof ClassicalBModel) {
			IDefinitions definitions = ((ClassicalBModel) currentTrace.get().getModel()).getDefinitions();
			bParser.setDefinitions(definitions);
		}
		try {
			final LTL formula = LTLFormulaChecker.parseFormula(item.getCode(), new ClassicalBParser(bParser), machine.getPatternManager());
			final LTLChecker checker = new LTLChecker(currentTrace.getStateSpace(), formula);
			final IModelCheckingResult result = checker.call();
			if (result instanceof LTLError) {
				resultHandler.handleFormulaParseErrors(item, ((LTLError)result).getErrors());
			} else {
				resultHandler.handleFormulaResult(item, result);
			}
		} catch (ProBError error) {
			logger.error("Could not parse LTL formula: ", error);
			resultHandler.handleFormulaParseErrors(item, error.getErrors());
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
