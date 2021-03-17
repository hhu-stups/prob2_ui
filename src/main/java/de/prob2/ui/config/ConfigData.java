package de.prob2.ui.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob2.ui.internal.PerspectiveKind;
import de.prob2.ui.operations.OperationsView;

import javafx.geometry.BoundingBox;

/**
 * The full set of config settings, used when the injector is available.
 */
@SuppressWarnings("PublicField")
public final class ConfigData extends BasicConfigData {
	public static final String FILE_TYPE = "Config";
	public static final int CURRENT_FORMAT_VERSION = 2;
	
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
	public List<String> expandedTitledPanes;
	public boolean bConsoleExpanded;
	public String defaultProjectLocation;
	public double[] horizontalDividerPositions;
	public double[] verticalDividerPositions;
	public List<Double> statesViewColumnsWidth;
	public OperationsView.SortMode operationsSortMode;
	public boolean operationsShowDisabled;
	public boolean operationsShowUnambiguous;
	public Map<String, String> globalPreferences;
	public Path pluginDirectory;
	public Map<FileChooserManager.Kind, Path> fileChooserInitialDirectories;
	public ErrorItem.Type errorLevel;
	
	ConfigData() {}
}
