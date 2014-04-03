package governance.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import governance.plugin.common.RegistrySOAPClient;
import governance.plugin.handler.OSGiServiceDependencyHandler;
import governance.plugin.handler.ServiceDependencyHandler;
import governance.plugin.util.Configurations;
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
 * Generates dependency tree for OSGi services by reading bundle meta data xml file
 */
@Mojo( name = "osgiservice", defaultPhase = LifecyclePhase.DEPLOY,  aggregator = true)
public class OSGiServiceDependencyMojo extends AbstractMojo
{
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

    public OSGiServiceDependencyMojo() throws MojoExecutionException{

    }

    public void execute() throws MojoExecutionException
    {
        configurations = new Configurations(project, settings, repositoryLocation, gregServiceUrl, gregUsername, gregPassword, gregHome, buildProfile);

        configure();

        getLog().info("Reading POM tree from:" +  configurations.getRepoLocation());
        List<MavenProject> pomTree = MavenProjectScanner.getPOMTree(configurations.getRepoLocation(), configurations.getBuildProfileId());

        OSGiServiceDependencyHandler osgiserviceDependencyHandler = new OSGiServiceDependencyHandler(configurations, getLog());
        osgiserviceDependencyHandler.process(pomTree);
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
