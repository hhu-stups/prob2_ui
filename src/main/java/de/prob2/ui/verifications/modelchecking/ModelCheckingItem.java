package de.prob2.ui.verifications.modelchecking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

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

		public ModelCheckingStep getLastStep() {
			return this.getSteps().get(this.getSteps().size() - 1);
		}

		@Override
		public CheckingStatus getStatus() {
			boolean inProgress = this.getSteps().stream()
				.map(ModelCheckingStep::getStatus)
				.anyMatch(CheckingStatus.IN_PROGRESS::equals);
			boolean failed = !inProgress && this.getSteps().stream()
				.map(ModelCheckingStep::getStatus)
				.anyMatch(CheckingStatus.FAIL::equals);
			boolean success = !failed && this.getSteps().stream()
				.map(ModelCheckingStep::getStatus)
				.anyMatch(CheckingStatus.SUCCESS::equals);

			if (inProgress) {
				return CheckingStatus.IN_PROGRESS;
			} else if (success) {
				return CheckingStatus.SUCCESS;
			} else if (failed) {
				return CheckingStatus.FAIL;
			} else {
				return CheckingStatus.TIMEOUT;
			}
		}

		@Override
		public List<Trace> getTraces() {
			return this.getSteps().stream()
				.map(ModelCheckingStep::getTrace)
				.filter(Objects::nonNull)
				.toList();
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;

	public ModelCheckingItem(final String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}
}
