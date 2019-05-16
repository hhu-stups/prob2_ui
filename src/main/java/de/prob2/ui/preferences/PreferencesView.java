package de.prob2.ui.preferences;

import java.util.Comparator;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
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
	@FXML private TreeTableView<PrefItem> tv;
	@FXML private TreeTableColumn<PrefItem, String> tvName;
	@FXML private TreeTableColumn<PrefItem, String> tvChanged;
	@FXML private TreeTableColumn<PrefItem, String> tvValue;
	@FXML private TreeTableColumn<PrefItem, String> tvDefaultValue;
	@FXML private TreeTableColumn<PrefItem, String> tvDescription;
	
	private final ObjectProperty<ProBPreferences> preferences;
	private final InvalidationListener refreshIL;
	
	@Inject
	private PreferencesView(final StageManager stageManager, final Injector injector) {
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
		
		tvChanged.setCellValueFactory(new TreeItemPropertyValueFactory<>("changed"));
		
		tvValue.setCellFactory(col -> {
			TreeTableCell<PrefItem, String> cell = new MultiTreeTableCell(this.preferencesProperty());
			cell.tableRowProperty().addListener((observable, from, to) ->
				to.treeItemProperty().addListener((observable1, from1, to1) ->
					cell.setEditable(to1 != null && to1.getValue() != null && to1.getValue() instanceof RealPrefTreeItem)
				)
			);
			return cell;
		});
		tvValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
		
		tvValue.setOnEditCommit(event -> this.getPreferences().setPreferenceValue(event.getRowValue().getValue().getName(), event.getNewValue()));

		tvDefaultValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("defaultValue"));
		
		tvDescription.setCellValueFactory(new TreeItemPropertyValueFactory<>("description"));
		
		tv.getRoot().setValue(new CategoryPrefTreeItem("Preferences (this should be invisible)"));
		
		this.preferencesProperty().addListener((observable, from, to) -> {
			if (from != null) {
				from.stateSpaceProperty().removeListener(this.refreshIL);
			}
			
			if (to != null) {
				to.stateSpaceProperty().addListener(this.refreshIL);
			}
			this.refresh();
		});
	}
	
	public void refresh() {
		if (this.getPreferences() == null || !this.getPreferences().hasStateSpace()) {
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
		
		if (!searchPattern.pattern().isEmpty()) {
			for (Iterator<TreeItem<PrefItem>> itcat = this.tv.getRoot().getChildren().iterator(); itcat.hasNext();) {
				final TreeItem<PrefItem> category = itcat.next();
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
		
		for (TreeItem<PrefItem> ti : this.tv.getRoot().getChildren()) {
			ti.getChildren().sort(Comparator.comparing(c -> c.getValue().getName()));
		}
	}

	private void generateTreeItems(final Pattern searchPattern) {
		for (ProBPreference pref : this.getPreferences().getPreferences().values()) {
			if (!searchPattern.matcher(pref.name).find() && !searchPattern.matcher(pref.description).find()) {
				// Preference's name and description don't match search, don't add it
				continue;
			}

			final TreeItem<PrefItem> category = this.tv.getRoot().getChildren().stream()
				.filter(ti -> ti.getValue().getName().equals(pref.category))
				.findAny()
				.orElseGet(() -> {
					final TreeItem<PrefItem> ti = new TreeItem<>(new CategoryPrefTreeItem(pref.category));
					this.tv.getRoot().getChildren().add(ti);
					ti.setExpanded(true);
					return ti;
				});

			final TreeItem<PrefItem> item = category.getChildren().stream()
				.filter(ti -> ti.getValue().getName().equals(pref.name))
				.findAny()
				.orElseGet(() -> {
					final TreeItem<PrefItem> ti = new TreeItem<>();
					category.getChildren().add(ti);
					return ti;
				});

			final String value = this.getPreferences().getPreferenceValue(pref.name);
			item.setValue(new RealPrefTreeItem(
				pref.name,
				value.equals(pref.defaultValue) ? "" : "*",
				value,
				ProBPreferenceType.fromProBPreference(pref),
				pref.defaultValue,
				pref.description
			));
		}
	}
}
