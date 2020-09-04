package org.reactome.cytoscape.sc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;

import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This class is used to download the scpy4reactome package from the server.
 * @author wug
 *
 */
public class Scpy4ReactomeDownloader {
    private final String VERSION_FILE = "scpy4reactome.ver";
    private final String APP_FILE = ScNetworkManager.SCPY_2_REACTOME_NAME;
    
    public Scpy4ReactomeDownloader() {
    }
    
    public void download(ProgressPane progPane) throws Exception {
        progPane.setText("Check analysis service version...");
        if (isUpdated())
            return;
        progPane.setText("Downloading Python service app...");
        downloadApp();
    }
    
    private void downloadApp() throws Exception {
        String fileUrl = getURL(APP_FILE);
        URL url = new URL(fileUrl);
        InputStream is = url.openStream();
        File localAppFile = new File(PythonPathHelper.getHelper().getScPythonPath(),
                                     APP_FILE);
        FileOutputStream fos = new FileOutputStream(localAppFile);
        byte[] buffer = new byte[10 * 1024]; // 10 k
        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            fos.write(buffer, 0, read);
        }
        is.close();
        fos.close();
    }
    
    private boolean isUpdated() throws Exception {
        // Get the version file from the server
        String fileUrl = getURL(VERSION_FILE);
        URL url = new URL(fileUrl);
        InputStream is = url.openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String remoteVersion = br.readLine().trim();
        br.close();
        isr.close();
        is.close();
        File versionFile = new File(PythonPathHelper.getHelper().getScPythonPath(),
                                    VERSION_FILE);
        if (versionFile.exists()) {
            // There should be one line only
            String localVersion = Files.readAllLines(versionFile.toPath()).get(0).trim();
            if (localVersion.equals(remoteVersion))
                return true;
        }
        // Write out the version
        PrintWriter pw = new PrintWriter(versionFile);
        pw.println(remoteVersion);
        pw.close();
        return false;
    }
    
    private String getURL(String fileName) {
        String hostURL = PlugInObjectManager.getManager().getHostURL();
        String fileUrl = hostURL + "Cytoscape/" + fileName;
        return fileUrl;
    }

}
