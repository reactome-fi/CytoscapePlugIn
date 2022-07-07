package org.reactome.cytoscape.bn;

import java.util.Map;

import javax.swing.JLabel;

import org.cytoscape.view.model.CyNetworkView;
import org.reactome.cytoscape.service.PathwayDiagramHighlighter;

public class NetworkTimeCoursePane extends TimeCoursePane {
	private CyNetworkView networkView;
	
	public NetworkTimeCoursePane(String title) {
		super(title);
		this.hiliteDiagramBtn.setText("Highlight network");
	}
	
	public void setNetworkView(CyNetworkView view) {
		this.networkView = view;
	}
	
	@Override
	protected JLabel createTimeLabel() {
		JLabel timeLabel = new JLabel("Choose time to highlight network:");
		return timeLabel;
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
        // Use 0 and 1 as default
        highlighter.highlighNetwork(networkView, idToValue, 0d, 1.0d);
    }

}
