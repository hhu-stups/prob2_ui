package de.prob2.ui.verifications.modelchecking;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.prob.check.ModelCheckingOptions;
import de.prob.json.JsonManager;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

public class ModelCheckingItem implements IExecutableItem {
	public static final JsonDeserializer<ModelCheckingItem> JSON_DESERIALIZER = ModelCheckingItem::new;
	
	private transient Checked checked;

	private String nodesLimit;

	private final ObjectProperty<ModelCheckingOptions> options;
	
	private BooleanProperty shouldExecute;
	
	private final transient ListProperty<ModelCheckingJobItem> items = new SimpleListProperty<>(this, "jobItems", FXCollections.observableArrayList());

	public ModelCheckingItem(ModelCheckingOptions options) {
		Objects.requireNonNull(options);
		this.checked = Checked.NOT_CHECKED;
		this.nodesLimit = "-";
		this.options = new SimpleObjectProperty<>(this, "options", options);
		this.shouldExecute = new SimpleBooleanProperty(true);
	}

	public ModelCheckingItem(String nodesLimit, ModelCheckingOptions options) {
		Objects.requireNonNull(options);
		this.checked = Checked.NOT_CHECKED;
		this.nodesLimit = nodesLimit;
		this.options = new SimpleObjectProperty<>(this, "options", options);
		this.shouldExecute = new SimpleBooleanProperty(true);
	}
	
	private ModelCheckingItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		final JsonObject object = json.getAsJsonObject();
		this.checked = Checked.NOT_CHECKED;
		this.nodesLimit = JsonManager.checkDeserialize(context, object, "nodesLimit", String.class);
		this.options = JsonManager.checkDeserialize(context, object, "options", new TypeToken<ObjectProperty<ModelCheckingOptions>>() {}.getType());
		this.shouldExecute = JsonManager.checkDeserialize(context, object, "shouldExecute", BooleanProperty.class);
	}
	
	@Override
	public Checked getChecked() {
		return checked;
	}
	
	public void setChecked(final Checked checked) {
		this.checked = checked;
	}

	public String getNodesLimit() {
		return nodesLimit;
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
	
	public void reset() {
		this.itemsProperty().clear();
		this.setChecked(Checked.NOT_CHECKED);
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
		return this.getNodesLimit().equals(other.nodesLimit) && this.getOptions().equals(other.getOptions());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.nodesLimit, this.getOptions());
	}
	
	@Override
	public String toString() {
		if("-".equals(nodesLimit)) {
			return String.format("%s(%s)", this.getClass().getSimpleName(), this.getOptions());
		}
		return String.format("%s(%s,%s)", this.getClass().getSimpleName(), this.getNodesLimit(), this.getOptions());
	}
}
