package de.prob2.ui.persistence;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.StageManager;

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
	private double[] horizontalDividerPositions;
	private double[] verticalDividerPositions;
		
	@Inject
	public UIState(final Config config) {
		this.localeOverride = new SimpleObjectProperty<>(this, "localeOverride", null);
		this.guiState = "main.fxml";
		this.savedVisibleStages = new LinkedHashSet<>();
		this.savedStageBoxes = new LinkedHashMap<>();
		this.stages = new LinkedHashMap<>();
		this.expandedTitledPanes = new ArrayList<>();
		
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				setLocaleOverride(configData.localeOverride);
				
				if (configData.guiState == null || configData.guiState.isEmpty()) {
					setGuiState("main.fxml");
				} else {
					setGuiState(configData.guiState);
				}
				
				if (configData.visibleStages != null) {
					getSavedVisibleStages().addAll(configData.visibleStages);
				}
				
				if (configData.stageBoxes != null) {
					getSavedStageBoxes().putAll(configData.stageBoxes);
				}
				
				if (configData.expandedTitledPanes != null) {
					getExpandedTitledPanes().addAll(configData.expandedTitledPanes);
				}
				
				if (configData.statesViewColumnsWidth != null) {
					setStatesViewColumnsWidth(configData.statesViewColumnsWidth);
				}
				
				if (configData.statesViewColumnsOrder != null) {
					setStatesViewColumnsOrder(configData.statesViewColumnsOrder);
				}
				
				if (configData.horizontalDividerPositions != null) {
					setHorizontalDividerPositions(configData.horizontalDividerPositions);
				}
				
				if (configData.verticalDividerPositions != null) {
					setVerticalDividerPositions(configData.verticalDividerPositions);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				updateSavedStageBoxes();
				
				configData.localeOverride = getLocaleOverride();
				configData.guiState = getGuiState();
				configData.visibleStages = new ArrayList<>(getSavedVisibleStages());
				configData.stageBoxes = new HashMap<>(getSavedStageBoxes());
				configData.expandedTitledPanes = new ArrayList<>(getExpandedTitledPanes());
			}
		});
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
