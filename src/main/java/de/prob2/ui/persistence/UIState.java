package de.prob2.ui.persistence;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.stage.Stage;

@Singleton
public class UIState {
	private final ObjectProperty<Locale> localeOverride;
	private String guiState;
	private final Set<String> savedVisibleStages;
	private final Map<String, BoundingBox> savedStageBoxes;
	private final Map<String, Reference<Stage>> stages;
	
	@Inject
	public UIState(final Config config) {
		this.localeOverride = new SimpleObjectProperty<>(this, "localeOverride", null);
		this.guiState = "main.fxml";
		this.savedVisibleStages = new LinkedHashSet<>();
		this.savedStageBoxes = new LinkedHashMap<>();
		this.stages = new LinkedHashMap<>();
		
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
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				updateSavedStageBoxes();
				
				configData.localeOverride = getLocaleOverride();
				configData.guiState = getGuiState();
				configData.visibleStages = new ArrayList<>(getSavedVisibleStages());
				configData.stageBoxes = new HashMap<>(getSavedStageBoxes());
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
}
