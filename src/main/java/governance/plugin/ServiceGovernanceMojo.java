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

import governance.plugin.rxt.ModuleCreator;
import governance.plugin.service.ServiceCreator;
import governance.plugin.service.ServiceJavaFileParser;
import governance.plugin.service.ServicesXMLParser;
import governance.plugin.util.DirectoryScanner;
import governance.plugin.util.POMFileCache;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Generates dependency tree by reading .java, services.xml, web.xml files
 */
@Mojo( name = "service", defaultPhase = LifecyclePhase.DEPLOY,  aggregator = true)
public class ServiceGovernanceMojo extends AbstractMojo
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

    //GReg resource paths


    private int pomFileCount = 0;
    private int directoryCount = 0;
    private int servicesXMLCount = 0;
    private int javaFileCount = 0;

    private ModuleCreator moduleCreator;
    private ServiceCreator serviceCreator;
    private GRegDependencyHandler gregDependencyHandler;
    private Configurations configurations;

    public ServiceGovernanceMojo() throws MojoExecutionException{

    }

    public void execute() throws MojoExecutionException
    {
        configurations = new Configurations(project, settings, repositoryLocation, gregServiceUrl, gregUsername, gregPassword, gregHome, buildProfile);

        gregDependencyHandler = new GRegDependencyHandler(getLog(), configurations.getGergServiceUrl());
        moduleCreator = new ModuleCreator(getLog(), configurations.getGergServiceUrl());
        serviceCreator = new ServiceCreator(getLog(), configurations.getGergServiceUrl());

        configure();

        getLog().info("Starting to scan with root:" +  configurations.getRepoLocation());
        //scanDirectory(configurations.getRepoLocation());
        scanPomTree(configurations.getRepoLocation());

        getLog().info("SUMMARY:"
                + "\nDirectories Scanned..............." + directoryCount
                + "\npom.xml Files Processed..........." + pomFileCount
                + "\nservices.xml Files Processed......" + servicesXMLCount
                + "\njava Files Processed.............." + javaFileCount
                + "\nModules ........[Created:" + moduleCreator.getCreatedAssetCount()
                + ", Existing:" + moduleCreator.getExistingAssetCount() + "]"
                + "\nServices........[Created:" + serviceCreator.getCreatedAssetCount()
                + ", Existing:" + serviceCreator.getCreatedAssetCount() + "]"
                + "\nAssocations.....[Added:" + gregDependencyHandler.getAddedAssocationCount()
                + ", Deleted:" + gregDependencyHandler.getRemovedAssocationCount() + "]");
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

    private void scanDirectory(File file) throws MojoExecutionException{
        if (file != null){
            if (file.isDirectory()){
                directoryCount++;

                File[] children = file.listFiles();
                if (children == null){
                    getLog().debug("Empty directory skipping.. :" + file.getAbsolutePath());
                }else{
                    for (File child : children){
                        scanDirectory(child);
                    }
                }
            }else{
                process(file);
            }
        }
    }

    private void scanPomTree(String path) throws MojoExecutionException{
        File rootFile = new File(path);
        if (rootFile != null){
            File pomFile = DirectoryScanner.findFile(rootFile, "pom.xml");
            if (pomFile != null){
                POMFileCache.put(pomFile.getParent(), pomFile);

                Model model = XmlParser.parsePom(pomFile);
                if (model == null){
                    throw new MojoExecutionException("Error while processing  " + pomFile.getAbsoluteFile());
                }

                MavenProject project = new MavenProject(model);
                if (project == null){
                    throw new MojoExecutionException("Cannot create a project from given POM file " + pomFile.getName());
                }

                if (!project.getPackaging().equalsIgnoreCase("pom")){
                    scanDirectory(rootFile);
                }

                List<String> modules = project.getModules();

                List<Profile> profiles = project.getModel().getProfiles();
                for (Profile profile : profiles){
                    if (profile.getId().equals(configurations.getBuildProfileId())){
                        modules.addAll(profile.getModules());
                        getLog().info("Adding modules of maven default with ID  '"  + configurations.getBuildProfileId() + "'");
                    }
                }

                for (String module : modules){
                    scanPomTree(path.concat(File.separatorChar + module.replace('/', File.separatorChar)));
                }
            }
        }
        else{
            process(new File(path));
        }
        getLog().debug("Finished scanning directory :" + path);
    }

    public void process(File file) throws MojoExecutionException{
        getLog().debug("Processing " + file.getAbsoluteFile());

        if (file.getName().equals("services.xml")){
            servicesXMLCount++;

            List<Object> serviceInfoList = null;
            try {
                serviceInfoList = ServicesXMLParser.parse(file);
            } catch (SAXException e) {
                e.printStackTrace();
                throw new MojoExecutionException(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                throw new MojoExecutionException(e.getMessage());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                throw new MojoExecutionException(e.getMessage());
            }

            for (int i = 0; i < serviceInfoList.size(); i++){
                serviceCreator.create((Map<String, String>)serviceInfoList.get(i));
                linkServiceWithModule((Map<String, String>)serviceInfoList.get(i), file);
            }

        }else if (file.getName().endsWith(".java")){
            javaFileCount++;

            List<Object> serviceInfoList = ServiceJavaFileParser.parse(file);
            //Object[] serviceInfoArray = serviceInfoList.toArray(new Object[serviceInfoList.size()]);

            for (int i = 0; i < serviceInfoList.size(); i++){
                serviceCreator.create((Map<String, String>)serviceInfoList.get(i));
                linkServiceWithModule((Map<String, String>)serviceInfoList.get(i), file);
            }
        }
    }

    public void linkServiceWithModule(Map<String, String> parameters, File file) throws MojoExecutionException {

        File currentPOM = POMFileCache.getNearestPOM(file);
        if (currentPOM == null){
            throw new MojoExecutionException("Cannot find a POM related to this module. [file=" + file.getAbsolutePath() + "]");
        }

        Model model = XmlParser.parsePom(currentPOM);
        if (model == null){
            throw new MojoExecutionException("Error while processing  " + currentPOM.getAbsoluteFile());
        }

        // Creating a module representing the artifact generated by pom file
        MavenProject project = new MavenProject(model);

        String moduleAbsolutPath = moduleCreator.
                getAbsoluteResourcePath(new String[]{project.getArtifactId(), project.getVersion()});

        String dependencyReosurcePath = serviceCreator.
                getAbsoluteResourcePath(new String[]{parameters.get("name"), parameters.get("namespace")});

        if (!moduleCreator.isModuleExisting(project.getArtifactId(), project.getVersion())){
            moduleCreator.create(new String[]{project.getArtifactId(), project.getVersion(), currentPOM.getAbsolutePath()});
        }

        // Adding the dependency
        gregDependencyHandler.addAssociation(moduleAbsolutPath, dependencyReosurcePath,
                GRegDependencyHandler.GREG_ASSOCIATION_TYPE_DEPENDS);

        // Adding the invert association(i.e.dependency is usedBy source)
        gregDependencyHandler.addAssociation(dependencyReosurcePath, moduleAbsolutPath,
                GRegDependencyHandler.GREG_ASSOCIATION_TYPE_USEDBY);
    }
}
