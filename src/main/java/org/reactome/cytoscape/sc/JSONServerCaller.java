package org.reactome.cytoscape.sc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFrame;

import org.junit.Test;
import org.reactome.cytoscape.sc.diff.DiffExpResult;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.util.FileUtility;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javajs.util.JSONException;
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
    
    @Test
    public void testCalculateGeneRelationships() throws Exception {
//        String fileName1 = "/Users/wug/temp/17_5_gfp_velocity_dynamic.h5ad";
//        String rtn = openAnalyzedData(fileName1);
//        System.out.println("openAnalyzedData: \n" + rtn);
//        List<String> cellTimeKeys = getCellTimeKeys();
//        System.out.println("getCellTimeKeys(): " + cellTimeKeys);
//        List<String> genePairs = Arrays.asList("Cps1\tBicc1", "Prom1\tMuc4");
        // For loading gene pairs
        String fileName = "/Users/wug/git/FIVizWS_corews/src/main/webapp/WEB-INF/dorothea_mm.tsv";
        FileUtility fu = new FileUtility();
        fu.setInput(fileName);
        String line = fu.readLine();
        Set<String> genePairs = new HashSet<>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            genePairs.add(tokens[0] + "\t" + tokens[2]);
        }
        fu.close();
        System.out.println("Total pairs: " + genePairs.size());
        List<String> groups = Arrays.asList("all");
        Map<String, List<Double>> pairToCor = calculateGeneRelationships(genePairs,
                                                                         groups,
                                                                         "latent_time",
                                                                         "velocity",
                                                                         7,
                                                                         "spearman");
        System.out.println("calculateGeneRelationships:");
        pairToCor.forEach((pair, cor) -> System.out.println(pair + "\t" + cor.get(0) + "\t" + cor.get(1)));
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
    
    public String openData(String dir) throws JsonEOFException, IOException {
        return (String) callJSONServer("open_data",
                                       dir);
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
    public Map<String, List<?>> project(String dir) throws JsonEOFException, IOException {
        Object result = callJSONServer("project", 
                                       dir,
                                       ScNetworkManager.getManager().isForRNAVelocity() + "");
        if (result instanceof String) // An error
            throw new IllegalStateException(result.toString());
        // There are only two types: String or List of String
        // Convert to a double array
        Map<String, List<?>> map = (Map<String, List<?>>) result;
        return map;
    }
    
    /**
     * To run this test method, make sure testLoadData() is called first to create a reference dataset.
     * @throws JsonEOFException
     * @throws IOException
     */
    @Test
    public void testProject() throws Exception {
        testLoadData();
        String dir = "/Users/wug/Documents/missy_single_cell/seq_data_v2/12_5_gfp/filtered_feature_bc_matrix";
        Map<String, List<?>> cellToUmap = project(dir);
        int count = 0;
        for (String cell : cellToUmap.keySet()) {
            if (count ++ == 10)
                break;
            System.out.println(cell + ": " + cellToUmap.get(cell));
        }
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
    
    @Test
    public void testInferCellRoot() throws Exception {
        // Without target clusters
        Object result = callJSONServer("infer_cell_root");
        System.out.println(result);
        // Specify a target candidate cluster
        List<String> clusters = new ArrayList<>();
        clusters.add("8");
        result = callJSONServer("infer_cell_root", clusters.toArray(new String[] {}));
        System.out.println(result);
        clusters.add("2");
        result = callJSONServer("infer_cell_root", clusters.toArray(new String[] {}));
        System.out.println(result);
        clusters.clear();
        clusters.add("9");
        result = callJSONServer("infer_cell_root", clusters.toArray(new String[] {}));
        System.out.println(result);
    }
    
    @Test
    public void test() throws Exception {
        List<String> list = new ArrayList<>();
        list.add("8");
        list.add("2");
        System.out.println(list);
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(list));
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
    
    @Test
    public void testPerformCytoTrace() throws Exception {
        List<Double> result = performCytoTrace();
        System.out.println("Result: " + result.size());
        result.subList(0, 10).forEach(System.out::println);
    }
    
    @Test
    public void testPerformDPA() throws Exception {
        String rootCell = "TTGACCCGTTAGCGGA-1";
        List<Double> result = performDPT(rootCell);
        System.out.println("Result: " + result.size());
        result.subList(0, 10).forEach(System.out::println);
    }
    
    @Test
    public void testPreprocess() throws Exception {
        Object result = callJSONServer("preprocess_data", 
                                       "", // For regressout keys
                                       ""); // For imputation
        System.out.println(result);
    }
    
    @Test
    public void testGeneGeneExp() throws Exception {
        String gene = "Cps1";
        List<Double> values = getGeneExp(gene);
        System.out.println(values.size() + ": " + values);
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
    
    @Test
    public void testGetCellFeatureNames() throws IOException {
        List<String> featureNames = getCellFeatureNames();
        featureNames.forEach(System.out::println);
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
    
    
    @Test
    public void testLoadData() throws Exception {
        List<String> list = Collections.EMPTY_LIST;
        System.out.println("Emtpty list: " + String.join(",", list));
//        String query = "{\"jsonrpc\": \"2.0\", \"method\": \"echo\", \"id\": 2, \"params\":[\"test\"]}";
        RequestObject request = new RequestObject();
        request.id = 2;
        request.method = "echo";
        request.addParams("This is a test!");
        callJSONServer(request);
        
        String dir_17_5 = "/Users/wug/Documents/missy_single_cell/seq_data_v2/17_5_gfp/filtered_feature_bc_matrix";
        String text = openData(dir_17_5);
        System.out.println("Open data: " + text);
        text = preprocessData(null, null);
        System.out.println("Preprocess data: " + text);
        text = clusterData();
        System.out.println("Cluster data: " + text);
        
        String writeFileName = "/Users/wug/temp/17_5_gfp.h5ad";
        text = writeData(writeFileName);
        System.out.println("Write data: " + text);
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
    public void testScVeloFunctions() throws Exception {
        String fileName = "/Users/wug/Documents/missy_single_cell/velocity/possorted_genome_bam_DP1YJ_E17_5.loom";
        Object obj = callJSONServer("scv_open", fileName);
        System.out.println("scv_open:\n" + obj);
        obj = callJSONServer("scv_preprocess");
        System.out.println("scv_preprocess:\n" + obj);
        obj = callJSONServer("scv_velocity", ScvVelocityMode.dynamical.toString());
        System.out.println("scv_velocity:\n" + obj);
//        obj = callJSONServer("scv_embedding", "Notch2");
//        System.out.println("scv_embedding:\n" + obj);
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
    public void testGetCellFeature() throws Exception {
        String[] features = {"n_genes", "pct_counts_mt"};
        for (String feature : features) {
            List<Object> result = getCellFeature(feature);
            System.out.println(feature + ": " + result.size());
            result.subList(0, 10).forEach(System.out::println);
        }
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
    
    //TODO: Add a parameter for top genes, which should be passed to the Python server to control
    // the size of text between processes.
    public Map<String, List<List<Double>>> findGroupMarkers(int topGenes) throws JsonEOFException, IOException {
        Object result = callJSONServer("rank_genes_groups"); // This is more like to find markers for individual clusters
        if (result instanceof String)
            throw new IllegalStateException(result.toString());
        return (Map<String, List<List<Double>>>) result;
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
    
    @Test
    public void stopServer() throws Exception {
        RequestObject request = new RequestObject();
        request.id = 100;
        request.method = "stop";
        callJSONServer(request);
    }

    private ResponseObject callJSONServer(RequestObject request) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        // For some NaN, infinity, etc.
        mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
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
