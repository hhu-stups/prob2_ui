package de.prob2.ui.visualisation.magiclayout;

import java.util.List;

public class MagicLayoutSettings {

	private String machineName;
	private List<MagicNodegroup> nodegroups;
	private List<MagicEdgegroup> edgegroups;

	public MagicLayoutSettings(String machineName, List<MagicNodegroup> nodegroups, List<MagicEdgegroup> edgegroups) {
		this.machineName = machineName;
		this.nodegroups = nodegroups;
		this.edgegroups = edgegroups;
	}
}
