package de.prob2.ui.verifications.modelchecking;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.prob.check.ModelCheckingOptions;
import de.prob.json.JsonManager;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

public class ModelCheckingItem implements IExecutableItem {
	public static final JsonDeserializer<ModelCheckingItem> JSON_DESERIALIZER = ModelCheckingItem::new;
	
	private final transient ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);

	private final String nodesLimit;

	private final String timeLimit;

	private final ModelCheckingOptions options;

	private final String goal;
	
	private BooleanProperty shouldExecute;
	
	private final transient ListProperty<ModelCheckingJobItem> items = new SimpleListProperty<>(this, "jobItems", FXCollections.observableArrayList());

	public ModelCheckingItem(String nodesLimit, String timeLimit, String goal, ModelCheckingOptions options) {
		Objects.requireNonNull(options);
		this.nodesLimit = nodesLimit;
		this.timeLimit = timeLimit;
		this.goal = goal;
		this.options = options;
		this.shouldExecute = new SimpleBooleanProperty(true);
		this.initListeners();
	}
	
	private ModelCheckingItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		final JsonObject object = json.getAsJsonObject();
		this.nodesLimit = JsonManager.checkDeserialize(context, object, "nodesLimit", String.class);
		this.timeLimit = JsonManager.checkDeserialize(context, object, "timeLimit", String.class);
		this.goal = JsonManager.checkDeserialize(context, object, "goal", String.class);
		this.options = JsonManager.checkDeserialize(context, object, "options", ModelCheckingOptions.class);
		this.shouldExecute = JsonManager.checkDeserialize(context, object, "shouldExecute", BooleanProperty.class);
		
		this.initListeners();
	}
	
	private void initListeners() {
		this.itemsProperty().addListener((o, from, to) -> {
			if (to.isEmpty()) {
				this.checked.set(Checked.NOT_CHECKED);
			} else {
				final boolean failed = to.stream()
					.map(ModelCheckingJobItem::getChecked)
					.anyMatch(Checked.FAIL::equals);
				final boolean success = !failed && to.stream()
					.map(ModelCheckingJobItem::getChecked)
					.anyMatch(Checked.SUCCESS::equals);
				
				if (success) {
					this.checked.set(Checked.SUCCESS);
				} else if (failed) {
					this.checked.set(Checked.FAIL);
				} else {
					this.checked.set(Checked.TIMEOUT);
				}
			}
		});
	}
	
	@Override
	public ReadOnlyObjectProperty<Checked> checkedProperty() {
		return this.checked;
	}
	
	@Override
	public Checked getChecked() {
		return this.checkedProperty().get();
	}

	public String getNodesLimit() {
		return nodesLimit;
	}

	public String getTimeLimit() {
		return timeLimit;
	}

	public String getGoal() {
		return goal;
	}

	public ModelCheckingOptions getOptions() {
		return this.options;
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
	}
	
	public ListProperty<ModelCheckingJobItem> itemsProperty() {
		return items;
	}
	
	public List<ModelCheckingJobItem> getItems() {
		return items.get();
	}

	public boolean settingsEqual(final ModelCheckingItem other) {
		return this.getNodesLimit().equals(other.getNodesLimit())
			&& this.getTimeLimit().equals(other.getTimeLimit())
			&& this.getGoal().equals(other.getGoal())
			&& this.getOptions().equals(other.getOptions());
	}
	
	@Override
	public String toString() {
		return String.format("%s(%s,%s,%s)", this.getClass().getSimpleName(), this.getNodesLimit(), this.getTimeLimit(), this.getGoal(), this.getOptions());
	}
}
