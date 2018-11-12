/*
 * Created on Feb 12, 2015
 *
 */
package org.reactome.cytoscape.pathway;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.TableRowSorter;

import org.reactome.cytoscape.pgm.PathwayResultSummary;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.util.MathUtilities;

/**
 * @author gwu
 *
 */
public class FactorGraphBatchResultPane extends PathwayEnrichmentResultPane {
    
    /**
     * @param eventTreePane
     * @param title
     */
    public FactorGraphBatchResultPane(EventTreePane eventTreePane, String title) {
        super(eventTreePane, title);
    }
    
    /**
     * Set the results to be displayed.
     * @param resultList
     */
    public void setResults(List<PathwayResultSummary> resultList) {
        FactorGraphBatchResultTableModel model = (FactorGraphBatchResultTableModel) contentTable.getModel();
        try {
            model.setResults(resultList);
            List<? extends SortKey> sortKeys = PlugInUtilities.getSortedKeys(contentTable,
                                                                             contentTable.getColumnCount() - 2);
            if (sortKeys != null)
                contentTable.getRowSorter().setSortKeys(sortKeys);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(this,
                                          "Error in displaying results: " + e,
                                          "Error in Displaying Results",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new FactorGraphBatchResultTableModel();
    }

    @Override
    protected TableRowSorter<NetworkModuleTableModel> createTableRowSorter(NetworkModuleTableModel model) {
        return new FactorGraphBatchResultTableRowSorter(model);
    }
    
    /**
     * For handling FDR filtering
     */
    @Override
    protected void resetAnnotations() {
        final double fdrCutoff = new Double(fdrFilter.getSelectedItem().toString());
        RowFilter<NetworkModuleTableModel, Integer> rowFilter = new RowFilter<NetworkModuleTableModel, Integer>() {

            @Override
            public boolean include(Entry<? extends NetworkModuleTableModel, ? extends Integer> entry) {
                NetworkModuleTableModel model = entry.getModel();
                // The last two values should be FDRs
                int count = entry.getValueCount();
                for (int i = count - 1; i > count - 3; i--) {
                    String value = entry.getStringValue(i);
                    Double fdr = new Double(value);
                    if (fdr <= fdrCutoff)
                        return true;
                }
                return false;
            }
        };
        @SuppressWarnings("unchecked")
        TableRowSorter<? extends NetworkModuleTableModel> sorter = (TableRowSorter<? extends NetworkModuleTableModel>) contentTable.getRowSorter();
        sorter.setRowFilter(rowFilter);
    }



    private class FactorGraphBatchResultTableModel extends AnnotationTableModel {
        private String[] columns = new String[] {
                "ReactomePathway",
                "AverageUpIPA",
                "AverageDownIPA",
                "CombinedPValue",
                "MinimumPValue",
                "FDR_CombinedPValue",
                "FDR_MinimumPValue"
        };
        
        public FactorGraphBatchResultTableModel() {
            columnHeaders = columns;
        }
        
        public void setResults(List<PathwayResultSummary> resultList) throws Exception {
            tableData.clear();
            calculateFDRs(resultList);
            for (PathwayResultSummary result : resultList) {
                String[] row = new String[columnHeaders.length];
                row[0] = result.getPathwayName();
                row[1] = PlugInUtilities.formatProbability(result.getAverageUpIPAs());
                row[2] = PlugInUtilities.formatProbability(result.getAverageDownIPAs());
                row[3] = PlugInUtilities.formatProbability(result.getCombinedPValue());
                row[4] = PlugInUtilities.formatProbability(result.getMinPValue());
                row[5] = PlugInUtilities.formatProbability(result.getCombinedPValueFDR());
                row[6] = PlugInUtilities.formatProbability(result.getMinPValueFDR());
                tableData.add(row);
            }
            fireTableDataChanged();
        }
        
        private void calculateFDRs(List<PathwayResultSummary> resultList) throws Exception {
            // Calculate FDRs based on combined pvalues
            // Sort resultList based on combined pvalues
            Method getMethod = PathwayResultSummary.class.getMethod("getCombinedPValue");
            Method setMethod = PathwayResultSummary.class.getMethod("setCombinedPValueFDR", Double.class);
            calculateFDRs(resultList, getMethod, setMethod);
            getMethod = PathwayResultSummary.class.getMethod("getMinPValue");
            setMethod = PathwayResultSummary.class.getMethod("setMinPValueFDR", Double.class);
            calculateFDRs(resultList, getMethod, setMethod);
        }

        private void calculateFDRs(List<PathwayResultSummary> resultList,
                                   final Method getMethod,
                                   final Method setMethod) throws IllegalAccessException, InvocationTargetException {
            Collections.sort(resultList, new Comparator<PathwayResultSummary>() {
                public int compare(PathwayResultSummary result1, PathwayResultSummary result2) {
                    try {
                        double value1 = (Double) getMethod.invoke(result1);
                        double value2 = (Double) getMethod.invoke(result2);
                        double diff = value1 - value2;
                        if (diff < 0.0d)
                            return -1;
                        if (diff > 0.0d)
                            return 1;
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    return 0;
                }
            });
            List<Double> pvalues = new ArrayList<Double>();
            for (PathwayResultSummary result : resultList)
                pvalues.add((Double)getMethod.invoke(result));
            List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
            for (int i = 0; i < pvalues.size(); i++) {
                PathwayResultSummary result = resultList.get(i);
                setMethod.invoke(result, fdrs.get(i));
            }
        }
        
        
    }
    
    private class FactorGraphBatchResultTableRowSorter extends TableRowSorter<NetworkModuleTableModel> {
        
        public FactorGraphBatchResultTableRowSorter(NetworkModuleTableModel model) {
            super(model);
        }

        @Override
        public Comparator<?> getComparator(int column) {
            if (column == 0)
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
    
}
