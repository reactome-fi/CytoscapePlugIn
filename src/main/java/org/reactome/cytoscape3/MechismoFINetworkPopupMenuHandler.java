package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.mechismo.MechismoDataFetcher;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.mechismo.model.CancerType;

public class MechismoFINetworkPopupMenuHandler extends GeneSetFINetworkPopupMenuHandler {
    
    public MechismoFINetworkPopupMenuHandler() {
    }
    
    @Override
    protected void installMenus() {
        super.installMenus();
        ShowCancerSpecificNetwork cancerMenu = new ShowCancerSpecificNetwork();
        installOtherNetworkMenu(cancerMenu,
                               "Show Cancer Specific Network");
    }
    
    private class ShowCancerSpecificNetwork implements CyNetworkViewContextMenuFactory {
        private CyNetworkView networkView;
        private JComboBox<String> cancerBox;
        private JTextField fdrTF;
        private boolean isInAnimation;
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            this.networkView = view;
            JMenuItem showCancerMenu = new JMenuItem("Show Cancer Specific Network");
            showCancerMenu.addActionListener(e -> {
                selectEdgesForCancer();
            });
            return new CyMenuItem(showCancerMenu, 1000.0f); // Make sure it should be installed at the end
        }
        
        private void selectEdgesForCancer() {
            JDialog dialog = new JDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setTitle("Choose Cancer Specific Network");
            JPanel contentPane = createPanel();
            dialog.getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            JButton okBtn = controlPane.getOKBtn();
            okBtn.setText("Play");
            okBtn.addActionListener(e -> playAnimation(okBtn));
            controlPane.getCancelBtn().addActionListener(e -> {
                isInAnimation = false;
                dialog.dispose();
            });
            controlPane.getCancelBtn().setText("Close");
            dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
            dialog.setSize(350, 250);
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setVisible(true);
        }
        
        private void playAnimation(JButton okBtn) {
            String text = okBtn.getText();
            if (text.startsWith("Play")) {
                isInAnimation = true;
                okBtn.setText("Stop");
                Thread t = new Thread() {
                    public void run() {
                        while (isInAnimation) {
                            int index = cancerBox.getSelectedIndex();
                            int next = index + 1;
                            if (next >= cancerBox.getItemCount())
                                next = 0;
                            cancerBox.setSelectedIndex(next);
                            try {
                                Thread.sleep(500); 
                            }
                            catch(Exception e) {}
                        }
                    }
                };
                t.start();
            }
            else {
                isInAnimation = false;
                okBtn.setText("Play");
            }
        }

        private void showCancerNetwork() {
            // Call the following two lines first. Otherwise
            // curve edges may be used, most likely caused by
            // no nodes there.
            PlugInUtilities.showAllNodes(networkView);
            PlugInUtilities.showAllEdges(networkView);
            String cancer = (String) cancerBox.getSelectedItem();
            if (cancer.equals("")) {
                networkView.updateView();
                return;
            }
            Double fdr = new Double(fdrTF.getText().trim());
            CyTable table = networkView.getModel().getDefaultEdgeTable();
            // Keep a list of nodes that should be displayed
            Set<CyNode> nodesToBeDisplayed = new HashSet<>();
            networkView.getEdgeViews().forEach(edgeView -> {
                CyEdge edge = edgeView.getModel();
                Double value = table.getRow(edge.getSUID()).get(cancer, Double.class);
                if (value == null || value > fdr)
                    PlugInUtilities.hideEdge(edgeView);
                else {
                    PlugInUtilities.showEdge(edgeView);
                    nodesToBeDisplayed.add(edge.getSource());
                    nodesToBeDisplayed.add(edge.getTarget());
                }
            });
            // Hide all nodes that are not linked
            networkView.getNodeViews().forEach(nodeView -> {
                if (nodesToBeDisplayed.contains(nodeView.getModel()))
                    PlugInUtilities.showNode(nodeView);
                else
                    PlugInUtilities.hideNode(nodeView);
            });
            networkView.updateView();
        }
        
        private List<String> getCancerTypes() {
            MechismoDataFetcher fetcher = new MechismoDataFetcher();
            List<CancerType> types = fetcher.loadCancerTypes();
            List<String> rtn = new ArrayList<>();
            types.forEach(type -> rtn.add(type.getAbbreviation()));
            Collections.sort(rtn);
            int pancan = rtn.indexOf("PANCAN");
            if (pancan > -1)
                rtn.remove(pancan);
            rtn.add(0, "PANCAN");
            rtn.add(0, "");
            return rtn;
        }
        
        private JPanel createPanel() {
            JPanel pane = new JPanel();
            pane.setBorder(BorderFactory.createEtchedBorder());
            pane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.WEST;
            JLabel choose = new JLabel("Choose a cancer:");
            constraints.gridy = 0;
            pane.add(choose, constraints);
            constraints.gridy ++;
            cancerBox = new JComboBox<>();
            List<String> types = getCancerTypes();
            types.forEach(type -> cancerBox.addItem(type));
            cancerBox.setEditable(false);
            cancerBox.addActionListener(e -> showCancerNetwork());
            cancerBox.setSelectedIndex(0); // As the default
            pane.add(cancerBox, constraints);
            JLabel fdrLabel = new JLabel("Choose a FDR cutoff:");
            fdrTF = new JTextField();
            fdrTF.setColumns(4);
            fdrTF.setText(0.05 + "");
            constraints.gridy ++;
            pane.add(fdrLabel, constraints);
            constraints.gridy ++;
            pane.add(fdrTF, constraints);
            return pane;
        }
        
    }
    
}
