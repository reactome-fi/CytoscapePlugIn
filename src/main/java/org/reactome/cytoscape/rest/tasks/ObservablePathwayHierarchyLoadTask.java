package org.reactome.cytoscape.rest.tasks;

import java.util.Collections;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.reactome.cytoscape.pathway.EventTreePane;
import org.reactome.cytoscape.pathway.EventTreePane.EventObject;
import org.reactome.cytoscape.pathway.PathwayControlPanel;
import org.reactome.cytoscape.pathway.PathwayHierarchyLoadTask;

/**
 * Convert to an ObservableTask for the REST api.
 * @author wug
 *
 */
public class ObservablePathwayHierarchyLoadTask extends PathwayHierarchyLoadTask implements ObservableTask {
    private EventObject eventRoot;
    
    public ObservablePathwayHierarchyLoadTask() {
    }
    
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        super.run(taskMonitor);
        extractResults();
    }
    
    private void extractResults() {
        EventTreePane eventTreePane = PathwayControlPanel.getInstance().getEventTreePane();
        JTree tree = eventTreePane.getEventTree();
        eventRoot = new EventObject();
        eventRoot.setName("Root"); // A container
        TreeModel model = tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        extractResults(root, eventRoot);
    }
    
    private void extractResults(DefaultMutableTreeNode treeNode, EventObject event) {
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            EventObject childEvent = (EventObject) childNode.getUserObject();
            event.addChild(childEvent);
            extractResults(childNode, childEvent);
        }
    }

    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(EventObject.class))
            return (R) eventRoot;
        return null;
    }

    @Override
    public List<Class<?>> getResultClasses() {
        return Collections.singletonList(EventObject.class);
    }
    
}
