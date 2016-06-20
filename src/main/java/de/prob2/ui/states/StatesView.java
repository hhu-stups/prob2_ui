package de.prob2.ui.states;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import com.google.inject.Inject;
import com.sun.javafx.collections.ObservableSetWrapper;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.Action;
import de.prob.model.representation.ModelElementList;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Animations;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.ITraceChangesListener;
import de.prob.statespace.Trace;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class StatesView extends AnchorPane implements Initializable, IAnimationChangeListener {
	private @FXML TreeTableColumn<StateTreeItem<?>, String> tvName;
	private @FXML TreeTableColumn<StateTreeItem<?>, String> tvValue;
	private @FXML TreeTableColumn<StateTreeItem<?>, String> tvPreviousValue;
	private @FXML TreeItem<StateTreeItem<?>> tvChildrenItem;
	private @FXML Button editBlacklistButton;
	
	private Stage editBlacklistStage;
	private BlacklistView editBlacklistStageController;

	private Trace trace;
	private ObservableSet<Class<? extends AbstractElement>> childrenClassBlacklist;
	private ObservableSet<Class<? extends AbstractElement>> knownAbstractElementSubclasses;
	private AnimationSelector animations;

	@Inject
	public StatesView(FXMLLoader loader, AnimationSelector animations) {
		this.animations = animations;
		animations.registerAnimationChangeListener(this);

		try {
			loader.setLocation(getClass().getResource("states_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static String stringRep(final AbstractEvalResult res) {
		if (res instanceof IdentifierNotInitialised) {
			return "";
		} else if (res instanceof EvalResult) {
			return ((EvalResult) res).getValue();
		} else if (res instanceof EvaluationErrorResult) {
			return ((EvaluationErrorResult) res).getResult();
		} else if (res instanceof EnumerationWarning) {
			return "?(\u221E)";
		} else {
			return res.getClass() + " toString: " + res;
		}
	}

	static String formatClassName(Class<?> clazz, boolean plural) {
		String shortName = clazz.getSimpleName();
		if (plural) {
			if (shortName.endsWith("y")) {
				shortName = shortName.substring(0, shortName.length() - 1) + "ies";
			} else {
				shortName += "s";
			}
		}
		return shortName;
	}

	static String formatClassName(Class<?> clazz) {
		return formatClassName(clazz, false);
	}

	private void updateElements(final Trace trace, final TreeItem<StateTreeItem<?>> treeItem,
			final List<? extends AbstractElement> elements) {
		for (AbstractElement e : elements) {
			TreeItem<StateTreeItem<?>> childItem = null;

			for (TreeItem<StateTreeItem<?>> ti : treeItem.getChildren()) {
				StateTreeItem<?> sti = ti.getValue();
				if (sti.getContents().equals(e)) {
					childItem = ti;
					childItem.getValue().update(trace);
					break;
				}
			}

			if (childItem == null) {
				childItem = new TreeItem<>(new ElementStateTreeItem(trace, e));
				treeItem.getChildren().add(childItem);
			}

			this.updateChildren(trace, childItem, e);
		}

		Iterator<TreeItem<StateTreeItem<?>>> it = treeItem.getChildren().iterator();
		while (it.hasNext()) {
			TreeItem<StateTreeItem<?>> ti = it.next();
			StateTreeItem<?> sti = ti.getValue();
			if (!(sti instanceof ElementStateTreeItem)
					|| !elements.contains(((ElementStateTreeItem) sti).getContents())) {
				it.remove();
			}
		}

		treeItem.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
	}

	private void updateChildren(final Trace trace, final TreeItem<StateTreeItem<?>> treeItem,
			final AbstractElement element) {
		Map<Class<? extends AbstractElement>, ModelElementList<? extends AbstractElement>> children = element
				.getChildren();
		this.knownAbstractElementSubclasses.addAll(children.keySet());
		for (Class<? extends AbstractElement> clazz : children.keySet()) {
			System.out.println(clazz);
			if (this.childrenClassBlacklist.contains(clazz)) {
				continue;
			}

			TreeItem<StateTreeItem<?>> childItem = null;
			for (TreeItem<StateTreeItem<?>> ti : treeItem.getChildren()) {
				StateTreeItem<?> sti = ti.getValue();
				if (sti.getContents().equals(clazz)) {
					childItem = ti;
					break;
				}
			}

			if (childItem == null) {
				childItem = new TreeItem<>(new ElementClassStateTreeItem(clazz));
				treeItem.getChildren().add(childItem);
			}

			this.updateElements(trace, childItem, children.get(clazz));
		}

		Iterator<TreeItem<StateTreeItem<?>>> it = treeItem.getChildren().iterator();
		while (it.hasNext()) {
			TreeItem<StateTreeItem<?>> ti = it.next();
			StateTreeItem<?> sti = ti.getValue();
			if (!(sti instanceof ElementClassStateTreeItem)
					|| !children.containsKey(((ElementClassStateTreeItem) sti).getContents())
					|| this.childrenClassBlacklist.contains(((ElementClassStateTreeItem) sti).getContents())) {
				it.remove();
			}
		}

		treeItem.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
	}

	private void updateModel(final Trace trace, final TreeItem<StateTreeItem<?>> root) {
		AbstractModel currentModel;
		if (trace == null) {
			return;
		}
		try {
			currentModel = trace.getModel();
		} catch (NullPointerException e) {
			return;
		}

		this.updateChildren(trace, root, currentModel);
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		animations.registerAnimationChangeListener(this);

		this.tvName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		this.tvValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("value"));
		this.tvPreviousValue.setCellValueFactory(new TreeItemPropertyValueFactory<>("previousValue"));

		this.tvChildrenItem.setValue(new SimpleStateTreeItem("Children"));

		FXMLLoader editBlacklistStageLoader = new FXMLLoader(this.getClass().getResource("blacklist_view.fxml"));
		try {
			this.editBlacklistStage = editBlacklistStageLoader.load();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		this.editBlacklistStageController = editBlacklistStageLoader.getController();

		this.childrenClassBlacklist = this.editBlacklistStageController.childrenClassBlacklist;
		this.knownAbstractElementSubclasses = new ObservableSetWrapper<>(new HashSet<>());

		this.childrenClassBlacklist
				.addListener((SetChangeListener.Change<? extends Class<? extends AbstractElement>> change) -> {
					this.updateModel(this.trace, this.tvChildrenItem);
				});
		this.knownAbstractElementSubclasses
				.addListener((SetChangeListener.Change<? extends Class<? extends AbstractElement>> change) -> {
					List<Class<? extends AbstractElement>> l = this.editBlacklistStageController.list.getItems();
					Class<? extends AbstractElement> added = change.getElementAdded();
					Class<? extends AbstractElement> removed = change.getElementRemoved();

					if (change.wasAdded() && !l.contains(added)) {
						l.add(added);
						l.sort((a, b) -> a.getCanonicalName().compareTo(b.getCanonicalName()));
					} else if (change.wasRemoved() && l.contains(removed)) {
						if (this.childrenClassBlacklist.contains(removed)) {
							this.childrenClassBlacklist.remove(removed);
						}
						l.remove(removed);
					}
				});

		this.knownAbstractElementSubclasses.add(Action.class);
		this.childrenClassBlacklist.add(Action.class);

		this.editBlacklistButton.setOnAction(event -> {
			this.editBlacklistStage.show();
			// Cleanup code to run when the window is closed should not go here
			// - otherwise it will not run properly when exceptions occur. Put
			// it below in the close request event handler instead.
		});
	}

	@Override
	public void traceChange(Trace trace, boolean currentAnimationChanged) {
		try {
			this.trace = trace;
			this.updateModel(this.trace, this.tvChildrenItem);
		} catch (Exception e) {
			// Otherwise the exception gets lost somewhere deep in a ProB log
			// file, without a traceback
			e.printStackTrace();
		}
	}

	@Override
	public void animatorStatus(boolean busy) {
		// TODO Auto-generated method stub

	}
}
