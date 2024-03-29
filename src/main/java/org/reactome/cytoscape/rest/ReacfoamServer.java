package org.reactome.cytoscape.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.pathway.EventTreePane;
import org.reactome.cytoscape.pathway.PathwayControlPanel;
import org.reactome.cytoscape.pathway.EventTreePane.EventObject;
import org.reactome.cytoscape.rest.tasks.PathwayEnrichmentResults;
import org.reactome.cytoscape.sc.ScNetworkManager;
import org.reactome.cytoscape.sc.utils.ScPathwayMethod;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * A simple bona fide sever as a mediator and provide data for the reacfoam JavaScript app.
 * @author wug
 *
 */
public class ReacfoamServer {
    // For logging 
    private static final Logger logger = LoggerFactory.getLogger(ReacfoamServer.class);
    private ServerSocket serverSocket;
    
    public static void main(String[] args) throws IOException {
        new ReacfoamServer().start();
    }
    
    public ReacfoamServer() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.addBundleListener(new BundleListener() {
            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getType() == BundleEvent.STOPPING) {
                    try {
                        stop();
                    }
                    catch(IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
    }
    
    public void stop() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
            logger.info("ReacfoamServer is closed!");
        }
    }
    
    public void start() throws IOException {
        // Use 0 for the systems to provide an available one
        serverSocket = new ServerSocket(0);
        PlugInObjectManager.getManager().getProperties().setProperty("reacfoam_port",
                                                                     serverSocket.getLocalPort() + "");
        logger.info("ReacfoamServer started at " + serverSocket.getLocalPort() + "...");
        while (true) {
            Socket socket = serverSocket.accept();
            Thread t = new Thread() {
                public void run() {
                    handleResponse(socket);
                }
            };
            t.start();
        }
    }
    
    private String getURL(String line) {
        if (line.startsWith("GET") || line.startsWith("PUT")) {
            return line.split(" ")[1];
        }
        return null;
    }
    
    private void handleResponse(Socket socket) {
        try {
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is,
                                                                         StandardCharsets.UTF_8));
            String line = null;
            String url = null;
            while ((line = br.readLine()) != null) {
                url = getURL(line);
                if (url != null)
                    break; 
                if (line.trim().length() == 0)
                    break; // Have to add this line. Otherwise this while loop will be stuck!
            }
            //        System.out.println("URL: " + url);
            OutputStream out = socket.getOutputStream();
            if (url == null) {
                out.write("Not supported".getBytes());
                out.close();
                br.close();
                is.close();
                socket.close();
                return;
            }
            // HTTP header is required
            String header = "HTTP/1.1 200 OK\r\nContent-Type: " + getContentType(url) + "\r\n\r\n";
            //        String header = "HTTP/1.1 200 OK\r\n\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.flush();
            // As the response
            if (url.startsWith("/reacfoam")) {
                String appUrlName = PlugInObjectManager.getManager().getReactomeRESTfulAppURL();
                URL appUrl = new URL(appUrlName + url);
//                System.out.println(appUrl);
                InputStream appIs = appUrl.openStream();
                int length = 0;
                byte[] buffer = new byte[1024];
                while ((length = appIs.read(buffer, 0, buffer.length)) > 0) {
                    // Just in case. However, the following exception still thrown
                    // if the connection with browser is done: java.net.SocketException: Broken pipe (Write failed)
                    // Not sure how to solve this issue!
                    if (!socket.isClosed()) 
                        out.write(buffer, 0, length);
                }
                out.flush();
                appIs.close();
            }
            else if (url.startsWith("/reactomefiviz")) { // Calling the local Cytoscape CyREST API.
                // There are two cases are supported
                if (url.contains("/event/")) {
                    int index = url.lastIndexOf("/");
                    selectEvent(url.substring(index + 1));
                }
                else if (url.endsWith("reacfoam/enrichment")) {
                	// For scRNA-seq, we expect a pattern like this: /reactomefiviz_sc_cluster_1_aucell
                	String analysisToken = url.split("/")[1]; // The first token is an empty string
                	if (analysisToken.equals("reactomefiviz"))
                		outputEnrichment(out); // For pathway tree
                	else if (analysisToken.matches("reactomefiviz_sc_cluster_(\\d)+_(\\w)+")) // For cluster pathway activities
                		outputScClusterPathwayActivities(out, analysisToken);
                }
                else
                    out.write("Not supported".getBytes());
            }
            else 
                out.write("Not supported".getBytes());
            out.close();
            br.close();
            is.close();
            socket.close();
        }
        catch (Exception e) {
            // In theory we should close all open sockets and streams in the try block.
            // However, to make code easy, just throw an exception.
            logger.error(e.getMessage(), e);
        }
        finally {
            if (!socket.isClosed()) {
                try {
                    socket.close();
                }
                catch(IOException e) {
                    // Last try
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
    
    private void outputScClusterPathwayActivities(OutputStream os,
                                                  String analysisToken) throws Exception {
    	EventTreePane treePane = PathwayControlPanel.getInstance().getEventTreePane();
        Map<String, EventObject> nameToObject = treePane.grepEventNameToObject();
    	String[] tokens = analysisToken.split("_");
    	int cluster = Integer.parseInt(tokens[3]);
    	ScPathwayMethod method = ScPathwayMethod.valueOf(tokens[4]);
    	Map<String, Double> pathway2score = ScNetworkManager.getManager().fetchClusterPathwayActivities(method, cluster);
    	PathwayEnrichmentResults results = new PathwayEnrichmentResults();
    	if (pathway2score != null && pathway2score.size() > 0) {
    		pathway2score.forEach((p, score) -> {
    			EventObject event = nameToObject.get(p);
    			// Just in case: there may be some unsyc happens.
    			if (event == null)
    				return;
    			results.addPathway(event.getStId(),
    					           event.getName(),
    					           score + "",
    					           "1.0", // To mimic ratio
    					           score + "");
    					           
    		});
    	}
    	outputEnrichment(os, results);
    }
    
    private void outputEnrichment(OutputStream os) throws IOException {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(ReactomeFIVizResource.class.getName());
        ReactomeFIVizResource resource = (ReactomeFIVizResource) context.getService(reference);
        if (resource == null) {
            context.ungetService(reference);
            return;
        }
        PathwayEnrichmentResults results = resource.fetchEnrichmentResults();
        if (results == null)
            os.write("No results".getBytes());
        outputEnrichment(os, results);
        context.ungetService(reference);
    }

	private void outputEnrichment(OutputStream os, PathwayEnrichmentResults results)
	        throws IOException, JsonGenerationException, JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        // Force to look at any fields
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        ObjectWriter writer = mapper.writer();
        writer.writeValue(os, results);
	}
    
    /**
     * We will call the serice directly to avoid any huddle related to the REST API.
     * @param eventId
     */
    private void selectEvent(String eventId) {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(ReactomeFIVizResource.class.getName());
        ReactomeFIVizResource resource = (ReactomeFIVizResource) context.getService(reference);
        if (resource != null) {
            resource.selectEvent(eventId);
            resource = null;
        }
        context.ungetService(reference);
    }
    
    /**
     * Currently this is hard-coded based on the files in the reacfoam project.
     * @param url
     * @return
     */
    private String getContentType(String url) {
        // Special case
        if (url.startsWith("/reactomefiviz"))
            return "application/json";
        int index = url.indexOf("?");
        if (index < 0)
            index = url.length();
        url = url.substring(0, index);
        if (url.endsWith(".html"))
            return "text/html";
        if (url.endsWith(".json"))
            return "application/json";
        if (url.endsWith(".css"))
            return "text/css";
        if (url.endsWith(".js"))
            return "application/javascript";
        if (url.endsWith(".png"))
            return "image/png";
        if (url.endsWith(".svg"))
            return "imag/svg+xml";
        if (url.endsWith(".ico"))
            return "image/x-icon";
        if (url.contains("fonts")) { // This should be after svg
            index = url.lastIndexOf(".");
            return "font/" + url.substring(index + 1); // This may not work
        }
        return "text/plain"; // as the default
    }
}
