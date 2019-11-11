package de.prob2.ui.animation.symbolic;


import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicExecutor;
import de.prob2.ui.symbolic.SymbolicFormulaItem;


import javax.inject.Inject;
import java.util.List;

@Singleton
public class SymbolicAnimationChecker extends SymbolicExecutor {

	@Inject
	public SymbolicAnimationChecker(final CurrentTrace currentTrace, final CurrentProject currentProject,
							final SymbolicAnimationResultHandler resultHandler, final Injector injector) {
		super(currentTrace, currentProject, resultHandler, injector);
	}

	public void updateMachine(Machine machine) {
		injector.getInstance(SymbolicAnimationView.class).refresh();
	}

	@Override
	protected void updateTrace(SymbolicFormulaItem item) {
		List<Trace> examples = ((SymbolicAnimationFormulaItem) item).getExamples();
		if(!examples.isEmpty()) {
			currentTrace.set(examples.get(0));
		}
	}

	
}
