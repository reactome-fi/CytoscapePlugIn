package org.reactome.cytoscape.sc;

import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.reactome.cytoscape.service.FIVisualStyleImpl;
import org.reactome.cytoscape.service.PathwaySpecies;

public class RegulatoryNetworkStyle extends FIVisualStyleImpl {
    
    public RegulatoryNetworkStyle() {
        styleName = "Regulatory Gene Network Style";
    }
    
//    @Override
//    protected void setEdgeStyleOnAnnotations(CyNetworkView view,
//                                             VisualStyle fiVisualStyle,
//                                             VisualMappingFunctionFactory visMapFuncFactoryD,
//                                             VisualMappingFunctionFactory visMapFuncFactoryC) {
//        // Turn off this for the time being since we want to display the same weights for pos/neg cor, which is difficult.
////        PlugInUtilities.setEdgeWeights(view, fiVisualStyle, visMapFuncFactoryC, "correlation", MIN_EDGE_WIDTH, MAX_EDGE_WIDTH);
//    }
    
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
    
    protected void displayNodeType(VisualStyle style,
                                   VisualMappingFunctionFactory visMapFuncFactoryD) {
        super.displayNodeType(style, visMapFuncFactoryD);
        
        // Set the node shape based on type
        // Currently two types are supported: Gene and Drug
        DiscreteMapping nodeTypeShape = (DiscreteMapping) visMapFuncFactoryD.createVisualMappingFunction("transcription factor", 
                                                                                                         Boolean.class,
                                                                                                         BasicVisualLexicon.NODE_SHAPE);
        nodeTypeShape.putMapValue(Boolean.TRUE, 
                                  NodeShapeVisualProperty.DIAMOND); 
        nodeTypeShape.putMapValue(Boolean.FALSE, 
                                  NodeShapeVisualProperty.ELLIPSE);
        style.addVisualMappingFunction(nodeTypeShape);
    }
    
}
