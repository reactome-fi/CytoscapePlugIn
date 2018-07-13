package org.reactome.cytoscape.genescore;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.gk.graphEditor.SelectionMediator;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This customized JPanel is used to show gene scores in a table.
 * @author wug
 *
 */
public class GeneScoreTablePane extends JPanel {
    
    private JTable contentTable;
    private JCheckBox filterGenesToDiagram;
    private Set<String> pathwayGenes;
    
    private GeneSelectionHandler selectionHandler;
    
    public GeneScoreTablePane() {
        init();
    }
    
    public void close() {
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        if (mediator.getSelectables() != null)
            mediator.getSelectables().remove(selectionHandler);
        
        getParent().remove(this);
    }
    
    private void init() {
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new BorderLayout());
        JLabel tileLabel = new JLabel("Gene scores and ranks:");
        Font font = tileLabel.getFont();
        tileLabel.setFont(font.deriveFont(Font.BOLD));
        add(tileLabel, BorderLayout.NORTH);
        GeneScoreTableModel model = new GeneScoreTableModel();
        contentTable = new JTable(model);
        contentTable.setAutoCreateRowSorter(true);
        add(new JScrollPane(contentTable), BorderLayout.CENTER);
        filterGenesToDiagram = new JCheckBox("Filter genes to diagram");
        filterGenesToDiagram.setSelected(true);
        filterGenesToDiagram.addItemListener(e -> filterRows());
        add(filterGenesToDiagram, BorderLayout.SOUTH);
        
        selectionHandler = new GeneSelectionHandler();
        selectionHandler.setGeneScoreTable(contentTable);
        PlugInObjectManager.getManager().getDBIdSelectionMediator().addSelectable(selectionHandler);
        contentTable.getSelectionModel().addListSelectionListener(e -> handleTableSelection());
    }
    
    private void handleTableSelection() {
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.fireSelectionEvent(selectionHandler);
    }
    
    public void setPathwayGenes(Set<String> genes) {
        this.pathwayGenes = genes;
        filterRows();
    }
    
    private void filterRows() {
        // Synchronize network views and FIs displayed in the table
        RowFilter<GeneScoreTableModel, Object> filter = new RowFilter<GeneScoreTableModel, Object>() {
            public boolean include(Entry<? extends GeneScoreTableModel, ? extends Object> entry) {
                if (!filterGenesToDiagram.isSelected() || pathwayGenes == null || pathwayGenes.size() == 0)
                    return true;
                String gene = entry.getStringValue(0);
                return pathwayGenes.contains(gene);
            }
        };
        TableRowSorter sorter = (TableRowSorter) contentTable.getRowSorter();
        sorter.setRowFilter(filter);
    }
    
    public void setGeneToScore(Map<String, Double> geneToScore) {
        GeneScoreTableModel model = (GeneScoreTableModel) contentTable.getModel();
        model.setData(geneToScore);
    }
    
    public void setGeneToDBIDs(Map<String, List<Long>> geneToDBIDs) {
        selectionHandler.setGeneToDBIDs(geneToDBIDs);
    }
    
    public void setDBIDToGenes(Map<Long, Set<String>> dbIdToGenes) {
        selectionHandler.setDbIdToGenes(dbIdToGenes);
    }
    
    private class GeneScoreTableModel extends AbstractTableModel {
        private String[] headers = {"Gene", "Score", "Rank"};
        private List<Object[]> data;
        
        public GeneScoreTableModel() {
            data = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        public void setData(Map<String, Double> geneToScore) {
            data.clear();
            List<String> geneList = new ArrayList<>(geneToScore.keySet());
            geneList.sort((gene1, gene2) -> geneToScore.get(gene2).compareTo(geneToScore.get(gene1)));
            for (int i = 0; i < geneList.size(); i++) {
                Object[] row = new Object[3];
                row[0] = geneList.get(i);
                row[1] = geneToScore.get(row[0]);
                row[2] = i + 1;
                data.add(row);
            }
            fireTableDataChanged();
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object[] row = data.get(rowIndex);
            return row[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0 : return String.class;
                case 1 : return Double.class;
                case 2 : return Integer.class;
                default : return String.class;
            }
        }
        
    }

}
