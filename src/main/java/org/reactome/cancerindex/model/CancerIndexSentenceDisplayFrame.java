/*
 * Created on Sep 23, 2010
 *
 */
package org.reactome.cancerindex.model;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.reactome.cytoscape3.PlugInScopeObjectManager;


/**
 * This customized JPanel is used to display a list of Sentence objects.
 * @author wgm
 *
 */
public class CancerIndexSentenceDisplayFrame extends JFrame {
    private JLabel titleLabel;
    private JEditorPane sentencePane;
    private NavigationPane navigationPane;
    private JComboBox orderBox;
    private JTextField filterTF; 
    private int entriesPerPage = 20;
    // Sentences in display
    private List<Sentence> originalList;
    private List<Sentence> sentences;
    
    public CancerIndexSentenceDisplayFrame() {
        init();
    }
    
    private void init() {
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        JPanel otherPanel = new JPanel();
        otherPanel.setLayout(new BorderLayout());
        String title = new String("Cancer Gene Index Annotations");
        titleLabel = new JLabel(title);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        Font font = content.getFont();
        font = font.deriveFont(Font.BOLD, 16);
        titleLabel.setFont(font);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        otherPanel.add(titleLabel, BorderLayout.NORTH);
        // Used to control the display
        JPanel controlPane = new JPanel();
        controlPane.setBorder(BorderFactory.createEtchedBorder());
        controlPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel orderByLabel = new JLabel("Order by ");
        controlPane.add(orderByLabel);
        String[] orders = new String[] {
                "PubMedID",
                "Cancer Type",
                "Status",
        };
        orderBox = new JComboBox(orders);
        orderBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {// So that only one sorting is needed
                    sortSentences();
                    // Need to re-display sentences
                    display(sentences,
                            navigationPane.startEntry);
                }
            }
        });
        controlPane.add(orderBox);
        JButton useFilterBtn = new JButton("Set Filters");
        controlPane.add(useFilterBtn);
        JLabel filterLabel = new JLabel("Current filters:");
        filterTF = new JTextField("None");
        filterTF.setEditable(false);
        filterTF.setColumns(20);
        controlPane.add(filterLabel);
        controlPane.add(filterTF);
        useFilterBtn.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                setFilters();
            }
        });
        
        otherPanel.add(controlPane, BorderLayout.SOUTH);
        content.add(otherPanel, BorderLayout.NORTH);
        // Used to navigation
        JPanel mainPane = new JPanel();
        mainPane.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
        mainPane.setLayout(new BorderLayout());
        navigationPane = new NavigationPane();
        mainPane.add(navigationPane, BorderLayout.NORTH);
        // Display for text
        sentencePane = new JEditorPane();
        sentencePane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        sentencePane.setContentType("text/html");
        sentencePane.setEditable(false);
        sentencePane.addHyperlinkListener(new HyperlinkListener() {
            
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String url = e.getURL().toString();
                    try
		    {
			Desktop.getDesktop().browse(java.net.URI.create(url));
		    } catch (IOException e1)
		    {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		    }
                }
            }
        });
        JScrollPane jsp = new JScrollPane(sentencePane);
        mainPane.add(jsp, BorderLayout.CENTER);
        content.add(mainPane, BorderLayout.CENTER);
        
        getContentPane().add(content, BorderLayout.CENTER);
    }
    
    private void setFilters() {
        FilterDialog dialog = new FilterDialog(this);
        dialog.setSize(400, 300);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(this);
        dialog.setFiltersFromText(filterTF.getText());
        dialog.setVisible(true);
        if (!dialog.isOkClicked)
            return;
        String filters = dialog.getFiltersText();
        filterTF.setText(filters);
        filterTF.setCaretPosition(0);
        doFiltering();
        display(sentences, 0);
    }
    
    private void doFiltering() {
        if (sentences == null)
            sentences = new ArrayList<Sentence>();
        else
            sentences.clear();
        String filters = filterTF.getText();
        if (filters.length() == 0 || filters.equals("None")) {
            sentences.addAll(originalList);
            return;
        }
        String[] tokens = filters.split("; ");
        String cancer = null;
        String status = null;
        String negation = null;
        String celline = null;
        boolean contains = false;
        for (String token : tokens) {
            String[] nameValue = token.split(": ");
            if (nameValue[0].equals("Cancer type"))
                cancer = nameValue[1].toLowerCase();
            else if (nameValue[0].equals("Status"))
                status = nameValue[1];
            else if (nameValue[0].equals("Negation"))
                negation = nameValue[1];
            else if (nameValue[0].equals("Cellline"))
                celline = nameValue[1];
            else if (nameValue[0].equals("contains"))
                contains = nameValue[1].equals("checked");
        }
        for (Sentence s : originalList) {
            // Check simple one first
            if (status != null && 
                (s.getSentenceStatusFlag() == null ||
                !s.getSentenceStatusFlag().equals(status)))
                continue;
            if (negation != null && 
                (s.getNegationIndicator() == null ||    
                !s.getNegationIndicator().equals(negation)))
                continue;
            if (celline != null && 
                (s.getCellineIndicator() == null ||
                !s.getCellineIndicator().equals(celline)))
                continue;
            if (cancer != null) {
                String sCancer = s.getDiseaseData().getMatchedDiseaseTerm().toLowerCase();
                if (contains) {
                    if (!sCancer.contains(cancer))
                        continue;
                }
                else if (!sCancer.equalsIgnoreCase(cancer))
                    continue;
            }
            sentences.add(s);
        }
        sortSentences();
    }
    
    private void sortSentences() {
        String type = orderBox.getSelectedItem().toString();
        Comparator<Sentence> sorter = null;
        if (type.equals("PubMedID")) {
            sorter = new Comparator<Sentence>() {
                public int compare(Sentence s1, Sentence s2) {
                    String id1 = s1.getPubMedID();
                    if (id1 == null)
                        id1 = "0";
                    String id2 = s2.getPubMedID();
                    if (id2 == null)
                        id2 = "0";
                    Integer i1 = new Integer(id1);
                    Integer i2 = new Integer(id2);
                    return i2.compareTo(i1); // In reverse order so that latest can be displayed at the top
                }
            };
        }
        else if (type.equals("Cancer Type")) {
            sorter = new Comparator<Sentence>() {
                public int compare(Sentence s1, Sentence s2) {
                    String type1 = s1.getDiseaseData().getMatchedDiseaseTerm();
                    if (type1 == null)
                        type1 = "";
                    String type2 = s2.getDiseaseData().getMatchedDiseaseTerm();
                    if (type2 == null)
                        type2 = "";
                    return type1.compareTo(type2);
                }
            };
        }
        else if (type.equals("Status")) {
            sorter = new Comparator<Sentence>() {
                public int compare(Sentence s1, Sentence s2) {
                    String status1 = s1.getSentenceStatusFlag();
                    if (status1 == null)
                        status1 = "";
                    String status2 = s2.getSentenceStatusFlag();
                    if (status2 == null)
                        status2 = "";
                    return status1.compareTo(status2);
                }
            };
        }
        if (sorter != null)
            Collections.sort(sentences, sorter);
    }
    
    public void setEntriesPerPage(int entries) {
        this.entriesPerPage = entries;
    }
    
    public int getEntriesPerPage() {
        return this.entriesPerPage;
    }
    
    private void setGeneInTitle(String gene) {
        titleLabel.setText("Cancer Gene Index Annotations for \"" + gene + "\"");
    }
    
    public void display(List<Sentence> sentences,
                        String gene) {
        setGeneInTitle(gene);
        this.originalList = sentences;
        doFiltering();
        sortSentences();
        display(this.sentences, 0);
    }
    
    private void display(List<Sentence> sentences, int startIndex) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body>");
        int max = startIndex + entriesPerPage;
        if (max > sentences.size())
            max = sentences.size();
        for (int i = startIndex; i < max; i++) {
            Sentence sentence = sentences.get(i);
            generateSentenceHTML(builder,
                                 sentence);
            builder.append("<hr><p>");
        }
        builder.append("</body></html>");
        sentencePane.setText(builder.toString());
        sentencePane.setCaretPosition(0);
        navigationPane.showResults(startIndex);
        
        int currentPage = startIndex / entriesPerPage + 1;
        int totalPageMinus1 = (int) Math.floor((double) sentences.size() / entriesPerPage);
        navigationPane.pageLabel.setText("Page " + currentPage + " of " + (totalPageMinus1 + 1));
    }

    private void generateSentenceHTML(StringBuilder builder, Sentence sentence) {
        DiseaseData disease = sentence.getDiseaseData();
        builder.append("<b>Cancer type: </b>").append(disease.getMatchedDiseaseTerm()).append("<br>");
        if (sentence.getRoles() != null && sentence.getRoles().size() > 0) {
            Set<String> ncrRoles = new HashSet<String>();
            Set<String> otherRoles = new HashSet<String>();
            for (Roles role : sentence.getRoles()) {
                if (role.getPrimaryNCIRoleCode() != null)
                    ncrRoles.addAll(role.getPrimaryNCIRoleCode());
                if (role.getOtherRole() != null)
                    otherRoles.addAll(role.getOtherRole());
            }
            if (ncrRoles.size() > 0) {
                List<String> list = new ArrayList<String>(ncrRoles);
                Collections.sort(list);
                builder.append("<b>Primary NCI role code: </b>");
                for (Iterator<String> it = ncrRoles.iterator(); it.hasNext();) {
                    builder.append(it.next());
                    if (it.hasNext())
                        builder.append(", ");
                }
                builder.append("<br>");
            }
            if (otherRoles.size() > 0) {
                List<String> list = new ArrayList<String>(otherRoles);
                Collections.sort(list);
                builder.append("<b>Other roles: </b>");
                for (Iterator<String> it = otherRoles.iterator(); it.hasNext();) {
                    builder.append(it.next());
                    if (it.hasNext())
                        builder.append(", ");
                }
                builder.append("<br>");
            }
        }
        List<String> evidences = sentence.getEvidenceCode();
        if (evidences != null && evidences.size() > 0) {
            builder.append("<b>Evidence code: </b>");
            for (Iterator<?> it = evidences.iterator(); it.hasNext();) {
                builder.append(it.next());
                if (it.hasNext())
                    builder.append(", ");
            }
            builder.append("<br>");
        }  
        if (sentence.getNegationIndicator() != null)
            builder.append("<b>Negation indicator: </b>").append(sentence.getNegationIndicator()).append("<br>");
        if (sentence.getCellineIndicator() != null)
            builder.append("<b>Cellline indicator: </b>").append(sentence.getCellineIndicator()).append("<br>");
        if (sentence.getSentenceStatusFlag() != null)
            builder.append("<b>Status: </b>").append(sentence.getSentenceStatusFlag()).append("<br>");
        if (sentence.getPubMedID() != null) {
            String id = sentence.getPubMedID();
            builder.append("<b>PubMedID: </b><a href=\"http://www.ncbi.nlm.nih.gov/pubmed/");
            builder.append(id).append("\">").append(id).append("</a><br>");
        }
        if (sentence.getComments() != null) {
            builder.append("<b>Comment: </b>").append(sentence.getComments()).append("<br>");
        }
        if (sentence.getStatement() != null) {
            builder.append("<br>").append(sentence.getStatement()).append("<br>");
        }
    }
    
    /**
     * This inner customized JPanel is used as navigation control.
     * @author wgm
     *
     */
    private class NavigationPane extends JPanel {
        private JLabel resultLabel;
        private JButton firstBtn;
        private JButton prevBtn;
        private JLabel pageLabel;
        private JButton nextBtn;
        private JButton lastBtn;
        // Track the current start entry
        private int startEntry;
        
        public NavigationPane() {
            init();
        }
        
        private void init() {
            setLayout(new BorderLayout());
            resultLabel = new JLabel("Results:");
            add(resultLabel, BorderLayout.WEST);
            JPanel controlPane = new JPanel();
            FlowLayout layout = new FlowLayout(FlowLayout.RIGHT, 2, 2);
            controlPane.setLayout(layout);
            firstBtn = new JButton("First");
            ImageIcon icon = PlugInScopeObjectManager.getManager().createImageIcon("First16.gif");
            firstBtn.setIcon(icon);
            controlPane.add(firstBtn);
            prevBtn = new JButton("Prev");
            icon = PlugInScopeObjectManager.getManager().createImageIcon("Back16.gif");
            prevBtn.setIcon(icon);
            controlPane.add(prevBtn);
            pageLabel = new JLabel("Page 1");
            controlPane.add(pageLabel);
            nextBtn = new JButton("Next");
            icon = PlugInScopeObjectManager.getManager().createImageIcon("Forward16.gif");
            nextBtn.setIcon(icon);
            controlPane.add(nextBtn);
            lastBtn = new JButton("Last");
            icon = PlugInScopeObjectManager.getManager().createImageIcon("Last16.gif");
            lastBtn.setIcon(icon);
            controlPane.add(lastBtn);
            add(controlPane, BorderLayout.EAST);
            installListeners();
        }
        
        private void installListeners() {
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Object source = e.getSource();
                    int totalPageMinus1 = (int) Math.floor((double) sentences.size() / entriesPerPage);
                    int currentIndex = 0;
                    if (source == firstBtn) {
                        currentIndex = 0;
                    }
                    else if (source == lastBtn) {
                        // Calculate the last page
                        currentIndex = totalPageMinus1 * entriesPerPage;
                    }
                    else if (source == prevBtn) {
                        if (startEntry <= 0)
                            return;
                        currentIndex = startEntry - entriesPerPage;
                    }
                    else if (source == nextBtn) {
                        if (startEntry + entriesPerPage >= sentences.size())
                            return;
                        currentIndex = startEntry + entriesPerPage;
                    }
                    // Set the current page number
                    display(sentences, currentIndex);
                }
            };
            firstBtn.addActionListener(listener);
            lastBtn.addActionListener(listener);
            prevBtn.addActionListener(listener);
            nextBtn.addActionListener(listener);
        }
        
        private void validateButtons() {
            firstBtn.setEnabled(startEntry > 0);
            prevBtn.setEnabled(startEntry > 0);
            // Check if there is any more pages
            int last = startEntry + entriesPerPage;
            lastBtn.setEnabled(last < sentences.size());
            nextBtn.setEnabled(last < sentences.size());
        }
        
        public void showResults(int startEntry) {
            this.startEntry = startEntry;
            // Need to figure out the end page
            int endEntry = startEntry + entriesPerPage;
            if (endEntry > sentences.size())
                endEntry = sentences.size(); // The maximum has been reached
            if (endEntry == 0) {
                resultLabel.setText("Results: No results!");
            }
            else {
                resultLabel.setText("Results: " + (startEntry + 1) + " to " +
                                    endEntry + " of " + sentences.size()); // Don't need to offset 1 for the endEntry
            }
            validateButtons();
        }
    }
    
    private class FilterDialog extends JDialog {
        private boolean isOkClicked;
        private JTextField cancerField;
        private JCheckBox containsBox;
        private JComboBox statusBox;
        private JComboBox negationBox;
        private JComboBox celllineBox;
        
        public FilterDialog(Frame owner) {
            super(owner);
            init();
        }
        
        public String getSelectedCancerType() {
            String rtn = cancerField.getText().trim();
            if (rtn.length() == 0)
                return null;
            return rtn;
        }
        
        public boolean isContainsChecked() {
            return containsBox.isSelected();
        }
        
        public String getStatus() {
            String rtn = (String) statusBox.getSelectedItem();
            if (rtn.length() == 0)
                return null;
            return rtn;
        }
        
        public String getNegationIndicator() {
            String rtn = (String) negationBox.getSelectedItem();
            if (rtn.length() == 0)
                return null;
            return rtn;
        }
        
        public String getCelllineIndicator() {
            String rtn = (String) celllineBox.getSelectedItem();
            if (rtn.length() == 0)
                return null;
            return rtn;
        }
        
        private void init() {
            setTitle("Set Annotation Display Filter");
            JPanel contentPane = new JPanel();
            Border border = BorderFactory.createEmptyBorder(8, 8, 8, 8);
            contentPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), border));
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JLabel label = new JLabel("<html><b><u>Show annotations for:</u></b><html>");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            constraints.gridwidth = 2;
            constraints.anchor = GridBagConstraints.CENTER;
            constraints.insets = new Insets(4, 4, 8, 4);
            contentPane.add(label, constraints);
            label = new JLabel("Cancer type:");
            constraints.anchor = GridBagConstraints.WEST;
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            constraints.insets = new Insets(4, 4, 0, 4);
            contentPane.add(label, constraints);
            cancerField = new JTextField();
            constraints.gridx = 1;
            constraints.weightx = 0.5;
            contentPane.add(cancerField, constraints);
            containsBox = new JCheckBox("contains");
            containsBox.setSelected(true); // Default should be checked
            containsBox.setEnabled(false);
            constraints.gridx = 1;
            constraints.gridy = 2;
            constraints.weightx = 0.0;
            constraints.insets = new Insets(0, 4, 4, 4);
            contentPane.add(containsBox, constraints);
            
            label = new JLabel("Status:");
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.gridwidth = 1;
            constraints.weightx = 0.0;
            constraints.insets = new Insets(4, 4, 4, 4);
            contentPane.add(label, constraints);
            String[] statusTypes = new String[] {
                    "",
                    "finished",
                    "unclear",
                    "no_fact",
                    "redundant_information"
            };
            statusBox = new JComboBox(statusTypes);
            constraints.gridx = 1;
            constraints.gridwidth = 1;
            contentPane.add(statusBox, constraints);
            label = new JLabel("Negation Indicator:");
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.gridwidth = 1;
            contentPane.add(label, constraints);
            String[] yesNoTypes = new String[] {
                    "",
                    "no",
                    "yes"
            };
            negationBox = new JComboBox(yesNoTypes);     
            constraints.gridx = 1;
            constraints.gridwidth = 1;
            contentPane.add(negationBox, constraints);
            label = new JLabel("Cellline Indicator:");
            constraints.gridx = 0;
            constraints.gridy = 5;
            constraints.gridwidth = 1;
            contentPane.add(label, constraints);
            celllineBox = new JComboBox(yesNoTypes);
            constraints.gridx = 1;
            constraints.gridwidth = 1;
            contentPane.add(celllineBox, constraints);
            getContentPane().add(contentPane, BorderLayout.CENTER);
            DialogControlPane controlPane = new DialogControlPane();
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = true;
                    dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            // Control the contains box
            cancerField.getDocument().addDocumentListener(new DocumentListener() {
                
                public void removeUpdate(DocumentEvent e) {
                    containsBox.setEnabled(cancerField.getText().trim().length() > 0);
                }
                
                public void insertUpdate(DocumentEvent e) {
                    containsBox.setEnabled(cancerField.getText().trim().length() > 0);
                }
                
                public void changedUpdate(DocumentEvent e) {
                    containsBox.setEnabled(cancerField.getText().trim().length() > 0);
                }
            });
        }
        
        public void setFiltersFromText(String text) {
            if (text.length() == 0 || text.equals("None"))
                return;
            String[] tokens = text.split("; ");
            for (String token : tokens) {
                String[] nameValue = token.split(": ");
                if (nameValue[0].equals("Cancer type"))
                    cancerField.setText(nameValue[1]);
                else if (nameValue[0].equals("Status"))
                    statusBox.setSelectedItem(nameValue[1]);
                else if (nameValue[0].equals("Negation"))
                    negationBox.setSelectedItem(nameValue[1]);
                else if (nameValue[0].equals("Cellline"))
                    celllineBox.setSelectedItem(nameValue[1]);
                else if (nameValue[0].equals("contains"))
                    containsBox.setSelected(nameValue[1].equals("checked"));
            }
        }
        
        public String getFiltersText() {
            String cancer = getSelectedCancerType();
            String status = getStatus();
            String negation = getNegationIndicator();
            String cellline = getCelllineIndicator();
            // Generate a filter label
            StringBuilder builder = new StringBuilder();
            if (cancer != null) {
                builder.append("Cancer type: " + cancer);
                builder.append("; contains: " + (isContainsChecked() ? "checked" : "not checked"));
            }
            if (status != null) {
                if (builder.length() > 0)
                    builder.append("; ");
                builder.append("Status: " + status);
            }
            if (negation != null) {
                if (builder.length() > 0)
                    builder.append("; ");
                builder.append("Negation: " + negation);
            }
            if (cellline != null) {
                if (builder.length() > 0)
                    builder.append("; ");
                builder.append("Cellline: " + cellline);
            }
            if (builder.length() == 0)
                builder.append("None");
            return builder.toString();
        }
        
    }
    
    public static void main(String[] agrs) throws Exception {
//        CancerIndexSentenceDisplayFrame display = new CancerIndexSentenceDisplayFrame();
//        display.setTitle("Cancer Index Frame");
//        display.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        display.setSize(800, 600);
//        GKApplicationUtilities.center(display);
//        display.setVisible(true);
//        long time1 = System.currentTimeMillis();
//        RESTFulFIService service = new RESTFulFIService();
//        String gene = "TP53";
//        List<Sentence> sentences = service.queryCGIAnnotations(gene);
//        long time2 = System.currentTimeMillis();
//        System.out.println("Time for querying: " + (time2 - time1));
//        display.display(sentences, gene);
    }
}
