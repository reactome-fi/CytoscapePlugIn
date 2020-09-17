package org.reactome.cytoscape.sc;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This helper class is used to handle Python path for starting the python service.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
public class PythonPathHelper {
    private final String PYTHON_PROP_KEY = "python";
    // Cached path
    private String scPythonPath;
    // A singleton
    private static PythonPathHelper helper;
    
    private PythonPathHelper() {
    }
    
    public static PythonPathHelper getHelper() {
        if (helper == null)
            helper = new PythonPathHelper();
        return helper;
    }
    
    public String getPythonPath() throws IOException {
        String pythonPath = getSavedPythonPath();
        if (pythonPath != null)
            return pythonPath;
        PythonPathDialog dialog = new PythonPathDialog();
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked)
            return null;
        pythonPath = dialog.tf.getText().trim();
        if (!checkPythonVersion(pythonPath))
            return null;
        savePythonPath(pythonPath);
        return pythonPath;
    }
    
    private boolean checkPythonVersion(String path) throws IOException {
        // Check the version by running python --version
        Process process = Runtime.getRuntime().exec(new String[] {path, "--version"});
        // Output for python 2 may be from error stream
        InputStream is = process.getErrorStream();
        String error = output(is);
        if (error.length() > 0) {
            if (error.matches("Python (\\d+)\\.(\\d+)\\.(\\d+)")) {
                return checkVersion(error);
            }
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "There is an error with a test run of python:\n" + error,
                                          "Error in Python",
                                          JOptionPane.ERROR_MESSAGE);
            return false; // This cannot work
        }
        String output = output(process.getInputStream());
        // Need to check output
        if (output.length() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Test run failed: Cannot get python version.",
                                          "Error in Python",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return checkVersion(output);
    }
    
    private boolean checkVersion(String output) {
        Matcher matcher = Pattern.compile("Python (\\d+)\\.(\\d+)\\.(\\d+)").matcher(output);
        if (matcher.matches()) {
            int major = new Integer(matcher.group(1));
            int minor = new Integer(matcher.group(2));
            int patch = new Integer(matcher.group(3));
            if (major >= 3 && ((minor == 6 && patch >= 9) || minor >= 7))
                return true;
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Python version 3.6.9 or higher is needed.\nYour version is " + output,
                                          "Error in Python",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                      "Cannot figure out python version. The output from version test is: " + output,
                                      "Error in Python",
                                      JOptionPane.ERROR_MESSAGE);
        return false;
    }
    
    private String output(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null)
            builder.append(line).append("\n");
        if (builder.length() > 0)
            builder.deleteCharAt(builder.length() - 1); // The last new line
        return builder.toString().trim();
    }
    
    private void savePythonPath(String path) throws IOException {
        CyServiceRegistrar serviceRegistrar = PlugInObjectManager.getManager().getServiceRegistra();
        CyProperty<Properties> cyProps = serviceRegistrar.getService(CyProperty.class,
                                                                     "(cyPropertyName=ReactomeFIViz.props)");
        cyProps.getProperties().put(PYTHON_PROP_KEY, path);
    }
    
    private String getSavedPythonPath() {
        CyServiceRegistrar serviceRegistrar = PlugInObjectManager.getManager().getServiceRegistra();
        CyProperty<Properties> cyProps = serviceRegistrar.getService(CyProperty.class,
                                                                     "(cyPropertyName=ReactomeFIViz.props)");
        String path = cyProps.getProperties().getProperty(PYTHON_PROP_KEY);
        return path;
    }
    
    public String getScPythonPath() {
        if (scPythonPath != null)
            return scPythonPath;
        // Get the scPythonPath
        File reactomeFIVizDir = getReactomeFIVizDir();
        if (!reactomeFIVizDir.exists()) {
            if(!reactomeFIVizDir.mkdirs()) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot create a folder for ReactomeFIViz: \n" + reactomeFIVizDir.getAbsolutePath(),
                                              "Error in Creating Folder",
                                              JOptionPane.ERROR_MESSAGE);
                throw new IllegalStateException("Cannot create a folder: " + reactomeFIVizDir.getAbsolutePath());
            }
        }
        scPythonPath = reactomeFIVizDir.getAbsolutePath();
        return scPythonPath;
    }

    private File getReactomeFIVizDir() {
        File cytoscapeDir = new File(System.getProperty("user.home"), CyProperty.DEFAULT_PROPS_CONFIG_DIR); 
        File reactomeFIVizDir = new File(cytoscapeDir, PlugInUtilities.APP_NAME);
        return reactomeFIVizDir;
    }
    
    public String getLogFileName() {
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        Date today = new Date();
        String dirName = getScPythonPath();
        File logFile = new File(dirName, PlugInUtilities.APP_NAME + "." + formatter.format(today) + ".log");
        return logFile.getAbsolutePath();
    }
    
    @SuppressWarnings("serial")
    private static class PythonPathDialog extends JDialog {
        private boolean isOkClicked;
        private JTextField tf;
        
        public PythonPathDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        private void init() {
            setTitle("Choose Python");
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JLabel label = new JLabel("Choose or enter the Python path:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 2;
            contentPane.add(label, constraints);
            tf = new JTextField();
            // A big guess
            tf.setText("python3");
            tf.setColumns(30);
            constraints.gridy ++;
            constraints.gridwidth = 1;
            contentPane.add(tf, constraints);
            JButton browse = new JButton("Browse");
            browse.addActionListener(e -> PlugInUtilities.browseFileForLoad(tf, "Choose Python", new String[] {}));
            constraints.gridx ++;
            contentPane.add(browse, constraints);
            
            JTextArea ta = PlugInUtilities.createTextAreaForNote(contentPane);
            String note = "Python 3.7.0 or higher is required. If you have not installed python, install one from www.python.org. " + 
                    "If your path has been set up, you may enter python3 or just python. " +
                    "Otherwise, you may use the Browser button to find where your python is. You may change to another "
                    + "Python later on by editing the properties for ReactomeFIViz via menu Edit/Preferences/Properties.";
            ta.setText(note);
            constraints.gridy ++;
            constraints.gridx = 0;
            constraints.gridwidth = 2;
            constraints.gridheight = 2;
            contentPane.add(ta, constraints);
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            controlPane.getOKBtn().addActionListener(e -> {
                isOkClicked = true;
                dispose();
            });
            controlPane.getCancelBtn().addActionListener(e -> {
                isOkClicked = false;
                dispose();
            });
            
            tf.getDocument().addDocumentListener(new DocumentListener() {
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateOKButton(controlPane.getOKBtn());
                }
                
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateOKButton(controlPane.getOKBtn());
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateOKButton(controlPane.getOKBtn());
                }
            });
            
//            addComponentListener(new ComponentAdapter() {
//
//                @Override
//                public void componentResized(ComponentEvent e) {
//                    System.out.println("Size: " + getSize());
//                }
//                
//            });
            
            setLocationRelativeTo(getOwner());
            setSize(554, 265);
        }
        
        private void updateOKButton(JButton okBtn) {
            okBtn.setEnabled(tf.getText().trim().length() > 0);
        }
        
    }
    
}
