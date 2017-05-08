package de.prob2.ui.verifications.ltl;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.paint.Color;

public class LTLCheckableItem {
	protected transient FontAwesomeIconView status;
	protected String name;
	protected String description;
	
	public LTLCheckableItem(String name, String description) {
		initializeStatus();
		this.name = name;
		this.description = description;
	}
	
	public void initializeStatus() {
		this.status = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.status.setFill(Color.BLUE);
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
	
	@Override
	public String toString() {
		return "Name: " + name + ", Description: " + description;
	}
	
	
}
