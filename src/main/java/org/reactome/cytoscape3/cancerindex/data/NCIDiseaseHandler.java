/*
 * Created on Jul 15, 2010
 *
 */
package org.reactome.cytoscape3.cancerindex.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.reactome.cytoscape3.cancerindex.model.DiseaseData;
import org.reactome.r3.util.FileUtility;

/**
 * This class is used to handle NCI disease terms and codes.
 * @author wgm
 *
 */
public class NCIDiseaseHandler {
    public static final String DEFAULT_DISEASE_FILE_NAME = "datasets/NCI_CancerIndex_allphases_disease/Disease_Thesaurus_10.05d.txt";
    // Keep track loaded disease
    private Map<String, DiseaseData> codeToDiseaseData;
    
    public NCIDiseaseHandler() {
    }
    
    public NCIDiseaseHandler(String fileName) throws IOException {
        loadData(fileName);
    }
    
    public Map<String, DiseaseData> getCodeToDisease() {
        return codeToDiseaseData;
    }
    
    private void loadData(String fileName) throws IOException {
        FileReader fileReader = new FileReader(fileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        loadData(bufferedReader);
        bufferedReader.close();
        fileReader.close();
    }
    
    public void loadData(BufferedReader reader) throws IOException {
        if (codeToDiseaseData == null)
            codeToDiseaseData = new HashMap<String, DiseaseData>();
        else
            codeToDiseaseData.clear();
        String line = null;
        Map<String, DiseaseData> termToDisease = new HashMap<String, DiseaseData>();
        Map<String, String[]> termToSupTerms = new HashMap<String, String[]>();
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split("\t");
            DiseaseData disease = new DiseaseData();
            disease.setNciDiseaseConceptCode(tokens[0]);
            disease.setMatchedDiseaseTerm(tokens[1]);
            if (tokens.length > 4 && tokens[4].length() > 0) {
                disease.setDefinition(tokens[4]);
            }
            String[] supTerms = tokens[2].split("\\|");
            termToDisease.put(tokens[1],
                              disease);
            termToSupTerms.put(tokens[1],
                               supTerms);
        }
        // Need to figure out sup terms
        for (String term : termToSupTerms.keySet()) {
            DiseaseData disease = termToDisease.get(term);
            String[] supTerms = termToSupTerms.get(term);
            if (supTerms == null || supTerms.length == 0)
                continue;
            for (String supTerm : supTerms) {
                if (supTerm.equals("root_node"))
                    continue;
                DiseaseData supDisease = termToDisease.get(supTerm);
                if (supDisease == null) {
                    throw new IllegalStateException(supTerm + " has no object!");
                }
                disease.addSupTerm(supDisease);
            }
        }
        // Time to figure out sub terms
        for (String term : termToDisease.keySet()) {
            DiseaseData disease = termToDisease.get(term);
            codeToDiseaseData.put(disease.getNciDiseaseConceptCode(),
                                  disease);
            List<DiseaseData> supTerms = disease.getSupTerms();
            if (supTerms == null || supTerms.size() == 0) {
                // Print out the top-level terms
//                System.out.println(disease.getNciDiseaseConceptCode() + "\t" +
//                                   disease.getMatchedDiseaseTerm());
                continue;
            }
            for (DiseaseData sup : supTerms) {
                sup.addSubTerm(disease);
            }
        }
    }
    
    /**
     * Grep sub disease terms.
     * @param code
     * @param needRecursive true for getting all contained diseases.
     * @return
     */
    public Set<DiseaseData> getSubDiseases(String code,
                                           boolean needRecursive) {
        Set<DiseaseData> rtn = new HashSet<DiseaseData>();
        DiseaseData data = codeToDiseaseData.get(code);
        if (data == null)
            throw new IllegalArgumentException(code + " has no disease data!");
        List<DiseaseData> subTerms = data.getSubTerms();
        if (subTerms != null)
            rtn.addAll(subTerms);
        if (needRecursive && subTerms != null) {
            for (DiseaseData subTerm : subTerms)
                getSubDiseases(subTerm, 
                               rtn);
        }
        return rtn;
    }
    
    public Set<String> getTumorCodes() {
        String rootCode = "C3262"; // Neoplasm. Cannot find a good root for cancer only. 
        // It seems that many cancers are not included in malignant neoplasm (e.g. HCC)
        Set<String> tumorCodes = getSubDiseaseCodes("C3262", true);
        tumorCodes.add(rootCode);
        return tumorCodes;
    }
    
    /**
     * Get all descendant codes specified by the root code plus the root code
     * itself.
     * @param rootCode
     * @return
     */
    public Set<String> getDiseaseCodes(String rootCode) {
        Set<String> codes = getSubDiseaseCodes(rootCode, true);
        codes.add(rootCode);
        return codes;
    }
    
    public Set<String> getSubDiseaseCodes(String code,
                                          boolean needRecursive) {
        Set<DiseaseData> diseases = getSubDiseases(code, needRecursive);
        Set<String> rtn = new HashSet<String>();
        for (DiseaseData disease : diseases)
            rtn.add(disease.getNciDiseaseConceptCode());
        return rtn;
    }
    
    public DiseaseData getDiseaseData(String code) {
        if (codeToDiseaseData == null)
            throw new IllegalStateException("The data has not been loaded!");
        return codeToDiseaseData.get(code);
    }
    
    private void getSubDiseases(DiseaseData disease,
                                Set<DiseaseData> rtn) {
        List<DiseaseData> subTerms = disease.getSubTerms();
        if (subTerms == null || subTerms.size() == 0)
            return;
        rtn.addAll(subTerms);
        for (DiseaseData subDisease : subTerms) 
            getSubDiseases(subDisease, rtn);
    }
    
    @Test
    public void generateDiseaseOnlyThesaurus() throws IOException {
        String fileName = "datasets/NCI_CancerIndex_allphases_disease/Thesaurus_10.05d.txt";
        String target = "datasets/NCI_CancerIndex_allphases_disease/Disease_Thesaurus_10.05d.txt";
        loadData(fileName);
        DiseaseData root = getDiseaseData("C7057");
        Set<DiseaseData> diseases = getSubDiseases("C7057",
                                                   true);
        diseases.add(root);
        Set<String> diseaseTerms = new HashSet<String>();
        for (DiseaseData disease : diseases)
            diseaseTerms.add(disease.getNciDiseaseConceptCode());
        FileUtility fu = new FileUtility();
        fu.setInput(fileName);
        FileUtility outFu = new FileUtility();
        outFu.setOutput(target);
        String line = null;
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            if (diseaseTerms.contains(tokens[0]))
                outFu.printLine(line);
        }
        outFu.close();
        fu.close();
        loadData(fileName);
    }
    
    @Test
    public void testLoadData() throws IOException {
        String fileName = "datasets/NCI_CancerIndex_allphases_disease/Disease_Thesaurus_10.05d.txt";
        long time1 = System.currentTimeMillis();
        long memory = Runtime.getRuntime().totalMemory();
        loadData(fileName);
        long memory1 = Runtime.getRuntime().totalMemory();
        System.out.println("Used memory: " + (memory1 - memory));
        long time2 = System.currentTimeMillis();
        System.out.println("Time for loading: " + (time2 - time1));
        DiseaseData root = getDiseaseData("C7057");
        Set<DiseaseData> diseases = getSubDiseases("C7057",
                                                   true);
        diseases.add(root);
        System.out.println("Total disease terms: " + diseases.size());
    }
}
