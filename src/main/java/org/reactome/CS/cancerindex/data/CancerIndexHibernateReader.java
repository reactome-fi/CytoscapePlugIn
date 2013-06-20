/*
 * Created on Jul 15, 2010
 *
 */
package org.reactome.CS.cancerindex.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.reactome.CS.cancerindex.model.DiseaseData;
import org.reactome.CS.cancerindex.model.Roles;
import org.reactome.CS.cancerindex.model.Sentence;
import org.reactome.r3.util.InteractionUtilities;

/**
 * This class is used to load CancerIndex data based on hibernate API.
 * @author wgm
 *
 */
public class CancerIndexHibernateReader {
    
    public CancerIndexHibernateReader() {
    }
    
    public Session openSession() throws Exception {
        SessionFactory sf = new CancerIndexHibernateUtility().initSession();
        return sf.openSession();
    }
    
    public Map<String, Set<String>> loadGeneToDiseaseCodes() throws Exception {
        Session session = openSession();
        String sqlQuery = "SELECT g.HUGO_GENE_SYMBOL, d.NCI_DISEASE_CONCEPT_CODE " +
                          "FROM Sentence s, GENE_ENTRY g, DISEASE_DATA d " +
                          "WHERE s.GENE_ENTRY_ID = g.ID AND s.DISEASE_DATA = d.ID AND " +
                          "s.NEGATION_INDICATOR != 'yes' AND s.SENTENCE_STATUS_FLAG = 'finished'";
        List<?> list = session.createSQLQuery(sqlQuery).list();
        Map<String, Set<String>> geneToDiseaseCodes = new HashMap<String, Set<String>>();
        for (Iterator<?> it = list.iterator(); it.hasNext();) {
            Object[] values = (Object[]) it.next();
            InteractionUtilities.addElementToSet(geneToDiseaseCodes,
                                                 values[0].toString(), 
                                                 values[1].toString());
        }
        session.close();
        return geneToDiseaseCodes;
    }
    
    @Test
    public void testLoadGeneToDiseasesForGeneSet() throws Exception {
        Session session = openSession();
        String sqlQuery = "SELECT g.HUGO_GENE_SYMBOL, d.NCI_DISEASE_CONCEPT_CODE " +
                          "FROM Sentence s, GENE_ENTRY g, DISEASE_DATA d " +
                          "WHERE s.GENE_ENTRY_ID = g.ID AND s.DISEASE_DATA = d.ID AND " +
                          "s.NEGATION_INDICATOR != 'yes' AND g.HUGO_GENE_SYMBOL in ('EGFR', 'TP53', 'RB1', 'NF1')";
        List<?> list = session.createSQLQuery(sqlQuery).list();
        for (Iterator<?> it = list.iterator(); it.hasNext();) {
            Object[] values = (Object[]) it.next();
            System.out.println(values[0] + "\t" + values[1]);
        }
        session.close();
    }
    
    public Map<String, Set<String>> loadDiseaseCodeToGenes() throws Exception {
        Map<String, Set<String>> geneToCodes = loadGeneToDiseaseCodes();
        return InteractionUtilities.switchKeyValues(geneToCodes);
    }
    
    @Test
    public void listGenesForCommonCancers() throws Exception {
        Map<String, Set<String>> diseaseToGenes = loadDiseaseCodeToGenes();
        String commonCarcinomaCodes = "C7629";
        NCIDiseaseHandler diseaseHandler = new NCIDiseaseHandler(NCIDiseaseHandler.DEFAULT_DISEASE_FILE_NAME);
        Set<DiseaseData> subTerms = diseaseHandler.getSubDiseases(commonCarcinomaCodes, 
                                                                  false);
        System.out.println("Total sub types: " + subTerms.size());
        System.out.println("Disease\tGenes");
        for (DiseaseData data : subTerms) {
            Set<String> diseaseCodes = diseaseHandler.getDiseaseCodes(data.getNciDiseaseConceptCode());
            Set<String> genes = new HashSet<String>();
            for (String code : diseaseToGenes.keySet()) {
                if (diseaseCodes.contains(code))
                    genes.addAll(diseaseToGenes.get(code));
            }
            System.out.println(data.getMatchedDiseaseTerm() + "\t" +
                               genes.size());
        }
        // This is used to list GBM genes
        Set<String> diseaseCodes = diseaseHandler.getDiseaseCodes("C3058");
        Set<String> genes = new HashSet<String>();
        for (String code : diseaseToGenes.keySet()) {
            if (diseaseCodes.contains(code))
                genes.addAll(diseaseToGenes.get(code));
        }
        System.out.println("Total genes for GBM: " + genes.size());
    }
    
    @Test
    public void checkDiseaseRelatedGenes() throws Exception {
        Session session = openSession();
        // Load all genes
        String queryString = "SELECT g.HUGO_GENE_SYMBOL FROM GENE_ENTRY g";
        List<?> list = session.createSQLQuery(queryString).list();
        // Get all genes
        Set<String> allGenes = new HashSet<String>();
        for (Iterator<?> it = list.iterator(); it.hasNext();) {
            String value = (String) it.next();
            allGenes.add(value);
        }
        session.close();
        // Some output
        System.out.println("Total genes: " + allGenes.size());
        Map<String, Set<String>> geneToDiseases = loadGeneToDiseaseCodes();
        System.out.println("Total disease related genes: " + geneToDiseases.size());
        List<String> nonDiseaseGenes = new ArrayList<String>(allGenes);
        nonDiseaseGenes.removeAll(geneToDiseases.keySet());
        System.out.println("Total non-disease related genes: " + nonDiseaseGenes.size());
//        Collections.sort(nonDiseaseGenes);
//        for (String gene : nonDiseaseGenes)
//            System.out.println(gene);
        // Check genes related to cancer disease only
        NCIDiseaseHandler diseaseHandler = new NCIDiseaseHandler(NCIDiseaseHandler.DEFAULT_DISEASE_FILE_NAME);
        String rootCode = "C3262"; // Neoplasm. Cannot find a good root for cancer only. 
                                   // It seems that many cancers are not included in malignant neoplasm (e.g. HCC)
        Set<String> tumorCodes = diseaseHandler.getSubDiseaseCodes("C3262", true);
        tumorCodes.add(rootCode);
        System.out.println("Tumor codes: " + tumorCodes.size());
        List<String> nonCancerGenes = new ArrayList<String>();
        for (String gene : geneToDiseases.keySet()) {
            Set<String> codes = geneToDiseases.get(gene);
            codes.retainAll(tumorCodes);
            if (codes.size() == 0)
                nonCancerGenes.add(gene);
        }
        System.out.println("Disease, but not tumor, related genes: " + nonCancerGenes.size());
        for (int i = 0; i < 10; i++) {
            String gene = nonCancerGenes.get(i);
            System.out.println("\t" + gene);
        }
    }
    
    @Test
    public void testListGeneToDiseases() throws Exception {
        long time1 = System.currentTimeMillis();
        long mem1 = Runtime.getRuntime().totalMemory();
        Map<String, Set<String>> geneToDiseaseCodes = loadGeneToDiseaseCodes();
        long time2 = System.currentTimeMillis();
        System.out.println("Total time: " + (time2 - time1));
        long mem2 = Runtime.getRuntime().totalMemory();
        System.out.println("Genes: " + geneToDiseaseCodes.size());
        System.out.println("Total memory: " + (mem2 - mem1));
        String gene = geneToDiseaseCodes.keySet().iterator().next();
        Set<String> diseaseTerms = geneToDiseaseCodes.get(gene);
        System.out.println(gene + ": " + diseaseTerms.size());
    }
    
    @Test
    public void testQuery() throws Exception {
        Session session = openSession();
      long time1 = System.currentTimeMillis();
      long mem1 = Runtime.getRuntime().totalMemory();
      String queryText = "SELECT s FROM GeneEntry as g inner join g.sentence as s WHERE g.hugoGeneSymbol = ?";
      String geneName = "NF1";
      List<?> list = session.createQuery(queryText).setString(0, geneName).list();
      System.out.println("Total sentences: " + list.size());
      JAXBContext jc = JAXBContext.newInstance(Sentence.class, DiseaseData.class);
      Marshaller marshaller = jc.createMarshaller();
      Sentence sentence = (Sentence) list.get(0);
      DiseaseData disease = sentence.getDiseaseData();
      System.out.println("Disease: " + disease.getMatchedDiseaseTerm());
      System.out.println("code: " + disease.getNciDiseaseConceptCode());
      System.out.println("id: " + disease.getId());
      //marshaller.marshal(sentence, System.out);
      marshaller.marshal(disease, System.out);
      System.out.println();
//        String queryText = "FROM " + GeneEntry.class.getName() + " g WHERE g.hugoGeneSymbol = ?";
//        Query query = session.createQuery(queryText);
//        
//        List<?> geneEntries = query.setString(0, geneName).list();
//        System.out.println("Total gene entries for " + geneName + ": " + geneEntries.size());

//        for (Iterator<?> it = geneEntries.iterator(); it.hasNext();) {
//            GeneEntry geneEntry = (GeneEntry) it.next();
//            System.out.println("Gene: " + geneEntry.getHugoGeneSymbol());
//            // Check Sentences
//            List<Sentence> sentences = geneEntry.getSentence();
//            System.out.println("Total sentences: " + sentences.size());
//            for (Sentence sentence : sentences) {
//                int c = 0;
//                System.out.println(sentence.getDiseaseData().getMatchedDiseaseTerm() + "\t" +
//                                   sentence.getEvidenceCode() + "\t" + 
//                                   sentence.getPubMedID() + "\t" + 
//                                   sentence.getStatement() + "\t" + 
//                                   getRoles(sentence));
//                if (c ++ == 10)
//                    continue;
//            }
//            
//        }
        session.close();
        long time2 = System.currentTimeMillis();
        long mem2 = Runtime.getRuntime().totalMemory();
        System.out.println("Total time: " + (time2 - time1));
        System.out.println("Total memory: " + (mem2 - mem1));
    }
    
    private String getRoles(Sentence sentence) {
        StringBuilder builder = new StringBuilder();
        List<Roles> roles = sentence.getRoles();
        for (Roles tmp : roles) {
            builder.append(tmp.getPrimaryNCIRoleCode());
            builder.append(tmp.getOtherRole());
        }
        return builder.toString();
    }
}
