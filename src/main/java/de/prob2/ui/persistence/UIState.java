package de.prob2.ui.persistence;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.MainController;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.PerspectiveKind;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class UIState {
	private static final Logger LOGGER = LoggerFactory.getLogger(UIState.class);

	private final ObjectProperty<Locale> localeOverride;
	private PerspectiveKind perspectiveKind;
	private String perspective;
	private final Set<String> savedVisibleStages;
	private final Map<String, BoundingBox> savedStageBoxes;
	private final Map<String, Reference<Stage>> stages;
	
	@Inject
	public UIState(final Config config) {
		this.localeOverride = new SimpleObjectProperty<>(this, "localeOverride", null);
		this.perspectiveKind = PerspectiveKind.PRESET;
		this.perspective = MainController.DEFAULT_PERSPECTIVE;
		this.savedVisibleStages = new LinkedHashSet<>();
		this.savedStageBoxes = new LinkedHashMap<>();
		this.stages = new LinkedHashMap<>();
		
		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				setLocaleOverride(configData.localeOverride);
				
				if (configData.perspectiveKind != null && configData.perspective != null) {
					setPerspectiveKind(configData.perspectiveKind);
					setPerspective(configData.perspective);
				}
				
				if (configData.visibleStages != null) {
					savedVisibleStages.addAll(configData.visibleStages);
				}
				
				if (configData.stageBoxes != null) {
					savedStageBoxes.putAll(configData.stageBoxes);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				updateSavedStageBoxes();
				
				configData.localeOverride = getLocaleOverride();
				configData.perspectiveKind = getPerspectiveKind();
				configData.perspective = getPerspective();
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
	
	public PerspectiveKind getPerspectiveKind() {
		return perspectiveKind;
	}
	
	public void setPerspectiveKind(final PerspectiveKind perspectiveKind) {
		this.perspectiveKind = perspectiveKind;
	}
	
	public String getPerspective() {
		return perspective;
	}
	
	public void setPerspective(final String perspective) {
		this.perspective = perspective;
	}
	
	public Set<String> getSavedVisibleStages() {
		return Collections.unmodifiableSet(this.savedVisibleStages);
	}
	
	public void stageWasShown(String stageId) {
		LOGGER.trace("Stage with ID \"{}\" was shown", stageId);
		this.savedVisibleStages.add(stageId);
	}
	
	public void stageWasHidden(String stageId) {
		LOGGER.trace("Stage with ID \"{}\" was hidden", stageId);
		this.savedVisibleStages.remove(stageId);
	}
	
	public void stageWasFocused(String stageId) {
		if (this.savedVisibleStages.remove(stageId)) {
			LOGGER.trace("Stage with ID \"{}\" was focused", stageId);
			this.savedVisibleStages.add(stageId);
		}
	}
	
	public void resetVisibleStages() {
		this.savedVisibleStages.clear();
	}
	
	public Map<String, BoundingBox> getSavedStageBoxes() {
		return Collections.unmodifiableMap(this.savedStageBoxes);
	}
	
	public void saveStageBox(Stage stage, String stageId) {
		BoundingBox box = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
		LOGGER.trace(
			"Saving position/size for stage with ID \"{}\": x={}, y={}, width={}, height={}",
			stageId, box.getMinX(), box.getMinY(), box.getWidth(), box.getHeight()
		);
		this.savedStageBoxes.put(stageId, box);
	}
	
	public Map<String, Reference<Stage>> getStages() {
		return Collections.unmodifiableMap(this.stages);
	}
	
	public void addStage(Stage stage, String stageId) {
		this.stages.put(stageId, new WeakReference<>(stage));
	}
	
	public void updateSavedStageBoxes() {
		for (final Map.Entry<String, Reference<Stage>> entry : this.getStages().entrySet()) {
			final Stage stage = entry.getValue().get();
			if (stage != null) {
				this.saveStageBox(stage, entry.getKey());
			}
		}
	}
}
