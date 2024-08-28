package de.prob2.ui.verifications.temporal;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.voparser.VOParseException;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingResult;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.vomanager.ast.IValidationExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonPropertyOrder({
	"id",
	"description",
	"stateLimit",
	"code",
	"startState",
	"startStateExpression",
	"expectedResult",
	"selected",
})
public abstract class TemporalFormulaItem extends AbstractCheckableItem {
	public enum StartState {
		ALL_INITIAL_STATES,
		CURRENT_STATE,
		FROM_EXPRESSION,
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TemporalFormulaItem.class);

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String id;
	private final String code;
	private final String description;
	private final int stateLimit;
	private final StartState startState;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String startStateExpression;
	private final boolean expectedResult;

	protected TemporalFormulaItem(String id, String code, String description, int stateLimit, StartState startState, String startStateExpression, boolean expectedResult) {
		super();

		this.id = id;
		this.code = Objects.requireNonNull(code, "code");
		this.description = Objects.requireNonNull(description, "description");
		this.stateLimit = stateLimit;
		this.startState = Objects.requireNonNull(startState, "startState");
		if (this.startState == StartState.FROM_EXPRESSION) {
			this.startStateExpression = Objects.requireNonNull(startStateExpression, "startStateExpression");
		} else if (startStateExpression != null) {
			throw new IllegalArgumentException("startStateExpression must only be set for StartState.FROM_EXPRESSION");
		} else {
			this.startStateExpression = null;
		}
		this.expectedResult = expectedResult;
	}

	@Override
	public String getId() {
		return this.id;
	}

	public String getCode() {
		return this.code;
	}

	public String getDescription() {
		return this.description;
	}

	public int getStateLimit() {
		return this.stateLimit;
	}

	public StartState getStartState() {
		return this.startState;
	}

	public String getStartStateExpression() {
		return this.startStateExpression;
	}

	public boolean getExpectedResult() {
		return this.expectedResult;
	}

	@Override
	public String getTaskDescription(final I18n i18n) {
		if (this.getDescription().isEmpty()) {
			return this.getCode();
		} else {
			return this.getCode() + " // " + getDescription();
		}
	}

	protected abstract void execute(ExecutionContext context, State startState);

	@Override
	public CompletableFuture<?> execute(CheckingExecutors executors, ExecutionContext context) {
		CompletableFuture<?> future = switch (this.getStartState()) {
			case ALL_INITIAL_STATES -> executors.cliExecutor().submit(() -> this.execute(context, null));
			case CURRENT_STATE -> {
				if (context.trace() == null) {
					this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "verifications.temporal.result.noCurrentState.message"));
					yield CompletableFuture.completedFuture(null);
				}
				State startState = context.trace().getCurrentState();
				yield executors.cliExecutor().submit(() -> this.execute(context, startState));
			}
			case FROM_EXPRESSION -> {
				IValidationExpression expr;
				try {
					expr = IValidationExpression.fromString(this.getStartStateExpression(), context.machine().getValidationTasksById());
				} catch (VOParseException exc) {
					LOGGER.error("Parse error in temporal checking start state validation expression", exc);
					this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "common.result.validationExpressionParseError", exc.getMessage()));
					yield CompletableFuture.completedFuture(null);
				}

				yield expr.check(executors, context).thenCompose(res -> {
					Trace trace = expr.getTrace();
					if (trace == null) {
						this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "common.result.validationExpressionNoTrace", this.getStartStateExpression()));
						return CompletableFuture.completedFuture(null);
					}
					State startState = trace.getCurrentState();
					return executors.cliExecutor().submit(() -> this.execute(context, startState));
				});
			}
			default -> throw new AssertionError("Unhandled start state type: " + this.getStartState());
		};

		return future.whenComplete((res, exc) -> {
			if (exc != null) {
				this.setResult(new CheckingResult(CheckingStatus.INVALID_TASK, "common.result.message", exc.toString()));
			}
		});
	}

	@Override
	public boolean settingsEqual(Object other) {
		return other instanceof TemporalFormulaItem that
			       && Objects.equals(this.getTaskType(), that.getTaskType())
			       && Objects.equals(this.getId(), that.getId())
			       && Objects.equals(this.getCode(), that.getCode())
			       && Objects.equals(this.getDescription(), that.getDescription())
			       && Objects.equals(this.getStateLimit(), that.getStateLimit())
			       && this.getStartState().equals(that.getStartState())
			       && Objects.equals(this.getStartStateExpression(), that.getStartStateExpression())
			       && Objects.equals(this.getExpectedResult(), that.getExpectedResult());
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "%s(%s,%s,%s)", this.getClass().getSimpleName(), this.getId(), this.getCode(), this.getExpectedResult());
	}
}
