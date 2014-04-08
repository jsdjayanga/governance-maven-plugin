package governance.plugin.rxt.artifact;

import governance.plugin.common.GovernanceSOAPMessageCreator;
import governance.plugin.rxt.AbstractAssetCreator;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class ArtifactCreator extends AbstractAssetCreator{
	public static final String GREG_ARTIFACT_RESOURCE_PATH = "/trunk/artifacts/";
	
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
		
		return ArtifactCreator.GREG_ARTIFACT_RESOURCE_PATH  + groupId + "/" + artifactId + "/" + version;
	}
	
	@Override
	public boolean create(Object[] parameters) throws MojoExecutionException{
		if (parameters.length < 3){
			throw new MojoExecutionException("Module Creater expects 3 mandatory parameters:" +
					"'GroupID', 'ArtifactID' and 'version'(3 Optional parameters: 'JarName', 'LicenseType' and 'PackagingType'");
		}
		
		String groupId = (String)parameters[0];
		String artifactId = (String)parameters[1];
		String version = (String)parameters[2];
		String jarName = (parameters.length > 3) ? (String)parameters[3] : null;
		String licenseType = (parameters.length > 4) ? (String)parameters[4] : null;
		String packaginType = (parameters.length > 5) ? (String)parameters[5] : null;
		
		String artifactPath = getResourcePath(new String[]{groupId, artifactId, version});
		
		String createArtifactRequst = 
    			GovernanceSOAPMessageCreator.createAddArtifactRequest(groupId, artifactId, version, jarName, licenseType, packaginType);
		
		boolean isDependencyCreated = super.createAsset(artifactPath, createArtifactRequst);
		
		if (isDependencyCreated){
			logger.info("Request sent to create 'Artifact': "+ groupId + ":" +  artifactId +  ":" + version);
		}else{
			logger.debug("'Artifact' already available: " + groupId + ":" +  artifactId +  ":" + version);
		}
		
		return isDependencyCreated;
	}
	
	public String getPathIfArtifactExisting(String groupId, String artifactId, String version) throws MojoExecutionException{
		String artifactPath = getResourcePath(new String[]{groupId, artifactId, version});
		return super.assetCreatorUtil.isAssetExisting(artifactPath)? artifactPath : null;
	}
}
