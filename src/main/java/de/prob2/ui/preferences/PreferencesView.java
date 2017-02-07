package de.prob2.ui.preferences;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob.prolog.term.ListPrologTerm;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;

import de.prob2.ui.internal.StageManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.BorderPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PreferencesView extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ListView.class);
	private static final Pattern EMPTY_PATTERN = Pattern.compile("", Pattern.CASE_INSENSITIVE);
	
	@FXML private TextField prefSearchField;
	@FXML private Button undoButton;
	@FXML private Button resetButton;
	@FXML private Button applyButton;
	@FXML private Label applyWarning;
	@FXML private TreeTableView<PrefTreeItem> tv;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvName;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvChanged;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvValue;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvDefaultValue;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvDescription;
	
	private final StageManager stageManager;
	private final ObjectProperty<ProBPreferences> preferences;
	
	@Inject
	private PreferencesView(final StageManager stageManager) {
		super();
		
		this.stageManager = stageManager;
		
		this.preferences = new SimpleObjectProperty<>(this, "preferences", null);
		
		stageManager.loadFXML(this, "preferences_view.fxml");
	}
	
	public ObjectProperty<ProBPreferences> preferencesProperty() {
		return this.preferences;
	}
	
	public ProBPreferences getPreferences() {
		return this.preferencesProperty().get();
	}
	
	public void setPreferences(final ProBPreferences preferences) {
		this.preferencesProperty().set(preferences);
	}
	
	@FXML
	private void initialize() {
		this.prefSearchField.textProperty().addListener(observable -> this.updatePreferences());
		this.resetButton.disableProperty().bind(this.preferencesProperty().isNull());
		
		tvName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		
		tvChanged.setCellValueFactory(new TreeItemPropertyValueFactory<>("changed"));
		
		tvValue.setCellFactory(col -> {
			TreeTableCell<PrefTreeItem, String> cell = new MultiTreeTableCell<>();
			cell.tableRowProperty().addListener((observable, from, to) ->
				to.treeItemProperty().addListener((observable1, from1, to1) ->
					cell.setEditable(to1 != null && to1.getValue() != null && to1.getValue() instanceof RealPrefTreeItem)
				)
			);
			return cell;
		});
		tvValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
		tvValue.setOnEditCommit(event -> {
			this.getPreferences().setPreferenceValue(event.getRowValue().getValue().getName(), event.getNewValue());
			this.updatePreferences();
		});
		
		tvDefaultValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("defaultValue"));
		
		tvDescription.setCellValueFactory(new TreeItemPropertyValueFactory<>("description"));
		
		tv.getRoot().setValue(new CategoryPrefTreeItem("Preferences"));
		
		final ChangeListener<StateSpace> updatePreferencesCL = (observable, from, to) -> this.updatePreferences();
		final MapChangeListener<String, String> updatePreferencesMCL = change -> this.updatePreferences();
		this.preferencesProperty().addListener((observable, from, to) -> {
			if (from != null) {
				from.stateSpaceProperty().removeListener(updatePreferencesCL);
				from.getChangedPreferences().removeListener(updatePreferencesMCL);
			}
			
			if (to == null) {
				this.undoButton.setDisable(true);
				this.applyWarning.setVisible(false);
				this.applyButton.setDisable(true);
			} else {
				this.undoButton.disableProperty().bind(to.changesAppliedProperty());
				this.applyWarning.visibleProperty().bind(to.changesAppliedProperty().not());
				this.applyButton.disableProperty().bind(to.changesAppliedProperty());
				
				to.stateSpaceProperty().addListener(updatePreferencesCL);
				to.getChangedPreferences().addListener(updatePreferencesMCL);
			}
		});
	}
	
	private void updatePreferences() {
		if (!this.getPreferences().hasStateSpace()) {
			this.tv.getRoot().getChildren().clear();
			return;
		}
		
		Pattern tempSearchPattern;
		try {
			tempSearchPattern = Pattern.compile(this.prefSearchField.getText(), Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
			LOGGER.trace("Bad regex syntax, this is probably not an error!", e);
			if (!this.prefSearchField.getStyleClass().contains("badsearch")) {
				this.prefSearchField.getStyleClass().add("badsearch");
			}
			tempSearchPattern = null;
		}
		
		// Extra final variable is necessary so the lambda below can use it.
		final Pattern searchPattern;
		if (tempSearchPattern == null) {
			searchPattern = EMPTY_PATTERN;
		} else {
			searchPattern = tempSearchPattern;
			this.prefSearchField.getStyleClass().remove("badsearch");
		}
		
		for (ProBPreference pref : this.getPreferences().getPreferences()) {
			if (
				!searchPattern.matcher(pref.name).find()
					&& !searchPattern.matcher(pref.description).find()
				) {
				// Preference's name and description don't match search, don't add it
				continue;
			}
			
			TreeItem<PrefTreeItem> category = null;
			for (TreeItem<PrefTreeItem> ti : this.tv.getRoot().getChildren()) {
				if (ti.getValue().getName().equals(pref.category)) {
					category = ti;
				}
			}
			if (category == null) {
				category = new TreeItem<>(new CategoryPrefTreeItem(pref.category));
				this.tv.getRoot().getChildren().add(category);
				category.setExpanded(true);
			}
			
			TreeItem<PrefTreeItem> item = null;
			for (TreeItem<PrefTreeItem> ti : category.getChildren()) {
				if (ti.getValue().getName().equals(pref.name)) {
					item = ti;
				}
			}
			
			final ProBPreferenceType type = createType(pref);
			
			final String value = this.getPreferences().getPreferenceValue(pref.name);
			
			if (item == null) {
				item = new TreeItem<>();
				category.getChildren().add(item);
			}
			item.setValue(new RealPrefTreeItem(
				pref.name,
				value.equals(pref.defaultValue) ? "" : "*",
				value,
				type,
				pref.defaultValue,
				pref.description
			));
		}
		
		if (!searchPattern.pattern().isEmpty()) {
			for (Iterator<TreeItem<PrefTreeItem>> itcat = this.tv.getRoot().getChildren().iterator(); itcat.hasNext();) {
				final TreeItem<PrefTreeItem> category = itcat.next();
				// Remove all items whose name and description don't match the search
				category.getChildren().removeIf(item ->
					!searchPattern.matcher(item.getValue().getName()).find()
						&& !searchPattern.matcher(item.getValue().getDescription()).find()
				);
				if (category.getChildren().isEmpty()) {
					// Category has no visible preferences, remove it
					itcat.remove();
				}
			}
		}
		
		this.tv.getRoot().getChildren().sort(Comparator.comparing(c -> c.getValue().getName()));
		
		for (TreeItem<PrefTreeItem> ti : this.tv.getRoot().getChildren()) {
			ti.getChildren().sort(Comparator.comparing(c -> c.getValue().getName()));
		}
	}
	
	private ProBPreferenceType createType(ProBPreference pref) {
		if (pref.type instanceof ListPrologTerm) {
			final ListPrologTerm values = (ListPrologTerm) pref.type;
			final String[] arr = new String[values.size()];
			for (int i = 0; i < values.size(); i++) {
				arr[i] = values.get(i).getFunctor();
			}
			return new ProBPreferenceType(arr);
		} else {
			return new ProBPreferenceType(pref.type.getFunctor());
		}
	}
	
	@FXML
	private void handleUndoChanges() {
		this.getPreferences().rollback();
	}
	
	@FXML
	private void handleRestoreDefaults() {
		for (ProBPreference pref : this.getPreferences().getPreferences()) {
			this.getPreferences().setPreferenceValue(pref.name, pref.defaultValue);
		}
	}
	
	@FXML
	private boolean handleApply() {
		try {
			this.getPreferences().apply();
			return true;
		} catch (IOException | ModelTranslationError e) {
			LOGGER.error("Application of changes failed", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to apply preference changes:\n" + e).showAndWait();
			return false;
		}
	}
}
