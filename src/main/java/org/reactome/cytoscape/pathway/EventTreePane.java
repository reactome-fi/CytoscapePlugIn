/*
 * Created on Jul 24, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import static org.reactome.cytoscape.service.PathwaySpecies.Homo_sapiens;
import static org.reactome.cytoscape.service.PathwaySpecies.Mus_musculus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.gk.model.ReactomeJavaConstants;
import org.gk.util.TreeUtilities;
import org.jdom.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.drug.DrugDataSource;
import org.reactome.cytoscape.drug.DrugListManager;
import org.reactome.cytoscape.pathway.GSEAPathwayAnalyzer.GeneScoreLoadingPane;
import org.reactome.cytoscape.pgm.PathwayResultSummary;
import org.reactome.cytoscape.service.GeneSetLoadingPane;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.service.ReactomeSourceView;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.cytoscape.util.SearchDialog;
import org.reactome.r3.util.FileUtility;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A customized JPanel that is used to display an event tree loaded via RESTful API.
 * @author gwu
 *
 */
public class EventTreePane extends JPanel implements EventSelectionListener {
    private PathwayEnrichmentResultPane annotationPanel;
    // This implementation should be changed in the future. This is very bad!!!
    // Synchronize to drug impact analysis result
    private List<DrugPathwayImpactResultPane> drugImpactPanes;
    JTree eventTree;
    // To control tree selection event firing
    private TreeSelectionListener selectionListener;
    private Map<String, GeneSetAnnotation> pathwayToAnnotation;
    // Cached red colors for FDRs
    private List<Color> fdrColors;
    // A color bar to show FDR values
    private JPanel fdrColorBar;
    // Use to display selected tree branch
    private JPanel selectionPane;
    private JTree selectionTree;
    // To support mouse pathways
    private JComboBox<PathwaySpecies> speciesBox;
    // The data type used for highlighting events
    private String dataType = "FDR"; // Default should be FDR
    private JLabel dataTypeLabel;
    
    /**
     * Default constructor
     */
    public EventTreePane() {
        init();
    }
    
    public void setHighlightDataType(String dataType) {
        if (this.dataType.equals(dataType))
            return;
        this.dataType = dataType;
        if (dataTypeLabel != null) 
            dataTypeLabel.setText(dataType + ": ");
    }
    
    private void init() {
        setLayout(new BorderLayout());
        // Have to override getVisibleRect() because of getBounds() in class BiModalJSplitPane.
        // See comment for class CyPathwayEditor.
        eventTree = new JTree() {
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
        };
        eventTree.setShowsRootHandles(true);
        eventTree.setRootVisible(false);
        eventTree.setExpandsSelectedPaths(true);
        // Only single selection
//        eventTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultTreeModel model = new DefaultTreeModel(root);
        eventTree.setModel(model);
        TreeCellRenderer renderer = new EventCellRenderer();
        eventTree.setCellRenderer(renderer);
        add(new JScrollPane(eventTree), BorderLayout.CENTER);
        
        initSelectionPane(renderer);
        
        installListners();
        initPathwayEnrichmments();
        
        // For some unknown reason, setLargeModel true will help to make text truncation not occurring
        // for enrichment results.
        eventTree.setLargeModel(true);
    }

    private void initSelectionPane(TreeCellRenderer renderer) {
        selectionPane = new JPanel();
        selectionPane.setBorder(BorderFactory.createEtchedBorder());
        selectionPane.setLayout(new BorderLayout());
        JLabel label = new JLabel("Selected Event Branch");
        selectionPane.add(label, BorderLayout.NORTH);
        // Make sure tree nodes cannot be collapsed. This is just a view.
        selectionTree = new JTree() {
            protected void setExpandedState(TreePath path, boolean state) {
                // Ignore all collapse requests; collapse events will not be fired
                if (state) {
                    super.setExpandedState(path, state);
                }
            }
        };
        selectionTree.setBorder(BorderFactory.createEtchedBorder());
        selectionTree.setRootVisible(false);
        DefaultTreeModel model1 = new DefaultTreeModel(new DefaultMutableTreeNode());
        selectionTree.setModel(model1);
        selectionTree.setLargeModel(true);
        selectionTree.setCellRenderer(renderer);
        selectionTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                // Just do a search for find the selected pathway in the eventTree
                TreePath path = selectionTree.getSelectionPath();
                if (path == null)
                    return;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                EventObject event = (EventObject) node.getUserObject();
                DefaultMutableTreeNode foundNode = TreeUtilities.searchNode(event, eventTree);
                if (foundNode != null) {
                    DefaultTreeModel eventModel = (DefaultTreeModel) eventTree.getModel();
                    TreePath eventPath = new TreePath(eventModel.getPathToRoot(foundNode));
                    scrollPathToVisible(eventPath);
                }
            }
        });
        
        selectionPane.add(selectionTree, BorderLayout.CENTER);
        
        add(selectionPane, BorderLayout.SOUTH);
        selectionPane.setVisible(false);
    }

    private void initPathwayEnrichmments() {
        // For easy processing
        pathwayToAnnotation = new HashMap<String, GeneSetAnnotation>();
        fdrColors = new ArrayList<Color>();
        int count = 4;
        double step = 255.0d / count;
        for (int i = 0; i < count; i++) {
            Color c = new Color((int)(step * (1 + i)), 185, 185);
            fdrColors.add(c);
        }
        fdrColorBar = new JPanel();
        fdrColorBar.setBorder(BorderFactory.createEtchedBorder());
        fdrColorBar.setLayout(new GridLayout(1, 5));
        dataTypeLabel = new JLabel(dataType + ": ");
        fdrColorBar.add(dataTypeLabel);
        String[] labels = new String[] {
                " >=0.1",
                " >=0.01",
                " >=0.001",
                " <0.001"
        };
        int i = 0;
        for (String text : labels) {
            JLabel label = new JLabel(text);
            label.setOpaque(true);
            label.setBackground(fdrColors.get(i ++));
            label.setHorizontalTextPosition(JLabel.CENTER);
            fdrColorBar.add(label);
        }
//        add(fdrColorBar, BorderLayout.NORTH);
        // Default should be disable
        fdrColorBar.setVisible(false);
        // Link the data model to the action class
        PathwayEnrichmentHighlighter highlighter = PathwayEnrichmentHighlighter.getHighlighter();
        highlighter.setPathwayToAnnotation(pathwayToAnnotation);
        // Use for species
        JPanel northPane = new JPanel();
        northPane.setLayout(new BorderLayout());
        northPane.add(createSpeciesPane(), BorderLayout.NORTH);
        northPane.add(fdrColorBar, BorderLayout.SOUTH);
        add(northPane, BorderLayout.NORTH);
    }
    
    public JComboBox<PathwaySpecies> getSpeciesBox() {
        return this.speciesBox;
    }
    
    private JPanel createSpeciesPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                           BorderFactory.createEmptyBorder(2, 1, 2, 0)));
        JLabel label = new JLabel("Pahtways for: ");
        panel.add(label);
        speciesBox = new JComboBox<PathwaySpecies>();
        speciesBox.addItem(Homo_sapiens);
        speciesBox.addItem(Mus_musculus);
        speciesBox.setSelectedIndex(0);
        panel.add(speciesBox);
        return panel;
    }
    
    private void installListners() {
        eventTree.addMouseListener(new MouseAdapter() {
            
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (e.getClickCount() == 2)
//                    loadSubPathways();
//            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showTreePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showTreePopup(e);
            }
            
        });
        
        selectionListener = new TreeSelectionListener() {
            
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                doTreeSelection();
            }
        };
        
        // In order to synchronize selection
        eventTree.addTreeSelectionListener(selectionListener);
        
        PathwayDiagramRegistry.getRegistry().getEventSelectionMediator().addEventSelectionListener(this);
    }
    
    public void addDrugImpactResultPane(DrugPathwayImpactResultPane pane) {
        if (drugImpactPanes == null)
            drugImpactPanes = new ArrayList<>();
        drugImpactPanes.add(pane);
    }
    
    public void removeDrugImpactResultPane(DrugPathwayImpactResultPane pane) {
        if (drugImpactPanes == null)
            return;
        drugImpactPanes.remove(pane);
    }
    
    @Override
    public void eventSelected(EventSelectionEvent selectionEvent) {
        eventTree.removeTreeSelectionListener(selectionListener);
        eventTree.clearSelection();
        Long containerId = selectionEvent.getParentId();
        Long eventId = selectionEvent.getEventId();
        // Check all displayed events 
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        boolean isSelected = false;
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            isSelected = selectEvent(eventId, child, model);
            if (isSelected)
                break;
        }
        if (!isSelected) {
            // Select the container instead
            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
                isSelected = selectEvent(containerId, child, model);
                if (isSelected)
                    break;
            }
        }
        showSelectionBranch();
        if (annotationPanel != null)
            annotationPanel.doTreeSelection();
        if (drugImpactPanes != null)
            drugImpactPanes.forEach(pane -> pane.doTreeSelection());
        eventTree.addTreeSelectionListener(selectionListener);
    }
    
    private boolean selectEvent(Long dbId,
                                DefaultMutableTreeNode treeNode,
                                DefaultTreeModel treeModel) {
        EventObject event = (EventObject) treeNode.getUserObject();
        if (event.dbId.equals(dbId)) {
            final TreePath treePath = new TreePath(treeModel.getPathToRoot(treeNode));
            eventTree.setSelectionPath(treePath);
            scrollPathToVisible(treePath);
            return true;
        }
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            if (selectEvent(dbId, childNode, treeModel))
                return true;
        }
        return false;
    }
    
    private void showSelectionBranch() {
        TreePath path = eventTree.getSelectionPath();
        DefaultTreeModel model = (DefaultTreeModel) selectionTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        if (path == null) {
            root.removeAllChildren();
            model.nodeStructureChanged(root);
            selectionPane.setVisible(false);
            return;
        }
        // Create a new tree branch
        DefaultMutableTreeNode preNode = root;
        root.removeAllChildren();
        for (int i = 1; i < path.getPathCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getPathComponent(i);
            DefaultMutableTreeNode copy = new DefaultMutableTreeNode();
            copy.setUserObject(node.getUserObject());
            preNode.add(copy);
            preNode = copy;
        }
        model.nodeStructureChanged(root);
        TreePath branch = new TreePath(model.getPathToRoot(preNode));
        selectionTree.setSelectionPath(branch);
        selectionTree.expandPath(branch);
        selectionPane.setVisible(true);
    }

    /**
     * Highlight pathway diagram for a selected event in the tree. The highlight (aka selection) is 
     * implemented as following:
     * 1). All PathwayInternalFrames registered will be checked.
     * 2). The selected event in a pathway diagram will be highlighted if it is drawn as a box (for pathway)
     * or as edge (for reaction)
     * 3). If the selected event is not drawn in a pathway diagram, its contained sub-pathways and reactions
     * will be checked. However, only the parent pathway diagram for the selected event will be checked.
     */
    private void doTreeSelection() {
        showSelectionBranch();
        TreePath treePath = eventTree.getSelectionPath();
        if (treePath == null)
            return;
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        EventObject selectedEvent = (EventObject) treeNode.getUserObject();
        // Have to unselect all diagram first
        PathwayDiagramRegistry registry = PathwayDiagramRegistry.getRegistry();
        registry.unSelectAllFrames();
        // Find a pathway diagram and select it
        EventObject event = null;
        for (Object obj : treePath.getPath()) {
            treeNode = (DefaultMutableTreeNode) obj;
            if (treeNode.getUserObject() == null)
                continue; // This should be the root
            // Don't need to check the last event
            event = (EventObject) treeNode.getUserObject();
            // The last event may be displayed directly 
//            if (event == selectedEvent)
//                continue;
            if (event.hasDiagram) {
                EventSelectionEvent selectionEvent = new EventSelectionEvent();
                selectionEvent.setParentId(event.dbId);
                selectionEvent.setEventId(selectedEvent.dbId);
                selectionEvent.setIsPathway(selectedEvent.isPathway);
                registry.getEventSelectionMediator().propageEventSelectionEvent(this, selectionEvent);
            }
        }
        if (registry.getSelectedPathwayFrame() == null) {
            // Need to check if a FI network view has been displayed for the event
            selectFINetworkView(event);
        }
        if (annotationPanel != null)
            annotationPanel.doTreeSelection();
        if (drugImpactPanes != null)
            drugImpactPanes.forEach(pane -> pane.doTreeSelection());
    }
    
    /**
     * Select a FI network view for the specified id.
     */
    private void selectFINetworkView(EventObject event) {
        if (event == null)
            return;
        PathwayDiagramRegistry.getRegistry().selectNetworkViewForPathway(event.dbId);
    }
    
    private void showTreePopup(MouseEvent e) {
        final EventObject event = getSelectedEvent();
        if (event == null)
            return;
        JPopupMenu popup = new JPopupMenu();
        // A simple view of the source
        JMenuItem viewReactomeSource = new JMenuItem("View Reactome Source");
        viewReactomeSource.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                ReactomeSourceView sourceView = new ReactomeSourceView();
                sourceView.viewReactomeSource(event.dbId,
                                              eventTree);
            }
        });
        popup.add(viewReactomeSource);
        // Point to Reactome web site
        JMenuItem showDetailed = new JMenuItem("View in Reactome");
        showDetailed.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                new ReactomeSourceView().viewInReactome(event.dbId, eventTree);
            }
        });
        popup.add(showDetailed);
        if (event.hasDiagram) {
            JMenuItem showDiagramMenu = new JMenuItem("Show Diagram");
            showDiagramMenu.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    showPathwayDiagram();
                }
            });
            popup.add(showDiagramMenu);
            showDiagramMenu.setEnabled(!isDiagramDisplayed());
        }
        else {
            JMenuItem viewInDiagramMenu = new JMenuItem("View in Diagram");
            viewInDiagramMenu.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    viewEventInDiagram();
                }
            });
            popup.add(viewInDiagramMenu);
            viewInDiagramMenu.setEnabled(!isDiagramDisplayed());
        }
        popup.addSeparator();
        // Add a search function
        JMenuItem searchTree = new JMenuItem("Search");
        searchTree.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchTree();
            }
        });
        popup.add(searchTree);
        JMenuItem enrichmentAnalysis = new JMenuItem("Analyze Pathway Enrichment");
        enrichmentAnalysis.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                doPathwayEnrichment();
            }
        });
        popup.add(enrichmentAnalysis);
        
        // Perform GSEA analysis
        JMenuItem gseaAnalysis = new JMenuItem("Perform GSEA Analysis");
        gseaAnalysis.addActionListener(ae -> performGSEAAnalysis());
        popup.add(gseaAnalysis);
        
        // If there is any annotation
        if (pathwayToAnnotation.size() > 0 || annotationPanel != null) {
            JMenuItem removeEnrichmentAnalysis = new JMenuItem("Remove Enrichment Results");
            removeEnrichmentAnalysis.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeEnrichmentResults();
                }
            });
            popup.add(removeEnrichmentAnalysis);
        }
        // For PGM analysis in a batch model
        JMenuItem pgmAnalysis = new JMenuItem("Run Graphical Model Analysis");
        pgmAnalysis.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                runPGMAnalysis();
            }
        });
        popup.add(pgmAnalysis);
        // For reload PGM analysis results
        JMenuItem openPGMResults = new JMenuItem("Load Graphical Model Results");
        openPGMResults.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                loadPGMResults();
            }
        });
        popup.add(openPGMResults);
        
        // For cancer drugs
        if (PlugInObjectManager.getManager().isCancerTargetEnabled()) {
            popup.addSeparator();
            JMenuItem viewCancerDrugs = new JMenuItem("View Cancer Drugs");
            viewCancerDrugs.addActionListener(ae -> viewDrugs(DrugDataSource.Targetome));
            popup.add(viewCancerDrugs);
            JMenuItem viewDrugCentralDrugs = new JMenuItem("View DrugCentral Drugs");
            viewDrugCentralDrugs.addActionListener(ae -> viewDrugs(DrugDataSource.DrugCentral));
            popup.add(viewDrugCentralDrugs);
        }
        
        // Add two new items for expanding/closing nodes
        popup.addSeparator();
        JMenuItem expandNode = new JMenuItem("Expand Pathway");
        expandNode.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (eventTree.getSelectionPaths() == null)
                    return;
                for (TreePath path : eventTree.getSelectionPaths()) {
                    TreeUtilities.expandAllNodes((DefaultMutableTreeNode) path.getLastPathComponent(),
                                                 eventTree);
                }
            }
        });
        popup.add(expandNode);
        JMenuItem collapseNode = new JMenuItem("Collapse Pathway");
        collapseNode.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (eventTree.getSelectionPaths() == null)
                    return;
                for (TreePath path : eventTree.getSelectionPaths()) {
                    TreeUtilities.collapseAllNodes((DefaultMutableTreeNode)path.getLastPathComponent(),
                                                   eventTree);
                }
            }
        });
        popup.add(collapseNode);
        popup.addSeparator();
        JMenuItem openReacfoam = new JMenuItem("Open Reactome Reacfoam");
        openReacfoam.setToolTipText("Visualize pathways in Reacfoam");
        openReacfoam.addActionListener(evt -> openReacfoam());
        popup.add(openReacfoam);
        popup.show(eventTree, e.getX(), e.getY());
    }
    
    private void openReacfoam() {
    	String analysisToken = null;
    	if (pathwayToAnnotation != null && pathwayToAnnotation.size() > 0)
            analysisToken = "reactomefiviz";
    	PlugInUtilities.openReacfoam(dataType,
    							     analysisToken,
    							     ((PathwaySpecies)speciesBox.getSelectedItem()).getDBID(),
    							     false);
    }
    
    private void loadPGMResults() {
        // Give it a warning
        int reply = JOptionPane.showConfirmDialog(this,
                                                  "The file should be generated by using \"Export Results\" from a previous\n" + 
                                                          "\"Run Graphical Model Analysis\".",
                                                          "Load Graphical Model Results",
                                                          JOptionPane.OK_CANCEL_OPTION);
        if (reply == JOptionPane.CANCEL_OPTION)
            return; // The user has chosen "cancel".
        ServiceReference reference = null;
        try {
            // Get a file
            Collection<FileChooserFilter> filters = new ArrayList<FileChooserFilter>();
            FileChooserFilter filter = new FileChooserFilter("Graphical Model Analysis Results",
                                                             new String[]{"txt", ".txt"});
            filters.add(filter);
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            reference = context.getServiceReference(FileUtil.class.getName());
            FileUtil fileUtil = (FileUtil) context.getService(reference);
            File file = fileUtil.getFile(PlugInObjectManager.getManager().getCytoscapeDesktop(), 
                                         "Load Graphical Model Results", 
                                         FileUtil.LOAD,
                                         filters);
            if (file == null)
                return;
            // Open the file and load the results
            FileUtility fu = new FileUtility();
            fu.setInput(file.getAbsolutePath());
            String line = fu.readLine(); // Escape the first header line
            List<PathwayResultSummary> resultList = new ArrayList<PathwayResultSummary>();
            int i = 0;
            while ((line = fu.readLine()) != null) {
                String[] tokens = line.split("\t");
                PathwayResultSummary result = new PathwayResultSummary();
                i = 0;
                result.setPathwayName(tokens[i++]);
                result.setAverageUpIPAs(new Double(tokens[i++]));
                result.setAverageDownIPAs(new Double(tokens[i++]));
                result.setCombinedPValue(new Double(tokens[i++]));
                result.setMinPValue(new Double(tokens[i++]));
                result.setCombinedPValueFDR(new Double(tokens[i++]));
                result.setMinPValueFDR(new Double(tokens[i++]));
                resultList.add(result);
            }
            fu.close();
            // Display these results
            FactorGraphBatchAnalyzer analyzer = new FactorGraphBatchAnalyzer();
            analyzer.setEventPane(this);
            analyzer.showResults(resultList);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                                          "Error in loading graphical model analysis results: " + e,
                                          "Error in Loading Graphical Model Results",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Perform a batch PGM analysis
     */
    private void runPGMAnalysis() {
        FactorGraphBatchAnalyzer analyzer = new FactorGraphBatchAnalyzer();
        analyzer.setEventPane(this);
        analyzer.startAnalysis();
    }
    
    private void viewDrugs(final DrugDataSource dataSource) {
        Thread t = new Thread() {
            public void run() {
                PathwayEnrichmentAnalysisTask enrichmentTask = new PathwayEnrichmentAnalysisTask();
                enrichmentTask.setEventPane(EventTreePane.this);
                DrugListManager.getManager().setEnrichmentTask(enrichmentTask);
                DrugPathwayImpactAnalysisAction impactAnlysisAction = new DrugPathwayImpactAnalysisAction();
                impactAnlysisAction.setEventPane(EventTreePane.this);
                DrugListManager.getManager().setRunImpactAnalysisAction(impactAnlysisAction);
                DrugListManager.getManager().listDrugs(dataSource);
            }
        };
        t.start();
    }
    
    private void performGSEAAnalysis() {
        GeneScoreLoadingPane loadingPane = new GeneScoreLoadingPane(EventTreePane.this);
        if (!loadingPane.isOkClicked() || loadingPane.getSelectedFile() == null)
            return; // Cancelled or nothing is input
        String fileName = loadingPane.getSelectedFile();
        int minSize = Integer.parseInt(loadingPane.getMinTF().getText().trim());
        int maxSize = Integer.parseInt(loadingPane.getMaxTF().getText().trim());
        int permutation = Integer.parseInt(loadingPane.getPermutationTF().getText().trim());
        GSEAPathwayAnalyzer analyzer = new GSEAPathwayAnalyzer();
        analyzer.performGSEAAnalysis(fileName,
                                     minSize,
                                     maxSize, 
                                     permutation,
                                     this);
    }
    
    /**
     * A helper method for performing pathway enrichment analysis.
     */
    private void doPathwayEnrichment() {
        try {
            GeneSetLoadingPane loadingPane = new GeneSetLoadingPane(EventTreePane.this);
            String genes = loadingPane.getGenes();
            if (genes == null)
                return;
            
            // Use a Task will make some text in the tree truncated for displaying
            // the enrichment result. This is more like a GUI threading issue.
            @SuppressWarnings("rawtypes")
            TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
            PathwayEnrichmentAnalysisTask task = new PathwayEnrichmentAnalysisTask();
            task.setGeneList(genes);
            task.setEventPane(this);
            taskManager.execute(new TaskIterator(task));
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(this, 
                                          "Error in Pathway Enrichment Analysis",
                                          "Error in pathway enrichment analysis: " + e,
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Open a new pathway diagram for the selected pathway in the tree.
     */
    private void showPathwayDiagram() {
        EventObject event = getSelectedEvent();
        if (event == null)
            return; // In case there is nothing selected
        PathwayDiagramRegistry.getRegistry().showPathwayDiagram(event.dbId, event.name);
    }
    
    void viewEventInDiagram() {
        if (eventTree.getSelectionCount() == 0)
            return;
        TreePath path = eventTree.getSelectionPath();
        EventObject event = null;
        // Find the pathway with its hasDiagram true
        for (int i = path.getPathCount(); i > 0; i--) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getPathComponent(i - 1);
            event = (EventObject) treeNode.getUserObject();
            if (event.hasDiagram) {
                // Don't want to make any change in the tree's selection
                EventSelectionMediator mediator = PathwayDiagramRegistry.getRegistry().getEventSelectionMediator();
                mediator.removeEventSelectionListener(this);
                PathwayDiagramRegistry.getRegistry().showPathwayDiagram(event.dbId, event.name);
                mediator.addEventSelectionListener(this);
                // Fire a new selection event for selecting selected events
                doTreeSelection();
                break; // Break it to avoid multiple showing pathway diagrams.
            }
        }
    }
    
    boolean isDiagramDisplayed() {
        if (eventTree.getSelectionCount() == 0)
            return true;
        TreePath path = eventTree.getSelectionPath();
        for (int i = path.getPathCount(); i > 0; i--) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getPathComponent(i - 1);
            EventObject event = (EventObject) treeNode.getUserObject();
            if (event.hasDiagram) {
                return PathwayDiagramRegistry.getRegistry().isPathwayDisplayed(event.dbId);
            }
        }
        return false;
    }

    private EventObject getSelectedEvent() {
        if (eventTree.getSelectionCount() == 0)
            return null;
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) eventTree.getSelectionPath().getLastPathComponent();
        EventObject event = (EventObject) selectedNode.getUserObject();
        return event;
    }
    
    /**
     * Load the top-level pathways from the Reactome RESTful API.
     */
    public void loadFrontPageItems() throws Exception {
        Element root = ReactomeRESTfulService.getService().frontPageItems();
        List<?> children = root.getChildren();
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        DefaultMutableTreeNode treeRoot = (DefaultMutableTreeNode) model.getRoot();
        treeRoot.removeAllChildren(); // Just in case there is anything there.
        for (Object obj : children) {
            Element elm = (Element) obj;
            EventObject event = parseFrontPageEvent(elm);
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode();
            treeNode.setUserObject(event);
            treeRoot.add(treeNode);
        }
        // Needs an update
        model.nodeStructureChanged(treeRoot);
    }
    
    /**
     * Set all pathways encoded in an JDOM Element.
     * @param root
     * @throws Exception
     */
    public void setAllPathwaysInElement(Element root) throws Exception {
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        DefaultMutableTreeNode treeRoot = (DefaultMutableTreeNode) model.getRoot();
        treeRoot.removeAllChildren(); // Just in case there is anything there.
        List<?> children = root.getChildren();
        for (Object obj : children) {
            Element elm = (Element) obj;
            // To avoid disease for the time being
            String name = elm.getAttributeValue("displayName");
            if (name.equals("Disease")) {
                // Want to add Infectious disease only, but not pull it up to 
                // keep the order of front page items.
                cleanUpDiseaseElm(elm);
                if (elm.getChildren() != null && elm.getChildren().size() > 0)
                    addEvent(treeRoot, elm); 
                continue;
            }
            addEvent(treeRoot, elm);
        }
        model.nodeStructureChanged(treeRoot);
    }
    
    /**
     * Keep the infection disease pathways only. We don't want to pull this pathway
     * to the top as one front page item to avoid breaking the order of pathways.
     * @param elm
     */
    @SuppressWarnings("unchecked")
    private void cleanUpDiseaseElm(Element elm) {
        List<Element> children = elm.getChildren();
        Element infectiousDisease = null;
        for (Element child : children) {
            String name = child.getAttributeValue("displayName");
            if (name.equals("Infectious disease")) {
                infectiousDisease = child;
                break;
            }
        }
        elm.removeContent(); // Remove all
        if (infectiousDisease != null)
            elm.addContent(infectiousDisease);
    }
    
    /**
     * Reset the tree to keep the memory usage minimum.
     */
    public void resetTree() {
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        root.removeAllChildren();
        model.nodeStructureChanged(root);
    }
    
    protected void removeEnrichmentResults() {
        if (annotationPanel != null) {
            annotationPanel.close();
            annotationPanel = null;
        }
        pathwayToAnnotation.clear();
        eventTree.repaint(eventTree.getVisibleRect());
        fdrColorBar.setVisible(false);
        // Remove highlights in opened pathway views
        PathwayDiagramRegistry.getRegistry().removeHighlightPathwayViews();
        firePropertyChange("showPathwayEnrichments", true, false);
    }
    
    public JTree getEventTree() {
        return eventTree;
    }
    
    public void showPathwayEnrichments(List<GeneSetAnnotation> annotations) {
        showPathwayEnrichments(annotations, true);
    }
    
    /**
     * Display pathway enrichment analysis in the pathway tree.
     * @param annotations
     */
    public void showPathwayEnrichments(List<GeneSetAnnotation> annotations, boolean needEnrichmentPane) {      
        if (annotations == null || annotations.size() == 0) {
            // Show a message
            JOptionPane.showMessageDialog(this,
                                          "It appears that there is no gene in your list can be mapped into any\n"
                                          + "Reactome pathway. Please make sure your file format is correct and\n"
                                          + "note that this app can support human genes only.",
                                          "Empty Result",
                                          JOptionPane.INFORMATION_MESSAGE);
                                                  
            return;
        }
        // Call this method first so that scrollPathToVisible may work correctly.
        fdrColorBar.setVisible(true);
        pathwayToAnnotation.clear();
        final DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) eventTree.getModel().getRoot();
        List<TreePath> paths = new ArrayList<TreePath>();
        for (GeneSetAnnotation annotation : annotations) {
            String pathway = annotation.getTopic();
            pathwayToAnnotation.put(pathway, annotation);
            searchPathway(root, pathway, model, paths);
        }
        showSearchResults(paths, true);
        
        if (!needEnrichmentPane)
            return;
        
        // Show annotations in a list
        if (annotationPanel == null) {
            annotationPanel = new PathwayEnrichmentResultPane(this, "Reactome Pathway Enrichment");
        }
        annotationPanel.setGeneSetAnnotations(annotations);
        // Need to select it
        PlugInObjectManager.getManager().selectCytoPane(annotationPanel, CytoPanelName.SOUTH);
        // Need to highlight any opened pathway diagrams or FI diagram view
        // Call this in case the value is not there any more.
        PathwayEnrichmentHighlighter.getHighlighter().setPathwayToAnnotation(pathwayToAnnotation);
        PathwayDiagramRegistry.getRegistry().highlightPathwayViews();
        // Fire a property change action
        firePropertyChange("showPathwayEnrichments", false, true);
    }
    
    public Map<String, GeneSetAnnotation> getPathwayToAnnotation() {
        return this.pathwayToAnnotation;
    }
    
    public void setAnnotationPane(PathwayEnrichmentResultPane pane) {
        this.annotationPanel = pane;
    }
    
    public Map<String, EventObject> grepEventNameToObject() {
        Map<String, EventObject> nameToObject = new HashMap<>();
        grepEventObjectInfo(nameToObject, 
                            eventObject -> nameToObject.put(eventObject.name, eventObject));
        return nameToObject;
    }
    
    private void grepEventObjectInfo(Map<?, ?> map,
                                     Consumer<EventObject> putFunc) {
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        Set<DefaultMutableTreeNode> current = new HashSet<>();
        for (int i = 0; i < root.getChildCount(); i++) {
            current.add((DefaultMutableTreeNode)root.getChildAt(i));
        }
        Set<DefaultMutableTreeNode> next = new HashSet<>();
        while (current.size() > 0) {
            for (DefaultMutableTreeNode node : current) {
                if (node.getUserObject() instanceof EventObject) {
                    EventObject eventObject = (EventObject) node.getUserObject();
                    putFunc.accept(eventObject);
                    if (node.getChildCount() > 0) {
                        for (int i = 0; i < node.getChildCount(); i++)
                            next.add((DefaultMutableTreeNode)node.getChildAt(i));
                    }
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
    }
    
    public Map<String, String> grepStIdToName() {
        Map<String, String> stIdToName = new HashMap<>();
        grepEventObjectInfo(stIdToName,
                            eventObject -> stIdToName.put(eventObject.stId, eventObject.name));
        return stIdToName;
    }
    
    private void showSearchResults(List<TreePath> paths,
                                   boolean keepOriginalSelectPath) {
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        final TreePath selectedPath = eventTree.getSelectionPath();
        if (!keepOriginalSelectPath)
            eventTree.clearSelection();
        for (TreePath path : paths) {
            // Actually we only need to expand the parent TreeNode only
            DefaultMutableTreeNode last = (DefaultMutableTreeNode) path.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) last.getParent();
            if (parent == null)
                eventTree.expandPath(path);
            else {
                TreePath parentPath = new TreePath(model.getPathToRoot(parent));
                eventTree.expandPath(parentPath);
            }
        }
        // Scroll to the first path
        if (keepOriginalSelectPath && selectedPath != null) {
            // Since tree expanding, we need to scroll back to the original selected path
            SwingUtilities.invokeLater(new Runnable() {
                
                @Override
                public void run() {
                    // Need to do a re-selection to force selection is synchronized
//                    eventTree.setSelectionPath(selectedPath);
                    eventTree.scrollPathToVisible(selectedPath);
                    if (annotationPanel != null) // Force selection in the table
                        annotationPanel.doTreeSelection();
                    if (drugImpactPanes != null)
                        drugImpactPanes.forEach(pane -> pane.doTreeSelection());
                }
            });
        }
        else {
            for (TreePath path : paths)
                eventTree.addSelectionPath(path);
            final TreePath firstPath = paths.get(0);
            scrollPathToVisible(firstPath);
        }
    }

    private void scrollPathToVisible(final TreePath firstPath) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                eventTree.scrollPathToVisible(firstPath);
            }
        });
    }
    
    void searchPathway(DefaultMutableTreeNode treeNode,
                       String pathway,
                       DefaultTreeModel model,
                       List<TreePath> selectedPaths) {
        searchPathway(treeNode, 
                      pathway,
                      false,
                      true,
                      model, 
                      selectedPaths);
    }
    
    private void searchPathway(DefaultMutableTreeNode treeNode,
                               String pathway,
                               boolean ignoreCase,
                               boolean needWholeName,
                               DefaultTreeModel model,
                               List<TreePath> selectedPaths) {
        if (treeNode.getUserObject() != null && treeNode.getUserObject() instanceof EventObject) {
            EventObject event = (EventObject) treeNode.getUserObject();
            if (needWholeName) {
                if (ignoreCase) {
                    if (event.name.equalsIgnoreCase(pathway))
                        selectedPaths.add(new TreePath(model.getPathToRoot(treeNode)));
                }
                else if (event.name.equals(pathway)) {
                    selectedPaths.add(new TreePath(model.getPathToRoot(treeNode)));
                }
            }
            else {
                if (ignoreCase) {
                    if (event.name.toLowerCase().contains(pathway.toLowerCase()))
                        selectedPaths.add(new TreePath(model.getPathToRoot(treeNode)));
                }
                else if (event.name.contains(pathway)) {
                    selectedPaths.add(new TreePath(model.getPathToRoot(treeNode)));
                }
            }
        }
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            searchPathway(child,
                          pathway,
                          ignoreCase,
                          needWholeName,
                          model,
                          selectedPaths);
        }
    }
    
    private void searchTree() {
        JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
        SearchDialog dialog = new SearchDialog(parentFrame);
        dialog.setTitle("Search Events");
        dialog.setLabel("Search pathways and reactions:");
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOKClicked())
            return;
        String key = dialog.getSearchKey();
        boolean needWholeName = dialog.isWholeNameNeeded();
        searchPathway(key, needWholeName);
    }

    /**
     * Search pathways for a string key.
     * @param key
     * @param needWholeName
     */
    public void searchPathway(String key, boolean needWholeName) {
        List<TreePath> results = new ArrayList<TreePath>();
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        searchPathway(root,
                      key, 
                      true,
                      needWholeName,
                      model, 
                      results);
        if (results.size() == 0) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot find any pathway or reaction for \"" + key + "\".",
                                          "Empty Result", 
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showSearchResults(results, false);
    }
    
    private void addEvent(DefaultMutableTreeNode parentNode,
                          Element eventElm) {
        EventObject event = parseEvent(eventElm);
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode();
        treeNode.setUserObject(event);
        parentNode.add(treeNode);
        List<?> children = eventElm.getChildren();
        if (children == null || children.size() == 0)
            return;
        for (Object obj : children) {
            Element childElm = (Element) obj;
            addEvent(treeNode, childElm);
        }
    }

    /**
     * Parse an XML element for an EventObject object.
     * @param elm
     * @return
     */
    private EventObject parseFrontPageEvent(Element elm) {
        String dbId = elm.getChildText("dbId");
        String name = elm.getChildText("displayName");
        EventObject event = new EventObject();
        event.dbId = new Long(dbId);
        event.name = name;
        String clsName = elm.getChildText("schemaClass");
        if (clsName.equals(ReactomeJavaConstants.Pathway))
            event.isPathway = true;
        else
            event.isPathway = false;
        String hasDiagram = elm.getChildText("hasDiagram");
        if (hasDiagram != null)
            event.hasDiagram = hasDiagram.equals("true") ? true : false;
        return event;
    }
    
    private EventObject parseEvent(Element elm) {
        String dbId = elm.getAttributeValue("dbId");
        String name = elm.getAttributeValue("displayName");
        String stId = elm.getAttributeValue("stId");
        EventObject event = new EventObject();
        event.dbId = new Long(dbId);
        event.name = name;
        event.stId = stId;
        String clsName = elm.getName();
        if (clsName.equals(ReactomeJavaConstants.Pathway))
            event.isPathway = true;
        else
            event.isPathway = false;
        String hasDiagram = elm.getAttributeValue("hasDiagram");
        if (hasDiagram != null)
            event.hasDiagram = hasDiagram.equals("true") ? true : false;
        return event;
    }
    
    @ApiModel(value = "Reactome Event", description = "An event may be a pathway or reaction.")
    public static class EventObject {
        String name;
        @ApiModelProperty(value = "Reactome internal Id")
        Long dbId;
        @ApiModelProperty(value = "Reactome stable Id")
        String stId;
        boolean isPathway;
        boolean hasDiagram;
        @ApiModelProperty(value = "Contained Events")
        private List<EventObject> children;
        
        @Override
        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStId() {
            return stId;
        }

        public void setStId(String stId) {
            this.stId = stId;
        }

        public Long getDbId() {
            return dbId;
        }

        public void setDbId(Long dbId) {
            this.dbId = dbId;
        }

        public boolean isPathway() {
            return isPathway;
        }

        public void setPathway(boolean isPathway) {
            this.isPathway = isPathway;
        }

        public boolean isHasDiagram() {
            return hasDiagram;
        }

        public void setHasDiagram(boolean hasDiagram) {
            this.hasDiagram = hasDiagram;
        }

        public List<EventObject> getChildren() {
            return children;
        }

        public void setChildren(List<EventObject> children) {
            this.children = children;
        }
        
        public void addChild(EventObject child) {
            if (children == null)
                children = new ArrayList<>();
            children.add(child);
        }
    }
    
    /**
     * A customized TreeCellRenderer in order to show icons.
     * @author gwu
     *
     */
    private class EventCellRenderer extends DefaultTreeCellRenderer {
        private Icon pathwayIcon;
        private Icon reactionIcon;
        
        public EventCellRenderer() {
            super();
            pathwayIcon = new ImageIcon(getClass().getResource("Pathway.gif"));
            reactionIcon = new ImageIcon(getClass().getResource("Reaction.gif"));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, 
                                                      Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf, int row,
                                                      boolean hasFocus) {
            Component comp = super.getTreeCellRendererComponent(tree, 
                                                                value, 
                                                                sel,
                                                                expanded,
                                                                leaf,
                                                                row, 
                                                                hasFocus);
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
            EventObject event = (EventObject) treeNode.getUserObject();
            if (event == null)
                return comp; // For the default root?
            // Show Reactome pathway enrichment results
            GeneSetAnnotation annotation = pathwayToAnnotation.get(event.name);
            if (annotation == null) {
                setText(event.name);
                setBackgroundNonSelectionColor(eventTree.getBackground());
            }
            else {
                setBackgroundNonSelectionColor(getFDRColor(annotation));
                String fdr = annotation.getFdr();
                if (!fdr.startsWith("<")) {
                    fdr = PlugInUtilities.formatProbability(new Double(fdr));
                }
                setText(event.name + " (" + dataType + ": " + fdr + ")");
            }
            if (event.isPathway)
                setIcon(pathwayIcon);
            else
                setIcon(reactionIcon);
            return comp;
        }
        
        private Color getFDRColor(GeneSetAnnotation annotation) {
            if (annotation.getFdr().startsWith("<"))
                return fdrColors.get(3);
            else {
                Double value = new Double(annotation.getFdr());
                if (value >= 0.1d)
                    return fdrColors.get(0);
                if (value >= 0.01d)
                    return fdrColors.get(1);
                if (value >= 0.001d)
                    return fdrColors.get(2);
                return fdrColors.get(3);
            }
        }
    }
}
