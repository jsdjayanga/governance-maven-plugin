package governance.plugin.handler;

import com.google.inject.internal.util.$SourceProvider;
import governance.plugin.rxt.GRegDependencyHandler;
import governance.plugin.rxt.module.ModuleCreator;
import governance.plugin.rxt.osgi.BundleXMLParser;
import governance.plugin.rxt.osgi.OSGiServiceComponentCreator;
import governance.plugin.rxt.osgi.OSGiServiceCreator;
import governance.plugin.util.Configurations;
import governance.plugin.util.PathNameResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by jayanga on 3/4/14.
 */
public class OSGiDependencyHandler {

    private Configurations configurations;
    private Log logger;

    private int bundleFileCount = 0;
    private int bundleXMLFileCount = 0;


    private ModuleCreator moduleCreator;
    private OSGiServiceComponentCreator osgiServiceComponentCreator;
    private OSGiServiceCreator osgiServiceCreator;
    private GRegDependencyHandler gregDependencyHandler;

    public OSGiDependencyHandler(Configurations configurations, Log logger) throws MojoExecutionException {
        this.configurations = configurations;
        this.logger = logger;

        gregDependencyHandler = new GRegDependencyHandler(logger, configurations.getGergServiceUrl());
        moduleCreator = new ModuleCreator(logger, configurations.getGergServiceUrl());
        osgiServiceComponentCreator = new OSGiServiceComponentCreator(logger, configurations.getGergServiceUrl());
        osgiServiceCreator = new OSGiServiceCreator(logger, configurations.getGergServiceUrl());
    }

    public void process(List<MavenProject> projectTree) throws MojoExecutionException {

        for (MavenProject project : projectTree){
            if (project.getPackaging().equalsIgnoreCase("bundle")){
                bundleFileCount++;
                try {
                    processBundle(project);
                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }

        logger.info("SUMMARY:"
                + "\nBundles Processed.................." + bundleFileCount
                + "\nBundle  xml files Processed........" + bundleXMLFileCount
                + "\nModules ...........................[Created:" + moduleCreator.getCreatedAssetCount()
                + ", Existing:" + moduleCreator.getExistingAssetCount() + "]"
                + "\nOSGiServiceComponents..............[Created:" + osgiServiceComponentCreator.getCreatedAssetCount()
                + ", Existing:" + osgiServiceComponentCreator.getExistingAssetCount() + "]"
                + "\nOSGiServices.......................[Created:" + osgiServiceCreator.getCreatedAssetCount()
                + ", Existing:" + osgiServiceCreator.getExistingAssetCount() + "]"
                + "\nAssociations.......................[Added:" + gregDependencyHandler.getAddedAssocationCount()
                + ", Deleted:" + gregDependencyHandler.getRemovedAssocationCount() + "]");
    }

    private void processBundle(MavenProject project) throws MojoExecutionException, IOException {
        List<String> list = new ArrayList<String>();

        File file = project.getFile();
        File rootDirectory = file.getParentFile();
        File bundleFile = getBungleFile(rootDirectory);
        if (bundleFile != null){
            JarFile jarFile = new JarFile(bundleFile);

            logger.info("Processing Jar file. [JarFile=" + jarFile.getName() + "]");

            Enumeration e = jarFile.entries();
            while (e.hasMoreElements()){
                JarEntry jarEntry = (JarEntry)e.nextElement();

                if (!jarEntry.isDirectory() && jarEntry.getName().contains("OSGI-INF") && jarEntry.getName().endsWith(".xml")){

                    bundleXMLFileCount++;

                    logger.info("Reading Service-Component xml. [File=" + jarEntry.getName() + "]");

                    InputStream inputStream = jarFile.getInputStream(jarEntry);

                    File tempFile = File.createTempFile("temp", ".tmp");
                    tempFile.deleteOnExit();

                    FileOutputStream outputStream = new FileOutputStream(tempFile);
                    IOUtils.copy(inputStream, outputStream);

                    List<Object> osgiServiceComponentInfoList = null;
                    try {
                        osgiServiceComponentInfoList = BundleXMLParser.parse(tempFile);
                    } catch (SAXException e1) {
                        e1.printStackTrace();
                    } catch (ParserConfigurationException e1) {
                        e1.printStackTrace();
                    }

                    for (int i = 0; i < osgiServiceComponentInfoList.size(); i++){
                        Map<String, Object> osgiServiceComponentInfo = (Map<String, Object>)osgiServiceComponentInfoList.get(i);

                        String className = (String)osgiServiceComponentInfo.get("className");
                        osgiServiceComponentInfo.put("version", project.getVersion());

                        logger.info("Creating OSGi service. [OSGiService=" + className + "]");

                        createDependentOSGiServices(osgiServiceComponentInfo);

                        osgiServiceComponentCreator.create(osgiServiceComponentInfo);
                        createAssociationsWithModule(osgiServiceComponentInfo, project, file);

                        markAssociationsWithOSGiServicesMap(osgiServiceComponentInfo);
                    }

                }
            }
        }
    }

    private void createDependentOSGiServices(Map<String, Object> osgiServiceComponentInfo) throws MojoExecutionException {
        List<Map<String, String>> references = (List<Map<String, String>>)osgiServiceComponentInfo.get("references");
        if (references != null){
            
            String version = (String)osgiServiceComponentInfo.get("version");
            
            for (int index = 0; index < references.size(); index++){
                Map<String, String> refMap = (Map<String, String>)references.get(index);
                if (refMap != null){
                    String refName = refMap.get("name");
                    String refInterface = refMap.get("interface");
                    
                    Map<String, String> serviceInfo = new HashMap<String, String>();
                    serviceInfo.put("name", refName);
                    serviceInfo.put("interface", refInterface);
                    serviceInfo.put("version", version);
                    String description = "generated by maven-governance-plugin.";
                    
                    osgiServiceCreator.create(serviceInfo);
                }
            }
        }
    }

    private File getBungleFile(File rootDirectory){
        File targetDirectory = new File(rootDirectory.getAbsolutePath() + File.separator + "target");

        File[] children = targetDirectory.listFiles();
        if (children != null){
            for (File child : children){
                if (child.getName().endsWith(".jar")){
                    return child;
                }
            }
        }

        return null;
    }

    public void createAssociationsWithModule(Map<String, Object> parameters, MavenProject project, File currentPOM) throws MojoExecutionException {

        String moduleAbsolutPath = moduleCreator.
                getAbsoluteResourcePath(new String[]{project.getArtifactId(), project.getVersion()});

        String className = (String)parameters.get("className");
        String namespace = PathNameResolver.reverseNamespace(className.substring(0, className.lastIndexOf(".")));

        String dependencyReosurcePath = osgiServiceComponentCreator.
                getAbsoluteResourcePath(new String[]{className.substring(className.lastIndexOf(".") + 1), namespace});

        if (!moduleCreator.isModuleExisting(project.getArtifactId(), project.getVersion())){
            moduleCreator.create(new String[]{project.getArtifactId(), project.getVersion(), currentPOM.getAbsolutePath()});
        }

        // Adding the dependency
        gregDependencyHandler.addAssociation(moduleAbsolutPath, dependencyReosurcePath,
                GRegDependencyHandler.GREG_ASSOCIATION_TYPE_CONTAINS);

        // Adding the invert association(i.e.dependency is usedBy source)
        gregDependencyHandler.addAssociation(dependencyReosurcePath, moduleAbsolutPath,
                GRegDependencyHandler.GREG_ASSOCIATION_TYPE_DEPENDS);
    }

    private void markAssociationsWithOSGiServicesMap(Map<String, Object> parameters) throws MojoExecutionException {
        String className = (String)parameters.get("className");
        String namespace = PathNameResolver.reverseNamespace(className.substring(0, className.lastIndexOf(".")));

        String reosurcePath = osgiServiceComponentCreator.
                getAbsoluteResourcePath(new String[]{className.substring(className.lastIndexOf(".") + 1), namespace});

        String version = (String)parameters.get("version");

        List<Map<String, String>> references = (List<Map<String, String>>)parameters.get("references");
        if (references != null){
            for (int index = 0; index < references.size(); index++){
                Map<String, String> refMap = (Map<String, String>)references.get(index);
                if (refMap != null){

                    String refName = refMap.get("name");

                    String dependencyReosurcePath = osgiServiceCreator.
                            getAbsoluteResourcePath(new String[]{refName, version});

                    // Adding the dependency
                    gregDependencyHandler.addAssociation(reosurcePath, dependencyReosurcePath,
                            GRegDependencyHandler.GREG_ASSOCIATION_TYPE_IMPORTS);

                    // Adding the invert association(i.e.dependency is usedBy source)
                    gregDependencyHandler.addAssociation(dependencyReosurcePath, reosurcePath,
                            GRegDependencyHandler.GREG_ASSOCIATION_TYPE_USEDBY);
                }
            }
        }
    }
}
