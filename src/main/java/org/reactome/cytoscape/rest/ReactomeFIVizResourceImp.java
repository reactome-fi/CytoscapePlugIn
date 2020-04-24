package org.reactome.cytoscape.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.pathway.EventTreePane;
import org.reactome.cytoscape.pathway.EventTreePane.EventObject;
import org.reactome.cytoscape.pathway.PathwayControlPanel;
import org.reactome.cytoscape.rest.tasks.FINetworkBuildTask;
import org.reactome.cytoscape.rest.tasks.FINetworkBuildTaskObserver;
import org.reactome.cytoscape.rest.tasks.ObservableAnnotateModulesTask;
import org.reactome.cytoscape.rest.tasks.ObservableAnnotateNetworkTask;
import org.reactome.cytoscape.rest.tasks.ObservableClusterFINetworkTask;
import org.reactome.cytoscape.rest.tasks.ObservablePathwayDiagramExportTask;
import org.reactome.cytoscape.rest.tasks.ObservablePathwayEnrichmentAnalysisTask;
import org.reactome.cytoscape.rest.tasks.ObservablePathwayHierarchyLoadTask;
import org.reactome.cytoscape.rest.tasks.PathwayEnrichmentResults;
import org.reactome.cytoscape.rest.tasks.ReactomeFIVizTable;
import org.reactome.cytoscape.rest.tasks.RestTaskObserver;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.cytoscape3.GeneSetMutationAnalysisDialog;
import org.reactome.cytoscape3.GeneSetMutationAnalysisOptions;
import org.reactome.cytoscape3.GeneSetMutationAnalysisTask;

/**
 * This class collects all ReactomeFIViz client-side REST functions to support Cytoscape automation.
 * @author wug
 *
 */
@SuppressWarnings("rawtypes")
public class ReactomeFIVizResourceImp implements ReactomeFIVizResource {
    
    public ReactomeFIVizResourceImp() {
        Thread t = new Thread() {
            public void run() {
                try {
                    new ReacfoamServer().start();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    public void selectEvent(String id) {
        if (id.contains("&")) {
            // Passed from reacfoam
            String[] tokens = id.split("&");
            for (String token : tokens) {
                if (token.startsWith("R-")) {
                    id = token;
                    break;
                }
            }
        }
        Long dbId = null;
        if (id.startsWith("R-HSA-")) {
            // This is a stable id
            int index = id.lastIndexOf("-");
            dbId = new Long(id.substring(index + 1));
        }
        else if (id.matches("\\d+"))
            dbId = new Long(id);
        if (dbId == null)
            return; // Do nothing
        EventTreePane treePane = PathwayControlPanel.getInstance().getEventTreePane();
        Map<Long, String> idToName = treePane.grepEventIdToName();
        String name = idToName.get(dbId);
        if (name == null) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot find an event for " + id + "\n*: Disease events are not supported.",
                                          "No Event",
                                          JOptionPane.INFORMATION_MESSAGE);
            return; // Cannot find a pathway
        }
        treePane.searchPathway(name, true);
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        frame.toFront();
        frame.requestFocus();
        frame.repaint();
    }
    
    public PathwayEnrichmentResults fetchEnrichmentResults() {
        PathwayEnrichmentResults results = new PathwayEnrichmentResults();
        EventTreePane treePane = PathwayControlPanel.getInstance().getEventTreePane();
        Map<Long, String> idToName = treePane.grepEventIdToName();
        Map<String, GeneSetAnnotation> nameToAnnotation = treePane.getPathwayToAnnotation();
        for (Long id : idToName.keySet()) {
            String name = idToName.get(id);
            GeneSetAnnotation annotation = nameToAnnotation.get(name);
            if (name == null || annotation == null)
                continue;
            results.addPathway("R-HSA-" + id,
                               name, 
                               annotation.getFdr(), // We want to use FDR for all annotation 
                               annotation.getNumberInTopic() + "",
                               annotation.getHitNumber() + "");
        }
//        results.addPathway("R-HSA-418597",
//                           "G alpha (z) signalling events", "0.0012616811274",
//                           "62", "7");
        return results;
    }

    @Override
    public List<String> getFIVersions() {
        String versions = PlugInObjectManager.getManager().getProperties()
                .getProperty("FINetworkVersions");
        String[] tokens = versions.split(",");
        return Arrays.asList(tokens);
    }
    
    @Override
    public Response buildFISubNetwork(GeneSetMutationAnalysisOptions parameters) {
        // Required by the original task
        String genes = parameters.getEnteredGenes();
        String genesInLines = genes.replaceAll(",", "\n");
        GeneSetMutationAnalysisDialog dialog = new GeneSetMutationAnalysisDialog();
        dialog.setEnteredGenes(genesInLines);
        GeneSetMutationAnalysisTask task = new GeneSetMutationAnalysisTask(parameters);
        FINetworkBuildTask cyTask = new FINetworkBuildTask(task);
        FINetworkBuildTaskObserver observer = new FINetworkBuildTaskObserver();
        TaskIterator iterator = new TaskIterator(cyTask);
        SynchronousTaskManager taskManager = PlugInObjectManager.getManager().getSyncTaskManager();
        taskManager.execute(iterator, observer);
        CIResponse<?> response = observer.getResponse();
        return generateResponse(response);
    }
    
    private <T> Response generateResponse(CIResponse<T> response) {
       return Response.status(response.errors.size() == 0 ? Response.Status.OK : Response.Status.INTERNAL_SERVER_ERROR)
        .type(MediaType.APPLICATION_JSON)
        .entity(response).build();
    }

    @Override
    public Response clusterFINetwork() {
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        ObservableClusterFINetworkTask task = new ObservableClusterFINetworkTask(view, frame);
        return exectuteRestTask(task, ReactomeFIVizTable.class);
    }

    protected <T> Response exectuteRestTask(ObservableTask task, Class<T> cls) {
        RestTaskObserver<T> observer = new RestTaskObserver<>(cls);
        SynchronousTaskManager taskManager = PlugInObjectManager.getManager().getSyncTaskManager();
        TaskIterator taskIterator = new TaskIterator(task);
        taskManager.execute(taskIterator, observer);
        CIResponse<T> response = observer.getResponse();
        return generateResponse(response);
    }
    
    @Override
    public Response performEnrichmentAnalysis(String type) {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        ObservableAnnotateNetworkTask task = new ObservableAnnotateNetworkTask(view, type);
        return exectuteRestTask(task, ReactomeFIVizTable.class);
    }
    
    @Override
    public Response performModuleEnrichmentAnalysis(String type) {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        ObservableAnnotateModulesTask task = new ObservableAnnotateModulesTask(view, type);
        task.setAvoidGUIs(true);
        return exectuteRestTask(task, ReactomeFIVizTable.class);
    }

    @Override
    public Response loadPathwayHierarchy() {
        ObservablePathwayHierarchyLoadTask task = new ObservablePathwayHierarchyLoadTask();
        return exectuteRestTask(task, EventObject.class);
    }

    @Override
    public Response performPathwayEnrichmentAnalysis(String genes) {
        ObservablePathwayEnrichmentAnalysisTask task = new ObservablePathwayEnrichmentAnalysisTask();
        task.setEventPane(PathwayControlPanel.getInstance().getEventTreePane());
        genes = genes.replaceAll(",", "\n");
        task.setGeneList(genes);
        return exectuteRestTask(task, ReactomeFIVizTable.class);
    }

    @Override
    public Response exportPathwayDiagram(PathwayDiagramOption diagramOption) {
        ObservablePathwayDiagramExportTask task = new ObservablePathwayDiagramExportTask(diagramOption.getDbId(),
                diagramOption.getPathwayName(),
                diagramOption.getFileName());
        return exectuteRestTask(task, String.class);
    }
}
    
