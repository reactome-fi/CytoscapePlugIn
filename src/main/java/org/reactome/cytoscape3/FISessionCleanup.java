package org.reactome.cytoscape3;

import java.awt.Component;
import java.awt.Container;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.view.model.events.NetworkViewDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewDestroyedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInObjectManager;
/**
 * A class for cleaning up after the Reactome FI session is
 * ended. Removes CytoPanelComponents from the Cytopanels and
 * other functions.
 * @author Eric T. Dawson
 *
 */
public class FISessionCleanup implements NetworkViewDestroyedListener, SessionAboutToBeSavedListener {
    public FISessionCleanup() {
    }
    
    @Override
    public void handleEvent(SessionAboutToBeSavedEvent e)
    {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference visMapManRef = context.getServiceReference(VisualMappingManager.class.getName());
        VisualMappingManager visMapMan = (VisualMappingManager) context.getService(visMapManRef); 
      //Remove FI Visual Style (fix for inability to save
        //continuous mapping of integer values.
        if(visMapMan.getCurrentVisualStyle().getTitle().equals("FI Network"));
        {
            visMapMan.removeVisualStyle(visMapMan.getCurrentVisualStyle());
            visMapMan.setCurrentVisualStyle(visMapMan.getDefaultVisualStyle());
        }
    }
    
    @Override
    public void handleEvent(NetworkViewDestroyedEvent e)
    {
        removeAllResultsPanels();
    }

    private void removeAllResultsPanels() {
        String [] titles = {"MCL Module Browser", "HotNet Module Browser", "Network Module Browser",
                "Pathways in Modules", "Pathways in Network", "GO CC in Network", "GO BP in Network",
                "GO MF in Network", "GO CC in Modules", "GO CC in Modules", "GO BP in Modules"};
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp
                .getCytoPanel(CytoPanelName.SOUTH);
        int numComps = tableBrowserPane.getCytoPanelComponentCount();
        for (int i = 0; i < numComps; i++)
        {
            CytoPanelComponent aComp = (CytoPanelComponent) tableBrowserPane
                    .getComponentAt(i);
            boolean hasRemoved = false;
            for (String title : titles)
            {
                if (aComp.getTitle().equalsIgnoreCase(title))
                {
                    ((Container) tableBrowserPane).remove((Component) aComp);
                    hasRemoved = true;
                }
            }
            if (hasRemoved) {
                // In case a tab has been deleted
                numComps = tableBrowserPane.getCytoPanelComponentCount();
                i = 0; // Start from the first tab again. It should be very fast though there is some processing waste.
            }
        }
        CytoPanel resultsPane = desktopApp.getCytoPanel(CytoPanelName.EAST);
        numComps = resultsPane.getCytoPanelComponentCount();
        for (int i = 0; i < numComps; i++)
        {
            Component aComp = (Component) resultsPane.getComponentAt(i);
            if ((aComp instanceof CytoPanelComponent) && ((CytoPanelComponent) aComp).getTitle().equalsIgnoreCase("Survival Analysis")) {
                ((Container) resultsPane).remove(aComp);
                break; // There should be only one survival analysis tab
            }
        }
        resultsPane = desktopApp.getCytoPanel(CytoPanelName.WEST);
        numComps = resultsPane.getCytoPanelComponentCount();
        for (int i = 0; i < numComps; i++)
        {
            Component aComp = (Component) resultsPane.getComponentAt(i);
            if ((aComp instanceof CytoPanelComponent) && ((CytoPanelComponent) aComp).getTitle().equals("NCI Diseases")) {
                ((Container) resultsPane).remove(aComp);
                break;
            }
        }
    }
    
}

