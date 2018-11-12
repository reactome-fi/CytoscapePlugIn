package org.reactome.cytoscape.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * A class for handling interaction with the KEGG
 * RESTful API. Displays a KEGG diagram in a browser
 * window with genes present in the sample highlighted in red.
 * @author Eric T. Dawson
 *
 */
public class KEGGHelper
{
    private String KEGG_BASE_URL = "http://rest.kegg.jp/";
    private String KEGG_PATHWAY_DETAIL_URL = "http://www.kegg.jp/kegg-bin/show_pathway?";
    
    private String getKeggMapNumber(String pathway) throws IOException
    {
        String mapNumber = "";
        String keggLocation = KEGG_BASE_URL + "list/pathway/hsa";
        BufferedReader br = null;
        try
        {
            String current = "";
            URL url = new URL(keggLocation);
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((current = br.readLine()) != null)
            {
                if (current.contains(pathway))
                {
                    mapNumber = current.substring(5, 13);
                    break;
                }
            }
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        if (br != null)
            br.close();
        return mapNumber;
    }

    public void openKeggUrl(String pathway, String nodes)
    {
        if (pathway.endsWith("(K)"))
        {
            pathway = pathway.substring(0, pathway.length() - 3);
        }
        try
        {
            StringBuilder urlBuilder = new StringBuilder(KEGG_PATHWAY_DETAIL_URL);
            String mapNumber = getKeggMapNumber(pathway);
            if (mapNumber == null || mapNumber.length() == 0)
                {
                    PlugInUtilities.showErrorMessage("Nonexistent Pathway", "No such KEGG Pathway");
                    return;
                }
            else
            {
                urlBuilder.append(mapNumber);
            }
            //For more complex coloring schema, use the following code.
            //List<String> keggIds = getKeggIds(Arrays.asList(nodes.split(",")));
            //if (!keggIds.isEmpty())
                //urlBuilder.append("/");
//            for (String id : keggIds)
//            {
//                //urlBuilder.append(id + "%09," + GENE_COLOR + "/");
//                System.out.println(urlBuilder);
//            }
            for (String node : nodes.split(","))
            {
                urlBuilder.append("+" + node);
            }
            String url = urlBuilder.toString();
            PlugInUtilities.openURL(url);
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            PlugInUtilities.showErrorMessage("Error in Opening KEGG", "The KEGG pathway could not be displayed.");
        }
    }
}
