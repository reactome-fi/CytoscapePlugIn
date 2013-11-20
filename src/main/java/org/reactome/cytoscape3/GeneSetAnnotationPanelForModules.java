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
                    row[start ++] = String.format("%.4f", annot.getPValue());
                    row[start ++] = annot.getFdr() + "";
                    row[start] = StringUtils.join(",", annot.getHitIds());
                    tableData.add(row);
                }
            }
            fireTableDataChanged();
        }
    }
}
