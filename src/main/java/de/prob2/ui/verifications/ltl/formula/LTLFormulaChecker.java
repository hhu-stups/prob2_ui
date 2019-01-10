package de.prob2.ui.verifications.ltl.formula;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.ClassicalBParser;
import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.LTL;
import de.prob.exception.ProBError;
import de.prob.ltl.parser.LtlParser;
import de.prob.statespace.State;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;

@FXMLInjected
@Singleton
public class LTLFormulaChecker {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaChecker.class);
				
	private final CurrentTrace currentTrace;
	
	private final LTLResultHandler resultHandler;
	
	private final Injector injector;
	
	@Inject
	private LTLFormulaChecker(final CurrentTrace currentTrace, final LTLResultHandler resultHandler,
								final Injector injector) {
		this.currentTrace = currentTrace;
		this.resultHandler = resultHandler;
		this.injector = injector;
	}
	
	public void checkMachine(Machine machine) {
		final ArrayList<Boolean> failed = new ArrayList<>();
		failed.add(false);
		for (LTLFormulaItem item : machine.getLTLFormulas()) {
			Checked result = this.checkFormula(item, machine);
			if(result == Checked.FAIL) {
				failed.set(0, true);
				machine.setLtlStatus(Machine.CheckingStatus.FAILED);
			}
			item.setChecked(result);
			if(Thread.currentThread().isInterrupted()) {
				return;
			}
		}
		Platform.runLater(() -> injector.getInstance(StatusBar.class).setLtlStatus(failed.get(0) ? StatusBar.CheckingStatus.ERROR : StatusBar.CheckingStatus.SUCCESSFUL));
	
	}
	
	public Checked checkFormula(LTLFormulaItem item, Machine machine) {
		if(!item.shouldExecute()) {
			return Checked.NOT_CHECKED;
		}
		State stateid = currentTrace.getCurrentState();
		LtlParser parser = new LtlParser(item.getCode());
		parser.setPatternManager(machine.getPatternManager());
		return resultHandler.handleFormulaResult(item, getResult(parser, item), stateid);
	}
	
	private Object getResult(LtlParser parser, LTLFormulaItem item) {
		State stateid = currentTrace.getCurrentState();
		LTLParseListener parseListener = parseFormula(parser);
		EvaluationCommand lcc = null;
		LTL formula = null;
		try {
			if(!parseListener.getErrorMarkers().isEmpty()) {
				formula = new LTL(item.getCode(), new ClassicalBParser());
			} else {
				formula = new LTL(item.getCode(), new ClassicalBParser(), parser);
			}
			lcc = formula.getCommand(stateid);
			currentTrace.getStateSpace().execute(lcc);
			injector.getInstance(StatsView.class).update(currentTrace.get());
		} catch (ProBError | LtlParseException error) {
			logger.error("Could not parse LTL formula: ", error);
			return error;
		}
		return lcc;
	}
	
	private LTLParseListener parseFormula(LtlParser parser) {
		LTLParseListener parseListener = new LTLParseListener();
		parser.removeErrorListeners();
		parser.addErrorListener(parseListener);
		parser.addWarningListener(parseListener);
		parser.parse();
		return parseListener;
	}
		
}
