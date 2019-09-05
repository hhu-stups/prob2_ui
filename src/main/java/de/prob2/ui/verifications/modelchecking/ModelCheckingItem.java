package de.prob2.ui.verifications.modelchecking;

import java.util.List;
import java.util.Objects;

import de.prob.check.ModelCheckingOptions;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

public class ModelCheckingItem extends AbstractModelCheckingItem implements IExecutableItem {

	private final ObjectProperty<ModelCheckingOptions> options;
	
	private BooleanProperty shouldExecute;
	
	private transient ListProperty<ModelCheckingJobItem> items;

	public ModelCheckingItem(ModelCheckingOptions options) {
		super();
		Objects.requireNonNull(options);
		this.options = new SimpleObjectProperty<>(this, "options", options);
		this.shouldExecute = new SimpleBooleanProperty(true);
		this.items = new SimpleListProperty<>(this, "jobItems", FXCollections.observableArrayList());
		initialize();
	}
	
	public ObjectProperty<ModelCheckingOptions> optionsProperty() {
		return this.options;
	}

	public ModelCheckingOptions getOptions() {
		return this.optionsProperty().get();
	}
	
	public void setOptions(final ModelCheckingOptions options) {
		this.optionsProperty().set(options);
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
	}

	/*
	* Required in initialize
	*/
	private void initializeStatus() {
		this.checked = Checked.NOT_CHECKED;
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
		return this.getOptions().equals(other.getOptions());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.getOptions());
	}
	
	@Override
	public String toString() {
		return String.format("%s(%s)", this.getClass().getSimpleName(), this.getOptions());
	}
	
}
