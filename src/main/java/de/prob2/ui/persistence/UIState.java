package de.prob2.ui.persistence;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.operations.OperationsView;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.stage.Stage;

@Singleton
public class UIState {
	private static final Set<String> DETACHED = new HashSet<>(Arrays.asList("History", "Operations", "Verifications", "Statistics", "Project"));
	
	private final ObjectProperty<Locale> localeOverride;
	private String guiState;
	private final Set<String> savedVisibleStages;
	private final Map<String, BoundingBox> savedStageBoxes;
	private final Map<String, Reference<Stage>> stages;
	private List<String> expandedTitledPanes;
	private double[] statesViewColumnsWidth;
	private String[] statesViewColumnsOrder;
	private OperationsView.SortMode operationsSortMode;
	private boolean operationsShowNotEnabled;
	private double[] horizontalDividerPositions;
	private double[] verticalDividerPositions;
		
	@Inject
	public UIState() {
		this.localeOverride = new SimpleObjectProperty<>(this, "localeOverride", null);
		this.guiState = "main.fxml";
		this.savedVisibleStages = new LinkedHashSet<>();
		this.savedStageBoxes = new LinkedHashMap<>();
		this.stages = new LinkedHashMap<>();
		this.expandedTitledPanes = new ArrayList<>();
		this.statesViewColumnsWidth = new double[3];
		this.statesViewColumnsOrder = new String[3];
	}
	
	public ObjectProperty<Locale> localeOverrideProperty() {
		return this.localeOverride;
	}
	
	public Locale getLocaleOverride() {
		return this.localeOverrideProperty().get();
	}
	
	public void setLocaleOverride(final Locale localeOverride) {
		this.localeOverrideProperty().set(localeOverride);
	}
	
	public void setGuiState(String guiState) {
		this.guiState = guiState;
	}
	
	public String getGuiState() {
		return guiState;
	}
	
	public Set<String> getSavedVisibleStages() {
		return this.savedVisibleStages;
	}
	
	public Map<String, BoundingBox> getSavedStageBoxes() {
		return this.savedStageBoxes;
	}
	
	public Map<String, Reference<Stage>> getStages() {
		return this.stages;
	}
	
	public void moveStageToEnd(String id) {
		savedVisibleStages.remove(id);
		savedVisibleStages.add(id);
	}
	
	public void updateSavedStageBoxes() {
		for (final Map.Entry<String, Reference<Stage>> entry : this.getStages().entrySet()) {
			final Stage stage = entry.getValue().get();
			if (stage != null) {
				this.getSavedStageBoxes().put(
					entry.getKey(),
					new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
				);
			}
		}
	}
	
	public void clearDetachedStages() {
		for (Reference<Stage> stageRef : stages.values()){
			Stage stage = stageRef.get();
			if (stage != null && !"de.prob2.ui.ProB2".equals(StageManager.getPersistenceID(stage))) {
				stage.hide();
			}
		}
		stages.keySet().removeAll(DETACHED);
	}
	
	public List<String> getExpandedTitledPanes() {
		return expandedTitledPanes;
	}
	
	public void setStatesViewColumnsWidth(double[] width) {
		this.statesViewColumnsWidth = width;
	}
	
	public double[] getStatesViewColumnsWidth() {
		return statesViewColumnsWidth;
	}
	
	public void setStatesViewColumnsOrder(String[] order) {
		this.statesViewColumnsOrder = order;
	}
	
	public String[] getStatesViewColumnsOrder() {
		return statesViewColumnsOrder;
	}
	
	public void setOperationsSortMode(OperationsView.SortMode mode) {
		this.operationsSortMode = mode;
	}
	
	public OperationsView.SortMode getOperationsSortMode() {
		return operationsSortMode;
	}
	
	public void setOperationsShowNotEnabled(boolean showNotEnabled) {
		operationsShowNotEnabled = showNotEnabled;
	}
	
	public boolean getOperationsShowNotEnabled() {
		return operationsShowNotEnabled;
	}
	
	public void setHorizontalDividerPositions(double[] pos) {
		this.horizontalDividerPositions = pos;
	}
	
	public double[] getHorizontalDividerPositions() {
		return horizontalDividerPositions;
	}
	
	public void setVerticalDividerPositions(double[] pos) {
		this.verticalDividerPositions = pos;
	}
	
	public double[] getVerticalDividerPositions() {
		return verticalDividerPositions;
	}
	
}
