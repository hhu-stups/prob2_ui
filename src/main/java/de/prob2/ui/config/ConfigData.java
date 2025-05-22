package de.prob2.ui.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;
import de.prob2.ui.internal.PerspectiveKind;
import de.prob2.ui.operations.OperationsView;

import javafx.geometry.BoundingBox;

/**
 * The full set of config settings, used when the injector is available.
 */
@SuppressWarnings("PublicField")
public final class ConfigData extends BasicConfigData implements HasMetadata {
	public static final String FILE_TYPE = "Config";
	public static final int CURRENT_FORMAT_VERSION = 5;
	
	public static String configFileNameForVersion(final int formatVersion) {
		if (formatVersion >= 2) {
			return "config_v" + formatVersion + ".json";
		} else {
			throw new IllegalArgumentException("Config file format version " + formatVersion + " did not use versioned file names yet");
		}
	}
	
	public int maxRecentProjects;
	public int fontSize;
	public List<Path> recentProjects;
	public List<String> groovyConsoleInstructions;
	public List<String> bConsoleInstructions;
	public PerspectiveKind perspectiveKind;
	public String perspective;
	public List<String> visibleStages;
	public Map<String, BoundingBox> stageBoxes;
	public String currentPreference;
	public String currentMainTab;
	public String currentVerificationTab;
	public String currentVisualisationTab;
	public List<String> expandedTitledPanes;
	public String defaultProjectLocation;
	public boolean autoReloadMachine;
	public double[] horizontalDividerPositions;
	public double[] verticalDividerPositions;
	public List<Double> statesViewColumnsWidth;
	public OperationsView.SortMode operationsSortMode;
	public boolean operationsShowDisabled;
	public boolean operationsShowUnambiguous;
	public boolean operationsShowDescriptions;
	public Map<String, String> globalPreferences;
	public Path pluginDirectory;
	public Map<FileChooserManager.Kind, Path> fileChooserInitialDirectories;
	public ErrorItem.Type errorLevel;
	private JsonMetadata metadata;
	
	ConfigData() {}
	
	public static JsonMetadataBuilder metadataBuilder() {
		return new JsonMetadataBuilder(FILE_TYPE, CURRENT_FORMAT_VERSION)
			.withUserCreator()
			.withSavedNow();
	}
	
	@Override
	public JsonMetadata getMetadata() {
		return this.metadata;
	}
	
	public void setMetadata(final JsonMetadata metadata) {
		this.metadata = metadata;
	}
	
	@Override
	public ConfigData withMetadata(final JsonMetadata metadata) {
		// This implementation is actually really bad -
		// we're supposed to return a copy of the object with just the metadata changed,
		// and not mutate the original object in-place!
		// But as long as this method is only called by JacksonManager,
		// this isn't a problem.
		this.setMetadata(metadata);
		return this;
	}
}
