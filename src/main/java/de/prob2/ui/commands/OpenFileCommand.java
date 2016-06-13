package de.prob2.ui.commands;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.scripting.Api;
import de.prob.statespace.Animations;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.events.OpenFileEvent;
import de.prob2.ui.modeline.ModelineExtension;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;

public class OpenFileCommand implements Command {

	private Api api;
	private Animations animations;
	private EventBus bus;

	@Inject
	public OpenFileCommand(EventBus bus, Api api, Animations animations) {
		this.bus = bus;
		this.api = api;
		this.animations = animations;
		bus.register(this);
	}

	@Subscribe
	public void openFileDialog(OpenFileEvent fileEvent) {
		String extension = fileEvent.getNormalizedExtension();
		switch (extension) {
		case "Classical B Files":
			try {
				StateSpace space = api.b_load(fileEvent.getFileName());
				Trace t = new Trace(space);
				animations.addNewAnimation(t);
				System.out.println("Loaded");
			} catch (IOException | BException e) {
				bus.post(e);
			}
			break;

		default:
			break;
		}
	}
	
	@Override
	public Collection<ModelineExtension> getModelineContribution() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void populateMenuBar(MenuBar menuBar, Map<String, Menu> menus) {
		// TODO Auto-generated method stub

	}

}
