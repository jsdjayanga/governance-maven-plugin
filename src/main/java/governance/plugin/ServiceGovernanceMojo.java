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
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.File;
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

    private Map<String, File> pomMap;

    public ServiceGovernanceMojo() throws MojoExecutionException{
        pomMap = new HashMap<String, File>();
    }

    public void execute() throws MojoExecutionException
    {
        configurations = new Configurations(project, settings, repositoryLocation, gregServiceUrl, gregUsername, gregPassword, gregHome, buildProfile);

        gregDependencyHandler = new GRegDependencyHandler(getLog(), configurations.getGergServiceUrl());
        moduleCreator = new ModuleCreator(getLog(), configurations.getGergServiceUrl());
        serviceCreator = new ServiceCreator(getLog(), configurations.getGergServiceUrl());

        configure();

        getLog().info("Starting to scan with root:" +  configurations.getRepoLocation());
        scanDirectory(configurations.getRepoLocation());

        getLog().info("SUMMARY:"
                + "\nDirectories Scanned..............." + directoryCount
                + "\npom.xml Files Processed..........." + pomFileCount
                + "\nservices.xml Files Processed......" + servicesXMLCount
                + "\njava Files Processed.............." + javaFileCount
                + "\nModules ........[Created:" + moduleCreator.getCreatedAssetCount()
                + ", Existing:" + moduleCreator.getExistingAssetCount() + "]"
                + "\nServices...........[Created:" + serviceCreator.getCreatedAssetCount()
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

    private void scanDirectory(String path) throws MojoExecutionException{
        File root = new File(path);
        if (root.isDirectory()){
            directoryCount++;
            File[] children = root.listFiles();
            if (children == null){
                getLog().debug("Empty directory skipping.. :" + path);
            }else{
                File pomFile = findPOMFileInCurrentDirectory(children);
                if (pomFile != null){
                    pomMap.put(pomFile.getParent(), pomFile);
                }

                for (File child : children){
                    scanDirectory(child.getAbsolutePath());
                }
            }
        }
        else{
            process(new File(path));
        }
        getLog().debug("Finished scanning directory :" + path);
    }

    private File findNearestPOMFile(File file){
        while (true){

            File pomFile = pomMap.get(file.getParent());
            if (pomFile != null){
                return pomFile;
            }
            file = file.getParentFile();
        }
    }

    private File findPOMFileInCurrentDirectory(File[] files){
        File file = null;

        for (int index = 0; index < files.length; index++){
            file = files[index];
            if (file != null && file.isFile()){
                if (file.getName().equals("pom.xml")){
                    return file;
                }
            }
        }
        return null;
    }

    public void process(File file) throws MojoExecutionException{
        getLog().debug("Processing " + file.getAbsoluteFile());

        if (file.getName().equals("services.xml")){
            servicesXMLCount++;

            List<Object> serviceInfoList = ServicesXMLParser.parse(file);
            //Object[] serviceInfoArray = serviceInfoList.toArray(new Object[serviceInfoList.size()]);

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

        File currentPOM = findNearestPOMFile(file);
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
