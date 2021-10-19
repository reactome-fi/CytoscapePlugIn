package org.reactome.cytoscape.sc.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.JFrame;

import org.junit.Test;
import org.reactome.cytoscape.sc.utils.ScPathwayMethod;
import org.reactome.cytoscape.sc.utils.ScvVelocityMode;
import org.reactome.r3.util.FileUtility;

import com.fasterxml.jackson.core.io.JsonEOFException;

import smile.plot.swing.Canvas;
import smile.plot.swing.ScatterPlot;

public class JSONSeverTests {
    private JSONServerCaller caller;
    
    public JSONSeverTests() {
        caller = new JSONServerCaller();
    }
    
    @Test
    public void testGetAnalyzedPathwayKeys() throws Exception {
        caller.setIsStarted(true); // Avoid to restart the server
        String fileName = "/Users/wug/Documents/wgm/work/FIPlugIns/test_data/ScRNASeq/SavedResults/mouse/17_5_gfp.h5ad";
        String text = caller.openAnalyzedData(fileName); // Use this method so that we get pre-processed data
        System.out.println("Read back the saved results:\n" + text);
        List<String> keys = caller.getAnalyzedPathwayKeys();
        System.out.println("Analyzed pathway keys: " + keys);
        fileName = "/Users/wug/Documents/missy_single_cell/test_results/17_5_gfp_with_pathways.h5ad";
        text = caller.openAnalyzedData(fileName);
        System.out.println("Read back the saved results:\n" + text);
        keys = caller.getAnalyzedPathwayKeys();
        System.out.println("Analyzed pathway keys: " + keys);
    }
    
    @Test
    public void testPathwayAnalysis() throws Exception {
        caller.setIsStarted(true); // Avoid to restar the server
        String fileName = "/Users/wug/Documents/wgm/work/FIPlugIns/test_data/ScRNASeq/SavedResults/mouse/17_5_gfp.h5ad";
        String text = caller.openAnalyzedData(fileName); // Use this method so that we get pre-processed data
        System.out.println("Read back the saved results:\n" + text);
        String reactomeGMT = "/Users/wug/git/reactome-fi/fi_sc_analysis/python/data/gmt/MouseReactomePathways_Rel_75_122220_small.gmt";
        Object result = caller.doPathwayAnalysis(reactomeGMT,
                                                 ScPathwayMethod.aucell);
        System.out.println("Results: " + result.toString());
        result = caller.doPathwayAnalysis(reactomeGMT,
                                          ScPathwayMethod.ssgsea);
        System.out.println("Results: " + result.toString());
//        Object result = "X_aucell";
//        Map<String, Map<String, Double>> anovaResults = caller.doPathwayAnova(result.toString());
//        System.out.println("Anova Results: " + anovaResults.size());
        // The output is a huge string. Avoid to print it out.
//        System.out.println("Results for aucell: \n" + result);
//        fileName = "/Users/wug/temp/17_5_gfp_with_pathways.h5ad";
//        caller.writeData(fileName);
    }
    
    @Test
    public void testTFsAnalysis() throws Exception {
        caller.setIsStarted(true);
        String fileName = "/Users/wug/Documents/wgm/work/FIPlugIns/test_data/ScRNASeq/SavedResults/mouse/17_5_gfp.h5ad";
        String text = caller.openAnalyzedData(fileName); // Use this method so that we get pre-processed data
        System.out.println("Read back the saved results:\n" + text);
        String dorotheaFileName = "/Volumes/ssd/datasets/dorothea/dorothea_mm.tsv";
        Object result = caller.doTFsAnalysis(dorotheaFileName,
                                             ScPathwayMethod.aucell);
        System.out.println("Results: " + result.toString());
        result = caller.doTFsAnalysis(dorotheaFileName,
                                      ScPathwayMethod.ssgsea);
        System.out.println("Results: " + result.toString());
        fileName = "/Users/wug/temp/17_5_gfp_with_tfs.h5ad";
        caller.writeData(fileName);
    }
    
    @Test
    public void testPathwayAnova() throws Exception {
        caller.setIsStarted(true);
        String fileName = "/Users/wug/temp/17_5_gfp_with_pathways.h5ad";
        String result = caller.openAnalyzedData(fileName);
        System.out.println("Result:\n" + result);
        String pathwayKey = "X_aucell";
        Map<String, Map<String, Double>> anovaResults = caller.doPathwayAnova(pathwayKey);
        System.out.println("Anova results: " + anovaResults.size());
        String pathway = anovaResults.keySet().stream().findAny().get();
        System.out.println(pathway + ":\n" + anovaResults.get(pathway));
        Map<String, Double> pathwayActivities = caller.getPathwayActivities(pathway,
                                                                            pathwayKey);
        System.out.println("Total cells: " + pathwayActivities.size());
        String cell = pathwayActivities.keySet().stream().findAny().get();
        System.out.println(cell + ": " + pathwayActivities.get(cell));
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
        Map<String, List<Double>> pairToCor = caller.calculateGeneRelationships(genePairs,
                                                                         groups,
                                                                         "latent_time",
                                                                         "velocity",
                                                                         7,
                                                                         "spearman");
        System.out.println("calculateGeneRelationships:");
        pairToCor.forEach((pair, cor) -> System.out.println(pair + "\t" + cor.get(0) + "\t" + cor.get(1)));
    }
    
    
    /**
     * To run this test method, make sure testLoadData() is called first to create a reference dataset.
     * @throws JsonEOFException
     * @throws IOException
     */
    @Test
    public void testProject() throws Exception {
        caller.setIsStarted(true);
        testLoadData();
        String dir = "/Users/wug/Documents/missy_single_cell/seq_data_v2/12_5_gfp/filtered_feature_bc_matrix";
        Map<String, List<?>> cellToUmap = caller.project(dir, "read_10x_mtx", false);
        int count = 0;
        for (String cell : cellToUmap.keySet()) {
            if (count ++ == 10)
                break;
            System.out.println(cell + ": " + cellToUmap.get(cell));
        }
    }
    
    @Test
    public void testInferCellRoot() throws Exception {
        // Without target clusters
        Object result = caller.callJSONServer("infer_cell_root");
        System.out.println(result);
        // Specify a target candidate cluster
        List<String> clusters = new ArrayList<>();
        clusters.add("8");
        result = caller.callJSONServer("infer_cell_root", clusters.toArray(new String[] {}));
        System.out.println(result);
        clusters.add("2");
        result = caller.callJSONServer("infer_cell_root", clusters.toArray(new String[] {}));
        System.out.println(result);
        clusters.clear();
        clusters.add("9");
        result = caller.callJSONServer("infer_cell_root", clusters.toArray(new String[] {}));
        System.out.println(result);
    }
    
    @Test
    public void testServer() throws Exception {
        caller.startServer();
        caller.stopServer();
    }
    
    @Test
    public void testPerformCytoTrace() throws Exception {
        List<Double> result = caller.performCytoTrace();
        System.out.println("Result: " + result.size());
        result.subList(0, 10).forEach(System.out::println);
    }
    
    @Test
    public void testPerformDPA() throws Exception {
        String rootCell = "TTGACCCGTTAGCGGA-1";
        List<Double> result = caller.performDPT(rootCell);
        System.out.println("Result: " + result.size());
        result.subList(0, 10).forEach(System.out::println);
    }
    
    @Test
    public void testPreprocess() throws Exception {
        Object result = caller.callJSONServer("preprocess_data", 
                                       "", // For regressout keys
                                       ""); // For imputation
        System.out.println(result);
    }
    
    @Test
    public void testGeneGeneExp() throws Exception {
        String gene = "Cps1";
        List<Double> values = caller.getGeneExp(gene);
        System.out.println(values.size() + ": " + values);
    }
    
    @Test
    public void testGetCellFeatureNames() throws IOException {
        List<String> featureNames = caller.getCellFeatureNames();
        featureNames.forEach(System.out::println);
    }
    
    @Test
    public void testLoadData() throws Exception {
        caller.setIsStarted(true);
        @SuppressWarnings("unchecked")
        List<String> list = Collections.EMPTY_LIST;
        System.out.println("Emtpty list: " + String.join(",", list));
//        String query = "{\"jsonrpc\": \"2.0\", \"method\": \"echo\", \"id\": 2, \"params\":[\"test\"]}";
        RequestObject request = new RequestObject();
        request.id = 2;
        request.method = "echo";
        request.addParams("This is a test!");
        ResponseObject rtn = caller.callJSONServer(request);
        System.out.println("echo return: " + rtn.getResult());
        
//        String dir_17_5 = "/Users/wug/Documents/missy_single_cell/seq_data_v2/17_5_gfp/filtered_feature_bc_matrix";
//        String text = openData(dir_17_5, "read_10x_mtx");
        
        String dir_17_5 = "/Users/wug/git/reactome-fi/fi_sc_analysis/cache/Users-wug-Documents-missy_single_cell-seq_data_v2-17_5_gfp-filtered_feature_bc_matrix-matrix.h5ad";
        String text = caller.openData(dir_17_5, "read_h5ad");
        
        System.out.println("Open data: " + text);
        text = caller.preprocessData(null, null);
        System.out.println("Preprocess data: " + text);
        text = caller.clusterData();
        System.out.println("Cluster data: " + text);
        
        String writeFileName = "/Users/wug/temp/17_5_gfp.h5ad";
        text = caller.writeData(writeFileName);
        System.out.println("Write data: " + text);
    }
    
    @Test
    public void testGetConnectivities() throws Exception {
        List<List<String>> list = caller.getConnectivities();
        List<String> first = list.get(0);
        System.out.println("First: " + first);
    }
    
    @Test
    public void testGetPaga() throws Exception {
        Map<String, List<List<Double>>> result = caller.getPAGA();
        System.out.println(result);
    }
    
    @Test
    public void testScVeloFunctions() throws Exception {
        String fileName = "/Users/wug/Documents/missy_single_cell/velocity/possorted_genome_bam_DP1YJ_E17_5.loom";
        Object obj = caller.callJSONServer("scv_open", fileName);
        System.out.println("scv_open:\n" + obj);
        obj = caller.callJSONServer("scv_preprocess");
        System.out.println("scv_preprocess:\n" + obj);
        obj = caller.callJSONServer("scv_velocity", ScvVelocityMode.dynamical.toString());
        System.out.println("scv_velocity:\n" + obj);
//        obj = callJSONServer("scv_embedding", "Notch2");
//        System.out.println("scv_embedding:\n" + obj);
    }
    
    @Test
    public void testUMap() throws Exception {
        caller.setIsStarted(true);
        List<List<Double>> list = caller.getUMAP();
        System.out.println("List size: " + list.size());
        double[][] umap = new double[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            List<Double> coords = list.get(i);
            umap[i] = new double[2];
            umap[i][0] = coords.get(0);
            umap[i][1] = coords.get(1);
        }
        List<Integer> result = caller.getCluster();
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
            List<Object> result = caller.getCellFeature(feature);
            System.out.println(feature + ": " + result.size());
            result.subList(0, 10).forEach(System.out::println);
        }
    }
    
    @Test
    public void testGetCellIds() throws Exception {
        List<String> cellIds = caller.getCellIds();
        System.out.println("Size of cell ids: " + cellIds.size());
        List<List<Double>> umap = caller.getUMAP();
        System.out.println("Size of umap: " + umap.size());
        // Note: cell ids and umap sizes are not the same!
        if (cellIds.size() != umap.size())
            throw new IllegalStateException("CellIds and umap have different sizes!");
    }
    
    // Note: If a test method cannot work, make sure @Test has not been added for methods
    // returning something!
    @Test
    public void testStopServer() throws Exception {
        caller.setIsStarted(true);
        caller.stopServer();
    }

}
