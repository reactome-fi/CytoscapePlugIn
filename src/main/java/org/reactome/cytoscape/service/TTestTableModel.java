/*
 * Created on Feb 9, 2015
 *
 */
package org.reactome.cytoscape.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.math.MathException;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.util.MathUtilities;

/**
 * This customized Table model can be used to perform t-test for two types of samples.
 * @author gwu
 *
 */
public class TTestTableModel extends AbstractTableModel {
    private List<String> colHeaders;
    private List<String[]> data;
    
    public TTestTableModel() {
        String[] headers = new String[]{
                "DB_ID",
                "Name",
                "RealMean",
                "RandomMean",
                "MeanDiff",
//                "t-statistic",
                "p-value",
                "FDR"
        };
        colHeaders = Arrays.asList(headers);
        data = new ArrayList<String[]>();
    }
    
    /**
     * Clear up the displayed data if any.
     */
    public void reset() {
        data.clear();
        fireTableDataChanged();
    }
    
    public void setSampleTypes(List<String> types) {
        if (types.size() < 2)
            return; // Cannot use it
        colHeaders.set(2, types.get(0));
        colHeaders.set(3, types.get(1));
    }
    
    /**
     * Add a new column to the table model. P-value will be returned from this method.
     * @param values1
     * @param values2
     * @param varLabel it should the DB_ID for a Reactome PhysicalEntity instance.
     * @return
     * @throws MathException
     */
    public double addRow(List<Double> values1,
                         List<Double> values2,
                         String... rowAnnotation) throws MathException {
        if (rowAnnotation.length + 5 != colHeaders.size())
            throw new IllegalArgumentException("The number of items in the rowAnnotation array is not right!");
        double mean1 = MathUtilities.calculateMean(values1);
        double mean2 = MathUtilities.calculateMean(values2);
        double diff = mean1 - mean2;
        // Need a double array
        double[] array1 = new double[values1.size()];
        for (int i = 0; i < values1.size(); i++)
            array1[i] = values1.get(i);
        double[] array2 = new double[values2.size()];
        for (int i = 0; i < values2.size(); i++)
            array2[i] = values2.get(i);
//        double t = TestUtils.t(realArray,
//                               randomArray);
//        double pvalue = TestUtils.tTest(realArray,
//                                        randomArray);
        double pvalue = new MannWhitneyUTest().mannWhitneyUTest(array1, array2);
        
        String[] row = new String[colHeaders.size()];
        for (int i = 0; i < rowAnnotation.length; i++)
            row[i] = rowAnnotation[i];
        row[rowAnnotation.length] = PlugInUtilities.formatProbability(mean1);
        row[rowAnnotation.length + 1] = PlugInUtilities.formatProbability(mean2);
        row[rowAnnotation.length + 2] = PlugInUtilities.formatProbability(diff);
//        row[4] = PlugInUtilities.formatProbability(t);
        row[rowAnnotation.length + 3] = PlugInUtilities.formatProbability(pvalue);
        
        data.add(row);
        
        return pvalue;
    }
    
    /**
     * A method to calculate FDRs. The order in the passed List should be the same
     * as p-values stored in the data object. Otherwise, the calculated FDRs assignment
     * will be wrong.
     * @param pvalues
     */
    public void calculateFDRs(List<Double> pvalues) {
        if (data.size() != pvalues.size())
            throw new IllegalArgumentException("Passed pvalues list has different size to the stored table data.");
        List<String[]> pvalueSortedList = new ArrayList<String[]>(data);
        final int fdrIndex = colHeaders.size() - 1;
        // Just copy pvalues into rowdata for the time being
        for (int i = 0; i < pvalueSortedList.size(); i++) {
            Double pvalue = pvalues.get(i);
            String[] rowData = pvalueSortedList.get(i);
            rowData[fdrIndex] = pvalue + "";
        }
        Collections.sort(pvalueSortedList, new Comparator<String[]>() {
            public int compare(String[] row1, String[] row2) {
                Double pvalue1 = new Double(row1[fdrIndex]);
                Double pvalue2 = new Double(row2[fdrIndex]);
                return pvalue1.compareTo(pvalue2);
            }
        });
        // pvalues will be sorted 
        List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
        // Modify pvalues into FDRs for the last column. Since the same String[] objects are
        // used in the sorted list and the original data, there is no need to do anything for
        // table display purpose.
        for (int i = 0; i < pvalueSortedList.size(); i++) {
            String[] rowData = pvalueSortedList.get(i);
            rowData[fdrIndex] = PlugInUtilities.formatProbability(fdrs.get(i));
        }
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return colHeaders.size(); 
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String[] row = data.get(rowIndex);
        return row[columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        return colHeaders.get(column);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }
}