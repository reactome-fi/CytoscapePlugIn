/*
 * Created on Feb 25, 2011
 *
 */
package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.util.FileUtility;

/**
 * This customized JPanel is used to show results from surival analysis.
 * @author wgm
 *
 */
public class SurvivalAnalysisResultPane extends JPanel implements CytoPanelComponent {
    // A hard-coded cutoff to display links for module based survival analysis
    private final double P_VALUE_CUTOFF_FOR_MODULE = 0.05d;
    // Customized constants for links
    private final String SINGLE_MODULE_LINK = "singleModuleLink";
    private final String FILE_LINK = "fileLink";
    private JTextPane contentPane;
    // The CytoPanel container used to hold this Panel.
    protected CytoPanel container;
    // This map is used to map file name to the full path for opening
    private Map<String, String> nameToPath;
    // Used to do single module surival analysis calling back
    private SingleModuleSurvivalActionListener singleModuleAction;
    private String title;
    
    public SurvivalAnalysisResultPane(String title) {
        setTitle(title);
        init();
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(CytoPanelComponent.class.getName(), 
                                this, 
                                new Properties());
        SessionLoadedListener sessionListener = new SessionLoadedListener() {
            
            @Override
            public void handleEvent(SessionLoadedEvent e) {
                close();
            }
        };
        context.registerService(SessionLoadedListener.class.getName(),
                                sessionListener,
                                null);
    }
    
    private void init() {
        setLayout(new BorderLayout());
        contentPane = new JTextPane();
        contentPane.setEditable(false);
        contentPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        // To support copy/paste
        contentPane.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doContentPanePopup(e);
                else
                    doMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doContentPanePopup(e);
            }
            
        });
        contentPane.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                AttributeSet style = getAttributeUnderMouse(e);
                Object name = style.getAttribute(StyleConstants.NameAttribute);
                if(name == SINGLE_MODULE_LINK || name == FILE_LINK) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                else
                    setCursor(Cursor.getDefaultCursor());
            }
        
        });
        add(new JScrollPane(contentPane), BorderLayout.CENTER);
        // Add a close button
        JButton closeBtn = new JButton("Close");
        add(closeBtn, BorderLayout.SOUTH);
        closeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (container != null)
                    close();
            }
        });
        nameToPath = new HashMap<String, String>();
       
    }
    
    private void doMousePressed(MouseEvent e) {
        // Need to find the text under the mouse
        int pos = contentPane.viewToModel(e.getPoint());
        StyledDocument doc = contentPane.getStyledDocument();
        Element elm = doc.getCharacterElement(pos);
        // Need to make sure it is clickable
        AttributeSet att = elm.getAttributes();
        Object name = att.getAttribute(StyleConstants.NameAttribute);
        if (name != SINGLE_MODULE_LINK && name != FILE_LINK)
            return;
        try {
            String text = doc.getText(elm.getStartOffset(), 
                                      elm.getEndOffset() - elm.getStartOffset());
            if (name == SINGLE_MODULE_LINK)
                doSingleModuleAnalysis(text);
            else if (name == FILE_LINK)
                openFile(text);
        }
        catch(BadLocationException e1) {
            e1.printStackTrace();
        }
    }
    
    public void setSingleModuleSurivivalAnalysisActionListener(SingleModuleSurvivalActionListener l) {
        this.singleModuleAction = l;
    }
    
    private void doSingleModuleAnalysis(String text) {
        if (singleModuleAction == null)
            return;
        SingleModuleSurvivalAnalysisActionEvent event = new SingleModuleSurvivalAnalysisActionEvent(this);
        // Need to get the module
        String[] tokens = text.split("\t");
        event.setModule(tokens[0]);
        singleModuleAction.doSingleModuleSurvivalAnalysis(event);
    }
    
    private void openFile(String fileName) {
        String path = nameToPath.get(fileName);
        if (path == null) {
            JOptionPane.showMessageDialog(SurvivalAnalysisResultPane.this,
                                          "Cannot open plot: " + fileName + ". File cannot be found!",
                                          "Error in Opening Plot",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        String url = "file:///" + path; //TODO: This should be checked under windows
        PlugInUtilities.openURL(url);
    }
    
    private AttributeSet getAttributeUnderMouse(MouseEvent e) {
        int pos = contentPane.viewToModel(e.getPoint());
        StyledDocument doc = contentPane.getStyledDocument();
        Element elm = doc.getCharacterElement(pos);
        return elm.getAttributes();
    }
    
    private void doContentPanePopup(MouseEvent e) {
        // Check if there is any selection
        String selection = contentPane.getSelectedText();
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copy = new JMenuItem("Copy");
        copy.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                contentPane.copy();
            }
        });
        if (selection == null || selection.length() == 0)
            copy.setEnabled(false);
        popup.add(copy);
        JMenuItem export = new JMenuItem("Export Results");
        export.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                exportResults();
            }
        });
        popup.add(export);
        popup.show(contentPane, e.getX(), e.getY());
    }
    
    private void exportResults() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference fileUtilRef = context.getServiceReference(FileUtil.class.getName());
        if (fileUtilRef == null) {
            JOptionPane.showMessageDialog(contentPane,
                                          "Error in export survival analysis results: cannot find registered FileUtil!",
                                          "Error in Exporting",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        FileUtil fileUtil = (FileUtil) context.getService(fileUtilRef);
        if (fileUtil == null) {
            JOptionPane.showMessageDialog(contentPane,
                                          "Error in export survival analysis results: cannot find registered FileUtil!",
                                          "Error in Exporting",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Export annotations in a text file
        Collection<FileChooserFilter> filters = new ArrayList<FileChooserFilter>();
        filters.add(new FileChooserFilter("Text files", "txt"));
        File file = fileUtil.getFile(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                     "Export Results", 
                                     FileUtil.SAVE, 
                                     filters);
        if (file == null)
            return;
        FileUtility fu = new FileUtility();
        try {
            fu.setOutput(file.getAbsolutePath());
            Document document = contentPane.getDocument();
            String text = document.getText(0, document.getLength());
            fu.printLine(text);
            fu.close();
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(contentPane,
                                          "Error in export survival analysis results: " + e,
                                          "Error in Exporting", 
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void setContainer(CytoPanel container) {
        this.container = container;
    }
    
    /**
     * Display results by appending to the current display.
     * @param results at most three elements: first: output; second: error; third: plotFileName
     */    
    public void appendResult(String title, String[] results) throws IOException, BadLocationException {
//        contentPane.setEditable(true);
//        // Need to clear up any selection
//        contentPane.setSelectionStart(0);
//        contentPane.setSelectionEnd(0);
        StyledDocument doc = contentPane.getStyledDocument();
        // used for selection
        int originalPos = doc.getLength();
        SimpleAttributeSet simpleAtt = new SimpleAttributeSet();
        TabStop tabStop = new TabStop(75, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
        StyleConstants.setTabSet(simpleAtt, new TabSet(new TabStop[]{tabStop}));
        doc.setParagraphAttributes(doc.getLength(), 1, simpleAtt, false);
        int len = doc.getLength();
        if (len > 0) { // Some results exist already
            appendLine("\n\n********************************\n\n", simpleAtt); // Used as separate
        }
        
        appendLine("Analysis: " + title + "\n", simpleAtt);
        for (int i = 0; i < results.length; i++) {
            String text = results[i];
            if (text == null || text.length() == 0)
                continue;
            switch (i) {
                case 0 :
                    appendLine("\n----Output----\n",
                               simpleAtt);
                    if (title.equals("Coxph (all modules)")) {
                        appendCoxphForAllModules(text, 
                                                 simpleAtt);
                    }
                    else
                        appendLine(text + "\n", simpleAtt);
                    break;
                case 1 :
                    appendLine("\n----Error----\n" + text + "\n", simpleAtt);
                    break;
                case 2 :
                    int index = text.lastIndexOf(File.separator);
                    String fileName = text.substring(index + 1);
                    // Use color only for links
                    appendLine("\n----Plot (click to open)----\n", simpleAtt);
                    appendLink(fileName, 
                               FILE_LINK,
                               simpleAtt);
                    nameToPath.put(fileName, text);
            }
        }
        int currentPos = doc.getLength();
        contentPane.setSelectionStart(originalPos);
        contentPane.setSelectionEnd(currentPos);
        // Don't call the following statement. Otherwise, the above selection
        // will not work.
//        contentPane.setCaretPosition(currentPos);
        contentPane.requestFocus();
    }
    
    private void appendCoxphForAllModules(String output,
                                          SimpleAttributeSet att) throws IOException, BadLocationException {
        // Use a little note here
        String note = "Note: Click underlined modules in blue for single module-based analysis. You may not " + 
                      "see any underlined module if all p-values > 0.05.\n\n";
        StyleConstants.setItalic(att, true);
        appendLine(note, att);
        StyleConstants.setItalic(att, false);
        StringReader sr = new StringReader(output);
        BufferedReader br = new BufferedReader(sr);
        String line = br.readLine();
        appendLine(line + "\n", att);
        while ((line = br.readLine()) != null) {
            // Check values
            String[] tokens = line.split("\t");
            try {
                Double pvalue = new Double(tokens[2]);
                if (pvalue <= P_VALUE_CUTOFF_FOR_MODULE) { // Only for pvalue <= 0.05d
                    // Need some special attribute
                    appendLink(line, SINGLE_MODULE_LINK, att);
                    appendLine("\n", att);
                }
                else
                    appendLine(line + "\n", att);
            }
            catch(NumberFormatException e) {
                appendLine(line + "\n", att);
            }
        }
        br.close();
        sr.close();
    }
    
    private void appendLink(String text, String type, SimpleAttributeSet att) throws BadLocationException{
        // Need some special attribute
        att.addAttribute(StyleConstants.NameAttribute,
                         type);
        StyleConstants.setForeground(att, Color.BLUE);
        StyleConstants.setUnderline(att, true);
        appendLine(text, att);
        // Revert back
        att.removeAttribute(StyleConstants.NameAttribute);
        StyleConstants.setForeground(att, Color.BLACK);
        StyleConstants.setUnderline(att, false);
    }
    
    private void appendLine(String line,
                            AttributeSet set) throws BadLocationException {
        StyledDocument doc = contentPane.getStyledDocument();
        doc.insertString(doc.getLength(), line, set);
    }
    
    @Test
    public void testMatch() {
        String text = "1298674201562_plot.pdf";
        if (text.matches("(\\d)+_plot.pdf"))
            System.out.println("Matched!");
    }

    @Override
    public Component getComponent()
    {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName()
    {
        return CytoPanelName.EAST;
    }

    @Override
    public Icon getIcon()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    private void close()
    {
        if (container == null)
            return;
        ((Container) container).remove(SurvivalAnalysisResultPane.this);
    }
    
}
