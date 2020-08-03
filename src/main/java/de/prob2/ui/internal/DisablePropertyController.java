package de.prob2.ui.internal;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.symbolic.SymbolicAnimationChecker;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerator;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.symbolicchecking.SymbolicFormulaChecker;

import javafx.beans.binding.BooleanExpression;

/**
 * This class provides a property that signals when probcli is busy,
 * which is used to disable UI elements that would hang when used while probcli is busy.
 */
@Singleton
public class DisablePropertyController {

	private final BooleanExpression disableProperty;

	@Inject
	public DisablePropertyController(final Injector injector) {
		this.disableProperty = injector.getInstance(TraceChecker.class).currentJobThreadsProperty().emptyProperty().not()
				.or(injector.getInstance(SymbolicAnimationChecker.class).currentJobThreadsProperty().emptyProperty().not())
				.or(injector.getInstance(TestCaseGenerator.class).currentJobThreadsProperty().emptyProperty().not())
				.or(injector.getInstance(SymbolicFormulaChecker.class).currentJobThreadsProperty().emptyProperty().not())
				.or(injector.getInstance(OperationsView.class).randomExecutionThreadProperty().isNotNull())
				.or(injector.getInstance(OperationsView.class).runningProperty())
				.or(injector.getInstance(CurrentTrace.class).animatorBusyProperty());
	}

	public BooleanExpression disableProperty() {
		return this.disableProperty;
	}
}
