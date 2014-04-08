package governance.plugin;

import governance.plugin.common.RegistrySOAPClient;
import governance.plugin.handler.PackLicenseDetailsExtractHandler;
import governance.plugin.util.Configurations;
import governance.plugin.util.MavenProjectScanner;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

@Mojo(name = "packlicfill", defaultPhase = LifecyclePhase.DEPLOY,  aggregator = true)
public class PackLicenseDetailsExtractMojo extends AbstractMojo{
	@Parameter (defaultValue = "${project}")
    private MavenProject project;
	
    @Parameter( property = "location" )
    private String repositoryLocation;
    
    @Parameter( defaultValue = "${settings}" )
    private Settings settings;
    
    @Parameter( property = "gregServiceUrl")
    private String gregServiceUrl;
    
    @Parameter( property = "gregUsername")
    private String gregUsername;
    
    @Parameter( property = "gregPassword")
    private String gregPassword;
    
    @Parameter( property = "gregHome")
    private String gregHome;
    
    @Parameter( property = "buildProfile")
   	private String buildProfile;
    
    @Parameter( property = "gethLicDetialsPerPom", defaultValue = "true")
   	private boolean resolvePerPom;
    
    @Parameter( property = "packPath", required=true)
   	private String packPath;
    
	private Configurations configurations;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		configurations = new Configurations(project, settings, repositoryLocation, gregServiceUrl, gregUsername, gregPassword, gregHome, buildProfile);
		
		PackLicenseDetailsExtractHandler packLicenseDetailsExtractHandler = new PackLicenseDetailsExtractHandler(configurations, getLog());
		
		configure();
		
		List<MavenProject> pomTree = MavenProjectScanner.getPOMTree(configurations.getRepoLocation() ,configurations.getBuildProfileId());
		
		packLicenseDetailsExtractHandler.process(new File(packPath), pomTree);
	}
	
	private void configure(){
    	System.setProperty("javax.net.ssl.trustStore", configurations.getGregHome() +  File.separator + "repository" + File.separator + 
    	                   "resources" + File.separator + "security"+ File.separator + "client-truststore.jks");
    	
    	System.setProperty("javax.net.ssl.trustStorePassword","wso2carbon");
    	System.setProperty("javax.net.ssl.trustStoreType","JKS");
    	
    	System.setProperty("javax.net.ssl.trustStore", configurations.getGregHome() + File.separator + "repository" +
                File.separator + "resources" + File.separator + "security" + File.separator +
                "wso2carbon.jks");
    	
    	RegistrySOAPClient.setCredentials(configurations.getGregUserName(), configurations.getGregPassword());
    }

}
