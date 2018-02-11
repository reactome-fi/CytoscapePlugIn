package org.reactome.cytoscape.rest;

import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.reactome.cytoscape.rest.tasks.FINetworkBuildTask;
import org.reactome.cytoscape.rest.tasks.FINetworkBuildTaskObserver;
import org.reactome.cytoscape.rest.tasks.ObservableAnnotateModulesTask;
import org.reactome.cytoscape.rest.tasks.ObservableAnnotateNetworkTask;
import org.reactome.cytoscape.rest.tasks.ObservableClusterFINetworkTask;
import org.reactome.cytoscape.rest.tasks.ReactomeFIVizTable;
import org.reactome.cytoscape.rest.tasks.RestTaskObserver;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.cytoscape3.GeneSetMutationAnalysisDialog;
import org.reactome.cytoscape3.GeneSetMutationAnalysisOptions;
import org.reactome.cytoscape3.GeneSetMutationAnalysisTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class collects all ReactomeFIViz client-side REST functions to support Cytoscape automation.
 * @author wug
 *
 */
@SuppressWarnings("rawtypes")
public class ReactomeFIVizResourceImp implements ReactomeFIVizResource {
    private static final Logger logger = LoggerFactory.getLogger(ReactomeFIVizResourceImp.class);
    
    public ReactomeFIVizResourceImp() {
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
        return exectuteRestTask(task);
    }

    protected Response exectuteRestTask(ObservableTask task) {
        RestTaskObserver observer = new RestTaskObserver();
        SynchronousTaskManager taskManager = PlugInObjectManager.getManager().getSyncTaskManager();
        TaskIterator taskIterator = new TaskIterator(task);
        taskManager.execute(taskIterator, observer);
        CIResponse<ReactomeFIVizTable> response = observer.getResponse();
        return generateResponse(response);
    }
    
    @Override
    public Response performEnrichmentAnalysis(String type) {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        ObservableAnnotateNetworkTask task = new ObservableAnnotateNetworkTask(view, type);
        return exectuteRestTask(task);
    }
    
    @Override
    public Response performModuleEnrichmentAnalysis(String type) {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        ObservableAnnotateModulesTask task = new ObservableAnnotateModulesTask(view, type);
        task.setAvoidGUIs(true);
        return exectuteRestTask(task);
    }
    
}
