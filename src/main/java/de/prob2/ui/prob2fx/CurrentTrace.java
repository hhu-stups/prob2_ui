package de.prob2.ui.prob2fx;

import java.util.function.Function;
import java.util.function.Predicate;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton read-only property that represents the current {@link Trace}. It
 * also provides convenience properties and methods for easy interaction with
 * JavaFX components using property binding.
 */
@Singleton
public final class CurrentTrace extends ReadOnlyObjectPropertyBase<Trace> {
	private final class AnimationChangeListener implements IAnimationChangeListener {
		private AnimationChangeListener() {}
		
		@Override
		public void traceChange(final Trace currentTrace, final boolean currentAnimationChanged) {
			try {
				if (currentTrace != null && !currentTrace.getCurrentState().isExplored()) {
					exploreState(currentTrace.getCurrentState());
				}
				// Has to be a lambda. For some reason, using a method reference here causes an IllegalAccessError at runtime.
				// noinspection Convert2MethodRef
				Platform.runLater(() -> CurrentTrace.this.fireValueChangedEvent());
			} catch (RuntimeException e) {
				LOGGER.error("Exception during trace change");
				Platform.runLater(() -> stageManager.makeExceptionAlert(e, "prob2fx.currentTrace.alerts.exceptionDuringTraceChange.content").show());
			}
		}

		private void exploreState(State currentState) {
			try {
				currentState.explore();
			} catch (RuntimeException e) {
				LOGGER.error("Exception while exploring new state");
				//Casting currentState.getId() is necessary because it returns a String so that the wrong makeExceptionAlert function is invoked
				Platform.runLater(() -> stageManager.makeExceptionAlert(e, "prob2fx.currentTrace.alerts.exceptionWhileExploringNewState.content", (Object) currentState.getId()).show());
			}
		}
		
		@Override
		public void animatorStatus(final boolean busy) {
			Platform.runLater(() -> CurrentTrace.this.animatorBusy.set(busy));
		}
	}
	
	private final class ROBoolProp extends ReadOnlyBooleanPropertyBase {
		private final String name;
		private final Predicate<Trace> getter;
		private final boolean noTraceDefault;
		
		private ROBoolProp(final String name, final Predicate<Trace> getter, final boolean noTraceDefault) {
			super();
			
			this.name = name;
			this.getter = getter;
			this.noTraceDefault = noTraceDefault;
			
			CurrentTrace.this.addListener(o -> this.fireValueChangedEvent());
		}
		
		@Override
		public boolean get() {
			final Trace trace = CurrentTrace.this.get();
			return trace == null ? this.noTraceDefault : this.getter.test(trace);
		}
		
		@Override
		public Object getBean() {
			return CurrentTrace.this;
		}
		
		@Override
		public String getName() {
			return this.name;
		}
	}
	
	private final class ROObjProp<T> extends ReadOnlyObjectPropertyBase<T> {
		private final String name;
		private final Function<Trace, T> getter;
		private final T noTraceDefault;
		
		private ROObjProp(final String name, final Function<Trace, T> getter, final T noTraceDefault) {
			super();
			
			this.name = name;
			this.getter = getter;
			this.noTraceDefault = noTraceDefault;
			
			CurrentTrace.this.addListener(o -> this.fireValueChangedEvent());
		}
		
		@Override
		public T get() {
			final Trace trace = CurrentTrace.this.get();
			return trace == null ? this.noTraceDefault : this.getter.apply(trace);
		}
		
		@Override
		public Object getBean() {
			return CurrentTrace.this;
		}
		
		@Override
		public String getName() {
			return this.name;
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CurrentTrace.class);
	
	private final AnimationSelector animationSelector;
	private final StageManager stageManager;
	
	private final ReadOnlyBooleanProperty exists;
	private final BooleanProperty animatorBusy;

	private final CurrentState currentState;
	private final ROObjProp<StateSpace> stateSpace;
	private final ROObjProp<AbstractModel> model;

	private final ReadOnlyBooleanProperty canGoBack;
	private final ReadOnlyBooleanProperty canGoForward;

	private final ReadOnlyObjectProperty<Trace> back;
	private final ReadOnlyObjectProperty<Trace> forward;

	@Inject
	private CurrentTrace(final AnimationSelector animationSelector, final StageManager stageManager) {
		super();
		this.animationSelector = animationSelector;
		this.stageManager = stageManager;
		this.animationSelector.registerAnimationChangeListener(new AnimationChangeListener());
		
		this.exists = new ROBoolProp("exists", trace -> true, false);

		this.animatorBusy = new SimpleBooleanProperty(this, "animatorBusy", false);

		this.currentState = new CurrentState(this);
		this.stateSpace = new ROObjProp<>("stateSpace", Trace::getStateSpace, null);
		this.model = new ROObjProp<>("model", Trace::getModel, null);

		this.canGoBack = new ROBoolProp("canGoBack", Trace::canGoBack, false);
		this.canGoForward = new ROBoolProp("canGoForward", Trace::canGoForward, false);

		this.back = new ROObjProp<>("back", Trace::back, null);
		this.forward = new ROObjProp<>("forward", Trace::forward, null);
	}

	@Override
	public Object getBean() {
		return null;
	}
	
	@Override
	public String getName() {
		return "";
	}
	
	@Override
	public Trace get() {
		return this.animationSelector.getCurrentTrace();
	}
	
	/**
	 * Set the given {@link Trace} as the new current trace.
	 * 
	 * @param trace the new current trace
	 */
	public void set(final Trace trace) {
		final Trace oldTrace = this.get();
		if (trace != null) {
			this.animationSelector.addNewAnimation(trace);
		}
		if (oldTrace != null) {
			this.animationSelector.removeTrace(oldTrace);
			if (trace == null || !trace.getStateSpace().equals(oldTrace.getStateSpace())) {
				oldTrace.getStateSpace().kill();
			}
		}
	}
	
	/**
	 * A read-only boolean property indicating whether a current trace exists
	 * (i. e. is not null).
	 * 
	 * @return a boolean property indicating whether a current trace exists
	 */
	public ReadOnlyBooleanProperty existsProperty() {
		return this.exists;
	}

	/**
	 * Return whether a current trace exists (i. e. is not null).
	 * 
	 * @return whether a current trace exists
	 */
	public boolean exists() {
		return this.existsProperty().get();
	}

	/**
	 * A read-only property indicating whether the animator is currently busy. It
	 * holds the last value reported by
	 * {@link IAnimationChangeListener#animatorStatus(boolean)}.
	 * 
	 * @return a read-only property indicating whether the animator is currently busy
	 */
	public ReadOnlyBooleanProperty animatorBusyProperty() {
		return this.animatorBusy;
	}

	/**
	 * Return whether the animator is currently busy.
	 * 
	 * @return whether the animator is currently busy
	 */
	public boolean isAnimatorBusy() {
		return this.animatorBusyProperty().get();
	}

	/**
	 * A {@link CurrentState} holding the current {@link Trace}'s {@link State}.
	 *
	 * @return a {@link CurrentState} holding the current {@link Trace}'s
	 *         {@link State}
	 */
	public CurrentState currentStateProperty() {
		return this.currentState;
	}

	/**
	 * Get the current {@link Trace}'s {@link State}.
	 *
	 * @return the current {@link Trace}'s {@link State}
	 */
	public State getCurrentState() {
		return this.currentStateProperty().get();
	}

	/**
	 * A read-only property holding the current {@link Trace}'s
	 * {@link StateSpace}.
	 * 
	 * @return a read-only property holding the current {@link Trace}'s {@link StateSpace}
	 */
	public ReadOnlyObjectProperty<StateSpace> stateSpaceProperty() {
		return this.stateSpace;
	}

	/**
	 * Get the current {@link Trace}'s {@link StateSpace}.
	 * 
	 * @return the current {@link Trace}'s {@link StateSpace}
	 */
	public StateSpace getStateSpace() {
		return this.stateSpaceProperty().get();
	}
	
	/**
	 * A read-only property holding the current {@link Trace}'s
	 * {@link AbstractModel}.
	 *
	 * @return a read-only property holding the current {@link Trace}'s {@link AbstractModel}
	 */
	public ReadOnlyObjectProperty<AbstractModel> modelProperty() {
		return this.model;
	}

	/**
	 * Get the current {@link Trace}'s {@link AbstractModel}.
	 *
	 * @return the current {@link Trace}'s {@link AbstractModel}
	 */
	public AbstractModel getModel() {
		return this.modelProperty().get();
	}

	/**
	 * A read-only property indicating whether it is possible to go backward
	 * from the current trace.
	 * 
	 * @return a read-only property indicating whether it is possible to go
	 *         backward from the current trace
	 */
	public ReadOnlyBooleanProperty canGoBackProperty() {
		return this.canGoBack;
	}

	/**
	 * Return whether it is possible to go backward from the current trace.
	 * 
	 * @return whether it is possible to go backward from the current trace
	 */
	public boolean canGoBack() {
		return this.canGoBackProperty().get();
	}

	/**
	 * A read-only property indicating whether it is possible to go forward from
	 * the current trace.
	 *
	 * @return a read-only property indicating whether it is possible to go
	 *         forward from the current trace
	 */
	public ReadOnlyBooleanProperty canGoForwardProperty() {
		return this.canGoForward;
	}

	/**
	 * Return whether it is possible to go forward from the current trace.
	 *
	 * @return whether it is possible to go forward from the current trace
	 */
	public boolean canGoForward() {
		return this.canGoForwardProperty().get();
	}

	/**
	 * A read-only property holding the {@link Trace} that is one transition
	 * backward of the current one.
	 * 
	 * @return a read-only property holding the {@link Trace} that is one
	 *         transition backward of the current one
	 */
	public ReadOnlyObjectProperty<Trace> backProperty() {
		return this.back;
	}

	/**
	 * Get the {@link Trace} that is one transition backward of the current one.
	 * 
	 * @return the {@link Trace} that is one transition backward of the current
	 *         one
	 */
	public Trace back() {
		return this.backProperty().get();
	}

	/**
	 * A read-only property holding the {@link Trace} that is one transition
	 * forward of the current one.
	 *
	 * @return a read-only property holding the {@link Trace} that is one
	 *         transition forward of the current one
	 */
	public ReadOnlyObjectProperty<Trace> forwardProperty() {
		return this.forward;
	}

	/**
	 * Get the {@link Trace} that is one transition forward of the current one.
	 *
	 * @return the {@link Trace} that is one transition forward of the current
	 *         one
	 */
	public Trace forward() {
		return this.forwardProperty().get();
	}
}
