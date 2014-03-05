package governance.plugin;

import governance.plugin.handler.ModuleDependecnyHandler;
import governance.plugin.handler.ServiceDependencyHandler;
import governance.plugin.common.RegistrySOAPClient;
import governance.plugin.handler.WebappDependencyHandler;
import governance.plugin.util.Configurations;
import governance.plugin.util.EffectivePom;
import governance.plugin.util.MavenProjectScanner;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.List;

/**
 * Created by jayanga on 3/5/14.
 */
@Mojo( name = "all", defaultPhase = LifecyclePhase.DEPLOY,  aggregator = true)
public class AggregatedDependencyMojo extends AbstractMojo{
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


    private Configurations configurations;

    public AggregatedDependencyMojo() throws MojoExecutionException{

    }

    public void execute() throws MojoExecutionException
    {
        configurations = new Configurations(project, settings, repositoryLocation, gregServiceUrl, gregUsername, gregPassword, gregHome, buildProfile);

        configure();

        getLog().info("Reading POM tree from:" +  configurations.getRepoLocation());
        List<MavenProject> pomTree = MavenProjectScanner.getEffectivePOMTree(configurations.getRepoLocation() ,configurations.getBuildProfileId());

        ModuleDependecnyHandler moduleDependecnyHandler = new ModuleDependecnyHandler(configurations, getLog());
        moduleDependecnyHandler.process(pomTree);

        ServiceDependencyHandler serviceDependencyHandler = new ServiceDependencyHandler(configurations, getLog());
        serviceDependencyHandler.process(pomTree);

        WebappDependencyHandler handler = new WebappDependencyHandler(configurations, getLog());
        handler.process(pomTree);
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
