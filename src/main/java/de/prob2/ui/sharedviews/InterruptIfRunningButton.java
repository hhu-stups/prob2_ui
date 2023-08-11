package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob.statespace.StateSpace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Builder;

/**
 * <p>
 * A button for interrupting any running tasks on the shared {@link CliTaskExecutor}.
 * The button is only visible when the shared executor is busy.
 * Otherwise the interrupt button disappears and some other content is shown instead,
 * usually a different button for starting a task on the shared executor.
 * </p>
 * <p>
 * Note: This object isn't actually the button itself -
 * it's a stack pane that contains the actual button ({@link #getInterruptButton()})
 * together with the alternative content.
 * Only one of the two is ever visible at once.
 * </p>
 */
@FXMLInjected
public final class InterruptIfRunningButton extends StackPane implements Builder<InterruptIfRunningButton> {
	private final BooleanProperty running;
	private final Button interruptButton;
	
	@Inject
	private InterruptIfRunningButton(final CliTaskExecutor cliExecutor, final CurrentTrace currentTrace, final I18n i18n) {
		this.running = new SimpleBooleanProperty(this, "running", false);
		this.running.bind(cliExecutor.runningProperty());
		
		this.interruptButton = new Button();
		this.interruptButton.visibleProperty().bind(this.running);
		this.interruptButton.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		this.interruptButton.setTooltip(new Tooltip(i18n.translate("common.buttons.cancel")));
		this.interruptButton.setOnAction(e -> {
			cliExecutor.interruptAll();
			final StateSpace stateSpace = currentTrace.getStateSpace();
			if (stateSpace != null) {
				stateSpace.sendInterrupt();
			}
		});
		this.getChildren().add(this.interruptButton);
		
		// Show the regular content only if the executor is not running
		// (i. e. there is nothing to be interrupted and it's safe to submit new tasks).
		this.getChildren().addListener((ListChangeListener<Node>)change -> {
			while (change.next()) {
				for (final Node node : change.getAddedSubList()) {
					if (node != this.interruptButton) {
						node.visibleProperty().bind(this.running.not());
					}
				}
				
				for (final Node node : change.getRemoved()) {
					if (node != this.interruptButton) {
						node.visibleProperty().unbind();
					}
				}
			}
		});
	}
	
	@Override
	public InterruptIfRunningButton build() {
		return this;
	}
	
	public BooleanProperty runningProperty() {
		return this.running;
	}
	
	public Button getInterruptButton() {
		return this.interruptButton;
	}
	
	public ObjectProperty<Node> graphicProperty() {
		return this.interruptButton.graphicProperty();
	}
	
	public Node getGraphic() {
		return this.interruptButton.getGraphic();
	}
	
	public void setGraphic(final Node value) {
		this.interruptButton.setGraphic(value);
	}
}
