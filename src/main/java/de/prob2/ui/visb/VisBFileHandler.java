package de.prob2.ui.visb;

import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.command.LoadVisBCommand;
import de.prob.animator.command.LoadVisBSetAttributesCommand;
import de.prob.animator.command.ReadVisBEventsHoversCommand;
import de.prob.animator.command.ReadVisBItemsCommand;
import de.prob.animator.command.ReadVisBSvgPathCommand;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.exceptions.VisBParseException;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
	 * @param inputFile File class object
	 * @return VisBVisualisation object
	 * @throws IOException If the file cannot be found, does not exist or is otherwise not accessible.
	 * @throws VisBParseException If the file does not have the VisB format.
	 */
	public VisBVisualisation constructVisualisationFromJSON(File inputFile) throws IOException, VisBParseException, JsonSyntaxException {
		LoadVisBCommand loadCmd = new LoadVisBCommand(inputFile.getPath());

		currentTrace.getStateSpace().execute(loadCmd);
		ReadVisBSvgPathCommand svgCmd = new ReadVisBSvgPathCommand(inputFile.getPath());

		currentTrace.getStateSpace().execute(svgCmd);
		String parentFile = inputFile.getParentFile().toString();

		String filePath = svgCmd.getSvgPath();
		Path svgPath = Paths.get(filePath);
		if (!svgPath.isAbsolute()) {
			svgPath = Paths.get(parentFile, filePath);
		}

		ReadVisBEventsHoversCommand readEventsCmd = new ReadVisBEventsHoversCommand();
		currentTrace.getStateSpace().execute(readEventsCmd);
		List<VisBEvent> visBEvents = readEventsCmd.getEvents();

		List<VisBItem> items = loadItems();

		return new VisBVisualisation(visBEvents, items, svgPath, inputFile);
	}

	public List<VisBItem> loadItems() {
		ReadVisBItemsCommand readVisBItemsCommand = new ReadVisBItemsCommand();
		currentTrace.getStateSpace().execute(readVisBItemsCommand);

		return readVisBItemsCommand.getItems();
	}

	/**
	 * This is another help-method for {@link VisBController}. It takes a file and gives back the lines in string format.
	 * @param file {@link File}
	 * @return String representation of the file
	 * @throws IOException If the file cannot be found, does not exist or is otherwise not accessible.
	 */
	String fileToString(File file) throws IOException{
		StringBuilder sb = new StringBuilder();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String s = bufferedReader.readLine();
		while (s != null) {
			sb.append(s);
			s = bufferedReader.readLine();
		}
		return sb.toString();
	}
}
