/*
 * Created on Mar 13, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Paint;

import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.FIVisualStyleImpl;
import org.reactome.cytoscape.service.PathwayDiagramHighlighter;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * Customied FIVisualSytle to color nodes based on genes' IPA values.
 * @author gwu
 */
public class PGMFIVisualStyle extends FIVisualStyleImpl {
    
    /**
     * Default constructor.
     */
    public PGMFIVisualStyle() {
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void handleNodeHighlight(VisualStyle fiVisualStyle,
                                    VisualMappingFunctionFactory visMapFuncFactoryD,
                                    VisualMappingFunctionFactory visMapFuncFactoryC) {
        // Set two colors and values
        double[] minMaxValue = PlugInObjectManager.getManager().getMinMaxColorValues();
        PathwayDiagramHighlighter highlighter = new PathwayDiagramHighlighter();
        // Just use node filling color though the highlight will be overwritten after network clustering.
        // Node border paint is not good for this purpose.
        ContinuousMapping mapping = (ContinuousMapping) visMapFuncFactoryC.createVisualMappingFunction(FIVisualStyle.GENE_VALUE_ATT, 
                                                                                                       Double.class,
                                                                                                       BasicVisualLexicon.NODE_FILL_COLOR);
        
        BoundaryRangeValues<Paint> brv1 = new BoundaryRangeValues<Paint>(highlighter.getMinColor(), highlighter.getMinColor(), highlighter.getMinColor());
        BoundaryRangeValues<Paint> brv2 = new BoundaryRangeValues<Paint>(highlighter.getMaxColor(), highlighter.getMaxColor(), highlighter.getMaxColor());
        mapping.addPoint(minMaxValue[0], brv1);
        mapping.addPoint(minMaxValue[1], brv2);
        fiVisualStyle.addVisualMappingFunction(mapping);
        // Just give it a little transparency for better view.
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.NODE_TRANSPARENCY, 225);
    }
    
}
