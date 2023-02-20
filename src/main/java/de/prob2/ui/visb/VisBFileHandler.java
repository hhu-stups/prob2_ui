package de.prob2.ui.visb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
	public VisBVisualisation constructVisualisationFromJSON(Path jsonPath) throws IOException {
		jsonPath = jsonPath.toRealPath();
		if (!Files.isRegularFile(jsonPath) && !jsonPath.toFile().isDirectory()) {
			throw new IOException("Given json path is not a regular file: " + jsonPath);
		}

		LoadVisBCommand loadCmd = new LoadVisBCommand(jsonPath.toFile().isDirectory() ? "" : jsonPath.toString());

		currentTrace.getStateSpace().execute(loadCmd);
		ReadVisBSvgPathCommand svgCmd = new ReadVisBSvgPathCommand(jsonPath.toFile().isDirectory() ? "" : jsonPath.toString());

		currentTrace.getStateSpace().execute(svgCmd);
		String svgPathString = svgCmd.getSvgPath();

		Path svgPath = jsonPath.resolveSibling(svgPathString).toRealPath();
		if (!svgPathString.isEmpty() && (!Files.isRegularFile(svgPath) || Files.size(svgPath) <= 0)) {
			throw new IOException("Given svg path is not a non-empty regular file: " + svgPath);
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
