package org.reactome.cytoscape.sc.diff;

import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.reactome.cytoscape.sc.JSONServerCaller;
import org.reactome.cytoscape.service.PathwayEnrichmentApproach;

@SuppressWarnings("serial")
public class ClusterGenesDialog extends DiffExpResultDialog {
    private JComboBox<Integer> clusterBox;
    
    public ClusterGenesDialog() {
    }
    
    protected ClusterGenesDialog(Window parent) {
        super(parent);
    }
    
    public static void main(String[] args) throws Exception {
        ClusterGenesDialog dialog = new ClusterGenesDialog(null);
        dialog.setSize(1300, 500);
        JSONServerCaller caller = new JSONServerCaller();
        List<List<String>> genes = caller.rankVelocityGenes();
        dialog.setClusterGenes(genes);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }
    
    @Override
    public DiffExpResult getDisplayedResult() {
        DiffExpResult result = new DiffExpResult();
        List<String> genes = new ArrayList<>();
        result.setNames(genes);
        ClusterGenesTableModel model = (ClusterGenesTableModel) resultTable.getModel();
        Integer geneCol = (Integer) clusterBox.getSelectedItem();
        for (int i = 0; i < model.getRowCount(); i++)
            genes.add((String)model.getValueAt(i, geneCol + 1));
        result.setResultName("Cluster" + geneCol + " Velocity Genes");
        result.setGeneListOnly(true);
        return result;
    }
    
    @Override
    protected TableModel createTableModel() {
        return new ClusterGenesTableModel();
    }
    
    @Override
    protected JPanel createFilterPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 1));
        JLabel label = new JLabel("Filter rows for gene: ");
        JComboBox<String> filterBox = new JComboBox<>();
        filterBox.addItem("equals");
        filterBox.addItem("contains");
        JTextField filterTF = new JTextField();
        filterTF.setColumns(8);
        JButton filterBtn = new JButton("Filter");
        panel.add(label);
        panel.add(filterBox);
        panel.add(filterTF);
        panel.add(filterBtn);
        ActionListener l = e -> doFilter(filterBox.getSelectedItem().toString(),
                                         filterTF.getText().trim());
        filterTF.addActionListener(l);
        filterBox.addActionListener(l);
        return panel;
    }
    
    @Override
    protected JPanel createControlPane() {
        JPanel controlPane = super.createControlPane();
        // Use Binomial test only
        pathwayBox.removeAllItems();
        pathwayBox.addItem(PathwayEnrichmentApproach.Binomial_Test);
        // Do some modification
        controlPane.remove(closeBtn);
        JLabel label = new JLabel("for cluster");
        clusterBox = new JComboBox<>();
        controlPane.add(label);
        controlPane.add(clusterBox);
        // Add this button back
        controlPane.add(closeBtn);
        return controlPane;
    }

    private void doFilter(String operator,
                          String key) {
        RowFilter<TableModel, Object> rowFilter = null;
        if (key.length() > 0) {
            rowFilter = new RowFilter<TableModel, Object>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                    // Go over all genes
                    boolean rtn = false;
                    for (int i = 1; i < entry.getValueCount(); i++) {
                        String value = (String) entry.getValue(i);
                        if (operator.equals("equals"))
                            rtn = value.equalsIgnoreCase(key);
                        if (operator.equals("contains"))
                            rtn = value.toLowerCase().contains(key);
                        if (rtn)
                            break;
                    }
                    return rtn;
                }
            };
        }
        @SuppressWarnings("unchecked")
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) resultTable.getRowSorter();
        sorter.setRowFilter(rowFilter);
        totalGeneLabel.setText("Total displayed rows: " + resultTable.getRowCount());
    }

    public void setClusterGenes(List<List<String>> genes) {
        ClusterGenesTableModel model = (ClusterGenesTableModel) resultTable.getModel();
        model.setGenes(genes);
        totalGeneLabel.setText("Total displayed rows: " + resultTable.getRowCount());
        clusterBox.removeAllItems();
        IntStream.range(0, genes.get(0).size()).forEach(i -> clusterBox.addItem(i));
    }

    private class ClusterGenesTableModel extends AbstractTableModel {
        private List<List<String>> genes;
        private List<String> colNames;
        
        public ClusterGenesTableModel() {
            genes = new ArrayList<>();
            colNames = new ArrayList<>();
        }
        
        public void setGenes(List<List<String>> genes) {
            this.genes.clear();
            colNames.clear();
            this.genes.addAll(genes);
            // Peak the total number of clusters
            int clusterSize = genes.get(0).size();
            for (int i = 0; i < clusterSize; i++)
                colNames.add("cluster" + i);
            fireTableStructureChanged();
        }
 
        @Override
        public String getColumnName(int column) {
            if (column == 0)
                return "Rank";
            return colNames.get(column - 1);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return Integer.class;
            return String.class;
        }

        @Override
        public int getRowCount() {
            return genes.size();
        }

        @Override
        public int getColumnCount() {
            return colNames.size() + 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0)
                return rowIndex;
            return genes.get(rowIndex).get(columnIndex - 1);
        }
    }

}
