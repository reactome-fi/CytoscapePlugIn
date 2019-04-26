/*
 * Created on Dec 23, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import edu.ohsu.bcb.druggability.dataModel.DatabaseRef;
import edu.ohsu.bcb.druggability.dataModel.Drug;
import edu.ohsu.bcb.druggability.dataModel.ExpEvidence;
import edu.ohsu.bcb.druggability.dataModel.Interaction;
import edu.ohsu.bcb.druggability.dataModel.Source;

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

    public InteractionView(Window owner) {
        super(owner);
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
    
    private String getDrugAccessUrl(Interaction interaction) {
        Set<Source> sources = interaction.getInteractionSourceSet();
        if (sources == null || sources.size() == 0)
            return null;
        for (Source source : sources) {
            DatabaseRef ref = source.getSourceDatabase();
            if (ref == null)
                continue;
            String dbName = ref.getDatabaseName();
            // Don't call the following to avoid No enum exception
//            DrugDataSource ds = DrugDataSource.valueOf(dbName);
            DrugDataSource ds = DrugDataSource.getDataSource(dbName);
            if (ds != null && ds.getAccessUrl() != null)
                return ds.getAccessUrl();
        }
        return null;
    }
    
    private void displayInteraction(Interaction interaction) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body>");
        builder.append("<table width=\"100%\" border=\"1\">");
        String drug = null;
        String drugAccessUrl = getDrugAccessUrl(interaction);
        if (drugAccessUrl == null)
            drug = "<a href=\"drug:" + interaction.getIntDrug().getDrugName() + "\">" + interaction.getIntDrug().getDrugName() + "</a>";
        else {
            Drug drugObj = interaction.getIntDrug();
            drug = "<a href=\"" + drugAccessUrl + drugObj.getDrugID() + "\">" + drugObj.getDrugName() + "</a>";
        }
        PlugInUtilities.fillTableRow("Drug", drug, builder);
        String target = "<a href=\"target:" + interaction.getIntTarget().getTargetName() + "\">" + interaction.getIntTarget().getTargetName() + "</a>";
        PlugInUtilities.fillTableRow("Target", target, builder);
        String type = interaction.getInteractionType();
        if (type != null && type.trim().length() > 0) // Type may be empty
            PlugInUtilities.fillTableRow("Interaction type", interaction.getInteractionType(), builder);
        displayInteractionSources(interaction,
                                  builder);
        displayExpEvidences(interaction, builder);
        displayComments(interaction, builder);
        
        builder.append("</body></html>");
        htmlPane.setText(builder.toString());
        htmlPane.setCaretPosition(0); // Want to start from the first line.
        
        if (interaction.getExpEvidenceSet() == null || interaction.getExpEvidenceSet().size() < 10)
            setSize(getWidth(), getHeight() / 2);
    }
    
    private String generateKeyForExpEvidence(ExpEvidence evidence) {
        String type = evidence.getAssayType();
        if (type == null)
            return null;
        StringBuilder text = new StringBuilder();
        if (type.equals("Kd"))
            type = "KD"; // Fix some error for merging
        text.append(type);
        String relation = evidence.getAssayRelation();
        if (relation == null || relation.trim().length() == 0)
            text.append("?");
        else
            text.append(relation);
        if (evidence.getAssayValueMedian() != null) {
            DrugTargetInteractionManager r = DrugTargetInteractionManager.getManager();
            text.append(evidence.getAssayValue());
        } else // We should get low and high value
            text.append(" [").append(evidence.getAssayValueLow()).append(", ").append(evidence.getAssayValueHigh()).append("]");
        return text.toString();
    }
    
    private void displayComments(Interaction interaction, StringBuilder builder) {
        Set<ExpEvidence> exps = interaction.getExpEvidenceSet();
        if (exps == null || exps.size() == 0)
            return;
        List<String> comments = exps.stream()
                .filter(e -> e.getAssayDescription() != null)
                .filter(e -> e.getAssayDescription().length() > 0)
                .map(e -> e.getAssayDescription())
                .filter(c -> !c.equals("NA"))
                .collect(Collectors.toList());
        if (comments.size() == 0)
            return;
        builder.append("<tr>");
        PlugInUtilities.fillTableRowHeader("Comment",
                builder,
                exps.size());
        comments.forEach(c -> {
            builder.append("<td>").append(c).append("</td>");
        });
        builder.append("</tr>");
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
                DrugTargetInteractionManager r = DrugTargetInteractionManager.getManager();
                Number value1 = exp1.getAssayValue();
                DrugTargetInteractionManager r1 = DrugTargetInteractionManager.getManager();
                Number value2 = exp2.getAssayValue();
                return new Double(value1.doubleValue()).compareTo(new Double(value2.doubleValue()));
            }
        });
        StringBuilder text = new StringBuilder();
        int c = 0;
        for (ExpEvidence evidence : list) {
            String key = generateKeyForExpEvidence(evidence);
            if (key == null)
                continue;
            if (c > 0)
                builder.append("<tr>");
            // Need to be encoded since < or >
            key = key.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            text.append(key);
            Set<Source> sources = repExpToSources.get(evidence);
            if (sources != null)
                generateTextForSources(sources, text);
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
        // Show the original databases only for interaction
        List<String> databases = interaction.getInteractionSourceSet()
                                            .stream()
                                            .filter(source -> source.getSourceDatabase() != null)
                                            .map(source -> source.getSourceDatabase())
                                            .filter(db -> db.getDatabaseName() != null)
                                            .map(db -> db.getDatabaseName())
                                            .collect(Collectors.toSet())
                                            .stream()
                                            .sorted()
                                            .collect(Collectors.toList());
        if (databases == null || databases.size() == 0)
            return;
        builder.append("<tr>");
        PlugInUtilities.fillTableRowHeader("Source", builder, databases.size());
        for (int i = 0; i < databases.size(); i++) {
            if (i > 0)
                builder.append("<tr>");
            PlugInUtilities.fillTableRowValue(databases.get(i), builder);
            builder.append("</tr>");
        }
    }
    
    private void generateTextForSources(Set<Source> sources,
                                        StringBuilder builder) {
        // Sort references based on database
        Map<String, List<String>> dbToRefs = new HashMap<>();
        Map<String, Set<String>> dbToUrls = new HashMap<>();
        for (Source source : sources) {
            String db = source.getSourceDatabase().getDatabaseName();
            if (source.getSourceLiterature() == null) {
                DatabaseRef dbRef = source.getSourceDatabase();
                if (dbRef.getDownloadURL() != null)
                    dbToUrls.compute(db, (key, set) -> {
                       if (set == null)
                           set = new HashSet<>();
                       set.add(dbRef.getDownloadURL());
                       return set;
                    });
                continue;
            }
            List<String> refs = dbToRefs.get(db);
            if (refs == null) {
                refs = new ArrayList<>();
                dbToRefs.put(db, refs);
            }
            refs.add(source.getSourceLiterature().getPubMedID());
        }
        builder.append(" (");
        List<String> dbList = new ArrayList<String>(dbToRefs.keySet());
        Collections.sort(dbList);
        for (String db : dbList) {
            List<String> refs = dbToRefs.get(db);
            Collections.sort(refs);
            String text = generatePubMedLinks(refs);
            builder.append(db).append(": ").append(text).append("; ");
        }
        dbList = dbToUrls.keySet().stream().sorted().collect(Collectors.toList());
        for (String db : dbList) {
            Set<String> urls = dbToUrls.get(db);
            generateUrl(urls, builder);
            builder.append("; ");
        }
        builder.delete(builder.length() - 2, builder.length());
        builder.append(")"); // Close the parenthesis
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
    
    private void generateUrl(Collection<String> urls, StringBuilder builder) {
        urls.forEach(url -> {
            builder.append("<a href=\"")
                   .append(url)
                   .append("\">")
                   .append(url)
                   .append("</a>")
                   .append("; ");
        });
        builder.delete(builder.length() - 2, builder.length());
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
        else if (link.startsWith("http"))
            PlugInUtilities.openURL(link);
            
    }
    
}
