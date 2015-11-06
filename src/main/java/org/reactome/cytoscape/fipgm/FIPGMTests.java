/*
 * Created on Nov 5, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.reactome.factorgraph.Variable;

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
    public void testLoadResults() throws Exception {
        String fileName = "/Users/gwu/Documents/EclipseWorkspace/caBigR3/test_data/tcga_ov/fi_pgm_ov_cnv_mutation_random_100.xml";
        File file = new File(fileName);
        FIPGMResults results = FIPGMResults.getResults();
        results.loadResults(file);
        System.out.println("Total samples: " + results.getObservations().size());
        System.out.println("Total random samples: " + results.getRandomObservations().size());
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
