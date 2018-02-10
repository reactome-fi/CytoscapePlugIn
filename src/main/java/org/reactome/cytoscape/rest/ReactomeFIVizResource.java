package org.reactome.cytoscape.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.ci.model.CIResponse;
import org.reactome.cytoscape.rest.tasks.ReactomeFIVizTable.ReactomeFIVizTableResponse;
import org.reactome.cytoscape3.GeneSetMutationAnalysisOptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This interface collects all ReactomeFIViz client-side REST functions to support Cytoscape automation.
 * For some unknown reason, we have to use an interface in order to generate CyREST API inside Eclipse without
 * manually update. This is very weird!
 * @author wug
 *
 */
@Api(tags = {"Apps: ReactomeFIViz"})
@Path("/reactomefiviz/v1/")
public interface ReactomeFIVizResource {

    /**
     * Return the list of FIs suported by ReactomeFIViz
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("fiVersions")
    @ApiOperation(value = "List versions of FI Networks",
                  notes = "Get the list of Reactome Functional Interaction networks supported by ReactomeFIViz",
                  response = List.class)
    public List<String> getFIVersions();
    
    /**
     * Cluster the current network.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cluster")
    @ApiOperation(value = "Cluster FI Sub-network",
                  notes = "Perform spectral partition-based network clustering for the current displayed FI sub-network",
                  response = ReactomeFIVizTableResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Cannot perform the FI network cluster. Check the Cytoscape logging for errors.", response = CIResponse.class)
    })
    public Response clusterFINetwork();
    
    /**
     * Build a FI sub-network for a set of genes submitted via a HTTP POST.
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("buildFISubNetwork")
    @ApiOperation(value = "Build a FI subnetwork for a set of genes",
                  notes = "Construct a Reactome functional interaction sub-network for a set of genes passed via HTTP post or in stored in file. The returned value is the id of the constructed network.",
                  response = Response.class)
    @ApiResponses(value = { 
            @ApiResponse(code = 404, message = "Cannot generate a FI sub-network. Check the Cytoscape logging for errors.", response = CIResponse.class)
    })
    public Response buildFISubNetwork(@ApiParam(value = "Parameters for Gene Set Analysis", required = true) GeneSetMutationAnalysisOptions parameters);

}
