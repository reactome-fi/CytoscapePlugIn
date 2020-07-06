/*
 * Created on Jul 25, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This method is used as a mediator to call Reactome RESTful API.
 * @author gwu
 *
 */
public class ReactomeRESTfulService {
    private String restfulAPIUrl;
//    private static ReactomeRESTfulService service;
    
    /**
     * Default constructor
     */
    private ReactomeRESTfulService() {
        restfulAPIUrl = PlugInObjectManager.getManager().getReactomeRESTfulURL();
    }
    
    public static ReactomeRESTfulService getService() {
//        if (service == null)
//            service = new ReactomeRESTfulService();
//        return service;
        // As of Feb 13, 2018, use new service for each call in case port is reset
        return new ReactomeRESTfulService();
    }
    
    /**
     * Get the FrontPageItems for human pathways.
     * @return
     */
    public Element frontPageItems() throws Exception {
        String url = restfulAPIUrl + "frontPageItems/Homo+sapiens";
        Element root = PlugInUtilities.callHttpInXML(url,
                                                     PlugInUtilities.HTTP_GET,
                                                     null);
        return root;
    }
    
    public String pathwayHierarchy(String species) throws Exception {
        species = species.replaceAll(" ", "+"); // Encode the species name
        String url = restfulAPIUrl + "pathwayHierarchy/" + species;
        String text = PlugInUtilities.callHttpInText(url, PlugInUtilities.HTTP_GET, null);
        return text;
    }
    
    /**
     * Get the PathwayDiagram in XML for a pathway specified by its DB_ID.
     */
    public String pathwayDiagram(Long pathwayId) throws Exception {
        String url = restfulAPIUrl + "pathwayDiagram/" + pathwayId + "/xml";
        String text = PlugInUtilities.callHttpInText(url, PlugInUtilities.HTTP_GET, "");
        return text;
    }
    
    /**
     * Get contained event ids for a pathway specified by its DB_ID.
     * @param pathwayId
     * @return
     * @throws Exception
     */
    public List<Long> getContainedEventIds(Long pathwayId) throws Exception {
        String url = restfulAPIUrl + "getContainedEventIds/" + pathwayId;
        String text = PlugInUtilities.callHttpInText(url, PlugInUtilities.HTTP_GET, "");
        String[] tokens = text.split(",");
        List<Long> rtn = new ArrayList<Long>();
        for (String token : tokens)
            rtn.add(new Long(token));
        return rtn;
    }
    
    /**
     * Query an instance based on its DB_ID and ClassName.
     * @param id
     * @param clsName
     * @return
     * @throws Exception
     */
    public Element queryById(Long id, String clsName) throws Exception {
        String url = restfulAPIUrl + "queryById/" + clsName + "/" + id;
        Element root = PlugInUtilities.callHttpInXML(url, 
                                                     PlugInUtilities.HTTP_GET, 
                                                     null);
        return root;
    }
}
