package de.prob2.ui.internal;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.geometry.BoundingBox;
import javafx.stage.Stage;

@Singleton
public class UIState {
	private static final Set<String> DETACHED = new HashSet<>(Arrays.asList(new String[] {"History", "Operations", "Model Check", "Statistics", "Animations"}));
	
	private String guiState;
	private final Set<String> savedVisibleStages;
	private final Map<String, BoundingBox> savedStageBoxes;
	private final Map<String, Reference<Stage>> stages;
	private final List<String> groovyObjectTabs;
	private List<String> expandedTitledPanes;
		
	@Inject
	public UIState() {
		this.guiState = "main.fxml";
		this.savedVisibleStages = new LinkedHashSet<>();
		this.savedStageBoxes = new LinkedHashMap<>();
		this.stages = new LinkedHashMap<>();
		this.groovyObjectTabs = new ArrayList<>();
		this.expandedTitledPanes = new ArrayList<>();
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
	
	public void moveStageToEnd(Stage stage) {
		savedVisibleStages.remove(stage.getClass().getName());
		savedVisibleStages.add(stage.getClass().getName());
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
	
	public void addGroovyObjectTab(String tab) {
		groovyObjectTabs.add(tab);
	}
	
	public void addGroovyObjectTab(String tab, int index) {
		groovyObjectTabs.add(index, tab);
	}
	
	public void removeGroovyObjectTab(int index) {
		groovyObjectTabs.remove(index);
	}
		
	public List<String> getGroovyObjectTabs() {
		return groovyObjectTabs;
	}
	
	public void clearDetachedStages() {
		stages.keySet().removeAll(DETACHED);
	}
	
	public List<String> getExpandedTitledPanes() {
		return expandedTitledPanes;
	}
}
