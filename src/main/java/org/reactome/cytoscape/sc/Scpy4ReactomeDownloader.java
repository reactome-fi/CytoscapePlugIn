package org.reactome.cytoscape.sc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;

import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

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
        String fileUrl = getURL(APP_FILE, true);
        URL url = new URL(fileUrl);
        InputStream is = url.openStream();
        File localAppFile = getLocalAppFile();
        FileOutputStream fos = new FileOutputStream(localAppFile);
        byte[] buffer = new byte[10 * 1024]; // 10 k
        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            fos.write(buffer, 0, read);
        }
        is.close();
        fos.close();
    }

    protected File getLocalAppFile() throws IOException {
        File localAppFile = new File(PythonPathHelper.getHelper().getScPythonPath(),
                                     APP_FILE);
        return localAppFile;
    }
    
    private boolean isUpdated() throws Exception {
        // Get the version file from the server
        String fileUrl = getURL(VERSION_FILE, false);
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
        // Also we need to make sure the app file there too
        File appFile = getLocalAppFile();
        if (versionFile.exists() && appFile.exists()) {
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
    
    private String getURL(String fileName, boolean isForApp) {
        String hostURL = PlugInObjectManager.getManager().getHostURL();
        StringBuilder builder = new StringBuilder();
        builder.append(hostURL).append("Cytoscape/scpy4reactome/");
        if (isForApp) {
            if (PlugInUtilities.isMac())
                builder.append("mac");
            else if (PlugInUtilities.isWindows())
                builder.append("win");
            else 
                throw new IllegalStateException("Currently this operation systems is not supported " + 
                                                "for scRNA-Seq data analysis and visualization.");
            builder.append("/");
        }
        return builder.append(fileName).toString();
    }

}
