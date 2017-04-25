/*
 * Created on Apr 25, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.util.List;

/**
 * @author gwu
 *
 */
public interface VariableTableModelInterface {
    
    public List<Integer> getRowsForSelectedIds(List<Long> selection);
    
}
