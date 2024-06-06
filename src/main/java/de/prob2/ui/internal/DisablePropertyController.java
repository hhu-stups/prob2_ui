package de.prob2.ui.internal;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * This class provides a property that signals when probcli is busy,
 * which is used to disable UI elements that would hang when used while probcli is busy.
 */
@Singleton
public final class DisablePropertyController {
	private final BooleanProperty disable;
	private BooleanExpression disableExpression;

	@Inject
	private DisablePropertyController() {
		this.disable = new SimpleBooleanProperty(this, "disable", false);
		this.disableExpression = Bindings.createBooleanBinding(() -> false);
		this.disable.bind(disableExpression);
	}
	
	/**
	 * Add an expression to {@link #disableProperty()}.
	 * When any of the expressions added via this method are {@code true},
	 * {@link #disableProperty()} also becomes true,
	 * indicating that probcli is busy.
	 * 
	 * @param expr an expression that should cause {@link #disableProperty()} to become true
	 */
	public void addDisableExpression(final BooleanExpression expr) {
		this.disableExpression = this.disableExpression.or(expr);
		this.disable.bind(this.disableExpression);
	}

	public BooleanExpression disableProperty() {
		return this.disable;
	}
}
