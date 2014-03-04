package governance.plugin.handler;

import governance.plugin.rxt.GRegDependencyHandler;
import governance.plugin.rxt.module.ModuleCreator;
import governance.plugin.rxt.service.ServiceCreator;
import governance.plugin.rxt.service.ServiceJavaFileParser;
import governance.plugin.rxt.service.ServicesXMLParser;
import governance.plugin.util.Configurations;
import governance.plugin.util.EffectivePom;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by jayanga on 3/4/14.
 */
public class ServiceHandler {

    private Configurations configurations;
    private Log logger;

    private int pomFileCount = 0;
    private int directoryCount = 0;
    private int servicesXMLCount = 0;
    private int javaFileCount = 0;

    private ModuleCreator moduleCreator;
    private ServiceCreator serviceCreator;
    private GRegDependencyHandler gregDependencyHandler;

    public ServiceHandler(Configurations configurations, Log logger) throws MojoExecutionException {
        this.configurations = configurations;
        this.logger = logger;

        gregDependencyHandler = new GRegDependencyHandler(logger, configurations.getGergServiceUrl());
        moduleCreator = new ModuleCreator(logger, configurations.getGergServiceUrl());
        serviceCreator = new ServiceCreator(logger, configurations.getGergServiceUrl());
    }

    public void process(List<MavenProject> projectTree) throws MojoExecutionException {

        for (MavenProject project : projectTree){
            if (!project.getPackaging().equalsIgnoreCase("pom")){
                File rootFile = project.getFile().getParentFile();
                scanDirectory(project, rootFile);
            }
        }

        logger.info("SUMMARY:"
                + "\nDirectories Scanned..............." + directoryCount
                + "\npom.xml Files Processed..........." + pomFileCount
                + "\nservices.xml Files Processed......" + servicesXMLCount
                + "\njava Files Processed.............." + javaFileCount
                + "\nModules ........[Created:" + moduleCreator.getCreatedAssetCount()
                + ", Existing:" + moduleCreator.getExistingAssetCount() + "]"
                + "\nServices........[Created:" + serviceCreator.getCreatedAssetCount()
                + ", Existing:" + serviceCreator.getExistingAssetCount() + "]"
                + "\nAssociations....[Added:" + gregDependencyHandler.getAddedAssocationCount()
                + ", Deleted:" + gregDependencyHandler.getRemovedAssocationCount() + "]");
    }

    private void scanDirectory(MavenProject project, File file) throws MojoExecutionException{
        if (file != null){
            if (file.isDirectory()){
                directoryCount++;

                File[] children = file.listFiles();
                if (children == null){
                    logger.debug("Empty directory skipping.. :" + file.getAbsolutePath());
                }else{
                    for (File child : children){
                        scanDirectory(project, child);
                    }
                }
            }else{
                process(project, file);
            }
        }
    }

    public void process(MavenProject project, File file) throws MojoExecutionException{
        logger.debug("Processing " + file.getAbsoluteFile());

        if (file.getName().equals("services.xml")){
            servicesXMLCount++;

            if (project.getVersion().contains("$")){
                EffectivePom effectivePom = new EffectivePom(project.getFile());
                project = effectivePom.fillChildProject(project);
            }

            List<Object> serviceInfoList = null;
            try {
                serviceInfoList = ServicesXMLParser.parse(file);
            } catch (SAXException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } catch (ParserConfigurationException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }

            for (int i = 0; i < serviceInfoList.size(); i++){
                Map<String, String> serviceInfo = (Map<String, String>)serviceInfoList.get(i);
                serviceInfo.put("version", project.getVersion());

                serviceCreator.create(serviceInfo);
                createAssociations(serviceInfo, project, file);
            }

        }else if (file.getName().endsWith(".java")){
            javaFileCount++;

            if (project.getVersion().contains("$")){
                EffectivePom effectivePom = new EffectivePom(project.getFile());
                project = effectivePom.fillChildProject(project);
            }

            List<Object> serviceInfoList = null;
            try {
                serviceInfoList = ServiceJavaFileParser.parse(file);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }

            for (int i = 0; i < serviceInfoList.size(); i++){
                Map<String, String> serviceInfo = (Map<String, String>)serviceInfoList.get(i);
                serviceInfo.put("version", project.getVersion());

                serviceCreator.create(serviceInfo);
                createAssociations(serviceInfo, project, file);
            }
        }
    }

    public void createAssociations(Map<String, String> parameters, MavenProject project, File currentPOM) throws MojoExecutionException {

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
