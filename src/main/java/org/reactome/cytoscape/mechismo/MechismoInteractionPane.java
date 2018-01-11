package org.reactome.cytoscape.mechismo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reactome.mechismo.model.Interaction;

//TODO: Need to check why FIs are stored into the database, but no analysis results are attached.
public class MechismoInteractionPane extends MechismoReactionPane {
    
    public static final String TITLE = "Mechismo Interaction";
    
    public MechismoInteractionPane() {
        super(TITLE);
    }
    
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new MechismoInteractionModel();
    }
    
    public void setInteractions(List<Interaction> interactions) {
        MechismoInteractionModel model = (MechismoInteractionModel) contentTable.getModel();
        model.setInteractions(interactions);
    }
    
    private class MechismoInteractionModel extends MechismoReactionModel {
        
        public MechismoInteractionModel() {
            
        }
        
        public void setInteractions(List<Interaction> interactions) {
            List<String> cancerTypes = grepCancerTypes(interactions);
            setUpColumnNames(cancerTypes, false);
            addValues(interactions, cancerTypes);
            fireTableStructureChanged();
        }
        
        private void addValues(List<Interaction> interactions, List<String> cancerTypes) {
            interactions.sort((i1, i2) -> i1.getName().compareTo(i2.getName()));
            tableData.clear();
            interactions.forEach(interaction -> {
                Object[] row = new Object[columnHeaders.length];
                int start = 0;
                row[start ++] = interaction.getName();
                Map<String, Double> cancerToFDR = getFDRs(interaction.getAnalysisResults());
                for (int i = 0; i < cancerTypes.size(); i++) {
                    Double fdr = cancerToFDR.get(cancerTypes.get(i));
                    row[start ++] = fdr;
                }
                tableData.add(row);
            });
        }
        
        private List<String> grepCancerTypes(List<Interaction> interactions) {
            Set<String> cancerTypes = new HashSet<>();
            interactions.forEach(interaction -> {
                if (interaction.getAnalysisResults() == null)
                    return;
                interaction.getAnalysisResults().forEach(result -> cancerTypes.add(result.getCancerType().getAbbreviation()));
            });
            return resortPanCancer(cancerTypes);
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return String.class;
            return Double.class;
        }
        
    }

}
