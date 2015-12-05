/*
 * Created on Nov 5, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.Test;
import org.reactome.cytoscape.service.FINetworkService;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape3.FIPlugInHelper;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.VariableAssignment;

/**
 * A list of test and checking methods is placed here.
 * @author gwu
 *
 */
public class FIPGMTests {
    
    /**
     * Default constructor.
     */
    public FIPGMTests() {
    }
    
    @Test
    public void testMCLClusters() throws Exception {
        FINetworkService networkService = FIPlugInHelper.getHelper().getNetworkService();
        Set<String> fis = networkService.queryAllFIs();
        System.out.println("Total FIs: " + fis.size());
        
//        String fileName = "/Users/gwu/Documents/EclipseWorkspace/caBigR3/test_data/tcga_ov/fi_pgm_ov_mutation_random_100.xml";
//        File file = new File(fileName);
//        FIPGMResults results = FIPGMResults.getResults();
//        results.loadResults(file);
//        System.out.println("Total samples: " + results.getObservations().size());
//        System.out.println("Total random samples: " + results.getRandomObservations().size());
//        
//        Map<String, Map<Variable, Double>> sampleToVarToScore = results.getSampleToVarToScore();
//        Map<String, Double> geneToScore = new HashMap<>();
//        for (String sample : sampleToVarToScore.keySet()) {
//            Map<Variable, Double> varToScore = sampleToVarToScore.get(sample);
//            for (Variable var : varToScore.keySet()) {
//                Double score = varToScore.get(var);
//                Double total = geneToScore.get(var.getName());
//                if (total == null)
//                    geneToScore.put(var.getName(), score);
//                else
//                    geneToScore.put(var.getName(), score + total);
//            }
//        }
//        // Get the average
//        for (String gene : geneToScore.keySet()) {
//            geneToScore.put(gene, geneToScore.get(gene) / sampleToVarToScore.size());
//        }
        // Generate a FI to score for MCL
        Set<String> fisWithScore = new HashSet<>();
        for (String fi : fis) {
//            int index = fi.indexOf("\t");
//            String gene1 = fi.substring(0, index);
//            String gene2 = fi.substring(index + 1);
//            Double score1 = geneToScore.get(gene1);
//            Double score2 = geneToScore.get(gene2);
//            if (score1 == null || score2 == null)
//                continue;
//            Double fiScore = (score1 + score2) / 2.0d;
            fisWithScore.add(fi + "\t" + 1.0d);
//            System.out.println(fi + "\t" + fiScore);
        }
        RESTFulFIService service = new RESTFulFIService();
        Element resultElm = service.doMCLClustering(fisWithScore, 7.5d);
        List<Set<String>> clusters = parseClusterResults(resultElm);
        for (int i = 0; i < clusters.size(); i++) {
            Set<String> cluster = clusters.get(i);
            System.out.println("Cluster " + i + "\t" + cluster.size());
        }
    }
    
    private List<Set<String>> parseClusterResults(Element resultElm) throws JDOMException {
        List<Set<String>> clusters = new ArrayList<Set<String>>();
        // // This is for test
        // String error = resultElm.getChildText("error");
        // System.out.println("Error output: \n" + error);
        List<?> children = resultElm.getChildren("clusters");
        for (Iterator<?> it = children.iterator(); it.hasNext();)
        {
            Element child = (Element) it.next();
            String text = child.getTextTrim();
            Set<String> cluster = new HashSet<String>();
            String[] tokens = text.split("\t");
            for (String token : tokens)
            {
                cluster.add(token);
            }
            clusters.add(cluster);
        }
        return clusters;
    }
    
    public static void main(String[] args) throws Exception {
        FIPGMTests tests = new FIPGMTests();
        tests.testCompareResults();
    }
    
    @Test
    public void testCompareResults() throws Exception {
        String fileName = "/Users/gwu/Documents/EclipseWorkspace/caBigR3/test_data/tcga_ov/fi_pgm_ov_cnv_mutation_random_100_110515.xml";
        File file = new File(fileName);
        FIPGMResults results = FIPGMResults.getResults();
        results.loadResults(file);
        System.out.println("Total samples: " + results.getObservations().size());
        System.out.println("Total random samples: " + results.getRandomObservations().size());
        PGMImpactAnalysisResultDialog dialog = new PGMImpactAnalysisResultDialog(null);
        dialog.setSampleResults(results.getSampleToVarToScore(),
                                results.getRandomSampleToVarToScore());
    }
    
    @Test
    public void testLoadResults() throws Exception {
        String fileName = "/Users/gwu/Documents/EclipseWorkspace/caBigR3/test_data/tcga_ov/fi_pgm_ov_cnv_mutation_random_100_110515.xml";
        File file = new File(fileName);
        FIPGMResults results = FIPGMResults.getResults();
        long time1 = System.currentTimeMillis();
        results.loadResults(file);
        long time11 = System.currentTimeMillis();
        System.out.println("Time for loading: " + (time11 - time1));
        System.out.println("Total samples: " + results.getObservations().size());
        System.out.println("Total random samples: " + results.getRandomObservations().size());
        for (Observation<Number> obs : results.getObservations()) {
            for (VariableAssignment<Number> varAssgn : obs.getVariableAssignments()) {
                Variable var = varAssgn.getVariable();
                if (var.getName().startsWith("EGFR_")) {
                    System.out.println(obs.getName() + "\t" +
                                       var.getName() + "\t" + varAssgn.getAssignment());
                }
            }
        }
        Map<String, Map<Variable, Double>> sampleToVarToScore = results.getSampleToVarToScore();
        System.out.println();
        for (String sample : sampleToVarToScore.keySet()) {
            Map<Variable, Double> varToScore = sampleToVarToScore.get(sample);
            for (Variable var : varToScore.keySet()) {
                if (var.getName().equals("EGFR"))
                    System.out.println(sample + "\t" + var.getName() + "\t" + varToScore.get(var));
            }
        }
        long time2 = System.currentTimeMillis();
        System.out.println("Total time: " + (time2 - time1));
    }
    
    @Test
    public void checkTP53Scores() throws Exception {
        String dirName = "/Users/gwu/Documents/EclipseWorkspace/caBigR3/test_data/tcga_ov/";
        String fileName = "fi_pgm_ov_mutation_random_100.xml";
        fileName = "fi_pgm_ov_cnv_random_1000.xml";
        File file = new File(dirName + fileName);
        FIPGMResults results = FIPGMResults.getResults();
        results.loadResults(file);
        
        // Get the variable for TP53
        Map<String, Variable> nameToVar = results.getNameToVariable();
        Variable tp53Var = nameToVar.get("TP53");
        Set<Variable> vars = new HashSet<Variable>();
        vars.add(tp53Var);
        
        Map<String, Map<Variable, Double>> sampleToVarToScore = results.getSampleToVarToScore(vars);
        Map<String, Map<Variable, Double>> randomSampleToVarToScore = results.getRandomSampleToVarToScore(vars);
        System.out.println("RealSamples");
        for (String sample : sampleToVarToScore.keySet()) {
            Map<Variable, Double> varToScore = sampleToVarToScore.get(sample);
            Double score = varToScore.get(tp53Var);
            System.out.println(score);
        }
        System.out.println("RandomSamples");
        for (String sample : randomSampleToVarToScore.keySet()) {
            Map<Variable, Double> varToScore = randomSampleToVarToScore.get(sample);
            Double score = varToScore.get(tp53Var);
            System.out.println(score);
        }
    }
    
}
