package governance.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GRegDependencyHandler {

	public static final String GREG_ASSOCIATION_TYPE_DEPENDS = "depends";
	public static final String GREG_ASSOCIATION_TYPE_USEDBY = "usedBy";
	
	public static final String GREG_ASSOCIATION_ACTION_ADD = "add";
	public static final String GREG_ASSOCIATION_ACTION_REMOVE = "remove";
	
	public static final String GREG_DEP_TREE_ELEMENT_NAME = "ax2457:treeCache";
	
	private Log logger;
	private String relationServiceEndPointRef;
	
	private int addedAssocationCount = 0;
	private int removedAssosicationCount = 0;
	
	GRegDependencyHandler(Log logger, String gregServiceUrl){
		this.logger = logger;
		this.relationServiceEndPointRef = gregServiceUrl + "RelationAdminService.RelationAdminServiceHttpsSoap11Endpoint";
	}
	/**
	 * Delete all current dependencies of a given resource of a given type
	 * @param absoluteResourcePath source resource path
	 * @param type Type of the dependency
	 * @throws MojoExecutionException 
	 */
	public void removeExistingAssociations(String absoluteResourcePath, String type) throws MojoExecutionException{
		String getDependencyTreeRequt = GovernanceSOAPMessageCreator.
				createGetDependencyTreeRequest(absoluteResourcePath, type);
		
		String response = RegistrySOAPClient.sendMessage(relationServiceEndPointRef, getDependencyTreeRequt);
		
		Document responseMessage = XmlParser.parseXmlString(RegistrySOAPClient.stripMessage(response));
		NodeList nodes = responseMessage.getElementsByTagName(GREG_DEP_TREE_ELEMENT_NAME);

		if (logger.isDebugEnabled()){
			logger.debug("Removing " + nodes.getLength() + " dependencies from " + absoluteResourcePath);
		}
		
		for (int i = 0; i < nodes.getLength(); i++){
			Node currentNode = nodes.item(i);
			String dependencyReourcePath = currentNode.getTextContent();
			removeAssociation(absoluteResourcePath, dependencyReourcePath, GREG_ASSOCIATION_TYPE_DEPENDS);
		}
	}
	
	public int getAddedAssocationCount(){
		return addedAssocationCount;
	}
	
	public int getRemovedAssocationCount(){
		return removedAssosicationCount;
	}
	
	public void addAssociation(String sourcePath, String destinationPath, String type) throws MojoExecutionException{
		manlipulateAssocation(sourcePath, destinationPath, type, GREG_ASSOCIATION_ACTION_ADD);
		addedAssocationCount++;
     }
	
	public void removeAssociation(String sourcePath, String destinationPath, String type) throws MojoExecutionException{
		manlipulateAssocation(sourcePath, destinationPath, type, GREG_ASSOCIATION_ACTION_REMOVE);
		removedAssosicationCount++;
     }
	
	public void manlipulateAssocation(String sourcePath, String destinationPath, String type, String todo) 
			throws MojoExecutionException{
		String manipulateAssocationRequest = GovernanceSOAPMessageCreator.
    			createAssociationRequst(sourcePath, destinationPath, type, todo);
    	
    	RegistrySOAPClient.sendMessage(relationServiceEndPointRef, manipulateAssocationRequest);
    	
    	if (logger.isDebugEnabled()){
    		logger.debug("'" +todo + "'assocation of type '" + type + "'[Source:"+sourcePath + ", destination:" +  destinationPath +"]");
    	}
	}
}
