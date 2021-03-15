package de.prob2.ui.visb.visbobjects;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBItem;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * The VisBVisualisation Object contains the functions needed to store all the visualisation information.
 */
public class VisBVisualisation {
	private Path svgPath;
	private File jsonFile;
	private List<VisBEvent> visBEvents;
	private List<VisBItem> visBItems;

	public VisBVisualisation(){
		this.visBEvents = null;
		this.visBItems = null;
		this.svgPath = null;
		this.jsonFile = null;
	}

	public VisBVisualisation(List<VisBEvent> visBEvents, List<VisBItem> visBItems, Path svgPath, File jFile) {
		this.visBEvents = visBEvents;
		this.visBItems = visBItems;
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
		return svgPath != null;
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Visualisation Events List:\n");
		appendListWithNull(stringBuilder, visBEvents);
		stringBuilder.append("Visualisation Item List:\n");
		appendListWithNull(stringBuilder, visBItems);
		stringBuilder.append("SVG: \n");
		appendObjectWithNull(stringBuilder, svgPath);
		stringBuilder.append("JSON: \n");
		appendObjectWithNull(stringBuilder, jsonFile);
		return stringBuilder.toString();
	}

	private void appendListWithNull(StringBuilder sb, List<?> list) {
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

	public void setVisBItems(List<VisBItem> visBItems) {
		this.visBItems = visBItems;
	}
}
