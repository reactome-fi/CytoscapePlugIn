package org.reactome.cytoscape.sc.diff;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.reactome.cytoscape.service.PathwayEnrichmentApproach;
import org.reactome.cytoscape.util.PlugInObjectManager;

@SuppressWarnings("serial")
public class DiffExpResultDialog extends JDialog {
    private JTable resultTable;
    private JLabel totalGeneLabel;
    private JComboBox<PathwayEnrichmentApproach> pathwayBox;
    private JButton pathwayBtn;
    private JButton fiBtn;

    public DiffExpResultDialog() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    /**
     * Return the displayed rows in a DiffExpResult object.
     * @return
     */
    public DiffExpResult getDisplayedResult() {
        DiffExpResult result = new DiffExpResult();
        List<String> genes = new ArrayList<>();
        result.setNames(genes);
        List<Double> scores = new ArrayList<>();
        result.setScores(scores);
        List<Double> logFoldChange = new ArrayList<>();
        result.setLogFoldChanges(logFoldChange);
        List<Double> pvalue = new ArrayList<>();
        result.setPvals(pvalue);
        List<Double> fdr = new ArrayList<>();
        result.setPvalsAdj(fdr);
        for (int i = 0; i < resultTable.getRowCount(); i++) {
            // Columns have been set not to be reordered. So this is safe
            for (int j = 0; j < resultTable.getColumnCount(); j++) {
                Object value = resultTable.getValueAt(i, j);
                switch (j) {
                    case 0 : genes.add((String)value); continue;
                    case 1 : scores.add((Double)value); continue;
                    case 2 : logFoldChange.add((Double)value); continue;
                    case 3 : pvalue.add((Double)value); continue;
                    case 4 : fdr.add((Double)value); continue;
                }
            }
        }
        DiffExpResultTableModel model = (DiffExpResultTableModel) resultTable.getModel();
        result.setResultName(model.result.getResultName());
        return result;
    }

    private void init() {
        setTitle("Differential Expression Analysis Result");

        DiffExpResultTableModel model = new DiffExpResultTableModel();
        resultTable = new JTable(model);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.getTableHeader().setReorderingAllowed(false);
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

        JPanel controlPane = createControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        setSize(975, 520);
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

    public void setResult(DiffExpResult result) {
        DiffExpResultTableModel model = (DiffExpResultTableModel) resultTable.getModel();
        model.setResult(result);
        totalGeneLabel.setText("Total genes displayed: " + resultTable.getRowCount());
    }
    
    private JPanel createControlPane() {
        JPanel pane = new JPanel();
        pane.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 1));
        pane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                          BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        JLabel label = new JLabel("Function analysis: ");
        pane.add(label);
        label = new JLabel("Reactome pathway analysis by ");
        pane.add(label);
        pathwayBox = new JComboBox<PathwayEnrichmentApproach>();
        Stream.of(PathwayEnrichmentApproach.values()).forEach(e -> pathwayBox.addItem(e));
        pathwayBox.setSelectedIndex(0); // Use Binomial test as the default.
        pane.add(pathwayBox);
        pathwayBtn = new JButton("Analyze");
        pathwayBtn.setToolTipText("Perform reactome pathway analysis");
        pane.add(pathwayBtn);
        fiBtn = new JButton("Build FI Network");
        fiBtn.setToolTipText("Build Reactome FI network");
        pane.add(fiBtn);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        pane.add(closeBtn);
        return pane;
    }
    
    public JButton getFINetworkBtn() {
        return this.fiBtn;
    }
    
    public JComboBox<PathwayEnrichmentApproach> getPathwayMethodBox() {
        return this.pathwayBox;
    }
    
    public JButton getPathwayAnalyzeBtn() {
        return this.pathwayBtn;
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
        private DiffExpResult result;

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

        public void setResult(DiffExpResult result) {
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
}