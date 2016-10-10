package de.prob2.ui.prob2fx;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.IAnimator;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A singleton writable property that represents the current {@link Trace}. It also provides convenience properties and methods for easy interaction with JavaFX components using property binding.
 */
@Singleton
public final class CurrentTrace extends SimpleObjectProperty<Trace> {
	private final AnimationSelector animationSelector;
	
	private final BooleanProperty exists;
	private final BooleanProperty animatorBusy;
	
	private final CurrentState currentState;
	private final CurrentStateSpace stateSpace;
	private final CurrentModel model;
	
	private final ListProperty<Transition> transitionListWritable;
	private final ListProperty<Transition> transitionList;
	
	private final BooleanProperty canGoBack;
	private final BooleanProperty canGoForward;
	
	private final ObjectProperty<Trace> back;
	private final ObjectProperty<Trace> forward;
	
	@Inject
	private CurrentTrace(
		final AnimationSelector animationSelector,
		final CurrentState currentState,
		final CurrentStateSpace currentStateSpace,
		final CurrentModel currentModel
	) {
		super(null);
		this.animationSelector = animationSelector;
		this.animationSelector.registerAnimationChangeListener(new IAnimationChangeListener() {
			@Override
			public void traceChange(final Trace currentTrace, final boolean currentAnimationChanged) {
				CurrentTrace.this.set(currentTrace);
			}
			
			@Override
			public void animatorStatus(final boolean busy) {
				CurrentTrace.this.animatorBusy.set(busy);
			}
		});
		
		this.exists = new SimpleBooleanProperty(false);
		this.exists.bind(Bindings.isNotNull(this));
		
		this.animatorBusy = new SimpleBooleanProperty(false);
		this.animatorBusy.addListener((observable, from, to) -> {
			if (to) {
				this.get().getStateSpace().startTransaction();
			} else {
				this.get().getStateSpace().endTransaction();
			}
		});
		
		this.currentState = currentState;
		this.stateSpace = currentStateSpace;
		this.model = currentModel;
		
		this.transitionListWritable = new SimpleListProperty<>(FXCollections.observableArrayList());
		this.transitionList = new SimpleListProperty<>(FXCollections.unmodifiableObservableList(this.transitionListWritable));
		
		this.canGoBack = new SimpleBooleanProperty(false);
		this.canGoForward = new SimpleBooleanProperty(false);
		
		this.back = new SimpleObjectProperty<>(null);
		this.forward = new SimpleObjectProperty<>(null);
		
		this.addListener((observable, from, to) -> {
			if (to == null) {
				this.currentState.set(null);
				this.stateSpace.set(null);
				this.model.set(null);
				
				this.transitionListWritable.clear();
				
				this.canGoBack.set(false);
				this.canGoForward.set(false);
				
				this.back.set(null);
				this.forward.set(null);
			} else {
				this.animationSelector.traceChange(to);
				
				this.currentState.set(to.getCurrentState());
				this.stateSpace.set(to.getStateSpace());
				this.model.set(to.getModel());
				
				this.transitionListWritable.setAll(to.getTransitionList());
				
				this.canGoBack.set(to.canGoBack());
				this.canGoForward.set(to.canGoForward());
				
				this.back.set(to.back());
				this.forward.set(to.forward());
			}
		});
	}
	
	/**
	 * A read-only boolean property indicating whether a current trace exists (i. e. is not null).
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
	 * A writable property indicating whether the animator is currently busy. It holds the last value reported by {@link IAnimationChangeListener#animatorStatus(boolean)}. When written to, {@link IAnimator#startTransaction()} or {@link IAnimator#endTransaction()} is called on the current {@link StateSpace}.
	 * 
	 * @return a property indicating whether the animator is currently busy
	 */
	public BooleanProperty animatorBusyProperty() {
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
	 * Set the animator's busy status.
	 * 
	 * @param busy the new busy status
	 */
	public void setAnimatorBusy(final boolean busy) {
		this.animatorBusyProperty().set(busy);
	}
	
	/**
	 * A {@link CurrentState} holding the current {@link Trace}'s {@link State}.
	 *
	 * @return a {@link CurrentState} holding the current {@link Trace}'s {@link State}
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
	 * A {@link CurrentStateSpace} holding the current {@link Trace}'s {@link StateSpace}.
	 * 
	 * @return a {@link CurrentStateSpace} holding the current {@link Trace}'s {@link StateSpace}
	 */
	public CurrentStateSpace stateSpaceProperty() {
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
	 * A {@link CurrentModel} holding the current {@link Trace}'s {@link AbstractModel}.
	 *
	 * @return a {@link CurrentModel} holding the current {@link Trace}'s {@link AbstractModel}
	 */
	public CurrentModel modelProperty() {
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
	 * A read-only list property holding a read-only {@link ObservableList} version of the current {@link Trace}'s transition list.
	 * 
	 * @return a read-only list property holding a read-only observable list version of the current {@link Trace}'s transition list
	 */
	public ReadOnlyListProperty<Transition> transitionListProperty() {
		return this.transitionList;
	}
	
	/**
	 * Get the current {@link Trace}'s transition list as a read-only {@link ObservableList}.
	 * 
	 * @return the current {@link Trace}'s transition list as a read-only {@link ObservableList}
	 */
	public ObservableList<Transition> getTransitionList() {
		return this.transitionListProperty().get();
	}
	
	/**
	 * A read-only property indicating whether it is possible to go backward from the current trace.
	 * 
	 * @return a read-only property indicating whether it is possible to go backward from the current trace
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
	 * A read-only property indicating whether it is possible to go forward from the current trace.
	 *
	 * @return a read-only property indicating whether it is possible to go forward from the current trace
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
	 * A read-only property holding the {@link Trace} that is one transition backward of the current one.
	 * 
	 * @return a read-only property holding the {@link Trace} that is one transition backward of the current one
	 */
	public ReadOnlyObjectProperty<Trace> backProperty() {
		return this.back;
	}
	
	/**
	 * Get the {@link Trace} that is one transition backward of the current one.
	 * 
	 * @return the {@link Trace} that is one transition backward of the current one
	 */
	public Trace back() {
		return this.backProperty().get();
	}
	
	/**
	 * A read-only property holding the {@link Trace} that is one transition forward of the current one.
	 *
	 * @return a read-only property holding the {@link Trace} that is one transition forward of the current one
	 */
	public ReadOnlyObjectProperty<Trace> forwardProperty() {
		return this.forward;
	}
	
	/**
	 * Get the {@link Trace} that is one transition forward of the current one.
	 *
	 * @return the {@link Trace} that is one transition forward of the current one
	 */
	public Trace forward() {
		return this.forwardProperty().get();
	}
}
