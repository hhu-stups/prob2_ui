package de.prob2.ui.visb;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.GetVisBSVGObjectsCommand;
import de.prob.animator.command.LoadVisBCommand;
import de.prob.animator.command.ReadVisBEventsHoversCommand;
import de.prob.animator.command.ReadVisBItemsCommand;
import de.prob.animator.command.ReadVisBSvgPathCommand;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;
import de.prob.animator.domainobjects.VisBSVGObject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;

/**
 * The VisBFileHandler handles everything, that needs to be done with files for the {@link VisBController}.
 */

@Singleton
public class VisBFileHandler {

	private final CurrentTrace currentTrace;

	@Inject
	public VisBFileHandler(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
	}

	/**
	 * This method takes a JSON / VisB file as input and returns a {@link VisBVisualisation} object.
	 * @param jsonPath path to the VisB JSON file
	 * @return VisBVisualisation object
	 */
	public VisBVisualisation constructVisualisationFromJSON(final Path jsonPath) {
		LoadVisBCommand loadCmd = new LoadVisBCommand(jsonPath.toString());

		currentTrace.getStateSpace().execute(loadCmd);
		ReadVisBSvgPathCommand svgCmd = new ReadVisBSvgPathCommand(jsonPath.toString());

		currentTrace.getStateSpace().execute(svgCmd);
		String parentFile = jsonPath.getParent().toString();

		String filePath = svgCmd.getSvgPath();
		Path svgPath = Paths.get(filePath);
		if (!svgPath.isAbsolute()) {
			svgPath = Paths.get(parentFile, filePath);
		}

		ReadVisBEventsHoversCommand readEventsCmd = new ReadVisBEventsHoversCommand();
		currentTrace.getStateSpace().execute(readEventsCmd);
		List<VisBEvent> visBEvents = readEventsCmd.getEvents();
		List<VisBItem> items = loadItems();

		GetVisBSVGObjectsCommand command = new GetVisBSVGObjectsCommand();
		currentTrace.getStateSpace().execute(command);
		List<VisBSVGObject> visBSVGObjects = command.getSvgObjects();

		return new VisBVisualisation(visBEvents, items, svgPath, visBSVGObjects);
	}

	public List<VisBItem> loadItems() {
		ReadVisBItemsCommand readVisBItemsCommand = new ReadVisBItemsCommand();
		currentTrace.getStateSpace().execute(readVisBItemsCommand);

		return readVisBItemsCommand.getItems();
	}
}
