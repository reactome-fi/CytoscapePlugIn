package org.reactome.cytoscape.sc;

import static org.reactome.cytoscape.service.PathwayEnrichmentApproach.Binomial_Test;
import static org.reactome.cytoscape.service.PathwayEnrichmentApproach.GSEA;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.math3.util.Pair;
import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.sc.diff.ClusterGenesDialog;
import org.reactome.cytoscape.sc.diff.DiffExpResult;
import org.reactome.cytoscape.sc.diff.DiffExpResultDialog;
import org.reactome.cytoscape.service.PathwayEnrichmentApproach;
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
     * Display the analysis result in a customized dialog.
     * @param result
     * @return
     */
    public void displayResult(DiffExpResult result) {
        DiffExpResultDialog dialog = new DiffExpResultDialog();
        dialog.setResult(result);
        dialog.setTitle("Differential Expression Analysis: " + result.getResultName());
        addAnalysisFeatures(dialog, result);
        dialog.setVisible(true);
    }

    private void addAnalysisFeatures(DiffExpResultDialog dialog, DiffExpResult result) {
        dialog.getFINetworkBtn().addActionListener(e -> {
            DiffExpResult displayedResult = dialog.getDisplayedResult();
            ScNetworkManager.getManager().buildFINetwork(displayedResult);
        });
        dialog.getPathwayAnalyzeBtn().addActionListener(e -> {
            PathwayEnrichmentApproach approach = (PathwayEnrichmentApproach) dialog.getPathwayMethodBox().getSelectedItem();
            if (approach == Binomial_Test) {
                // For displayed results only
                DiffExpResult displayedResult = dialog.getDisplayedResult();
                ScNetworkManager.getManager().doBinomialTest(displayedResult);
            }
            else if (approach == GSEA && result != null)
                ScNetworkManager.getManager().doGSEATest(result, dialog);
        });
    }
    
    /**
     * Display cluster specific genes.
     * @param genes
     */
    public void displayClusterGenes(List<List<String>> genes,
                                    String title) {
        ClusterGenesDialog dialog = new ClusterGenesDialog();
        dialog.setClusterGenes(genes);
        dialog.setTitle(title);
        dialog.setSize(1300, 500);
        addAnalysisFeatures(dialog, null);
        dialog.setVisible(true);
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
