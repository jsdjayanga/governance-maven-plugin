package governance.plugin.rxt;

import governance.plugin.GovernanceSOAPMessageCreator;
import governance.plugin.ModuleDependecnyMojo;
import governance.plugin.RegistrySOAPClient;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AssetCreatorUtil {

	private String genericArtifactManagerEndPointRef;
	
	public AssetCreatorUtil(String gregServiceUrl) throws MojoExecutionException{
		this.genericArtifactManagerEndPointRef = gregServiceUrl + 
				"ManageGenericArtifactService.ManageGenericArtifactServiceHttpsSoap11Endpoint";
	}
	
	public boolean isAssetExisting(String resourcePath) throws MojoExecutionException{	
		String getAssetContentRequest = GovernanceSOAPMessageCreator.createGetArtifactContentRequest(resourcePath);
		String response = RegistrySOAPClient.sendMessage(genericArtifactManagerEndPointRef, getAssetContentRequest);
		return ((response != null) && (isContentAvailable(response)));
	}

	public boolean isContentAvailable(String responseString) throws MojoExecutionException{
	        Document doc = null;
	        String textContent = null;
            try {
	            doc = RegistrySOAPClient.parseXmlString(RegistrySOAPClient.stripMessage(responseString));
	            NodeList nodes = doc.getElementsByTagName("ns:return");
	            if (nodes != null && nodes.getLength() != 0){
	            	Node returnNode = nodes.item(0);
    	            if (returnNode != null){
    	            	textContent = returnNode.getTextContent();
    	            }
	            }
            } catch (Exception e) {
            	throw new MojoExecutionException(e.getMessage());
            }
            return ((textContent != null) && (!textContent.isEmpty()));
	}
}
