package gov.usgs.cida.utilities.communication;

import gov.usgs.cida.utilities.xml.XMLUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

/**
 * Manage GeoServer with its REST interface.
 * 
 * @see http://docs.geoserver.org/latest/en/user/restconfig/rest-config-api.html
 */
public class GeoserverHandler {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(GeoserverHandler.class);
    private static final String PARAM_POST = "POST";
    private static final String PARAM_PUT = "PUT";
    private static final String PARAM_GET = "GET";
    private static final String PARAM_DELETE = "DELETE";
    private static final String PARAM_REST_WORKSPACES = "rest/workspaces/";
    private static final String PARAM_TEXT_XML = "text/xml";
    private static final String PARAM_DOT_XML = ".xml";
    private static final String PARAM_DATASTORES = "/datastores/";
    private static final int PARAM_SERVER_OK = 200;
    private static final int PARAM_SERVER_NOT_FOUND = 404;
    private String url;
    private String user;
    private String password;

    public GeoserverHandler(String url, String user, String password) {
        this.url = fixURL(url); // ensure url ends with a '/'
        this.user = user;
        this.password = password;
    }

    public void reloadConfiguration() throws IOException {
        sendRequest("rest/reload/", PARAM_POST, null, "");
    }
    

    
    public String importFeatures(File file, String workspace, String name) throws IOException {
        File wpsRequestFile = createImportProcessXMLFile(file, workspace, name);
        HttpResponse requestResponse = sendRequest("wps", PARAM_POST, PARAM_TEXT_XML, wpsRequestFile);
        return requestResponse.getEntity().getContent().toString();
    }
    
    File createImportProcessXMLFile(File shapeFileZip, String workspace, String name) throws IOException {
        File wpsRequestFile = null;
        FileOutputStream wpsRequestOutputStream = null;
        FileInputStream shapeZipInputStream = null;

        try {
            wpsRequestFile = File.createTempFile("wps.import.", ".xml");
            wpsRequestOutputStream = new FileOutputStream(wpsRequestFile);
            shapeZipInputStream = new FileInputStream(shapeFileZip);

            wpsRequestOutputStream.write(new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    .append("<wps:Execute version=\"1.0.0\" service=\"WPS\" "
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "xmlns=\"http://www.opengis.net/wps/1.0.0\" "
                    + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                    + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                    + "xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                    + "xmlns:gml=\"http://www.opengis.net/gml\" "
                    + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                    + "xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" "
                    + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                    + "xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 "
                    + "http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">")
                    .append("<ows:Identifier>gs:Import</ows:Identifier>")
                    .append("<wps:DataInputs>")
                    .append("<wps:Input>")
                    .append("<ows:Identifier>features</ows:Identifier>")
                    .append("<wps:Data>")
                    .append("<wps:ComplexData mimeType=\"application/zip\"><![CDATA[").toString().getBytes());

            IOUtils.copy(shapeZipInputStream, new Base64OutputStream(wpsRequestOutputStream, true, 0, null));

            wpsRequestOutputStream.write(new StringBuilder("]]></wps:ComplexData>")
                    .append("</wps:Data>")
                    .append("</wps:Input>")
                    .append("<wps:Input>")
                    .append("<ows:Identifier>workspace</ows:Identifier>")
                    .append("<wps:Data>")
                    .append("<wps:LiteralData>").append(workspace).append("</wps:LiteralData>")
                    .append("</wps:Data>")
                    .append("</wps:Input>")
                    .append("<wps:Input>")
                    .append("<ows:Identifier>name</ows:Identifier>")
                    .append("<wps:Data>")
                    .append("<wps:LiteralData>").append(name).append("</wps:LiteralData>")
                    .append("</wps:Data>")
                    .append("</wps:Input>")
                    .append("<wps:Input>")
                    .append("<ows:Identifier>srs</ows:Identifier>")
                    .append("<wps:Data>")
                    .append("<wps:LiteralData>EPSG:900913</wps:LiteralData>")
                    .append("</wps:Data>")
                    .append("</wps:Input>")
                    .append("<wps:Input>")
                    .append("<ows:Identifier>srsHandling</ows:Identifier>")
                    .append("<wps:Data>")
                    .append("<wps:LiteralData>REPROJECT_TO_DECLARED</wps:LiteralData>")
                    .append("</wps:Data>")
                    .append("</wps:Input>")
                    .append("</wps:DataInputs>")
                    .append("<wps:ResponseForm>")
                    .append("<wps:RawDataOutput>")
                    .append("<ows:Identifier>layerName</ows:Identifier>")
                    .append("</wps:RawDataOutput>")
                    .append("</wps:ResponseForm>")
                    .append("</wps:Execute>")
                    .toString().getBytes());

        } finally {
            IOUtils.closeQuietly(wpsRequestOutputStream);
            IOUtils.closeQuietly(shapeZipInputStream);
        }
        return wpsRequestFile;
    }

    public void createDataStore(String shapefilePath, String layer,
            String workspace, String nativeCRS, String declaredCRS) throws IOException {
        
        LOG.debug("Creating data store on WFS server located at: " + url);
        
        String workspacesPath = PARAM_REST_WORKSPACES;
        if (!workspaceExists(workspace)) {
            String workspaceXML = createWorkspaceXML(workspace);
            LOG.debug("Sending XML to GeoServer to create workspace: " + workspace + ". Response follows.");
            HttpResponse htr = sendRequest(workspacesPath, PARAM_POST, PARAM_TEXT_XML, workspaceXML);
        }

        String dataStoresPath = workspacesPath + workspace + PARAM_DATASTORES;
        
        String namespace = "";
        Matcher nsMatcher = Pattern.compile(".*<uri>(.*)</uri>.*", Pattern.DOTALL).matcher(getNameSpaceXML(workspace));
        if (nsMatcher.matches()) {
            namespace = nsMatcher.group(1);
        }
        
        String dataStoreXML = createDataStoreXML(layer, workspace, namespace, shapefilePath);
        if (!dataStoreExists(workspace, layer)) {
            // send POST to create the datastore if it doesn't exist
            sendRequest(dataStoresPath, PARAM_POST, PARAM_TEXT_XML, dataStoreXML);
        } else {
            // otherwise send PUT to ensure that it's pointing to the correct shapefile
            sendRequest(dataStoresPath + layer + PARAM_DOT_XML, PARAM_PUT, PARAM_TEXT_XML, dataStoreXML);
        }

        if (!layerExists(workspace, layer, layer)) {
            // create featuretype based on the datastore
            String featureTypeXML = createFeatureTypeXML(layer, workspace, nativeCRS, declaredCRS);
            String featureTypesPath = dataStoresPath + layer + "/featuretypes.xml";
            sendRequest(featureTypesPath, PARAM_POST, PARAM_TEXT_XML, featureTypeXML);
        }

        // Make sure we render using the default polygon style, and not whatever
        // colored style might have been used before
        sendRequest("rest/layers/" + workspace + ":" + layer, PARAM_PUT, PARAM_TEXT_XML,
                "<layer><defaultStyle><name>polygon</name></defaultStyle>"
                + "<enabled>true</enabled></layer>");
        
        LOG.debug("Datastore successfully created on WFS server located at: " + url);
    }

    String getNameSpaceXML(String workspace) throws IOException {
        return getResponse("rest/namespaces/" + workspace + PARAM_DOT_XML);
    }

    /**
     * @param maximumFileAge 
     * @param workspaces 
     * @throws IOException 
     * @throws XPathExpressionException 
     * @See http://internal.cida.usgs.gov/jira/browse/GDP-174?focusedCommentId=18712&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_18712
     */
    public void deleteOutdatedDataStores(long maximumFileAge, String... workspaces)
            throws IOException, XPathExpressionException {

        LOG.info("Wiping old files task for existing workspaces.");

        long now = new Date().getTime();

        for (String workspace : workspaces) {
            if (!workspaceExists(workspace)) {
                LOG.info("Workspace '" + workspace + "' does not exist on Geoserver. Skipping.");
                continue;
            }

            LOG.info("Checking workspace: " + workspace);
            List<String> dataStoreNames = listDataStores(workspace);
            // Check the data stores in this workspace
            for (String dataStore : dataStoreNames) {
                // Let's get the disk location for this store -- We must remove the "file:" part of the return string
                String diskLocation = retrieveDiskLocationOfDataStore(workspace, dataStore).split("file:")[1];
                File diskLocationFileObject = new File(diskLocation);

                // If either the file is on Geoserver and is old or
                // the file doesn't exist on Geoserver, we will delete the
                // the datastore from geoserver
                boolean deleteFromGeoserver = false;
                if (diskLocationFileObject.exists()) {
                    if (diskLocationFileObject.lastModified() < now - maximumFileAge) {
                        LOG.info("File " + diskLocationFileObject.getPath() + 
                                " older than cutoff. Will remove from Geoserver.");
                        
                        deleteFromGeoserver = true;
                    }
                } else {
                    LOG.info("File " + diskLocationFileObject.getPath() + 
                            " not found on disk. Will remove from Geoserver.");
                    
                    deleteFromGeoserver = true;
                }

                if (deleteFromGeoserver) {
                    deleteAndWipeDataStore(workspace, dataStore);
                }
            }
        }
    }

    /**
     * Deletes the directory the shapefiles are located in on disk.
     * @param workspace 
     * @param dataStore 
     * @throws IOException
     * @throws XPathExpressionException  
     */
    public void deleteAndWipeDataStore(String workspace, String dataStore)
            throws IOException, XPathExpressionException {
        
        String diskLocation = retrieveDiskLocationOfDataStore(workspace, dataStore).split("file:")[1];
        
        deleteDataStore(workspace, dataStore);

        // Get the location of the file from GeoServer
        File diskLocationFileObject = new File(diskLocation);
        if (diskLocationFileObject.isDirectory() && diskLocationFileObject.listFiles().length == 0) {
            // The location is a directory. Delete it
            try { FileUtils.deleteDirectory(diskLocationFileObject); }
            catch (IOException e) { 
                LOG.warn("An error occurred while trying to delete directory" + 
                         diskLocationFileObject.getPath() + ". This may need to " +
                         "be deleted manually. \nError: "+e.getMessage()+"\nContinuing."); 
            }
        } else {
            if(!FileUtils.deleteQuietly(new File(diskLocationFileObject.getParent()))) {
                LOG.warn("Could not fully remove the directory: " + 
                         diskLocationFileObject.getParent() + "\nPossibly files left over.");
            }
        }
    }

    boolean workspaceExists(String workspace) throws IOException {
        int responseCode = getResponseCode(PARAM_REST_WORKSPACES + workspace, PARAM_GET);
        
        return isSuccessResponse(responseCode);
    }


    boolean dataStoreExists(String workspace, String dataStore) throws IOException {
        int responseCode = getResponseCode(PARAM_REST_WORKSPACES + workspace + 
                PARAM_DATASTORES + dataStore, PARAM_GET);
        
        return isSuccessResponse(responseCode);
    }

    boolean layerExists(String workspace, String dataStore, String layerName) throws IOException {
        int responseCode = getResponseCode(PARAM_REST_WORKSPACES + workspace + 
                PARAM_DATASTORES + dataStore + "/featuretypes/" + layerName + PARAM_DOT_XML, PARAM_GET);

        return isSuccessResponse(responseCode);
    }

    boolean styleExists(String styleName) throws IOException {
        int responseCode = getResponseCode("rest/styles/" + styleName, PARAM_GET);

        return isSuccessResponse(responseCode);
    }
    
    boolean isSuccessResponse(int responseCode) {
        switch(responseCode) {
            case PARAM_SERVER_OK: return true;
            default: return false;
        }
    }

    boolean deleteLayer(String layerName, boolean recursive) throws IOException {
        LOG.info("Deleting layer '"+layerName+"'");
        int responseCode = getResponseCode("rest/layers/" + layerName + 
                ((recursive) ? "?recurse=true" : ""), PARAM_DELETE);
        
        switch (responseCode) {
            case PARAM_SERVER_OK: 
                LOG.info("Layer '"+layerName+"' was successfully deleted.");
                break;
            case PARAM_SERVER_NOT_FOUND: 
                LOG.info("Layer '"+layerName+"' was not found on server.");
                break;
            default:  
                LOG.info("Layer '"+layerName+"' could not be deleted.");
        }
        
        return isSuccessResponse(responseCode);
    }

    boolean deleteDataStore(String workspace, String dataStore)
            throws IOException, XPathExpressionException {
        
        LOG.info("Deleting datastore '"+dataStore+"' under workspace '"+workspace+"'");
        
        int responseCode = getResponseCode(PARAM_REST_WORKSPACES + workspace + 
                PARAM_DATASTORES + dataStore + "?recurse=true", PARAM_DELETE);

        switch (responseCode) {
            case PARAM_SERVER_OK: 
                LOG.info("Datastore '"+workspace+":"+dataStore+"' was successfully deleted.");
                break;
            case PARAM_SERVER_NOT_FOUND: 
                LOG.info("Datastore '"+workspace+":"+dataStore+"' was not found on server.");
                break;
            default:  
                LOG.info("Datastore '"+workspace+":"+dataStore+"' could not be deleted.");
        }
        
        return isSuccessResponse(responseCode);
    }

    /**
     * Attempts to remove a featuretype from underneath a datastore on the Geoserver server.
     */
    boolean deleteFeatureType(String workspace, String dataStore, String featureType, boolean recursive) 
            throws IOException {

        LOG.info("Deleting feature type '"+workspace+":"+featureType+"'");

        int responseCode = getResponseCode(PARAM_REST_WORKSPACES + workspace + 
                PARAM_DATASTORES + dataStore + "/featuretypes/" + featureType + 
                ((recursive) ? "?recurse=true" : ""), PARAM_DELETE);
        
        switch (responseCode) {
            case PARAM_SERVER_OK: 
                LOG.info("Feature type '"+workspace+":"+featureType+"' was successfully deleted.");
                break;
            case PARAM_SERVER_NOT_FOUND: 
                LOG.info("Feature type '"+workspace+":"+featureType+"' was not found on server.");
                break;
            default:  
                LOG.info("Feature type '"+workspace+":"+featureType+"' could not be deleted.");
        }
        
        return isSuccessResponse(responseCode);
    }

    String retrieveDiskLocationOfDataStore(String workspace, String dataStore) 
            throws IOException, XPathExpressionException {
        
        String responseXML = getResponse(PARAM_REST_WORKSPACES + workspace + PARAM_DATASTORES + dataStore + PARAM_DOT_XML);
        
        String result = XMLUtils.createNodeUsingXPathExpression(
                "/dataStore/connectionParameters/entry[@key='url']", responseXML).getTextContent();

        return result;
    }

    public List<String> listWorkspaces()
            throws IOException, XPathExpressionException {
        
        return createListFromXML("/workspaces/workspace/name", getWorkspacesXML());
    }

    public List<String> listDataStores(String workspace)
            throws IOException, XPathExpressionException {
        
        if (!workspaceExists(workspace)) {
            return new ArrayList<String>(0);
        }

        return createListFromXML("/dataStores/dataStore/name", getDataStoresXML(workspace));
    }
    
    public List<String> listFeatureTypes(String workspace, String dataStore)
            throws XPathExpressionException, IOException {
        
        if (!workspaceExists(workspace) || !dataStoreExists(workspace, dataStore)) {
            return new ArrayList<String>(0);
        }
        
        return createListFromXML("/featureTypes/featureType/name", 
                getFeatureTypesXML(workspace, dataStore));
    }
    
    List<String> createListFromXML(String expression, String xml) 
            throws XPathExpressionException, UnsupportedEncodingException {
        
        NodeList nodeList = XMLUtils.createNodeListUsingXPathExpression(expression, xml);
        
        List<String> result = new ArrayList<String>(nodeList.getLength());
        for (int nodeIndex = 0;nodeIndex < nodeList.getLength();nodeIndex++) {
            result.add(nodeList.item(nodeIndex).getTextContent());
        }
        
        return result;
    }

    String getWorkspacesXML() throws IOException {
        return getResponse("rest/workspaces.xml");
    }

    String getDataStoresXML(String workspace) throws IOException {
        return getResponse(PARAM_REST_WORKSPACES + workspace + "/datastores.xml");
    }

    String getFeatureTypesXML(String workspace, String dataStore) throws IOException {
        return getResponse(PARAM_REST_WORKSPACES + workspace + PARAM_DATASTORES + dataStore + "/featuretypes.xml");
    }
    
    int getResponseCode(String path, String requestMethod) throws IOException {
        HttpResponse response = sendRequest(path, requestMethod, null, "");
        return response.getStatusLine().getStatusCode();
    }
    
    String getResponse(String path) throws IOException {
        
        HttpResponse response = sendRequest(path, PARAM_GET, null, "");
        
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();

            HttpEntity entity = response.getEntity();
            entity.writeTo(baos);
        } finally {
            baos.close();
        }
        
        return baos.toString();
    }
    
    HttpResponse sendRequest(String path, String requestMethod, String contentType, File content) throws FileNotFoundException, IOException {
        HttpPost post = null;
        HttpClient httpClient = new DefaultHttpClient();
        
        post = new HttpPost(url + path);

        FileInputStream wpsRequestInputStream = null;
        try {
            wpsRequestInputStream = new FileInputStream(content);

            AbstractHttpEntity entity = new InputStreamEntity(wpsRequestInputStream, content.length());
        
            post.setEntity(entity);

            HttpResponse response = httpClient.execute(post);

            return response;

        } finally {
            IOUtils.closeQuietly(wpsRequestInputStream);
        }
    }
    
    HttpResponse sendRequest(String path, String requestMethod, String contentType, String content)
            throws IOException {
        
        String fullURL = url + path;
        
        HttpUriRequest request = null;
        
        if (PARAM_GET.equals(requestMethod)) {
            request = new HttpGet(fullURL);
            
        } else if (PARAM_POST.equals(requestMethod)) {
            request = new HttpPost(fullURL);
            
        } else if (PARAM_PUT.equals(requestMethod)) {
            request = new HttpPut(fullURL);
            
        } else if (PARAM_DELETE.equals(requestMethod)) {
            request = new HttpDelete(fullURL);
            
        } else {
            throw new InvalidParameterException();
        }
        
        HttpClient client = new DefaultHttpClient();
        
        //Set authentication
        String encoding = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes());
        request.addHeader("Authorization", "Basic " + encoding);
        
        if (contentType != null) {
            request.addHeader("Content-Type", contentType);
        }

        if (StringUtils.isBlank(content) && request instanceof HttpEntityEnclosingRequestBase) {
            StringEntity contentEntity = new StringEntity(content);
            ((HttpEntityEnclosingRequestBase) request).setEntity(contentEntity);
        }
        HttpResponse response = client.execute(request);
        LOG.debug("Response: " + response.getStatusLine().getReasonPhrase() + " " + response.getStatusLine().getStatusCode());
        return response;
    }
  
    static String createWorkspaceXML(String workspace) {
        return "<workspace><name>" + workspace + "</name></workspace>";
    }

    static String createDataStoreXML(String name, String workspace, String namespace, String url) {

        return  "<dataStore>" +
                "  <name>" + name + "</name>" +
                "  <type>Shapefile</type>" +
                "  <enabled>true</enabled>" +
                "  <workspace>" +
                "    <name>" + workspace + "</name>" +
                "  </workspace>" +
                "  <connectionParameters>" +
                "    <entry key=\"memory mapped buffer\">true</entry>" +
                "    <entry key=\"create spatial index\">true</entry>" +
                "    <entry key=\"charset\">ISO-8859-1</entry>" +
                "    <entry key=\"url\">file:" + url + "</entry>" +
                "    <entry key=\"namespace\">" + namespace + "</entry>" +
                "  </connectionParameters>" +
                "</dataStore>";
    }

    static String createFeatureTypeXML(String name, String workspace, String nativeCRS, String declaredCRS) {

        return  "<featureType>" +
                "  <name>" + name + "</name>" +
                "  <nativeName>" + name + "</nativeName>" +
                "  <namespace>" +
                "    <name>" + workspace + "</name>" +
                "  </namespace>" +
                "  <title>" + name + "</title>" +
                // use CDATA as this may contain WKT with XML reserved characters
                "  <nativeCRS><![CDATA[" + nativeCRS + "]]></nativeCRS>" +
                "  <srs>" + declaredCRS + "</srs>" +
                "  <projectionPolicy>REPROJECT_TO_DECLARED</projectionPolicy>" +
                "  <enabled>true</enabled>" +
                "  <metadata>"+
                "    <entry key=\"cachingEnabled\">true</entry>" +
                "  </metadata>" +
                "  <store class=\"dataStore\">" +
                "    <name>" + name + "</name>" + // this is actually the datastore name (we keep it the same as the layer name)
                "  </store>" +
                "</featureType>";
}
    
    /**
     *  Ensure url ends with a '/'
     */
    static String fixURL(String url) {
        String localUrl = "";
        Matcher matcher = Pattern.compile("/$").matcher(url);
        if (!matcher.matches()) {
            localUrl = url + "/";
        }

        return localUrl;
    }
}