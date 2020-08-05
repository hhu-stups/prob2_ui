package de.prob2.ui.verifications;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.prob.json.JsonManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public abstract class AbstractCheckableItem implements IExecutableItem {
	private String name;
	private String description;
	private String code;
	private BooleanProperty selected;
	private final transient ObjectProperty<CheckingResultItem> resultItem = new SimpleObjectProperty<>(this, "resultItem", null);
	private final transient ObjectProperty<Checked> checked = new SimpleObjectProperty<>(this, "checked", Checked.NOT_CHECKED);
	
	public AbstractCheckableItem(String name, String description, String code) {
		this.name = name;
		this.description = description;
		this.code = code;
		this.selected = new SimpleBooleanProperty(true);
		
		this.initListeners();
	}
	
	protected AbstractCheckableItem(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		final JsonObject object = json.getAsJsonObject();
		this.name = JsonManager.checkDeserialize(context, object, "name", String.class);
		this.description = JsonManager.checkDeserialize(context, object, "description", String.class);
		this.code = JsonManager.checkDeserialize(context, object, "code", String.class);
		this.selected = JsonManager.checkDeserialize(context, object, "selected", BooleanProperty.class);
		
		this.initListeners();
	}
	
	private void initListeners() {
		this.resultItemProperty().addListener((o, from, to) -> this.checked.set(to == null ? Checked.NOT_CHECKED : to.getChecked()));
	}
	
	public void setData(String name, String description, String code) {
		reset();
		setName(name);
		setDescription(description);
		setCode(code);
	}
		
	public void reset() {
		this.setResultItem(null);
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
		this.resultItem.set(resultItem);
	}
	
	public CheckingResultItem getResultItem() {
		return resultItem.get();
	}
	
	public ObjectProperty<CheckingResultItem> resultItemProperty() {
		return resultItem;
	}
	
	@Override
	public ReadOnlyObjectProperty<Checked> checkedProperty() {
		return this.checked;
	}
	
	@Override
	public Checked getChecked() {
		return this.checkedProperty().get();
	}
}
