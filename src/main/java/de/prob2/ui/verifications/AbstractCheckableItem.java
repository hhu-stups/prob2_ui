package de.prob2.ui.verifications;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public abstract class AbstractCheckableItem implements IExecutableItem {
	
	protected transient FontAwesomeIconView status;
	protected Checked checked;
	protected String name;
	protected String description;
	protected String code;
	protected BooleanProperty shouldExecute;
	protected transient ObjectProperty<CheckingResultItem> resultItem;
	
	public AbstractCheckableItem(String name, String description, String code) {
		initializeStatus();
		this.name = name;
		this.description = description;
		this.code = code;
		this.shouldExecute = new SimpleBooleanProperty(true);
		this.resultItem = new SimpleObjectProperty<>(null);
	}	
	
	public void setData(String name, String description, String code) {
		initializeStatus();
		setName(name);
		setDescription(description);
		setCode(code);
	}
		
	public void initializeStatus() {
		this.status = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.status.setFill(Color.BLUE);
		this.checked = Checked.NOT_CHECKED;
		this.resultItem = new SimpleObjectProperty<>(null);
	}
	
	public FontAwesomeIconView getStatus() {
		return status;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public void setShouldExecute(boolean shouldExecute) {
		this.shouldExecute.set(shouldExecute);
	}
	
	@Override
	public boolean shouldExecute() {
		return shouldExecute.get();
	}
	
	public BooleanProperty shouldExecuteProperty() {
		return shouldExecute;
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
		status.setIcon(FontAwesomeIcon.CHECK);
		status.setFill(Color.GREEN);
	}

	public void setCheckedFailed() {
		status.setIcon(FontAwesomeIcon.REMOVE);
		status.setFill(Color.RED);
	}
	
	public void setCheckInterrupted() {
		status.setIcon(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
		status.setFill(Color.YELLOW);
	}
	
	public void setChecked(Checked checked) {
		this.checked = checked;
	}
	
	@Override
	public Checked getChecked() {
		return checked;
	}
}
