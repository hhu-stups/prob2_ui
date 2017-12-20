package de.prob2.ui.helpsystem;

import java.io.File;
import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.Main;

import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;

import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.project.ProjectTab;
import de.prob2.ui.project.machines.MachinesTab;
import de.prob2.ui.project.preferences.PreferencesTab;
import de.prob2.ui.states.StatesView;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.ltl.LTLView;
import de.prob2.ui.verifications.modelchecking.ModelcheckingController;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingView;
import de.prob2.ui.verifications.tracereplay.TraceReplayView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class HelpButton extends Button{
	private Injector injector;
	private File helpContent;
	private HashMap<Class, String> germanHelpMap = new HashMap<>();
	private HashMap<Class, String> englishHelpMap = new HashMap<>();

	@Inject
	private HelpButton(StageManager stageManager, Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "helpbutton.fxml");
	}

	@FXML
	private void initialize() {
		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (this.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
		germanHelpMap.put(HistoryView.class, "Verlauf.md.html");
		germanHelpMap.put(OperationsView.class, "ProB2UI.md.html");
		germanHelpMap.put(ProjectTab.class, "Projekt.md.html");
		germanHelpMap.put(MachinesTab.class, "Projekt.md.html");
		germanHelpMap.put(PreferencesTab.class, "Projekt.md.html");
		germanHelpMap.put(StatsView.class, "Statistik.md.html");
		germanHelpMap.put(LTLView.class, "Überprüfungen.md.html");
		germanHelpMap.put(ModelcheckingController.class, "Überprüfungen.md.html");
		germanHelpMap.put(SymbolicCheckingView.class, "Überprüfungen.md.html");
		germanHelpMap.put(TraceReplayView.class, "Überprüfungen.md.html");
		germanHelpMap.put(StatesView.class, "Hauptansicht" + File.separator + "Zustandsansicht.md.html");

		englishHelpMap.put(HistoryView.class, "History.md.html");
		englishHelpMap.put(OperationsView.class, "ProB2UI.md.html");
		englishHelpMap.put(ProjectTab.class, "Project.md.html");
		englishHelpMap.put(MachinesTab.class, "Project.md.html");
		englishHelpMap.put(PreferencesTab.class, "Project.md.html");
		englishHelpMap.put(StatsView.class, "Statistics.md.html");
		englishHelpMap.put(LTLView.class, "Verification.md.html");
		englishHelpMap.put(ModelcheckingController.class, "Verification.md.html");
		englishHelpMap.put(SymbolicCheckingView.class, "Verification.md.html");
		englishHelpMap.put(TraceReplayView.class, "Verification.md.html");
		englishHelpMap.put(StatesView.class, "Main View" + File.separator + "State.md.html");

	}

	@FXML
	public void openHelp() {
		final HelpSystemStage helpSystemStage = injector.getInstance(HelpSystemStage.class);
		if (helpContent!=null) {
			helpSystemStage.setContent(helpContent);
		}
		helpSystemStage.show();
		helpSystemStage.toFront();
	}

	public void setHelpContent(Class clazz) {
		switch (injector.getInstance(HelpSystemStage.class).help.helpSubdirectoryString) {
			case "help_de":
				setGermanHelp(clazz);
				break;
			case "help_en":
			default:
				setEnglishHelp(clazz);
				break;
		}
		//helpContent = new File(Main.getProBDirectory() + "prob2ui" + File.separator + "help" + File.separator + fileName);
	}

	private void setGermanHelp(Class clazz) {
		setHelp(clazz,
				Main.getProBDirectory() +
						"prob2ui" + File.separator +
						"help" + File.separator +
						"help_de" + File.separator,
				germanHelpMap);
	}

	private void setEnglishHelp(Class clazz) {
		setHelp(clazz,
				Main.getProBDirectory() +
						"prob2ui" + File.separator +
						"help" + File.separator +
						"help_en" + File.separator,
				englishHelpMap);
	}

	private void setHelp(Class clazz, String main, HashMap<Class, String> map) {
		helpContent = new File(main + "ProB2UI.md.html");
		for (Class c : map.keySet()) {
			if (clazz.equals(c)) {
				helpContent = new File(main + map.get(c));
			}
		}
	}
}
