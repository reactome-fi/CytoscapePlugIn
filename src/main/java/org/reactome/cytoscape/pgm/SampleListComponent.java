/*
 * Created on Dec 2, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * @author gwu
 *
 */
public class SampleListComponent extends JPanel implements CytoPanelComponent {
    public static final String TITLE = "Sample List";
    
    public SampleListComponent() {
        init();
    }
    
    private void init() {
        // Register this as a CytoPanelComponent so that it can be added automatically
        // when it is intialized.
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(CytoPanelComponent.class.getName(), 
                                this,
                                null);
    }

    @Override
    public Component getComponent() {
        
        return null;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }
    
    
}
