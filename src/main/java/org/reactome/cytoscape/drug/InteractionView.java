/*
 * Created on Dec 23, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

import edu.ohsu.bcb.druggability.ExpEvidence;
import edu.ohsu.bcb.druggability.Interaction;
import edu.ohsu.bcb.druggability.Source;

/**
 * A customized JPanel to show details of a drug/target interaction.
 * @author gwu
 *
 */
public class InteractionView extends JDialog {
    private JEditorPane htmlPane;
    
    /**
     * Default constructor.
     */
    public InteractionView() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    private void init() {
        htmlPane = new JEditorPane();
        htmlPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        htmlPane.setEditable(false);
        htmlPane.setContentType("text/html");
        htmlPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String desc = e.getDescription();
                    processLink(desc);
                }
            }
        }); 
        getContentPane().add(new JScrollPane(htmlPane), 
                             BorderLayout.CENTER);
        
        JPanel controlPane = new JPanel();
        JButton closeBtn = new JButton("Close");
        controlPane.add(closeBtn);
        closeBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        setSize(500, 600);
        setLocationRelativeTo(getOwner());
    }
    
    public void setInteraction(Interaction interaction) {
        String title = interaction.getIntDrug().getDrugName() + " - " + interaction.getIntTarget().getTargetName();
        setTitle(title);
        displayInteraction(interaction);
    }
    
    private void displayInteraction(Interaction interaction) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body>");
        builder.append("<table width=\"100%\" border=\"1\">");
        String drug = "<a href=\"drug:" + interaction.getIntDrug().getDrugName() + "\">" + interaction.getIntDrug().getDrugName() + "</a>";
        PlugInUtilities.fillTableRow("Drug", drug, builder);
        String target = "<a href=\"target:" + interaction.getIntTarget().getTargetName() + "\">" + interaction.getIntTarget().getTargetName() + "</a>";
        PlugInUtilities.fillTableRow("Target", target, builder);
        if (interaction.getInteractionType() != null)
            PlugInUtilities.fillTableRow("Interaction type", interaction.getInteractionType(), builder);
        displayInteractionSources(interaction,
                                  builder);
        displayExpEvidences(interaction, builder);
        
        builder.append("</body></html>");
        htmlPane.setText(builder.toString());
        htmlPane.setCaretPosition(0); // Want to start from the first line.
    }
    
    private String generateKeyForExpEvidence(ExpEvidence evidence) {
        StringBuilder text = new StringBuilder();
        String type = evidence.getAssayType();
        if (type.equals("Kd"))
            type = "KD"; // Fix some error for merging
        text.append(evidence.getAssayType());
        text.append(evidence.getAssayRelation());
        if (evidence.getAssayValueMedian() != null)
            text.append(DrugTargetInteractionManager.getManager().getExpEvidenceValue(evidence)); // Want to use double to avoid weird many zero
        else // We should get low and high value
            text.append(" [").append(evidence.getAssayValueLow()).append(", ").append(evidence.getAssayValueHigh()).append("]");
        return text.toString();
    }

    private void displayExpEvidences(Interaction interaction, StringBuilder builder) {
        Set<ExpEvidence> exps = interaction.getExpEvidenceSet();
        if (exps == null || exps.size() == 0)
            return;
        Map<ExpEvidence, Set<Source>> repExpToSources = mergeExpEvidences(exps);
        
        builder.append("<tr>");
        PlugInUtilities.fillTableRowHeader("Affinity (nM)",
                                           builder,
                                           exps.size());
        List<ExpEvidence> list = new ArrayList<>(repExpToSources.keySet());
        Collections.sort(list, new Comparator<ExpEvidence>() {
            public int compare(ExpEvidence exp1, ExpEvidence exp2) {
                int rtn = exp1.getAssayType().toUpperCase().compareTo(exp2.getAssayType().toUpperCase());
                if (rtn != 0)
                    return rtn;
                Number value1 = DrugTargetInteractionManager.getManager().getExpEvidenceValue(exp1);
                Number value2 = DrugTargetInteractionManager.getManager().getExpEvidenceValue(exp2);
                return new Double(value1.doubleValue()).compareTo(new Double(value2.doubleValue()));
            }
        });
        StringBuilder text = new StringBuilder();
        int c = 0;
        for (ExpEvidence evidence : list) {
            if (c > 0)
                builder.append("<tr>");
            String key = generateKeyForExpEvidence(evidence);
            text.append(key);
            Set<Source> sources = repExpToSources.get(evidence);
            if (sources != null)
                generateTextForSources(sources, text, false);
            PlugInUtilities.fillTableRowValue(text.toString(), builder);
            text.setLength(0);
            builder.append("</tr>");
            c ++;
        }
    }

    private Map<ExpEvidence, Set<Source>> mergeExpEvidences(Set<ExpEvidence> exps) {
        // The following code is used to merge sources for same values.
        // Sort exps based on type and values
        Map<String, ExpEvidence> keyToRep = new HashMap<>();
        Map<ExpEvidence, Set<Source>> repToSources = new HashMap<>();
        for (ExpEvidence evidence : exps) {
            String key = generateKeyForExpEvidence(evidence);
            if (!keyToRep.containsKey(key))
                keyToRep.put(key, evidence);
            if (evidence.getExpSourceSet() == null)
                continue;
            ExpEvidence rep = keyToRep.get(key);
            Set<Source> sources = repToSources.get(rep);
            if (sources == null) {
                sources = new HashSet<>();
                repToSources.put(rep, sources);
            }
            sources.addAll(evidence.getExpSourceSet());
        }
        return repToSources;
    }
    
    private void displayInteractionSources(Interaction interaction, StringBuilder builder) {
        Set<Source> sources = interaction.getInteractionSourceSet();
        if (sources == null || sources.size() == 0)
            return;
        generateTextForSources(sources, builder, true);
    }

    private void generateTextForSources(Set<Source> sources,
                                        StringBuilder builder,
                                        boolean isForInteraction) {
        // Sort references based on database
        Map<String, List<String>> dbToRefs = new HashMap<>();
        for (Source source : sources) {
            String db = source.getSourceDatabase().getDatabaseName();
            if (source.getSourceLiterature() == null)
                continue;
            List<String> refs = dbToRefs.get(db);
            if (refs == null) {
                refs = new ArrayList<>();
                dbToRefs.put(db, refs);
            }
            refs.add(source.getSourceLiterature().getPubMedID());
        }
        if (isForInteraction) {
            builder.append("<tr>");
            PlugInUtilities.fillTableRowHeader("Source (pubmed)", builder, dbToRefs.size());
        }
        else
            builder.append(" (");
        List<String> dbList = new ArrayList<String>(dbToRefs.keySet());
        Collections.sort(dbList);
        int c = 0;
        for (String db : dbList) {
            List<String> refs = dbToRefs.get(db);
            Collections.sort(refs);
            String text = generatePubMedLinks(refs);
            if (c > 0) {
                if (isForInteraction)
                    builder.append("<tr>");
            }
            if (isForInteraction) {
                PlugInUtilities.fillTableRowValue(db + ": " + text, builder);
                builder.append("</tr>");
            }
            else {
                builder.append(db).append(": ").append(text).append("; ");
            }
            c ++;
        }
        if (!isForInteraction) {
            builder.delete(builder.length() - 2, builder.length());
            builder.append(")");
        }
    }
    
    private String generatePubMedLinks(List<String> list) {
        StringBuilder builder = new StringBuilder();
        for (String id : list) {
            if (id.contains("NA"))
                builder.append(id);
            else {
                builder.append("<a href=\"pubmed:" + id + "\">" + id + "</a>");
            }
            builder.append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
        return builder.toString();
    }
    
    private void processLink(String link) {
        int index = link.indexOf(":");
        String type = link.substring(0, index);
        String value = link.substring(index + 1);
        if (type.equals("drug")) {
            // Search Google
            PlugInUtilities.queryGoogle(value);
        }
        else if (type.equals("target"))
            PlugInUtilities.queryGeneCard(value);
        else if (type.equals("pubmed")) {
            String url = "https://www.ncbi.nlm.nih.gov/pubmed/" + value;
            PlugInUtilities.openURL(url);
        }
    }
    
}
