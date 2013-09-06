package org.reactome.cytoscape3;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

/**
 * A class providing a panel for managing the Reactome FI context
 * menu functions, since menu gravity has not been implemented as
 * of Cytoscape 3.0.2.
 * @author Eric T. Dawson
 *
 */
public class ReactomeFIControlPanel extends JPanel implements CytoPanelComponent
{
    public ReactomeFIControlPanel()
    {
        init();
    }
    private void init()
    {
        setLayout(new GridBagLayout());
        
        JPanel topPanel = new JPanel();
        JPanel midPanel = new JPanel();
        JPanel bottomPanel = new JPanel();
        
        JPanel trioPanel = new JPanel();
        
        JPanel nodePanel = new JPanel();
        nodePanel.setLayout(new GridBagLayout());
        JLabel nodeLabel = new JLabel("Nodes");
        String [] nodeActions = {"Fetch FI Partners", "Fetch Cancer Gene Index", "Fetch Gene Card"};
        JComboBox nodeActionBox = new JComboBox(nodeActions);
        JButton goButton1 = new JButton("Go!");
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0.1;
        c.gridwidth = 1;
        nodePanel.add(nodeLabel, c);
        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = 1;
        c2.insets = new Insets(4,4,4,4);
        c2.weightx = 0.1;
        c2.gridwidth = 1;
        c2.anchor = GridBagConstraints.LINE_START;
        nodePanel.add(nodeActionBox, c2);
        GridBagConstraints c3 = new GridBagConstraints();
        c3.anchor = GridBagConstraints.LINE_END;
        c3.gridx = 1;
        c3.gridy = 1;
        c3.weightx = .1;
        c3.gridwidth = 1;
        nodePanel.add(goButton1, c3);
        
        JPanel edgePanel = new JPanel();
        edgePanel.setLayout(new GridBagLayout());
        JLabel edgeLabel = new JLabel("Edges");
        String [] edgeActions = {"Query FI Source"};
        JComboBox edgeActionBox = new JComboBox(edgeActions);
        JButton goButton2 = new JButton("Go!");
        edgePanel.add(edgeLabel, c);
        edgePanel.add(edgeActionBox, c2);
        edgePanel.add(goButton2, c3);
        
        JPanel netPanel = new JPanel();
        JLabel netLabel = new JLabel("Network");
        String [] netActions = {"Pathway Enrichment", "GO Cell Component", "GO Biological Process", "GO Molecular Function", "Fetch Cancer Gene Indices"};
        JComboBox netActionBox = new JComboBox(netActions);
        JButton goButton3 = new JButton("Go!");
        netPanel.setLayout(new GridBagLayout());
        netPanel.add(netLabel, c);
        netPanel.add(netActionBox, c2);
        netPanel.add(goButton3, c3);
        
        trioPanel.setLayout(new BoxLayout(trioPanel, BoxLayout.Y_AXIS));
        Border etchBorder = BorderFactory.createEtchedBorder();
        Font font = new Font("Verdana", Font.BOLD, 13);
        Border titleBorder = BorderFactory.createTitledBorder(etchBorder, "Object Actions", TitledBorder.LEFT, TitledBorder.CENTER, font);
        trioPanel.setBorder(titleBorder);
        trioPanel.add(nodePanel);
        trioPanel.add(edgePanel);
        trioPanel.add(netPanel);
        
        JPanel actionButtonPanel = new JPanel();
        actionButtonPanel.setLayout(new GridBagLayout());
        GridBagConstraints actionConstraints = new GridBagConstraints();
        actionConstraints.anchor = GridBagConstraints.LINE_START;
        actionConstraints.weightx = .1;
        actionConstraints.weighty = 0;
        actionConstraints.gridheight = 1;
        actionConstraints.gridwidth = 1;
        actionConstraints.gridx = 0;
        actionConstraints.gridy = 0;
        JButton annoButton = new JButton("Annotate Network");
        JButton clusterButton = new JButton("Cluster Network");
        JButton styleButton = new JButton("Stylize Network");
        actionButtonPanel.add(annoButton, actionConstraints);
        actionConstraints.gridx = 2;
        actionButtonPanel.add(clusterButton, actionConstraints);
        actionConstraints.gridx = 0;
        actionConstraints.gridy = 1;
        actionButtonPanel.add(styleButton, actionConstraints);
        Border titleBorder2 = BorderFactory.createTitledBorder(etchBorder, "Reactomify My Network!", TitledBorder.LEFT, TitledBorder.CENTER, font);
        actionButtonPanel.setBorder(titleBorder2);
        
        
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(trioPanel);
        topPanel.add(actionButtonPanel);
        
        JPanel modulePanel = new JPanel();
        JLabel moduleLabel = new JLabel("Modules");
        modulePanel.setLayout(new GridBagLayout());
        String [] moduleActions = {"Pathway Enrichment", "GO Cell Component", "GO Biological Process", "GO Molecular Function"};
        JComboBox moduleActionBox = new JComboBox(moduleActions);
        JButton goButton4 = new JButton("Go!");
        JButton survivalButton = new JButton("Survival Analysis");
        modulePanel.add(moduleLabel, c);
        modulePanel.add(moduleActionBox, c2);
        modulePanel.add(goButton4, c3);
        GridBagConstraints survivalC = new GridBagConstraints();
        survivalC.anchor = GridBagConstraints.LINE_START;
        survivalC.gridx = 0;
        survivalC.gridheight = 1;
        survivalC.gridwidth = 1;
        survivalC.gridx = 0;
        survivalC.gridy = 2;
        modulePanel.add(survivalButton, survivalC);
        Border titleBorder3 = BorderFactory.createTitledBorder(etchBorder, "Module Analysis", TitledBorder.LEFT, TitledBorder.CENTER, font);
        modulePanel.setBorder(titleBorder3);
        topPanel.add(modulePanel);
        try{
        BufferedImage logo = ImageIO.read(new File("Reactome_Logo_sq.png"));
        JLabel picLabel = new JLabel(new ImageIcon(logo));
        midPanel.add(picLabel);
        }
        catch (Throwable t)
        {
            //t.printStackTrace();
        }
        
        
        JButton saveButton = new JButton("Save Session");
        JButton exitButton = new JButton("Exit Reactome FI");
        bottomPanel.add(saveButton);
        bottomPanel.add(exitButton);
                
        GridBagConstraints c4 = new GridBagConstraints();
        c4.gridx = 0;
        c4.gridy = 0;
        c4.weightx = 0.1;
        c4.weighty = 0.1;
        c4.fill = GridBagConstraints.BOTH;
        add(topPanel, c4);
        c4.gridy = GridBagConstraints.RELATIVE;
        add(midPanel, c4);
        add(bottomPanel, c4);
        setVisible(true);
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
        return "Reactome FI";
    }
    
}
