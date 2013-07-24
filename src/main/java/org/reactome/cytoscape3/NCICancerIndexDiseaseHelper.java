package org.reactome.cytoscape3;



import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.gk.util.GKApplicationUtilities;
import org.reactome.cancerindex.model.DiseaseData;
import org.reactome.cytoscape.util.NodeUtilitiesImpl;



public class NCICancerIndexDiseaseHelper
{

    private CyNetworkViewManager viewManager;





    public NCICancerIndexDiseaseHelper()
    {
    }





    private class DiseaseDisplayPane extends JPanel {
        private JTree diseaseTree;
        private JTextArea definitionTA;

        public DiseaseDisplayPane() {
            init();
        }
        private void selectGenesForDisease(final DiseaseData disease, final CyNetworkView view)
        {
            if (view == null)
                return;
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    Set<String> checkingCodes = new HashSet<String>();
                    grepDiseaseCodes(disease, checkingCodes);
                    CyTable nodeTable = view.getModel().getDefaultNodeTable();
                    for (Iterator<View<CyNode>>it = view.getNodeViews().iterator(); it.hasNext();)
                    {
                        View<CyNode> nodeView = it.next();
                        NodeUtilitiesImpl.unselectNode(nodeView.getModel(), view.getModel());
                        String nodeName = nodeTable.getRow(nodeView.getModel().getSUID()).get("name", String.class);
                        String diseases = nodeTable.getRow(nodeView.getModel().getSUID()).get("Diseases", String.class);
                        if (diseases == null || diseases.length() == 0)
                            continue;
                        String[] tokens = diseases.split(",");
                        for (String token : tokens)
                        {
                            if (checkingCodes.contains(token))
                            {
                                NodeUtilitiesImpl.selectNode(nodeView.getModel(), view.getModel());
                                break;
                            }


                        }
                    }
                    view.updateView();
                }
            });
        }
        private void grepDiseaseCodes(DiseaseData disease, Set<String> codes) {
            codes.add(disease.getNciDiseaseConceptCode());
            if (disease.getSubTerms() == null || disease.getSubTerms().size() == 0)
                return;
            for (DiseaseData subDisease : disease.getSubTerms())
                grepDiseaseCodes(subDisease, codes);
        }
        private void init()
        {
            // Display tree
            JPanel hierarchyPane = new JPanel();
            hierarchyPane.setLayout(new BorderLayout());
            JLabel label = GKApplicationUtilities.createTitleLabel("Disease Hierarchy");
            hierarchyPane.add(label, BorderLayout.NORTH);
            // Set up disease tree
            diseaseTree = new JTree();
            diseaseTree.putClientProperty("JTree.lineStyle", "Horizontal");
            diseaseTree.setEditable(false);
            DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
            ImageIcon icon = PlugInScopeObjectManager.getManager().createImageIcon("Bullet1.gif");
            // Use same icons for all states
            renderer.setIcon(icon);
            renderer.setLeafIcon(icon);
            ImageIcon folderIcon = PlugInScopeObjectManager.getManager().createImageIcon("Bullet.gif");
            renderer.setOpenIcon(folderIcon);
            renderer.setClosedIcon(folderIcon);
            diseaseTree.setCellRenderer(renderer);
            diseaseTree.setRootVisible(false);
            diseaseTree.setShowsRootHandles(true);
            addTreePopupMenu(diseaseTree);

            hierarchyPane.add(new JScrollPane(diseaseTree), BorderLayout.CENTER);
            // The following code is used to fix a bug resulting from the Cytoscape codebase: the selection
            // in a tree cannot make a repaint in the tree. This bug occurs in Cytoscape only and has not been 
            // seen in other place!!!
            diseaseTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {

                public void valueChanged(TreeSelectionEvent e) {
                    TreePath[] paths = e.getPaths();
                    if (paths == null)
                        return;
                    for (TreePath path : paths) {
                        Rectangle rect = diseaseTree.getPathBounds(path);
                        if (rect == null)
                            continue;
                        diseaseTree.repaint(rect);
                    }
                    if (diseaseTree.getSelectionCount() == 1) {
                        // Show the definition
                        TreePath path = diseaseTree.getSelectionPath();
                        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                        DiseaseData disease = (DiseaseData) treeNode.getUserObject();
                        showDefinition(disease);
                        // For a single selection only
                        //selectGenesForDisease(disease, view);
                    }
                }
            });
            // Display definition
            JPanel definitionPane = new JPanel();
            definitionPane.setLayout(new BorderLayout());
            label = GKApplicationUtilities.createTitleLabel("Definition");
            definitionPane.add(label, BorderLayout.NORTH);
            definitionTA = new JTextArea();
            definitionTA.setRows(8);
            definitionTA.setEditable(false);
            definitionTA.setLineWrap(true);
            definitionTA.setWrapStyleWord(true);
            definitionPane.add(new JScrollPane(definitionTA), BorderLayout.CENTER);

            setLayout(new BorderLayout());
            add(hierarchyPane, BorderLayout.CENTER);
            add(definitionPane, BorderLayout.SOUTH);
        }

        public void showDefinition(DiseaseData disease) {
            definitionTA.setText(disease.getMatchedDiseaseTerm() + " (" + 
                    disease.getNciDiseaseConceptCode() + "): " + 
                    (disease.getDefinition() == null ? "not defined" : disease.getDefinition()));
            definitionTA.setCaretPosition(0);
        }
        private void addTreePopupMenu(final JTree tree)
        {
            tree.addMouseListener(new MouseAdapter() 
            {
                public void mousePressed(MouseEvent e)
                {
                    //if (e.isPopupTrigger())
                       // doTreePopup(e, tree);
                }
                public void mouseReleased(MouseEvent e)
                {
                   // if (e.isPopupTrigger())
                       // doTreePopup(e, tree);
                }
            });
        }
    }
}
