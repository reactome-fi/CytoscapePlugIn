package org.reactome.cytoscape.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.ci.model.CIResponse;
import org.reactome.cytoscape.pathway.EventTreePane.EventObject;
import org.reactome.cytoscape.rest.tasks.ReactomeFIVizTable.ReactomeFIVizTableResponse;
import org.reactome.cytoscape3.GeneSetMutationAnalysisOptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
     * Perform pathway or GO enrichment analysis.
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("enrichment/{type}")
    @ApiOperation(value = "Perform Enrichment Analysis",
                  notes = "Perform pathway or GO enrichment analysis for the current displayed network.",
                  response = ReactomeFIVizTableResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Cannot perform enrichment analysis. Check the Cytoscape logging for errors.", response = CIResponse.class)
    })
    public Response performEnrichmentAnalysis(@ApiParam(value = "Enrichment type", allowableValues = "Pathway, BP, CC, MF", example = "Pathway") @PathParam(value = "type") String type);
    
    /**
     * Perform pathway or GO enrichment analysis for network modules.
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("moduleEnrichment/{type}")
    @ApiOperation(value = "Perform Enrichment Analysis for Network Modules",
                  notes = "Perform pathway or GO enrichment analysis for the network modules in the current displayed network.",
                  response = ReactomeFIVizTableResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Cannot perform enrichment analysis. Check the Cytoscape logging for errors.", response = CIResponse.class)
    })
    public Response performModuleEnrichmentAnalysis(@ApiParam(value = "Enrichment type", allowableValues = "Pathway, BP, CC, MF", example = "Pathway") @PathParam(value = "type") String type);
    
    
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("pathwayTree")
    @ApiOperation(value = "Load the Reactome pathway tree",
                  notes = "Load the Reactome pathway hierarchical tree. The root is a virual container and should be skipped.",
                  response = PathwayTreeResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Cannot load the Reactome tree. Check the Cytoscape logging for errors.", response = CIResponse.class)
    })
    public Response loadPathwayHierarchy();
    
    public static class PathwayTreeResponse extends CIResponse<EventObject> {}
    
    /**
     * Perform pathway enrichment analysis for a set of genes.
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("ReactomePathwayEnrichment")
    @ApiOperation(value = "Perform Reactome pathway enrichment analysis",
                  notes = "Perform pathway enrichment analysis using Reactome pathways. The pathway tree should be loaded first. Genes should be sent as one gene per line.",
                  response = ReactomeFIVizTableResponse.class)
    @ApiResponses(value = { 
            @ApiResponse(code = 404, message = "Cannot perform Reactome pathway enrichment analysis. Check the Cytoscape logging for errors.", response = CIResponse.class)
    })
    public Response performPathwayEnrichmentAnalysis(@ApiParam(value = "List of genes delimited by \",\"", required = true) String geneList);

    /**
     * Load and export pathway diagram.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("exportPathwayDiagram")
    @ApiOperation(value = "Export pathway diagram in PDF",
                  notes = "Export pathway diagram from the database in PDF. Diagrams only for pathways labeled with hasDiagram = true in the tree can be exported. "
                          + "The client is required to check if an Event listed in the tree has diagram first before calling this method. If a pathway enrichment "
                          + "analysis has been performed before exporting, entities having hit genes will be highlighted.",
                  response = StringResponse.class)
    public Response exportPathwayDiagram(@ApiParam(value = "Option for exporting pathway diagram", required = true) PathwayDiagramOption diagramOption);
    
    @ApiModel(value = "Pathway Digram Option", description = "Configuration to export pathway diagram")
    public static class PathwayDiagramOption {
        @ApiModelProperty(value = "DB_ID", notes = "Reactome internal DB_ID", example = "69620")
        private Long dbId;
        @ApiModelProperty(value = "Pathway Name", notes = "Name for pathway having diagram", example = "Cell Cycle Checkpoints")
        private String pathwayName;
        @ApiModelProperty(value = "Destination PDF File", notes = "PDF file for holding the exported diagram", example = "Cell Cycle Checkpoints.pdf")
        private String fileName;
        
        public PathwayDiagramOption() {
        }

        public Long getDbId() {
            return dbId;
        }

        public void setDbId(Long dbId) {
            this.dbId = dbId;
        }

        public String getPathwayName() {
            return pathwayName;
        }

        public void setPathwayName(String pathwayName) {
            this.pathwayName = pathwayName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
    
    @ApiModel(value = "PDF file name for pathway diagram")
    public static class StringResponse extends CIResponse<String> {}
}
