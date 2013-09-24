package org.reactome.cytoscape3;



import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.TreeUtilities;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cancerindex.data.NCIDiseaseHandler;
import org.reactome.cancerindex.model.DiseaseData;
import org.reactome.cytoscape.util.NodeUtilitiesImpl;
import org.reactome.cytoscape.util.PlugInObjectManager;



public class NCICancerIndexDiseaseHelper
{

    private DiseaseDisplayPane displayPane;
    private Map<String, DiseaseData> codeToDisease;
    private CyNetworkView view;

    public NCICancerIndexDiseaseHelper(CyNetworkView view)
    {
        this.view = view;
    }

    public boolean areDiseasesShown() 
    {
        FIPlugInHelper r = FIPlugInHelper.getHelper();
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        org.cytoscape.application.swing.CytoPanel westPanel = desktopApp.getCytoPanel(CytoPanelName.WEST);
        int numComps = westPanel.getCytoPanelComponentCount();
        for (int i = 0; i < numComps; i++)
        {
            Component aComp = (Component) westPanel.getComponentAt(i);
            if ((aComp instanceof CytoPanelComponent) && ((CytoPanelComponent) aComp).getTitle().equals("NCI Diseases"))
                return true;
        }
        return false;
    }
    
    public Map<String, DiseaseData> fetchDiseases() throws IOException
    {
        String serviceUrl = FIPlugInHelper.getHelper().getRestfulURL();
     // Get the host URL name
        int index = serviceUrl.lastIndexOf("/", serviceUrl.length() - 2);
        String diseaseUrl = serviceUrl.substring(0, index + 1) + "Cytoscape/Disease_Thesaurus_10.05d.txt.zip";
        URL url = new URL(diseaseUrl);
        InputStream is = url.openStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        ZipInputStream zis = new ZipInputStream(is);
        InputStreamReader reader = new InputStreamReader(zis);
        BufferedReader bReader = new BufferedReader(reader);
        ZipEntry entry = null;
        Map<String, DiseaseData> codeToDisease = null;
        NCIDiseaseHandler handler = new NCIDiseaseHandler();
        while ((entry = zis.getNextEntry()) != null) {
            handler.loadData(bReader);
            codeToDisease = handler.getCodeToDisease();
        }
        bReader.close();
        reader.close();
        zis.close();
        bis.close();
        is.close();
        return codeToDisease;
    }

    public void displayDiseases(Map<String, DiseaseData> codeToDisease)
    {
        if (displayPane == null)
            displayPane = new DiseaseDisplayPane();
        displayPane.showDiseases(codeToDisease);
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel westPane = desktopApp.getCytoPanel(CytoPanelName.WEST);
        int numComps = westPane.getCytoPanelComponentCount();
        for (int i = 0; i < numComps; i++)
        {
            if (westPane.getComponentAt(i) instanceof CytoPanelComponent && 
                ((CytoPanelComponent) westPane.getComponentAt(i)).getTitle().equals("NCI Diseases"))
            {
                westPane.setSelectedIndex(i);
                break;
            }
        }
        this.codeToDisease = codeToDisease;
    }
    
    private void buildTree(DefaultMutableTreeNode root, List<DiseaseData> subDiseases) {
        for (DiseaseData disease : subDiseases) {
            DefaultMutableTreeNode subNode = new DefaultMutableTreeNode();
            subNode.setUserObject(disease);
            root.add(subNode);
            List<DiseaseData> subSubDiseases = disease.getSubTerms();
            if (subSubDiseases != null && subSubDiseases.size() > 0) {
                sortDiseases(subSubDiseases);
                buildTree(subNode, subSubDiseases);
            }
        }
    }
    
    private void sortDiseases(List<DiseaseData> diseases) {
        Collections.sort(diseases, new Comparator<DiseaseData>() {
            public int compare(DiseaseData disease1, DiseaseData disease2) {
                String name1 = disease1.getMatchedDiseaseTerm();
                String name2 = disease2.getMatchedDiseaseTerm();
                return name1.compareTo(name2);
            }
        });
    }
    
    private class SearchDialog extends JDialog {
        private boolean isOkClicked;
        private JTextField textField;
        private JButton okBtn;
        private JButton cancelBtn;
        private JCheckBox wholeNameBox;
        
        public SearchDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        private void init() {
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            JLabel searchLabel = new JLabel("Search diseases:");
            contentPane.add(searchLabel, constraints);
            textField = new JTextField();
            constraints.gridy = 1;
            textField.setPreferredSize(new Dimension(250, 25));
            contentPane.add(textField, constraints);
            wholeNameBox = new JCheckBox("Match whole name only");
            constraints.gridy = 2;
            contentPane.add(wholeNameBox, constraints);
            DialogControlPane controlBox = new DialogControlPane();
            okBtn = controlBox.getOKBtn();
            cancelBtn = controlBox.getCancelBtn();
            getContentPane().add(contentPane, BorderLayout.CENTER);
            getContentPane().add(controlBox, BorderLayout.SOUTH);
            setSize(370, 220);
            setLocationRelativeTo(getOwner());
            installListeners();
        }
        
        private void installListeners() {
            okBtn.setEnabled(false);
            textField.getDocument().addDocumentListener(new DocumentListener() {
                public void removeUpdate(DocumentEvent e) {
                    validateOkBtn();
                }
                
                public void insertUpdate(DocumentEvent e) {
                    validateOkBtn();
                }
                
                public void changedUpdate(DocumentEvent e) {
                    validateOkBtn();
                }
            });
            
            okBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = true;
                    dispose();
                }
            });
            
            cancelBtn.addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
        }
        
        private void validateOkBtn() {
            String text = textField.getText().trim();
            okBtn.setEnabled(text.length() > 0);
        }
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
    
    private void searchDiseases() {
        if (codeToDisease == null)
            return;
        SearchDialog dialog = new SearchDialog();
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked)
            return;
        List<DiseaseData> found = new ArrayList<DiseaseData>();
        String key = dialog.textField.getText().trim().toLowerCase();
        if (dialog.wholeNameBox.isSelected()) {
            for (DiseaseData disease : codeToDisease.values()) {
                if (disease.getMatchedDiseaseTerm().toLowerCase().equals(key)) {
                    found.add(disease);
                }
            }
        }
        else {
            for (DiseaseData disease : codeToDisease.values()) {
                if (disease.getMatchedDiseaseTerm().toLowerCase().contains(key))
                    found.add(disease);
            }
        }
        if (found.size() == 0) {
            JOptionPane.showMessageDialog(null,
                                          "Cannot find any disease for: " + dialog.textField.getText().trim() + ".",
                                          "Search Diseases",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Need to select
        Set<TreeNode> foundNodes = new HashSet<TreeNode>();
        JTree tree = displayPane.diseaseTree;
        for (DiseaseData disease : found) {
            @SuppressWarnings("unchecked")
            List<TreeNode> nodes = TreeUtilities.searchNodes(disease, 
                                                             tree);
            if (nodes != null)
                foundNodes.addAll(nodes);
        }
        // This is hard-coded for performance reason
        int size = foundNodes.size();
        if (size > 100) {
            JOptionPane.showMessageDialog(tree,
                                          found.size() + " diseases have been found in " + foundNodes.size() + " tree nodes.\n" +
                                          "For performance reason, only 100 tree nodes will be highlighted.",
                                          "Too Many Hits",
                                          JOptionPane.INFORMATION_MESSAGE);
            size = 100;
        }
        tree.clearSelection();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        // The first path should be visible
        TreePath firstPath = null;
        int count = 0;
        for (TreeNode node : foundNodes) {
            TreePath path = new TreePath(model.getPathToRoot(node));
            if (firstPath == null)
                firstPath = path;
            //eventTree.expandPath(path);
            tree.addSelectionPath(path);
            count ++;
            if (count == size)
                break;
        }
        if (firstPath != null) 
            tree.scrollPathToVisible(firstPath);
    }
    
    private class DiseaseDisplayPane extends JPanel implements CytoPanelComponent {
        private JTree diseaseTree;
        private JTextArea definitionTA;

        public DiseaseDisplayPane() {
            init();
            FIPlugInHelper r = FIPlugInHelper.getHelper();
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            ServiceRegistration servReg = context.registerService(CytoPanelComponent.class.getName(), this, new Properties());
            //The above returns null. Attempts to cache it have been futile.

        }
        
        private List<DiseaseData> searchTopDiseases(Map<String, DiseaseData> codeToDisease) {
            List<DiseaseData> top = new ArrayList<DiseaseData>();
            for (DiseaseData disease : codeToDisease.values()) {
                if (disease.getSupTerms() == null || disease.getSupTerms().size() == 0)
                    top.add(disease);
            }
            sortDiseases(top);
            return top;
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
            FIPlugInHelper r = FIPlugInHelper.getHelper();
            ImageIcon icon = PlugInObjectManager.getManager().createImageIcon("Bullet1.gif");
            // Use same icons for all states
            renderer.setIcon(icon);
            renderer.setLeafIcon(icon);
            FIPlugInHelper r1 = FIPlugInHelper.getHelper();
            ImageIcon folderIcon = PlugInObjectManager.getManager().createImageIcon("Bullet.gif");
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
                        selectGenesForDisease(disease, view);
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
        public void showDiseases(Map<String, DiseaseData> codeToDisease) {
            List<DiseaseData> top = searchTopDiseases(codeToDisease);
            // Try to find Disease_and_Disorders
            DiseaseData first = top.get(0);
            // There should be only one
            // Try to display diseases only.
            for (DiseaseData disease : first.getSubTerms()) {
                if (disease.getNciDiseaseConceptCode().equals("C2991")) {
                    top.clear();
                    top.add(disease);
                    break;
                }
            }
            // Construct a tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            buildTree(root, top);
            DefaultTreeModel model = new DefaultTreeModel(root);
            diseaseTree.setModel(model);
            expandFirstLevel(diseaseTree,
                    root);
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
                    if (e.isPopupTrigger())
                     doTreePopup(e, tree);
                }
                public void mouseReleased(MouseEvent e)
                {
                     if (e.isPopupTrigger())
                     doTreePopup(e, tree);
                }
            });
        }
        
        protected void doTreePopup(MouseEvent e,final JTree tree)
        {
            // TODO Auto-generated method stub
            JPopupMenu popup = null;
            int selected = tree.getSelectionCount();
            if (selected == 1) {
                final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
                popup = new JPopupMenu();
                JMenuItem openItem = new JMenuItem("Expand All");
                openItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        TreeUtilities.expandAllNodes(treeNode, tree);
                    }
                });
                popup.add(openItem);
                JMenuItem closeItem = new JMenuItem("Collapse All");
                closeItem.addActionListener(new ActionListener() {
                    
                    public void actionPerformed(ActionEvent e) {
                        TreeUtilities.collapseAllNodes(treeNode, tree);
                    }
                });
                popup.add(closeItem);
            }
            if (popup == null)
                popup = new JPopupMenu();
            else
                popup.addSeparator();
            JMenuItem searchItem = new JMenuItem("Search Diseases");
            searchItem.addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent e) {
                    searchDiseases();
                }
            });
            popup.add(searchItem);
            if (popup != null)
                popup.show(tree, e.getX(), e.getY());
        
        }

        private void expandFirstLevel(JTree tree,
                DefaultMutableTreeNode root) {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            for (int i = 0; i < root.getChildCount(); i++) {
                TreeNode childNode = root.getChildAt(i);
                TreePath path = new TreePath(model.getPathToRoot(childNode));
                tree.expandPath(path);
            }
        }
        @Override
        public Component getComponent()
        {
            return this;
        }
        @Override
        public CytoPanelName getCytoPanelName()
        {
            return CytoPanelName.WEST;
        }
        @Override
        public Icon getIcon()
        {
            return null;
        }
        @Override
        public String getTitle()
        {
            return "NCI Diseases";
        }
    }
}
