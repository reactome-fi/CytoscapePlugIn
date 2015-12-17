/*
 * Created on Oct 12, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import org.cytoscape.application.swing.CytoPanelName;
import org.reactome.cytoscape.pgm.SampleListComponent;
import org.reactome.cytoscape.service.PopupMenuManager;
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
    
    public void showInferenceResults() throws IllegalAccessException, InstantiationException {
        showSampleWiseResults();
        showGeneWiseResults();
        showSingleSamples();
    }
    
    private void showSingleSamples() throws IllegalAccessException, InstantiationException{
        FIPGMSampleListComponent samplePane = (FIPGMSampleListComponent) PlugInUtilities.getCytoPanelComponent(FIPGMSampleListComponent.class,
                                                                                                               CytoPanelName.EAST,
                                                                                                               SampleListComponent.TITLE);
        samplePane.showResults();
    }
    
    /**
     * Show results in a sample-wise way.
     * @param resultsList
     * @param target
     * @return true if values are shown.
     */
    private void showSampleWiseResults() throws IllegalAccessException, InstantiationException{
        ImpactSampleValueTablePane valuePane = (ImpactSampleValueTablePane) PlugInUtilities.getCytoPanelComponent(ImpactSampleValueTablePane.class,
                                                                                     CytoPanelName.SOUTH,
                                                                                     ImpactSampleValueTablePane.TITLE);
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
    }
    
    private void showGeneWiseResults() throws IllegalAccessException, InstantiationException{
        ImpactGeneValueTablePane valuePane = (ImpactGeneValueTablePane) PlugInUtilities.getCytoPanelComponent(ImpactGeneValueTablePane.class,
                                                                                                              CytoPanelName.SOUTH,
                                                                                                              ImpactGeneValueTablePane.TITLE);
        // Need to call this method in case the network is the first one to be displayed.
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
        valuePane.selectViewButtonWithoutFiringEvent();
//        valuePane.showResults();
    }
    
}
