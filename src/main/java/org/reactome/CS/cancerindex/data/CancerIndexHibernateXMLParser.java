/*
 * Created on Jul 14, 2010
 *
 */
package org.reactome.CS.cancerindex.data;

import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.FileInputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Test;
import org.reactome.CS.cancerindex.model.CancerIndexConstants;
import org.reactome.CS.cancerindex.model.DiseaseData;
import org.reactome.CS.cancerindex.model.GeneData;
import org.reactome.CS.cancerindex.model.GeneEntry;
import org.reactome.CS.cancerindex.model.Roles;
import org.reactome.CS.cancerindex.model.Sentence;
import org.reactome.CS.cancerindex.model.SequenceIdentificationCollection;

/**
 * This is a parser to cancer XML. Results from XML parsing have been saved into the database
 * directly via Hiberante API.
 * @author wgm
 *
 */
public class CancerIndexHibernateXMLParser {
    private GeneEntry currentGeneEntry;
    private Sentence currentSentence;
    private Roles currentRoles;
    private String currentText;
    // Check status
    private int totalGeneEntries;
    
    public CancerIndexHibernateXMLParser() {
    }
    
    /**
     * The actual place for doing parsing.
     * @param xmlFileName
     * @param session
     * @throws Exception
     */
    public void parse(String xmlFileName,
                      Session session) throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
        inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.TRUE);
        inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        FileInputStream fis = new FileInputStream(xmlFileName);
        XMLEventReader eventReader = inputFactory.createXMLEventReader(fis);
        while (eventReader.hasNext()) {
            XMLEvent xmlEvent = eventReader.nextEvent();
            handleXMLEvent(xmlEvent, 
                           session);
        }
    }
    
    private void handleXMLEvent(XMLEvent event, 
                                Session session) throws Exception {
        int eventType = event.getEventType();
        switch (eventType) {
            case START_DOCUMENT:
                System.out.println("Starting parsing the document...");
                break;
            case START_ELEMENT:
                StartElement startElement = event.asStartElement();
                handleStartElement(startElement);
                break;               
            case END_ELEMENT :
                handleEndElement(event.asEndElement(),
                                 session);
                break;
            case CHARACTERS :
                handleCharacters(event.asCharacters());
                break;
            case END_DOCUMENT :
                System.out.println("Ending parsing the document.");
                System.out.println("Total gene entries: " + totalGeneEntries);
                break;
        }
    }
    
    private void handleStartElement(StartElement startElement) {
        String name = startElement.getName().getLocalPart();
        currentText = null;
        if (name.equals(CancerIndexConstants.GeneEntry)) {
            currentGeneEntry = new GeneEntry();
        }
        else if (name.equals(CancerIndexConstants.SequenceIdentificationCollection)) {
            SequenceIdentificationCollection sidc = new SequenceIdentificationCollection();
            currentGeneEntry.setSequenceIdentificationCollection(sidc);
        }
        else if (name.equals(CancerIndexConstants.Sentence)) {
            currentSentence = new Sentence();
            currentGeneEntry.addSentence(currentSentence);
        }
        else if (name.equals(CancerIndexConstants.GeneData)) {
            GeneData geneData = new GeneData();
            currentSentence.setGeneData(geneData);
        }
        else if (name.equals(CancerIndexConstants.DiseaseData)) {
            DiseaseData diseaseData = new DiseaseData();
            currentSentence.setDiseaseData(diseaseData);
        }
        else if (name.equals(CancerIndexConstants.Roles)) {
            currentRoles = new Roles();
            currentSentence.addRoles(currentRoles);
        }
    }
    
    private void handleEndElement(EndElement endElement,
                                  Session session) {
        String name = endElement.getName().getLocalPart();
        if (name.equals(CancerIndexConstants.GeneAlias)) 
            currentGeneEntry.addGeneAlias(currentText);
        else if (name.equals(CancerIndexConstants.HUGOGeneSymbol))
            currentGeneEntry.setHugoGeneSymbol(currentText);
        else if (name.equals(CancerIndexConstants.HgncID))
            currentGeneEntry.getSequenceIdentificationCollection().setHgncID(currentText);
        else if (name.equals(CancerIndexConstants.LocusLinkID))
            currentGeneEntry.getSequenceIdentificationCollection().setLocusLinkID(currentText);
        else if (name.equals(CancerIndexConstants.GenbankAccession))
            currentGeneEntry.getSequenceIdentificationCollection().setGenbankAccession(currentText);
        else if (name.equals(CancerIndexConstants.RefSeqID))
            currentGeneEntry.getSequenceIdentificationCollection().setRefSeqID(currentText);
        else if (name.equals(CancerIndexConstants.UniProtID))
            currentGeneEntry.getSequenceIdentificationCollection().setUniProtID(currentText);
        else if (name.equals(CancerIndexConstants.GeneStatusFlag))
            currentGeneEntry.setGeneStatusFlag(currentText);
        else if (name.equals(CancerIndexConstants.MatchedGeneTerm))
            currentSentence.getGeneData().setMatchedGeneTerm(currentText);
        else if (name.equals(CancerIndexConstants.NCIGeneConceptCode))
            currentSentence.getGeneData().setNciGeneConceptCode(currentText);
        else if (name.equals(CancerIndexConstants.MatchedDiseaseTerm))
            currentSentence.getDiseaseData().setMatchedDiseaseTerm(currentText);
        else if (name.equals(CancerIndexConstants.NCIDiseaseConceptCode))
            currentSentence.getDiseaseData().setNciDiseaseConceptCode(currentText);
        else if (name.equals(CancerIndexConstants.Statement))
            currentSentence.setStatement(currentText);
        else if (name.equals(CancerIndexConstants.PubMedID))
            currentSentence.setPubMedID(currentText);
        else if (name.equals(CancerIndexConstants.Organism))
            currentSentence.setOrganism(currentText);
        else if (name.equals(CancerIndexConstants.NegationIndicator))
            currentSentence.setNegationIndicator(currentText);
        else if (name.equals(CancerIndexConstants.CellineIndicator))
            currentSentence.setCellineIndicator(currentText);
        else if (name.equals(CancerIndexConstants.Comments))
            currentSentence.setComments(currentText);
        else if (name.equals(CancerIndexConstants.EvidenceCode)) 
            currentSentence.addEvidenceCode(currentText);
        else if (name.equals(CancerIndexConstants.PrimaryNCIRoleCode))
            currentRoles.addPrimaryNCIRoleCode(currentText);
        else if (name.equals(CancerIndexConstants.OtherRole))
            currentRoles.addOtherRole(currentText);
        else if (name.equals(CancerIndexConstants.SentenceStatusFlag))
            currentSentence.setSentenceStatusFlag(currentText);
        else if (name.equals(CancerIndexConstants.GeneEntry)) {
            session.persist(currentGeneEntry);
            session.flush();
            session.clear();
            if (totalGeneEntries % 100 == 0)
                System.out.println("Finished loading: " + totalGeneEntries);
            totalGeneEntries ++;
        }
    }
    
    private void handleCharacters(Characters characters) {
        currentText = characters.toString();
    }
    
    @Test
    public void runParse() throws Exception {
        String fileName = "datasets/NCI_CancerIndex_allphases_disease/NCI_CancerIndex_allphases_disease.xml";
        SessionFactory sf = new CancerIndexHibernateUtility().initSession();
        Session session = sf.openSession();
        long time1 = System.currentTimeMillis();
        Transaction t = session.beginTransaction();
        try {
            parse(fileName,
                  session);
            t.commit();
        }
        catch(Exception e) {
            t.rollback();
            throw e;
        }
        long time2 = System.currentTimeMillis();
        System.out.println("Time for parsing: " + (time2 - time1));
        session.close();
    }
}
