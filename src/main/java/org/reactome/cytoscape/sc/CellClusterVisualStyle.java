package org.reactome.cytoscape.sc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.reactome.cytoscape.service.TableHelper;

/**
 * This style is used for showing the paga cell cluster network.
 * @author wug
 *
 */
public class CellClusterVisualStyle extends SCNetworkVisualStyle {
    private final double MIN_NODE_SIZE = 0.25d;
    private final double MAX_NODE_SIZE = 2.5d;
    private final double MIN_EDGE_WIDTH = MIN_NODE_SIZE / 10.0d;
    private final double MAX_EDGE_WIDTH = MAX_NODE_SIZE / 10.0d;
    
    public CellClusterVisualStyle() {
        styleName = "Cell Cluster Style";
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
    protected void setNodeSizes(CyNetworkView view, 
                                VisualStyle fiVisualStyle,
                                VisualMappingFunctionFactory visMapFuncFactoryC) {
        int[] cellsRange = getCellsRange(view);
        if (cellsRange == null)
            return;
        // Set the node size based on sample number
        ContinuousMapping<Integer, Double> nodeSizeFunc = (ContinuousMapping<Integer, Double>) visMapFuncFactoryC.createVisualMappingFunction("cells", 
                                                                                                                                              Integer.class, 
                                                                                                                                            BasicVisualLexicon.NODE_SIZE);
        BoundaryRangeValues<Double> lowerBoundary = new BoundaryRangeValues<Double>(MIN_NODE_SIZE, MIN_NODE_SIZE, MIN_NODE_SIZE);
        BoundaryRangeValues<Double> upperBoundary = new BoundaryRangeValues<Double>(MAX_NODE_SIZE, MAX_NODE_SIZE, MAX_NODE_SIZE);
        nodeSizeFunc.addPoint(cellsRange[0],
                              lowerBoundary);
        nodeSizeFunc.addPoint(cellsRange[1],
                              upperBoundary);
        fiVisualStyle.addVisualMappingFunction(nodeSizeFunc);
    }
    
    private int[] getCellsRange(CyNetworkView view) {
        Map<Long, Object> idToCells = new TableHelper().getNodeTableValuesBySUID(view.getModel(), 
                                                                                        CELL_NUMBER_NAME, 
                                                                                        Integer.class);
        if (idToCells == null || idToCells.isEmpty())
            return null;
        Set<Object> set = new HashSet<Object>(idToCells.values());
        List<Integer> list = new ArrayList<Integer>();
        for (Object obj : set) {
            list.add((Integer) obj);
        }
        Collections.sort(list);
        Integer min = list.get(0);
        Integer max = list.get(list.size() - 1);
        return new int[]{min, max};
    }

}
