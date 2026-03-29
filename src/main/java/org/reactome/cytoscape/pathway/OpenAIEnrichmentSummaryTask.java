package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.TableModel;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.osgi.framework.ServiceRegistration;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * Minimal OpenAI client for pathway enrichment result summarization.
 */
public class OpenAIEnrichmentSummaryTask extends AbstractTask {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OpenAIEnrichmentSummaryTask.class);
    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-5.4-mini";

    private TableModel tableModel;
    private String researchContext;
    private String apiToken;
    private String model;
    private String sourceTabTitle;

    public OpenAIEnrichmentSummaryTask(TableModel tableModel,
                                       String sourceTabTitle) {
        this.tableModel = tableModel;
        this.sourceTabTitle = sourceTabTitle;
    }

    /**
     * This method should be called first before starting the task.
     * @return
     */
    public boolean setUpEnv() {
        if (tableModel == null || tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                    "No enrichment result is displayed in the table.",
                    "No Results Displayed",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        OpenAIDialog dialog = new OpenAIDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
        dialog.setVisible(true);
        if (!dialog.isConfirmed())
            return false;
        String researchContext = dialog.getResearchContext();
        String apiToken = dialog.getApiToken();
        String model = dialog.getModel();
        if (researchContext == null || researchContext.trim().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                    "No research context is provided. Do you want to proceed without it?",
                    "Confirm No Research Context",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return false;
        }
        if (apiToken == null || apiToken.trim().isEmpty()) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                    "OpenAI API token is required to generate summary.",
                    "API Token Required",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (model == null || model.trim().isEmpty()) {
            model = DEFAULT_MODEL;
        }
        this.researchContext = researchContext;
        this.apiToken = apiToken;
        this.model = model;
        return true;
    }

    private LLMSummaryResultPane ensureSummaryPane() {
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, LLMSummaryResultPane.TITLE);
        LLMSummaryResultPane summaryPane = null;
        if (index >= 0) {
            summaryPane = (LLMSummaryResultPane) tableBrowserPane.getComponentAt(index);
        }
        else {
            summaryPane = new LLMSummaryResultPane();
            ServiceRegistration registration = PlugInUtilities.registerCytoPanelComponent(summaryPane);
            summaryPane.setServiceRegistration(registration);
        }
        summaryPane.setLabelText(LLMSummaryResultPane.TITLE + " - " + sourceTabTitle);
        return summaryPane;
    }

    @Override
    public void run(TaskMonitor monitor) throws Exception {
        LLMSummaryResultPane resultPane = ensureSummaryPane();
        PlugInObjectManager.getManager().selectCytoPane(resultPane, CytoPanelName.SOUTH);

        monitor.setTitle("OpenAI Pathway Summary");
        monitor.setStatusMessage("Preparing enrichment results...");
        monitor.setProgress(0.1);

        try {
            monitor.setStatusMessage("Contacting OpenAI...");
            monitor.setProgress(0.4);

            String summary = summarize(researchContext, apiToken);

            monitor.setStatusMessage("Summary received.");
            monitor.setProgress(1.0);

            resultPane.setSummary(summary);
            // If all work well, keep the API token in memory for a while to avoid asking users to input it again for the next summary generation.
            PlugInObjectManager.getManager().setOpenAIKey(apiToken);
            PlugInObjectManager.getManager().setReserachContext(researchContext);
            PlugInObjectManager.getManager().setOpenAIModel(model);
        }
        catch (Exception ex) {
            logger.error("Error summarizing pathway enrichment results", ex);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    PlugInObjectManager.getManager().getCytoscapeDesktop(),
                    "Error generating summary: " + ex.getMessage(),
                    "LLM Summary Error",
                    JOptionPane.ERROR_MESSAGE));
        }
    }

    private String summarize(String researchContext,
                             String apiToken) throws Exception {
        if (apiToken == null || apiToken.trim().isEmpty())
            throw new IllegalArgumentException("OpenAI API token is required.");

        String prompt = buildPrompt(researchContext);
        String payload = buildPayload(prompt);

        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(OPENAI_CHAT_URL);
        method.setRequestHeader("Authorization", "Bearer " + apiToken.trim());
        method.setRequestHeader("Content-Type", "application/json");
        method.setRequestEntity(new StringRequestEntity(payload, "application/json", StandardCharsets.UTF_8.name()));
        try {
            int status = client.executeMethod(method);
            String response = method.getResponseBodyAsString();
            if (status != HttpStatus.SC_OK) {
                throw new IOException("OpenAI API error (" + status + "): " + response);
            }
            String content = extractAssistantContent(response);
            if (content == null || content.trim().isEmpty())
                throw new IOException("OpenAI API returned an empty summary.");
            return content.trim();
        }
        finally {
            method.releaseConnection();
        }
    }

    private String buildPrompt(String reserachContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("The following is a list of significant pathways identified in an enrichment analysis. ");
        if (reserachContext != null && !reserachContext.trim().isEmpty()) {
            builder.append("The study is about: ");
            builder.append(reserachContext);
        }
        builder.append(".\n\n");
        builder.append("Summarize the following Reactome pathway enrichment results and return:\n");
        builder.append("1. A overview paragraph.\n");
        builder.append("2. Bullet points describing the main biological themes.\n");
        builder.append("3. A short note about any caveats, based only on the supplied results.\n");
        builder.append("4. Format the output in HTML.\n\n");
        builder.append("Pathway enrichment results:\n");
        builder.append(buildPathwayResults(tableModel));
        builder.append("\nBase the summary only on these results.");
        return builder.toString();
    }
    
    private String buildPathwayResults(TableModel model) {
        StringBuilder builder = new StringBuilder();
        int rowCount = model.getRowCount();
        int colCount = model.getColumnCount();
        for (int i = 0; i < rowCount; i++) {
            builder.append("{");
            for (int col = 0; col < colCount; col++) {
                String columnName = model.getColumnName(col);
                Object value = model.getValueAt(i, col);
                builder.append("\"")
                       .append(columnName)
                       .append("\": \"")
                       .append(value)
                       .append("\"");

                if (col < colCount - 1) {
                    builder.append(", ");
                }
            }
            builder.append("}\n"); // JSONL: one object per line
        }
        return builder.toString();
    }

    private String buildPayload(String prompt) {
        return "{" +
                "\"model\":\"" + DEFAULT_MODEL + "\"," +
                "\"temperature\":0.2," +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"" + escapeJson("You are an expert in interpreting Reactome pathway enrichment results"
                        + " with strong biology background. Please summarize Reactome pathway enrichment results. "
                        + "Be concise, specific, and avoid making unsupported claims.") + "\"}," +
                        "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}" +
                        "]}";
    }

    private String extractAssistantContent(String response) {
        String marker = "\"content\":";
        int index = response.indexOf(marker);
        if (index < 0)
            return null;
        index += marker.length();
        while (index < response.length() && Character.isWhitespace(response.charAt(index)))
            index++;
        if (index >= response.length() || response.charAt(index) != '"')
            return null;
        index++;
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (; index < response.length(); index++) {
            char ch = response.charAt(index);
            if (escaped) {
                switch (ch) {
                case 'n': builder.append('\n'); break;
                case 'r': builder.append('\r'); break;
                case 't': builder.append('\t'); break;
                case '"': builder.append('"'); break;
                case '\\': builder.append('\\'); break;
                default: builder.append(ch);
                }
                escaped = false;
            }
            else if (ch == '\\') {
                escaped = true;
            }
            else if (ch == '"') {
                break;
            }
            else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private class OpenAIDialog extends JDialog {

        private JPasswordField apiTokenField;
        private JTextArea researchContextArea;
        private JComboBox<String> modelComboBox;
        private boolean confirmed = false;

        public OpenAIDialog(Frame parent) {
            super(parent, "OpenAI LLM Settings", true);
            initUI();
        }

        private void initUI() {
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            Border border = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            mainPanel.setBorder(border);

            // Form panel
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEtchedBorder());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;

            // =========================
            // API Token
            // =========================
            gbc.gridx = 0;
            gbc.gridy = 0;
            formPanel.add(new JLabel("OpenAI API Token:"), gbc);

            gbc.gridx = 1;
            apiTokenField = new JPasswordField(30);
            formPanel.add(apiTokenField, gbc);

            if (PlugInObjectManager.getManager().getOpenAIKey() != null) {
                apiTokenField.setText(PlugInObjectManager.getManager().getOpenAIKey());
            }

            // =========================
            // Model Selection
            // =========================
            gbc.gridx = 0;
            gbc.gridy = 1;
            formPanel.add(new JLabel("OpenAI Model:"), gbc);

            gbc.gridx = 1;

            String[] models = {
                    "gpt-5.4-mini",
                    "gpt-5.4",
                    "gpt-5.3",
                    "gpt-4.1",
                    "gpt-4o",
                    "gpt-4o-mini"
            };

            modelComboBox = new JComboBox<>(models);
            modelComboBox.setEditable(true);

            if (PlugInObjectManager.getManager().getOpenAIModel() != null) {
                modelComboBox.setSelectedItem(PlugInObjectManager.getManager().getOpenAIModel());
            } else {
                modelComboBox.setSelectedItem("gpt-5.4-mini");
            }

            formPanel.add(modelComboBox, gbc);

            // =========================
            // Research Context
            // =========================
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            formPanel.add(new JLabel("Research Context:"), gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;

            researchContextArea = new JTextArea(5, 30);
            researchContextArea.setLineWrap(true);
            researchContextArea.setWrapStyleWord(true);

            if (PlugInObjectManager.getManager().getReserachContext() != null) {
                researchContextArea.setText(
                        PlugInObjectManager.getManager().getReserachContext());
            }

            JScrollPane scrollPane = new JScrollPane(researchContextArea);
            formPanel.add(scrollPane, gbc);

            mainPanel.add(formPanel, BorderLayout.CENTER);

            // =========================
            // Buttons
            // =========================
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");

            okButton.addActionListener((ActionEvent e) -> {
                confirmed = true;
                dispose();
            });

            cancelButton.addActionListener((ActionEvent e) -> {
                confirmed = false;
                dispose();
            });

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            setContentPane(mainPanel);
            setSize(650, 300);
            setLocationRelativeTo(getOwner());
        }

        public String getApiToken() {
            return new String(apiTokenField.getPassword());
        }

        public String getResearchContext() {
            return researchContextArea.getText();
        }

        public String getModel() {
            return (String) modelComboBox.getSelectedItem();
        }

        public boolean isConfirmed() {
            return confirmed;
        }
    }
}