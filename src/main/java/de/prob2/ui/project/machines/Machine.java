package de.prob2.ui.project.machines;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.verifications.ltl.LTLFormulaItem;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;

//TODO: Refactor this and LTLFormulaItem
public class Machine {
	private String name;
	private String description;
	private String location;
	private transient FontAwesomeIconView status;
	private final ListProperty<LTLFormulaItem> ltlFormulas;

	public Machine(String name, String description, Path location) {
		this.name = name;
		this.description = description;
		this.location = location.toString();
		initializeStatus();
		this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
	}

	public String getName() {
		return name;
	}

	public String getFileName() {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] splittedFileName = location.split(pattern);
		return splittedFileName[splittedFileName.length - 1];
	}

	public String getDescription() {
		return description;
	}
	
	public FontAwesomeIconView getStatus() {
		return status;
	}
	
	public void initializeStatus() {
		this.status = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		status.setFill(Color.BLUE);
		for (LTLFormulaItem item: ltlFormulas) {
			item.initializeStatus();
		}
	}
		
	public ListProperty<LTLFormulaItem> ltlFormulasProperty() {
		return ltlFormulas;
	}
	
	public List<LTLFormulaItem> getFormulas() {
		return ltlFormulasProperty().get();
	}
	
	public void addLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.add(formula);
	}
	
	public void removeLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.remove(formula);
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
	
	public Path getPath() {
		return Paths.get(location);
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Machine)) {
			return false;
		}
		Machine otherMachine = (Machine) other;
		return otherMachine.location.equals(this.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(location);
	}
}
