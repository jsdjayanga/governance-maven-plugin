package governance.plugin.rxt.otherdependency;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import governance.plugin.common.GovernanceSOAPMessageCreator;
import governance.plugin.rxt.AbstractAssetCreator;

public class OtherDependencyCreator extends AbstractAssetCreator {

	public static final String GREG_OTHER_DEPENDENCY_RESOURCE_PATH = "/trunk/other-dependency/";
	private Log logger;
	
	public OtherDependencyCreator(Log logger, String gregServiceUrl) 
			throws MojoExecutionException {
	    super(gregServiceUrl, "OtherDependency.OtherDependencyHttpsSoap11Endpoint");
		this.logger = logger;
	    // TODO Auto-generated constructor stub
    }

	@Override
    public boolean create(Object[] parameters) throws MojoExecutionException {
		if (parameters.length < 2){
			throw new MojoExecutionException("Module Creater expects 2 mandatory parameters:" +
					"'Archive Name' and 'Packaging Type' (2 Optional parameters: 'Version' and 'LicenseType'");
		}
		
		String archiveName = (String)parameters[0];
		String packaginType = (String)parameters[1];
		String version = (parameters.length > 2) ? (String)parameters[2] : null;
		String licenseType = (parameters.length > 3) ? (String)parameters[3] : null;
		
		String artifactPath = getResourcePath(new String[]{archiveName});
		
		String createOtherDependencyRequst = 
    			GovernanceSOAPMessageCreator.createOtherDependency(archiveName, packaginType, version, licenseType);
		
		boolean isDependencyCreated = super.createAsset(artifactPath, createOtherDependencyRequst);
		
		if (isDependencyCreated){
			logger.info("Request sent to create 'Other Dependency': " + archiveName);
		}else{
			logger.debug("'Other Dependency' already available: " + archiveName);
		}
		
		return isDependencyCreated;
    }

	@Override
    public String getResourcePath(String[] parameters) throws MojoExecutionException {
		if (parameters.length != 1){
			throw new MojoExecutionException("Other Dependency Resource Path expects 1 parameters:" +
					"'Archive Name'");
		}
		
		String jarName = parameters[0];
		
		return OtherDependencyCreator.GREG_OTHER_DEPENDENCY_RESOURCE_PATH  + jarName;
    }

}
