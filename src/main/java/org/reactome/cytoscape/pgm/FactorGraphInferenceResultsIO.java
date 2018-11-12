/*
 * Created on Dec 2, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.gk.util.StringUtils;
import org.junit.Test;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.VariableAssignment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * A customized IO for reading/writing FactorGraphInferenceResults object.
 * Note: Some of XML write/read code is adapted from http://www.mkyong.com/java/how-to-create-xml-file-in-java-dom/
 * and http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/.
 * @author gwu
 *
 */
public class FactorGraphInferenceResultsIO {
    
    /**
     * Default constructor.
     */
    public FactorGraphInferenceResultsIO() {
    }
    
    @Test
    public void testRead() throws Exception {
        long time1 = System.currentTimeMillis();
        String fileName = "/Users/gwu/Documents/EclipseWorkspace/caBigR3/test_data/tcga_ov/CellCycleCNVmRNA_120215.xml";
        fileName = "/Users/gwu/Documents/EclipseWorkspace/caBigR3/test_data/tcga_hnsc/CellCycle_120315.xml";
        File file = new File(fileName);
        FactorGraphInferenceResults results = read(file);
        long time11 = System.currentTimeMillis();
        FactorGraph fg = results.getFactorGraph();
        if (fg != null) {
            System.out.println("FactorGraph: " + fg.getName());
            System.out.println("Total variables: " + fg.getVariables().size());
            System.out.println("Total factors: " + fg.getFactors().size());
        }
        System.out.println("pathwayDiagramId: " + results.getPathwayDiagramId());
        System.out.println("usedForTwoCaeses: " + results.isUsedForTwoCases());
        List<Observation<Number>> observations = results.getObservations();
        System.out.println("observations: " + observations.size());
        // Check the first
        Observation<Number> obs = observations.get(0);
        List<VariableAssignment<Number>> list = obs.getVariableAssignments();
        System.out.println("First: " + obs.getName() + " with assgns " + list.size());
        for (VariableAssignment<Number> varAssgn : list)
            System.out.println(varAssgn.getVariable().getId() + ": " + varAssgn.getAssignment());
        List<Observation<Number>> randomObs = results.getRandomObservations();
        System.out.println("Random observations: " + randomObs.size());
        // Check the first
        if (randomObs.size() > 0) {
            obs = randomObs.get(0);
            list = obs.getVariableAssignments();
            System.out.println("First: " + obs.getName() + " with assgns " + list.size());
            Map<Variable, VariableInferenceResults> varResults = results.getVarToResults();
            System.out.println("VarToResults: " + varResults.size());
            for (Variable var : varResults.keySet()) {
                VariableInferenceResults varResults1 = varResults.get(var);
                System.out.println("Variable id: " + var.getId());
                System.out.println("Prior: " + varResults1.getPriorValues());
                Map<String, ArrayList<Double>> sampleToValues = varResults1.getSampleToValues();
                for (String sample : sampleToValues.keySet())
                    System.out.println(sample + ": " + sampleToValues.get(sample));
                break;
            }
        }
        // Check Sample
        Map<String, String> sampleToType = results.getSampleToType();
        if (sampleToType != null) {
            System.out.println("SampleToType: " + sampleToType.size());
            for (String sample : sampleToType.keySet())
                System.out.println(sample + ": " + sampleToType.get(sample));
        }
        long time2 = System.currentTimeMillis();
        System.out.println("Time for parsing: " + (time2 - time1));
    }
    
    private Map<String, Variable> getIdToVar(FactorGraph fg) {
        Map<String, Variable> idToVar = new HashMap<>();
        for (Variable var : fg.getVariables()) 
            idToVar.put(var.getId(), var);
        return idToVar;
    }
    
    /**
     * Read a FactorGraphInferenceResults into a file. The implementation of this method
     * is based on Java StAX (iteator API) for quick performance.
     * @param input
     * @return
     */
    public FactorGraphInferenceResults read(File input) throws XMLStreamException, IOException, JAXBException {
        XMLInputFactory factor = XMLInputFactory.newInstance();
        factor.setProperty(XMLInputFactory.IS_COALESCING, true);
        XMLEventReader reader = factor.createXMLEventReader(input.getAbsolutePath(),
                                                            new FileInputStream(input));
        FactorGraphInferenceResults results = new FactorGraphInferenceResults();
        Map<String, Variable> idToVar = null;
        while (reader.hasNext()) {
            XMLEvent event = null;
            // Since we want to use JAXB, so we have to use peek first
            event = reader.peek();
            int eventType = event.getEventType();
            if (eventType == XMLEvent.START_ELEMENT) {
                StartElement element = event.asStartElement();
                String name = element.getName().getLocalPart();
                // Want to get the name
                if (name.equals("factorGraph")) {
                    FactorGraph fg = readFactorGraph(reader);
                    results.setFactorGraph(fg);
                    idToVar = getIdToVar(fg);
                }
                else if (name.equals("FactorGraphInferenceResults")) {
                    readResultsAttributes(element, results);
                }
                else if (name.equals("Observations")) {
                    List<Observation<Number>> observations = readObservations(reader,
                                                                              idToVar,
                                                                              "Observations");
                    results.setObservations(observations);
                }
                else if (name.equals("RandomObservations")) {
                    List<Observation<Number>> observations = readObservations(reader,
                                                                              idToVar,
                                                                              "RandomObservations");
                    results.setRandomObservations(observations);
                }
                else if (name.equals("VariableToResults")) {
                    Map<Variable, VariableInferenceResults> varToResults = readVarToResults(reader,
                                                                                            idToVar);
                    results.setVarToResults(varToResults);
                }
                else if (name.equals("SampleToType")) {
                    Map<String, String> sampleToType = readSampleToType(reader);
                    results.setSampleToType(sampleToType);
                }
            }
            if (reader.hasNext()) // In case it has been processed in the above statements
                reader.nextEvent(); // Need to go to next event
        }
        return results;
    }
    
    private Map<String, String> readSampleToType(XMLEventReader reader) throws XMLStreamException {
        Map<String, String> sampleToType = new HashMap<>();
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.getEventType() == XMLEvent.END_ELEMENT)
                break; // There should be only one END_ELEMENT
            else if (event.getEventType() == XMLEvent.CHARACTERS) {
                String text = event.asCharacters().getData();
                String[] lines = text.split("\n");
                for (String line : lines) {
                    if (line.trim().length() == 0)
                        continue;
                    String[] tokens = line.split(":");
                    sampleToType.put(tokens[0], tokens[1]);
                }
            }
        }
        return sampleToType;
    }
    
    private Map<Variable, VariableInferenceResults> readVarToResults(XMLEventReader reader,
                                                                     Map<String, Variable> idToVar) throws XMLStreamException {
        Map<Variable, VariableInferenceResults> varToResults = new HashMap<>();
        VariableInferenceResults results = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.getEventType() == XMLEvent.START_ELEMENT) {
                StartElement element = event.asStartElement();
                String name = element.getName().getLocalPart();
                if (name.equals("varResults")) {
                    String varId = getAttributeValue(element, "var");
                    Variable var = idToVar.get(varId);
                    results = new VariableInferenceResults();
                    results.setVariable(var);
                    String prior = getAttributeValue(element, "prior");
                    results.setPriorValues(parseDoubleText(prior));
                    varToResults.put(var, results);
                }
            }
            else if (event.getEventType() == XMLEvent.CHARACTERS && results != null) {
                Characters text = event.asCharacters();
                parseSampleToValues(text.getData(), results);
            }
            else if (event.getEventType() == XMLEvent.END_ELEMENT) {
                EndElement element = event.asEndElement();
                String name = element.getName().getLocalPart();
                if (name.equals("varResults"))
                    results = null;
                else if (name.equals("VariableToResults"))
                    break;
            }
        }
        return varToResults;
    }
    
    private void parseSampleToValues(String text, VariableInferenceResults results) {
        String[] lines = text.split("\n");
        Map<String, ArrayList<Double>> sampleToValues = new HashMap<>();
        for (String line : lines) {
            if (line.trim().length() == 0)
                continue;
            String[] tokens = line.split(":|,");
            ArrayList<Double> values = new ArrayList<>();
            for (int i = 1; i < tokens.length; i++) {
                values.add(new Double(tokens[i]));
            }
            sampleToValues.put(tokens[0], values);
        }
        results.setSampleToValues(sampleToValues);
    }
    
    private double[] parseDoubleText(String text) {
        String[] tokens = text.split(",");
        double[] rtn = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++)
            rtn[i] = new Double(tokens[i]);
        return rtn;
    }
    
    private List<Observation<Number>> readObservations(XMLEventReader reader, 
                                                       Map<String, Variable> idToVar,
                                                       String type) throws XMLStreamException {
        List<Observation<Number>> observations = new ArrayList<>();
        Observation<Number> observation = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.getEventType() == XMLEvent.START_ELEMENT) {
                StartElement element = event.asStartElement();
                String name = element.getName().getLocalPart();
                if (name.equals("Observation")) {
                    observation = new Observation<>();
                    observations.add(observation);
                    String sampleName = getAttributeValue(element, "name");
                    observation.setName(sampleName);
                }
            }
            else if (event.getEventType() == XMLEvent.CHARACTERS && observation != null) {
                Characters text = event.asCharacters();
                parseVarAssgns(text.getData(), idToVar, observation);
            }
            // Need to stop in this method
            else if (event.getEventType() == XMLEvent.END_ELEMENT) {
                EndElement element = event.asEndElement();
                String name = element.getName().getLocalPart();
                if (name.equals("Observation"))
                    observation = null; // This observation has been read
                else if (name.equals(type))
                    break;
            }
        }
        return observations;
    }
    
    private void parseVarAssgns(String text,
                                Map<String, Variable> idToVar,
                                Observation<Number> observation) {
        String[] lines = text.split("\n");
        List<VariableAssignment<Number>> list = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().length() == 0)
                continue;
            String[] tokens = line.split(",");
            Variable var = idToVar.get(tokens[0]);
            Number value = null;
            if (tokens[1].contains("."))
                value = new Double(tokens[1]);
            else
                value = new Integer(tokens[1]);
            VariableAssignment<Number> varAssgn = new VariableAssignment<>();
            varAssgn.setVariable(var);
            varAssgn.setAssignment(value);
            list.add(varAssgn);
        }
        observation.setVariableAssignments(list);
    }
                                
    
    private String getAttributeValue(StartElement element,
                                     String attName) {
        QName name = new QName(attName);
        Attribute att = element.getAttributeByName(name);
        if (att == null)
            return null;
        return att.getValue();
    }
    
    private void readResultsAttributes(StartElement element,
                                      FactorGraphInferenceResults results) {
        Iterator it = element.getAttributes();
        while (it.hasNext()) {
            Attribute att = (Attribute) it.next();
            String attName = att.getName().getLocalPart();
            String value = att.getValue();
            if (attName.equals("pathwayDiagramId"))
                results.setPathwayDiagramId(new Long(value));
            else if (attName.equals("usedForTwoCases"))
                results.setUsedForTwoCases(new Boolean(value));
        }
    }
    
    /**
     * Write a FactorGraphInferenceResults into a file.
     * @param results
     * @param output
     * @throws ParserConfigurationException
     * @throws JAXBException
     * @throws TransformerException
     */
    public void write(FactorGraphInferenceResults results,
                      File output) throws ParserConfigurationException, JAXBException, TransformerException {
        // Use build-in xml support in java
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuidler = docFactory.newDocumentBuilder();
        Document doc = docBuidler.newDocument();
        Element rootElement = doc.createElement("FactorGraphInferenceResults");
        doc.appendChild(rootElement);
        
        writeFactorGraph(results, rootElement);
        writeObservations(results, rootElement, doc);
        writeVarResults(results, rootElement, doc);
        // Other properties
        rootElement.setAttribute("usedForTwoCases", results.isUsedForTwoCases() + "");
        rootElement.setAttribute("pathwayDiagramId", results.getPathwayDiagramId() + "");
        writeSampleToType(results, rootElement, doc);
        
        TransformerFactory transformerFactor = TransformerFactory.newInstance();
        Transformer transformer = transformerFactor.newTransformer();
        // Want to make output nicer for human viewing
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);
        transformer.transform(source, result);
    }
    
    private void writeSampleToType(FactorGraphInferenceResults results,
                                   Element rootElm,
                                   Document doc) {
        if (!results.isUsedForTwoCases())
            return;
        Map<String, String> sampleToType = results.getSampleToType();
        if (sampleToType == null || sampleToType.size() == 0)
            return;
        Element sampleToTypeElm = doc.createElement("SampleToType");
        rootElm.appendChild(sampleToTypeElm);
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        for (String sample : sampleToType.keySet()) {
            String type = sampleToType.get(sample);
            builder.append(sample).append(":").append(type);
            builder.append("\n");
        }
        Text text = doc.createTextNode(builder.toString());
        sampleToTypeElm.appendChild(text);
    }
    
    private void writeVarResults(FactorGraphInferenceResults results,
                                 Element rootElement,
                                 Document doc) {
        Map<Variable, VariableInferenceResults> varToResults = results.getVarToResults();
        if (varToResults == null || varToResults.size() == 0)
            return;
        Element varResultsElm = doc.createElement("VariableToResults");
        rootElement.appendChild(varResultsElm);
        for (Variable var : varToResults.keySet()) {
            VariableInferenceResults varResults = varToResults.get(var);
            Element varResultsElm1 = doc.createElement("varResults");
            varResultsElm.appendChild(varResultsElm1);
            writeVarResults(var, 
                            varResults,
                            varResultsElm1,
                            doc);
        }
    }

    private void writeVarResults(Variable var, 
                                 VariableInferenceResults varResults,
                                 Element varResultsElm1,
                                 Document doc) {
        varResultsElm1.setAttribute("var", var.getId());
        List<Double> priorValues = varResults.getPriorValues();
        varResultsElm1.setAttribute("prior", StringUtils.join(",", priorValues));
        Map<String, ArrayList<Double>> sampleToValues = varResults.getSampleToValues();
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        for (String sample : sampleToValues.keySet()) {
            List<Double> values = sampleToValues.get(sample);
            builder.append(sample).append(":");
            builder.append(StringUtils.join(",", values));
            builder.append("\n");
        }
        Text text = doc.createTextNode(builder.toString());
        varResultsElm1.appendChild(text);
    }
    
    private void writeObservations(FactorGraphInferenceResults results,
                                   Element rootElement,
                                   Document doc) {
        List<Observation<Number>> observations = results.getObservations();
        writeObservations(observations, "Observations", rootElement, doc);
        List<Observation<Number>> randomObs = results.getRandomObservations();
        writeObservations(randomObs, "RandomObservations", rootElement, doc);
    }
    
    private void writeObservations(List<Observation<Number>> observations,
                                   String type,
                                   Element parentElm,
                                   Document doc) {
        Element observationsElm = doc.createElement(type);
        parentElm.appendChild(observationsElm);
        StringBuilder builder = new StringBuilder();
        // Add a new line so that text can start in a new line
        builder.append("\n");
        for (Observation<Number> obs : observations) {
            Element obsElm = doc.createElement("Observation");
            obsElm.setAttribute("name", obs.getName());
            observationsElm.appendChild(obsElm);
            builder.setLength(0);
            List<VariableAssignment<Number>> varAssgns = obs.getVariableAssignments();
            for (VariableAssignment<Number> varAssgn : varAssgns) {
                builder.append(varAssgn.getVariable().getId()).append(",");
                builder.append(varAssgn.getAssignment());
                builder.append("\n");
            }
            Text text = doc.createTextNode(builder.toString());
            obsElm.appendChild(text);
        }
    }

    private void writeFactorGraph(FactorGraphInferenceResults results, Element rootElement) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FactorGraph.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        
        FactorGraph fg = results.getFactorGraph();
        jaxbMarshaller.marshal(fg, rootElement);
    }
    
    private FactorGraph readFactorGraph(XMLEventReader reader) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FactorGraph.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        FactorGraph fg = (FactorGraph) unmarshaller.unmarshal(reader);
        return fg;
    }
}
