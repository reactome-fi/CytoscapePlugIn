package org.reactome.cytoscape.sc;

import java.awt.Color;
import java.awt.Paint;
import java.util.List;
import java.util.stream.Collectors;

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
import org.reactome.cytoscape.service.FIVisualStyleImpl;
import org.reactome.cytoscape.service.PathwaySpecies;

public class RegulatoryNetworkStyle extends FIVisualStyleImpl {
    public static final String CORRELATION_COL_NAME = "correlation";
    public static final String CORRELATION_PVALUE_COL_NAME = "corr_pvalue";
    public static final String TF_COL_NAME = "transcription factor";
    
    private final double MIN_EDGE_WEIGHT = 0.5d;
    private final double MAX_EDGE_WEIGHT = 5.0d;
    // Color scheme based on default Cytoscape
    private final Color MIN_COLOR = new Color(67, 147, 195);
    private final Color MID_COLOR = new Color(247, 247, 247);
    private final Color MAX_COLOR = new Color(214, 96, 77);
    
    public RegulatoryNetworkStyle() {
        styleName = "Regulatory Network Style";
    }
    
    @Override
    protected void setEdgeStyleOnAnnotations(CyNetworkView view,
                                             VisualStyle fiVisualStyle,
                                             VisualMappingFunctionFactory visMapFuncFactoryD,
                                             VisualMappingFunctionFactory visMapFuncFactoryC) {
        super.setEdgeStyleOnAnnotations(view, fiVisualStyle, visMapFuncFactoryD, visMapFuncFactoryC);
        double[] minMaxCor = getCorRange(view);
        // Edge widths based on correlation
        ContinuousMapping<Double, Double> edgeWidthFunction = (ContinuousMapping<Double, Double>) visMapFuncFactoryC.createVisualMappingFunction(CORRELATION_COL_NAME, 
                                                                                                                                                 Double.class, 
                                                                                                                                                 BasicVisualLexicon.EDGE_WIDTH);
        BoundaryRangeValues<Double> lowerBoundary = new BoundaryRangeValues<Double>(MAX_EDGE_WEIGHT, MAX_EDGE_WEIGHT, MAX_EDGE_WEIGHT);
        BoundaryRangeValues<Double> middle = new BoundaryRangeValues<Double>(MIN_EDGE_WEIGHT, MIN_EDGE_WEIGHT, MIN_EDGE_WEIGHT);
        BoundaryRangeValues<Double> upperBoundary = new BoundaryRangeValues<Double>(MAX_EDGE_WEIGHT, MAX_EDGE_WEIGHT, MAX_EDGE_WEIGHT);
        edgeWidthFunction.addPoint(-minMaxCor[1], lowerBoundary);
        edgeWidthFunction.addPoint(-minMaxCor[0], middle);
        edgeWidthFunction.addPoint(minMaxCor[0], middle);
        edgeWidthFunction.addPoint(minMaxCor[1], upperBoundary);
        fiVisualStyle.addVisualMappingFunction(edgeWidthFunction);
        // Edge colors based on correlation
        ContinuousMapping<Double, Paint> edgeColorFunction = (ContinuousMapping<Double, Paint>) visMapFuncFactoryC.createVisualMappingFunction(CORRELATION_COL_NAME, 
                                                                                                                                               Double.class, 
                                                                                                                                               BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
        BoundaryRangeValues<Paint> lowerColor = new BoundaryRangeValues<Paint>(MIN_COLOR, MIN_COLOR, MIN_COLOR);
        BoundaryRangeValues<Paint> middleColor = new BoundaryRangeValues<Paint>(MID_COLOR, MID_COLOR, MID_COLOR);
        BoundaryRangeValues<Paint> upperColor = new BoundaryRangeValues<Paint>(MAX_COLOR, MAX_COLOR, MAX_COLOR);
        edgeColorFunction.addPoint(-minMaxCor[1], lowerColor);
        edgeColorFunction.addPoint(-minMaxCor[0], middleColor);
        edgeColorFunction.addPoint(minMaxCor[0], middleColor);
        edgeColorFunction.addPoint(minMaxCor[1], upperColor);
        fiVisualStyle.addVisualMappingFunction(edgeColorFunction);
    }

    private double[] getCorRange(CyNetworkView view) {
        CyTable table = view.getModel().getDefaultEdgeTable();
        CyColumn col = table.getColumn(CORRELATION_COL_NAME);
        List<Double> values = col.getValues(Double.class);
        values = values.stream().map(Math::abs).sorted().collect(Collectors.toList());
        return new double[]{values.get(0), values.get(values.size() - 1)};
    }
    
    @Override
    protected void setDefaultNodeStyle(VisualStyle style,
                                       VisualMappingFunctionFactory func) {
        super.setDefaultNodeStyle(style, func);
        if (ScNetworkManager.getManager().getSpecies() == PathwaySpecies.Mus_musculus) {
            PassthroughMapping<String, String> mapping = (PassthroughMapping<String, String>) func.createVisualMappingFunction("mouseGenes",
                                                                                                                               String.class, 
                                                                                                                               BasicVisualLexicon.NODE_LABEL);
            style.addVisualMappingFunction(mapping);
        }
        style.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 3.0d);
        style.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 40.0d);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void displayNodeType(VisualStyle style,
                                   VisualMappingFunctionFactory visMapFuncFactoryD) {
        super.displayNodeType(style, visMapFuncFactoryD);
        
        // Set the node shape based on type
        // Currently two types are supported: Gene and Drug
        DiscreteMapping nodeTypeShape = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction(TF_COL_NAME, 
                                                                                                         Boolean.class,
                                                                                                         BasicVisualLexicon.NODE_SHAPE);
        nodeTypeShape.putMapValue(Boolean.TRUE, 
                                  NodeShapeVisualProperty.DIAMOND); 
        nodeTypeShape.putMapValue(Boolean.FALSE, 
                                  NodeShapeVisualProperty.ELLIPSE);
        style.addVisualMappingFunction(nodeTypeShape);
    }
    
}
