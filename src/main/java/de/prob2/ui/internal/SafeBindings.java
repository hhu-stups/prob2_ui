package de.prob2.ui.internal;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;

import javafx.beans.Observable;
import javafx.beans.binding.StringBinding;
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
				super.unbind(copiedDependencies);
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
}
