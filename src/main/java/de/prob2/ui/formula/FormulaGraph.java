package de.prob2.ui.formula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FormulaGraph {
	
	private FormulaNode root;
	
	public FormulaGraph(final Map<String,String[]> params) {
		Set<String> set = params.keySet();
		Object[] keys = set.toArray();
		String rootvalue = (String) keys[0];
		//Collection<String[]> values = params.values();
		List<String[]> values = new ArrayList<>(params.values());
		List<FormulaNode> nodes = new ArrayList<FormulaNode>();
		for(int i = 0; i < values.size(); i++) {
			nodes.add(new FormulaNode(values.get(0)[i]));
		}
		root = new FormulaNode(100, 400, rootvalue, nodes);
	}
	
	

}
