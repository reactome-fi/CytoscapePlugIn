package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.reactome.cancerindex.model.CancerIndexSentenceDisplayFrame;
import org.reactome.cancerindex.model.Sentence;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

public class NodeActionCollection
{

    class GeneCardMenu implements CyNodeViewContextMenuFactory
    {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                final View<CyNode> nodeView)
        {
            JMenuItem geneCardMenuItem = new JMenuItem("Gene Card");
            geneCardMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Long nodeID = nodeView.getModel().getSUID();
                    String id = netView.getModel().getDefaultNodeTable()
                            .getRow(nodeID).get("name", String.class);
                    String url = "http://www.genecards.org/cgi-bin/carddisp.pl?gene="
                            + id;
                    PlugInUtilities.openURL(url);
                }

            });
            return new CyMenuItem(geneCardMenuItem, 3.0f);
        }
    }

    class FetchFIsMenu implements CyNodeViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                final View<CyNode> nodeView)
        {
            JMenuItem fetchFIsItem = new JMenuItem("Fetch FIs");
            fetchFIsItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    fetchFIsForNode(netView, nodeView);
                }

            });

            return new CyMenuItem(fetchFIsItem, 1.0f);
        }

    }

    private void fetchFIsForNode(CyNetworkView netView, View<CyNode> nodeView)
    {
        CyTable nodeTable = netView.getModel().getDefaultNodeTable();
        Long nodeSUID = nodeView.getModel().getSUID();
        String nodeName = nodeTable.getRow(nodeSUID).get("name", String.class);
        try
        {
            RESTFulFIService fiService = new RESTFulFIService(netView);
            Set<String> fiPartners = fiService.queryFIsForNode(nodeName);
            if (fiPartners.isEmpty())
            {
                PlugInUtilities
                        .showErrorMessage("Error in Fetching FIs",
                                "Interactions for " + nodeName
                                        + " could not be found.");
                return;
            }
            displayNodeFIs(nodeName, fiPartners, netView);
        }
        catch (Exception e)
        {
            PlugInUtilities.showErrorMessage("Error in Fetching FIs",
                    "Please see the error log for details.");
        }
    }

    private void displayNodeFIs(final String name, Set<String> partners,
            final CyNetworkView view)
    {
        FIPartnersDialog dialog = new FIPartnersDialog(name, partners, view);
        FIPlugInHelper r = FIPlugInHelper.getHelper();
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(desktopApp.getJFrame());
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private class FIPartnersDialog extends JDialog
    {
        private JLabel inLabel;
        private JLabel outLabel;
        private JList inList;
        private JList outList;
        private List<String> partnersInNetwork;
        private List<String> partnersNotInNetwork;
        private JButton addPartnersBtn;

        public FIPartnersDialog(String name, Set<String> partners,
                CyNetworkView networkView)
        {
            init(name, partners, networkView);
        }

        private void init(final String name, Set<String> partners,
                final CyNetworkView view)
        {
            // Create a dialog to display FI partners
            JPanel contentPane = new JPanel();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            JLabel label = new JLabel();
            label.setText("Total FI parnters for \"" + name + "\": "
                    + partners.size());
            contentPane.add(label);
            partnersInNetwork = new ArrayList<String>();
            partnersNotInNetwork = new ArrayList<String>();
            splitPartners(partners, partnersInNetwork, partnersNotInNetwork,
                    view);
            contentPane.add(Box.createVerticalStrut(6));
            // List the node partners which are already in the network
            inLabel = new JLabel("Partners in network: "
                    + partnersInNetwork.size());
            contentPane.add(inLabel);
            if (partnersInNetwork.size() > 0)
            {
                inList = new JList();
                DefaultListModel model = new DefaultListModel();
                for (String id : partnersInNetwork)
                {
                    model.addElement(id);
                }
                inList.setModel(model);
                inList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
                inList.setVisibleRowCount(-1);
                inList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                JScrollPane listScroller = new JScrollPane(inList);
                listScroller.setPreferredSize(new Dimension(300, 40));
                listScroller.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentPane.add(listScroller);
            }
            contentPane.add(Box.createVerticalStrut(6));
            outLabel = new JLabel("Partners not in network: "
                    + partnersNotInNetwork.size());
            contentPane.add(outLabel);
            if (partnersNotInNetwork.size() > 0)
            {
                outList = new JList();
                DefaultListModel model = new DefaultListModel();
                for (String id : partnersNotInNetwork)
                {
                    model.addElement(id);
                }
                outList.setModel(model);
                outList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
                outList.setVisibleRowCount(-1);
                JScrollPane listScroller = new JScrollPane(outList);
                listScroller.setPreferredSize(new Dimension(300, 40));
                listScroller.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentPane.add(listScroller);
                final JButton addPartnersBtn = new JButton(
                        "Add selected partner(s) to network");
                contentPane.add(addPartnersBtn);
                addPartnersBtn.setEnabled(false);
                outList.addListSelectionListener(new ListSelectionListener()
                {
                    @Override
                    public void valueChanged(ListSelectionEvent e)
                    {
                        if (outList.getSelectedValue() != null)
                        {
                            addPartnersBtn.setEnabled(true);
                        }
                        else
                        {
                            addPartnersBtn.setEnabled(false);
                        }
                    }
                });
                addPartnersBtn.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        addSelectedPartnersToNetwork(name, view);
                    }
                });
            }
            JPanel controlPane = new JPanel();
            controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            JButton closeBtn = new JButton("Close");
            controlPane.add(closeBtn);
            setTitle("FI Partners for " + name);
            getContentPane().add(contentPane, BorderLayout.CENTER);
            Border border = BorderFactory.createCompoundBorder(BorderFactory
                    .createEtchedBorder(), BorderFactory.createEmptyBorder(8,
                    8, 8, 8));
            contentPane.setBorder(border);
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            controlPane.setBorder(BorderFactory.createEtchedBorder());
            closeBtn.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    dispose();
                }
            });
        }

        private void splitPartners(Set<String> partners,
                List<String> partnersInNetwork,
                List<String> partnersNotInNetwork, CyNetworkView networkView)
        {
            // TODO
            Set<String> displayed = new HashSet<String>();
            CyTable nodeTable = networkView.getModel().getDefaultNodeTable();
            for (CyNode node : networkView.getModel().getNodeList())
            {
                String name = nodeTable.getRow(node.getSUID()).get("name",
                        String.class);
                displayed.add(name);
            }
            for (String partner : partners)
            {
                if (displayed.contains(partner))
                {
                    partnersInNetwork.add(partner);
                }
                else
                {
                    partnersNotInNetwork.add(partner);
                }
            }
            Collections.sort(partnersInNetwork);
            Collections.sort(partnersNotInNetwork);
        }

        private void addSelectedPartnersToNetwork(String name,
                CyNetworkView view)
        {
            FINetworkGenerator generator = new FINetworkGenerator();
            Set<String> partners = new HashSet<String>();
            
           // List<Object> selected = outList.getSelectedValuesList();
            //The below method is deprecated. When Apple updates to Java 1.7
            //the above should be used instead. TODO
            @SuppressWarnings("deprecation")
            Object [] selected = outList.getSelectedValues(); 
            for (Object s : selected)
            {
                partners.add(s.toString());
            }
            generator.addFIPartners(name, partners, view);
            // Only add FIs between new nodes and existing node.
            Set<String> existing = new HashSet<String>();
            CyTable nodeTable = view.getModel().getDefaultNodeTable();
            for (CyNode node : view.getModel().getNodeList())
            {
                Long nodeSUID = node.getSUID();
                existing.add(nodeTable.getRow(nodeSUID).get("name",
                        String.class));
            }
            existing.remove(name);
            RESTFulFIService fiService = new RESTFulFIService(view);
            try
            {
                Set<String> interactions = fiService.queryFIsBetween(partners,
                        existing);
                generator.addFIs(interactions, view);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                PlugInUtilities
                        .showErrorMessage("Error in Fetching FIs",
                                "FIs for the given node could not be fetched. Please check the error logs.");
            }
            // Update the labels and lists in the GUI
            partnersNotInNetwork.removeAll(partners);
            partnersInNetwork.addAll(partners);
            Collections.sort(partnersNotInNetwork);
            Collections.sort(partnersInNetwork);
            updateAllUIs();
        }

        @SuppressWarnings("rawtypes")
        private void updateAllUIs()
        {
            inLabel.setText("Partners in network: " + partnersInNetwork.size());
            inList.clearSelection();
            DefaultListModel model = (DefaultListModel) inList.getModel();
            model.removeAllElements();
            for (String id : partnersInNetwork)
            {
                model.addElement(id);
            }
            outLabel.setText("Partners not in network: "
                    + partnersNotInNetwork.size());
            if (outList != null)
            {
                outList.clearSelection();
                model = (DefaultListModel) outList.getModel();
                model.removeAllElements();
                for (String id : partnersNotInNetwork)
                {
                    model.addElement(id);
                }
                if (addPartnersBtn != null)
                {
                    addPartnersBtn.setEnabled(false);
                }
            }
        }
    }

    class CancerGeneIndexMenu implements CyNodeViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView, final View<CyNode> nodeView)
        {
            JMenuItem getCancerGeneIndexItem = new JMenuItem(
                    "Fetch Cancer Gene Index");
            getCancerGeneIndexItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                   loadCancerGeneIndex(netView, nodeView);
                }
            });
            return new CyMenuItem(getCancerGeneIndexItem, 2.0f);
        }

    }
    private void loadCancerGeneIndex(final CyNetworkView netView, final View<CyNode> nodeView)
    {
        Thread t = new Thread()
        {
            public void run()
            {
                try
                {
                    RESTFulFIService fiService = new RESTFulFIService(netView);
                    String geneName = netView.getModel().getDefaultNodeTable().getRow(nodeView.getModel().getSUID()).get("name", String.class);
                    List<Sentence> sentences = fiService.queryCGIAnnotations(geneName);
                    displayCGISentences(geneName, sentences);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    PlugInUtilities.showErrorMessage("Error in Fetching CGI", "The cancer gene indices could not be fetched.");
                }
            }
        };
        t.start();
    }
    private synchronized void displayCGISentences(String gene, List<Sentence> sentences)
    {
        if (sentences == null || sentences.isEmpty())
        {
            FIPlugInHelper r = FIPlugInHelper.getHelper();
            CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
            JOptionPane.showMessageDialog(desktopApp.getJFrame(), "No cancer gene index annotations were found for \"" + gene + "\".", "No Annotations for Gene", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        FIPlugInHelper r = FIPlugInHelper.getHelper();
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CancerIndexSentenceDisplayFrame cgiFrame = FIPlugInHelper.getHelper().getCancerIndexFrame(desktopApp.getJFrame());
        cgiFrame.display(sentences, gene);
    }
}
