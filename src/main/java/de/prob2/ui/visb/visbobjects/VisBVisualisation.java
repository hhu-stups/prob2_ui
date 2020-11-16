package de.prob2.ui.visb.visbobjects;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * The VisBVisualisation Object contains the functions needed to store all the visualisation information.
 */
public class VisBVisualisation {
	private Path svgPath;
	private File jsonFile;
	private List<VisBItem> visBItems;
	private List<VisBEvent> visBEvents;

	public VisBVisualisation(){
		this.visBItems = null;
		this.visBEvents = null;
		this.svgPath = null;
		this.jsonFile = null;
	}

	public VisBVisualisation(List<VisBItem> visBItems, List<VisBEvent> visBEvents, Path svgPath, File jFile) {
		this.visBItems = visBItems;
		this.visBEvents = visBEvents;
		this.svgPath = svgPath;
		this.jsonFile = jFile;
	}

	public List<VisBEvent> getVisBEvents() {
		return visBEvents;
	}

	public List<VisBItem> getVisBItems() {
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
		appendListWithNull(stringBuilder, visBItems);
		stringBuilder.append("Visualisation Events List:\n");
		appendListWithNull(stringBuilder, visBEvents);
		stringBuilder.append("SVG: \n");
		appendObjectWithNull(stringBuilder, svgPath);
		stringBuilder.append("JSON: \n");
		appendObjectWithNull(stringBuilder, jsonFile);
		return stringBuilder.toString();
	}

	private void appendListWithNull(StringBuilder sb, List list) {
		if(list != null) {
			for (Object obj : list) {
				sb.append(obj);
				sb.append("\n");
			}
		} else {
			sb.append("null");
			sb.append("\n");
		}
	}

	private void appendObjectWithNull(StringBuilder sb, Object obj) {
		if(obj != null) {
			sb.append(obj);
		} else {
			sb.append("null");
		}
		sb.append("\n");
	}
}
