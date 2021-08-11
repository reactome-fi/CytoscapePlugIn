package org.reactome.cytoscape.sc.server;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.cytoscape.application.events.CyShutdownListener;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.sc.ScNetworkManager;
import org.reactome.cytoscape.sc.diff.DiffExpResult;
import org.reactome.cytoscape.sc.utils.PythonPathHelper;
import org.reactome.cytoscape.sc.utils.ScPathwayMethod;
import org.reactome.cytoscape.sc.utils.ScvVelocityMode;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javajs.util.JSONException;

/**
 * This class is used to call a json server providing single cell data analysis services via 
 * wrapping Python-based scanpy API.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
public class JSONServerCaller {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(JSONServerCaller.class);
    
    private int port = 8999; // Default
    // As long as we have an id. 
    private RequestObject request;
    // flag to track is the server is started
    // During the development, make sure isStarted is true so that ReactomeFIViz can talk to
    // the Python JSON server directly
    private boolean isStarted = true;
//    private boolean isStarted;
    
    public JSONServerCaller() {
        request = new RequestObject();
        request.id = 1; // As long as we have an id, it should be fine
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        if (context == null) // For test
            return;
        // This is much more reliable than listing to BundleEvent
        CyShutdownListener l = e -> {
            if (!e.actuallyShutdown()) // Only stop when it is actually shutdown.
                return;
            try {
                stopServer();
            }
            catch(Exception e1) {
                e1.printStackTrace();
            }
        };
        context.registerService(CyShutdownListener.class.getName(),
                                l,
                                new Properties());
        // Don't use BundleListener. It is not reliable enough to shutdown the server even during the development stage.
    }
    
    public boolean isStarted() {
        return this.isStarted;
    }
    
    public void setIsStarted(boolean isStarted) {
        this.isStarted = isStarted;
    }
    
    public Map<String, List<Double>> calculateGeneRelationships(Collection<String> genePairs,
                                                                List<String> groups,
                                                                String cellTimeKey,
                                                                String layer,
                                                                int delayWindow,
                                                                String mode) throws JsonEOFException, IOException {
        Object rtn = callJSONServer("calculate_gene_relations",
                                    String.join("\n", genePairs),
                                    String.join(",", groups),
                                    cellTimeKey,
                                    layer,
                                    delayWindow + "",
                                    mode);
        if (rtn instanceof String)
            throw new IllegalStateException(rtn.toString());
        return (Map<String, List<Double>>) rtn;
    }
    
    public List<String> getCellTimeKeys() throws JsonEOFException, IOException {
        return (List<String>) callJSONServer("get_cell_time_keys");
    }
    
    public String openData(String dir,
                           String fileFormat) throws JsonEOFException, IOException {
        return (String) callJSONServer("open_data",
                                       dir,
                                       fileFormat);
    }
    
    /**
     * Write a data into an external file in the h5ad format.
     * @param fileName
     * @return
     * @throws JsonEOFException
     * @throws IOException
     */
    public String writeData(String fileName) throws JsonEOFException, IOException {
        if (!fileName.endsWith(".h5ad"))
            throw new IllegalArgumentException("The supported format for writing is h5ad only. Make sure the passed extension name is .h5ad.");
        return (String) callJSONServer("write_data", fileName);
    }
    
    public String openAnalyzedData(String fileName) throws JsonEOFException, IOException {
        if (!fileName.endsWith(".h5ad"))
            throw new IllegalArgumentException("The supported format for opening is h5ad only. This type of file is not supported.");
        return (String) callJSONServer("open_analyzed_data", fileName);
    }
    
    public String scvOpenData(String dir) throws JsonEOFException, IOException {
        return (String) callJSONServer("scv_open", dir);
    }
    
    public List<List<String>> rankVelocityGenes() throws JSONException, IOException {
        Object result = callJSONServer("scv_rank_velocity_genes");
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (List<List<String>>) result;
    }
    
    public List<List<String>> rankDynamicGenes() throws JSONException, IOException {
        Object result = callJSONServer("scv_rank_dynamic_genes");
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (List<List<String>>) result;
    }
    
    /**
     * Very weird: return two double and one String in the List. This needs to be handled.
     * @param dir
     * @return
     * @throws JsonEOFException
     * @throws IOException
     */
    public Map<String, List<?>> project(String dir,
                                        String fileFormat,
                                        boolean isForRNAVelocity) throws JsonEOFException, IOException {
        Object result = callJSONServer("project", 
                                       dir,
                                       fileFormat,
                                       ScNetworkManager.getManager().isForRNAVelocity() + "");
        if (result instanceof String) // An error
            throw new IllegalStateException(result.toString());
        // There are only two types: String or List of String
        // Convert to a double array
        Map<String, List<?>> map = (Map<String, List<?>>) result;
        return map;
    }
    
    public String preprocessData(List<String> regressoutKeys,
                                 String imputationMethod) throws JsonEOFException, IOException {
        Object rtn = null;
        if (regressoutKeys == null)
            regressoutKeys = Collections.emptyList();
        if (imputationMethod == null || imputationMethod.trim().length() == 0)
            imputationMethod = "";
            
        rtn = callJSONServer("preprocess_data", 
                             String.join(",", regressoutKeys),
                             imputationMethod);
        return (String) rtn;
    }
    
    public String scvPreprocessData() throws JsonEOFException, IOException {
        return (String) callJSONServer("scv_preprocess");
    }
    
    public String clusterData() throws JsonEOFException, IOException {
        return (String) callJSONServer("cluster_data");
    }
    
    public String scvVelocity(ScvVelocityMode mode) throws JsonEOFException, IOException {
        return (String) callJSONServer("scv_velocity", mode.toString());
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
    
    public String inferCellRoot(List<String> targetClusters) throws JsonEOFException, IOException {
        Object result = null;
        if (targetClusters == null || targetClusters.size() == 0 || targetClusters.get(0).equals("all"))
            result = callJSONServer("infer_cell_root");
        else
            result = callJSONServer("infer_cell_root", targetClusters.toArray(new String[] {}));
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        List<?> list = (List<?>) result;
        return list.get(0).toString();
    }
    
    public List<Double> performDPT(String rootCell) throws JsonEOFException, IOException {
        Object result = callJSONServer("dpt", rootCell);
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        List<Double> list = (List<Double>) result;
        return list;
    }
    
    public List<Double> performCytoTrace() throws JsonEOFException, IOException {
        Object result = callJSONServer("cytotrace");
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        List<Double> list = (List<Double>) result;
        return list;
    }
    
    public List<Double> getGeneExp(String gene) throws JsonEOFException, IOException {
        Object result = callJSONServer("get_gene_exp", gene);
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (List<Double>) result;
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
    
    public List<String> getCellFeatureNames() throws JsonEOFException, IOException {
        Object result = callJSONServer("get_obs_names");
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (List<String>)result;
    }
    
    /**
     * May return String, Double, Integer.
     * @param featureName
     * @return
     * @throws JsonEOFException
     * @throws IOException
     */
    public List<Object> getCellFeature(String featureName) throws JsonEOFException, IOException {
        Object result = callJSONServer("get_obs", featureName);
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (List<Object>) result;
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
    
    public Object callJSONServer(String method,
                                 String... params) throws JsonEOFException, IOException {
        request.method = method;
        request.resetParams();
        if (params != null)
            Stream.of(params).forEach(request::addParams);
        ResponseObject response = callJSONServer(request);
        return response.getResult();
    }
    
    
    //TODO: Add a parameter for top genes, which should be passed to the Python server to control
    // the size of text between processes.
    public Map<String, List<List<Double>>> findGroupMarkers(int topGenes) throws JsonEOFException, IOException {
        Object result = callJSONServer("rank_genes_groups"); // This is more like to find markers for individual clusters
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (Map<String, List<List<Double>>>) result;
    }
    
    public String doPathwayAnalysis(String gmtFileName,
                                    ScPathwayMethod method) throws JsonEOFException, IOException {
        Object dataKey = callJSONServer("analyze_pathways",
                                        gmtFileName,
                                        method.toString(),
                                        Boolean.FALSE + "");
        return dataKey + "";
    }
    
    public String doTFsAnalysis(String dorotheaFileName,
                                ScPathwayMethod method) throws JsonEOFException, IOException {
        Object dataKey = callJSONServer("analyze_tfs",
                                        dorotheaFileName,
                                        method.toString(),
                                        Boolean.FALSE + "");
        return dataKey + "";
    }
    
    public Map<String, Map<String, Double>> doPathwayAnova(String dataKey) throws JsonEOFException, IOException {
        Object result = callJSONServer("anova_pathway",
                                        dataKey);
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (Map<String, Map<String, Double>>) result;
    }
    
    public Map<String, Double> getPathwayActivities(String pathway, String dataKey) throws JsonEOFException, IOException {
        Object result = callJSONServer("pathway_activities",
                                       pathway,
                                       dataKey);
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (Map<String, Double>) result;
    }
                                       
    
    public DiffExpResult doDiffGeneExpAnalysis(String group,
                                               String reference) throws JsonEOFException, IOException {
        Object result = callJSONServer("rank_genes_groups", group, reference);
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        // Map to a model object
        Map<String, List<?>> keyToList = (Map<String, List<?>>) result;
        DiffExpResult rtn = new DiffExpResult();
        for (String key : keyToList.keySet()) {
            List<?> list = keyToList.get(key);
            if (key.equals("names")) {
                List<String> names = new ArrayList<>(list.size());
                list.forEach(o -> {
                    List<String> tmp = (List<String>) o;
                    names.add(tmp.get(0));
                });
                rtn.setNames(names);
            }
            else {
                List<Double> valueList = new ArrayList<>();
                list.forEach(o -> {
                    List<Double> tmp = (List<Double>) o;
                    valueList.add(tmp.get(0));
                });
                if (key.equals("scores"))
                    rtn.setScores(valueList);
                else if (key.equals("logfoldchanges"))
                    rtn.setLogFoldChanges(valueList);
                else if (key.equals("pvals"))
                    rtn.setPvals(valueList);
                else if (key.equals("pvals_adj"))
                    rtn.setPvalsAdj(valueList);
            }
        }
        return rtn;
    }
    
    public void stopServer() throws Exception {
        if (!isStarted)
            return;
        RequestObject request = new RequestObject();
        request.id = 100;
        request.method = "stop";
        callJSONServer(request);
        isStarted = false;
        System.out.println("The scpy4reactome has stopped.");
        logger.info("The scpy4reactome has stopped.");
    }
    
    public boolean startServer() throws IOException {
        if (isStarted)
            return true; // Just return. There is no need to starty another process.
        // Get an available port
        ServerSocket socket = new ServerSocket(0);
        this.port = socket.getLocalPort();
        socket.close();
        // Make a process call
        String scPythonPath = PythonPathHelper.getHelper().getScPythonPath();
        String pythonPath = PythonPathHelper.getHelper().getPythonPath();
        if (pythonPath == null)
            return false; // Cannot find a python path. This may be aborted by the user.
        String[] parameters = {pythonPath, 
                               scPythonPath + File.separator + PythonPathHelper.SCPY_2_REACTOME_NAME,
                               this.port + "",
                               PythonPathHelper.getHelper().getLogFileName()};
        ProcessBuilder builder = new ProcessBuilder(parameters);
        builder.directory(new File(scPythonPath));
        builder.redirectErrorStream(true);
        builder.redirectOutput(Redirect.INHERIT);
        builder.start();
        // Poll the port to make sure it is reachable
        long time1 = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(250);
                // Try again
                Socket testSocket = new Socket("localhost", port);
                if (testSocket.isConnected()) {
                    isStarted = true;
                    testSocket.close();
                    break;
                }
                else
                    testSocket.close();
            }
            catch(Exception e) { // Do nothing
            }
            long time2 = System.currentTimeMillis();
            // It is observed that the service starts slow when it starts for the first time after download under Mac.
            if ((time2 - time1) > 60000) {
                // This is 60 seconds. Too long to start the server.
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "There is some problem to start the python serivce for scRNA-seq data analysis.\n" + 
                                              "Make sure you have python >= 3.7.0 and scpy4reactome installed.",
                                              "Error in Starting Python Service",
                                              JOptionPane.ERROR_MESSAGE);
                break;
            }
        }
        System.out.println("The scpy4reactome has started.");
        logger.info("The scpy4reactome has started.");
        return isStarted;
    }

    protected ResponseObject callJSONServer(RequestObject request) throws JsonProcessingException, IOException {
        if (!isStarted) 
            startServer();
        ObjectMapper mapper = new ObjectMapper();
        // For some NaN, infinity, etc.
        mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        String query = mapper.writeValueAsString(request);
        logger.debug(query);
        String url = "http://localhost:" + port;
        String output = PlugInUtilities.callHttpInJson(url,
                                                       PlugInUtilities.HTTP_POST,
                                                       query); // POST should be used always since the query is a JSON object.
        ResponseObject response = mapper.readValue(output, ResponseObject.class);
        logger.debug("Result: " + response.getResult());
        return response;
    }

}
