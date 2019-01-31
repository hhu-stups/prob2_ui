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
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;
import de.prob2.ui.verifications.ltl.ILTLItemHandler;
import de.prob2.ui.verifications.ltl.LTLParseListener;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;

@FXMLInjected
@Singleton
public class LTLFormulaChecker implements ILTLItemHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLFormulaChecker.class);
				
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final ListProperty<Thread> currentJobThreads;
	
	private final LTLResultHandler resultHandler;
	
	private final Injector injector;
	
	@Inject
	private LTLFormulaChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
			final LTLResultHandler resultHandler, final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.resultHandler = resultHandler;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
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
	
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			checkMachine(machine);
			Platform.runLater(() -> injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL));
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	public Checked checkFormula(LTLFormulaItem item, Machine machine) {
		if(!item.selected()) {
			return Checked.NOT_CHECKED;
		}
		State stateid = currentTrace.getCurrentState();
		LtlParser parser = new LtlParser(item.getCode());
		parser.setPatternManager(machine.getPatternManager());
		return resultHandler.handleFormulaResult(item, getResult(parser, item), stateid);
	}
	
	public void checkFormula(LTLFormulaItem item) {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			Checked result = checkFormula(item, machine);
			item.setChecked(result);
			Platform.runLater(() -> injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL));
			if(item.getCounterExample() != null) {
				currentTrace.set(item.getCounterExample());
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
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
	
	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}
		
}
