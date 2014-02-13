package governance.plugin.rxt.artifact;

import governance.plugin.ModuleDependecnyMojo;
import governance.plugin.GovernanceSOAPMessageCreator;
import governance.plugin.rxt.AbstractAssetCreator;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class ArtifactCreator extends AbstractAssetCreator{
	private Log logger;
	
	public ArtifactCreator(Log logger, String gregServiceUrl) 
			throws MojoExecutionException{
		super(gregServiceUrl, "Artifact.ArtifactHttpsSoap11Endpoint");
		this.logger = logger;	
	}
	
	@Override
	public String getResourcePath(String[] parameters) throws MojoExecutionException{
		if (parameters.length != 3){
			throw new MojoExecutionException("Artifact Resource Path expects 3 parameters:" +
					"'GroupID','ArtifactID' and 'Version'");
		}
		
		String groupId = parameters[0];
		String artifactId = parameters[1];
		String version = parameters[2];
		
		return ModuleDependecnyMojo.GREG_ARTIFACT_RESOURCE_PATH  + groupId + "/" + artifactId + "/" + version;
	}
	
	@Override
	public boolean create(Object[] parameters) throws MojoExecutionException{
		if (parameters.length != 3){
			throw new MojoExecutionException("Module Creater expects 3 parameters:" +
					"'GroupID', 'ArtifactID' and 'version'");
		}
		
		String groupId = (String)parameters[0];
		String artifactId = (String)parameters[1];
		String version = (String)parameters[2];
		
		String artifactPath = getResourcePath(new String[]{groupId, artifactId, version});
		
		String createArtifactRequst = 
    			GovernanceSOAPMessageCreator.createAddArtifactRequest(groupId, artifactId, version);
		
		boolean isDependencyCreated = super.createAsset(artifactPath, createArtifactRequst);
		
		if (logger.isInfoEnabled()){
    		if (isDependencyCreated){
    			logger.info("Request sent to create 'Artifact': "+ groupId + "/" +  artifactId +  "/" + version);
    		}else{
    			logger.debug("'Artifact' already available: " + groupId + "/" +  artifactId +  "/" + version);
    		}
		}
		
		return isDependencyCreated;
	}
}
