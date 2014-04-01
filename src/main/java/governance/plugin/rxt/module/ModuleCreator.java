package governance.plugin.rxt.module;

import governance.plugin.common.GovernanceSOAPMessageCreator;


import governance.plugin.rxt.AbstractAssetCreator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class ModuleCreator extends AbstractAssetCreator {

	public static final String GREG_MODULE_RESOURCE_PATH = "/trunk/modules/";
	public static  String DEFAULT_MODULE_TYPE = "Unknown";
	
	private Log logger;
	
	public ModuleCreator(Log logger, String gregServiceUrl)
			throws MojoExecutionException{
		super(gregServiceUrl, "Module.ModuleHttpsSoap11Endpoint");
		this.logger = logger;	

	}
	
	@Override
	/**
	 * @param parameters Array of 2 strings, where first elements must be the 'artifactId'
	 * 					 and second element must be the 'version' of the module
	 * @return the relative Greg repository resource path of the module.
	 */
	public String getResourcePath(String[] parameters) throws MojoExecutionException{
		if (parameters.length != 2){
			throw new MojoExecutionException("Module Resource Path expects 2 parameters: " +
					"'artifactID' and 'version'");
		}
		
		String artifactID = parameters[0];
		String version = parameters[1];
		
		return ModuleCreator.GREG_MODULE_RESOURCE_PATH  + artifactID + "/" + version;
	}
	
	/**
	 * Create a Module asset. If the Module is already existing delete all its dependencies
	 */
	@Override
	public boolean create(Object[] parameters) throws MojoExecutionException{
		if (parameters.length != 3){
			throw new MojoExecutionException("Module Creater expects 3 Parameters: " +
					"'artifactID', 'version' and 'filepath' as parameters");
		}
		
		String artifactID = (String)parameters[0];
		String version = (String)parameters[1];
		String filePath = (String)parameters[2];
		
		String modulePath = getResourcePath(new String[]{artifactID, version});
		
		String createModuleRequst = 
    			GovernanceSOAPMessageCreator.createAddModuleRequest(artifactID, version, filePath, DEFAULT_MODULE_TYPE);
			
		boolean isModuleExisting = super.createAsset(modulePath, createModuleRequst);
		
		if (logger.isInfoEnabled()){
    		if (isModuleExisting){
    			logger.info("Request sent to create 'Module': "+ artifactID + ":" +  version);
    		}else{
    			logger.debug("Module already available: " + artifactID + ":" +  version);
    		}
		}
		
		return isModuleExisting;
	}
	
	/**
	 * Get the relative path of a module if the module is existing in the registry
	 * @param artifactId module artifact id
	 * @param version module version
	 * @return relative path of a module
	 * @throws MojoExecutionException
	 */
	public String getPathIfModuleExisting(String artifactId, String version) throws MojoExecutionException{
		String modulePath = getResourcePath(new String[]{artifactId, version});
		return super.assetCreatorUtil.isAssetExisting(modulePath)? modulePath : null;
	}
	
	public boolean isModuleExisting(String artifactId, String version) throws MojoExecutionException{
		String modulePath = getResourcePath(new String[]{artifactId, version});
		return super.assetCreatorUtil.isAssetExisting(modulePath);
	}
	
}
