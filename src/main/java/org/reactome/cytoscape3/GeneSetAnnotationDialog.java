/*
 * Created on Oct 28, 2015
 *
 */
package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import org.cytoscape.view.model.CyNetworkView;
import org.gk.util.DialogControlPane;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cytoscape.service.GeneSetAnnotationPanel;

/**
 * This customized dialog is used to display gene set annotation for a selected genes.
 * @author gwu
 *
 */
public class GeneSetAnnotationDialog extends JDialog {
    private GeneSetAnnotationPanel annotationPane;
    private JTextArea titleTF;
    
    /**
     * @param owner
     */
    public GeneSetAnnotationDialog(Frame owner) {
        super(owner);
        init();
    }
    
    private void init() {
        titleTF = new JTextArea();
        Border outborder = BorderFactory.createEtchedBorder();
        Border inborder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        Border border = BorderFactory.createCompoundBorder(outborder, inborder);
        titleTF.setBorder(border);
        annotationPane = new AnnotationPaneForSelectedGenes("Selected Genes Annotation");
        annotationPane.setBorder(border);
        titleTF.setBackground(annotationPane.getBackground());
        titleTF.setEditable(false);
        titleTF.setWrapStyleWord(true);
        titleTF.setLineWrap(true);
        
        getContentPane().add(titleTF, BorderLayout.NORTH);
        getContentPane().add(annotationPane, BorderLayout.CENTER);
        
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.getCancelBtn().setVisible(false);
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        getContentPane().add(controlPane, BorderLayout.SOUTH);
    }
    
    /**
     * Set annotations to be displayed.
     * @param annotations
     */
    public void setAnnotations(Collection<String> selectedGenes,
                               CyNetworkView networkView,
                               List<ModuleGeneSetAnnotation> annotations,
                               String type) {
        annotationPane.setAnnotations(annotations);
        annotationPane.setNetworkView(networkView);
        setSelectedGenes(selectedGenes);
        // Need to set title
        String title;
        if (type.equals("Pathway"))
            title = "Pathways in Selected Genes";
        else
            title = "GO " + type + " in Selected Genes";
        setTitle(title);
    }
    
    /**
     * Display the selected genes.
     * @param selectedGenes
     */
    private void setSelectedGenes(Collection<String> selectedGenes) {
        StringBuilder builder = new StringBuilder();
        builder.append("Selected genes (");
        if (selectedGenes == null || selectedGenes.size() == 0)
            builder.append("0)");
        else {
            builder.append(selectedGenes.size()).append("): ");
            // Want to sort them
            List<String> geneList = new ArrayList<String>(selectedGenes);
            Collections.sort(geneList);
            for (Iterator<String> it = geneList.iterator(); it.hasNext();) {
                builder.append(it.next());
                if (it.hasNext())
                    builder.append(", ");
            }
        }
        titleTF.setText(builder.toString());
    }
    
    /**
     * Use a subclass to hide the close button.
     * @author gwu
     *
     */
    private class AnnotationPaneForSelectedGenes extends GeneSetAnnotationPanel {
        
        public AnnotationPaneForSelectedGenes(String title) {
            super(title, false);
            controlToolBar.remove(closeBtn);
            this.close();
        }
        
    }
    
}
