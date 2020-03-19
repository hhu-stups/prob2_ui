package de.prob2.ui.verifications;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.prob2.ui.json.JsonManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public abstract class AbstractCheckableItem implements IExecutableItem {
	protected Checked checked;
	protected String name;
	protected String description;
	protected String code;
	protected BooleanProperty selected;
	protected transient ObjectProperty<CheckingResultItem> resultItem;
	
	public AbstractCheckableItem(String name, String description, String code) {
		initialize();
		this.name = name;
		this.description = description;
		this.code = code;
		this.selected = new SimpleBooleanProperty(true);
	}	
	
	protected AbstractCheckableItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		final JsonObject object = json.getAsJsonObject();
		this.checked = JsonManager.checkDeserialize(context, object, "checked", Checked.class);
		this.name = JsonManager.checkDeserialize(context, object, "name", String.class);
		this.description = JsonManager.checkDeserialize(context, object, "description", String.class);
		this.code = JsonManager.checkDeserialize(context, object, "code", String.class);
		this.selected = JsonManager.checkDeserialize(context, object, "selected", BooleanProperty.class);
		initialize();
	}
	
	public void setData(String name, String description, String code) {
		initialize();
		setName(name);
		setDescription(description);
		setCode(code);
	}
		
	public void initialize() {
		this.checked = Checked.NOT_CHECKED;
		this.resultItem = new SimpleObjectProperty<>(null);
	}
	
	public String getName() {
		return name;
	}
	
	public void setSelected(boolean selected) {
		this.selected.set(selected);
	}
	
	@Override
	public boolean selected() {
		return selected.get();
	}
	
	public BooleanProperty selectedProperty() {
		return selected;
	}

	public String getDescription() {
		return description;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setResultItem(CheckingResultItem resultItem) {
		if(resultItem == null) {
			this.resultItem = new SimpleObjectProperty<>(resultItem);
			return;
		}
		this.resultItem.set(resultItem);
	}
	
	public CheckingResultItem getResultItem() {
		return resultItem.get();
	}
	
	public ObjectProperty<CheckingResultItem> resultItemProperty() {
		return resultItem;
	}
	
	public void setChecked(Checked checked) {
		this.checked = checked;
	}

	@Override
	public Checked getChecked() {
		return checked;
	}
}
