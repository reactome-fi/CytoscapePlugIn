/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameListener;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * All opened pathway diagrams have been registered here.
 * @author gwu
 *
 */
public class PathwayDiagramRegistry {
    private static PathwayDiagramRegistry registry;
    private Map<Long, PathwayInternalFrame> diagramIdToFrame;
    private List<InternalFrameListener> frameListeners;
    
    /**
     * Default private constructor.
     */
    private PathwayDiagramRegistry() {
        diagramIdToFrame = new HashMap<Long, PathwayInternalFrame>();
    }
    
    public static PathwayDiagramRegistry getRegistry() {
        if (registry == null) {
            registry = new PathwayDiagramRegistry();
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            context.addBundleListener(new SynchronousBundleListener() {
                
                @Override
                public void bundleChanged(BundleEvent event) {
                    if (event.getType() == BundleEvent.STOPPING) {
                        getRegistry().closeAllFrames();
                    }
                }
            });
        }
        return registry;
    }
    
    /**
     * Add an InternalFrameListener so that it can be registered for newly created JInternalFrame that
     * is used to display pathway diagram.
     * @param listener
     */
    public void addInternalFrameListener(InternalFrameListener listener) {
        if (frameListeners == null)
            frameListeners = new ArrayList<InternalFrameListener>();
        if (!frameListeners.contains(listener))
            frameListeners.add(listener);
    }
    
    /**
     * Register an opened JInternalFrame for a DB_ID of a PathwayDiagram instance.
     * @param diagramId
     * @param frame
     */
    public void register(Long diagramId,
                         PathwayInternalFrame frame) {
        diagramIdToFrame.put(diagramId, frame);
        if (frameListeners != null && frameListeners.size() > 0) {
            for (InternalFrameListener listener : frameListeners)
                frame.addInternalFrameListener(listener);
        }
    }
    
    /**
     * Close all opened JInternalFrames for pathways.
     */
    public void closeAllFrames() {
        for (JInternalFrame frame : diagramIdToFrame.values()) {
            frame.setVisible(false);
            frame.dispose();
        }
    }
    
}
