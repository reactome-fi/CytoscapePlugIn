package org.reactome.cytoscape3;

/**
 * This class provides most of the functionality for
 * interacting with the Reactome FI server and performing
 * various FI network related functions (such as annotation).
 * 
 * @author Eric T Dawson
 * @date July 2013
 */

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.RenderablePathway;
import org.jdom.Element;
import org.jdom.output.DOMOutputter;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cancerindex.model.Sentence;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.funcInt.FIAnnotation;
import org.reactome.funcInt.Interaction;
import org.reactome.r3.graph.NetworkClusterResult;
import org.reactome.r3.util.InteractionUtilities;
import org.w3c.dom.NodeList;

public class RESTFulFIService implements FINetworkService
{
    private final static String HTTP_GET = PlugInUtilities.HTTP_GET;
    private final static String HTTP_POST = PlugInUtilities.HTTP_POST;
    private String restfulURL;

    public RESTFulFIService()
    {
        init();
    }

    public RESTFulFIService(CyNetworkView view)
    {
        init(view);
    }

    private void init(CyNetworkView view)
    {
        String fiVersion = PlugInScopeObjectManager.getManager().getFiNetworkVersion();
        TableHelper tableManager = new TableHelper();
        
        if (fiVersion == null || fiVersion.length() <= 0)
        {
          fiVersion = tableManager.getStoredFINetworkVersion(view);
          if (fiVersion == null || fiVersion.length() <= 0)
              restfulURL = PlugInScopeObjectManager.getManager().getRestfulURL(PlugInScopeObjectManager.getManager().getDefaultFINeworkVersion());
          restfulURL = PlugInScopeObjectManager.getManager().getRestfulURL(fiVersion);
        }
        else
            restfulURL = PlugInScopeObjectManager.getManager().getRestfulURL();
    }

    private void init()
    {
        // Properties prop =
        // PlugInScopeObjectManager.getManager().getProperties();
        // restfulURL = prop.getProperty("restfulURL");
        restfulURL = PlugInScopeObjectManager.getManager().getRestfulURL();
    }

    @Override
    public Integer getNetworkBuildSizeCutoff() throws Exception
    {
        String url = restfulURL + "network/networkBuildSizeCutoff";
        String text = callHttp(url, HTTP_GET, null);
        return new Integer(text);
    }

    //
    @Override
    public Set<String> buildFINetwork(Set<String> genes, boolean useLinkers)
            throws Exception
    {
        // Query URL: A restful service
        String url = null;
        if (useLinkers)
        {
            url = restfulURL + "network/buildNetwork";
        }
        else
        {
            url = restfulURL + "network/queryFIs";
        }
        String query = InteractionUtilities.joinStringElements("\t", genes);
        Element root = callInXML(url, query);
        List<?> interactions = root.getChildren();
        Set<String> fis = new HashSet<String>(); // To be returned
        // Get the interactions
        for (Object name : interactions)
        {
            Element elm = (Element) name;
            String firstProtein = elm.getChild("firstProtein").getChildText(
                    "name");
            String secondProtein = elm.getChild("secondProtein").getChildText(
                    "name");
            // FIs saved in tab delimited String
            fis.add(firstProtein + "\t" + secondProtein);
        }
        return fis;
    }

    public List<Long> highlight(List<Long> dbIds, String nodes)
            throws IOException
    {
        String url = restfulURL + "network/pathwayDiagram/highlight";
        // Generate a query
        String idText = InteractionUtilities.joinStringElements(",", dbIds);
        String query = idText + "\n" + nodes;
        String rtn = callHttp(url, HTTP_POST, query);
        if (rtn.length() == 0) return new ArrayList<Long>();
        String[] tokens = rtn.split(",");
        List<Long> hitIds = new ArrayList<Long>(tokens.length);
        for (String token : tokens)
        {
            hitIds.add(new Long(token));
        }
        return hitIds;
    }

    @Override
    public Set<String> queryAllFIs() throws IOException
    {
        String url = restfulURL + "network/queryAllFIs";
        String text = callHttp(url, HTTP_GET, null);
        // Do some parse
        String[] tokens = text.split("\n");
        Set<String> fis = new HashSet<String>();
        for (String token : tokens)
        {
            fis.add(token);
        }
        return fis;
    }

    public Set<String> queryFIsBetween(Set<String> srcSet, Set<String> targetSet)
            throws Exception
    {
        String srcQuery = InteractionUtilities.joinStringElements(",", srcSet);
        String targetQuery = InteractionUtilities.joinStringElements(",",
                targetSet);
        String query = srcQuery + "\n" + targetQuery;
        String url = restfulURL + "network/queryFIsBetween";
        Element root = callInXML(url, query);
        List<?> interactions = root.getChildren();
        Set<String> rtn = new HashSet<String>();
        // Get the interactions
        for (Object name : interactions)
        {
            Element elm = (Element) name;
            String firstProtein = elm.getChild("firstProtein").getChildText(
                    "name");
            String secondProtein = elm.getChild("secondProtein").getChildText(
                    "name");
            rtn.add(firstProtein + "\t" + secondProtein);
        }
        return rtn;
    }

    public Set<String> queryFIsForNode(String nodeName) throws Exception
    {
        String url = restfulURL + "network/queryFIs";
        Element root = callInXML(url, nodeName);
        List<?> interactions = root.getChildren();
        Set<String> rtn = new HashSet<String>();
        // Get the interactions
        for (Object name : interactions)
        {
            Element elm = (Element) name;
            String firstProtein = elm.getChild("firstProtein").getChildText(
                    "name");
            String secondProtein = elm.getChild("secondProtein").getChildText(
                    "name");
            if (firstProtein.equals(nodeName))
            {
                rtn.add(secondProtein);
            }
            else
            {
                rtn.add(firstProtein);
            }
        }
        return rtn;
    }

    public Long queryPathwayId(String pathwayName) throws IOException
    {
        String url = restfulURL + "network/queryPathwayId";
        String text = callHttp(url, HTTP_POST, pathwayName);
        return new Long(text);
    }

    public String queryKEGGPathwayId(String pathwayName) throws IOException
    {
        String url = restfulURL + "network/queryKEGGPathwayId";
        return callHttp(url, HTTP_POST, pathwayName);
    }

    public String queryKEGGGeneIds(String geneNames) throws IOException
    {
        String url = restfulURL + "network/queryKEGGGeneIds";
        return callHttp(url, HTTP_POST, geneNames);
    }

    public RenderablePathway queryPathwayDiagram(String pathwayDiagram)
            throws Exception
    {
        String url = restfulURL + "network/queryPathwayDiagram";
        Element root = callInXML(url, pathwayDiagram);
        DiagramGKBReader reader = new DiagramGKBReader();
        return reader.openProcess(root);
    }

    public List<Interaction> queryEdge(String name1, String name2)
            throws Exception
    {
        String url = restfulURL + "network/queryEdge";
        String query = name1 + "\t" + name2;
        Element root = callInXML(url, query);
        // Convert it into org.w3.dom.Document to be used in JAXB
        org.w3c.dom.Document document = new DOMOutputter().output(root
                .getDocument());
        org.w3c.dom.Node docRoot = document.getDocumentElement();
        JAXBContext jc = JAXBContext.newInstance(Interaction.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        NodeList nodeList = docRoot.getChildNodes();
        List<Interaction> interactions = new ArrayList<Interaction>();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            org.w3c.dom.Node interactionNode = nodeList.item(i);
            Interaction interaction = unmarshaller.unmarshal(interactionNode,
                    Interaction.class).getValue();
            interactions.add(interaction);
        }
        return interactions;
    }

    public NetworkClusterResult cluster(List<CyEdge> edges, CyNetworkView view)
            throws Exception
    {
        String url = restfulURL + "network/cluster";
        String query = convertEdgesToString(edges, view);
        Element root = callInXML(url, query);
        org.w3c.dom.Document document = new DOMOutputter().output(root
                .getDocument());
        org.w3c.dom.Node docRoot = document.getDocumentElement();
        JAXBContext jc = JAXBContext.newInstance(NetworkClusterResult.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        NetworkClusterResult result = unmarshaller.unmarshal(docRoot,
                NetworkClusterResult.class).getValue();
        return result;
    }

    private Element callInXML(String url, String query) throws Exception
    {
        return PlugInUtilities.callHttpInXML(url, query);
    }

    private String callHttp(String url, String type, String query)
            throws IOException {
        return PlugInUtilities.callHttpInText(url, type, query);
    }

    public List<ModuleGeneSetAnnotation> annotateGeneSet(Set<String> genes,
            String type) throws Exception
    {
        // Recover network modules information
        String url = restfulURL + "network/annotateGeneSet/" + type;
        // Create a query
        StringBuilder builder = new StringBuilder();
        for (String node : genes)
        {
            builder.append(node).append("\n");
        }
        return annotateGeneSets(url, builder.toString());
    }

    public Map<String, String> queryGeneToDisease(Set<String> genes)
            throws Exception
    {
        String url = restfulURL + "cancerGeneIndex/queryGeneToDiseases";
        // Create query
        StringBuilder builder = new StringBuilder();
        for (String gene : genes)
        {
            builder.append(gene).append("\n");
        }
        String result = callHttp(url, HTTP_POST, builder.toString());
        Map<String, String> geneToDiseases = new HashMap<String, String>();
        String[] lines = result.split("\n");
        for (String line : lines)
        {
            String[] tokens = line.split("\t");
            geneToDiseases.put(tokens[0], tokens[1]);
        }
        return geneToDiseases;
    }

    public List<Sentence> queryCGIAnnotations(String gene) throws Exception
    {
        String url = restfulURL + "cancerGeneIndex/queryAnnotations";
        Element element = callInXML(url, gene);
        org.w3c.dom.Document document = new DOMOutputter().output(element
                .getDocument());
        org.w3c.dom.Node docRoot = document.getDocumentElement();
        JAXBContext jc = JAXBContext.newInstance(Sentence.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        NodeList nodeList = docRoot.getChildNodes();
        List<Sentence> annotations = new ArrayList<Sentence>();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            org.w3c.dom.Node sentenceNode = nodeList.item(i);
            Sentence sentence = unmarshaller.unmarshal(sentenceNode,
                    Sentence.class).getValue();
            annotations.add(sentence);
        }
        return annotations;
    }

    private List<ModuleGeneSetAnnotation> annotateGeneSets(String url,
            String query) throws Exception
    {
        Element root = callInXML(url, query);
        List<?> children = root.getChildren();
        // Need to create ModuleGeneSetAnnotation from XML
        List<ModuleGeneSetAnnotation> rtn = new ArrayList<ModuleGeneSetAnnotation>();
        for (Object name2 : children)
        {
            Element elm = (Element) name2;
            ModuleGeneSetAnnotation moduleAnnotation = new ModuleGeneSetAnnotation();
            List<GeneSetAnnotation> annotations = new ArrayList<GeneSetAnnotation>();
            moduleAnnotation.setAnnotations(annotations);
            Set<String> ids = new HashSet<String>();
            moduleAnnotation.setIds(ids);
            for (Iterator<?> it1 = elm.getChildren().iterator(); it1.hasNext();)
            {
                Element childElm = (Element) it1.next();
                String name = childElm.getName();
                if (name.equals("annotations"))
                {
                    GeneSetAnnotation annotation = generateSimpleObjectFromElement(
                            childElm, GeneSetAnnotation.class);
                    annotations.add(annotation);
                }
                else if (name.equals("ids"))
                {
                    ids.add(childElm.getText());
                }
                else if (name.equals("module"))
                {
                    moduleAnnotation.setModule(new Integer(childElm.getText()));
                }
            }
            rtn.add(moduleAnnotation);
        }
        // Sorting based on modules
        Collections.sort(rtn, new Comparator<ModuleGeneSetAnnotation>()
        {
            @Override
            public int compare(ModuleGeneSetAnnotation annot1,
                    ModuleGeneSetAnnotation annot2)
            {
                return annot1.getModule() - annot2.getModule();
            }
        });
        return rtn;
    }

    public List<ModuleGeneSetAnnotation> annotateNetworkModules(
            Map<String, Integer> nodeToModule, String type) throws Exception
    {
        // Recover network modules information
        String url = restfulURL + "network/annotateModules/" + type;
        // Create a query
        StringBuilder builder = new StringBuilder();
        for (String node : nodeToModule.keySet())
        {
            Integer module = nodeToModule.get(node);
            builder.append(node).append("\t").append(module).append("\n");
        }
        return annotateGeneSets(url, builder.toString());
    }

    public Element doSurvivalAnalysis(String scoreMatrix,
            String clinInfoMatrix, String modelName, Integer moduleIndex)
            throws Exception
    {
        // Set a RESTful URL
        // Note: moduleIndex may be null
        String url = restfulURL + "network/survivalAnalysis/" + modelName + "/"
                + moduleIndex;
        // Create a query
        StringBuilder query = new StringBuilder();
        query.append("#Sample score matrix begin!\n");
        query.append(scoreMatrix);
        query.append("#Sample score matrix end!\n");
        query.append("#Clin matrix begin!\n");
        query.append(clinInfoMatrix);
        query.append("#Clin matrix end!\n");
        Element resultElm = callInXML(url, query.toString());
        return resultElm;
    }

    public Element doMCLClustering(Set<String> fisWithCorrs, Double inflation)
            throws Exception
    {
        String url = restfulURL + "network/mclClustering";
        // Create a query
        StringBuilder builder = new StringBuilder();
        for (String fiWithCorr : fisWithCorrs)
        {
            builder.append(fiWithCorr).append("\n");
        }
        builder.append("Inflation: " + inflation);
        Element resultElm = callInXML(url, builder.toString());
        return resultElm;
    }

    public Element doHotNetAnalysis(Map<String, Double> geneToScore,
            Double delta, Double fdrCutoff, Integer permutation)
            throws Exception
    {
        String url = restfulURL + "network/hotnetAnalysis";
        // Create query
        StringBuilder builder = new StringBuilder();
        for (String gene : geneToScore.keySet())
        {
            Double score = geneToScore.get(gene);
            builder.append(gene + "\t" + score).append("\n");
        }
        builder.append("delta:" + delta).append("\n");
        builder.append("fdrCutoff:" + fdrCutoff).append("\n");
        builder.append("permutationNumber:" + permutation).append("\n");
        Element resultElm = callInXML(url, builder.toString());
        return resultElm;
    }

    public Map<String, FIAnnotation> annotate(List<CyEdge> edges,
            CyNetworkView view) throws Exception
    {
        String url = restfulURL + "network/annotate";
        // Create a query
        String query = convertEdgesToString(edges, view);
        Element root = callInXML(url, query);
        List<?> annotations = root.getChildren();
        Map<String, FIAnnotation> edgeIdToAnnotation = new HashMap<String, FIAnnotation>();
        for (Object name : annotations)
        {
            Element element = (Element) name;
            FIAnnotation annotation = generateSimpleObjectFromElement(element,
                    FIAnnotation.class);
            edgeIdToAnnotation.put(annotation.getInteractionId(), annotation);
        }
        return edgeIdToAnnotation;
    }

    private String convertEdgesToString(List<CyEdge> edges, CyNetworkView view)
    {
        StringBuilder queryBuilder = new StringBuilder();
        int compare = 0;
        for (CyEdge edge : edges)
        {
            CyNode start = edge.getSource();
            CyNode end = edge.getTarget();
            // The view must be passed in because a call to getNetworkPointer()
            // returns null. It is impossible to grab a network from a node or edge.
            CyTable edgeTable = view.getModel().getDefaultEdgeTable();
            // edge.getSource().getNetworkPointer().getDefaultEdgeTable();
            String edgeName = edgeTable.getRow(edge.getSUID()).get("name",
                    String.class);
            queryBuilder.append(edgeName).append("\t");

            // Have to make sure the start id is less than end id based on
            // conventions we used in the server side
            CyTable nodeTable = view.getModel().getDefaultNodeTable();
            String startId = nodeTable.getRow(start.getSUID()).get("name",
                    String.class); // start.getSUID().toString();
            String endId = nodeTable.getRow(end.getSUID()).get("name",
                    String.class);
            compare = startId.compareTo(endId);
            if (compare < 0)
            {
                queryBuilder.append(startId).append("\t");
                queryBuilder.append(endId).append("\n");
            }
            else
            {
                queryBuilder.append(endId).append("\t");
                queryBuilder.append(startId).append("\n");
            }
        }
        String query = queryBuilder.toString();
        return query;
    }

    private <T> T generateSimpleObjectFromElement(Element elm, Class<T> cls)
            throws Exception
    {
        T rtn = cls.newInstance();
        List<?> children = elm.getChildren();
        for (Object name2 : children)
        {
            Element child = (Element) name2;
            String name = child.getName();
            String fieldName = name.substring(0, 1).toLowerCase()
                    + name.substring(1);
            Field field = cls.getDeclaredField(fieldName);
            String methodName = null;
            Constructor<?> valueConstructor = null;
            if (field.getType() == List.class)
            {
                methodName = "add" + name.substring(0, 1).toUpperCase()
                        + name.substring(1);
                Method method = getMethod(cls, methodName);
                if (method != null)
                {
                    valueConstructor = method.getParameterTypes()[0]
                            .getConstructor(String.class);
                }
            }
            else
            {
                // Need to call method
                methodName = "set" + name.substring(0, 1).toUpperCase()
                        + name.substring(1);
                valueConstructor = field.getType().getConstructor(String.class);
            }
            Method method = getMethod(cls, methodName);
            if (valueConstructor != null && method != null)
            {
                method.invoke(rtn, valueConstructor
                        .newInstance(child.getText()));
            }
        }
        return rtn;
    }

    private Method getMethod(Class<?> cls, String methodName)
    {
        for (Method method : cls.getMethods())
            if (method.getName().equals(methodName)) return method;
        return null;
    }
    //
}
