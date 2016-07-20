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
import java.util.Set;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.DiagramGKBWriter;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.jdom.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.TableHelper;
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
    // Diagrams are saved in XML String to avoid node colors change (e.g. highlighting)
    private Map<CyNetwork, String> networkToDiagram;
    
    /**
     * Default private constructor.
     */
    private PathwayDiagramRegistry() {
        diagramIdToFrame = new HashMap<Long, PathwayInternalFrame>();
        pathwayIdToDiagramId = new HashMap<Long, Long>();
        networkToDiagram = new HashMap<CyNetwork, String>();
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
            SessionLoadedListener sessionListener = new SessionLoadedListener() {
                
                @Override
                public void handleEvent(SessionLoadedEvent e) {
                    getRegistry().closeAllFrames();
                }
            };
            context.registerService(SessionLoadedListener.class.getName(),
                                    sessionListener,
                                    null);
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
                                         RenderablePathway pathway) {
        try {
            DiagramGKBWriter writer = new DiagramGKBWriter();
            writer.setNeedDisplayName(true);
            String text = writer.generateXMLString(pathway);
            networkToDiagram.put(network, text);
        }
        catch(Exception e) {
            logger.error("registerNetworkToDiagram: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the original pathway diagram for a converted FI network.
     * @param network
     * @return
     */
    public Renderable getDiagramForNetwork(CyNetwork network) {
        String text = networkToDiagram.get(network);
        if (text != null) {
            DiagramGKBReader reader = new DiagramGKBReader();
            try {
                return reader.openDiagram(text);
            }
            catch (Exception e) {
                logger.error("getDiagramForNetwork: " + e.getMessage(), e);
            }
        }
        return null;
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
     * Check if a pathways has been displayed as a diagram or a FI network.
     * @param pathwayId
     * @return
     */
    public boolean isPathwayDisplayed(Long pathwayId) {
        PathwayInternalFrame frame = getPathwayFrame(pathwayId);
        if (frame != null)
            return true;
        CyNetworkView network = selectNetworkViewForPathway(pathwayId, false);
        if (network != null)
            return true;
        return false;
    }
    
    /**
     * Get a converted FI network view for a pathway specified by its DB_ID.
     * @param pathwayId
     * @return
     */
    public CyNetworkView selectNetworkViewForPathway(Long pathwayId) {
        return selectNetworkViewForPathway(pathwayId, true);
    }
    
    /**
     * Get a list of CyNetworkViews that are switched from PathwayDiagram views.
     * @return
     */
    private List<CyNetworkView> getDiagramNetworkViews() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(CyNetworkViewManager.class.getName());
        CyNetworkViewManager viewManager = (CyNetworkViewManager) context.getService(reference);
        ServiceReference appRef = context.getServiceReference(CyApplicationManager.class.getName());
        CyApplicationManager manager = (CyApplicationManager) context.getService(appRef);
        List<CyNetworkView> views = new ArrayList<CyNetworkView>();
        if (viewManager.getNetworkViewSet() != null) {
            TableHelper tableHelper = new TableHelper();
            for (CyNetworkView view : viewManager.getNetworkViewSet()) {
                String dataSetType = tableHelper.getDataSetType(view);
                if (!"PathwayDiagram".equals(dataSetType))
                    continue;
                views.add(view);
            }
        }
        context.ungetService(reference);
        context.ungetService(appRef);
        return views;
    }
    
    private CyNetworkView selectNetworkViewForPathway(Long pathwayId, 
                                                      boolean setAsCurrentView) {
        if (pathwayId == null)
            return null; // Nothing can be done
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(CyNetworkViewManager.class.getName());
        CyNetworkViewManager viewManager = (CyNetworkViewManager) context.getService(reference);
        ServiceReference appRef = context.getServiceReference(CyApplicationManager.class.getName());
        CyApplicationManager manager = (CyApplicationManager) context.getService(appRef);
        if (viewManager.getNetworkViewSet() != null) {
            TableHelper tableHelper = new TableHelper();
            for (CyNetworkView view : viewManager.getNetworkViewSet()) {
                String dataSetType = tableHelper.getDataSetType(view);
                if (!"PathwayDiagram".equals(dataSetType))
                    continue;
                Long storedId = tableHelper.getStoredNetworkAttribute(view.getModel(),
                                                                      "PathwayId",
                                                                      Long.class);
                if (pathwayId.equals(storedId)) {
                    if (setAsCurrentView)
                        manager.setCurrentNetworkView(view);
                    context.ungetService(reference);
                    context.ungetService(appRef);
                    return view;
                }
            }
        }
        context.ungetService(reference);
        context.ungetService(appRef);
        return null; // Have not found it
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
    public void showPathwayDiagram(Long pathwayId,
                                   String pathwayName) {
        showPathwayDiagram(pathwayId, true, pathwayName);
    }
    
    /**
     * The actual method to show a pathway diagram.
     * @param pathwayId
     * @param needCheckNetworkView
     * @param pathwayName
     */
    private void showPathwayDiagram(Long pathwayId,
                                    boolean needCheckNetworkView,
                                    String pathwayName) {
        PathwayInternalFrame frame = getPathwayFrame(pathwayId);
        if (frame != null) {
            showPathwayDiagramFrame(frame);
            return; 
        }
        if (needCheckNetworkView) {
            // Check if a network view has been displayed
            CyNetworkView networkView = selectNetworkViewForPathway(pathwayId);
            if (networkView != null) {
                return;
            }
        }
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        PathwayDiagramLoadTask task = new PathwayDiagramLoadTask();
        task.setPathwayId(pathwayId);
        task.setPathwayName(pathwayName);
        taskManager.execute(new TaskIterator(task));
    }

    /**
     * A utilty method to show JInternalFrame for showing a pathway.
     * @param frame
     */
    public void showPathwayDiagramFrame(JInternalFrame frame) {
        if (frame == null)
            return;
        try {
            PlugInObjectManager.getManager().showPathwayDesktop();
            frame.setSelected(true);
            frame.toFront();
        }
        catch(PropertyVetoException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Show a pathway diagram specified by its DB_ID. This method will not check if a network view is
     * available for the pathway. In other words, using this method, a pathway can be displayed as both
     * views: pathway diagram and FI view.
     * @param pathwayId
     */
    public void showPathwayDiagram(Long pathwayId) {
        showPathwayDiagram(pathwayId, false, null);
    }
    
    /**
     * Gene highlighting for any opened diagrams of FI views
     */
    public void highlightPathwayViews() {
        PathwayEnrichmentHighlighter hiliter = PathwayEnrichmentHighlighter.getHighlighter();
        Set<String> genes = hiliter.getHitGenes();
        List<String> geneList = new ArrayList<String>(genes);
        for (PathwayInternalFrame frame : diagramIdToFrame.values()) {
            hiliter.highlightPathway(frame, geneList);
        }
        // Check with network views
        List<CyNetworkView> networkViews = getDiagramNetworkViews();
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference servRef = context.getServiceReference(FIVisualStyle.class.getName());
        FIVisualStyle visStyler = (FIVisualStyle) context.getService(servRef);
        for (CyNetworkView view : networkViews) {
            hiliter.highlightNework(view.getModel(),
                                    geneList);
            visStyler.setVisualStyle(view, false); // Need to reset visual style to force update the view
        }
        context.ungetService(servRef);
    }
    
    public void removeHighlightPathwayViews() {
        PathwayEnrichmentHighlighter hiliter = PathwayEnrichmentHighlighter.getHighlighter();
        Set<String> genes = hiliter.getHitGenes();
        List<String> geneList = new ArrayList<String>(genes);
        for (PathwayInternalFrame frame : diagramIdToFrame.values()) {
            hiliter.removeHighlightPathway(frame);
        }
        // Check with network views
        List<CyNetworkView> networkViews = getDiagramNetworkViews();
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference servRef = context.getServiceReference(FIVisualStyle.class.getName());
        FIVisualStyle visStyler = (FIVisualStyle) context.getService(servRef);
        for (CyNetworkView view : networkViews) {
            hiliter.removeHighlightNewtork(view.getModel());
            visStyler.setVisualStyle(view, false);
        }
        context.ungetService(servRef);
    }    
}
