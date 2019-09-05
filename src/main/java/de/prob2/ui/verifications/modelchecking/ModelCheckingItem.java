package de.prob2.ui.verifications.modelchecking;

import java.util.List;
import java.util.Objects;

import de.prob.check.ModelCheckingOptions;
import de.prob.check.ModelCheckingOptions.Options;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;

import org.controlsfx.glyphfont.FontAwesome;

public class ModelCheckingItem extends AbstractModelCheckingItem implements IExecutableItem {

	private ModelCheckingOptions options;
	
	private transient BindableGlyph deadlocks;
	
	private transient BindableGlyph invariantViolations;
	
	private transient BindableGlyph assertionViolations;
	
	private transient BindableGlyph goals;
	
	private transient BindableGlyph stopWhenAllOperationsCovered;
	
	private String strategy;
	
	private BooleanProperty shouldExecute;
	
	private transient ListProperty<ModelCheckingJobItem> items;

	public ModelCheckingItem(ModelCheckingOptions options, String strategy) {
		super();
		Objects.requireNonNull(options);
		this.options = options;
		this.strategy = strategy;
		this.shouldExecute = new SimpleBooleanProperty(true);
		this.items = new SimpleListProperty<>(this, "jobItems", FXCollections.observableArrayList());
		initialize();
	}
	
	public void setOptions(ModelCheckingOptions options) {
		this.options = options;
		initializeOptionIcons(options);
	}

	public ModelCheckingOptions getOptions() {
		return this.options;
	}
	
	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}
	
	public String getStrategy() {
		return strategy;
	}
	
	public void setSelected(boolean selected) {
		this.shouldExecute.set(selected);
	}
	
	@Override
	public boolean selected() {
		return shouldExecute.get();
	}
	
	public BooleanProperty selectedProperty() {
		return shouldExecute;
	}


	/*
	* This function is needed for initializing checked for items that are loaded via JSON and might not contain these fields.
	*/
	public void initialize() {
		if(this.items == null) {
			this.items = new SimpleListProperty<>(this, "jobItems", FXCollections.observableArrayList());
		}
		initializeStatus();
		initializeOptionIcons(options);
	}

	/*
	* Required in initialize
	*/
	private void initializeStatus() {
		this.checked = Checked.NOT_CHECKED;
	}

	/*
	* Required in initialize
	*/
	private void initializeOptionIcons(ModelCheckingOptions options) {
		this.deadlocks = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		this.invariantViolations = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		this.assertionViolations = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		this.goals = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		this.stopWhenAllOperationsCovered = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		
		this.deadlocks.setTextFill(Color.BLUE);
		this.invariantViolations.setTextFill(Color.BLUE);
		this.assertionViolations.setTextFill(Color.BLUE);
		this.goals.setTextFill(Color.BLUE);
		this.stopWhenAllOperationsCovered.setTextFill(Color.BLUE);
		
		initializeOptionIcon(this.deadlocks, options, Options.FIND_DEADLOCKS);
		initializeOptionIcon(this.invariantViolations, options, Options.FIND_INVARIANT_VIOLATIONS);
		initializeOptionIcon(this.assertionViolations, options, Options.FIND_ASSERTION_VIOLATIONS);
		initializeOptionIcon(this.goals, options, Options.FIND_GOAL);
		initializeOptionIcon(this.stopWhenAllOperationsCovered, options, Options.STOP_AT_FULL_COVERAGE);
	}
	
	private void initializeOptionIcon(BindableGlyph icon, ModelCheckingOptions options, Options option) {
		if(options.getPrologOptions().contains(option)) {
			icon.setIcon(FontAwesome.Glyph.CHECK);
			icon.setTextFill(Color.GREEN);
		} else {
			icon.setIcon(FontAwesome.Glyph.REMOVE);
			icon.setTextFill(Color.RED);
		}
	}
	
	public BindableGlyph getDeadlocks() {
		return deadlocks;
	}
	
	public BindableGlyph getInvariantViolations() {
		return invariantViolations;
	}
	
	public BindableGlyph getAssertionViolations() {
		return assertionViolations;
	}
	
	public BindableGlyph getGoals() {
		return goals;
	}
	
	public BindableGlyph getStopWhenAllOperationsCovered() {
		return stopWhenAllOperationsCovered;
	}
	
	public ListProperty<ModelCheckingJobItem> itemsProperty() {
		return items;
	}
	
	public List<ModelCheckingJobItem> getItems() {
		return items.get();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(!(obj instanceof ModelCheckingItem)) {
			return false;
		}
		ModelCheckingItem other = (ModelCheckingItem) obj;
		return options.equals(other.options);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(options);
	}
	
	@Override
	public String toString() {
		return options.toString();
	}
	
}
