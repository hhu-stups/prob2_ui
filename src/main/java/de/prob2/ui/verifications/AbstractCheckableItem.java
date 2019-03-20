package de.prob2.ui.verifications;

import de.prob2.ui.layout.BindableGlyph;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

import org.controlsfx.glyphfont.FontAwesome;

public abstract class AbstractCheckableItem implements IExecutableItem {
	protected transient BindableGlyph status;
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
	
	public void setData(String name, String description, String code) {
		initialize();
		setName(name);
		setDescription(description);
		setCode(code);
	}
		
	public void initialize() {
		replaceMissingWithDefaults();
		this.status = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
		this.status.setTextFill(Color.BLUE);
		this.checked = Checked.NOT_CHECKED;
		this.resultItem = new SimpleObjectProperty<>(null);
	}

	public void replaceMissingWithDefaults() {
		if(selected == null) {
			this.selected = new SimpleBooleanProperty(true);
		}
	}

	public BindableGlyph getStatus() {
		return status;
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
	
	public void setCheckedSuccessful() {
		status.setIcon(FontAwesome.Glyph.CHECK);
		status.setTextFill(Color.GREEN);
	}

	public void setCheckedFailed() {
		status.setIcon(FontAwesome.Glyph.REMOVE);
		status.setTextFill(Color.RED);
	}
	
	public void setCheckInterrupted() {
		status.setIcon(FontAwesome.Glyph.EXCLAMATION_TRIANGLE);
		status.setTextFill(Color.YELLOW);
	}

	public void setParseError() {
		status.setIcon(FontAwesome.Glyph.EXCLAMATION_TRIANGLE);
		status.setTextFill(Color.ORANGE);
	}
	
	public void setChecked(Checked checked) {
		this.checked = checked;
	}

	@Override
	public Checked getChecked() {
		return checked;
	}
}
