package org.reactome.cytoscape.bn;

import java.util.Map;

import org.cytoscape.view.model.CyNetworkView;
import org.reactome.cytoscape.service.PathwayDiagramHighlighter;
import org.reactome.cytoscape.util.PlugInUtilities;

public class NetworkComparisonPane extends SimulationComparisonPane {
	private CyNetworkView networkView;
	
	public NetworkComparisonPane(String title) {
		super(title);
		hiliteDiagramBtn.setText("Highlight network");
		this.variablePropKey = "name";
	}
	
	public CyNetworkView getNetworkView() {
		return networkView;
	}

	public void setNetworkView(CyNetworkView networkView) {
		this.networkView = networkView;
	}

	/**
	 * Highlight network by override the method used for pathway highlight
	 */
	@Override
    protected void hilitePathway(int column) {
        if (networkView == null || column < 1) // The first column is entity
            return;
        VariableTableModel model = (VariableTableModel) contentTable.getModel();
        Map<String, Double> idToValue = model.getIdToValue(column);
        PathwayDiagramHighlighter highlighter = new PathwayDiagramHighlighter();
        // Want to narrow down the highlights]
        double[] minMax = PlugInUtilities.getMinMax(idToValue.values());
        highlighter.highlighNetwork(networkView, idToValue, minMax[0], minMax[1]);
    }

}
