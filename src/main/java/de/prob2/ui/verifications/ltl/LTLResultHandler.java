package de.prob2.ui.verifications.ltl;

import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLNotYetFinished;
import de.prob.check.LTLOk;
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

@Singleton
public class LTLResultHandler extends AbstractResultHandler {
	@Inject
	public LTLResultHandler(final StageManager stageManager, final I18n i18n) {
		super(stageManager, i18n);
	}
	
	public void handleFormulaResult(LTLFormulaItem item, IModelCheckingResult result) {
		assert !(result instanceof LTLError);
		
		if (result instanceof LTLCounterExample) {
			item.setCounterExample(((LTLCounterExample)result).getTraceToLoopEntry());
		} else {
			item.setCounterExample(null);
		}
		
		if (result instanceof LTLOk) {
			item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.result.succeeded.header", "verifications.ltl.result.succeeded.message"));
		} else if (result instanceof LTLCounterExample) {
			item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.result.counterExampleFound.header", "verifications.ltl.result.counterExampleFound.message"));
		} else if (result instanceof LTLNotYetFinished || result instanceof CheckInterrupted) {
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "common.result.interrupted.header", "common.result.message", result.getMessage()));
		} else {
			throw new AssertionError("Unhandled LTL checking result type: " + result.getClass());
		}
	}
	
	public void handleFormulaParseErrors(LTLFormulaItem item, List<ErrorItem> errorMarkers) {
		item.setCounterExample(null);
		String errorMessage = errorMarkers.stream().map(ErrorItem::getMessage).collect(Collectors.joining("\n"));
		if(errorMessage.isEmpty()) {
			errorMessage = "Parse Error in typed formula";
		}
		item.setResultItem(new LTLCheckingResultItem(Checked.PARSE_ERROR, errorMarkers, "common.result.couldNotParseFormula.header", "common.result.message", errorMessage));
	}
	
	public void handlePatternResult(LTLParseListener parseListener, LTLPatternItem item) {
		CheckingResultItem resultItem;
		// Empty Patterns do not have parse errors which is a little bit confusing
		if(parseListener.getErrorMarkers().isEmpty() && !item.getCode().isEmpty()) {
			resultItem = new LTLCheckingResultItem(Checked.SUCCESS, parseListener.getErrorMarkers(), "verifications.result.patternParsedSuccessfully", "verifications.result.patternParsedSuccessfully");
		} else {
			String msg;
			List<ErrorItem> errorMarkers = parseListener.getErrorMarkers();
			if(item.getCode().isEmpty()) {
				msg = i18n.translate("verifications.ltl.pattern.empty");
			} else {
				msg = parseListener.getErrorMarkers().stream().map(ErrorItem::getMessage).collect(Collectors.joining("\n"));
			}
			resultItem = new LTLCheckingResultItem(Checked.PARSE_ERROR, errorMarkers, "verifications.result.couldNotParsePattern.header",
					"common.result.message", msg);
		}
		item.setResultItem(resultItem);
	}
}
