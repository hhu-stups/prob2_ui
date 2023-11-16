package de.prob2.ui.codecompletion;

import javafx.beans.NamedArg;
import javafx.scene.control.SpinnerValueFactory;

/**
 * This class is an extension of {@link SpinnerValueFactory.IntegerSpinnerValueFactory} that does not crash when incrementing/decrementing with an empty text field.
 */
public class ImprovedIntegerSpinnerValueFactory extends SpinnerValueFactory.IntegerSpinnerValueFactory {

	private final int initialValue;

	public ImprovedIntegerSpinnerValueFactory(@NamedArg("min") int min, @NamedArg("max") int max) {
		this(min, max, min);
	}

	public ImprovedIntegerSpinnerValueFactory(@NamedArg("min") int min, @NamedArg("max") int max, @NamedArg("initialValue") int initialValue) {
		this(min, max, initialValue, 1);
	}

	public ImprovedIntegerSpinnerValueFactory(@NamedArg("min") int min, @NamedArg("max") int max, @NamedArg("initialValue") int initialValue, @NamedArg("amountToStepBy") int amountToStepBy) {
		super(min, max, initialValue, amountToStepBy);
		this.initialValue = initialValue >= min && initialValue <= max ? initialValue : min;
	}

	public int getInitialValue() {
		return this.initialValue;
	}

	public int getValueOrInitial() {
		Integer value = getValue();
		if (value != null) {
			return value;
		} else {
			return this.getInitialValue();
		}
	}

	@Override
	public void decrement(int steps) {
		final int min = getMin();
		final int max = getMax();
		final int newIndex = getValueOrInitial() - steps * getAmountToStepBy();
		setValue(newIndex >= min ? newIndex : (isWrapAround() ? wrapValue(newIndex, min, max) + 1 : min));
	}

	@Override
	public void increment(int steps) {
		final int min = getMin();
		final int max = getMax();
		final int newIndex = getValueOrInitial() + steps * getAmountToStepBy();
		setValue(newIndex <= max ? newIndex : (isWrapAround() ? wrapValue(newIndex, min, max) - 1 : max));
	}

	/**
	 * copied from {@code Spinner#wrapValue(int, int, int)}
	 */
	public static int wrapValue(int value, int min, int max) {
		if (max == 0) {
			throw new RuntimeException();
		}

		int r = value % max;
		if (r > min && max < min) {
			r = r + max - min;
		} else if (r < min && max > min) {
			r = r + max - min;
		}
		return r;
	}
}
