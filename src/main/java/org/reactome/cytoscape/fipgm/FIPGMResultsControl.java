/*
 * Created on Oct 12, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * Control how to display inference results after a FI network is constructed.
 * @author gwu
 *
 */
public class FIPGMResultsControl {
    
    /**
     * Default constructor.
     */
    public FIPGMResultsControl() {
    }
    
    public void showInferneceResults() {
        showSampleWiseResults();
        showGeneWiseResults();
    }
    
    /**
     * Show results in a sample-wise way.
     * @param resultsList
     * @param target
     * @return true if values are shown.
     */
    private void showSampleWiseResults() {
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        String title = "Impact Sample Values";
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        ImpactSampleValueTablePane valuePane = null;
        if (index < 0)
            valuePane = new ImpactSampleValueTablePane(title);
        else
            valuePane = (ImpactSampleValueTablePane) tableBrowserPane.getComponentAt(index);
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
    }
    
    private void showGeneWiseResults() {
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        String title = "Impact Gene Values";
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        ImpactGeneValueTablePane valuePane = null;
        if (index < 0)
            valuePane = new ImpactGeneValueTablePane(title);
        else
            valuePane = (ImpactGeneValueTablePane) tableBrowserPane.getComponentAt(index);
        // Need to call this method in case the network is the first one to be displayed.
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
//        valuePane.showResults();
        // Want to select this if this tab is newly created
        // Only select it if this tab is newly created.
        if (index == -1) {
            index = tableBrowserPane.indexOfComponent(valuePane);
            if (index >= 0) // Select this as the default table for viewing the results
                tableBrowserPane.setSelectedIndex(index);
        }
    }
    
}
