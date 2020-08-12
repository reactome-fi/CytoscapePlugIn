package org.reactome.cytoscape.sc;

import java.awt.Color;
import java.awt.Paint;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.FIVisualStyleImpl;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;

import smile.plot.swing.Palette;

// In Cytoscape, to make sure the node border used for cells should be colored in a very zoomed
// level, change the value for render.nodeBorderThreshold in properties.cytoscape 3 as 0. This is more
// like a bug to me. 
public class SCNetworkVisualStyle extends FIVisualStyleImpl {
    public static final String CLUSTER_NAME = "cluster";
    public static final String CONNECTIVITY_NAME = "connectivity";
    public static final String EDGE_IS_DIRECTED = "isDirected";
    public static final String CELL_NUMBER_NAME = "cells";
    // This is the key used in scanpy
    public static final String DPT_NAME = "dpt_pseudotime";
    public static final String CYTOTRACE_NAME = "cytotrace";
    // For cluster ids
    public static final String CLUSTER_NODE_PREFIX = "cluster";
    private final double DEFAULT_NODE_SIZE = 0.10d;
    private final double MIN_EDGE_WIDTH = DEFAULT_NODE_SIZE / 100.0d;
    private final double MAX_EDGE_WIDTH = DEFAULT_NODE_SIZE / 10.0d;
    protected boolean needNodeLabel = false;
    
    public SCNetworkVisualStyle() {
        styleName = "Single Cell Style";
    }
    
    /**
     * Copied some of code from the supper class to avoid showing node labels, which is very difficult to
     * control with the coordinates returned from scanpy.
     */
    @Override
    protected void setDefaultNodeStyle(VisualStyle fiVisualStyle, 
                                       VisualMappingFunctionFactory visMapFuncFactoryP) {
        // Set the default node shape and color
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_SHAPE,
                                      NodeShapeVisualProperty.ELLIPSE);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH,
                                      0.0d); // Don't want any border for cells
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.black);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_PAINT, Color.cyan);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_TRANSPARENCY, 100);
        // Don't want to have any label for performance
        if (!needNodeLabel)
            fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL, null);
        else {
            String nodeLabelAttrName = "nodeLabel";
            PassthroughMapping<String, String> labelFunction = (PassthroughMapping<String, String>) visMapFuncFactoryP.createVisualMappingFunction(
                    nodeLabelAttrName, String.class, BasicVisualLexicon.NODE_LABEL);
            fiVisualStyle.addVisualMappingFunction(labelFunction);
        }
        String toolTipAttrName = "nodeToolTip";
        PassthroughMapping<String, String> toolTipFunction = (PassthroughMapping<String, String>) visMapFuncFactoryP.createVisualMappingFunction(toolTipAttrName, 
                                                                                                                 String.class, 
                                                                                                                 BasicVisualLexicon.NODE_TOOLTIP);
        fiVisualStyle.addVisualMappingFunction(toolTipFunction);
        // Really small nodes
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_SIZE, DEFAULT_NODE_SIZE);
    }
    
    @Override
    protected void handleNodeHighlight(VisualStyle fiVisualStyle,
                                       VisualMappingFunctionFactory visMapFuncFactoryD,
                                       VisualMappingFunctionFactory visMapFuncFactoryC) {
        // Turn hit gene display off.
    }
    
    @Override
    protected void displayNodeType(VisualStyle style,
                                   VisualMappingFunctionFactory visMapFuncFactoryD) {
        // Set the node color based on module
        DiscreteMapping<Integer, Paint> colorToModuleFunction = (DiscreteMapping<Integer, Paint>) visMapFuncFactoryD.createVisualMappingFunction(CLUSTER_NAME,
                                                                                                                                                 Integer.class,
                                                                                                                                                 BasicVisualLexicon.NODE_FILL_COLOR);
        // We use the colors defined in the smile package for 100 modules, which should be fig enough
        IntStream.range(0, 100).forEach(i -> {
            Color color = Palette.COLORS[i % Palette.COLORS.length];
            colorToModuleFunction.putMapValue(i, color);
        });
        style.addVisualMappingFunction(colorToModuleFunction);
    }
    
    public void updateNodeColors(CyNetworkView view,
                                 String attributeName,
                                 Class<?> type) {
        if (String.class.isAssignableFrom(type)) {
            updateNodeColorsForCategories(view, attributeName);
            return;
        }
        if (Number.class.isAssignableFrom(type)) {
            updateNodeColorsForNumbers(view, attributeName);
        }
    }
    
    private void updateNodeColorsForCategories(CyNetworkView view, String attributeName) {
        VisualStyle style = getVisualStyle();
        if (style == null)
            return;
        ServiceReference reference = getVisualMappingFunctionFactorServiceReference("(mapping.type=discrete)");
        if (reference == null) {
            return ;
        }
        Map<Long, Object> idToValue = new TableHelper().getNodeTableValuesBySUID(view.getModel(), 
                                                                                 attributeName, 
                                                                                 String.class);
        if (idToValue == null || idToValue.isEmpty())
            return ;
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        VisualMappingFunctionFactory visMapFuncFactory = (VisualMappingFunctionFactory) context.getService(reference);
        // Set the node color based on module
        DiscreteMapping<String, Paint> colorToModuleFunction = (DiscreteMapping<String, Paint>) visMapFuncFactory.createVisualMappingFunction(attributeName,
                                                                                                                                              String.class,
                                                                                                                                              BasicVisualLexicon.NODE_FILL_COLOR);
        Comparator<Object> sorter = (v1, v2) -> {
            String s1 = v1.toString();
            String s2 = v2.toString();
            if (s1.matches("\\d+") && s2.matches("\\d+")) {
                // Both are integer
                Integer i1 = new Integer(s1);
                Integer i2 = new Integer(s2);
                return i1.compareTo(i2);
            }
            return s1.compareTo(s2);
        };
        List<Object> values = idToValue.values().stream().distinct().sorted(sorter).collect(Collectors.toList());
        for (int i = 0; i < values.size(); i++) {
            Color color = Palette.COLORS[i % Palette.COLORS.length];
            colorToModuleFunction.putMapValue(values.get(i).toString(), color);
        }
        style.addVisualMappingFunction(colorToModuleFunction);
        context.ungetService(reference);
    }

    private void updateNodeColorsForNumbers(CyNetworkView view, String attributeName) {
        updateNodeColorsForNumbers(view, attributeName, BasicVisualLexicon.NODE_FILL_COLOR);
    }
    
    @Override
    protected void setEdgeStyleOnAnnotations(CyNetworkView view,
                                             VisualStyle fiVisualStyle,
                                             VisualMappingFunctionFactory visMapFuncFactoryD,
                                             VisualMappingFunctionFactory visMapFuncFactoryC) {
        setEdgeWeights(view, fiVisualStyle, visMapFuncFactoryC, MIN_EDGE_WIDTH, MAX_EDGE_WIDTH);
    }

    public void setEdgeWeights(CyNetworkView view,
                               VisualStyle fiVisualStyle,
                               VisualMappingFunctionFactory visMapFuncFactoryC,
                               double minEdgeWidth,
                               double maxEdgeWidth) {
        double[] edgeWeidghts = getEdgeWeightRange(view);
        ContinuousMapping<Double, Double> edgeWidthFunction = (ContinuousMapping<Double, Double>) visMapFuncFactoryC.createVisualMappingFunction(CONNECTIVITY_NAME, 
                                                                                                                                                 Double.class, 
                                                                                                                                                 BasicVisualLexicon.EDGE_WIDTH);
        // Make sure it is consistent with the node size, which is 0.10
        // 1/100 of the node size
        BoundaryRangeValues<Double> lowerBoundary = new BoundaryRangeValues<Double>(minEdgeWidth, minEdgeWidth, minEdgeWidth);
        // 1/10 of the node size
        BoundaryRangeValues<Double> upperBoundary = new BoundaryRangeValues<Double>(maxEdgeWidth, maxEdgeWidth, maxEdgeWidth);
        edgeWidthFunction.addPoint(edgeWeidghts[0], lowerBoundary);
        edgeWidthFunction.addPoint(edgeWeidghts[1], upperBoundary);
        fiVisualStyle.addVisualMappingFunction(edgeWidthFunction);
    }
    
    private double[] getEdgeWeightRange(CyNetworkView view) {
        CyTable table = view.getModel().getDefaultEdgeTable();
        CyColumn col = table.getColumn(CONNECTIVITY_NAME);
        List<Double> values = col.getValues(Double.class);
        Collections.sort(values);
        return new double[]{values.get(0), values.get(values.size() - 1)};
    }

    @Override
    protected void setDefaultEdgeStyle(VisualStyle fiVisualStyle) {
        super.setDefaultEdgeStyle(fiVisualStyle);
        // 1/100 of node size
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, MIN_EDGE_WIDTH);
    }
    
    

}
