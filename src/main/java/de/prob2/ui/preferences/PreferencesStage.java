package de.prob2.ui.preferences;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BException;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.ProBError;
import de.prob.model.representation.AbstractElement;
import de.prob.prolog.term.ListPrologTerm;
import de.prob.statespace.Trace;

import de.prob2.ui.menu.RecentFiles;
import de.prob2.ui.prob2fx.CurrentStage;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.states.ClassBlacklist;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class PreferencesStage extends Stage {
	private static final StringConverter<Class<? extends AbstractElement>> ELEMENT_CLASS_STRING_CONVERTER = new StringConverter<Class<? extends AbstractElement>>() {
		@Override
		public Class<? extends AbstractElement> fromString(final String s) {
			final Class<?> clazz;

			try {
				clazz = Class.forName(s);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e);
			}

			return clazz.asSubclass(AbstractElement.class);
		}

		@Override
		public String toString(final Class<? extends AbstractElement> clazz) {
			return clazz == null ? "null" : clazz.getSimpleName();
		}
	};
	
	private static final Logger logger = LoggerFactory.getLogger(ListView.class);

	@FXML private Stage stage;
	@FXML private Spinner<Integer> recentFilesCountSpinner;
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
	@FXML private ListView<Class<? extends AbstractElement>> blacklistView;

	private final ClassBlacklist classBlacklist;
	private final CurrentTrace currentTrace;
	private final ProBPreferences preferences;
	private final RecentFiles recentFiles;

	@Inject
	private PreferencesStage(
			final ClassBlacklist classBlacklist,
			final CurrentTrace currentTrace,
			final ProBPreferences preferences,
			final RecentFiles recentFiles,
			final FXMLLoader loader,
			final CurrentStage currentStage) {
		this.classBlacklist = classBlacklist;
		this.currentTrace = currentTrace;
		this.preferences = preferences;
		this.preferences.setStateSpace(currentTrace.exists() ? currentTrace.getStateSpace() : null);
		this.recentFiles = recentFiles;

		loader.setLocation(this.getClass().getResource("preferences_stage.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}

		currentStage.register(this);
	}

	@FXML
	public void initialize() {
		// General
		
		final SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50);
		
		// bindBidirectional doesn't work properly here, don't ask why
		this.recentFiles.maximumProperty().addListener((observable, from, to) -> valueFactory.setValue((Integer)to));
		valueFactory.valueProperty().addListener((observable, from, to) -> this.recentFiles.setMaximum(to));
		valueFactory.setValue(this.recentFiles.getMaximum());
		
		this.recentFilesCountSpinner.setValueFactory(valueFactory);
		
		// ProB Preferences

		this.undoButton.disableProperty().bind(this.preferences.changesAppliedProperty());
		this.resetButton.disableProperty().bind(this.currentTrace.existsProperty().not());
		this.applyWarning.visibleProperty().bind(this.preferences.changesAppliedProperty().not());
		this.applyButton.disableProperty().bind(this.preferences.changesAppliedProperty());

		tvName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));

		tvChanged.setCellValueFactory(new TreeItemPropertyValueFactory<>("changed"));

		tvValue.setCellFactory(col -> {
			TreeTableCell<PrefTreeItem, String> cell = new MultiTreeTableCell<>();
			cell.tableRowProperty().addListener((observable, from, to) ->
				to.treeItemProperty().addListener((observable1, from1, to1) ->
					cell.setEditable(
							to1 != null && to1.getValue() != null && to1.getValue() instanceof RealPrefTreeItem)
				)
			);
			return cell;
		});
		tvValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
		tvValue.setOnEditCommit(event -> {
			try {
				this.preferences.setPreferenceValue(event.getRowValue().getValue().getName(), event.getNewValue());
			} catch (final ProBError exc) {
				logger.error("Invalid preference", exc);
				Alert alert = new Alert(Alert.AlertType.ERROR,
						"The entered preference value is not valid.\n" + exc.getMessage());
				alert.getDialogPane().getStylesheets().add("prob.css");
				alert.show();
			}
			this.updatePreferences();
		});

		tvDefaultValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("defaultValue"));

		tvDescription.setCellValueFactory(new TreeItemPropertyValueFactory<>("description"));

		tv.getRoot().setValue(new CategoryPrefTreeItem("Preferences"));

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			this.preferences.setStateSpace(to == null ? null : to.getStateSpace());
			this.updatePreferences();
		};
		this.currentTrace.addListener(traceChangeListener);
		// Fire the listener manually once to load the current preferences
		traceChangeListener.changed(this.currentTrace, null, currentTrace.get());

		// States View

		this.blacklistView.setCellFactory(CheckBoxListCell.forListView(clazz -> {
			final BooleanProperty prop = new SimpleBooleanProperty(!this.classBlacklist.getBlacklist().contains(clazz));

			prop.addListener((changed, from, to) -> {
				if (to) {
					this.classBlacklist.getBlacklist().remove(clazz);
				} else {
					this.classBlacklist.getBlacklist().add(clazz);
				}
			});

			this.classBlacklist.getBlacklist()
					.addListener((SetChangeListener<? super Class<? extends AbstractElement>>) change -> {
				if (clazz.equals(change.getElementAdded()) || clazz.equals(change.getElementRemoved())) {
					prop.set(change.wasRemoved());
				}
			});

			return prop;
		}, ELEMENT_CLASS_STRING_CONVERTER));
		
		this.blacklistView.getItems().setAll(this.classBlacklist.getKnownClasses());
		this.blacklistView.getItems().sort(Comparator.comparing(Class::getSimpleName));

		this.classBlacklist.getKnownClasses()
				.addListener((SetChangeListener<? super Class<? extends AbstractElement>>) change -> {
					final List<Class<? extends AbstractElement>> items = this.blacklistView.getItems();
					final Class<? extends AbstractElement> added = change.getElementAdded();
					final Class<? extends AbstractElement> removed = change.getElementRemoved();

					if (change.wasAdded() && !items.contains(added)) {
						items.add(added);
						items.sort(Comparator.comparing(Class::getSimpleName));
					} else if (change.wasRemoved() && items.contains(removed)) {
						if (this.classBlacklist.getBlacklist().contains(removed)) {
							this.classBlacklist.getBlacklist().remove(removed);
						}
						items.remove(removed);
					}
				});
	}

	private void updatePreferences() {
		if (!this.preferences.hasStateSpace()) {
			this.tv.getRoot().getChildren().clear();
			return;
		}

		for (ProBPreference pref : this.preferences.getPreferences()) {
			TreeItem<PrefTreeItem> category = null;
			for (TreeItem<PrefTreeItem> ti : this.tv.getRoot().getChildren()) {
				if (ti.getValue().getName().equals(pref.category)) {
					category = ti;
				}
			}
			if (category == null) {
				category = new TreeItem<>(new CategoryPrefTreeItem(pref.category));
				this.tv.getRoot().getChildren().add(category);
			}

			TreeItem<PrefTreeItem> item = null;
			for (TreeItem<PrefTreeItem> ti : category.getChildren()) {
				if (ti.getValue().getName().equals(pref.name)) {
					item = ti;
				}
			}
			
			final ProBPreferenceType type = createType(pref);

			final String value = this.preferences.getPreferenceValue(pref.name);
			
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
	private void handleClose() {
		if (this.preferences.hasStateSpace()) {
			this.handleUndoChanges();
		}
		this.stage.close();
	}

	@FXML
	private void handleUndoChanges() {
		this.preferences.rollback();
		this.updatePreferences();
	}

	@FXML
	private void handleRestoreDefaults() {
		for (ProBPreference pref : this.preferences.getPreferences()) {
			this.preferences.setPreferenceValue(pref.name, pref.defaultValue);
		}
		this.updatePreferences();
	}

	@FXML
	private boolean handleApply() {
		try {
			this.preferences.apply();
			return true;
		} catch (BException | IOException e) {
			logger.error("Application of changes failed", e);
			Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to apply preference changes:\n" + e);
			alert.getDialogPane().getStylesheets().add("prob.css");
			alert.showAndWait();
			return false;
		}
	}
}
