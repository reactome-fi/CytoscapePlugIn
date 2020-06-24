package org.reactome.cytoscape.sc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFrame;

import org.junit.Test;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.ObjectMapper;

import smile.plot.swing.Canvas;
import smile.plot.swing.ScatterPlot;

/**
 * This class is used to call a json server providing single cell data analysis services via 
 * wrapping Python-based scanpy API.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
public class JSONServerCaller {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(JSONServerCaller.class);
    
    //TODO: To be externalized
    private final String URL = "http://localhost:8999";
    // As long as we have an id. 
    private RequestObject request;
    
    public JSONServerCaller() {
        request = new RequestObject();
        request.id = 1; // As long as we have an id, it should be fine
    }
    
    public String openData(String dir) throws JsonEOFException, IOException {
        return (String) callJSONServer("open_data",
                                       dir);
    }
    
    public String preprocessData() throws JsonEOFException, IOException {
        return (String) callJSONServer("preprocess_data");
    }
    
    public String clusterData() throws JsonEOFException, IOException {
        return (String) callJSONServer("cluster_data");
    }
    
    /**
     * Return a n x 2 coordinates
     * @return
     * @throws JsonEOFException
     * @throws IOException
     */
    public List<List<Double>> getUMAP() throws JsonEOFException, IOException {
        Object result = callJSONServer("get_umap");
        if (result instanceof String) // An error
            throw new IllegalStateException(result.toString());
        // There are only two types: String or List of String
        // Convert to a double array
        List<List<Double>> list = (List<List<Double>>) result;
        return list;
    }
    
    public Map<String, List<List<Double>>> getPAGA() throws JsonEOFException, IOException {
        Object result = callJSONServer("get_paga");
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (Map<String, List<List<Double>>>) result;
    }
    
    public List<Integer> getCluster() throws JsonEOFException, IOException {
        Object result = callJSONServer("get_cluster");
        if (result instanceof String) // An error
            throw new IllegalStateException(result.toString());
        // Return as a list of String.
        return ((List<String>)result).stream().map(Integer::parseInt).collect(Collectors.toList());
    }
    
    public List<String> getCellIds() throws JsonEOFException, IOException {
        Object result = callJSONServer("get_cell_ids");
        if (result instanceof String) // An error
            throw new IllegalStateException(result.toString());
        return (List<String>) result; // Otherwise, it should be a list of string.
    }
    
    public List<List<String>> getConnectivities() throws JsonEOFException, IOException {
        Object result = callJSONServer("get_connectivites");
        if (result instanceof String) // An error
            throw new IllegalStateException(result.toString());
        return (List<List<String>>) result;
    }
    
    private Object callJSONServer(String method,
                                  String... params) throws JsonEOFException, IOException {
        request.method = method;
        request.resetParams();
        if (params != null)
            Stream.of(params).forEach(request::addParams);
        ResponseObject response = callJSONServer(request);
        return response.getResult();
    }
    
    
    @Test
    public void testLoadData() throws Exception {
//        String query = "{\"jsonrpc\": \"2.0\", \"method\": \"echo\", \"id\": 2, \"params\":[\"test\"]}";
        RequestObject request = new RequestObject();
        request.id = 2;
        request.method = "echo";
        request.addParams("This is a test!");
        callJSONServer(request);
        
        String dir_17_5 = "/Users/wug/Documents/missy_single_cell/seq_data_v2/17_5_gfp/filtered_feature_bc_matrix";
        String text = openData(dir_17_5);
        System.out.println("Open data: " + text);
        text = preprocessData();
        System.out.println("Preprocess data: " + text);
        text = clusterData();
        System.out.println("Cluster data: " + text);
    }
    
    @Test
    public void testGetConnectivities() throws Exception {
        List<List<String>> list = getConnectivities();
        List<String> first = list.get(0);
        System.out.println("First: " + first);
    }
    
    @Test
    public void testGetPaga() throws Exception {
        Map<String, List<List<Double>>> result = getPAGA();
        System.out.println(result);
    }
    
    @Test
    public void testUMap() throws Exception {
        List<List<Double>> list = getUMAP();
        logger.debug("List size: " + list.size());
        double[][] umap = new double[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            List<Double> coords = list.get(i);
            umap[i] = new double[2];
            umap[i][0] = coords.get(0);
            umap[i][1] = coords.get(1);
        }
        List<Integer> result = getCluster();
        int[] clusters = result.stream().mapToInt(Integer::intValue).toArray();
        Canvas canvas = ScatterPlot.of(umap, clusters, '.').canvas();
        canvas.setAxisLabels("UMPA1", "UMAP2");
        JFrame frame = canvas.window();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Thread.sleep(Integer.MAX_VALUE);
    }
    
    @Test
    public void testGetCellIds() throws Exception {
        List<String> cellIds = getCellIds();
        System.out.println("Size of cell ids: " + cellIds.size());
        List<List<Double>> umap = getUMAP();
        System.out.println("Size of umap: " + umap.size());
        // Note: cell ids and umap sizes are not the same!
        if (cellIds.size() != umap.size())
            throw new IllegalStateException("CellIds and umap have different sizes!");
    }
    
    @Test
    public void stopServer() throws Exception {
        RequestObject request = new RequestObject();
        request.id = 100;
        request.method = "stop";
        callJSONServer(request);
    }

    private ResponseObject callJSONServer(RequestObject request) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        String query = mapper.writeValueAsString(request);
        logger.debug(query);
        String output = PlugInUtilities.callHttpInJson(URL,
                                                       PlugInUtilities.HTTP_POST,
                                                       query); // POST should be used always since the query is a JSON object.
        ResponseObject response = mapper.readValue(output, ResponseObject.class);
        logger.debug("Result: " + response.getResult());
        return response;
    }

}
