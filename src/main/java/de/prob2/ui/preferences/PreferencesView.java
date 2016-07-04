package de.prob2.ui.preferences;

import java.io.IOException;

import com.google.inject.Inject;
import de.prob.animator.domainobjects.ProBPreference;
import de.prob.exception.ProBError;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.util.converter.DefaultStringConverter;

public class PreferencesView extends TitledPane implements IAnimationChangeListener {
	@FXML private TreeTableView<PrefTreeItem> tv;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvName;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvValue;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvDefaultValue;
	@FXML private TreeTableColumn<PrefTreeItem, String> tvDescription;

	private AnimationSelector animations;
	private Trace trace;
	private Preferences preferences;

	@Inject
	public PreferencesView(FXMLLoader loader, AnimationSelector animations) {
		this.animations = animations;
		animations.registerAnimationChangeListener(this);

		try {
			loader.setLocation(getClass().getResource("preferences_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void initialize() {
		tvName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		
		tvValue.setCellFactory(col -> {
			TreeTableCell<PrefTreeItem, String> cell = new TextFieldTreeTableCell<>(new DefaultStringConverter());
			// TODO Make value cell in header rows not editable
			/*
			cell.editableProperty().addListener(observable -> {
				System.out.println("Editability changed on " + observable);
			});
			cell.tableRowProperty().addListener(observable -> {
				TreeTableRow<PrefTreeItem> row = ((ReadOnlyObjectProperty<TreeTableRow<PrefTreeItem>>)observable).get();
				row.treeItemProperty().addListener(observable1 -> {
					TreeItem<PrefTreeItem> ti = ((ReadOnlyObjectProperty<TreeItem<PrefTreeItem>>)observable1).get();
					cell.setEditable(ti != null && ti.getValue() != null && ti.getValue() instanceof RealPrefTreeItem);
					if (ti == null) {
						System.out.println(cell + " not editable because TreeItem is null");
					} else if (ti.getValue() == null) {
						System.out.println(cell + " not editable because TreeItem value is null");
					} else if (ti.getValue() instanceof RealPrefTreeItem) {
						System.out.println(cell + " (" + ti.getValue().getName() + ") editable");
					} else {
						System.out.println(cell + " (" + ti.getValue().getName() + ") not editable because value is not a RealPrefTreeItem");
					}
					System.out.println(cell.isEditable());
				});
			});
			*/
			return cell;
		});
		tvValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
		tvValue.setOnEditCommit(event -> {
			if (preferences == null) {
				return;
			}
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
				item = new TreeItem<>(new RealPrefTreeItem(pref.name, "", pref.defaultValue, pref.description));
				category.getChildren().add(item);
			}
			item.getValue().updateValue(this.preferences);
		}
	}

	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		this.trace = currentTrace;
		this.preferences = new Preferences(this.trace.getStateSpace());
		this.updatePreferences();
	}

	@Override
	public void animatorStatus(boolean busy) {}
}
