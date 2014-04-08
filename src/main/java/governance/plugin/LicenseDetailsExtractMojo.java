package governance.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import governance.plugin.common.RegistrySOAPClient;
import governance.plugin.handler.LicenseDetailsExtractHandler;
import governance.plugin.util.Configurations;
import governance.plugin.util.MavenProjectScanner;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

@Mojo(name = "licfill", defaultPhase = LifecyclePhase.DEPLOY,  aggregator = true)
public class LicenseDetailsExtractMojo extends AbstractMojo{
	@Parameter ( defaultValue = "${project}" )
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
    
    @Parameter( property = "resolvePerPom", defaultValue = "true")
   	private boolean resolvePerPom;
    
	private Configurations configurations;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		configurations = new Configurations(project, settings, repositoryLocation, gregServiceUrl, gregUsername, gregPassword, gregHome, buildProfile);
		
		LicenseDetailsExtractHandler LicenseDetailsExtractHandler = new LicenseDetailsExtractHandler(configurations, getLog(), resolvePerPom);
		
		configure();
    	
    	getLog().info("Retreving pom tree");
    	List<MavenProject> pomTree = MavenProjectScanner.getEffectivePOMTree(configurations.getRepoLocation() ,configurations.getBuildProfileId());
    	
    	LicenseDetailsExtractHandler.process(pomTree);
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
