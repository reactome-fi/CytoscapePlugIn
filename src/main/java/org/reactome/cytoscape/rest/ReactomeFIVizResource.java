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
    @ApiResponses(value = { 
            @ApiResponse(code = 404, message = "Error", response = String.class)
    })
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
                  response = CIResponse.class)
    @ApiResponses(value = { 
            @ApiResponse(code = 404, message = "Network or Network View does not exist", response = CIResponse.class)
    })
    public Response buildFISubNetwork(@ApiParam(value = "FI Network Version") @PathParam("version") Integer version,
                                      @ApiParam(value = "A set of genes delimited by \",\"", required = true) String genes) {
        return null;
    }

}
