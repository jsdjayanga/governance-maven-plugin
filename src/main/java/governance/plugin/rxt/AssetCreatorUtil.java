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
	
}
