/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.Renderable;
import org.jdom.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All opened pathway diagrams have been registered here.
 * @author gwu
 *
 */
public class PathwayDiagramRegistry {
    private final Logger logger = LoggerFactory.getLogger(PathwayDiagramRegistry.class);
    private static PathwayDiagramRegistry registry;
    // Register PathwayDiagram id to PathwayInternalFrame
    private Map<Long, PathwayInternalFrame> diagramIdToFrame;
    // Register Pathway DB_ID to PathwayDiagram ID
    private Map<Long, Long> pathwayIdToDiagramId;
    // For handling internal frame events
    private List<InternalFrameListener> frameListeners;
    // For selection
    private EventSelectionMediator selectionMediator;
    // A flag to avoid a ConcurrentModficationException
    private boolean isFromCloseAll;
    // Used to catch some property change
    private PropertyChangeSupport propertyChangeSupport;
    // This map is used to map a converted FI network to its original PathwayDiagram
    private Map<CyNetwork, Renderable> networkToDiagram;
    
    /**
     * Default private constructor.
     */
    private PathwayDiagramRegistry() {
        diagramIdToFrame = new HashMap<Long, PathwayInternalFrame>();
        pathwayIdToDiagramId = new HashMap<Long, Long>();
        networkToDiagram = new HashMap<CyNetwork, Renderable>();
        selectionMediator = new EventSelectionMediator();
        propertyChangeSupport = new PropertyChangeSupport(this);
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
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
    
    public void firePropertyChange(PropertyChangeEvent e) {
        propertyChangeSupport.firePropertyChange(e);
    }
    
    /**
     * Store the mapping from network to pathway.
     * @param network
     * @param pathway
     */
    public void registerNetworkToDiagram(CyNetwork network, 
                                          Renderable pathway) {
        networkToDiagram.put(network, pathway);
    }
    
    /**
     * Get the original pathway diagram for a converted FI network.
     * @param network
     * @return
     */
    public Renderable getDiagramForNetwork(CyNetwork network) {
        return networkToDiagram.get(network);
    }
    
    public EventSelectionMediator getEventSelectionMediator() {
        return this.selectionMediator;
    }
    
    /**
     * Use this method to clean any selection in registered PathwayInternalFrames.
     */
    public void clearSelection() {
        for (PathwayInternalFrame frame : diagramIdToFrame.values()) {
            PathwayEditor editor = frame.getPathwayEditor();
            editor.removeSelection();
            editor.repaint(editor.getVisibleRect());
        }
    }
    
    /**
     * Select a sub-pathway in a parent pathway. Both pathways are specified by their DB_IDs. If
     * a sub-pathway cannot be found, its contained sub-events will be checked.
     * @param parentId
     * @param containedId
     * @param isPathway true if the contained pathway is a pathway
     */
    public void select(Long parentId,
                       final Long containedId,
                       boolean isPathway) {
        // Need to remove selection first
        clearSelection();
        PathwayInternalFrame frame = getPathwayFrame(parentId);
        if (frame == null)
            return;
        final PathwayEditor editor = frame.getPathwayEditor();
        List<Renderable> selection = new ArrayList<Renderable>();
        for (Object obj : editor.getDisplayedObjects()) {
            Renderable r = (Renderable) obj;
            if (containedId.equals(r.getReactomeId()))
                selection.add(r);
        }
        if (selection.size() > 0 || !isPathway) {
            editor.setSelection(selection);
            return;
        }
        // Need to check if any contained events should be highlighted since the selected event cannot
        // be highlighted
        try {
            List<Long> dbIds = ReactomeRESTfulService.getService().getContainedEventIds(containedId);
            for (Object obj : editor.getDisplayedObjects()) {
                Renderable r = (Renderable) obj;
                if (dbIds.contains(r.getReactomeId()))
                    selection.add(r);
            }
            if (selection.size() > 0)
                editor.setSelection(selection);
        }
        catch(Exception e) {
            logger.error("Error in select: " + e, e);
        }
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
    public void register(final Long diagramId,
                         final PathwayInternalFrame frame) {
        diagramIdToFrame.put(diagramId, frame);
        // In order to remove this registry if the passed frame is closed
        frame.addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                selectionMediator.removeEventSelectionListener(frame.getZoomablePathwayEditor());
                if (isFromCloseAll)
                    return; // Don't do anything
                diagramIdToFrame.remove(diagramId);
                // Since one diagram id may be mapped to multiple pathway id,
                // we need to check each diagramId in pathwayIdToDiagramId map
                for (Iterator<Long> it = pathwayIdToDiagramId.keySet().iterator(); it.hasNext();) {
                    Long pathwayId1 = it.next();
                    Long diagramId1 = pathwayIdToDiagramId.get(pathwayId1);
                    if (diagramId.equals(diagramId1)) {
                        it.remove();
                    }
                }
            }            
            
        });
        if (frameListeners != null && frameListeners.size() > 0) {
            for (InternalFrameListener listener : frameListeners)
                frame.addInternalFrameListener(listener);
        }
        selectionMediator.addEventSelectionListener(frame.getZoomablePathwayEditor());
        fetchPathwaysForDiagram(frame,
                                diagramId);
    }
    
    private void fetchPathwaysForDiagram(PathwayInternalFrame frame,
                                         Long diagramId) {
        try {
            Element diagramElm = ReactomeRESTfulService.getService().queryById(diagramId, 
                                                                               ReactomeJavaConstants.PathwayDiagram);
            List<?> children = diagramElm.getChildren(ReactomeJavaConstants.representedPathway);
            List<Long> pathwayIds = new ArrayList<Long>();
            for (Object obj : children) {
                Element elm = (Element) obj;
                Long dbId = new Long(elm.getChildText("dbId"));
                pathwayIdToDiagramId.put(dbId, diagramId);
                pathwayIds.add(dbId);
            }
            frame.setRelatedPathwayIds(pathwayIds);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get a registered PathwayInternalFrame if it is available.
     */
    public PathwayInternalFrame getPathwayFrame(Long pathwayId) {
        Long diagramId = pathwayIdToDiagramId.get(pathwayId);
        if (diagramId == null)
            return null;
        PathwayInternalFrame pathwayFrame = diagramIdToFrame.get(diagramId);
        // Make sure pathwayId is used for the returned PathwayInternalFrame object
        pathwayFrame.setPathwayId(pathwayId);
        return pathwayFrame;
    }
    
    /**
     * Get the PathwayInternalFrame currently selected
     */
    public PathwayInternalFrame getSelectedPathwayFrame() {
        for (PathwayInternalFrame frame : diagramIdToFrame.values()) {
            if (frame.isSelected())
                return frame;
        }
        return null;
    }
    
    public void unSelectAllFrames() {
        try {
            for (PathwayInternalFrame frame : diagramIdToFrame.values()) {
                frame.setSelected(false);
            }
        }
        catch(PropertyVetoException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Close all opened JInternalFrames for pathways.
     */
    private void closeAllFrames() {
        isFromCloseAll = true;
        for (JInternalFrame frame : diagramIdToFrame.values()) {
            frame.setVisible(false);
            frame.dispose();
        }
        isFromCloseAll = false;
        diagramIdToFrame.clear(); // Empty the opened diagrams
        pathwayIdToDiagramId.clear();
        networkToDiagram.clear();
    }
    
    /**
     * Show a diagram for a pathway. If it is not loaded, this method will load it using
     * a task.
     * @param pathwayId
     */
    public void showPathwayDiagram(Long pathwayId) {
        PathwayInternalFrame frame = getPathwayFrame(pathwayId);
        if (frame != null) {
            try {
                frame.setSelected(true);
                frame.toFront();
            }
            catch(PropertyVetoException e) {
                e.printStackTrace();
            }
            return; 
        }
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        PathwayDiagramLoadTask task = new PathwayDiagramLoadTask();
        task.setPathwayId(pathwayId);
        taskManager.execute(new TaskIterator(task));
    }
    
    /**
     * Get a PathwayInternalFrame showing a pathway specificed by its DB_ID. If the pathway
     * has not been displayed, the method will wait for 10 seconds until it is displayed. 
     * After 10 seconds, if the pathway is still not displayed, a null object will be returned.
     * @param eventId
     * @return
     * @TODO: This method may have returned null return in a very slow Internet connection!!!
     * So probably a time-out configuration that can be configured by the user?
     */
    public PathwayInternalFrame getPathwayFrameWithWait(Long eventId) {
        long time1 = System.currentTimeMillis();
        long diff = 0;
        PathwayInternalFrame rtn = null;
        while (diff < 10000) { // This is a dangerous code
            PathwayInternalFrame frame = getPathwayFrame(eventId);
            if (frame != null && frame.isSelected()) {
                rtn = frame;
                break;
            }
            try {
                Thread.sleep(100); // Sleep for 0.1 second
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            long time2 = System.currentTimeMillis();
            diff = time2 - time1; 
        }
        return rtn;
    }
    
}
