package de.prob2.ui.preferences;

import java.util.Comparator;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.BorderPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public final class PreferencesView extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesView.class);
	private static final Pattern EMPTY_PATTERN = Pattern.compile("", Pattern.CASE_INSENSITIVE);
	
	@FXML private TextField prefSearchField;
	@FXML private TreeTableView<PrefTreeItem> tv;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvName;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvChanged;
	@FXML private TreeTableColumn<PrefTreeItem, PrefTreeItem> tvValue;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvDefaultValue;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvDescription;
	
	private final ObjectProperty<ProBPreferences> preferences;
	private final InvalidationListener refreshIL;
	
	@Inject
	private PreferencesView(final StageManager stageManager) {
		super();
		
		this.preferences = new SimpleObjectProperty<>(this, "preferences", null);
		this.refreshIL = o -> this.refresh();
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
		this.prefSearchField.textProperty().addListener(observable -> this.refresh());
		
		this.tv.getSelectionModel().clearSelection();
		
		tvName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		
		tvChanged.setCellValueFactory(features -> Bindings.createStringBinding(
			() -> features.getValue().getValue().isChanged() ? "*" : "",
			features.getValue().valueProperty()
		));
		
		tvValue.setCellFactory(col -> new PreferenceValueCell(this.preferencesProperty()));
		tvValue.setCellValueFactory(features -> features.getValue().valueProperty());

		tvDefaultValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("defaultValue"));
		
		tvDescription.setCellValueFactory(new TreeItemPropertyValueFactory<>("description"));
		
		tv.getRoot().setValue(new PrefTreeItem.Category("Preferences (this should be invisible)"));
		
		this.preferencesProperty().addListener((observable, from, to) -> {
			if (from != null) {
				from.getCurrentPreferenceValues().removeListener(this.refreshIL);
				from.getPreferenceChanges().removeListener(this.refreshIL);
			}
			
			if (to != null) {
				to.getCurrentPreferenceValues().addListener(this.refreshIL);
				to.getPreferenceChanges().addListener(this.refreshIL);
			}
			this.refresh();
		});
	}
	
	public void refresh() {
		if (this.getPreferences() == null) {
			this.tv.getRoot().getChildren().clear();
			return;
		}
		
		Pattern tempSearchPattern;
		try {
			tempSearchPattern = Pattern.compile(this.prefSearchField.getText(), Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
			LOGGER.trace("Bad regex syntax, this is probably not an error!", e);
			if (!this.prefSearchField.getStyleClass().contains("text-field-error")) {
				this.prefSearchField.getStyleClass().add("text-field-error");
			}
			tempSearchPattern = null;
		}
		
		// Extra final variable is necessary so the lambda below can use it.
		final Pattern searchPattern;
		if (tempSearchPattern == null) {
			searchPattern = EMPTY_PATTERN;
		} else {
			searchPattern = tempSearchPattern;
			this.prefSearchField.getStyleClass().remove("text-field-error");
		}
		generateTreeItems(searchPattern);
		
		for (Iterator<TreeItem<PrefTreeItem>> itcat = this.tv.getRoot().getChildren().iterator(); itcat.hasNext();) {
			final TreeItem<PrefTreeItem> category = itcat.next();
			// Remove all items whose name and description don't match the search or which are no longer present in the ProBPreferences object
			category.getChildren().removeIf(item ->
				(!searchPattern.matcher(item.getValue().getName()).find()
					&& !searchPattern.matcher(item.getValue().getDescription()).find())
				|| !this.getPreferences().getPreferenceInfos().containsKey(item.getValue().getName())
			);
			if (category.getChildren().isEmpty()) {
				// Category has no visible preferences, remove it
				itcat.remove();
			}
		}
		
		this.tv.getRoot().getChildren().sort(Comparator.comparing(c -> c.getValue().getName()));
		
		for (TreeItem<PrefTreeItem> ti : this.tv.getRoot().getChildren()) {
			ti.getChildren().sort(Comparator.comparing(c -> c.getValue().getName()));
		}
	}

	private void generateTreeItems(final Pattern searchPattern) {
		for (ProBPreference pref : this.getPreferences().getPreferenceInfos().values()) {
			if (!searchPattern.matcher(pref.name).find() && !searchPattern.matcher(pref.description).find()) {
				// Preference's name and description don't match search, don't add it
				continue;
			}

			final TreeItem<PrefTreeItem> category = this.tv.getRoot().getChildren().stream()
				.filter(ti -> ti.getValue().getName().equals(pref.category))
				.findAny()
				.orElseGet(() -> {
					final TreeItem<PrefTreeItem> ti = new TreeItem<>(new PrefTreeItem.Category(pref.category));
					this.tv.getRoot().getChildren().add(ti);
					ti.setExpanded(true);
					return ti;
				});

			final TreeItem<PrefTreeItem> item = category.getChildren().stream()
				.filter(ti -> ti.getValue().getName().equals(pref.name))
				.findAny()
				.orElseGet(() -> {
					final TreeItem<PrefTreeItem> ti = new TreeItem<>();
					category.getChildren().add(ti);
					return ti;
				});

			item.setValue(new PrefTreeItem.Preference(pref, this.getPreferences().getPreferenceValueWithChanges(pref.name)));
		}
	}
}
