package org.reactome.cytoscape.sc;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.pathway.EventTreePane;
import org.reactome.cytoscape.pathway.PathwayEnrichmentResultPane;
import org.reactome.cytoscape.sc.utils.ScPathwayMethod;
import org.reactome.cytoscape.util.PlugInUtilities;

@SuppressWarnings("serial")
public class PathwayANOVAResultPane extends PathwayEnrichmentResultPane {

    private JLabel summaryLabel;
    private ScPathwayMethod method;
    
    public PathwayANOVAResultPane(EventTreePane eventTreePane, String title) {
        super(eventTreePane, title);
        // Need to modify the control bars
        controlToolBar.removeAll();
        summaryLabel = new JLabel("Pathway ANOVA"); // Just a place hold
        controlToolBar.add(summaryLabel);
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        // Cell renderer for double
        TableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                // Convert a double into a string for format
                Double doubleValue = (Double) value;
                String formatted = PlugInUtilities.formatProbability(doubleValue);
                return super.getTableCellRendererComponent(table, formatted, isSelected, hasFocus, row, column);
            }
        };
        contentTable.setDefaultRenderer(Double.class, renderer);
        if (eventTreePane != null)
            eventTreePane.setAnnotationPane(this);
    }

    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new PathwayANOVATableModel();
    }
    
    @Override
    protected void doContentTablePopup(MouseEvent e) {
        JPopupMenu popupMenu = createExportAnnotationPopup();
        createDiagramMenuItem(popupMenu);
        
        int[] selectedRows = contentTable.getSelectedRows();
        if (selectedRows != null && selectedRows.length == 1) {
            JMenuItem item = new JMenuItem("View Pathway Activities");
            item.addActionListener(e1 -> {
                int modelIndex = contentTable.convertRowIndexToModel(selectedRows[0]);
                TableModel tableModel = contentTable.getModel();
                String pathway = (String) tableModel.getValueAt(modelIndex, 0);
                ScNetworkManager.getManager().viewPathwayActivities(method, pathway);
            });
            popupMenu.add(item);
        }
        popupMenu.show(contentTable, e.getX(), e.getY());
    }

    public void setResults(ScPathwayMethod method,
                           Map<String, Map<String, Double>> results) {
        summaryLabel.setText("Pathway ANOVA results based on " + method + " (*1/F: Inverse of F after scaled by the minimum of F)");
        
        PathwayANOVATableModel model = (PathwayANOVATableModel) contentTable.getModel();
        model.setResults(results);
        if (eventTreePane != null) {
            List<GeneSetAnnotation> annotations = model.convertToAnnotation();
            eventTreePane.setHighlightDataType("1/F");
            eventTreePane.showPathwayEnrichments(annotations, false);
        }
        
        // Sort based on the FDR, p-value, F and then pathway name
        List<SortKey> sortedKeys = new ArrayList<>();
        sortedKeys.add(new RowSorter.SortKey(4, SortOrder.ASCENDING));
        sortedKeys.add(new RowSorter.SortKey(3, SortOrder.ASCENDING));
        sortedKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
        sortedKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        contentTable.getRowSorter().setSortKeys(sortedKeys);
        
        this.method = method;
    }
    
    @Override
    protected TableRowSorter<NetworkModuleTableModel> createTableRowSorter(NetworkModuleTableModel model) {
        return new TableRowSorter<NetworkModuleTableModel>(model);
    }
    
    private class PathwayANOVATableModel extends AnnotationTableModel {
        
        private String[] headers = new String[] {
                "ReactomePathway",
                "F",
                "1/F",
                "P-value (PR(>F))",
                "FDR (fdr_bh)"
        };
        
        public PathwayANOVATableModel() {
            super.columnHeaders = headers;
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return String.class;
            return Double.class;
        }
        
        public void setResults(Map<String, Map<String, Double>> results) {
            tableData.clear();
            results.forEach((p, v) -> {
                Object[] row = new Object[headers.length];
                tableData.add(row);
                row[0] = p;
                row[1] = v.get("F");
                row[3] = v.get("PR(>F)");
                row[4] = v.get("fdr_bh");
            });
            double minF = tableData.stream().map(row -> (Double)row[1]).collect(Collectors.minBy(Comparator.naturalOrder())).get();
            tableData.forEach(row -> row[2] = minF / (Double) row[1]);
            fireTableDataChanged();
        }
        
        public List<GeneSetAnnotation> convertToAnnotation() {
            List<GeneSetAnnotation> annotations = new ArrayList<>();
            for (Object[] row : tableData) {
                GeneSetAnnotation annotation = new GeneSetAnnotation();
                annotation.setTopic((String)row[0]);
                annotation.setPValue((Double)row[3]);
                annotation.setFdr(row[2] + ""); // Use 1/F value
                annotations.add(annotation);
            }
            return annotations;
        }
    }
    
}
