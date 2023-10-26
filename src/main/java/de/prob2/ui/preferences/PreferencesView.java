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

	@FXML
	private TextField prefSearchField;
	@FXML
	private TreeTableView<PrefTreeItem> tv;
	@FXML
	private TreeTableColumn<PrefTreeItem, String> tvName;
	@FXML
	private TreeTableColumn<PrefTreeItem, String> tvChanged;
	@FXML
	private TreeTableColumn<PrefTreeItem, PrefTreeItem> tvValue;
	@FXML
	private TreeTableColumn<PrefTreeItem, String> tvDefaultValue;
	@FXML
	private TreeTableColumn<PrefTreeItem, String> tvDescription;

	private final ObjectProperty<PreferencesChangeState> state;
	private final InvalidationListener refreshIL;

	@Inject
	private PreferencesView(final StageManager stageManager) {
		super();

		this.state = new SimpleObjectProperty<>(this, "preferences", null);
		this.refreshIL = o -> this.refresh();
		stageManager.loadFXML(this, "preferences_view.fxml");
	}

	public ObjectProperty<PreferencesChangeState> stateProperty() {
		return this.state;
	}

	public PreferencesChangeState getState() {
		return this.stateProperty().get();
	}

	public void setState(final PreferencesChangeState state) {
		this.stateProperty().set(state);
	}

	@FXML
	private void initialize() {
		this.prefSearchField.textProperty().addListener(observable -> this.refresh());

		this.tv.getSelectionModel().clearSelection();

		tvName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));

		tvChanged.setCellValueFactory(features -> Bindings.createStringBinding(
			() -> features.getValue().getValue().isChanged() ? "*" : " ",
			features.getValue().valueProperty()
		));

		tvValue.setCellFactory(col -> new PreferenceValueCell(this.stateProperty()));
		tvValue.setCellValueFactory(features -> features.getValue().valueProperty());

		tvDefaultValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("defaultValue"));

		tvDescription.setCellValueFactory(new TreeItemPropertyValueFactory<>("description"));

		tv.getRoot().setValue(new PrefTreeItem.Category("Preferences (this should be invisible)"));

		this.stateProperty().addListener((observable, from, to) -> {
			// this uses the maps directly because JavaFX does not propagate the events correctly.
			if (from != null) {
				from.currentPreferenceValues.removeListener(this.refreshIL);
				from.preferenceChanges.removeListener(this.refreshIL);
			}

			if (to != null) {
				to.currentPreferenceValues.addListener(this.refreshIL);
				to.preferenceChanges.addListener(this.refreshIL);
			}
			this.refresh();
		});
	}

	public void refresh() {
		if (this.getState() == null) {
			this.tv.getRoot().getChildren().clear();
			return;
		}

		Pattern tempSearchPattern;
		try {
			tempSearchPattern = Pattern.compile(this.prefSearchField.getText(), Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
			tempSearchPattern = EMPTY_PATTERN;
			LOGGER.trace("Bad regex syntax, this is probably not an error!", e);
			this.prefSearchField.getStyleClass().add("text-field-error");
		}

		// Extra final variable is necessary so the lambda below can use it.
		final Pattern searchPattern = tempSearchPattern;
		generateTreeItems(searchPattern);

		for (Iterator<TreeItem<PrefTreeItem>> itcat = this.tv.getRoot().getChildren().iterator(); itcat.hasNext(); ) {
			final TreeItem<PrefTreeItem> category = itcat.next();
			// Remove all items whose name and description don't match the search or which are no longer present in the PreferencesChangeState object
			category.getChildren().removeIf(item ->
				(!searchPattern.matcher(item.getValue().getName()).find()
					&& !searchPattern.matcher(item.getValue().getDescription()).find())
					|| !this.getState().getPreferenceInfos().containsKey(item.getValue().getName())
			);
			if (category.getChildren().isEmpty()) {
				// Category has no visible preferences, remove it
				itcat.remove();
			}
		}

		final Comparator<TreeItem<? extends PrefTreeItem>> nameComparator = Comparator.comparing(c -> c.getValue().getName());
		this.tv.getRoot().getChildren().sort(nameComparator);
		for (TreeItem<PrefTreeItem> ti : this.tv.getRoot().getChildren()) {
			ti.getChildren().sort(nameComparator);
		}
	}

	private void generateTreeItems(final Pattern searchPattern) {
		for (ProBPreference pref : this.getState().getPreferenceInfos().values()) {
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

			item.setValue(new PrefTreeItem.Preference(pref, this.getState().getPreferenceValueWithChanges(pref.name)));
		}
	}
}
