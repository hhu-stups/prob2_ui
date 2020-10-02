package de.prob2.ui.visb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

import de.prob2.ui.visb.exceptions.VisBParseException;
import de.prob2.ui.visb.visbobjects.VisBEvent;
import de.prob2.ui.visb.visbobjects.VisBItem;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;

/**
 * The VisBFileHandler handles everything, that needs to be done with files for the {@link VisBController}.
 */
class VisBFileHandler {

	/**
	 * This method takes a JSON / VisB file as input and returns a {@link VisBVisualisation} object.
	 * @param inputFile File class object
	 * @return VisBVisualisation object
	 * @throws IOException If the file cannot be found, does not exist or is otherwise not accessible.
	 * @throws VisBParseException If the file does not have the VisB format.
	 */
	static VisBVisualisation constructVisualisationFromJSON(File inputFile) throws IOException, VisBParseException {
		Gson gson = new Gson();
		JsonReader reader = new JsonReader(new FileReader(inputFile));
		JsonObject visBFile = gson.fromJson(reader, JsonObject.class);
		Path svgPath;
		if(visBFile.has("svg")){
			String filePath = visBFile.get("svg").getAsString();
			if(filePath == null ||filePath.isEmpty()){
				throw new VisBParseException("There was no path to an SVG file found in your VisB file. Make sure, that you include one under the id \"svg\".");
			} else {
				svgPath = Paths.get(filePath);
				if (!svgPath.isAbsolute()) {
					svgPath = Paths.get(inputFile.getParentFile().toString(), filePath);
				}
			}
		} else{
			throw new VisBParseException("There was no path to an SVG file found in your VisB file. Make sure, that you include one under the id \"svg\".");
		}
		ArrayList<VisBItem> visBItems = new ArrayList<>();
		if(visBFile.has("items")) {
			JsonArray visArray = (JsonArray) visBFile.get("items");
			visBItems = assembleVisList(visArray);
		}
		ArrayList<VisBEvent> visBEvents = new ArrayList<>();
		if(visBFile.has("events")) {
			JsonArray eventsArray = (JsonArray) visBFile.get("events");
			 visBEvents = assembleEventList(eventsArray);
		}
		if((visBItems.isEmpty() && visBEvents.isEmpty()) || svgPath == null){
			return null;
		} else {
			return new VisBVisualisation(visBItems, visBEvents, svgPath);
		}
	}

	/**
	 * This method assembles the events into a {@link ArrayList}. There is only one event possible for on click events for SVGs.
	 * @param array {@link JsonArray} containing possible events
	 * @return {@link ArrayList}, where the key is the String id and the {@link VisBEvent} is the value
	 * @throws VisBParseException If the format of the file is not right.
	 */
	private static ArrayList<VisBEvent> assembleEventList(JsonArray array) throws VisBParseException{
		ArrayList<VisBEvent> visBEvents = new ArrayList<>();
		if(array == null || array.isJsonNull() || array.size() == 0) return null;
		for (Object event : array) {
			JsonObject current_obj = (JsonObject) event;
			if(current_obj.isJsonNull()){
				continue;
			}
			if(current_obj.has("id") && current_obj.has("event")) {
				String id = current_obj.get("id").getAsString();
				String eventS = current_obj.get("event").getAsString();
				ArrayList<String> predicates = new ArrayList<>();
				if(current_obj.has("predicates")){
					JsonArray jsonPredicates = current_obj.getAsJsonArray("predicates");
					for(int i = 0; i < jsonPredicates.size();i++){
						predicates.add(jsonPredicates.get(i).getAsString());
					}
				}
				if(id.isEmpty() || eventS.isEmpty()){
					throw new VisBParseException("There is an event in your visualisation file that has an empty id, event, or predicates body.");
				}
				VisBEvent visBEvent = new VisBEvent(id, eventS, predicates);
				boolean add = !containsId(visBEvents, id);
				if(add) {
					visBEvents.add(visBEvent);
				} else {
					throw new VisBParseException("This id has already an event: " +id+".");
				}
			} else if (!current_obj.has("id")){
				throw new VisBParseException("There is a event in your visualisation file, that has no \"id\" member.");
			} else if (!current_obj.has("event")){
				throw new VisBParseException("There is a event in your visualisation file, that has no \"event\" member.");
			}
		}
		return visBEvents;
	}

	private static boolean containsId(ArrayList<VisBEvent> visBEvents, String id){
		for(VisBEvent visBEvent : visBEvents){
			if(id.equals(visBEvent.getId())){
				return true;
			}
		}
		return false;
	}

	/**
	 * This method assembles the visualisation items into an {@link ArrayList}. There are multiple possible attribute changes for an SVG element after a state changed.
	 * @param array {@link JsonArray} containing possible items
	 * @return {@link ArrayList}, which contains {@link VisBItem} as items.
	 * @throws VisBParseException If the format of the file is not right.
	 */
	private static ArrayList<VisBItem> assembleVisList(JsonArray array) throws VisBParseException{
		ArrayList<VisBItem> visBItems = new ArrayList<>();
		if(array == null || array.isJsonNull() || array.size() == 0) return null;
		for (Object item : array) {
			JsonObject current_obj = (JsonObject) item;
			if(current_obj.isJsonNull()){
				continue;
			}
			if(current_obj.has("id") && current_obj.has("attr") && current_obj.has("value")) {
				String id = current_obj.get("id").getAsString();
				String attribute = current_obj.get("attr").getAsString();
				String value = current_obj.get("value").getAsString();
				if(id.isEmpty() || attribute.isEmpty() || value.isEmpty()){
					throw new VisBParseException("There is an item in your visualisation file, that has an empty id, attr, or value body.");
				}
				if (current_obj.has("ignore")) {
				   System.out.println("Ignoring VisB Item: " + id + "." + attribute);
				} else if (current_obj.has("repeat")) {
				   // a list of strings which will replace %this in the three other attributes:
				   JsonArray repArray = (JsonArray) current_obj.get("repeat");
				   for(JsonElement rep : repArray) {
				      String thisVal = rep.getAsString();
				      System.out.println("Repeating item " + id + "." + attribute + " for %this = " + thisVal);
				      
				      String repId = new String(id).replace("%this", thisVal);
				      // no need to replace in attribute
				      String repVal = new String(value).replace("%this", thisVal);
				      visBItems.add(new VisBItem(repId, attribute, repVal));
				   }
				} else {
				   visBItems.add(new VisBItem(id, attribute, value));
				}
			} else if (!current_obj.has("id")){
				throw new VisBParseException("There is a item in your visualisation file, that has no \"id\" member.");
			} else if (!current_obj.has("attr")){
				throw new VisBParseException("There is a item in your visualisation file, that has no \"attr\" member.");
			} else if (!current_obj.has("value")){
				throw new VisBParseException("There is a item in your visualisation file, that has no \"value\" member.");
			}
		}
		return visBItems;
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
