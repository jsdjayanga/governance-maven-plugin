package governance.plugin.rxt;

import governance.plugin.ModuleDependecnyMojo;
import governance.plugin.RegistrySOAPClient;

import org.apache.maven.plugin.MojoExecutionException;


public abstract class  AbstractAssetCreator{

	public static final String GREG_TRUNK_LOCATION = "/_system/governance";
	
	private int createdAssetCount = 0;
	private int existingAssetCount = 0;
	
	private String assetEndPointRef;
	
	protected AssetCreatorUtil assetCreatorUtil;
	
	public AbstractAssetCreator(String gregServiceUrl, String assetEndPoint) throws MojoExecutionException{
		this.assetEndPointRef = gregServiceUrl + assetEndPoint;
		assetCreatorUtil = new AssetCreatorUtil(gregServiceUrl);	
	}
	
	public abstract boolean create(Object[] parameters) throws MojoExecutionException;
	
	public abstract String getResourcePath(String[] parameters)throws MojoExecutionException;
	
	public final int getCreatedAssetCount() {
	    return createdAssetCount;
    }

	public final int getExistingAssetCount() {
	    return existingAssetCount;
    }
	
	public final void increaseCreatedAssetCount(){
		createdAssetCount++;
	}
	
	public final void increasExistingAssetCount(){
		existingAssetCount++;
	}
	
	public final String getAssetEndpointRef(){
		return assetEndPointRef;
	}
		
	public boolean createAsset(String relativeResourcePath, String soapRequest) throws MojoExecutionException{
		boolean isAssetCreated = false;
		if (assetCreatorUtil.isAssetExisting(relativeResourcePath) == false){
			RegistrySOAPClient.sendMessage(assetEndPointRef, soapRequest);
			createdAssetCount++;
			isAssetCreated = true;
		}else{
			existingAssetCount++;
		}
		return isAssetCreated;
	}

	public String getAbsoluteResourcePath(String[] parameters) throws MojoExecutionException{
		return AbstractAssetCreator.GREG_TRUNK_LOCATION + getResourcePath(parameters);
	}
}
