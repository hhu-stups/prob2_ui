package de.prob2.ui.visualisation.magiclayout;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.prob.json.JsonManager;

import java.lang.reflect.Type;
import java.util.List;

public class MagicLayoutSettings {
	public static final JsonDeserializer<MagicLayoutSettings> JSON_DESERIALIZER = MagicLayoutSettings::new;

	private String machineName;
	private List<MagicNodegroup> nodegroups;
	private List<MagicEdgegroup> edgegroups;

	public MagicLayoutSettings(String machineName, List<MagicNodegroup> nodegroups, List<MagicEdgegroup> edgegroups) {
		this.machineName = machineName;
		this.nodegroups = nodegroups;
		this.edgegroups = edgegroups;
	}
	
	private MagicLayoutSettings(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
		final JsonObject object = json.getAsJsonObject();
		this.machineName = JsonManager.checkDeserialize(context, object, "machineName", String.class);
		this.nodegroups = JsonManager.checkDeserialize(context, object, "nodegroups", new TypeToken<List<MagicNodegroup>>() {}.getType());
		this.edgegroups = JsonManager.checkDeserialize(context, object, "edgegroups", new TypeToken<List<MagicEdgegroup>>() {}.getType());
	}
	
	public String getMachineName() {
		return machineName;
	}
	
	public List<MagicNodegroup> getNodegroups() {
		return nodegroups;
	}
	
	public List<MagicEdgegroup> getEdgegroups() {
		return edgegroups;
	}
}
