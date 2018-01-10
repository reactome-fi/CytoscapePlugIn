package org.reactome.cytoscape.mechismo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reactome.cytoscape.bn.SimulationComparisonPane;
import org.reactome.mechismo.model.AnalysisResult;
import org.reactome.mechismo.model.CancerType;
import org.reactome.mechismo.model.Reaction;

/**
 * This customized pane is used to display reaction level mechismo output. It customized
 * SimulationComparisonPane for Boolean network modeling.
 * @author wug
 *
 */
public class MechismoReactionPane extends SimulationComparisonPane {
    
    public MechismoReactionPane(String title) {
        super(title);
    }
    
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new MechismoReactionModel();
    }
    
    public void setReactions(List<Reaction> reactions) {
        MechismoReactionModel model = (MechismoReactionModel) contentTable.getModel();
        model.setReactions(reactions);
    }

    private class MechismoReactionModel extends VariableTableModel {
        
        public MechismoReactionModel() {
        }
        
        public void setReactions(List<Reaction> reactions) {
            // Grep all cancer types
            // Using jackson creates many copies of same CancerType. Therefore
            // use String instead.
            List<String> cancerTypes = grepCancerTypes(reactions);
            setUpColumnNames(cancerTypes);
            addValues(reactions, cancerTypes);
            fireTableStructureChanged();
        }
        
        @Override
        public List<Integer> getRowsForSelectedIds(List<Long> selection) {
            List<Integer> rtn = new ArrayList<>();
            for (int i = 0; i < tableData.size(); i++) {
                Object[] row = tableData.get(i);
                Long id = (Long) row[0];
                if (selection.contains(id))
                    rtn.add(i);
            }
            return rtn;
        }
        
        private void addValues(List<Reaction> reactions, List<String> cancerTypes) {
            reactions.sort((rxt1, rxt2) -> rxt1.getId().compareTo(rxt2.getId()));
            tableData.clear();
            reactions.forEach(rxt -> {
                Object[] row = new Object[columnHeaders.length];
                row[0] = rxt.getId();
                row[1] = rxt.getName();
                Map<String, Double> cancerToFDR = getFDRs(rxt.getAnalysisResults());
                for (int i = 0; i < cancerTypes.size(); i++) {
                    Double fdr = cancerToFDR.get(cancerTypes.get(i));
                    row[i + 2] = fdr;
                }
                tableData.add(row);
            });
        }
        
        private Map<String, Double> getFDRs(Set<AnalysisResult> results) {
            Map<String, Double> cancerToFDR = new HashMap<>();
            if (results == null)
                return cancerToFDR;
            results.forEach(result -> cancerToFDR.put(result.getCancerType().getAbbreviation(), result.getFdr()));
            return cancerToFDR;
        }
        
        private void setUpColumnNames(List<String> cancerTypes) {
            columnHeaders = new String[2 + cancerTypes.size()];
            columnHeaders[0] = "ID";
            columnHeaders[1] = "Name";
            for (int i = 0; i < cancerTypes.size(); i++)
                columnHeaders[i + 2] = cancerTypes.get(i);
        }
        
        private List<String> grepCancerTypes(List<Reaction> reactions) {
            Set<String> cancerTypes = new HashSet<>();
            reactions.forEach(reaction -> {
                if (reaction.getAnalysisResults() == null)
                    return;
                reaction.getAnalysisResults().forEach(result -> cancerTypes.add(result.getCancerType().getAbbreviation()));
            });
            List<String> list = new ArrayList<>(cancerTypes);
            Collections.sort(list);
            // Check if pancancer is there
            String pancan = null;
            for (String type : list) {
                if (type.equals("PANCAN")) {
                    pancan = type;
                    break;
                }
            }
            if (pancan != null) {
                list.remove(pancan);
                list.add(0, pancan);
            }
            return list;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return Integer.class;
            if (columnIndex == 1)
                return String.class;
            return Double.class;
        }
        
    }

}
