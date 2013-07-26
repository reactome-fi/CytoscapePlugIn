/*
 * Created on Jul 24, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.model.ReactomeJavaConstants;
import org.jdom.Element;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * A customized JPanel that is used to display an event tree loaded via RESTful API.
 * @author gwu
 *
 */
public class EventTreePane extends JPanel {
    private JTree eventTree;
    
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
        // Only single selection
        eventTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
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
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    loadSubPathways();
            }

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
    }
    
    /**
     * Load sub-pathways for a selected pathway.
     */
    private void loadSubPathways() {
        EventObject event = getSelectedEvent();
        if (event == null || event.isLoaded)
            return;
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        Task task = new LoadSubPathwayTask(event.dbId);
        taskManager.execute(new TaskIterator(task));
        event.isLoaded = true;
    }
    
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
            EventObject event = createEventObject(child);
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(event);
            treeNode.add(childNode);
        }
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        model.nodeChanged(treeNode);
        // Expand newly expanded pathway node
        eventTree.expandPath(treePath);
    }
    
    private void showTreePopup(MouseEvent e) {
        EventObject event = getSelectedEvent();
        if (event == null || !event.hasDiagram)
            return;
        JPopupMenu popup = new JPopupMenu();
        JMenuItem showDiagramMenu = new JMenuItem("Show Diagram");
        showDiagramMenu.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                showPathwayDiagram();
            }
        });
        popup.add(showDiagramMenu);
        popup.show(eventTree, e.getX(), e.getY());
    }
    
    /**
     * Open a new pathway diagram for the selected pathway in the tree.
     */
    @SuppressWarnings("rawtypes")
    private void showPathwayDiagram() {
        EventObject event = getSelectedEvent();
        if (event == null)
            return; // In case there is nothing selected
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        PathwayDiagramLoadTask task = new PathwayDiagramLoadTask();
        task.setPathwayId(event.dbId);
        taskManager.execute(new TaskIterator(task));
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
    public void loadEventTree() throws Exception {
        Element root = ReactomeRESTfulService.getService().frontPageItems();
        List<?> children = root.getChildren();
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        DefaultMutableTreeNode treeRoot = (DefaultMutableTreeNode) model.getRoot();
        treeRoot.removeAllChildren(); // Just in case there is anything there.
        for (Object obj : children) {
            Element elm = (Element) obj;
            EventObject event = createEventObject(elm);
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode();
            treeNode.setUserObject(event);
            treeRoot.add(treeNode);
        }
        // Needs an update
        model.nodeStructureChanged(treeRoot);
    }

    /**
     * Parse an XML element for an EventObject object.
     * @param elm
     * @return
     */
    private EventObject createEventObject(Element elm) {
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
    
}
