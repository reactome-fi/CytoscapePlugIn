package org.reactome.cytoscape.mechismo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.reactome.cytoscape.bn.VariableSelectionHandler;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.mechismo.model.Interaction;

@SuppressWarnings("serial")
public class MechismoInteractionPane extends MechismoReactionPane {
    
    public static final String TITLE = "Mechismo Interaction";
    
    public MechismoInteractionPane() {
        super(TITLE);
    }
    
    @Override
    protected void handleTableSelection() {
    }
    
    @Override
    protected String getDataType() {
        return "interactions";
    }
    
    @Override
    protected void _handleNetworkSelection() {
        CyNetwork network = view.getModel();
        List<CyEdge> selectedEdges = CyTableUtil.getEdgesInState(network,
                                                                 CyNetwork.SELECTED,
                                                                 true);
        CyTable table = network.getDefaultEdgeTable();
        Set<String> edgeNames = selectedEdges.stream()
                                             .map(edge -> table.getRow(edge.getSUID()).get("name", String.class))
                                             .collect(Collectors.toSet());
        doFIFilter(edgeNames);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void doFIFilter(final Set<String> edgeNames) {
        // Synchronize network views and FIs displayed in the table
        RowFilter<MechismoInteractionModel, Object> filter = new RowFilter<MechismoInteractionModel, Object>() {
            public boolean include(Entry<? extends MechismoInteractionModel, ? extends Object> entry) {
                String name = entry.getStringValue(0);
                if (edgeNames == null || edgeNames.size() == 0)
                    return true;
                return edgeNames.contains(name);
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
            Boolean isVisible = edgeView.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE);
            if (isVisible)
                names.add(name);
        });
        return names;
    }

    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new MechismoInteractionModel();
    }
    
    @Override
    protected VariableSelectionHandler createSelectionHandler() {
        VariableSelectionHandler handler = new VariableSelectionHandler() {

            @Override
            public void setSelection(List selection) {
            }

            @Override
            public List getSelection() {
                return new ArrayList<>();
            }
            
        };
        return handler;
    }

    public void setInteractions(List<Interaction> interactions) {
        MechismoInteractionModel model = (MechismoInteractionModel) contentTable.getModel();
        model.setInteractions(interactions);
        fillCancerTypes(model, 1);
    }
    
    /**
     * We just borrow this super-class method to highlight displayed network. So don't
     * confuse the name.
     */
    @Override
    protected void hilitePathway(int column) {
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
        
        public List<Integer> getRowsForFIs(Set<String> fis) {
            List<Integer> rows = new ArrayList<>();
            for (int i = 0; i < getRowCount(); i++) {
                String fi = (String) getValueAt(i, 0);
                if (fis.contains(fi))
                    rows.add(i);
            }
            return rows;
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
