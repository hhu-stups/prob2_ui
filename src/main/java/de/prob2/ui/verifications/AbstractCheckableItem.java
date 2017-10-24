package de.prob2.ui.verifications;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.paint.Color;

public abstract class AbstractCheckableItem implements IExecutableItem {
	
	protected transient FontAwesomeIconView status;
	protected Checked checked;
	protected String name;
	protected String description;
	protected String code;
	protected boolean shouldExecute;
	protected CheckingResultItem resultItem;
	
	public AbstractCheckableItem(String name, String description, String code) {
		initializeStatus();
		this.name = name;
		this.description = description;
		this.code = code;
		this.shouldExecute = true;
		this.resultItem = null;
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
	}
	
	public FontAwesomeIconView getStatus() {
		return status;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public void setShouldExecute(boolean shouldExecute) {
		this.shouldExecute = shouldExecute;
	}
	
	@Override
	public boolean shouldExecute() {
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
		this.resultItem = resultItem;
	}
	
	public CheckingResultItem getResultItem() {
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
	
	public Checked getChecked() {
		return checked;
	}
		
	
}
