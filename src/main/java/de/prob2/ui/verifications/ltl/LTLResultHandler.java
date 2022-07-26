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
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.verifications.AbstractVerificationsResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

@Singleton
public class LTLResultHandler extends AbstractVerificationsResultHandler {
	
	@Inject
	public LTLResultHandler(final StageManager stageManager, final I18n i18n) {
		super(stageManager, i18n);
		this.type = CheckingType.LTL;
	}
	
	@Override
	protected boolean isSuccess(final Object result) {
		return result instanceof LTLOk;
	}
	
	@Override
	protected boolean isCounterExample(final Object result) {
		return result instanceof LTLCounterExample;
	}
	
	@Override
	protected boolean isInterrupted(final Object result) {
		return result instanceof LTLNotYetFinished || result instanceof CheckInterrupted;
	}
	
	@Override
	protected boolean isParseError(final Object result) {
		return result instanceof Throwable || result instanceof LTLError;
	}
	
	public void handleFormulaResult(LTLFormulaItem item, IModelCheckingResult result) {
		assert !isParseError(result);
		if (result instanceof LTLCounterExample) {
			item.setCounterExample(((LTLCounterExample)result).getTraceToLoopEntry());
		} else {
			item.setCounterExample(null);
		}
		item.setResultItem(handleFormulaResult(result));
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
