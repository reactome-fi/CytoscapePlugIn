package org.reactome.cytoscape.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape3.GeneSetMutationAnalysisDialog;
import org.reactome.cytoscape3.GeneSetMutationAnalysisTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class collects all ReactomeFIViz client-side REST functions to support Cytoscape automation.
 * @author wug
 *
 */
@Api(tags = {"Apps: ReactomeFIViz"})
@Path("/reactomefiviz/v1/")
public class ReactomeFIVizResource {
    private static final Logger logger = LoggerFactory.getLogger(ReactomeFIVizResource.class);
    
    public ReactomeFIVizResource() {
    }
    
    /**
     * Build a FI sub-network for a set of genes submitted via a HTTP POST.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("hello")
    @ApiOperation(value = "Check",
                  notes = "Just a check. \"Hello! This is a ReactomeFIViz!\" should be returned",
                  response = String.class)
    public String hello() {
        return "Hello! This is ReactomeFIViz!";
    }
    
    /**
     * Build a FI sub-network for a set of genes submitted via a HTTP POST.
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("buildFISubNetwork/{version}")
    @ApiOperation(value = "Build a FI subnetwork for a set of genes",
                  notes = "Construct a Reactome functional interaction sub-network for a set of genes passed via HTTP post.",
                  response = Response.class)
    @ApiResponses(value = { 
            @ApiResponse(code = 404, message = "Network or Network View does not exist", response = CIResponse.class)
    })
    public Response buildFISubNetwork(@ApiParam(value = "FI Network Version") @PathParam("version") Integer version,
                                      @ApiParam(value = "A set of genes delimited by \",\"", required = true) String genes) {
        if (genes == null || genes.length() == 0) {
            logger.error("No gene has been posted!");
            return null;
        }
        // Required by the original task
        String genesInLines = genes.replaceAll(",", "\n");
        GeneSetMutationAnalysisDialog dialog = new GeneSetMutationAnalysisDialog();
        dialog.setEnteredGenes(genesInLines);
        GeneSetMutationAnalysisTask task = new GeneSetMutationAnalysisTask(dialog);
        FINetworkBuildTask cyTask = new FINetworkBuildTask(task);
        FINetworkBuildTaskObserver observer = new FINetworkBuildTaskObserver();
        TaskIterator iterator = new TaskIterator(cyTask);
        SynchronousTaskManager taskManager = PlugInObjectManager.getManager().getSyncTaskManager();
        taskManager.execute(iterator, observer);
        CIResponse<?> response = observer.getResponse();
        return Response.status(response.errors.size() == 0 ? Response.Status.OK : Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(response).build();
    }

}
