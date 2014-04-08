package governance.plugin.rxt;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import governance.plugin.common.GovernanceSOAPMessageCreator;
import governance.plugin.common.RegistrySOAPClient;
import governance.plugin.common.XmlParser;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AssetCreatorUtil {
	public static final String LICENSE_ELEMENT_NAME = "license";
	public static final String JAR_FILE_ELEMENT_NAME = "jarName";
	public static final String PACKAGINTYPE_ELEMENT_NAME = "packagingType"; 
	public static final String MAVEN_BUNDEL_PLUGIN_KEY = "org.apache.felix:maven-bundle-plugin";
	
	private String genericArtifactManagerEndPointRef;
	
	public AssetCreatorUtil(String gregServiceUrl) throws MojoExecutionException{
		this.genericArtifactManagerEndPointRef = gregServiceUrl + 
				"ManageGenericArtifactService.ManageGenericArtifactServiceHttpsSoap11Endpoint";
	}
	
	/**
	 * Edits an existing asset
	 * @param resourcePath Absolute resource path of the asset to be modified
	 * @param assetType name  type of the asset (e.g. "module", "artifact", "service")
	 * @param Document Xml document containing the modified content
	 * @throws MojoExecutionException 
	 */
	public void editAsset(String resourcePath, String assetType, Document newAssetContent) throws MojoExecutionException{
		try {
    		TransformerFactory tf = TransformerFactory.newInstance();
    		Transformer transformer = tf.newTransformer();
    		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    		StringWriter writer = new StringWriter();
	        transformer.transform(new DOMSource(newAssetContent), new StreamResult(writer));
	    	String assteContentText = writer.getBuffer().toString().replaceAll("\n|\r", "");
	    	String modifyAssetMessage = GovernanceSOAPMessageCreator.createEditArtifactMessage(AbstractAssetCreator.GREG_TRUNK_LOCATION + resourcePath, assetType, assteContentText);

	    	RegistrySOAPClient.sendMessage(this.genericArtifactManagerEndPointRef, modifyAssetMessage);
        } catch (TransformerException e) {
	        throw new MojoExecutionException(e.getMessage(), e);
        }
	}
	
	public boolean isAssetExisting(String resourcePath) throws MojoExecutionException{	
		return isContentAvailable(resourcePath);
	}
	
	private boolean isContentAvailable(String resourcePath) throws MojoExecutionException{
		Document doc = null;
        try {
        	 doc = getAssetContent(resourcePath);
        } catch (Exception e) {
        	throw new MojoExecutionException(e.getMessage(), e);
        }
        return (doc != null);
	}
	
	/**
	 * Give a XML document which contains information of an asset
	 * @param resourcePath Relative path of the asset to be retrieved  
	 * @return Contents of the asset if asset is available, otherwise null
	 * @throws MojoExecutionException
	 */
	public Document getAssetContent(String resourcePath) throws MojoExecutionException{
		  Document contentDoc = null; 
		  String responseString = getAssetContentFromRepository(resourcePath);
		  Document doc = XmlParser.parseXmlString(RegistrySOAPClient.stripMessage(responseString));
		  NodeList nodes = doc.getElementsByTagName("ns:return");
		  
		  if (nodes != null && nodes.getLength() != 0){
          	Node contentsNode = nodes.item(0);
          	if (contentsNode.getTextContent()!= null && !contentsNode.getTextContent().isEmpty()){
          		contentDoc = XmlParser.parseXmlString(contentsNode.getTextContent()); 
          	}
		  }
		  return contentDoc;
	}

	private String getAssetContentFromRepository(String resourcePath) throws MojoExecutionException{
		String getAssetContentRequest = GovernanceSOAPMessageCreator.createGetArtifactContentRequest(resourcePath);
		String response = RegistrySOAPClient.sendMessage(genericArtifactManagerEndPointRef, getAssetContentRequest);
		return response;
	}
	
	public static String getKey(Dependency dependecny){
		return getKey(dependecny.getGroupId(), dependecny.getArtifactId(), dependecny.getVersion());
	}
	
	public static String getKey(String groupId, String artifactId, String version){
		StringBuilder key = new StringBuilder();
		key.append(groupId);
		key.append(":");
		key.append(artifactId);
		key.append(":");
		key.append(version);
		return key.toString();
	}
	
	
	public static void replaceFieldInOverviewTable(Document existingContent, String value, String elementName){
		Node newNode = existingContent.createElement(elementName);
		newNode.appendChild(existingContent.createTextNode(value));
		Element overviewNode = (Element)existingContent.getElementsByTagName("overview").item(0);
		
		NodeList existingElements = overviewNode.getElementsByTagName(elementName);
		for (int i = 0; i < existingElements.getLength(); i++){
			overviewNode.removeChild(existingElements.item(i));
		}
		overviewNode.appendChild(newNode);
	}
	
	public static Document addJARFileNameToAsset(Document assetContent, String jarFileName){
		Element overviewNode = (Element)assetContent.getElementsByTagName("overview").item(0);
		
		NodeList existingElements = overviewNode.getElementsByTagName(AssetCreatorUtil.JAR_FILE_ELEMENT_NAME);
		StringBuilder jarFileNames = new StringBuilder();
		for (int i = 0; i < existingElements.getLength(); i++){
			if (i == 0){
				jarFileNames.append(existingElements.item(i).getTextContent());
			}else{
				jarFileNames.append(",");
				jarFileNames.append(existingElements.item(i).getTextContent());
			}
			overviewNode.removeChild(existingElements.item(i));
		}  
		
		if (jarFileNames.toString().isEmpty()){
			jarFileNames.append(jarFileName);
		}else if(!jarFileNames.toString().contains(jarFileName)){
			jarFileNames.append(",");
			jarFileNames.append(jarFileName);
		}
		
		Node newNode = assetContent.createElement(AssetCreatorUtil.JAR_FILE_ELEMENT_NAME);
		newNode.appendChild(assetContent.createTextNode(jarFileNames.toString()));
		overviewNode.appendChild(newNode);
		
		return assetContent;
	}
	
	public static boolean isJarsEmbeded(MavenProject project){
		String packagingType = project.getPackaging();
		if (packagingType.equals("bundle")){
			Plugin bundelPlugin = project.getPlugin(MAVEN_BUNDEL_PLUGIN_KEY);
			if (bundelPlugin != null){
				Xpp3Dom pluginConfigs = (Xpp3Dom)bundelPlugin.getConfiguration();
				if (pluginConfigs != null){
					Xpp3Dom instuctionConfigs = pluginConfigs.getChild("instructions");
					if (instuctionConfigs != null){
						Xpp3Dom embedDependencyInstructions = instuctionConfigs.getChild("Embed-Dependency");
						if (embedDependencyInstructions != null){
							return true;
						}
					}
					
				}
			}
		}
		return false;
	}
	
	public static Document addPackagingTypeToAsset(Document assetContent, String packageingType){
		replaceFieldInOverviewTable(assetContent, packageingType, PACKAGINTYPE_ELEMENT_NAME);
		return assetContent;
	}
	
	public static Document addLicenseTypeToAssset(Document assetContent, String licenseType){
		if (licenseType != null){
			replaceFieldInOverviewTable(assetContent, licenseType, LICENSE_ELEMENT_NAME);
		}
		return assetContent;
	}
}
