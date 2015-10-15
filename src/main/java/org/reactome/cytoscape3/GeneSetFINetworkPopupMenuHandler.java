package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * Some extra work needs for a FI network generated from a gene set.
 * 
 */
public class GeneSetFINetworkPopupMenuHandler extends FINetworkPopupMenuHandler {
    
    public GeneSetFINetworkPopupMenuHandler() {
    }
    
    @Override
    protected void installMenus() {
        super.installMenus();
        FIAnnotationFetcherMenu annotFIsMenu = new FIAnnotationFetcherMenu();
        installOtherNetworkMenu(annotFIsMenu,
                        "Fetch FI Annotations");
    }

    /**
     * A class for the network view context menu item to fetch FI annotations.
     * 
     * @author Eric T. Dawson
     * 
     */
    private class FIAnnotationFetcherMenu implements CyNetworkViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem fetchFIAnnotationsMenu = new JMenuItem(
                    "Fetch FI Annotations");
            fetchFIAnnotationsMenu.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Thread t = new Thread()
                    {
                        @Override
                        public void run() {
                            try {
                                EdgeActionCollection.annotateFIs(view);
                                BundleContext context = PlugInObjectManager.getManager().getBundleContext();
                                ServiceReference servRef = context.getServiceReference(FIVisualStyle.class.getName());
                                FIVisualStyle visStyler = (FIVisualStyle) context.getService(servRef);
                                visStyler.setVisualStyle(view, false); // If there is one already, don't recreate it.
                                context.ungetService(servRef);
                            }
                            catch (Exception t) {
                                JOptionPane.showMessageDialog(
                                        PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                        "The visual style could not be applied.",
                                        "Visual Style Error",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };
                    t.start();
                }
            });
            return new CyMenuItem(fetchFIAnnotationsMenu, 1.0f);
        }
    }

}
