package de.prob2.ui.verifications.modelchecking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.prob.statespace.Trace;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ICheckingResult;
import de.prob2.ui.verifications.ICliTask;

public abstract class ModelCheckingItem extends AbstractCheckableItem implements ICliTask {
	public static final class Result implements ICheckingResult {
		private final List<ModelCheckingStep> steps;

		public Result(List<ModelCheckingStep> steps) {
			if (steps.isEmpty()) {
				throw new IllegalArgumentException("ModelCheckingItem result must contain at least one step");
			}
			this.steps = new ArrayList<>(steps);
		}

		public List<ModelCheckingStep> getSteps() {
			return Collections.unmodifiableList(this.steps);
		}

		/**
		 * Get the last {@link ModelCheckingStep} that was executed.
		 * Note that the status of the last step is not always the same as the status of the overall result - see {@link #getDecidingStep()}.
		 * 
		 * @return the last executed step
		 */
		public ModelCheckingStep getLastStep() {
			return this.getSteps().get(this.getSteps().size() - 1);
		}
		
		/**
		 * Get the {@link ModelCheckingStep} that determines the overall status of this result.
		 * This is often the same as {@link #getLastStep()}, but not always.
		 * In particular, if any of the steps failed, the deciding step is the first failed step,
		 * even if a later step was successful.
		 * 
		 * @return the step that determines this result's status
		 */
		public ModelCheckingStep getDecidingStep() {
			ModelCheckingStep lastStep = this.getLastStep();
			if (lastStep.getStatus() == CheckingStatus.IN_PROGRESS) {
				return lastStep;
			}

			return this.getSteps().stream()
				.filter(step -> step.getStatus() == CheckingStatus.FAIL || step.getStatus() == CheckingStatus.INVALID_TASK)
				.findFirst()
				.orElse(lastStep);
		}

		@Override
		public CheckingStatus getStatus() {
			return this.getDecidingStep().getStatus();
		}

		@Override
		public List<Trace> getTraces() {
			return this.getSteps().stream()
				.map(ModelCheckingStep::getTrace)
				.filter(Objects::nonNull)
				.toList();
		}
	}

	public ModelCheckingItem(final String id) {
		super(id);
	}
}
