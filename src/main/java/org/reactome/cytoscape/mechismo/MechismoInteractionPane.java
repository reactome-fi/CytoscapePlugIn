package org.reactome.cytoscape.mechismo;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableRowSorter;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.reactome.cytoscape.bn.VariableSelectionHandler;
import org.reactome.cytoscape.service.PathwayDiagramHighlighter;
import org.reactome.mechismo.model.Interaction;

@SuppressWarnings("serial")
public class MechismoInteractionPane extends MechismoReactionPane {
    
    public static final String TITLE = "Mechismo Interaction";
    
    public MechismoInteractionPane() {
        super(TITLE);
    }
    
    @Override
    protected void doTableSelection(ListSelectionEvent e) {
        // For the time being, follow the default edge table behavior,
        // don't do anything.
    }
    
    private void selectEdgesForSelectedRows() {
        if (view == null || view.getModel() == null)
            return;
        CyTable table = view.getModel().getDefaultEdgeTable();
        if (table == null)
            return;
        // Get the list of selected FIs
        Set<String> selectedFIs = new HashSet<>();
        int[] selectedRows = contentTable.getSelectedRows();
        if (selectedRows != null && selectedRows.length > 0) {
            for (int i = 0; i < selectedRows.length; i++) {
                int modelRow = contentTable.convertRowIndexToModel(selectedRows[i]);
                String fi = (String) contentTable.getModel().getValueAt(modelRow, 0);
                selectedFIs.add(fi);
            }
        }
        view.getEdgeViews().forEach(edgeView -> {
            CyRow row = table.getRow(edgeView.getModel().getSUID());
            String edgeName = row.get("name", String.class);
            row.set("selected", selectedFIs.contains(edgeName));
        });
    }
    
    @Override
    protected void doContentTablePopup(MouseEvent e) {
        JPopupMenu popup = createExportAnnotationPopup();
        popup.addSeparator();
        JMenuItem selectItem = new JMenuItem("Selected edges for selected rows");
        selectItem.addActionListener(actionEvent -> selectEdgesForSelectedRows());
        popup.add(selectItem);
        
        popup.show(contentTable, 
                   e.getX(), 
                   e.getY());
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
        if (view == null || view.getModel() == null)
            return;
        PathwayDiagramHighlighter highlighter = new PathwayDiagramHighlighter();
        highlighter.setMinColor(Color.YELLOW);
        highlighter.setMaxColor(Color.MAGENTA);
        MechismoInteractionModel model = (MechismoInteractionModel) contentTable.getModel();
        double[] minMax = model.getMinMaxFDR(column);
        Map<String, Double> fiToFDR = model.getFIToFDR(column);
        // Need to check this code
        CyTable table = view.getModel().getDefaultEdgeTable();
        view.getEdgeViews().forEach(edgeView -> {
            String fi = table.getRow(edgeView.getModel().getSUID()).get("name", String.class);
            Double fdr = fiToFDR.get(fi);
            Color lineColor = null;
            if (fdr == null) {
                if (fiToFDR.containsKey(fi))
                    lineColor = Color.BLACK; // Means there is mechismo information
                else
                    lineColor = Color.LIGHT_GRAY; // Used as the background
            }
            else
                lineColor = highlighter.getColor(fdr, minMax[0], minMax[1]);
            edgeView.setVisualProperty(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, lineColor);
        });
        // Use white background for nodes so that we can see all
        view.getNodeViews().forEach(nodeView -> nodeView.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE));
        view.updateView();
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
        
        public Map<String, Double> getFIToFDR(int column) {
            if (column == 0)
                return null;
            Map<String, Double> fiToFDR = new HashMap<>();
            for (int i = 0; i < getRowCount(); i++) {
                String fi = (String) getValueAt(i, 0);
                Double value = (Double) getValueAt(i, column);
                fiToFDR.put(fi, value);
            }
            return fiToFDR;
        }
        
        public double[] getMinMaxFDR(int column) {
            if (column == 0)
                return null;
            Double min = Double.POSITIVE_INFINITY;
            Double max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < getRowCount(); i++) {
                Double value = (Double) getValueAt(i, column);
                if (value == null)
                    continue;
                if (value > max)
                    max = value;
                if (value < min)
                    min = value;
            }
            // Min and max may be the same
            return new double[]{min, max}; 
        }
        
    }

}
