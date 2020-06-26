package org.reactome.cytoscape.sc;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
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
    public static final String CELL_NUMBER_NAME = "cells";
    // This is the key used in scanpy
    public static final String DPT_NAME = "dpt_pseudotime";
    // For cluster ids
    public static final String CLUSTER_NODE_PREFIX = "cluster";
    private final double DEFAULT_NODE_SIZE = 0.10d;
    private final double MIN_EDGE_WIDTH = DEFAULT_NODE_SIZE / 100.0d;
    private final double MAX_EDGE_WIDTH = DEFAULT_NODE_SIZE / 10.0d;
    // From yellow to blue
    private final Color MIN_VALUE_COLOR = Color.YELLOW;
    private final Color MAX_VALUE_COLOR = Color.BLUE;
    
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
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_BORDER_PAINT,
                                      Color.LIGHT_GRAY);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.black);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_PAINT, Color.cyan);
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_TRANSPARENCY, 100);
        // Don't want to have any label for performance
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_LABEL, null);
        String toolTipAttrName = "nodeToolTip";
        PassthroughMapping<String, String> toolTipFunction = (PassthroughMapping<String, String>) visMapFuncFactoryP.createVisualMappingFunction(toolTipAttrName, 
                                                                                                                 String.class, 
                                                                                                                 BasicVisualLexicon.NODE_TOOLTIP);
        fiVisualStyle.addVisualMappingFunction(toolTipFunction);
        // Really small nodes
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_SIZE, DEFAULT_NODE_SIZE);
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
        Number[] range = getValueRange(view, attributeName);
        if (range == null)
            return; // Do nothing
        VisualStyle style = getVisualStyle();
        if (style == null)
            return;
        ServiceReference referenceC = getVisualMappingFunctionFactorServiceReference("(mapping.type=continuous)");
        if (referenceC == null) {
            return ;
        }
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        VisualMappingFunctionFactory visMapFuncFactoryC = (VisualMappingFunctionFactory) context.getService(referenceC);
        ContinuousMapping<Number, Paint> colorFunc = (ContinuousMapping<Number, Paint>) visMapFuncFactoryC.createVisualMappingFunction(attributeName,
                                                                                                                                       Number.class,
                                                                                                                                       BasicVisualLexicon.NODE_FILL_COLOR);
        BoundaryRangeValues<Paint> lowerBoundary = new BoundaryRangeValues<Paint>(
                MIN_VALUE_COLOR, MIN_VALUE_COLOR, MIN_VALUE_COLOR);
        BoundaryRangeValues<Paint> upperBoundary = new BoundaryRangeValues<Paint>(
                MAX_VALUE_COLOR, MAX_VALUE_COLOR, MAX_VALUE_COLOR);
        colorFunc.addPoint(range[0], lowerBoundary);
        colorFunc.addPoint(range[1], upperBoundary);
        style.addVisualMappingFunction(colorFunc);
        context.ungetService(referenceC);
        view.updateView();
    }
    
    private VisualStyle getVisualStyle() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(VisualMappingManager.class.getName());
        VisualMappingManager visMapManager = (VisualMappingManager) context.getService(reference);
        Optional<VisualStyle> found = visMapManager.getAllVisualStyles()
                                                   .stream()
                                                   .filter(v -> v.getTitle().equals(styleName))
                                                   .findAny();
        context.ungetService(reference);
        return found.isPresent() ? found.get() : null;
    }
    
    private Number[] getValueRange(CyNetworkView view,
                                   String attributeName) {
        Map<Long, Object> idToValue = new TableHelper().getNodeTableValuesBySUID(view.getModel(), 
                                                                                 attributeName, 
                                                                                 Number.class);
        if (idToValue == null || idToValue.isEmpty())
            return null;
        Set<Object> set = new HashSet<Object>(idToValue.values());
        List<Double> list = new ArrayList<>();
        for (Object obj : set) {
            list.add(((Number)obj).doubleValue()); // Regardless we should be able to use double
        }
        Collections.sort(list);
        return new Number[]{list.get(0), list.get(list.size() - 1)};
    }

    @Override
    protected void setEdgeStyleOnAnnotations(VisualStyle fiVisualStyle,
                                             VisualMappingFunctionFactory visMapFuncFactoryD,
                                             VisualMappingFunctionFactory visMapFuncFactoryC) {
        ContinuousMapping<Double, Double> edgeWidthFunction = (ContinuousMapping<Double, Double>) visMapFuncFactoryC.createVisualMappingFunction(CONNECTIVITY_NAME, 
                                                                                                                                                 Double.class, 
                                                                                                                                                 BasicVisualLexicon.EDGE_WIDTH);
        // Make sure it is consistent with the node size, which is 0.10
        // 1/100 of the node size
        BoundaryRangeValues<Double> lowerBoundary = new BoundaryRangeValues<Double>(MIN_EDGE_WIDTH, MIN_EDGE_WIDTH, MIN_EDGE_WIDTH);
        // 1/10 of the node size
        BoundaryRangeValues<Double> upperBoundary = new BoundaryRangeValues<Double>(MAX_EDGE_WIDTH, MAX_EDGE_WIDTH, MAX_EDGE_WIDTH);
        edgeWidthFunction.addPoint(new Double(0.0d), lowerBoundary);
        edgeWidthFunction.addPoint(new Double(1.0d), upperBoundary);
        fiVisualStyle.addVisualMappingFunction(edgeWidthFunction);
    }

    @Override
    protected void setDefaultEdgeStyle(VisualStyle fiVisualStyle) {
        super.setDefaultEdgeStyle(fiVisualStyle);
        // 1/100 of node size
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, MIN_EDGE_WIDTH);
    }
    
    

}
