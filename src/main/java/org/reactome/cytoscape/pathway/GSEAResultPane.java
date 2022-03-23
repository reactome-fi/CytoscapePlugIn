package org.reactome.cytoscape.pathway;

import java.util.Comparator;
import java.util.List;

import javax.swing.table.TableRowSorter;

import org.reactome.gsea.model.GseaAnalysisResult;

public class GSEAResultPane extends PathwayEnrichmentResultPane {
    public static final String RESULT_PANE_TITLE = "Reactome GSEA Analysis";

    private List<GseaAnalysisResult> results; // To be filtered

    public GSEAResultPane() {
        this(null, RESULT_PANE_TITLE);
    }

    public void setEventTreePane(EventTreePane eventPane) {
        this.eventTreePane = eventPane;
        if (eventPane != null)
            eventPane.setAnnotationPane(this);
    }

    public GSEAResultPane(EventTreePane eventTreePane, String title) {
        super(eventTreePane, title);
    }

    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new GSEAResultTableModel();
    }

    public void setResults(List<GseaAnalysisResult> results) {
        this.results = results;
        GSEAResultTableModel model = (GSEAResultTableModel) contentTable.getModel();
        model.setResults(results, 
                         (Double) fdrFilter.getSelectedItem());
    }
    
    public List<GseaAnalysisResult> getResults() {
    	return this.results;
    }

    @Override
    protected void resetAnnotations() {
        GSEAResultTableModel model = (GSEAResultTableModel) contentTable.getModel();
        model.setResults(results,
                         (Double) fdrFilter.getSelectedItem());
    }

    @Override
    protected TableRowSorter<NetworkModuleTableModel> createTableRowSorter(NetworkModuleTableModel model) {
        return new GSEAResultTableRowSorter(model);
    }

    private class GSEAResultTableRowSorter extends TableRowSorter<NetworkModuleTableModel> {

        public GSEAResultTableRowSorter(NetworkModuleTableModel model) {
            super(model);
        }

        @Override
        public Comparator<?> getComparator(int column) {
            if (column == 0 || column == 5) // Use default
                return super.getComparator(column);
            // Do based on numbers
            Comparator<String> comparator = new Comparator<String>() {
                public int compare(String value1, String value2) {
                    Double d1 = new Double(value1.toString());
                    Double d2 = new Double(value2.toString());
                    return d1.compareTo(d2);
                }
            };
            return comparator;
        }
    }

    private class GSEAResultTableModel extends AnnotationTableModel {
        private String[] geneSetHeaders = new String[] {
                "ReactomePathway",
                "EnrichedScore",
                "NormalizedEnrichedScore",
                "P-value",
                "FDR",
                "Direction"
        };

        public GSEAResultTableModel() {
            columnHeaders = geneSetHeaders;
        }

        public void setResults(List<GseaAnalysisResult> results,
                               Double fdrCutoff) {
            tableData.clear();
            if (results == null || results.size() == 0) {
                fireTableDataChanged();
                return;
            }
            results.stream()
            .filter(result -> result.getFdr() <= fdrCutoff)
            .sorted((r1, r2) -> {
                Float f1 = r1.getFdr();
                Float f2 = r2.getFdr();
                return f1.compareTo(f2);
            })
            .forEach(result -> {
                String[] row = new String[columnHeaders.length];
                int start = 0;
                row[start ++] = result.getPathway().getName();
                row[start ++] = String.format("%.4f", result.getScore());
                row[start ++] = String.format("%.4f", result.getNormalizedScore());
                row[start ++] = formatPValue(result.getPvalue());
                row[start ++] = formatPValue(result.getFdr());
                row[start] = result.getRegulationType().toString();
                tableData.add(row);
            });
            fireTableDataChanged();
        }

    }

}
