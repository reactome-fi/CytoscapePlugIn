/*
 * Created on Oct 22, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.VariableAssignment;

/**
 * This class is used to read/write FIPGMResults object. The build-in JAXB is not used because
 * the generated file is too large if Observations are included.
 * @author gwu
 *
 */
@SuppressWarnings("unchecked")
public class FIPGMResultsIO {
    
    /**
     * Default constructor.
     */
    public FIPGMResultsIO() {
    }
    
    /**
     * TEST_CODE: DON'T USE IT!
     * The following implement used StAX is not faster than JDOM based method, read().
     * There is a bug in this method still: Random samples have been merged into real samples.
     * @param results
     * @param file
     * @throws XMLStreamException
     * @throws IOException
     */
    public void read2(FIPGMResults results,
                      File file) throws XMLStreamException, IOException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
        inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        FileInputStream fis = new FileInputStream(file);
        XMLEventReader eventReader = inputFactory.createXMLEventReader(fis);
        String name = null;
        
        Map<String, Variable> idToVar = null;
        Map<String, Map<Variable, Double>> sampleToVarToScore = new HashMap<>();
        Map<String, Map<Variable, Double>> randomSampleToVarToScore = new HashMap<>();
        results.setSampleToVarToScore(sampleToVarToScore);
        results.setRandomSampleToVarToScore(randomSampleToVarToScore);
        List<Observation<Number>> observations = new ArrayList<>();
        List<Observation<Number>> randomObservations = new ArrayList<>();
        results.setObservations(observations);
        results.setRandomObservations(randomObservations);
        
        boolean isInResults = false;
        boolean isInRandomResults = false;
        boolean isInObservations = false;
        boolean isInRandomObservations = false;
        String nameAtt = null;
        
        while (eventReader.hasNext()) {
            XMLEvent xmlEvent = eventReader.nextEvent();
            int eventType = xmlEvent.getEventType();
            if (eventType == XMLEvent.START_ELEMENT) {
                StartElement elm = xmlEvent.asStartElement();
                Attribute attribute = elm.getAttributeByName(new QName("name"));
                if (attribute != null)
                    nameAtt = attribute.getValue();
                name = elm.getName().getLocalPart();
                if (name.equals("Results"))
                    isInResults = true;
                else if (name.equals("RandomResults"))
                    isInRandomResults = true;
                else if (name.equals("Observations"))
                    isInObservations = true;
                else if (name.equals("RandomObservations")) 
                    isInRandomObservations = true;
            }
            else if (eventType == XMLEvent.END_ELEMENT) {
                name = null;
            }
            else if (eventType == XMLEvent.CHARACTERS) {
                if (name == null)
                    continue;
                String text = xmlEvent.asCharacters().getData();
                if (name.equals("Variables")) {
                    idToVar = readVariables(text);
                }
                else if (name.equals("Sample")) {
                    Map<Variable, Double> varToScore = readResults(idToVar, text);
                    if (isInResults)
                        sampleToVarToScore.put(nameAtt, varToScore);
                    else if (isInRandomResults)
                        randomSampleToVarToScore.put(nameAtt, varToScore);
                }
                else if (name.equals("Observation")) {
                    Observation<Number> observation = readObservation(idToVar, nameAtt, text);
                    if (isInObservations)
                        observations.add(observation);
                    else if (isInRandomObservations)
                        randomObservations.add(observation);
                }
            }
        }
    }
    
    /**
     * TODO: This method needs to be re-implemented in order to improve the speed and memory
     * usage. The current way based on JDOM API is too slow. Need to consider to use StAX API.
     * @param results
     * @param file
     * @throws IOException
     * @throws JDOMException
     */
    public void read(FIPGMResults results,
                     File file) throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(file);
        Element root = document.getRootElement();
        List<Element> elms = root.getChildren();
        Map<String, Variable> idToVar = null;
        for (Element elm : elms) {
            String name = elm.getName();
            if (name.equals("Variables")) // This should be the first element.
                idToVar = readVariables(elm);
            else if (name.equals("Results")) {
                Map<String, Map<Variable, Double>> sampleToVarToScore = readResults(elm, idToVar);
                results.setSampleToVarToScore(sampleToVarToScore);
            }
            else if (name.equals("RandomResults")) {
                Map<String, Map<Variable, Double>> sampleToVarToScore = readResults(elm, idToVar);
                results.setRandomSampleToVarToScore(sampleToVarToScore);
            }
            else if (name.equals("Observations")) {
                List<Observation<Number>> observations = readObservations(elm, idToVar);
                results.setObservations(observations);
            }
            else if (name.equals("RandomObservations")) {
                List<Observation<Number>> observations = readObservations(elm, idToVar);
                results.setRandomObservations(observations);
            }
        }
    }
    
    public void write(FIPGMResults results,
                      File file) throws IOException {
        Element root = new Element("FIResults");
        Map<String, Map<Variable, Double>> sampleToVarToScore = results.getSampleToVarToScore();
        Set<Variable> variables = getAllVariables(results);
        writeVariables(variables,
                       root);
        writeSampleToVarToScore(sampleToVarToScore, "Results", root);
        Map<String, Map<Variable, Double>> randomSampleToVarToScore = results.getRandomSampleToVarToScore();
        writeSampleToVarToScore(randomSampleToVarToScore, "RandomResults", root);
        // Observations
        writeObservations(results.getObservations(),
                          "Observations",
                          root);
        writeObservations(results.getRandomObservations(),
                          "RandomObservations",
                          root);
        
        // Output root
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream fos = new FileOutputStream(file);
        outputter.output(root, fos);
        fos.close();
    }
    
    private void writeObservations(List<Observation<Number>> observations,
                                   String type,
                                   Element parentElm) {
        Element observationsElm = new Element(type);
        parentElm.addContent(observationsElm);
        StringBuilder builder = new StringBuilder();
        for (Observation<Number> obs : observations) {
            Element obsElm = new Element("Observation");
            obsElm.setAttribute("name", obs.getName());
            observationsElm.addContent(obsElm);
            builder.setLength(0);
            List<VariableAssignment<Number>> varAssgns = obs.getVariableAssignments();
            for (VariableAssignment<Number> varAssgn : varAssgns) {
                builder.append(varAssgn.getVariable().getId()).append(",");
                builder.append(varAssgn.getAssignment());
                builder.append("\n");
            }
            obsElm.setText(builder.toString());
        }
    }
    
    private List<Observation<Number>> readObservations(Element obsElm,
                                                       Map<String, Variable> idToVar) throws IOException {
        List<Observation<Number>> observations = new ArrayList<>();
        List<Element> elms = obsElm.getChildren();
        for (Element elm : elms) {
            String name = elm.getAttributeValue("name");
            String text = elm.getTextTrim();
            
            Observation<Number> obs = readObservation(idToVar, name, text);
            
            observations.add(obs);
        }
        return observations;
    }

    private Observation<Number> readObservation(Map<String, Variable> idToVar, 
                                                String name, 
                                                String text) throws IOException {
        Observation<Number> obs = new Observation<>();
        obs.setName(name);
        Map<Variable, Number> varToAssgn = new HashMap<>();
        
        StringReader reader = new StringReader(text);
        BufferedReader bReader = new BufferedReader(reader);
        String line = null;
        while ((line = bReader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0)
                continue;
            int index = line.indexOf(",");
//            String[] tokens = line.split(",");
            Variable var = idToVar.get(line.substring(0, index));
            Double assgn = new Double(line.substring(index + 1));
            varToAssgn.put(var, assgn);
        }
        obs.setVariableToAssignment(varToAssgn);
        bReader.close();
        reader.close();
        
        return obs;
    }
    
    private void writeVariables(Set<Variable> variables,
                                Element parentElm) {
        Element variablesElm = new Element("Variables");
        parentElm.addContent(variablesElm);
        StringBuilder builder = new StringBuilder();
        for (Variable variable : variables) {
            builder.append(variable.getId()).append(",");
            builder.append(variable.getName());
            builder.append("\n");
        }
        variablesElm.setText(builder.toString());
    }
    
    private Map<String, Variable> readVariables(Element varElm) throws IOException {
        String text = varElm.getTextTrim();
        return readVariables(text);
    }

    private Map<String, Variable> readVariables(String text) throws IOException {
//        System.out.println(text);
        Map<String, Variable> idToVar = new HashMap<>();
        // Use this stream based parsing to increase the performance.
        // This approach is faster than String.split("\n")
        StringReader reader = new StringReader(text);
        BufferedReader bReader = new BufferedReader(reader);
        String line = null;
        while ((line = bReader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0)
                continue;
            int index = line.indexOf(",");
            Variable var = new Variable();
            var.setId(line.substring(0, index));
            var.setName(line.substring(index + 1));
            var.setStates(2); // Two states always
            idToVar.put(var.getId(), var);
        }
        bReader.close();
        reader.close();
        return idToVar;
    }
    
    /**
     * Write a map from sample to var to score.
     * @param sampleToVarToScore
     * @param type real results or random results.
     */
    private void writeSampleToVarToScore(Map<String, Map<Variable, Double>> sampleToVarToScore,
                                         String type,
                                         Element parentElm) {
        Element element = new Element(type);
        parentElm.addContent(element);
        StringBuilder builder = new StringBuilder();
        for (String sample : sampleToVarToScore.keySet()) {
            Map<Variable, Double> varToScore = sampleToVarToScore.get(sample);
            Element sampleElm = new Element("Sample");
            sampleElm.setAttribute("name", sample);
            element.addContent(sampleElm);
            builder.setLength(0);
            for (Variable var : varToScore.keySet()) {
                builder.append(var.getId()).append(",");
                builder.append(varToScore.get(var));
                builder.append("\n");
            }
            sampleElm.setText(builder.toString());
        }
    }
    
    private Map<String, Map<Variable, Double>> readResults(Element resultElm,
                                                           Map<String, Variable> idToVar) throws IOException {
        Map<String, Map<Variable, Double>> sampleToVarToScore = new HashMap<>();
        List<Element> elms = resultElm.getChildren();
        for (Element elm : elms) {
            String name = elm.getAttributeValue("name");
            String text = elm.getTextTrim();
            Map<Variable, Double> varToScore = readResults(idToVar, text);
            sampleToVarToScore.put(name, varToScore);
        }
        return sampleToVarToScore;
    }

    private Map<Variable, Double> readResults(Map<String, Variable> idToVar,
                                              String text) throws IOException {
        Map<Variable, Double> varToScore = new HashMap<>();
        
        StringReader reader = new StringReader(text);
        BufferedReader bReader = new BufferedReader(reader);
        String line = null;
        while ((line = bReader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0)
                continue;
            int index = line.indexOf(",");
            Variable var = idToVar.get(line.substring(0, index));
            Double score = new Double(line.substring(index + 1));
            varToScore.put(var, score);
        }
        bReader.close();
        reader.close();
        return varToScore;
    }
    
    private Set<Variable> getAllVariables(FIPGMResults results) {
        Set<Variable> variables = new HashSet<>();
        Map<String, Map<Variable, Double>> sampleToVarToScore = results.getSampleToVarToScore();
        Map<Variable, Double> varToScore = sampleToVarToScore.values().iterator().next();
        variables.addAll(varToScore.keySet());
        // Need to go over all observations
        List<Observation<Number>> observations = results.getObservations();
        for (Observation<Number> obs : observations) {
            List<VariableAssignment<Number>> varAssgns = obs.getVariableAssignments();
            for (VariableAssignment<Number> varAssgn : varAssgns)
                variables.add(varAssgn.getVariable());
            
        }
        return variables;
    }
    
}
