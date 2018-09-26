/*
 * Created on Sep 19, 2018
 *
 */
package org.reactome.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.junit.Test;
import org.reactome.cytoscape.drug.DrugDataSource;
import org.reactome.cytoscape.drug.DrugTargetInteractionParser;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.r3.util.FileUtility;

import edu.ohsu.bcb.druggability.dataModel.Drug;

/**
 * This class is used to perform drug systematic analysis based on caBigR3Web application to 
 * avoid re-programming anything complicated. The analysis calls a several RESTful API
 * deployed locally for performance reason.
 * @author wug
 *
 */
public class DrugImpactSystematicAnalyzer {
    private final String DIR = "/Users/wug/Documents/eclipse_workspace/caBIGR3/results/BooleanNetwork/drugs/";
    
    public DrugImpactSystematicAnalyzer() {
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("java -Xmx8G DrugImpactSystematicAnalyzer {Targetome|DrugCentral} {output_dir} (optional)");
            System.exit(1);
        }
        DrugDataSource dataSource = DrugDataSource.valueOf(args[0]);
        DrugImpactSystematicAnalyzer analyzer = new DrugImpactSystematicAnalyzer();
        analyzer.performImpactAnalysis(dataSource, (args.length > 1 ? args[1] : null));
    }
    
    public void performImpactAnalysis(DrugDataSource dataSource,
                                      String dirName) throws Exception {
        List<Drug> drugs = getDrugs(dataSource.name());
        RESTFulFIService service = new RESTFulFIService();
        FileUtility fu = new FileUtility();
        if (dirName == null)
            dirName = DIR;
        String fileName = dirName + dataSource + "_Impact_091918.txt";
        fu.setOutput(fileName);
        fu.printLine("Drug\tDB_ID\tPathway\tSum\tOutputAverage\tTargets");
        int c = 0;
        for (Drug drug : drugs) {
            String results = service.runDrugImpactAnalysis(drug.getDrugName(), dataSource.name());
            String[] lines = results.split("\n");
            for (String line : lines)
                fu.printLine(drug.getDrugName() + "\t" + line);
            c ++;
//            if (c == 5)
//                break;
        }
        fu.close();
        System.out.println("Total drug checked: " + c);
    }
    
    public List<Drug> getDrugs(String dataSource) throws Exception {
        RESTFulFIService service = new RESTFulFIService();
        Element element = service.listDrugs(dataSource);
        DrugTargetInteractionParser parser = new DrugTargetInteractionParser();
        parser.parse(element);
        Map<String, Drug> idToDrug = parser.getIdToDrug();
        return new ArrayList<>(idToDrug.values());
    }
    
    @Test
    public void testGetDrugs() throws Exception {
        List<Drug> drugs = getDrugs("targetome");
        drugs.forEach(drug -> System.out.println(drug));
    }

}
