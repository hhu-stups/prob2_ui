package de.prob2.ui.verifications;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.paint.Color;

public abstract class AbstractCheckableItem {
	
	protected transient FontAwesomeIconView status;
	protected Checked checked;
	protected String name;
	protected String description;
	protected String code;
	
	public AbstractCheckableItem(String name, String description, String code) {
		initializeStatus();
		this.name = name;
		this.description = description;
		this.code = code;
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
	
	public void setCheckedSuccessful() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
		icon.setFill(Color.GREEN);
		this.status = icon;
	}

	public void setCheckedFailed() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);
		icon.setFill(Color.RED);
		this.status = icon;
	}
	
	public void setChecked(Checked checked) {
		this.checked = checked;
	}
	
	public Checked getChecked() {
		return checked;
	}
		
	
}
