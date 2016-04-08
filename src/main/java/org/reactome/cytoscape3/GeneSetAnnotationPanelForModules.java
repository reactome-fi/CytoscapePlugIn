/*
 * Created on Jul 30, 2010
 *
 */
package org.reactome.cytoscape3;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.table.TableRowSorter;

import org.gk.util.StringUtils;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cytoscape.service.GeneSetAnnotationPanel;

public class GeneSetAnnotationPanelForModules extends GeneSetAnnotationPanel {
    private JComboBox moduleSizeFilter;

    public GeneSetAnnotationPanelForModules(String title){
        super(title);
        init();
    }
        
    private void init() {
        // Remove closeBtn 
        controlToolBar.remove(closeGlue);
        controlToolBar.remove(closeBtn);
        // Add a module size filter
        Font font = hideOtherNodesBox.getFont();
        //filterPane.setBorder(BorderFactory.createEtchedBorder());
        moduleSizeFilter = new JComboBox();
        ComboBoxModel model = new DefaultComboBoxModel();
        moduleSizeFilter.setModel(model);
        moduleSizeFilter.setFont(font);
        JLabel moduleLabel = new JLabel("Module Size");
        moduleLabel.setFont(font);
        controlToolBar.add(moduleLabel);
        controlToolBar.add(moduleSizeFilter);
        // Add close button back
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        // Add listener to two spinners
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetAnnotations();
            }
        };
        moduleSizeFilter.addActionListener(listener);
    }
    
    protected String getSelectedPathway() {
        return (String) contentTable.getValueAt(contentTable.getSelectedRow(),
                                                1);
    }
    
    public void setAnnotations(List<ModuleGeneSetAnnotation> annotations) {
        this.annotations = annotations;
        // Need to set module size if it is for module
        // Reset the module size filter based on the passed argument
        Set<Integer> sizeSet = new HashSet<Integer>();
        for (ModuleGeneSetAnnotation annot : annotations) {
            sizeSet.add(annot.getIds().size());
        }
        List<Integer> moduleSizes = new ArrayList<Integer>(sizeSet);
        Collections.sort(moduleSizes, new Comparator<Integer>() {
            public int compare(Integer size1, Integer size2) {
                return size2 - size1;
            }
        });
        DefaultComboBoxModel model = (DefaultComboBoxModel) moduleSizeFilter.getModel();
        model.removeAllElements();
        for (Integer size : moduleSizes)
            model.addElement(size);
        // Choose the smallest as the default
        moduleSizeFilter.setSelectedItem(moduleSizes.get(moduleSizes.size() - 1));
        resetAnnotations();
    }
    
    protected void resetAnnotations() {
        AnnotationTableModelForModules model = (AnnotationTableModelForModules) contentTable.getModel();
        model.setContent(annotations,
                         (Integer) moduleSizeFilter.getSelectedItem(),
                         (Double) fdrFilter.getSelectedItem());
    }
    
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new AnnotationTableModelForModules();
    }

    @Override
    protected TableRowSorter<NetworkModuleTableModel> createTableRowSorter(NetworkModuleTableModel model) {
        return new AnnotationTableRowSorterForModules(model);
    }

    /**
     * Sorter for table models for modules.
     * @author gwu
     *
     */
    private class AnnotationTableRowSorterForModules extends TableRowSorter<NetworkModuleTableModel> {
        
        public AnnotationTableRowSorterForModules(NetworkModuleTableModel model) {
            super(model);
        }

        @Override
        public Comparator<?> getComparator(int column) {
            if (column == 1 || column == 7)
                return super.getComparator(column);
            // Something special for FDR since it may contains "<"
            Comparator<String> comparator = new Comparator<String>() {
                public int compare(String value1, String value2) {
                    if (value1.startsWith("<") && value2.startsWith("<")) {
                        String value11 = value1.substring(1);
                        String value21 = value2.substring(1);
                        return new Double(value11).compareTo(new Double(value21));
                    }
                    else if (value1.startsWith("<"))
                        return -1;
                    else if (value2.startsWith("<"))
                        return 1;
                    else {
                        Double d1 = new Double(value1);
                        Double d2 = new Double(value2);
                        return d1.compareTo(d2);
                    }
                }
            };
            return comparator;
        }
    }
    
    protected class AnnotationTableModelForModules extends AnnotationTableModel {
        private String[] moduleHeaders = new String[] {
                "Module",
                "GeneSet",
                "RatioOfProteinInGeneSet",
                "NumberOfProteinInGeneSet",
                "ProteinFromModule",
                "P-value",
                "FDR",
                "Nodes" 
        };
        
        public AnnotationTableModelForModules() {
            columnHeaders = moduleHeaders;
        }
        
        public void setContent(List<ModuleGeneSetAnnotation> annotations,
                               Integer moduleSizeCutoff,
                               Double fdrCutoff) {
            tableData.clear();
            for (ModuleGeneSetAnnotation moduleAnnot : annotations) {
                if (moduleSizeCutoff != null && 
                    moduleAnnot.getIds().size() < moduleSizeCutoff) // Filter based module size
                    continue;
                // This should be sorted already
                List<GeneSetAnnotation> annots = moduleAnnot.getAnnotations();
                for (GeneSetAnnotation annot : annots) {
                    if (fdrCutoff != null &&
                        !annot.getFdr().startsWith("<") &&
                        (new Double(annot.getFdr()) > fdrCutoff)) // Filter based on FDR
                        continue; 
                    String[] row = new String[columnHeaders.length];
                    int start = 0;
                    row[start ++] = moduleAnnot.getModule() + "";
                    row[start ++] = annot.getTopic();
                    row[start ++] = String.format("%.4f", annot.getRatioOfTopic());
                    row[start ++] = annot.getNumberInTopic() + "";
                    row[start ++] = annot.getHitNumber() + "";
                    row[start ++] = formatPValue(annot.getPValue());
                    String fdr = annot.getFdr(); // FDRs are calculated based on Benjamni-Hocherber.
                    // It should be safe to convert it into double
                    row[start ++] = formatPValue(new Double(fdr));
                    row[start] = StringUtils.join(",", annot.getHitIds());
                    tableData.add(row);
                }
            }
            fireTableDataChanged();
        }
    }
}
