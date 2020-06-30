package org.reactome.cytoscape.sc;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.math3.util.Pair;
import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This class is used to perform differential expression analysis among cell clusters or other
 * defined cell groups.
 * @author wug
 *
 */
@SuppressWarnings("serial")
public class DifferentialExpressionAnalyzer {
    
    public DifferentialExpressionAnalyzer() {
        
    }
    
    /**
     * Get the selected groups. Thought the passed parameter is a list of Integer, the returned
     * Pair object is for String, since we need to make it possible to use "rest" as the reference.
     * @param groups
     * @return
     */
    public Pair<String, String> getSelectedClusters(List<Integer> clusters) {
        List<String> groups = clusters.stream().map(c -> c.toString()).collect(Collectors.toList());
        GroupSelectionDialog selectionDialog = new GroupSelectionDialog(groups);
        if (!selectionDialog.isOkCliked)
            return null;
        Pair<String, String> selected = selectionDialog.getSelected();
        return selected;
    }
    
    /**
     * Select 
     * @param result
     * @return
     */
    public void displayResult(DifferentialExpressionResult result,
                              Pair<String, String> pair) {
        DiffExpResultDialog dialog = new DiffExpResultDialog();
        dialog.setResult(result);
        dialog.setTitle("Differential Expression Analysis: " + pair.getFirst() + " vs. " + pair.getSecond());
        dialog.setVisible(true);
    }
    
    private class DiffExpResultDialog extends JDialog {
        private JTable resultTable;
        private JLabel totalGeneLabel;
        
        public DiffExpResultDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        private void init() {
            setTitle("Differential Expression Analysis Result");
            
            DiffExpResultTableModel model = new DiffExpResultTableModel();
            resultTable = new JTable(model);
            resultTable.setAutoCreateRowSorter(true);
            JPanel contentPane = new JPanel();
            contentPane.setLayout(new BorderLayout());
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.add(new JScrollPane(resultTable), BorderLayout.CENTER);
            totalGeneLabel = new JLabel("Total genes:");
            totalGeneLabel.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 0));
            contentPane.add(totalGeneLabel, BorderLayout.SOUTH);
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            JPanel filterPane = createFilterPane(resultTable);
            getContentPane().add(filterPane, BorderLayout.NORTH);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.setBorder(BorderFactory.createEtchedBorder());
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            controlPane.getOKBtn().addActionListener(e -> dispose());
            controlPane.getCancelBtn().addActionListener(e -> dispose());
            
            setSize(800, 650);
            setModal(false);
            setLocationRelativeTo(getOwner());
        }
        
        private JPanel createFilterPane(JTable resultTable) {
            // We need a container for filters since we may have more than one
            JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            container.setBorder(BorderFactory.createEtchedBorder());
            DiffExpResultTableFilterPane filterPane = new DiffExpResultTableFilterPane(resultTable);
            container.add(filterPane);
            filterPane.addPropertyChangeListener(e -> {
                if (e.getPropertyName().equals("doFilter"))
                    totalGeneLabel.setText("Total genes displayed: " + resultTable.getRowCount());
            });
            return container;
        }
        
        public void setResult(DifferentialExpressionResult result) {
            DiffExpResultTableModel model = (DiffExpResultTableModel) resultTable.getModel();
            model.setResult(result);
            totalGeneLabel.setText("Total genes displayed: " + resultTable.getRowCount());
        }
    }
    
    /**
     * A customized TableModel to show the analysis results.
     * @author wug
     *
     */
    private class DiffExpResultTableModel extends AbstractTableModel {

        private final String[] COL_NAMES = {
                "Gene",
                "Score",
                "LogFoldChange",
                "pValue",
                "FDR"
        };
        private DifferentialExpressionResult result;
        
        public DiffExpResultTableModel() {
        }
        
        @Override
        public String getColumnName(int column) {
            return COL_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return String.class;
            return Double.class;
        }
        
        public void setResult(DifferentialExpressionResult result) {
            this.result = result;
        }

        @Override
        public int getRowCount() {
            if (result == null)
                return 0;
            return result.getNames().size();
        }

        @Override
        public int getColumnCount() {
            return COL_NAMES.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return result.getNames().get(rowIndex);
            case 1:
                return result.getScores().get(rowIndex);
            case 2:
                return result.getLogFoldChanges().get(rowIndex);
            case 3:
                return result.getPvals().get(rowIndex);
            case 4:
                return result.getPvalsAdj().get(rowIndex);
            default :
                return null;
            }
        }
        
    }
    
    /**
     * A customized JDialog for users to choose groups for differential expression analysis.
     * @author wug
     *
     */
    private class GroupSelectionDialog extends JDialog {
        private JComboBox<String> groupBox;
        private JComboBox<String> referenceBox;
        private boolean isOkCliked = false;
        
        public GroupSelectionDialog(List<String> groups) {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init(groups);
        }
        
        private void init(List<String> groups) {
            JPanel contentPane = new JPanel();
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            // For the group
            JLabel label = new JLabel("Choose a group for analysis: ");
            groupBox = new JComboBox<>();
            groups.forEach(groupBox::addItem);
            groupBox.setSelectedIndex(0); // Default
            JPanel panel = new JPanel();
            panel.add(label);
            panel.add(groupBox);
            contentPane.add(panel, constraints);
            // For reference
            label = new JLabel("Choose a reference (use rest for all other cells): ");
            referenceBox = new JComboBox<>();
            referenceBox.addItem("rest");
            groups.forEach(referenceBox::addItem);
            referenceBox.setSelectedIndex(0); // Default
            panel = new JPanel();
            panel.add(label);
            panel.add(referenceBox);
            constraints.gridy = 1;
            contentPane.add(panel, constraints);
            
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            controlPane.getOKBtn().addActionListener(e -> {
                isOkCliked = true;
                dispose();
            });
            controlPane.getCancelBtn().addActionListener(e -> {
                isOkCliked = false;
                dispose();
            });
            
            setTitle("Cell Group Selection");
            setLocationRelativeTo(getOwner());
            setSize(425, 235);
            setModal(true);
            setVisible(true);
        }
        
        public Pair<String, String> getSelected() {
            return new Pair<>(groupBox.getSelectedItem().toString(),
                              referenceBox.getSelectedItem().toString());
        }
        
    }

}
