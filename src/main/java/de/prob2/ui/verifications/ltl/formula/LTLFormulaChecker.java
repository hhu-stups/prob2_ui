package de.prob2.ui.verifications.ltl.formula;

import java.util.ArrayList;
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
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLChecker;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLNotYetFinished;
import de.prob.check.LTLOk;
import de.prob.exception.ProBError;
import de.prob.ltl.parser.LtlParser;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.parserbase.ProBParserBase;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLCheckingResultItem;
import de.prob2.ui.verifications.ltl.LTLParseListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LTLFormulaChecker {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LTLFormulaChecker.class);
	
	private final CliTaskExecutor cliExecutor;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	@Inject
	private LTLFormulaChecker(final CliTaskExecutor cliExecutor, final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.cliExecutor = cliExecutor;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
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
			LOGGER.warn("Failed to parse LTL formula using ANTLR-based parser! Retrying using SableCC-based parser without pattern support. Formula: {}", code);
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
	
	public LTL parseFormula(final String code, final Machine machine) {
		BParser bParser = new BParser();
		if (currentTrace.get().getModel() instanceof ClassicalBModel) {
			IDefinitions definitions = ((ClassicalBModel) currentTrace.get().getModel()).getDefinitions();
			bParser.setDefinitions(definitions);
		}
		return LTLFormulaChecker.parseFormula(code, new ClassicalBParser(bParser), machine.getPatternManager());
	}
	
	private static void handleFormulaResult(LTLFormulaItem item, IModelCheckingResult result) {
		assert !(result instanceof LTLError);
		
		if (result instanceof LTLCounterExample) {
			item.setCounterExample(((LTLCounterExample)result).getTraceToLoopEntry());
		} else {
			item.setCounterExample(null);
		}
		
		if (result instanceof LTLOk) {
			if(item.getExpectedResult()) {
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.ltl.result.succeeded.message"));
			} else {
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.ltl.result.counterExampleFound.message"));
			}
		} else if (result instanceof LTLCounterExample) {
			if(item.getExpectedResult()) {
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.ltl.result.counterExampleFound.message"));
			} else {
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.ltl.result.succeeded.example.message"));
			}
		} else if (result instanceof LTLNotYetFinished || result instanceof CheckInterrupted) {
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", result.getMessage()));
		} else {
			throw new AssertionError("Unhandled LTL checking result type: " + result.getClass());
		}
	}
	
	private static void handleFormulaParseErrors(LTLFormulaItem item, List<ErrorItem> errorMarkers) {
		item.setCounterExample(null);
		String errorMessage = errorMarkers.stream().map(ErrorItem::getMessage).collect(Collectors.joining("\n"));
		if(errorMessage.isEmpty()) {
			errorMessage = "Parse Error in typed formula";
		}
		item.setResultItem(new LTLCheckingResultItem(Checked.PARSE_ERROR, errorMarkers, "common.result.message", errorMessage));
	}
	
	public void checkFormula(LTLFormulaItem item, Machine machine) {
		BParser bParser = new BParser();
		if (currentTrace.get().getModel() instanceof ClassicalBModel) {
			IDefinitions definitions = ((ClassicalBModel) currentTrace.get().getModel()).getDefinitions();
			bParser.setDefinitions(definitions);
		}
		try {
			final LTL formula = this.parseFormula(item.getCode(), machine);
			final LTLChecker checker = new LTLChecker(currentTrace.getStateSpace(), formula);
			final IModelCheckingResult result = checker.call();
			if (result instanceof LTLError) {
				handleFormulaParseErrors(item, ((LTLError)result).getErrors());
			} else {
				handleFormulaResult(item, result);
			}
		} catch (ProBError error) {
			LOGGER.error("Could not parse LTL formula: ", error);
			handleFormulaParseErrors(item, error.getErrors());
		}
	}
	
	public CompletableFuture<LTLFormulaItem> checkFormulaNoninteractive(LTLFormulaItem item) {
		Machine machine = currentProject.getCurrentMachine();
		return this.cliExecutor.submit(() -> {
			checkFormula(item, machine);
			return item;
		});
	}
}
