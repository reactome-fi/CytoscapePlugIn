/*
 * Created on Jul 22, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.math.MathException;
import org.cytoscape.property.CyProperty;
import org.cytoscape.util.swing.FileUtil;
import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.*;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.SwingImageCreator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.pgm.FactorGraphInferenceResults;
import org.reactome.cytoscape.pgm.FactorGraphInferenceResultsIO;
import org.reactome.cytoscape.pgm.FactorGraphRegistry;
import org.reactome.cytoscape.pgm.GeneLevelResultDialog;
import org.reactome.cytoscape.pgm.ObservationDataDialog;
import org.reactome.cytoscape.service.FISourceQueryHelper;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.ReactomeSourceView;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.cytoscape.util.SearchDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Because of the overload of method getBounds() in class BiModalJSplitPane, which is one of JDesktopPane used in Cyotscape,
 * we have to method getVisibleRect() in this customized PathwayEditor in order to provide correct value.
 * @author gwu
 *
 */
public class CyZoomablePathwayEditor extends ZoomablePathwayEditor implements EventSelectionListener {
    private final Logger logger = LoggerFactory.getLogger(CyZoomablePathwayEditor.class);
    // A PathwayDiagram may be used by multile pathways (e.g. a disease pathway and a 
    // normal pathway may share a same PathwayDiagram)
    private List<Long> relatedPathwayIds;
    // Keep the original colors so that they can be revert back in case remove highlighting
    private Map<Renderable, Color> rToFg;
    private Map<Renderable, Color> rToBg;
    // To control pathway highlight
    private PathwayHighlightControlPanel hiliteControlPane;

    public CyZoomablePathwayEditor() {
        // Don't need the title
        titleLabel.setVisible(false);
        init();
    }
    
    private void init() {
        getPathwayEditor().getSelectionModel().addGraphEditorActionListener(new GraphEditorActionListener() {
            @Override
            public void graphEditorAction(GraphEditorActionEvent e) {
                if (e.getID() != ActionType.SELECTION)
                    return;
                handleGraphSelectionEvent(e);
            }
        });
        
        // Add a popup listener
        getPathwayEditor().addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doPopup(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doPopup(e);
            }
            
        });
        initColorSpectrumPane();
    }
    
    private void initColorSpectrumPane() {
        JPanel southPane = null;
        for (Component comp : getComponents()) {
            if (!(comp instanceof JPanel))
                continue;
            JPanel panel = (JPanel) comp;
            for (Component tmp : panel.getComponents()) {
                if (tmp instanceof JSlider) {
                    southPane = panel;
                    break;
                }
            }
        }
        if (southPane == null)
            return; // Cannot do anything
        hiliteControlPane = new PathwayHighlightControlPanel();
        southPane.add(hiliteControlPane);
        hiliteControlPane.setVisible(false); // Default is null
        hiliteControlPane.setPathwayEditor(pathwayEditor);
    }
    
    public PathwayHighlightControlPanel getHighlightControlPane() {
        return this.hiliteControlPane;
    }
    
    @Override
    public void eventSelected(EventSelectionEvent selectionEvent) {
        List<Long> relatedIds = getRelatedPathwaysIds();
        if (!relatedIds.contains(selectionEvent.getParentId())) {
            // Remove any selection
            PathwayEditor editor = getPathwayEditor();
            // it is possible a contained event may be there
            @SuppressWarnings("unchecked")
            List<Renderable> renderables = getPathwayEditor().getDisplayedObjects();
            List<Renderable> selection = new ArrayList<Renderable>();
            for (Renderable r : renderables) {
                if (selectionEvent.getEventId().equals(r.getReactomeId())) {
                    selection.add(r);
                }
            }
            editor.setSelection(selection);
            editor.repaint(editor.getVisibleRect());
            return;
        }
        // If this is contained by a JInternalFrame, select it
        JInternalFrame frame = (JInternalFrame) SwingUtilities.getAncestorOfClass(JInternalFrame.class,
                                                                                  this);
        if (frame != null) {
            try {
                frame.setSelected(true); // Select this PathwayInternalFrame too!
            }
            catch(PropertyVetoException e) {
                logger.error("Error in eventSelected: " + e, e);
            }
        }
        Long eventId = selectionEvent.getEventId();
        PathwayEditor editor = getPathwayEditor();
        // The top-level should be selected
        if (relatedIds.contains(eventId)) {
            editor.removeSelection();
            editor.repaint(editor.getVisibleRect());
            return;
        }
        List<Renderable> selection = new ArrayList<Renderable>();
        for (Object obj : editor.getDisplayedObjects()) {
            Renderable r = (Renderable) obj;
            if (eventId.equals(r.getReactomeId()))
                selection.add(r);
        }
        if (selection.size() > 0 || !selectionEvent.isPathway()) {
            editor.setSelection(selection);
            return;
        }
        // Need to check if any contained events should be highlighted since the selected event cannot
        // be highlighted
        try {
            List<Long> dbIds = ReactomeRESTfulService.getService().getContainedEventIds(eventId);
            for (Object obj : editor.getDisplayedObjects()) {
                Renderable r = (Renderable) obj;
                if (dbIds.contains(r.getReactomeId()))
                    selection.add(r);
            }
            if (selection.size() > 0)
                editor.setSelection(selection);
        }
        catch(Exception e) {
            logger.error("Error in eventSelected: " + e, e);
        }
    }
    
    /**
     * Set the ids of pathways displayed in this PathwayInternalFrame. A pathway diagram may represent
     * multiple pathways.
     * @param pathwayIds
     */
    public void setRelatedPathwayIds(List<Long> pathwayIds) {
        this.relatedPathwayIds = pathwayIds;
    }
    
    /**
     * Record colors used originally in order to set it back.
     */
    public void recordColors() {
        if (rToBg == null)
            rToBg = new HashMap<Renderable, Color>();
        else
            rToBg.clear();
        if (rToFg == null)
            rToFg = new HashMap<Renderable, Color>();
        else
            rToFg.clear();
        for (Object obj : getPathwayEditor().getDisplayedObjects()) {
            Renderable r = (Renderable) obj;
            rToBg.put(r, r.getBackgroundColor());
            rToFg.put(r, r.getForegroundColor());
        }
    }
    
    /**
     * Reset colors to the originally set in the database.
     */
    public void resetColors() {
        for (Renderable r : rToBg.keySet())
            r.setBackgroundColor(rToBg.get(r));
        for (Renderable r : rToFg.keySet())
            r.setForegroundColor(rToFg.get(r));
        PathwayEditor editor = getPathwayEditor();
        editor.repaint(getPathwayEditor().getVisibleRect());
        // Forge update in other views if any
        editor.fireGraphEditorActionEvent(ActionType.SELECTION);
    }
    
    public List<Long> getRelatedPathwaysIds() {
        if (relatedPathwayIds != null && relatedPathwayIds.size() > 0)
            return relatedPathwayIds;
        List<Long> rtn = new ArrayList<Long>();
        if (getPathwayEditor().getRenderable().getReactomeId() != null)
            rtn.add(getPathwayEditor().getRenderable().getReactomeId());
        return rtn;
    }
    
    private boolean removeOverlaidFIs() {
        CyPathwayEditor pathwayEditor = (CyPathwayEditor) getPathwayEditor();
        if (!pathwayEditor.hasFIsOverlaid())
            return true;
        // Make sure all overlaid FIs should be removed
        int reply = JOptionPane.showConfirmDialog(this,
                                                 "Overlaid FIs need to be removed first in order to generate the FI network view.\n" + 
                                                 "Do you want to remove the overlaid FIs?",
                                                 "Remove FIs?", 
                                                 JOptionPane.OK_CANCEL_OPTION);
        if (reply != JOptionPane.OK_OPTION)
            return false;
        pathwayEditor.removeFIs();
        return true;
    }
    
    private void doPathwayPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem convertToFINetwork = new JMenuItem("Convert to FI Network");
        convertToFINetwork.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!removeOverlaidFIs())
                    return;
                // A kind of hack
                CyZoomablePathwayEditor.this.firePropertyChange("convertAsFINetwork", 
                                                                false,
                                                                true);
            }
        });
        popup.add(convertToFINetwork);
        
        JMenuItem runPGMAnalysis = new JMenuItem("Run Graphical Model Analysis");
        runPGMAnalysis.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                runFactorGraphAnalysis();
            }
        });
        popup.addSeparator();
        popup.add(runPGMAnalysis);
        JMenuItem showGeneData = new JMenuItem("Show Gene Level Analysis Results");
        showGeneData.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
//                TODO: Selection is not correct: cannot highlight
                GeneLevelResultDialog dialog = new GeneLevelResultDialog();
                if(!dialog.showResultsForDiagram((RenderablePathway)pathwayEditor.getRenderable()))
                    return; // Nothing to be displayed
                dialog.enableDiagramSelection(pathwayEditor);
                dialog.setSize(750, 600);
//                dialog.setModal(true);
                dialog.setVisible(true);
            }
        });
        popup.add(showGeneData);
        JMenuItem showObservation = new JMenuItem("Show Observation");
        showObservation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ObservationDataDialog dialog = new ObservationDataDialog();
                if(!dialog.showResultsForDiagram((RenderablePathway)pathwayEditor.getRenderable()))
                    return; // Nothing to be displayed
                dialog.enableDiagramSelection(pathwayEditor);
                dialog.setSize(750, 600);
//                dialog.setModal(true);
                dialog.setVisible(true);
            }
        });
        popup.add(showObservation);
        // Output analysis results
        JMenuItem saveResults = new JMenuItem("Save Analysis Results");
        saveResults.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAnalysisResults();
            }
        });
        popup.add(saveResults);
        // Input analysis results
        JMenuItem openResults = new JMenuItem("Open Analysis Results");
        openResults.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                openAnalysisResults();
            }
        });
        popup.add(openResults);
        
        // As of January 26, 2015, we will not display the converted Factor Graph for normal use.
        // The old factor graph based visualization will be enabled for advanced users only and will
        // be developed along the normal use.
        String pgmDebug = PlugInObjectManager.getManager().getProperties().getProperty("PGMDebug");
        if (pgmDebug != null && pgmDebug.equalsIgnoreCase("true")) {
            // Convert as a factor graph
            JMenuItem convertAsFactorGraph = new JMenuItem("Convert to Graphical Model");
            convertAsFactorGraph.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Show a warning
                    // Use the JFrame so that the position is the same as other dialog
                    int reply = JOptionPane.showConfirmDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                              "Features related to probabilistic graphical models are still experimental,\n"
                                                                      + "and will be changed in the future. Please use inferred results with \n"
                                                                      + "caution. Do you still want to continue?",
                                                                      "Experimental Feature Warning",
                                                                      JOptionPane.OK_CANCEL_OPTION,
                                                                      JOptionPane.WARNING_MESSAGE);
                    if (reply == JOptionPane.CANCEL_OPTION)
                        return;
                    // A kind of hack
                    CyZoomablePathwayEditor.this.firePropertyChange("convertAsFactorGraph", 
                                                                    false,
                                                                    true);
                }
            });
            popup.add(convertAsFactorGraph);
        }
        popup.addSeparator();
        
        final CyPathwayEditor pathwayEditor = (CyPathwayEditor) getPathwayEditor();
        if (pathwayEditor.hasFIsOverlaid()) {
            // Give a chance to remove overlaid FIs
            JMenuItem item = new JMenuItem("Remove FIs");
            item.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    pathwayEditor.removeFIs();
                }
            });
            popup.add(item);
        }
        JMenuItem searchDiagram = new JMenuItem("Search Diagram");
        searchDiagram.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                searchDiagrams();
            }
        });
        popup.add(searchDiagram);
        
        JMenuItem exportDiagram = new JMenuItem("Export Diagram");
        exportDiagram.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    exportDiagram();
                }
                catch(Exception e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(pathwayEditor,
                                                  "Pathway diagram cannot be exported: " + e1,
                                                  "Error in Diagram Export",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        popup.add(exportDiagram);
        
        popup.show(getPathwayEditor(),
                   e.getX(),
                   e.getY());
    }
    
    /**
     * A local implementation so that we can use the FileUtil in Cytoscape.
     */
    private void exportDiagram() throws Exception {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = null;
        CyProperty<?> cyProp = null;
        ServiceReference[] references = context.getServiceReferences(CyProperty.class.getName(), null);
        if (references != null) {
            for (ServiceReference tmp : references) {
                CyProperty<?> prop = (CyProperty<?>) context.getService(tmp);
                if (prop.getPropertyType().equals(Properties.class)) {
                    reference = tmp;
                    cyProp = prop;
                    break;
                }
                context.ungetService(tmp);
            }
        }
        JFileChooser fileChooser = new JFileChooser();
        if (cyProp != null) {
            Properties properties = (Properties) cyProp.getProperties();
            String lastDirName = properties.getProperty(FileUtil.LAST_DIRECTORY);
            if (lastDirName != null) {
                File dir = new File(lastDirName);
                if (dir.exists())
                    fileChooser.setCurrentDirectory(dir);
            }
        }
        fileChooser.setDialogTitle("Export Pathway Diagram ...");
        SwingImageCreator.exportImage(pathwayEditor, fileChooser);
        // However, we cannot save the current directory.
        File file = fileChooser.getSelectedFile();
        if (file != null && cyProp != null) {
            Properties properties = (Properties) cyProp.getProperties();
            properties.setProperty(FileUtil.LAST_DIRECTORY, file.getParent());
        }
        if (reference != null)
            context.ungetService(reference);
    }
    
    private void searchDiagrams() {
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
        SearchDialog dialog = new SearchDialog(frame);
        dialog.setLabel("Search objects:");
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOKClicked())
            return;
        String key = dialog.getSearchKey();
        List<Renderable> selected = new ArrayList<Renderable>();
        boolean isWholeNameNeeded = dialog.isWholeNameNeeded();
        String lowKey = key.toLowerCase();
        for (Object obj : getPathwayEditor().getDisplayedObjects()) {
            Renderable r = (Renderable) obj;
            if (r instanceof RenderableCompartment ||
                r instanceof HyperEdge)
                continue; // Escape compartments and reactions
            String name = r.getDisplayName();
            if (name == null || name.length() == 0)
                continue; // Just in case
            name = name.toLowerCase();
            if (isWholeNameNeeded) { // Don't merge this check with the next enclosed one.
                if (name.equals(lowKey))
                    selected.add(r);
            }
            else if (name.contains(lowKey))
                selected.add(r);
        }
        if (selected.size() == 0) {
            JOptionPane.showMessageDialog(this, 
                                          "Cannot find any object displayed for \"" + key + "\"", 
                                          "Search Result", 
                                          JOptionPane.INFORMATION_MESSAGE);
        }
        else
            getPathwayEditor().setSelection(selected);
    }
    
    /**
     * Do popup for newly added FIs that don't have Reactome DB_IDs specified.
     * @param r
     * @param event
     */
    private void doPopupForNewObject(final Renderable r,
                                     MouseEvent event) {
        JPopupMenu popup = null;
        if (r instanceof RenderableInteraction) {
            JMenuItem queryFISource = createQueryFISourceMenuItem((RenderableInteraction)r);
            if (queryFISource != null) {
                popup = new JPopupMenu();
                popup.add(queryFISource);
            }
        }
        else if (r instanceof RenderableProtein) {
            // Check if there is any interaction added
            CyPathwayEditor pathwayEditor = (CyPathwayEditor) getPathwayEditor();
            if (pathwayEditor.hasFIsOverlaid(r)) {
                JMenuItem queryGeneCard = new JMenuItem("Query Gene Card");
                queryGeneCard.addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        queryGeneCard((RenderableProtein)r);
                    }
                });
                popup = new JPopupMenu();
                popup.add(queryGeneCard);
            }
        }
        if (popup != null)
            popup.show(getPathwayEditor(),
                       event.getX(),
                       event.getY());
    }
    
    private JMenuItem createQueryFISourceMenuItem(RenderableInteraction interaction) {
        JMenuItem rtn = null;
        String name = interaction.getDisplayName();
        // Check how many FIs have been merged in the specified interaction
        String[] tokens = name.split(", ");
        if (tokens.length > 1) {
            // Need to use submenus
            rtn = new JMenu("Query FI Source");
            // Do a sorting
            List<String> list = Arrays.asList(tokens);
            Collections.sort(list);
            for (String fi : list) {
                JMenuItem item = new JMenuItem(fi);
                final String tmpFi = fi;
                item.addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        queryFISource(tmpFi);
                    }
                });
                rtn.add(item);
            }
        }
        else {
            rtn = new JMenuItem("Query FI Source");
            final String fi = tokens[0];
            rtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    queryFISource(fi);
                }
            });
        }
        return rtn;
    }
    
    private void queryFISource(String fi) {
        // Do a match
        Pattern pattern = Pattern.compile("(.+) - (.+)");
        Matcher matcher = pattern.matcher(fi);
        if (matcher.matches()) {
            String partner1 = matcher.group(1);
            String partner2 = matcher.group(2);
            FISourceQueryHelper queryHelper = new FISourceQueryHelper();
            queryHelper.queryFISource(partner1,
                                      partner2,
                                      this);
        }
    }
    
    private void queryGeneCard(RenderableProtein protein) {
        PlugInUtilities.queryGeneCard(protein.getDisplayName());
    }
    
    private void doPopup(MouseEvent event) {
        @SuppressWarnings("unchecked")
        List<Renderable> selection = getPathwayEditor().getSelection();
        if (selection == null || selection.size() == 0) {
            doPathwayPopup(event);
            return;
        }
        if (selection.size() != 1)
            return;
        Renderable r = selection.get(0);
        if (r.getReactomeId() == null) {
            doPopupForNewObject(r, event);
            return;
        }
        final Long dbId = r.getReactomeId();
        final String name = r.getDisplayName();
        JPopupMenu popup = new JPopupMenu();
        // Add a view Reactome source so that the web site may not be accessed in order to view
        // the detailed information
        JMenuItem viewReactomeSource = new JMenuItem("View Reactome Source");
        viewReactomeSource.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                ReactomeSourceView view = new ReactomeSourceView();
                view.viewReactomeSource(dbId,
                                        getPathwayEditor());
            }
        });
        popup.add(viewReactomeSource);
        
        JMenuItem showDetailed = new JMenuItem("View in Reactome");
        showDetailed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String reactomeURL = PlugInObjectManager.getManager().getProperties().getProperty("ReactomeURL");
                String url = reactomeURL + dbId;
                PlugInUtilities.openURL(url);
            }
        });
        popup.add(showDetailed);
        // If it is a Pathway, add a menu item
        if (r instanceof ProcessNode) {
            JMenuItem openInDiagram = new JMenuItem("Show Diagram");
            openInDiagram.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    PathwayDiagramRegistry.getRegistry().showPathwayDiagram(dbId, name);
                    PathwayInternalFrame pathwayFrame = PathwayDiagramRegistry.getRegistry().getPathwayFrameWithWait(dbId);
                    if (pathwayFrame != null) {
                        PathwayEnrichmentHighlighter.getHighlighter().highlightPathway(pathwayFrame, 
                                                                                        name);
                    }
                }
            });
            popup.add(openInDiagram);
        }
        else if (r instanceof RenderableProtein || 
                 r instanceof RenderableEntitySet || // This may be an EntitySet of SimpleEntity, which contains no gene
                 r instanceof RenderableComplex) {
            JMenuItem listGenesItem = new JMenuItem("List Genes");
            listGenesItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    listGenes(dbId, name);
                }
            });
            popup.add(listGenesItem);
            JMenuItem showGeneLevelPGMResults = new JMenuItem("Show Gene Level Analysis Results");
            showGeneLevelPGMResults.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    showGeneLevelPGMResultsForEntity(dbId, name);
                }
            });
            popup.addSeparator();
            popup.add(showGeneLevelPGMResults);
            JMenuItem showObservations = new JMenuItem("Show Observation");
            showObservations.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    showObservationsForEntity(dbId, name);
                }
            });
            popup.add(showObservations);
            // Fetch FIs
            JMenuItem fetchFIs = new JMenuItem("Fetch FIs");
            fetchFIs.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    fetchFIs(dbId);
                }
            });
            popup.addSeparator();
            popup.add(fetchFIs);
            // Check if a remove FIs action should be added
            CyPathwayEditor pathwayEditor = (CyPathwayEditor) getPathwayEditor();
            final Node node = (Node) r;
            if (pathwayEditor.hasFIsOverlaid(r)) {
                JMenuItem removeFIs = new JMenuItem("Remove FIs");
                removeFIs.addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        removeFIs(node);
                    }
                });
                popup.add(removeFIs);
            }
        }
        popup.show(getPathwayEditor(), 
                   event.getX(),
                   event.getY());
    }
    
    private void  showObservationsForEntity(final Long dbId,
                                            final String name) {
        Set<String> geneSet = fetchGenes(dbId, name);
        if (geneSet == null)
            return;
        ObservationDataDialog dialog = new ObservationDataDialog();
        dialog.setTargetGenes(geneSet);
        if(!dialog.showResultsForDiagram((RenderablePathway)pathwayEditor.getRenderable()))
            return; // Nothing to be displayed
        dialog.setSize(750, 450);
//        dialog.setModal(true);
        dialog.setVisible(true);
    }
    
    private void showGeneLevelPGMResultsForEntity(Long dbId,
                                           String name) {
        Set<String> geneSet = fetchGenes(dbId, name);
        if (geneSet == null)
            return;
        GeneLevelResultDialog dialog = new GeneLevelResultDialog();
        if(!dialog.showResultsForDiagram((RenderablePathway)pathwayEditor.getRenderable(),
                                         geneSet))
            return; // Nothing to be displayed
        dialog.setSize(750, 450);
//        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private Set<String> fetchGenes(Long dbId, String name) {
        RESTFulFIService service = new RESTFulFIService();
        String genes = null;
        try {
            genes = service.listGenes(dbId);
            
            if (genes == null || genes.trim().length() == 0) {
                JOptionPane.showMessageDialog(CyZoomablePathwayEditor.this,
                                              "There is no gene contained in \"" + name + "\"",
                                              "No Gene",
                                              JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(CyZoomablePathwayEditor.this,
                                          "Error in fetching genes for \"" + name + "\": " + e.getMessage(),
                                          "Error in Fetching Genes",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }
        Set<String> geneSet = new HashSet<String>();
        for (String tmp : genes.split(",")) {
            geneSet.add(tmp);
        }
        return geneSet;
    }
    
    /**
     * Remove FIs for all Nodes having the DB_ID specified by the parameter. If a node
     * is displayed multiple times, only FIs associated with the passed Node will be removed.
     * @param dbId
     */
    private void removeFIs(Node r) {
        CyPathwayEditor pathwayEditor = (CyPathwayEditor) getPathwayEditor();
        pathwayEditor.removeFIs(r);
    }
    
    private void fetchFIs(final Long dbId) {
        Thread t = new Thread() {
            public void run() {
                FetchFIForPEInDiagramHelper helper = new FetchFIForPEInDiagramHelper(dbId,
                                                                                     CyZoomablePathwayEditor.this);
                helper.fetchFIs();
            }
        };
        t.start();
    }
    
    /**
     * Get a gene list
     * @param entityId
     */
    private void listGenes(Long entityId,
                           String name) {
        try {
            RESTFulFIService service = new RESTFulFIService();
            String genes = service.listGenes(entityId);
            
            if (genes == null || genes.trim().length() == 0) {
                JOptionPane.showMessageDialog(this,
                                              "There is no gene contained in \"" + name + "\"",
                                              "No Gene",
                                              JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            JEditorPane pane = new JEditorPane();
            pane.setEditable(false);
            pane.setContentType("text/html");
            pane.setBorder(BorderFactory.createEtchedBorder());
            // Using HTML so that genes can be clicked
            StringBuilder builder = new StringBuilder();
            builder.append("<html><body><br /><br /><b><center><u>");
            // Generate a label
            String text = null;
           if (genes.contains(",")) {
                text = "Genes contained by " + name;
            }
            else
                text = "Corresponded gene for protein " + name;
            builder.append(text).append("</u></center></b>");
            builder.append("<br /><br />");

            builder.append(formatGenesText(genes));
            
            if (genes.contains(",")) {
                builder.append("<hr />");
                String reactomeURL = PlugInObjectManager.getManager().getProperties().getProperty("ReactomeURL");
                String url = reactomeURL + entityId;
                builder.append("* Genes may be linked via complex subunits or entity set members. "
                        + "For details, click <a href=\"" + url + "\">View in Reactome</a>.");
            }
            
            builder.append("</body></html>");
            
            pane.setText(builder.toString());
            pane.addHyperlinkListener(new HyperlinkListener() {
                
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        String desc = e.getDescription();
                        if (desc.startsWith("http")) {
                            PlugInUtilities.openURL(desc);
                        }
                        else { // Just a gene
                            String url = "http://www.genecards.org/cgi-bin/carddisp.pl?gene=" + desc;
                            PlugInUtilities.openURL(url);
                        }
                    }
                }
            });
            
            final JDialog dialog = GKApplicationUtilities.createDialog(this, "Gene List");
            dialog.getContentPane().add(new JScrollPane(pane), BorderLayout.CENTER);
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getCancelBtn().setVisible(false);
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            dialog.setSize(400, 300);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(this,
                                          "Error in listing genes in a selected object: " + e,
                                          "Error in Listing Geens",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void openAnalysisResults() {
        try {
            File file = PlugInUtilities.getAnalysisFile("Open Analysis Results",
                                                        FileUtil.LOAD);
            if (file == null)
                return;
//            JAXBContext jaxbContext = JAXBContext.newInstance(FactorGraphInferenceResults.class);
//            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
//            FactorGraphInferenceResults results = (FactorGraphInferenceResults) unmarshaller.unmarshal(file);
            
            FactorGraphInferenceResultsIO reader = new FactorGraphInferenceResultsIO();
            FactorGraphInferenceResults results = reader.read(file);
            
            RenderablePathway diagram = (RenderablePathway) pathwayEditor.getRenderable();
            if (!(diagram.getReactomeDiagramId().equals(results.getPathwayDiagramId()))) {
                JOptionPane.showMessageDialog(this,
                                              "The saved results are not for this pathway diagram!",
                                              "Error in Opening Results", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            showInferenceResults(results);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot open inferece results: " + e,
                                          "Error in Opening Results", 
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Show inference results for the displayed pathway diagram.
     * @param results
     * @throws MathException
     */
    public void showInferenceResults(FactorGraphInferenceResults results) throws MathException {
        FactorGraphAnalyzer analyzer = getFactorGraphAnalyzer();
        analyzer.showInferenceResults(results);
    }

    private FactorGraphAnalyzer getFactorGraphAnalyzer() {
        FactorGraphAnalyzer analyzer = new FactorGraphAnalyzer();
        analyzer.setPathwayEditor(pathwayEditor);
        analyzer.setHighlightControlPane(hiliteControlPane);
        return analyzer;
    }
    
    private void saveAnalysisResults() {
        RenderablePathway pathway = (RenderablePathway) pathwayEditor.getRenderable();
        FactorGraphInferenceResults results = FactorGraphRegistry.getRegistry().getInferenceResults(pathway);
        if (results == null) {
            JOptionPane.showMessageDialog(this, 
                                          "Graphical model analysis has not been performed for this pathway.\n" + 
                                          "Please perform the analysis first.",
                                          "No Results",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            // Get a file
            File file = PlugInUtilities.getAnalysisFile("Save Analysis Results",
                                                        FileUtil.SAVE);
            if (file == null) {
                return; // Canceled
            }
            // Have to make sure all ids are not null and unique
            results.getFactorGraph().setIdsInFactors();
            RenderablePathway diagram = (RenderablePathway) pathwayEditor.getRenderable();
            results.setPathwayDiagramId(diagram.getReactomeDiagramId());
            FactorGraphInferenceResultsIO writer = new FactorGraphInferenceResultsIO();
            writer.write(results, file);
            //          JAXBContext jaxbContext = JAXBContext.newInstance(FactorGraphInferenceResults.class);
            //          Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            //          jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            //            jaxbMarshaller.marshal(results, file);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot save analysis results: " + e,
                                          "Error in Saving Results", 
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Perform factor graph analysis without displaying the converted factor graph in Cytoscape's
     * desktop as the normal function.
     */
    private void runFactorGraphAnalysis() {
        FactorGraphAnalyzer analyzer = getFactorGraphAnalyzer();
        analyzer.startAnalysis();
    }
    
    private String formatGenesText(String genes) {
        PathwayEnrichmentHighlighter hiliter = PathwayEnrichmentHighlighter.getHighlighter();
        Set<String> hitGenes = hiliter.getHitGenes();
        // Add an extra space before delimit , and |
        StringBuilder builder = new StringBuilder();
        String gene = "";
        builder.append("<center><a href=\"");
        for (char c : genes.toCharArray()) {
            if (c == '|' || c == ',') {
                builder.append(gene).append("\">").append(gene).append("</a>").append(c).append(" <a ");
                if (hitGenes.contains(gene))
                    builder.append("style=\"background-color:purple;color:white;\" ");
                builder.append(" href=\""); 
                gene = "";
            }
            else
                gene += c;
        }
        builder.append(gene).append("\">").append(gene).append("</a></center>");
        return builder.toString();
    }
    
    @Override
    protected PathwayEditor createPathwayEditor() {
        return new CyPathwayEditor();
    }
    
    private void handleGraphSelectionEvent(GraphEditorActionEvent event) {
        // Delegate this event to other registered listeners
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        try {
            ServiceReference[] references = context.getServiceReferences(GraphEditorActionListener.class.getName(), null);
            if (references != null && references.length > 0) {
                // Here a new event is used for easy handling
                GraphEditorActionEvent event1 = new GraphEditorActionEvent(getPathwayEditor(), ActionType.SELECTION);
                for (ServiceReference reference : references) {
                    GraphEditorActionListener l = (GraphEditorActionListener) context.getService(reference);
                    l.graphEditorAction(event1);
                    context.ungetService(reference);
                }
            }
        }
        catch(InvalidSyntaxException e) {
            e.printStackTrace();
        }
        @SuppressWarnings("unchecked")
        List<Renderable> selection = getPathwayEditor().getSelection();
        EventSelectionEvent selectionEvent = new EventSelectionEvent();
        List<Long> relatedIds = getRelatedPathwaysIds();
        Long pathwayId = null;
        if (relatedIds.size() > 0)
            pathwayId = relatedIds.get(0);
        selectionEvent.setParentId(pathwayId);
        // Get the first selected event
        Renderable firstEvent = null;
        if (selection != null && selection.size() > 0) {
            for (Renderable r : selection) {
                if (r.getReactomeId() != null && (r instanceof HyperEdge || r instanceof ProcessNode)) {
                    firstEvent = r;
                    break;
                }
            }
        }
        if (firstEvent == null) {
            selectionEvent.setEventId(pathwayId);
            selectionEvent.setIsPathway(true);
        }
        else {
            selectionEvent.setEventId(firstEvent.getReactomeId());
            selectionEvent.setIsPathway(firstEvent instanceof ProcessNode);
        }
        PathwayDiagramRegistry.getRegistry().getEventSelectionMediator().propageEventSelectionEvent(CyZoomablePathwayEditor.this,
                                                                                                    selectionEvent);
    }

    /**
     * A customized PathwayEditor in order to do something specifial for PathwayDiagram displayed in
     * Cytoscape.
     * @author gwu
     */
    private class CyPathwayEditor extends PathwayEditor {
        // Record newly added RenderableInteraction for new drawing
        private List<FIRenderableInteraction> overlaidFIs;
        
        public CyPathwayEditor() {
            setEditable(false); // Only used as a pathway diagram view and not for editing
        }

        @Override
        public Rectangle getVisibleRect() {
            if (getParent() instanceof JViewport) {
                JViewport viewport = (JViewport) getParent();
                Rectangle parentBounds = viewport.getBounds();
                Rectangle visibleRect = new Rectangle();
                Point position = SwingUtilities.convertPoint(viewport.getParent(),
                                                             parentBounds.x,
                                                             parentBounds.y,
                                                             this);
                visibleRect.x = position.x;
                visibleRect.y = position.y;
                visibleRect.width = parentBounds.width;
                visibleRect.height = parentBounds.height;
                return visibleRect;
            }
            return super.getVisibleRect();
        }
        
        /**
         * Remove FIs attached to a Node specified by r.
         * @param r
         */
        public void removeFIs(Node r) {
            if (overlaidFIs == null || overlaidFIs.size() == 0)
                return;
            List<RenderableInteraction> toBeRemoved = new ArrayList<RenderableInteraction>();
            for (RenderableInteraction fi : overlaidFIs) {
                if (fi.getInputNodes().contains(r) || fi.getOutputNodes().contains(r)) {
                    toBeRemoved.add(fi);
                    // Deleted added gene if it is not connected to others
                    Node input = fi.getInputNode(0);
                    Node output = fi.getOutputNode(0);
                    delete(fi);
                    Node geneNode = null;
                    if (input == r)
                        geneNode = output;
                    else
                        geneNode = input;
                    if (geneNode.getConnectedReactions().size() == 0)
                        delete(geneNode);
                }
            }
            overlaidFIs.removeAll(toBeRemoved);
            repaint(getVisibleRect());
        }
        
        /**
         * Remove all overlaid FIs.
         */
        public void removeFIs() {
            if (overlaidFIs == null || overlaidFIs.size() == 0)
                return;
           for (RenderableInteraction fi : overlaidFIs) {
               Node input = fi.getInputNode(0);
               Node output = fi.getOutputNode(0);
               delete(fi);
               // Check if a connected node should be deleted too
               if (input.getReactomeId() == null && input.getConnectedReactions().size() == 0)
                   delete(input);
               if (output.getReactomeId() == null && output.getConnectedReactions().size() == 0)
                   delete(output);
           }
           overlaidFIs.clear();
           repaint(getVisibleRect());
        }
        
        /**
         * Check if there is any FI overlaid for the specified Renderable object.
         * @param r usually should be a Node object.
         * @return
         */
        public boolean hasFIsOverlaid(Renderable r) {
            if (overlaidFIs == null || overlaidFIs.size() == 0)
                return false;
            for (RenderableInteraction fi : overlaidFIs) {
                if (fi.getInputNodes().contains(r) || fi.getOutputNodes().contains(r))
                    return true;
            }
            return false;
        }
        
        /**
         * Check if there is any FI overlaid
         * @return
         */
        public boolean hasFIsOverlaid() {
            if (overlaidFIs == null || overlaidFIs.size() == 0)
                return false;
            return true;
        }
        
        @Override
        public void insertEdge(HyperEdge edge, boolean useDefaultInsertPos) {
            super.insertEdge(edge, useDefaultInsertPos);
            if (edge instanceof FIRenderableInteraction) {
                if (overlaidFIs == null)
                    overlaidFIs = new ArrayList<FIRenderableInteraction>();
                overlaidFIs.add((FIRenderableInteraction)edge);
            }
        }
        
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (overlaidFIs == null || overlaidFIs.size() == 0)
                return;
            // The following code is based on DiseasePathwayImageEditor
            // in package org.gk.graphEditor.
            // Create a background
            Graphics2D g2 = (Graphics2D) g;
            // Draw a transparent background: light grey
            Color color = new Color(204, 204, 204, 175);
            g2.setPaint(color);
            Dimension size = getPreferredSize();
            // Preferred size has been scaled. Need to scale it back
            g2.fillRect(0, 
                        0, 
                        (int)(size.width / scaleX + 1.0d), 
                        (int)(size.height / scaleY + 1.0d));
            // Draw overlaid FIs and their associated Objects
            Set<Node> nodes = new HashSet<Node>();
            for (RenderableInteraction fi : overlaidFIs) {
                nodes.addAll(fi.getInputNodes());
                nodes.addAll(fi.getOutputNodes());
            }
            Rectangle clip = g.getClipBounds();
            for (Node node : nodes) {
                node.validateBounds(g);
                if (clip.intersects(node.getBounds()))
                    node.render(g);
            }
            for (RenderableInteraction fi : overlaidFIs) {
                fi.validateConnectInfo();
                if (clip.intersects(fi.getBounds()))
                    fi.render(g);
            }
        }

        /**
         * Override so that nothing is needed to be done for added RenderableInteractions.
         */
        @Override
        protected void validateCompartmentSetting(Renderable r) {
            return; 
        }
    }
    
}
