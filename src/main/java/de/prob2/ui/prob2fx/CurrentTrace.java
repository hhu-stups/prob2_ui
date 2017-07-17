package de.prob2.ui.prob2fx;

import java.io.IOException;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetCurrentPreferencesCommand;
import de.prob.exception.CliError;
import de.prob.exception.ProBError;
import de.prob.model.representation.AbstractModel;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import de.prob2.ui.project.machines.Machine;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * A singleton writable property that represents the current {@link Trace}. It
 * also provides convenience properties and methods for easy interaction with
 * JavaFX components using property binding.
 */
@Singleton
public final class CurrentTrace extends SimpleObjectProperty<Trace> {
	private final class ROBoolProp extends ReadOnlyBooleanPropertyBase {
		private final String name;
		private final BooleanSupplier getter;
		
		private ROBoolProp(final String name, final BooleanSupplier getter) {
			super();
			
			this.name = name;
			this.getter = getter;
			
			CurrentTrace.this.addListener(o -> this.fireValueChangedEvent());
		}
		
		@Override
		public boolean get() {
			return this.getter.getAsBoolean();
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
		private final Supplier<T> getter;
		
		private ROObjProp(final String name, final Supplier<T> getter) {
			super();
			
			this.name = name;
			this.getter = getter;
			
			CurrentTrace.this.addListener(o -> this.fireValueChangedEvent());
		}
		
		@Override
		public T get() {
			return this.getter.get();
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
	
	private final Injector injector;
	private final AnimationSelector animationSelector;
	private final Api api;
	
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
	private CurrentTrace(
		final Injector injector,
		final AnimationSelector animationSelector,
		final Api api
	) {
		super(null);
		this.injector = injector;
		this.animationSelector = animationSelector;
		this.animationSelector.registerAnimationChangeListener(new IAnimationChangeListener() {
			@Override
			public void traceChange(final Trace currentTrace, final boolean currentAnimationChanged) {
				Platform.runLater(() -> CurrentTrace.this.set(currentTrace));
			}

			@Override
			public void animatorStatus(final boolean busy) {
				Platform.runLater(() -> CurrentTrace.this.animatorBusy.set(busy));
			}
		});
		this.api = api;
		
		this.addListener((observable, from, to) -> {
			if (to != null) {
				this.animationSelector.traceChange(to);
			}
		});
		
		this.exists = new ROBoolProp("exists", () -> this.get() != null);

		this.animatorBusy = new SimpleBooleanProperty(this, "animatorBusy", false);

		this.currentState = new CurrentState(this);
		this.stateSpace = new ROObjProp<>("stateSpace", () -> this.exists() ? this.get().getStateSpace() : null);
		this.model = new ROObjProp<>("model", () -> this.exists() ? this.get().getModel() : null);

		this.canGoBack = new ROBoolProp("canGoBack", () -> this.exists() && this.get().canGoBack());
		this.canGoForward = new ROBoolProp("canGoForward", () -> this.exists() && this.get().canGoForward());

		this.back = new ROObjProp<>("back", () -> this.exists() ? this.get().back() : null);
		this.forward = new ROObjProp<>("forward", () -> this.exists() ? this.get().forward() : null);
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
	 * A writable property indicating whether the animator is currently busy. It
	 * holds the last value reported by
	 * {@link IAnimationChangeListener#animatorStatus(boolean)}.
	 * 
	 * @return a property indicating whether the animator is currently busy
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
	
	/**
	 * Reload the given trace's model and make the reloaded trace the current one. If the reload fails, the current trace is not changed.
	 * 
	 * @param trace the trace to reload
	 * @param preferences preferences for the reloaded model
	 * @throws CliError when thrown by the loader method
	 * @throws IllegalStateException if there is no current trace
	 * @throws IOException when thrown by the loader method
	 * @throws ModelTranslationError when thrown by the loader method
	 * @throws ProBError when thrown by the loader method
	 */
	public void reload(final Trace trace, final Map<String, String> preferences) throws IOException, ModelTranslationError {
		if (!this.exists()) {
			return;
		}
		
		final CurrentProject currentProject = this.injector.getInstance(CurrentProject.class);
		final Machine currentMachine = currentProject.getCurrentRunconfiguration().getMachine();
		assert currentMachine != null;
		final String filename = trace.getModel().getModelFile().getAbsolutePath();
		final Trace newTrace = new Trace(currentMachine.getType().getLoader().load(api, filename, preferences));
		this.animationSelector.removeTrace(trace);
		this.animationSelector.addNewAnimation(newTrace);
	}
	
	/**
	 * Reload the given trace's model and make the reloaded trace the current one. Preferences are maintained from the old model. If the reload fails, the current trace is not changed.
	 *
	 * @param trace the trace to reload
	 * @throws CliError when thrown by the loader method
	 * @throws IllegalStateException if there is no current trace
	 * @throws IOException when thrown by the loader method
	 * @throws ModelTranslationError when thrown by the loader method
	 * @throws ProBError when thrown by the loader method
	 */
	public void reload(final Trace trace) throws IOException, ModelTranslationError {
		if (!this.exists()) {
			return;
		}
		
		final GetCurrentPreferencesCommand cmd = new GetCurrentPreferencesCommand();
		this.getStateSpace().execute(cmd);
		this.reload(trace, cmd.getPreferences());
	}
}
