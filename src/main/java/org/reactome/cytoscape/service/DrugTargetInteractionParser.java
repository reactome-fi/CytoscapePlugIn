/*
 * Created on Dec 16, 2016
 *
 */
package org.reactome.cytoscape.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

import edu.ohsu.bcb.druggability.DatabaseRef;
import edu.ohsu.bcb.druggability.Drug;
import edu.ohsu.bcb.druggability.ExpEvidence;
import edu.ohsu.bcb.druggability.Interaction;
import edu.ohsu.bcb.druggability.LitEvidence;
import edu.ohsu.bcb.druggability.Source;
import edu.ohsu.bcb.druggability.Target;

/**
 * This class is used to parse cancer drug/target interactions in XML received from a RESTful API. Don't try to reuse
 * the object in different threads to avoid values overloading.
 * @author gwu
 */
@SuppressWarnings("unchecked")
public class DrugTargetInteractionParser {
    // The following variables are used for caching parsing results
    private Map<String, Interaction> idToInteraction;
    private Map<String, Drug> idToDrug;
    private Map<String, Target> idToTarger;
    private Map<String, DatabaseRef> idToDatabase;
    private Map<String, ExpEvidence> idToExpEvidence;
    private Map<String, Source> idToSource;
    private Map<String, LitEvidence> idToLitEvidence;
    // Parsing results
    private List<Interaction> interactions;
    private Map<Long, Set<Interaction>> dbIdToInteractions;
    // A helper object for parsing
    private SetHandler sourceSetHandler;
    
    /**
     * Default constructor.
     */
    public DrugTargetInteractionParser() {
    }
    
    public void parse(Element rootElement) {
        init();
        List<Element> elements = rootElement.getChildren();
        for (Element elm : elements) {
            String elmName = elm.getName();
            if (elmName.equals("interaction"))
                parseInteraction(elm);
            else if (elmName.equals("drug"))
                parseDrug(elm);
            else if (elmName.equals("target"))
                parseTarget(elm);
            else if (elmName.equals("expEvidence"))
                parseExpEvidence(elm);
            else if (elmName.equals("literature"))
                parseLiterature(elm);
            else if (elmName.equals("Source"))
                parseSource(elm);
            else if (elmName.equals("database"))
                parseDatabase(elm);
            else if (elmName.equals("dbIdToInteractions"))
                parseDbIdToInteractions(elm);
        }
    }
    
    private void parseDbIdToInteractions(Element elm) {
        if (dbIdToInteractions == null)
            dbIdToInteractions = new HashMap<>();
        String dbId = null;
        Set<Interaction> interactions = new HashSet<>();
        List<Element> childElms = elm.getChildren();
        for (Element childElm : childElms) {
            String elmName = childElm.getName();
            String id = childElm.getTextNormalize();
            if (elmName.equals("dbId")) {
                dbId = id;
            }
            else {
                Interaction interaction = getObject(id, idToInteraction, Interaction.class);
                interactions.add(interaction);
            }
        }
        dbIdToInteractions.put(new Long(dbId),
                               interactions);
    }
    
    /**
     * Get the parsing results.
     * @return
     */
    public List<Interaction> getInteractions() {
        return this.interactions;
    }
    
    public Map<Long, Set<Interaction>> getDbIdToInteractions() {
        return this.dbIdToInteractions;
    }
    
    private void parseInteraction(Element elm) {
        String id = getId(elm);
        Interaction interaction = getObject(id, 
                                            idToInteraction,
                                            Interaction.class);
        interactions.add(interaction);
        List<Element> childElms = elm.getChildren();
        Set<Source> sources = new HashSet<>();
        Set<ExpEvidence> evidences = new HashSet<>();
        for (Element childElm : childElms) {
            String elmName = childElm.getName();
            if (elmName.equals("target")) {
                String targetId = getId(childElm);
                Target target = getObject(targetId, idToTarger, Target.class);
                interaction.setIntTarget(target);
            }
            else if (elmName.equals("drug")) {
                String drugId = getId(childElm);
                Drug drug = getObject(drugId, idToDrug, Drug.class);
                interaction.setIntDrug(drug);
            }
            else if (elmName.equals("interactionID")) {
                String interactionID = childElm.getTextNormalize();
                interaction.setInteractionID(new Integer(interactionID));
            }
            else if (elmName.equals("interactionType")) {
                String type = childElm.getTextNormalize();
                interaction.setInteractionType(type);
            }
            else if (elmName.equals("source")) {
                String sourceId = getId(childElm);
                Source source = getObject(sourceId, idToSource, Source.class);
                sources.add(source);
            }
            else if (elmName.equals("expEvidence")) {
                String evidenceId = getId(childElm);
                ExpEvidence evidence = getObject(evidenceId, idToExpEvidence, ExpEvidence.class);
                evidences.add(evidence);
            }
        }
        if (sources.size() > 0)
            interaction.setInteractionSourceSet(sources);
        if (evidences.size() > 0)
            interaction.setExpEvidenceSet(evidences);
    }
    
    private String getId(Element element) {
        String id = element.getTextNormalize();
        if (id != null && id.length() > 0)
            return id;
        id = element.getChildText("id");
        return id;
    }
    
    private <T> T getObject(String id,
                            Map<String, T> idToObj,
                            Class<T> type) {
        T obj = idToObj.get(id);
        if (obj != null)
            return obj;
        try {
            obj = type.newInstance();
            idToObj.put(id, obj);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return obj;
    }
    
    private void parseDrug(Element elm) {
        String id = getId(elm);
        Drug drug = getObject(id, idToDrug, Drug.class);
        parseSimpleObject(elm, drug);
    }
    
    private <T> void parseSimpleObject(Element elm, 
                                       Object obj) {
        parseSimpleObject(elm, obj, null);
    }

    private <T> void parseSimpleObject(Element elm, 
                                       Object obj,
                                       SetHandler setHandler) {
        List<Element> childElms = elm.getChildren();
        Set<String> synonyms = new HashSet<>();
        for (Element childElm : childElms) {
            String childElmName = childElm.getName();
            String propName = upperFirst(childElmName);
            String text = childElm.getTextNormalize();
            if (text == null || text.length() == 0)
                continue; // Nothing to be done
            
            Method setMethod = getNamedMethod(obj, "set" + propName);
            // We may not be able to get a method (e.g. id)
            if (setMethod == null)
                continue;
            Class<?> argType = setMethod.getParameterTypes()[0]; // Only the first matters
            
            if (argType == Set.class) {
                Method getMethod = getNamedMethod(obj, "get" + propName);
                try {
                    Set<Object> set = (Set<Object>) getMethod.invoke(obj);
                    if (set == null) {
                        set = new HashSet<>();
                        setMethod.invoke(obj, set);
                    }
                    if (setHandler == null)
                        set.add(text);
                    else
                        setHandler.handleSet(set, text);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                // All other types can be converted safely
                try {
                    if (argType == Integer.class)
                        setMethod.invoke(obj, new Object[]{new Integer(text)});
                    else
                        setMethod.invoke(obj, new Object[]{text});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private Method getNamedMethod(Object target, 
                                  String methodName) {
        Method[] methods = target.getClass().getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName))
                return m;
        }
        return null;
    }
    
    private String upperFirst(String propName) {
        return propName.substring(0, 1).toUpperCase() + propName.substring(1);
    }
    
    private void parseTarget(Element elm) {
        String id = getId(elm);
        Target target = getObject(id, idToTarger, Target.class);
        parseSimpleObject(elm, target);
    }
    
    private void parseExpEvidence(Element elm) {
        String id = getId(elm);
        ExpEvidence exp = getObject(id, idToExpEvidence, ExpEvidence.class);
        if (sourceSetHandler == null) {
            sourceSetHandler = new SetHandler() {
                @Override
                public void handleSet(Set<Object> set, String value) {
                    Source source = getObject(value, idToSource, Source.class);
                    set.add(source);
                }
            };
        }
        parseSimpleObject(elm, exp, sourceSetHandler);
    }
    
    private void parseLiterature(Element elm) {
        String id = getId(elm);
        LitEvidence literature = getObject(id, idToLitEvidence, LitEvidence.class);
        parseSimpleObject(elm, literature);
    }
    
    private void parseSource(Element elm) {
        String id = getId(elm);
        Source source = getObject(id, idToSource, Source.class);
        List<Element> childElms = elm.getChildren();
        for (Element childElm : childElms) {
            String name = childElm.getName();
            String text = childElm.getTextNormalize();
            if (name.equals("sourceID"))
                source.setSourceID(new Integer(text));
            else if (name.equals("literature")) {
                String litId = getId(childElm);
                LitEvidence litEvidence = getObject(litId, idToLitEvidence, LitEvidence.class);
                source.setSourceLiterature(litEvidence);
            }
            else if (name.equals("sourceDatabase")) {
                String dbId = getId(childElm);
                DatabaseRef database = getObject(dbId, idToDatabase, DatabaseRef.class);
                source.setSourceDatabase(database);
            }
            else if (name.equals("parentDatabase")) {
                String dbId = getId(childElm);
                DatabaseRef database = getObject(dbId, idToDatabase, DatabaseRef.class);
                source.setParentDatabase(database);
            }
        }
    }
    
    private void parseDatabase(Element elm) {
        String id = getId(elm);
        DatabaseRef database = getObject(id, idToDatabase, DatabaseRef.class);
        parseSimpleObject(elm, database);
    }
    
    private void init() {
        // Initialize maps
        idToInteraction = new HashMap<>();
        idToDrug = new HashMap<>();
        idToTarger = new HashMap<>();
        idToDatabase = new HashMap<>();
        idToExpEvidence = new HashMap<>();
        idToSource = new HashMap<>();
        idToLitEvidence = new HashMap<>();
        // To keep the parsing results
        interactions = new ArrayList<>();
    }
    
    private interface SetHandler {
        
        public void handleSet(Set<Object> set,
                              String value);
        
    }
    
    @Test
    public void testParse() throws Exception {
        String fileName = "/Users/gwu/Documents/temp/Druggability.xml";
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(fileName);
        Element root = document.getRootElement();
        parse(root);
        List<Interaction> interations = getInteractions();
        System.out.println("Total interactions: " + interactions.size());
        for (Interaction interaction : interactions)
            System.out.println(interaction.getIntDrug() + "\t" + interaction.getIntTarget());
        System.out.println("Total dbIdToInteractions: " + dbIdToInteractions.size());
        for (Long dbId : dbIdToInteractions.keySet())
            System.out.println(dbId + "\t" + dbIdToInteractions.get(dbId).size());
    }
    
}
