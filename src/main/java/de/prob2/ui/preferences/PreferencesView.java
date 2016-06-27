package de.prob2.ui.preferences;

import java.io.IOException;

import com.google.inject.Inject;
import de.prob.animator.domainobjects.ProBPreference;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.Trace;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;

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
		tvValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
		tvDefaultValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("defaultValue"));
		tvDescription.setCellValueFactory(new TreeItemPropertyValueFactory<>("description"));
		tv.getRoot().setValue(new SimplePrefTreeItem("Preferences"));
	}

	@Override
	public void traceChange(Trace currentTrace, boolean currentAnimationChanged) {
		this.trace = currentTrace;
		this.preferences = new Preferences(this.trace.getStateSpace());

		for (ProBPreference pref : this.preferences.getPreferences()) {
			TreeItem<PrefTreeItem> category = null;
			for (TreeItem<PrefTreeItem> ti : this.tv.getRoot().getChildren()) {
				if (ti.getValue().getName().equals(pref.category)) {
					category = ti;
				}
			}
			if (category == null) {
				category = new TreeItem<>(new SimplePrefTreeItem(pref.category));
				this.tv.getRoot().getChildren().add(category);
			}

			TreeItem<PrefTreeItem> item = null;
			for (TreeItem<PrefTreeItem> ti : category.getChildren()) {
				if (ti.getValue().getName().equals(pref.name)) {
					item = ti;
				}
			}
			if (item == null) {
				item = new TreeItem<>(new SimplePrefTreeItem(pref.name, "", pref.defaultValue, pref.description));
				category.getChildren().add(item);
			}
			item.getValue().updateValue(this.preferences);
		}
	}

	@Override
	public void animatorStatus(boolean busy) {}
}
