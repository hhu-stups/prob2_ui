package de.prob2.ui.animation.tracereplay;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

@FXMLInjected
public final class TraceReplayMenuButton extends MenuButton {
	private final I18n i18n;
	private final CurrentProject currentProject;
	private final CliTaskExecutor cliExecutor;
	private final TraceChecker traceChecker;
	
	private final ListProperty<ReplayTrace> traces;
	
	@Inject
	private TraceReplayMenuButton(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject, final CliTaskExecutor cliExecutor, final TraceChecker traceChecker) {
		this.i18n = i18n;
		this.currentProject = currentProject;
		this.cliExecutor = cliExecutor;
		this.traceChecker = traceChecker;
		
		this.traces = new SimpleListProperty<>(this, "traces", FXCollections.emptyObservableList());
		
		stageManager.loadFXML(this, "trace_replay_menu_button.fxml");
	}
	
	@FXML
	private void initialize() {
		final ChangeListener<Machine> machineChangeListener = (o, from, to) -> {
			this.traces.unbind();
			if (to == null) {
				this.traces.set(FXCollections.emptyObservableList());
			} else {
				this.traces.set(to.getMachineProperties().getTraces());
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		
		this.disableProperty().bind(currentProject.currentMachineProperty().isNull());
		this.traces.addListener((ListChangeListener<ReplayTrace>)change -> {
			if (change.getList().isEmpty()) {
				final MenuItem placeholderMenuItem = new MenuItem(i18n.translate("animation.tracereplay.view.placeholder"));
				placeholderMenuItem.setDisable(true);
				this.getItems().setAll(placeholderMenuItem);
			} else {
				final List<MenuItem> menuItems = new ArrayList<>();
				for (final ReplayTrace trace : change.getList()) {
					final MenuItem menuItem = new MenuItem(trace.getName());
					menuItem.setMnemonicParsing(false);
					menuItem.setOnAction(e -> cliExecutor.execute(() -> traceChecker.check(trace)));
					menuItems.add(menuItem);
				}
				this.getItems().setAll(menuItems);
			}
		});
		
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
	}
}
