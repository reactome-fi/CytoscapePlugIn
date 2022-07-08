/*
 * Created on Jul 15, 2013
 *
 */
package org.reactome.cytoscape.util;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NETWORK_SCALE_FACTOR;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.math.MathException;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.gk.graphEditor.PathwayEditor;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderablePathway;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jfree.chart.plot.CategoryMarker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Variable;
import org.reactome.pathway.factorgraph.IPACalculator;
import org.reactome.pathway.factorgraph.VariableRole;
import org.reactome.r3.util.InteractionUtilities;
import org.reactome.r3.util.MathUtilities;

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
    public final static String APP_NAME = "ReactomeFIViz";
    public final static String HTTP_GET = "Get";
    public final static String HTTP_POST = "Post";
    public final static int PLOT_CATEGORY_AXIX_LABEL_CUT_OFF = 16;
    public final static Color DRUG_COLOR = new Color(255, 153, 153);

    public PlugInUtilities() {
    }
    
    public static boolean isMac() {
    	String osName = System.getProperty("os.name").toLowerCase();
        return osName != null && osName.contains("mac");
    }
    
    public static boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName != null && osName.contains("win");
    }
    
    public static JTextArea createTextAreaForNote(Component container) {
        JTextArea noteTF = new JTextArea();
        noteTF.setBackground(container.getBackground());
        noteTF.setEditable(false);
        noteTF.setLineWrap(true);
        noteTF.setWrapStyleWord(true);
        Font font = noteTF.getFont();
        noteTF.setFont(font.deriveFont(Font.ITALIC, font.getSize() - 1));
        return noteTF;
    }
    
    /**
     * Set the pathway diagram in XML to be displayed
     */
    public static void setPathwayDiagramInXML(PathwayEditor editor, String xml) throws Exception {
        DiagramGKBReader reader = new DiagramGKBReader();
        RenderablePathway pathway = reader.openDiagram(xml);
        // Hide disease objects
        Set<Integer> normalIds = getNormalIds(xml);
        if (normalIds != null) {
            for (Object obj : pathway.getComponents()) {
                if (obj instanceof RenderableCompartment)
                    continue; // Show the above objects always
                Renderable r = (Renderable) obj;
                if (!normalIds.contains(r.getID()))
                    r.setIsVisible(false);
            }
        }
        editor.setRenderable(pathway);
    }
    
    private static Set<Integer> getNormalIds(String xml) throws IOException {
        StringReader reader = new StringReader(xml);
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        Set<Integer> normalIds = null;
        while ((line = br.readLine()) != null) {
            if (line.trim().startsWith("<normal")) {
                if (normalIds == null)
                    normalIds = new HashSet<>();
                parseNormalIds(line, normalIds);
            }
        }
        br.close();
        reader.close();
        return normalIds;
    }
    
    private static void parseNormalIds(String line,
                                Set<Integer> normalIds) {
        int index1 = line.indexOf(">");
        int index2 = line.indexOf("<", index1);
        line = line.substring(index1 + 1, index2);
        String[] tokens = line.split(",");
        for (String token : tokens)
            normalIds.add(new Integer(token));
    }
    
    public static CyNetworkView getCurrentNetworkView() {
        // Show detailed information for the selected network
        CyApplicationManager appManager = PlugInObjectManager.getManager().getApplicationManager();
        CyNetworkView networkView = null;
        if (appManager != null && appManager.getCurrentNetworkView() != null) {
            networkView = appManager.getCurrentNetworkView();
        }
        return networkView;
    }
    
    public static void unselectNetwork() {
        // Show detailed information for the selected network
        CyApplicationManager appManager = PlugInObjectManager.getManager().getApplicationManager();
        appManager.setCurrentNetwork(null);
    }
    
    /**
     * Convert a list of CyEdges into a list of FIs encoded in Strings.
     * @param edges
     * @param view
     * @return
     */
    public static Set<String> convertEdgesToFIs(List<CyEdge> edges,
                                                 CyNetworkView view) {
        // Have to make sure the start id is less than end id based on
        // conventions we used in the server side
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        Set<String> fis = new HashSet<>();
        int compare = 0;
        for (CyEdge edge : edges) {
            CyNode start = edge.getSource();
            CyNode end = edge.getTarget();
            String startId = nodeTable.getRow(start.getSUID()).get("name",
                    String.class); 
            String endId = nodeTable.getRow(end.getSUID()).get("name",
                    String.class);
            compare = startId.compareTo(endId);
            if (compare < 0) {
                fis.add(startId + "\t" + endId);
            }
            else {
                fis.add(endId + "\t" + startId);
            }
        }
        return fis;
    }
    
    public static void fillTableRow(String header,
                              String value,
                              StringBuilder buffer) {
        buffer.append("<tr>");
        fillTableRowHeader(header, buffer, 1);
        fillTableRowValue(value, buffer);
        buffer.append("</tr>");
    }
    
    /**
     * Used to parse sample lists in a table.
     * @param nodeToSamples
     * @return
     */
    public static Map<String, Set<String>> extractNodeToSampleSet(Map<String, Object> nodeToSamples) {
        Map<String, Set<String>> nodeToSampleSet = null;
        if (nodeToSamples != null) {
            nodeToSampleSet = new HashMap<String, Set<String>>();
            for (String node : nodeToSamples.keySet()) {
                String sampleText = (String) nodeToSamples.get(node);
                String[] tokens = sampleText.split(";");
                Set<String> set = new HashSet<String>();
                for (String token : tokens) {
                    set.add(token);
                }
                nodeToSampleSet.put(node, set);
            }
        }
        return nodeToSampleSet;
    }
    
    public static void fillTableRowHeader(String header, StringBuilder buffer, int rowSpan) {
        buffer.append("<th align=\"left\" bgcolor=\"#C0C0C0\" rowspan=\"" + rowSpan + "\">" + header + "</th>");
    }
    
    public static void fillTableRowValue(String value, StringBuilder buffer) {
        buffer.append("<td>" + value + "</td>");
    }
    
    /**
     * Get a file for analysis.
     * @param title
     * @param readOrWrite FileUtil.SAVE or LOAD.
     * @return
     */
    public static File getAnalysisFile(String title,
                                       int readOrWrite) {
        // Get a file
        Collection<FileChooserFilter> filters = PlugInUtilities.getAnalysisResultFilters();
        return getAnalysisFile(title, readOrWrite, filters);
    }
    
    /**
     * Get a file for analysis.
     * @param title
     * @param readOrWrite FileUtil.SAVE or LOAD.
     * @return
     */
    public static File getAnalysisFile(String title,
                                       int readOrWrite,
                                       Collection<FileChooserFilter> filters) {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(FileUtil.class.getName());
        FileUtil fileUtil = (FileUtil) context.getService(reference);
        File file = fileUtil.getFile(PlugInObjectManager.getManager().getCytoscapeDesktop(), 
                                     title, 
                                     readOrWrite,
                                     filters);
        context.ungetService(reference);
        return file;
    }
    
    /**
     * Get the filter for analysis results.
     * @return
     */
    private static Collection<FileChooserFilter> getAnalysisResultFilters() {
        Collection<FileChooserFilter> filters = new ArrayList<FileChooserFilter>();
        FileChooserFilter filter = new FileChooserFilter("Analysis Results",
                                                         new String[]{"xml", "txt", ".xml", ".txt"});
        filters.add(filter);
        return filters;
    }
    
    /**
     * Calculate combined p-value from a list of p-values using Fisher's method
     * @param pvalues
     * @throws MathException
     */
    public static double calculateCombinedPValue(List<Double> pvalues,
                                         List<List<Double>> values) throws MathException {
        if (pvalues.size() == 0) {
            return Double.MAX_VALUE; // Nothing to do. Mark this as the largest pvalue.
        }
        // Have to make sure no 0 in the pvalues list in order to make the implementation in MathUtility work
        List<Double> pvalueCopy = new ArrayList<Double>();
        for (Double pvalue : pvalues) {
            if (pvalue.equals(0.0d))
                pvalueCopy.add(Double.MIN_VALUE);
            else
                pvalueCopy.add(pvalue);
        }
        double combinedPValue = MathUtilities.combinePValuesWithFisherMethod(pvalueCopy);
        
        // Since it is pretty sure, variables are dependent in pathway, use another method
        // to combine p-values
//        PValueCombiner combiner = new PValueCombiner();
//        double combinedPValue = combiner.combinePValue(values,
//                                                       pvalues);
        return combinedPValue;
    }
    
    public static double[] getMinMax(Collection<Double> values) {
    	double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Double value : values) {
            if (value > max)
                max = value;
            if (value < min)
                min = value;
        }
        double[] rtn = {min, max};
        return rtn;
    }
    
    /**
     * Get the sorted keys used in the table. If nothing is selected, the passed column will be
     * used for sorting in ASCENDING.
     * @return
     */
    public static List<? extends SortKey> getSortedKeys(JTable table,
                                                        int defaultColumn) {
        if (table.getRowSorter() == null)
            return null;
        List<? extends SortKey> sortedKeys = table.getRowSorter().getSortKeys();
        if (sortedKeys != null && sortedKeys.size() > 0)
            return sortedKeys;
        // Otherwise, sort based on p-values
        // We need to initialize a new rtn list to avoid generic related
        // error.
        List<SortKey> rtn = new ArrayList<SortKey>();
        // The second to the last should be the p-value column
        rtn.add(new RowSorter.SortKey(defaultColumn, SortOrder.ASCENDING));
        return rtn;
    }
    
    /**
     * Split the random values into two parts: one for positive and another for negative.
     * P-values should be calculated based on these two parts. In other words, this should
     * be a two-tailed test.
     * @param value
     * @param randomValues
     * @return
     */
    public static double calculateIPAPValue(double value, List<Double> randomValues) {
//        if (value == 0.0d)
//            return 1.0; // Always
        if (value > 0.0d) {
            return calculateNominalPValue(value, randomValues, "right");
        }
        else {
            return calculateNominalPValue(value, randomValues, "left");
        }
    }
    
    /**
     * Calculate nominal p-value based on a list of random values.
     * @param value
     * @param randomValues values should be sorted already from small to large.
     * @param leftOrRight value of left or right.
     * @return
     */
    public static double calculateNominalPValue(double value,
                                                List<Double> randomValues,
                                                String leftOrRight) {
        // Make sure leftOrRight is correct
        if (!leftOrRight.equals("left") && !leftOrRight.equals("right"))
            throw new IllegalArgumentException("leftOrRight should be one of left or right.");
        double pvalue = 1.0d;
        if (leftOrRight.equals("left")) { // Check the left-side value
            int supposedIndex = -1;
            for (int i = 0; i < randomValues.size(); i++) {
                // Find the value that is larger than the real value
                if (randomValues.get(i) > value) {
                    supposedIndex = i;
                    break;
                }
            }
            if (supposedIndex == -1)
                supposedIndex = randomValues.size();
            pvalue = (double) supposedIndex / randomValues.size();
        }
        else { // Check the right-side value
            int supposedIndex = -1;
            for (int i = randomValues.size() - 1; i >= 0; i--) {
                // Find the value that is less than the real value
                if (randomValues.get(i) < value) {
                    supposedIndex = i;
                    break;
                }
            }
            pvalue = (double) (randomValues.size() - supposedIndex - 1) / randomValues.size();
        }
        return pvalue;
    }
    
    private static double calculateIPAPValueRightTail(double value, 
                                                      List<Double> randomValues) {
        // Values in copy should be sorted already.
        int index = -1;
        for (int i = randomValues.size() - 1; i >= 0; i--) {
            if (randomValues.get(i) < value) {
                index = i;
                break;
            }
        }
        // In order to plot and sort, use 0.0 for "<"
//        if (index == randomValues.size() - 1)
//            return "<" + (1.0d / randomValues.size());
        if (index == -1)
            return 1.0d;
        // Move the count one position ahead
        return (double) (randomValues.size() - index - 1) / randomValues.size();
    }
    
    private static double calculateIPAPValueLeftTail(double value, List<Double> randomValues) {
        // Values in copy should be sorted already.
        int index = -1;
        for (int i = 0; i < randomValues.size(); i++) {
            if (randomValues.get(i) > value) {
                index = i;
                break;
            }
        }
        // In order to plot and sort, use 0.0 for "<"
//        if (index == 0)
//            return "<" + (1.0d / randomValues.size());
        if (index == -1)
            return 1.0;
        return (double) index / randomValues.size();
    }
    
    /**
     * Get a set of variables converted from outputs in pathways.
     * @param fg
     * @return
     */
    public static Set<Variable> getOutputVariables(FactorGraph fg) {
        Set<Variable> outputVar = new HashSet<Variable>();
        // If a variable's reactome id is in this list, it should be an output
        for (Variable var : fg.getVariables()) {
            String roles = var.getProperty("role");
            if (roles == null || roles.length() == 0)
                continue;
            if (roles.contains(VariableRole.OUTPUT.toString()))
                outputVar.add(var);
        }
        return outputVar;
    }
    
    /**
     * Get a set of Variables directly related to a pathway.
     * In other words, the original entities used to generate variables
     * should be displayed in a pathway diagram.
     * @param fg
     * @return
     */
    public static Set<Variable> getPathwayVars(FactorGraph fg) {
        Set<Variable> pathwayVars = new HashSet<Variable>();
        // If a variable's reactome id is in this list, it should be a output
        for (Variable var : fg.getVariables()) {
            String roles = var.getProperty("role");
            if (roles != null && roles.length() > 0)
                pathwayVars.add(var); // If there is a role assigned to the variable, it should be used as a pathway variable
        }
        return pathwayVars;
    }
    
    /**
     * Do selection based on a list of DB_IDs for objects displayed in
     * the provided PathwayEditor.
     * @param pathwayEditor
     * @param sourceIds
     */
    public static void selectByDbIds(PathwayEditor pathwayEditor,
                                     Collection<Long> sourceIds) {
        // Check if the passed ids are the same as ids of selected objects
        Set<Long> originalIds = new HashSet<>();
        List<Renderable> oldSelection = pathwayEditor.getSelection();
        for (Renderable r : oldSelection) {
            originalIds.add(r.getReactomeId());
        }
        if (sourceIds.equals(originalIds))
            return; // No need to do anything
        List<Renderable> selection = new ArrayList<Renderable>();
        for (Object o : pathwayEditor.getDisplayedObjects()) {
            Renderable r = (Renderable) o;
            if (!r.isVisible())
                continue;
            if (sourceIds.contains(r.getReactomeId()))
                selection.add(r);
        }
        pathwayEditor.setSelection(selection);
    }
    
    public static List<Long> getSelectedDBIDs(PathwayEditor editor) {
        Set<Long> dbIds = new HashSet<Long>();
        List<Renderable> selection = editor.getSelection();
        for (Renderable r : selection) {
            if (r.getReactomeId() != null)
                dbIds.add(r.getReactomeId());
        }
        return new ArrayList<Long>(dbIds);
    }
    
    /**
     * Calculate a list of IPAs for values in sampleToProbs. The returned list is
     * sorted by sample names alphabetically.
     * @param priorValues
     * @param sampleToProbs
     * @return
     */
    public static List<Double> calculateIPAs(List<Double> priorValues,
                                             Map<String, List<Double>> sampleToProbs) {
        List<String> sampleList = new ArrayList<String>(sampleToProbs.keySet());
        Collections.sort(sampleList);
        List<Double> ipas = new ArrayList<Double>();
        for (String sample : sampleList) {
            List<Double> probs = sampleToProbs.get(sample);
            double ipa = IPACalculator.calculateIPA(priorValues, probs);
            ipas.add(ipa);
        }
        return ipas;
    }
    
    /**
     * A type can have multiple samples. This method converts sampleToType to typeToSamples.
     * @param sampleToType
     * @return
     */
    public static Map<String, Set<String>> getTypeToSamples(Map<String, String> sampleToType) {
        // Do a reverse map
        Map<String, Set<String>> typeToSamples = new HashMap<String, Set<String>>();
        for (String sample : sampleToType.keySet()) {
            String type = sampleToType.get(sample);
            InteractionUtilities.addElementToSet(typeToSamples, type, sample);
        }
        return typeToSamples;
    }
    
    public static double[] convertDoubleListToArray(List<Double> list) {
        double[] rtn = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Double value = list.get(i);
            if (value == null)
                throw new IllegalArgumentException("Double List contains a null value!");
            rtn[i] = value;
        }
        return rtn;
    }
    
    public static List<Double> convertArrayToList(double[] values) {
        List<Double> list = new ArrayList<Double>(values.length);
        for (int i = 0; i < values.length; i++)
            list.add(values[i]);
        return list;
    }
    
    /**
     * Get all genes displayed in a network view.
     * @param networkView
     * @return
     */
    public static Set<String> getDisplayedGenesInNetwork(CyNetwork network) {
        List<CyNode> nodes = network.getNodeList();
        return getGeneNamesFromNodes(network, nodes);
    }
    
    private static Set<String> getGeneNamesFromNodes(CyNetwork network,
                                                     List<CyNode> nodes) {
        if (nodes == null || nodes.size() == 0)
            return new HashSet<String>();
        Set<String> genes = new HashSet<>();
        CyTable nodeTable = network.getDefaultNodeTable();
        for (CyNode node : nodes) {
            String nodeName = nodeTable.getRow(node.getSUID()).get("name", String.class);
            if (nodeName != null)
                genes.add(nodeName);
        }
        return genes;
    }
    
    public static Set<String> getSelectedGenesInNetwork(CyNetwork network) {
        List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network,
                                                                 CyNetwork.SELECTED,
                                                                 true);
        return getGeneNamesFromNodes(network, selectedNodes);
    }
    
    /**
     * Get the index for a CytoPanelComponent specified by its name in the passed
     * CytoPanel object. If nothing can be found, -1 is going to be returned.
     * @param cytoPanel
     * @param title
     * @return
     */
    public static int getCytoPanelComponent(CytoPanel cytoPanel,
                                            String title) {
        int numComps = cytoPanel.getCytoPanelComponentCount();
        for (int i = 0; i < numComps; i++) {
            CytoPanelComponent aComp = (CytoPanelComponent) cytoPanel.getComponentAt(i);
            if (aComp.getTitle().equalsIgnoreCase(title)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get an existing CytoPanelComponent
     * @param name
     * @param title
     * @return
     */
    public static CytoPanelComponent getCytoPanelComponent(CytoPanelName name,
                                                           String title) {
        CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(name);
        int index = PlugInUtilities.getCytoPanelComponent(cytoPanel, 
                                                          title);
        // The class should return itself in method getComponent() to make this work
        if (index > -1) {
            CytoPanelComponent component = (CytoPanelComponent) cytoPanel.getComponentAt(index); 
            return component;
        }
        return null;
    }

    /**
     * Get the index of a CytoPanelComponent. If this CytoPanelComponent doesn't exit,
     * this method will initialize it.
     * @param cytoPanelCls
     * @param name
     * @param title
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static <T extends CytoPanelComponent> T getCytoPanelComponent(Class<T> cytoPanelCls,
                                                                         CytoPanelName name,
                                                                         String title) {
        // Show samples for the user's selection
        CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(name);
        // We want the east pane in the dock state
        if (cytoPanel.getState() != CytoPanelState.DOCK)
            cytoPanel.setState(CytoPanelState.DOCK);
        CytoPanelComponent component = null;
        int index = PlugInUtilities.getCytoPanelComponent(cytoPanel, 
                                                          title);
        // The class should return itself in method getComponent() to make this work
        if (index > -1) {
            component = (CytoPanelComponent) cytoPanel.getComponentAt(index); 
            // Make sure component is the required type
            if(!cytoPanelCls.isInstance(component)) {
                // A little bit hack way
                if (cytoPanel instanceof JPanel) {
                    JPanel pane = (JPanel) cytoPanel;
                    pane.getParent().remove(pane);
                    index = -1;
                }
            }
        }
        if (index < 0) {
            try {
                component =  (CytoPanelComponent) cytoPanelCls.newInstance();
                index = cytoPanel.indexOfComponent(component.getComponent());
            }
            catch(Exception e) {
                throw new IllegalStateException(e);
            }
        }
        if (index > -1)
            cytoPanel.setSelectedIndex(index);
        return (T) component;
    }
                                            
    
    /**
     * Create a mark for the plot.
     * @param category
     * @return
     */
    public static CategoryMarker createMarker(Comparable<?> category) {
        CategoryMarker marker = new CategoryMarker(category);
        marker.setStroke(new BasicStroke(2.0f)); // Give it an enough stroke
        marker.setPaint(new Color(0.0f, 0.0f, 0.0f, 0.5f));
        return marker;
    }
    
    public static String join(double[] values) {
        StringBuilder builder = new StringBuilder();
        for (double v : values)
            builder.append(v).append(", ");
        return builder.toString();
    }
    
    /**
     * A customized method to zoom into a list of selected node. If there is only
     * one node is selected, some zoom-out is done to provide some context.
     * @param networkView
     * @param totalSelected
     */
    public static void zoomToSelected(CyNetworkView networkView,
                                      int totalSelected) {
        if (networkView == null || networkView.getModel() == null)
            return;
        if (totalSelected > 0) {
        	double oldFactor = networkView.getVisualProperty(NETWORK_SCALE_FACTOR).doubleValue();
            networkView.fitSelected();
            if (totalSelected == 1) {
            	// Zoom back when only one node is selected
            	double fitFactor = networkView.getVisualProperty(NETWORK_SCALE_FACTOR).doubleValue();
            	double newFactor = fitFactor * 0.1d; // In case zoom to fit is too much
            	if (newFactor < oldFactor) // Only re-zoom 
            		newFactor = oldFactor;
            	networkView.setVisualProperty(NETWORK_SCALE_FACTOR, newFactor);
            }
        }
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
    
    public static void showAllNodes(CyNetworkView view) {
        if (view == null)
            return;
        view.getNodeViews().forEach(nodeView -> showNode(nodeView));
    }
    
    public static void showAllEdges(CyNetworkView view) {
        if (view == null)
            return;
        view.getEdgeViews().forEach(edgeView -> showEdge(edgeView));
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
    
    public static ServiceRegistration registerCytoPanelComponent(final CytoPanelComponent comp) {
        // As of Dec 5, 2015, manual insert tab to control close during Quit.
        // Otherwise, index exception thrown for tabs used for PGMs.
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel cytoPane = desktopApp.getCytoPanel(comp.getCytoPanelName());
        JTabbedPane tabbedPane = getTabbedPane(cytoPane);
//        System.out.println("TabbedPane: " + tabbedPane);
        if (tabbedPane == null) {
            return context.registerService(CytoPanelComponent.class.getName(), 
                                           comp, 
                                           new Properties());
        }
        else { // Control self to avoid bug related to thread issues
            tabbedPane.add(comp.getTitle(), 
                           comp.getComponent());
            // Need to remove it when this bundle is closed
            context.addBundleListener(new SynchronousBundleListener() {

                @Override
                public void bundleChanged(BundleEvent event) {
                    if (event.getType() == BundleEvent.STOPPING) {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    Container container = comp.getComponent().getParent();
                                    if (container != null)
                                        container.remove(comp.getComponent());
                                }
                            });
                        }
                        catch(Exception e) {e.printStackTrace();} 
                    }
                }
            });
            return null;
        }
    }
    
    public static JTabbedPane getTabbedPane(CytoPanel panel) {
        Component comp = panel.getThisComponent();
        if (comp instanceof Container) {
            Container container = (Container) comp;
            for (int i = 0; i < container.getComponentCount(); i++) {
                Component comp1 = container.getComponent(i);
                if (comp1 instanceof JTabbedPane)
                    return (JTabbedPane) comp1;
            }
        }
        return null;
    }
    
    public static String formatProbability(double value) {
        if (Double.isNaN(value))
            return value + "";
        if (Math.abs(value) < 0.01)
            return String.format("%.3E", value);
        else
            return String.format("%.3f", value);
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
    
    public static void browseFileForLoad(JTextField fileNameTF, 
                                         String dialogTitle,
                                         String[] fileExtensions) {
        Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
        if (fileExtensions != null && fileExtensions.length > 0) {
            FileChooserFilter filter = new FileChooserFilter(dialogTitle, fileExtensions);
            filters.add(filter);
        }
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference fileUtilRef = context.getServiceReference(FileUtil.class.getName());
        FileUtil fileUtil = (FileUtil) context.getService(fileUtilRef);
        // JTextField is not supported here. Have to do explicitly
        Window window = (Window) SwingUtilities.getAncestorOfClass(Window.class, fileNameTF);
        File dataFile = fileUtil.getFile(window,
                                         dialogTitle, 
                                         FileUtil.LOAD, 
                                         filters);
        if (dataFile == null)
            fileNameTF.setText("");
        else
            fileNameTF.setText(dataFile.getAbsolutePath());
        fileNameTF.setToolTipText(fileNameTF.getText());
        context.ungetService(fileUtilRef);
    }
    
    /**
     * Open a web page for a gene in a web browser.
     * @param gene
     */
    public static void queryGeneCard(String gene) {
        String url = "http://www.genecards.org/cgi-bin/carddisp.pl?gene=" + gene;
        openURL(url);
    }
    
    public static void queryCosmic(String gene) {
        String url = "http://cancer.sanger.ac.uk/cosmic/gene/overview?ln=" + gene;
        openURL(url);
    }
    
    public static void queryGoogle(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = "https://www.google.com/#q=" + encodedQuery;
            openURL(url);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void openReacfoam(String dataType,
                                    String analysisToken,
                                    String dbId,
                                    boolean needCoverage) {
        if (!dataType.equals("FDR")) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Note: The values used to highlight pathways in Reacform are " + dataType + ".",
                                          "Warning About Data Type",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
        String reacfoamUrl = "http://localhost:" + 
                              PlugInObjectManager.getManager().getProperties().getProperty("reacfoam_port") + 
                              "/reacfoam/index.html?species=" + dbId;
        if (analysisToken != null && analysisToken.length() > 0)
            reacfoamUrl += "&analysis=" + analysisToken;
        if (needCoverage)
        	reacfoamUrl += "&coverage=true&color=COPPER_COV"; // Use a customized color scheme for better view
        // TODO: Don't forget to add a new entry useCyBrowser=false for the preference to avoid using the low functional CyBrowser.
        PlugInUtilities.openURL(reacfoamUrl);
    }
    
    public static void openPathwayPlot(String token) {
    	String pathwayPlotUrl = "http://localhost:" + 
                PlugInObjectManager.getManager().getProperties().getProperty("reacfoam_port") + 
                "/pathwayplot.html";
    	if (token != null)
    		pathwayPlotUrl += ("?analysis=" + token);
    	PlugInUtilities.openURL(pathwayPlotUrl);
    }

    /**
     * Open an OS web browser to display the passed URL.
     * 
     * @param url
     */
    @SuppressWarnings("unchecked")
    public static void openURL(String url) {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        // In Cytoscape 3.8.0, the default CyBrowser is used. However, it is not good enough to support
        // some of HTML page (e.g. reactfoam). Therefore, we want to use the desktop browser by doing this.
        CyServiceRegistrar serviceRegistrar = PlugInObjectManager.getManager().getServiceRegistra();
        CyProperty<Properties> cyProps = serviceRegistrar.getService(CyProperty.class,
                                                                     "(cyPropertyName=cytoscape3.props)");
        Properties props = cyProps.getProperties();
        String propKey = "useCyBrowser";
        String oldValue = props.getProperty(propKey); // The key is defined in 3.8.0 only. Therefore, we hard-coded this
        if (oldValue == null || Boolean.parseBoolean(oldValue)) {
            // Default is true. But we want to set it false
            props.setProperty(propKey, Boolean.FALSE + "");
        }
        // We want to use the desktop version of browser
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
        // Reset values
        if (oldValue == null)
            props.remove(propKey);
        else if (Boolean.parseBoolean(oldValue))
            props.setProperty(propKey, oldValue);
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
                               "text/plain",
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
                        "text/plain",
                        "text/plain, application/xml"); // A String may be wrapped in an XML element.
    }
    
    /**
     * Call a RESTful API. The returned text should be in a json text.
     * @param url
     * @param type 
     * @param query
     * @return
     * @throws IOException
     */
    public static String callHttpInJson(String url, String type, String query) throws IOException {
        return callHttp(url, 
                        type, 
                        query, 
                        "text/plain",
                        "application/json"); // A String may be wrapped in an XML element.
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
                                   String sendType,
                                   String requestType) throws IOException {
        HttpMethod method = null;
        
        if (type.equals(HTTP_POST)) {
            method = new PostMethod(url);
            PostMethod post = (PostMethod) method;
            RequestEntity entity = new StringRequestEntity(query, 
                    sendType,
                    "UTF-8");
            post.setRequestEntity(entity);
            post.setRequestHeader("Accept", requestType);
        }
        else {
            method = new GetMethod(url); // Default
            method.setRequestHeader("Accept", requestType);
        }
        HttpClient client = new HttpClient();
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
