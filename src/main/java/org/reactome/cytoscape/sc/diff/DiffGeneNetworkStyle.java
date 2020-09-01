package org.reactome.cytoscape.sc.diff;

import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.reactome.cytoscape.sc.ScNetworkManager;
import org.reactome.cytoscape.service.FIVisualStyleImpl;
import org.reactome.cytoscape.service.PathwaySpecies;

public class DiffGeneNetworkStyle extends FIVisualStyleImpl {
    
    public DiffGeneNetworkStyle() {
        styleName = "Diff Gene Network Style";
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
        style.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 2.0d);
        style.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 40.0d);
    }
    
}
