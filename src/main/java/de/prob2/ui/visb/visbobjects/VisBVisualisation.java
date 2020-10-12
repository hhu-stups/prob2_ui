package de.prob2.ui.visb.visbobjects;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * The VisBVisualisation Object contains the functions needed to store all the visualisation information.
 */
public class VisBVisualisation {
	private Path svgPath;
	private File jsonFile;
	private ArrayList<VisBItem> visBItems;
	private ArrayList<VisBEvent> visBEvents;

	public VisBVisualisation(){
		this.visBItems = null;
		this.visBEvents = null;
		this.svgPath = null;
		this.jsonFile = null;
	}

	public VisBVisualisation(ArrayList<VisBItem> visBItems, ArrayList<VisBEvent> visBEvents, Path svgPath, File jFile) {
		this.visBItems = visBItems;
		this.visBEvents = visBEvents;
		this.svgPath = svgPath;
		this.jsonFile = jFile;
	}

	public ArrayList<VisBEvent> getVisBEvents() {
		return visBEvents;
	}

	public ArrayList<VisBItem> getVisBItems() {
		return visBItems;
	}

	public Path getSvgPath() {
		return svgPath;
	}

	public File getJsonFile() {
		return jsonFile;
	}

	public VisBEvent getEventForID(String id){
		if(visBEvents != null && !visBEvents.isEmpty()){
			for(VisBEvent visBEvent : visBEvents){
				if(id.equals(visBEvent.getId())){
					return visBEvent;
				}
			}
		}
		return null;
	}

	public boolean isReady(){
		return ((visBEvents != null && !visBEvents.isEmpty()) || (visBItems != null && !visBItems.isEmpty())) && svgPath != null;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Visualisation Items List:\n");
		for (VisBItem visBItem : visBItems){
			stringBuilder.append(visBItem.toString());
			stringBuilder.append("\n");
		}
		stringBuilder.append("Visualisation Events List:\n");
		for (VisBEvent visBEvent : visBEvents){
			stringBuilder.append(visBEvent.toString());
			stringBuilder.append("\n");
		}
		stringBuilder.append("SVG: \n");
		stringBuilder.append(svgPath.toString());
		stringBuilder.append("\n");
		return stringBuilder.toString();
	}
}
