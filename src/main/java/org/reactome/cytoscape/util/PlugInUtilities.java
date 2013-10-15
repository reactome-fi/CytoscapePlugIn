/*
 * Created on Jul 15, 2013
 *
 */
package org.reactome.cytoscape.util;

import java.awt.Component;
import java.awt.Container;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Utility methods that can be used by Reactome FI plug-in have been grouped
 * here. There some overlappings between this class and PlugInObjectManager.
 * Probably a refactoring is needed between this class and that one. Or should
 * these classes be merged together?
 * 
 * @author gwu
 * 
 */
public class PlugInUtilities {
    public final static String HTTP_GET = "Get";
    public final static String HTTP_POST = "Post";

    public PlugInUtilities() {
    }
    
    /**
     * A convenience method to show a node in the current network view.
     * 
     * @param nodeView
     *            The View object for the node to show.
     */
    public static void showNode(View<CyNode> nodeView)
    {
        nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
    }

    /**
     * A convenience method to hide a node in the current network view.
     * 
     * @param nodeView
     *            The View object for the node to be hidden.
     */
    public static void hideNode(View<CyNode> nodeView)
    {
        nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
    }

    public static void showEdge(View<CyEdge> edgeView)
    {
        edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
    }

    public static void hideEdge(View<CyEdge> edgeView)
    {
        edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
    }
    
    /**
     * A handy method to select or unselect a node in a network.
     * @param network
     * @param node
     * @param isSelected
     */
    public static void setNodeSelected(CyNetwork network,
                                       CyNode node,
                                       boolean isSelected) {
        Long nodeSUID = node.getSUID();
        CyTable nodeTable = network.getDefaultNodeTable();
        nodeTable.getRow(nodeSUID).set("selected", 
                                       isSelected);
    }
    
    /**
     * Create an empty CyNetwork using registered OGSi service.
     * @return
     */
    public static CyNetwork createNetwork() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(CyNetworkFactory.class.getName());
        CyNetworkFactory networkFactory = (CyNetworkFactory) context.getService(reference);
        CyNetwork network = networkFactory.createNetwork();
        networkFactory = null;
        context.ungetService(reference);
        return network;
    }

    /**
     * Get the JDesktop used by the Swing-based Cytoscape Application.
     * @return
     */
    public static JDesktopPane getCytoscapeDesktop() {
        CySwingApplication application = PlugInObjectManager.getManager().getCySwingApplication();
        JFrame frame = application.getJFrame();
        // Use this loop to find JDesktopPane
        Set<Component> children = new HashSet<Component>();
        for (Component comp : frame.getComponents())
            children.add(comp);
        Set<Component> next = new HashSet<Component>();
        while (children.size() > 0) {
            for (Component comp : children) {
                if (comp instanceof JDesktopPane)
                    return (JDesktopPane) comp;
                if (comp instanceof Container) {
                    Container container = (Container) comp;
                    if (container.getComponentCount() > 0) {
                        for (Component comp1 : container.getComponents())
                            next.add(comp1);
                    }
                }
            }
            children.clear();
            children.addAll(next);
            next.clear();
        }
        return null;
    }  

    /**
     * Show an error message
     * 
     * @param message
     */
    public static void showErrorMessage(String title, String message)
    {
        // Need a parent window to display error message
        CySwingApplication application = PlugInObjectManager.getManager().getCySwingApplication();
        JOptionPane.showMessageDialog(application.getJFrame(), 
                                      message, 
                                      title,
                                      JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Open an OS web browser to display the passed URL.
     * 
     * @param url
     */
    public static void openURL(String url)
    {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference serviceReference = context.getServiceReference(OpenBrowser.class.getName());
        boolean isOpened = false;
        if (serviceReference != null)
        {
            OpenBrowser browser = (OpenBrowser) context.getService(serviceReference);
            if (browser != null)
            {
                browser.openURL(url);
                isOpened = true;
            }
            context.ungetService(serviceReference);
        }
        // In case the passed URL cannot be opened!
        if (!isOpened)
        {
            showErrorMessage("Error in Opening URL",
                    "Error in opening URL: cannot find a configured browser in Cytoscape!");
        }
    }
    
    private static HttpClient initializeHTTPClient(PostMethod post, String query) throws UnsupportedEncodingException {
        RequestEntity entity = new StringRequestEntity(query, "text/plain",
                "UTF-8");
        post.setRequestEntity(entity);
        post.setRequestHeader("Accept", "application/xml, text/plain");
        HttpClient client = new HttpClient();
        return client;
    }
    
    /**
     * Call a RESTful API for an XML document.
     * @param url
     * @param query
     * @return
     * @throws Exception
     */
    public static Element callHttpInXML(String url, 
                                        String type,
                                        String query) throws Exception {
        String text = callHttp(url, 
                               type,
                               query, 
                               "application/xml");
        StringReader reader = new StringReader(text);
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(reader);
        Element root = document.getRootElement();
        return root;
    }
    
    /**
     * Call a RESTful API. The returned text should be in a plain text.
     * @param url
     * @param type 
     * @param query
     * @return
     * @throws IOException
     */
    public static String callHttpInText(String url, String type, String query) throws IOException {
        return callHttp(url, 
                        type, 
                        query, 
                        "text/plain");
    }
    
    /**
     * The actual method body to make a http call.
     * @param url
     * @param type
     * @param query
     * @param requestType
     * @return
     * @throws IOException
     */
    private static String callHttp(String url,
                                   String type,
                                   String query,
                                   String requestType) throws IOException {
        HttpMethod method = null;
        HttpClient client = null;
        if (type.equals(HTTP_POST)) {
            method = new PostMethod(url);
            client = initializeHTTPClient((PostMethod) method, query);
        }
        else {
            method = new GetMethod(url); // Default
            method.setRequestHeader("Accept", requestType);
            client = new HttpClient();
        }
        int responseCode = client.executeMethod(method);
        if (responseCode == HttpStatus.SC_OK) {
            InputStream is = method.getResponseBodyAsStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                builder.append(line).append("\n");
            }
            reader.close();
            isr.close();
            is.close();
            // Remove the last new line
            String rtn = builder.toString();
            // Just in case an empty string is returned
            if (rtn.length() == 0) return rtn;
            return rtn.substring(0, rtn.length() - 1);
        }
        else
            throw new IllegalStateException(method.getResponseBodyAsString());
    }
    
}
