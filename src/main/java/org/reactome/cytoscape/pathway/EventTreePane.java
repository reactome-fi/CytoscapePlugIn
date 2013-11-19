/*
 * Created on Jul 24, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.jdom.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.util.FileUtility;

/**
 * A customized JPanel that is used to display an event tree loaded via RESTful API.
 * @author gwu
 *
 */
public class EventTreePane extends JPanel implements EventSelectionListener {
    private JTree eventTree;
    // To control tree selection event firing
    private TreeSelectionListener selectionListener;
    
    /**
     * Default constructor
     */
    public EventTreePane() {
        init();
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
        installListners();
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
        eventTree.addTreeSelectionListener(selectionListener);
    }
    
    private boolean selectEvent(Long dbId,
                                DefaultMutableTreeNode treeNode,
                                DefaultTreeModel treeModel) {
        EventObject event = (EventObject) treeNode.getUserObject();
        if (event.dbId.equals(dbId)) {
            TreePath treePath = new TreePath(treeModel.getPathToRoot(treeNode));
            eventTree.setSelectionPath(treePath);
            eventTree.scrollPathToVisible(treePath);
            return true;
        }
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            if (selectEvent(dbId, childNode, treeModel))
                return true;
        }
        return false;
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
        TreePath treePath = eventTree.getSelectionPath();
        if (treePath == null)
            return;
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        EventObject selectedEvent = (EventObject) treeNode.getUserObject();
        // Find a pathway diagram and select it
        for (Object obj : treePath.getPath()) {
            treeNode = (DefaultMutableTreeNode) obj;
            if (treeNode.getUserObject() == null)
                continue; // This should be the root
            // Don't need to check the last event
            EventObject event = (EventObject) treeNode.getUserObject();
            // The last event may be displayed directly 
//            if (event == selectedEvent)
//                continue;
            if (event.hasDiagram) {
                EventSelectionEvent selectionEvent = new EventSelectionEvent();
                selectionEvent.setParentId(event.dbId);
                selectionEvent.setEventId(selectedEvent.dbId);
                selectionEvent.setIsPathway(selectedEvent.isPathway);
                PathwayDiagramRegistry.getRegistry().getEventSelectionMediator().propageEventSelectionEvent(this,
                                                                                                            selectionEvent);
            }
        }
    }
    
//    /**
//     * Load sub-pathways for a selected pathway.
//     */
//    @SuppressWarnings("rawtypes")
//    private void loadSubPathways() {
//        EventObject event = getSelectedEvent();
//        if (event == null || event.isLoaded)
//            return;
//        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
//        Task task = new LoadSubPathwayTask(event.dbId);
//        taskManager.execute(new TaskIterator(task));
//        event.isLoaded = true;
//    }
    
    /**
     * Add sub-pathways from a JDOM Element returned from a RESTful API call to a
     * selected TreeNode.
     * @param root
     */
    private void addSubPathways(Element root) {
        if (eventTree.getSelectionCount() == 0)
            return;
        TreePath treePath = eventTree.getSelectionPath();
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        List<?> children = root.getChildren(ReactomeJavaConstants.hasEvent);
        for (Object obj : children) {
            Element child = (Element) obj;
            EventObject event = parseFrontPageEvent(child);
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(event);
            treeNode.add(childNode);
        }
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        model.nodeChanged(treeNode);
        // Expand newly expanded pathway node
        eventTree.expandPath(treePath);
    }
    
    private void showTreePopup(MouseEvent e) {
        final EventObject event = getSelectedEvent();
        if (event == null)
            return;
        JPopupMenu popup = new JPopupMenu();
        // Point to Reactome web site
        JMenuItem showDetailed = new JMenuItem("View in Reactome");
        showDetailed.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                String reactomeURL = PlugInObjectManager.getManager().getProperties().getProperty("ReactomeURL");
                String url = reactomeURL + event.dbId;
                PlugInUtilities.openURL(url);
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
        }
        popup.addSeparator();
        JMenuItem enrichmentAnalysis = new JMenuItem("Analyze Pathway Enrichment");
        enrichmentAnalysis.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                doPathwayEnrichment();
            }
        });
        popup.add(enrichmentAnalysis);
        popup.show(eventTree, e.getX(), e.getY());
    }
    
    /**
     * A helper method for performing pathway enrichment analysis.
     */
    private void doPathwayEnrichment() {
        GeneSetLoadingPane loadingPane = new GeneSetLoadingPane();
        if (!loadingPane.isOkClicked() || loadingPane.getSelectedFile() == null)
            return;
        String fileName = loadingPane.getSelectedFile();
        String format = loadingPane.getFileFormat();
        try {
            // Need to parse the genes to create a list of genes in a line delimited format
            FileUtility fu = new FileUtility();
            fu.setInput(fileName);
            StringBuilder builder = new StringBuilder();
            String line = null;
            if (format.equals("line")) {
                while ((line = fu.readLine()) != null) {
                    builder.append(line.trim()).append("\n");
                }
            }
            else {
                line = fu.readLine();
                String[] tokens = null;
                if (format.equals("comma"))
                    tokens = line.split(",( )?");
                else
                    tokens = line.split("\t");
                for (String token : tokens)
                    builder.append(token).append("\n");
            }
            fu.close();
            @SuppressWarnings("rawtypes")
            TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
            PathwayEnrichmentAnalysisTask task = new PathwayEnrichmentAnalysisTask();
            task.setGeneList(builder.toString());
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
        PathwayDiagramRegistry.getRegistry().showPathwayDiagram(event.dbId);
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
            addEvent(treeRoot, elm);
        }
        model.nodeStructureChanged(treeRoot);
    }
    
    /**
     * Display pathway enrichment analysis in the pathway tree.
     * @param annotations
     */
    public void showPathwayEnrichments(List<GeneSetAnnotation> annotations) {
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) eventTree.getModel().getRoot();
        List<TreePath> paths = new ArrayList<TreePath>();
        for (GeneSetAnnotation annotation : annotations) {
            String pathway = annotation.getTopic();
            searchPathway(root, pathway, model, paths);
        }
        eventTree.clearSelection();
        TreePath[] selection = new TreePath[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            selection[i] = paths.get(i);
        }
        eventTree.setSelectionPaths(selection);
        // Scroll to the first path
        eventTree.scrollPathToVisible(paths.get(0));
    }
    
    private void searchPathway(DefaultMutableTreeNode treeNode,
                               String pathway,
                               DefaultTreeModel model,
                               List<TreePath> selectedPaths) {
        if (treeNode.getUserObject() != null && treeNode.getUserObject() instanceof EventObject) {
            EventObject event = (EventObject) treeNode.getUserObject();
            if (event.name.equals(pathway)) {
                selectedPaths.add(new TreePath(model.getPathToRoot(treeNode)));
            }
        }
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            searchPathway(child, pathway, model, selectedPaths);
        }
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
        EventObject event = new EventObject();
        event.isLoaded = true; // Everything via this route is loaded
        event.dbId = new Long(dbId);
        event.name = name;
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
    
    private class EventObject {
        String name;
        Long dbId;
        boolean isPathway;
        boolean isLoaded; // A flag indicating if a pathway's sub-pathways have been loaded or not.
        boolean hasDiagram;
        
        @Override
        public String toString() {
            return name;
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
            if (event.isPathway)
                setIcon(pathwayIcon);
            else
                setIcon(reactionIcon);
            // Have to call this method to make paint correctly.
            return comp;
        }
    }
    
    /**
     * A customized Task for loading a selected pathway's sub-pathways.
     * @author gwu
     *
     */
    private class LoadSubPathwayTask extends AbstractTask {
        
        private Long pathwayId;
        
        LoadSubPathwayTask(Long pathwayId) {
            this.pathwayId = pathwayId;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("Load Pathway Components");
            taskMonitor.setProgress(0.0d);
            taskMonitor.setStatusMessage("Loading pathway components...");
            Element root = ReactomeRESTfulService.getService().queryById(pathwayId,
                                                                         ReactomeJavaConstants.Event);
            taskMonitor.setProgress(0.50d);
            taskMonitor.setStatusMessage("Rebuilding tree...");
            addSubPathways(root);
            taskMonitor.setProgress(1.0d);
            taskMonitor.setStatusMessage("Done!");
        }
    }
    
    /**
     * A customized JPanel for loading a gene set from a file.
     * @author gwu
     *
     */
    private class GeneSetLoadingPane extends JPanel {
        boolean isOkClicked;
        JTextField fileNameTF;
        JRadioButton commaDelimitedBtn;
        JRadioButton tabDelimitedBtn;
        JRadioButton lineDelimitedBtn;
        DialogControlPane controlPane;
        JDialog parentDialog;
        
        public GeneSetLoadingPane() {
            init();
        }
        
        public boolean isOkClicked() {
            return this.isOkClicked;
        }
        
        public String getSelectedFile() {
            String file = this.fileNameTF.getText().trim();
            if (file.length() == 0)
                return null;
            return file;
        }
        
        public String getFileFormat() {
            if (commaDelimitedBtn.isSelected())
                return "comma";
            if (tabDelimitedBtn.isSelected())
                return "tab";
            return "line";
        }
        
        private void validateOkButton() {
            if (getSelectedFile() != null)
                controlPane.getOKBtn().setEnabled(true);
            else
                controlPane.getOKBtn().setEnabled(false);
        }
        
        private void browseFile() {
            Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
            FileChooserFilter mafFilter = new FileChooserFilter("Gene Set File", ".txt");
            filters.add(mafFilter);
            
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            ServiceReference fileUtilRef = context.getServiceReference(FileUtil.class.getName());
            FileUtil fileUtil = (FileUtil) context.getService(fileUtilRef);
            File dataFile = fileUtil.getFile(parentDialog,
                                             "Select a Gene Set File", 
                                             FileUtil.LOAD, 
                                             filters);
            if (dataFile == null)
                fileNameTF.setText("");
            else
                fileNameTF.setText(dataFile.getAbsolutePath());
            context.ungetService(fileUtilRef);
        }
        
        private void init() {
            this.setBorder(BorderFactory.createEtchedBorder());
            this.setLayout(new GridBagLayout());;
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 8, 4);
            JLabel label = new JLabel("<html><b><u>Gene Set Loading</u></b></html>");
            this.add(label, constraints);
            constraints.insets = new Insets(0, 0, 0, 0);
            JPanel filePanel = new JPanel();
            filePanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
            label = new JLabel("Choose a gene set file:");
            filePanel.add(label);
            fileNameTF = new JTextField();
            fileNameTF.setEnabled(false);
            fileNameTF.setColumns(10);
            fileNameTF.getDocument().addDocumentListener(new DocumentListener() {
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    validateOkButton();
                }
                
                @Override
                public void insertUpdate(DocumentEvent e) {
                    validateOkButton();
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                    validateOkButton();
                }
            });
            filePanel.add(fileNameTF);
            JButton browseFileBtn = new JButton("Browse");
            browseFileBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    browseFile();
                }
            });
            filePanel.add(browseFileBtn);
            constraints.gridy = 1;
            constraints.anchor = GridBagConstraints.CENTER;
            this.add(filePanel, constraints);
            JPanel formatPane = new JPanel();
            formatPane.setLayout(new GridBagLayout());
            label = new JLabel("Specify file format:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.WEST;
            formatPane.add(label, constraints);
            commaDelimitedBtn = new JRadioButton("Comma delimited (e.g. TP53, EGFR)");
            tabDelimitedBtn = new JRadioButton("Tab delimited (e.g. TP53   EGFR)");
            lineDelimitedBtn = new JRadioButton("One gene per line");
            lineDelimitedBtn.setSelected(true); // The default file format
            ButtonGroup buttonGroup = new ButtonGroup();
            buttonGroup.add(commaDelimitedBtn);
            buttonGroup.add(tabDelimitedBtn);
            buttonGroup.add(lineDelimitedBtn);
            constraints.gridx = 1;
            formatPane.add(lineDelimitedBtn, constraints);
            constraints.gridy = 1;
            formatPane.add(commaDelimitedBtn, constraints);
            constraints.gridy = 2;
            formatPane.add(tabDelimitedBtn, constraints);
            constraints.gridx = 0;
            constraints.anchor = GridBagConstraints.CENTER;
            this.add(formatPane, constraints);
            
            parentDialog = GKApplicationUtilities.createDialog(EventTreePane.this,
                                                                 "Reactome Pathway Enrichment Analysis");
            parentDialog.getContentPane().add(this, BorderLayout.CENTER);
            controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = true;
                    parentDialog.dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    parentDialog.dispose();
                }
            });
            validateOkButton();
            parentDialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
            parentDialog.setLocationRelativeTo(EventTreePane.this);
            parentDialog.setSize(480, 280);
            parentDialog.setModal(true);
            parentDialog.setVisible(true);
        }
        
        
    }
    
}
