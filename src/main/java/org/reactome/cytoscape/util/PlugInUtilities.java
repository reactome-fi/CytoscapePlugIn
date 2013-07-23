/*
 * Created on Jul 15, 2013
 *
 */
package org.reactome.cytoscape.util;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.swing.JOptionPane;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape3.PlugInScopeObjectManager;

/**
 * Utility methods that can be used by Reactome FI plug-in have been grouped
 * here.
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
     * Show an error message
     * 
     * @param message
     */
    public static void showErrorMessage(String title, String message)
    {
        // Need a parent window to display error message
        Component parent = null;
        BundleContext context = PlugInScopeObjectManager.getManager()
                .getBundleContext();
        ServiceReference serviceReference = context
                .getServiceReference(CySwingApplication.class.getName());
        if (serviceReference != null)
        {
            CySwingApplication cytoscape = (CySwingApplication) context
                    .getService(serviceReference);
            if (cytoscape != null)
            {
                parent = cytoscape.getJFrame();
            }
        }
        JOptionPane.showMessageDialog(parent, message, title,
                JOptionPane.ERROR_MESSAGE);
        if (serviceReference != null)
        { // Unget the service and null serviceReference
            context.ungetService(serviceReference);
            serviceReference = null;
        }
    }

    /**
     * Open an OS web browser to display the passed URL.
     * 
     * @param url
     */
    public static void openURL(String url)
    {
        BundleContext context = PlugInScopeObjectManager.getManager()
                .getBundleContext();
        ServiceReference serviceReference = context
                .getServiceReference(OpenBrowser.class.getName());
        boolean isOpened = false;
        if (serviceReference != null)
        {
            OpenBrowser browser = (OpenBrowser) context
                    .getService(serviceReference);
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
    public static Element callHttpInXML(String url, String query) throws Exception {
        PostMethod post = new PostMethod(url);
        HttpClient client = initializeHTTPClient(post, query);
        int responseCode = client.executeMethod(post);
        if (responseCode == HttpStatus.SC_OK)
        {
            InputStream stream = post.getResponseBodyAsStream();
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(stream);
            Element root = document.getRootElement();
            return root;
        }
        else
            throw new IllegalStateException(post.getResponseBodyAsString());
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
        HttpMethod method = null;
        HttpClient client = null;
        if (type.equals(HTTP_POST)) {
            method = new PostMethod(url);
            client = initializeHTTPClient((PostMethod) method, query);
        }
        else {
            method = new GetMethod(url); // Default
            method.setRequestHeader("Accept", "text/plain");
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
