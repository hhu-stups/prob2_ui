package de.prob2.ui.preferences;

import java.io.IOException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.ProBError;
import de.prob.prolog.term.ListPrologTerm;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.Stage;

@Singleton
public class PreferencesStage extends Stage implements IAnimationChangeListener {
	@FXML private Button applyButton;
	@FXML private Button resetButton;
	@FXML private Label applyWarning;
	@FXML private TreeTableView<PrefTreeItem> tv;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvName;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvChanged;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvValue;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvDefaultValue;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvDescription;
	
	private final AnimationSelector animationSelector;
	private final Preferences preferences;

	@Inject
	private PreferencesStage(
		final AnimationSelector animationSelector,
		final Preferences preferences,
		final FXMLLoader loader
	) {
		this.animationSelector = animationSelector;
		this.animationSelector.registerAnimationChangeListener(this);
		this.preferences = preferences;
		Trace currentTrace = this.animationSelector.getCurrentTrace();
		this.preferences.setStateSpace(currentTrace == null ? null : currentTrace.getStateSpace());
		
		loader.setLocation(this.getClass().getResource("preferences_stage.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void initialize() {
		applyButton.disableProperty().bind(this.preferences.changesAppliedProperty());
		resetButton.setDisable(this.animationSelector.getCurrentTrace() == null);
		applyWarning.visibleProperty().bind(this.preferences.changesAppliedProperty().not());
		
		tvName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		
		tvChanged.setCellValueFactory(new TreeItemPropertyValueFactory<>("changed"));
		
		tvValue.setCellFactory(col -> {
			TreeTableCell<PrefTreeItem, String> cell = new MultiTreeTableCell<>();
			cell.tableRowProperty().addListener((observable, from, to) -> {
				to.treeItemProperty().addListener((observable1, from1, to1) -> {
					cell.setEditable(to1 != null && to1.getValue() != null && to1.getValue() instanceof RealPrefTreeItem);
				});
			});
			return cell;
		});
		tvValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
		tvValue.setOnEditCommit(event -> {
			try {
				this.preferences.setPreferenceValue(event.getRowValue().getValue().getName(), event.getNewValue());
			} catch (final ProBError exc) {
				exc.printStackTrace();
				new Alert(Alert.AlertType.ERROR, "The entered preference value is not valid.\n" + exc.getMessage()).show();
			}
			this.updatePreferences();
		});
		
		tvDefaultValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("defaultValue"));
		
		tvDescription.setCellValueFactory(new TreeItemPropertyValueFactory<>("description"));
		
		tv.getRoot().setValue(new CategoryPrefTreeItem("Preferences"));
	}
	
	private void updatePreferences() {
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
			if (item == null) {
				final PreferenceType type;
				if (pref.type instanceof ListPrologTerm) {
					final ListPrologTerm values = (ListPrologTerm)pref.type;
					final String[] arr = new String[values.size()];
					for (int i = 0; i < values.size(); i++) {
						arr[i] = values.get(i).getFunctor();
					}
					type = new PreferenceType(arr);
				} else {
					type = new PreferenceType(pref.type.getFunctor());
				}
				item = new TreeItem<>(new RealPrefTreeItem(pref.name, "", "", type, pref.defaultValue, pref.description));
				category.getChildren().add(item);
			}
			item.getValue().updateValue(this.preferences);
		}
	}
	
	@FXML
	private void handleApplyChanges(final ActionEvent event) {
		this.preferences.apply();
	}
	
	@FXML
	private void handleResetAll(final ActionEvent event) {
		for (ProBPreference pref : this.preferences.getPreferences()) {
			this.preferences.setPreferenceValue(pref.name, pref.defaultValue);
		}
	}

	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		this.preferences.setStateSpace(currentTrace == null ? null : currentTrace.getStateSpace());
		this.updatePreferences();
		this.resetButton.setDisable(currentTrace == null);
	}

	@Override
	public void animatorStatus(boolean busy) {}
}
