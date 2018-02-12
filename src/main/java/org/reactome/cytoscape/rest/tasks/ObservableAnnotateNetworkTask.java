package org.reactome.cytoscape.rest.tasks;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.gk.util.ProgressPane;
import org.reactome.annotate.AnnotationType;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cytoscape.service.GeneSetAnnotationPanel;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape3.ResultDisplayHelper;

/**
 * Refactored task to perform network annotation.
 * @author wug
 *
 */
public class ObservableAnnotateNetworkTask extends AbstractTask implements ObservableTask {
    private CyNetworkView view;
    private String type;
    // Result
    private ReactomeFIVizTable result;
    
    public ObservableAnnotateNetworkTask(CyNetworkView view,
                                         String type) {
        this.view = view;
        this.type = type;
    }
    
    private Set<String> grepGenes(CyNetworkView view) {
        CyNetwork network = view.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();
        Set<String> genes = network.getNodeList().stream().map(node -> {
            Long nodeSUID = node.getSUID();
            String nodeName = nodeTable.getRow(nodeSUID).get("name", String.class);
            return nodeName;
        }).collect(Collectors.toSet());
        return genes;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Network Annotation");
        taskMonitor.setProgress(0.0d);
        taskMonitor.setStatusMessage("Collecting genes...");
        taskMonitor.setProgress(0.1d);
        Set<String> genes = grepGenes(view);
        taskMonitor.setStatusMessage("Annotating network...");
        ProgressPane progPane = new ProgressPane();
        progPane.setIndeterminate(true);
        progPane.setText("Annotating network...");
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        frame.setGlassPane(progPane);
        frame.getGlassPane().setVisible(true);
        RESTFulFIService fiService = new RESTFulFIService(view);
        List<ModuleGeneSetAnnotation> annotations = fiService.annotateGeneSet(genes, type);
        taskMonitor.setStatusMessage("Displaying results...");
        taskMonitor.setProgress(0.9d);
        // Check if selection is used
        List<CyNode> nodeList = CyTableUtil.getNodesInState(view.getModel(), CyNetwork.SELECTED, true);
        if (nodeList != null && nodeList.size() > 0)
            ResultDisplayHelper.getHelper().displaySelectedGenesAnnotations(annotations, view, type, genes);
        else
            ResultDisplayHelper.getHelper().displayModuleAnnotations(annotations, view, type, false);
        progPane.setIndeterminate(false);
        frame.getGlassPane().setVisible(false);
        taskMonitor.setProgress(1.0d);
        taskMonitor.setStatusMessage("Done");
        
        extractResults();
    }
    
    private void extractResults() {
        String title = (type.equals(AnnotationType.Pathway.toString())) ? "Pathways in Network" : "GO " + type + " in Network";
        result = new ReactomeFIVizTable();
        result.fillTableFromResults(GeneSetAnnotationPanel.class, title);
    }

    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(ReactomeFIVizTable.class))
            return (R) result;
        return null;
    }

    @Override
    public List<Class<?>> getResultClasses() {
        return Collections.singletonList(ReactomeFIVizTable.class);
    }
    
}
