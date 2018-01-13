package org.reactome.cytoscape.mechismo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.mechismo.model.Interaction;

//TODO: Need to check why FIs are stored into the database, but no analysis results are attached.
public class MechismoInteractionPane extends MechismoReactionPane {
    
    public static final String TITLE = "Mechismo Interaction";
    private PropertyChangeListener viewUpdateListener;
    
    public MechismoInteractionPane() {
        super(TITLE);
    }
    
    @Override
    protected void modifyContentPane() {
        super.modifyContentPane();
        viewUpdateListener = new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                doFIFilter();
            }
        };
        PlugInObjectManager.getManager().addPropetyChangeListener(viewUpdateListener);
    }
    
    @Override
    public void close() {
        super.close();
        PlugInObjectManager.getManager().removePropertyChangeLisener(viewUpdateListener);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void doFIFilter() {
        // Synchronize network views and FIs displayed in the table
        RowFilter<MechismoInteractionModel, Object> filter = new RowFilter<MechismoInteractionModel, Object>() {
            Set<String> displayedFIs = getDisplayedFIs();
            public boolean include(Entry<? extends MechismoInteractionModel, ? extends Object> entry) {
                String name = entry.getStringValue(0);
                return displayedFIs.contains(name);
            }
        };
        TableRowSorter sorter = (TableRowSorter) contentTable.getRowSorter();
        sorter.setRowFilter(filter);
    }
    
    private Set<String> getDisplayedFIs() {
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        Set<String> names = new HashSet<>();
        CyTable table = view.getModel().getDefaultEdgeTable();
        view.getEdgeViews().forEach(edgeView -> {
            String name = table.getRow(edgeView.getModel().getSUID()).get("name", String.class);
            Boolean isVisiuble = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE);
            if (isVisiuble)
                names.add(name);
        });
        return names;
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
                row[start ++] = interaction.getName().replace("\t", " (FI) "); // So that it is the same as the original
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
