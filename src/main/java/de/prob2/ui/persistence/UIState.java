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

import de.prob2.ui.MainController;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.internal.PerspectiveKind;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.stage.Stage;

@Singleton
public final class UIState {
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
