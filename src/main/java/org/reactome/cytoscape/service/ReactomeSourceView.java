/*
 * Created on Feb 13, 2014
 *
 */
package org.reactome.cytoscape.service;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.gk.model.ReactomeJavaConstants;
import org.jdom.Element;
import org.jdom.Namespace;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to view an instance in the Reactome database. This is a singleton.
 * In the future, this may be used as a service for Cytoscape OGSi with some refactoring.
 * @author gwu
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class ReactomeSourceView {
    private static Logger logger = LoggerFactory.getLogger(ReactomeSourceView.class);
    
    /**
     * Default constructor.
     */
    public ReactomeSourceView() {
    }
    
    public void viewReactomeSource(Long dbId,
                                   Component component) {
        // Need to quick the restfulAPI
        RESTFulFIService fiService = new RESTFulFIService();
        try {
            Element element = fiService.queryReactomeInstance(dbId);
            Window window = (Window) SwingUtilities.getAncestorOfClass(Window.class, component);
            InstanceDialog instanceDialog = new InstanceDialog(window);
            instanceDialog.setInstance(element);
            instanceDialog.setSize(500, 400);
            instanceDialog.setLocationRelativeTo(component);
            instanceDialog.setModal(true);
            instanceDialog.setVisible(true);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(component,
                                          "Error in view reactome source: " + e.getMessage(), 
                                          "Error in View Reactome Source", 
                                          JOptionPane.ERROR_MESSAGE);
            logger.error("Error in viewReactomeSource", e);
        }
    }
    
    /**
     * A customized JFrame that is used to show the content of an instance
     * in the Reactome database.
     * @author gwu
     *
     */
    private class InstanceDialog extends JDialog {
        private JEditorPane htmlPane;
        private Long dbId;
        
        public InstanceDialog(Window parent) {
            super(parent);
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
            closeBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            controlPane.add(closeBtn);
            getContentPane().add(controlPane, BorderLayout.SOUTH);
        }
        
        private void processLink(String desc) {
            // Parse link
            int index = desc.indexOf(":");
            String type = desc.substring(0, index);
            String dbId = desc.substring(index + 1);
            if (type.equals("Instance"))
                viewReactomeSource(new Long(dbId),
                                   InstanceDialog.this);
            else if (type.equals("pubmedid")) {
                String url = "http://www.ncbi.nlm.nih.gov/pubmed/" + dbId;
                PlugInUtilities.openURL(url);
            }
        }
        
        public void setInstance(Element element) {
            StringBuilder builder = new StringBuilder();
            // Parse displayName and DB_ID first
            String schemaClass = element.getChildText("schemaClass");
            String displayName = element.getChildText("displayName");
            String dbId = element.getChildText("dbId");
            
            builder.append("<html><body>");
            builder.append("<table width=\"100%\" border=\"1\">");
            // three most important information
            fillTableRow("classType", schemaClass, builder);
            fillTableRow("dbId", dbId, builder);
            fillTableRow("displayName", displayName, builder);
            
            List<Element> attributes = element.getChildren("attributes");
            for (Element attribute : attributes) {
                generateHTMLForAttribute(attribute, builder);
            }
            
            builder.append("</body></html>");
            htmlPane.setText(builder.toString());
            htmlPane.setCaretPosition(0); // Want to start from the first line.
        }
        
        private void fillTableRow(String header,
                                  String value,
                                  StringBuilder buffer) {
            buffer.append("<tr>");
            fillTableRowHeader(header, buffer, 1);
            fillTableRowValue(value, buffer);
            buffer.append("</tr>");
        }
        
        private void fillTableRowHeader(String header, StringBuilder buffer, int rowSpan) {
            buffer.append("<th align=\"left\" bgcolor=\"#C0C0C0\" rowspan=\"" + rowSpan + "\">" + header + "</th>");
        }
        
        private void fillTableRowValue(String value, StringBuilder buffer) {
            buffer.append("<td>" + value + "</td>");
        }
        
        private void generateHTMLForAttribute(Element attribute,
                                              StringBuilder builder) {
            List<Element> values = attribute.getChildren("values");
            if (values == null || values.size() == 0)
                return;
            // Peak the type
            Element value = values.get(0);
            Namespace ns = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            String type = value.getAttributeValue("type", ns);
            if (type.equals("reactomeInstance"))
                generateHTMLForInstanceAttribute(attribute, builder);
            else
                generateHTMLForStringAttribute(attribute, builder);
        }
        
        private void generateHTMLForInstanceAttribute(Element attribute,
                                                      StringBuilder builder) {
            List<Element> values = attribute.getChildren("values");
            List<String[]> instances = new ArrayList<String[]>(values.size());
            Map<String, Integer> dbIdToStoic = new HashMap<String, Integer>();
            for (Element value : values) {
                String dbId = value.getChildText("dbId");
                String displayName = value.getChildText("displayName");
                Integer stoi = dbIdToStoic.get(dbId);
                if (stoi == null) {
                    // First time
                    // Just use this type
                    String[] instance = new String[]{dbId, displayName};
                    instances.add(instance);
                    dbIdToStoic.put(dbId, 1);
                }
                else {
                    dbIdToStoic.put(dbId, ++stoi);
                }
            }
            generateHTMLForList(instances,
                                dbIdToStoic, 
                                builder,
                                attribute.getChildText("name"));
        }
        
        private void generateHTMLForList(List<String[]> instances, 
                                         Map<String, Integer> dbIdToStoi, 
                                         StringBuilder buffer, 
                                         String rowTitle) {
            int index = 0;
            buffer.append("<tr>");
            fillTableRowHeader(rowTitle,
                               buffer,
                               instances.size());
            for (String[] instance : instances) {
                if (index > 0)
                    buffer.append("<tr>");
                Integer stoi = (Integer) dbIdToStoi.get(instance[0]);
                String value = "";
                if (stoi != null && stoi.intValue() > 1)
                    value += stoi + " x ";
                value += "<a href=\"Instance:" + instance[0] + "\">" + instance[1] + "</a>";
                fillTableRowValue(value, buffer);
                buffer.append("</tr>");
                index++;
            }
        }
        
        private void generateHTMLForStringAttribute(Element attribute,
                                                    StringBuilder builder) {
            String title = attribute.getChildText("name");
            List<Element> valueElms = attribute.getChildren("values");
            List<String> values = new ArrayList<String>(valueElms.size());
            for (Element valueElm : valueElms) {
                String value = valueElm.getText();
                values.add(value);
            }
            generateHtmlForStringList(builder,
                                      values,
                                      title);
        }
        
        private void generateHtmlForStringList(StringBuilder buffer, 
                                               java.util.List values,
                                               String title) {
              if (values != null && values.size() > 0) {
                  buffer.append("<tr>");
                  fillTableRowHeader(title, buffer, values.size());
                  for (int i = 0; i < values.size(); i++) {
                      if (i > 0)
                          buffer.append("<tr>");
                      String value = values.get(i).toString();
                      // Add a link for pubmedid
                      if (title.equals(ReactomeJavaConstants.pubMedIdentifier))
                          value = "<a href=\"pubmedid:" + value + "\">" + value + "</a>";;
                      fillTableRowValue(value,
                                        buffer);
                      buffer.append("</tr>");
                  }
              }
          }
        
    }
}
