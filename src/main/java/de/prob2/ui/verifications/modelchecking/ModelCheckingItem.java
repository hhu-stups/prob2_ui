package de.prob2.ui.verifications.modelchecking;

import java.util.Objects;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.check.ModelCheckingOptions;

import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.paint.Color;

public class ModelCheckingItem implements IExecutableItem {

	private ModelCheckingOptions options;
	private transient ModelCheckStats stats;
	
	private transient FontAwesomeIconView status;
	private Checked checked;
	
	private String strategy;
	
	private String description;
	
	private BooleanProperty shouldExecute;

	public ModelCheckingItem(ModelCheckingOptions options, ModelCheckStats stats, String strategy,
							 String description) {
		initializeStatus();
		Objects.requireNonNull(options);
		Objects.requireNonNull(stats);
		
		this.options = options;
		this.stats = stats;
		this.strategy = strategy;
		this.description = description;
		this.shouldExecute = new SimpleBooleanProperty(true);
	}
	
	public void setOptions(ModelCheckingOptions options) {
		this.options = options;
	}

	public ModelCheckingOptions getOptions() {
		return this.options;
	}
	
	public void setStats(ModelCheckStats stats) {
		this.stats = stats;
	}

	public ModelCheckStats getStats() {
		return this.stats;
	}
	
	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}
	
	public String getStrategy() {
		return strategy;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	@Override
	public void setShouldExecute(boolean shouldExecute) {
		this.shouldExecute.set(shouldExecute);
	}
	
	@Override
	public boolean shouldExecute() {
		return shouldExecute.get();
	}
	
	@Override
	public BooleanProperty shouldExecuteProperty() {
		return shouldExecute;
	}
	
	public void initializeStatus() {
		this.status = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
		this.status.setFill(Color.BLUE);
		this.checked = Checked.NOT_CHECKED;
		this.stats = null;
	}
	
	public FontAwesomeIconView getStatus() {
		return status;
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
	
	public void setTimeout() {
		FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
		icon.setFill(Color.YELLOW);
		this.status = icon;
	}

	
	public void setChecked(Checked checked) {
		this.checked = checked;
	}
	
	@Override
	public Checked getChecked() {
		return checked;
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
		return strategy.equals(other.getStrategy()) && description.equals(other.getDescription());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(strategy, description);
	}
	
	@Override
	public String toString() {
		return strategy + ": " + description;
	}
	
}
