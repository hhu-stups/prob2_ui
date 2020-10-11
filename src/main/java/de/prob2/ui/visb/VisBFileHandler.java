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
import com.google.gson.JsonPrimitive;
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
		} else {
		     visBItems = new ArrayList<>();
		}
		ArrayList<VisBEvent> visBEvents = new ArrayList<>();
		if(visBFile.has("events")) {
			 JsonArray eventsArray = (JsonArray) visBFile.get("events");
			 visBEvents = assembleEventList(eventsArray);
		} else {
		     visBEvents = new ArrayList<>();
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
		if(array == null || array.isJsonNull() || array.size() == 0) return visBEvents;
		for (Object event : array) {
			JsonObject current_obj = (JsonObject) event;
			if(current_obj.isJsonNull()){
				continue;
			}
			if(current_obj.has("id") && current_obj.has("event")) {
				String id = current_obj.get("id").getAsString();
				if(id.isEmpty()){
					throw new VisBParseException("An event in your visualisation file has an empty id.");
				}
				if (current_obj.has("ignore")) {
					System.out.println("Ignoring VisB Event for " + id);
				} else {
					String eventS = current_obj.get("event").getAsString();
					if(eventS.isEmpty()){
						throw new VisBParseException("The event for " + id + " in your visualisation file has an empty event body.");
					}
					ArrayList<String> predicates = new ArrayList<>();
					if(current_obj.has("predicates")){
						JsonArray jsonPredicates = current_obj.getAsJsonArray("predicates");
						for(int i = 0; i < jsonPredicates.size();i++){
							predicates.add(jsonPredicates.get(i).getAsString());
						}
					}
					String hoverAttr; String hoverEnter; String hoverLeave;
					if(current_obj.has("hover")){
					   JsonObject hv = current_obj.getAsJsonObject("hover");
					   hoverAttr = getAttrString(hv,"attr","hover within event "+ id);
					   hoverEnter = getAttrString(hv,"enter","hover within event "+id);
					   hoverLeave = getAttrString(hv,"leave","hover within event "+id);
					   System.out.println("Detected hover: " +id + " for "+ hoverAttr);
					} else {
					   hoverAttr = null; hoverEnter = null; hoverLeave = null;
					}
					
					JsonArray repArray = getRepeatArray(current_obj,"event "+id);
					if (repArray != null) {
						// a list of strings which will replace %0, ... 
						System.out.println("repeats = " + repArray);
						for(JsonElement rep : repArray) {
							// now replace %0, %1, ... by values provided
							String repId = new String(id);
							String repEvent = new String(eventS);
							ArrayList<String> repPreds= new ArrayList<String>();
							repPreds.addAll(predicates);
							
							JsonArray replaceArr = getJsonArray(rep);
							for(int i =0; i<replaceArr.size();i++) {
								String thisVal = replaceArr.get(i).getAsString();
								String pattern = new String("%"+i);
								System.out.println("Repeating event " + id + " for '" + pattern + "' = " + thisVal);
								repId = repId.replace(pattern, thisVal);
								repEvent = repEvent.replace(pattern, thisVal);
								for(int j = 0; j < repPreds.size(); j++) {
									repPreds.set(j,repPreds.get(j).replace(pattern, thisVal));
								}
								// we could check that all arrays have same size; otherwise a pattern will not be replaced
							}
							AddVisBEvent(visBEvents, repId, repEvent, repPreds, hoverAttr, hoverEnter, hoverLeave);
						}
					} else { // no repititions
						AddVisBEvent(visBEvents, id, eventS, predicates, hoverAttr, hoverEnter, hoverLeave);
					}
				}
			} else if (!current_obj.has("id")){
				throw new VisBParseException("There is a event in your visualisation file, that has no \"id\" attribute.");
			} else if (!current_obj.has("event")){
				String id = current_obj.get("id").getAsString();
				throw new VisBParseException("The event for " + id + " in your visualisation file has no \"event\" attribute.");
			}
		}
		return visBEvents;
	}
	
	// utility to get attribute and throw exception if it does not exist
	private static String getAttrString(JsonObject obj, String attr,String ctxt) throws VisBParseException {
	   JsonElement el = obj.get(attr);
	   if(el==null) {
	       throw new VisBParseException("Missing attribute "+attr+" for "+ctxt);
	   } else {
	     return el.getAsString();
	   }
	}
	
	private static void AddVisBEvent(ArrayList<VisBEvent> visBEvents, 
	                            String id, String eventS, ArrayList<String> predicates,
	                            String hover, String enter, String leave) throws VisBParseException {
		VisBEvent visBEvent = new VisBEvent(id, eventS, predicates, hover, enter, leave);
		if(!containsId(visBEvents, id)) {
			visBEvents.add(visBEvent);
		} else {
			throw new VisBParseException("This id has already an event: " +id+".");
		}
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
		if(array == null || array.isJsonNull() || array.size() == 0) return visBItems;
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
				} else {
					JsonArray repArray = getRepeatArray(current_obj,"VisB item "+id+ "." + attribute);
					if (repArray != null) {
						// a list of strings which will replace %0, ... in the id and value attributes:
						for(JsonElement rep : repArray) {
							// now replace %0, %1, ... by values provided
							String repId = new String(id); 
							// no need to replace in attribute
							String repVal = new String(value);
						
							JsonArray replaceArr = getJsonArray(rep);
							for(int i =0; i<replaceArr.size();i++) {
								String thisVal = replaceArr.get(i).getAsString();
								String pattern = new String("%"+i);
								System.out.println("Repeating item " + id + "." + attribute + " for '" + pattern + "' = " + thisVal);
								repId = repId.replace(pattern, thisVal);
								repVal = repVal.replace(pattern, thisVal);
								// we could check that all arrays have same size; otherwise a pattern will not be replaced
							}
							visBItems.add(new VisBItem(repId, attribute, repVal));
						}
					} else { // no repititions
						visBItems.add(new VisBItem(id, attribute, value));
					}
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
	
	// utility to get a JsonArray; a single Object is automatically transformed into a single item array
	private static JsonArray getJsonArray (JsonElement rep) {
		if(rep instanceof JsonArray) {
			return rep.getAsJsonArray();
		} else {
			JsonArray replaceArr = new JsonArray();
			replaceArr.add(rep); // create a one element array
			return replaceArr;
		}
	}
	
	// get a JsonArray of repetitions; either as a repeat list of explicit strings, or a for loop
	private static JsonArray getRepeatArray (JsonObject current_obj, String ctxt) throws VisBParseException {
	     if (current_obj.has("for")) {
	        JsonObject forloop = (JsonObject) current_obj.get("for"); // To do: check if JsonElement instanceof JsonObject
	        if (!(forloop.has("from") && forloop.has("to"))) {
			    throw new VisBParseException("A for loop requires both a from and to attribute within "+ctxt);
	        }
	        int from = forloop.get("from").getAsInt();
	        int to = forloop.get("to").getAsInt();
	        int step = 1;
	        if (forloop.has("step")) {
	           step = forloop.get("step").getAsInt();
	           if (step<1) {
			    throw new VisBParseException("The step " + step + " must be > 0 in for loop " + from + ".." + to + " within "+ctxt);
	           }
	        }
			if ((to-from)/step > 100000) {
			    throw new VisBParseException("The for loop " + from + ".." + to + " seems too large within "+ctxt);
			}
			JsonArray replaceArr = new JsonArray();
			for(int i=from; i <= to; i += step) {
				if (current_obj.has("repeat")) {
				    for(JsonElement rep : (JsonArray) current_obj.get("repeat") ) {
				       JsonArray arr_inner = new JsonArray();
				       arr_inner.add(new JsonPrimitive(i)); // add i at end of beginning of list
				       arr_inner.addAll(getJsonArray(rep)); // now copy original rep list
				       replaceArr.add(arr_inner); // add modified repetition list to overall list
				    }
				} else {
					replaceArr.add(new JsonPrimitive(i) );
			    }
			    System.out.println("repeat-for: " + i + " within "+ctxt);
			}
			return replaceArr;
	     }  else if (current_obj.has("repeat")) { // repeat without for
	        return (JsonArray) current_obj.get("repeat");
	     } else {
	        return null;
	     }
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
