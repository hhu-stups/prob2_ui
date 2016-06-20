package de.prob2.ui.states;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.sun.javafx.collections.ObservableSetWrapper;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EnumerationWarning;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob.model.representation.Action;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
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
	private Map<IEvalElement, AbstractEvalResult> currentValues;
	private Map<IEvalElement, AbstractEvalResult> previousValues;
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
		if (res == null) {
			return "null";
		} else if (res instanceof IdentifierNotInitialised) {
			return "(not initialized)";
		} else if (res instanceof EvalResult) {
			return ((EvalResult) res).getValue();
		} else if (res instanceof EvaluationErrorResult) {
			return ((EvaluationErrorResult) res).getResult();
		} else if (res instanceof EnumerationWarning) {
			return "?(∞)";
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

	private void unsubscribeAllChildren(final AbstractElement element) {
		if (element instanceof AbstractFormulaElement) {
			((AbstractFormulaElement)element).unsubscribe(this.trace.getStateSpace());
		}

		for (final Class<? extends AbstractElement> clazz : element.getChildren().keySet()) {
			this.unsubscribeAllChildren(element.getChildren().get(clazz));
		}
	}

	private void unsubscribeAllChildren(final List<? extends AbstractElement> elements) {
		for (AbstractElement e : elements) {
			this.unsubscribeAllChildren(e);
		}
	}

	private void updateElements(
		final Trace trace,
		final TreeItem<StateTreeItem<?>> treeItem,
		final List<? extends AbstractElement> elements
	) {
		for (AbstractElement e : elements) {
			if (e instanceof AbstractFormulaElement) {
				((AbstractFormulaElement)e).subscribe(this.trace.getStateSpace());
			}

			TreeItem<StateTreeItem<?>> childItem = null;

			for (TreeItem<StateTreeItem<?>> ti : treeItem.getChildren()) {
				StateTreeItem<?> sti = ti.getValue();
				if (sti.getContents().equals(e)) {
					childItem = ti;
					childItem.getValue().update(this.currentValues, this.previousValues);
					break;
				}
			}

			if (childItem == null) {
				childItem = new TreeItem<>(new ElementStateTreeItem(e, this.currentValues, this.previousValues));
				treeItem.getChildren().add(childItem);
			}

			this.updateChildren(trace, childItem, e);
		}

		Iterator<TreeItem<StateTreeItem<?>>> it = treeItem.getChildren().iterator();
		while (it.hasNext()) {
			TreeItem<StateTreeItem<?>> ti = it.next();
			StateTreeItem<?> sti = ti.getValue();
			if (sti instanceof ElementStateTreeItem) {
				AbstractElement element = ((ElementStateTreeItem) sti).getContents();
				if (!elements.contains(element)) {
					it.remove();
					if (element instanceof AbstractFormulaElement) {
						((AbstractFormulaElement)element).unsubscribe(this.trace.getStateSpace());
					}
				}
			} else {
				it.remove();
			}
		}

		treeItem.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
	}

	private void updateChildren(
		final Trace trace,
		final TreeItem<StateTreeItem<?>> treeItem,
		final AbstractElement element
	) {
		this.knownAbstractElementSubclasses.addAll(element.getChildren().keySet());
		for (Class<? extends AbstractElement> clazz : element.getChildren().keySet()) {
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

			this.updateElements(trace, childItem, element.getChildren().get(clazz));
		}

		Iterator<TreeItem<StateTreeItem<?>>> it = treeItem.getChildren().iterator();
		while (it.hasNext()) {
			TreeItem<StateTreeItem<?>> ti = it.next();
			StateTreeItem<?> sti = ti.getValue();
			if (sti instanceof ElementClassStateTreeItem) {
				Class<? extends AbstractElement> clazz = ((ElementClassStateTreeItem) sti).getContents();
				if (!element.getChildren().containsKey(clazz) || this.childrenClassBlacklist.contains(clazz)) {
					this.unsubscribeAllChildren(element.getChildren().get(clazz));
					it.remove();
				}
			} else {
				it.remove();
			}
		}

		treeItem.getChildren().sort((a, b) -> a.getValue().compareTo(b.getValue()));
	}

	private void updateModel(final Trace trace, final TreeItem<StateTreeItem<?>> root) {
		if (trace == null) {
			return;
		}

		this.updateChildren(trace, root, trace.getModel());
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		this.animations.registerAnimationChangeListener(this);

		this.currentValues = null;
		this.previousValues = null;
		
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

		this.childrenClassBlacklist.addListener(
			(SetChangeListener.Change<? extends Class<? extends AbstractElement>> change) -> {
				this.updateModel(this.trace, this.tvChildrenItem);
			}
		);
		this.knownAbstractElementSubclasses.addListener(
			(SetChangeListener.Change<? extends Class<? extends AbstractElement>> change) -> {
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
			}
		);

		this.knownAbstractElementSubclasses.add(Action.class);
		this.childrenClassBlacklist.add(Action.class);

		this.editBlacklistButton.setOnAction(event -> {
			this.editBlacklistStage.show();
			// Cleanup code to run when the window is closed should not go here,
			// otherwise it will not run properly when exceptions occur.
			// Put it below in the close request event handler instead.
		});
	}

	@Override
	public void traceChange(Trace trace, boolean currentAnimationChanged) {
		try {
			this.trace = trace;
			this.currentValues = this.trace.getCurrentState().getValues();
			this.previousValues = this.trace.canGoBack() ? this.trace.getPreviousState().getValues() : null;
			this.updateModel(this.trace, this.tvChildrenItem);
		} catch (Exception e) {
			// Otherwise the exception gets lost somewhere deep in a
			// ProB log file, without a traceback.
			e.printStackTrace();
		}
	}

	@Override
	public void animatorStatus(boolean busy) {
		// TODO Auto-generated method stub
	}
}
