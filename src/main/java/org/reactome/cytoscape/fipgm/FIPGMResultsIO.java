/*
 * Created on Oct 22, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                                                       Map<String, Variable> idToVar) {
        List<Observation<Number>> observations = new ArrayList<>();
        List<Element> elms = obsElm.getChildren();
        for (Element elm : elms) {
            String name = elm.getAttributeValue("name");
            Observation<Number> obs = new Observation<>();
            obs.setName(name);
            Map<Variable, Number> varToAssgn = new HashMap<>();
            String text = elm.getTextTrim();
            String[] lines = text.split("\n");
            for (String line : lines) {
                String[] tokens = line.split(",");
                Variable var = idToVar.get(tokens[0]);
                Double assgn = new Double(tokens[1]);
                varToAssgn.put(var, assgn);
            }
            obs.setVariableToAssignment(varToAssgn);
            observations.add(obs);
        }
        return observations;
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
    
    private Map<String, Variable> readVariables(Element varElm) {
        Map<String, Variable> idToVar = new HashMap<>();
        String text = varElm.getTextTrim();
        String[] lines = text.split("\n");
        for (String line : lines) {
            String[] tokens = line.split(",");
            Variable var = new Variable();
            var.setId(tokens[0]);
            var.setName(tokens[1]);
            var.setStates(2); // Two states always
            idToVar.put(var.getId(), var);
        }
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
                                                           Map<String, Variable> idToVar) {
        Map<String, Map<Variable, Double>> sampleToVarToScore = new HashMap<>();
        List<Element> elms = resultElm.getChildren();
        for (Element elm : elms) {
            String name = elm.getAttributeValue("name");
            String text = elm.getTextTrim();
            String[] lines = text.split("\n");
            Map<Variable, Double> varToScore = new HashMap<>();
            for (String line : lines) {
                String[] tokens = line.split(",");
                Variable var = idToVar.get(tokens[0]);
                Double score = new Double(tokens[1]);
                varToScore.put(var, score);
            }
            sampleToVarToScore.put(name, varToScore);
        }
        return sampleToVarToScore;
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
