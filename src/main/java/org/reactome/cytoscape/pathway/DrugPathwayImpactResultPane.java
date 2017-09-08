package org.reactome.cytoscape.pathway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;

public class DrugPathwayImpactResultPane extends PathwayEnrichmentResultPane {
    private JLabel summaryLabel;
    
    public DrugPathwayImpactResultPane(EventTreePane eventTreePane, String title) {
        super(eventTreePane, title);
        // Need to modifiy the control bars
        controlToolBar.removeAll();
        summaryLabel = new JLabel("Summary:"); // Just a place hold
        controlToolBar.add(summaryLabel);
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        
        eventTreePane.addDrugImpactResultPane(this);
    }
    
    @Override
    public void close() {
        super.close();
        eventTreePane.removeDrugImpactResultPane(this);
    }

    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new DrugPathwayTableModel();
    }

    public void setResults(String drug,
                           String results) {
        summaryLabel.setText("Pathway impact analysis results for " + drug);
        
        DrugPathwayTableModel model = (DrugPathwayTableModel) contentTable.getModel();
        model.setResults(results);
        
        // Fake GeneSetAnnotation for highlight pathway diagrams
        //TODO: This will be implemented later on. For the time being
        // The user has to show cancer drugs manually.
//        setUpDiagramHighlight(model);
        
        // Sort based on the mean, sum, and then pathway name
        List<SortKey> sortedKeys = new ArrayList<>();
        sortedKeys.add(new RowSorter.SortKey(2, SortOrder.DESCENDING));
        sortedKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
        sortedKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        contentTable.getRowSorter().setSortKeys(sortedKeys);
    }
    
    private class DrugPathwayTableModel extends AnnotationTableModel {
        private String[] geneSetHeaders = new String[] {
                "ReactomePathway",
                "SumOfTotalOutputImpacts",
                "AverageOfOutputImpacts",
                "TargetProteins"
        };
        
        public void setResults(String results) {
            tableData.clear();
            String[] lines = results.split("\n");
            if (lines.length > 0) {
                Arrays.asList(lines)
                      .stream()
                      .map(line -> line.split("\t"))
                      .forEach(tokens -> {
                          // Remove the first DB_ID token
                          String[] row = new String[tokens.length - 1];
                          for (int i = 1; i < tokens.length; i++)
                              row[i - 1] = tokens[i];
                          tableData.add(row);
                      });
            }
            fireTableDataChanged();
        }
        
        public DrugPathwayTableModel() {
            columnHeaders = geneSetHeaders;
        }
    }

}
