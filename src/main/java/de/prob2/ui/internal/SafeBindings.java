package de.prob2.ui.internal;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class mirrors the JavaFX {@link javafx.beans.binding.Bindings Bindings} class.
 * But all operations will never throw an exception, log something to a level above trace or return null.
 */
public final class SafeBindings {

	private static final Logger LOGGER = LoggerFactory.getLogger(SafeBindings.class);

	private SafeBindings() {
		throw new AssertionError();
	}

	public static StringBinding createSafeStringBinding(Callable<String> func, Observable... dependencies) {
		Objects.requireNonNull(func, "func");
		Objects.requireNonNull(dependencies, "dependencies");
		Observable[] copiedDependencies = Arrays.copyOf(dependencies, dependencies.length);
		return new StringBinding() {
			{
				bind(copiedDependencies);
			}

			@Override
			protected String computeValue() {
				try {
					return Objects.requireNonNull(func.call(), "func.call()");
				} catch (Exception e) {
					LOGGER.trace("Exception while evaluating safe binding", e);
				}

				return "";
			}

			@Override
			public void dispose() {
				unbind(copiedDependencies);
			}

			@Override
			public ObservableList<?> getDependencies() {
				return copiedDependencies.length == 0 ?
					       FXCollections.emptyObservableList()
					       : copiedDependencies.length == 1 ?
						         FXCollections.singletonObservableList(copiedDependencies[0])
						         : FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(copiedDependencies));
			}
		};
	}

	public static <T> BooleanBinding wrappedBooleanBinding(Predicate<? super T> predicate, ObservableValue<? extends T> wrapper) {
		Objects.requireNonNull(predicate, "predicate");
		Objects.requireNonNull(wrapper, "wrapper");
		return new BooleanBinding() {
			final ChangeListener<? super T> onWrapperChange = (o, from, to) -> {
				if (from instanceof Observable observable) {
					unbind(observable);
				}
				if (to instanceof Observable observable) {
					bind(observable);
				}
			};

			{
				bind(wrapper);
				wrapper.addListener(onWrapperChange);
				onWrapperChange.changed(null, null, wrapper.getValue());
			}

			@Override
			protected boolean computeValue() {
				return predicate.test(wrapper.getValue());
			}

			@Override
			public void dispose() {
				super.unbind(wrapper);
				onWrapperChange.changed(null, wrapper.getValue(), null);
			}

			@Override
			public ObservableList<?> getDependencies() {
				ObservableList<Observable> dependencies = FXCollections.observableArrayList(wrapper);
				T value = wrapper.getValue();
				if (value instanceof Observable observable) {
					dependencies.add(observable);
				}

				return FXCollections.unmodifiableObservableList(dependencies);
			}
		};
	}
}
