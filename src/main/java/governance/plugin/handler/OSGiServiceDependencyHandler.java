package governance.plugin.handler;

import governance.plugin.rxt.GRegDependencyHandler;
import governance.plugin.rxt.module.ModuleCreator;
import governance.plugin.rxt.osgiservice.BundleXMLParser;
import governance.plugin.rxt.osgiservice.OSGiServiceCreator;
import governance.plugin.rxt.service.ServicesXMLParser;
import governance.plugin.util.Configurations;
import governance.plugin.util.PathNameResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import javax.lang.model.element.NestingKind;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by jayanga on 3/4/14.
 */
public class OSGiServiceDependencyHandler {

    private Configurations configurations;
    private Log logger;

    private int pomFileCount = 0;
    private int directoryCount = 0;
    private int servicesXMLCount = 0;
    private int javaFileCount = 0;

    private ModuleCreator moduleCreator;
    private OSGiServiceCreator osgiServiceCreator;
    private GRegDependencyHandler gregDependencyHandler;

    public OSGiServiceDependencyHandler(Configurations configurations, Log logger) throws MojoExecutionException {
        this.configurations = configurations;
        this.logger = logger;

        gregDependencyHandler = new GRegDependencyHandler(logger, configurations.getGergServiceUrl());
        moduleCreator = new ModuleCreator(logger, configurations.getGergServiceUrl());
        osgiServiceCreator = new OSGiServiceCreator(logger, configurations.getGergServiceUrl());
    }

    public void process(List<MavenProject> projectTree) throws MojoExecutionException {

        for (MavenProject project : projectTree){
            if (project.getPackaging().equalsIgnoreCase("bundle")){

                try {
                    processBundle(project);
                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }

        logger.info("SUMMARY:"
                + "\nDirectories Scanned..............." + directoryCount
                + "\npom.xml Files Processed..........." + pomFileCount
                + "\nservices.xml Files Processed......" + servicesXMLCount
                + "\njava Files Processed.............." + javaFileCount
                + "\nModules ........[Created:" + moduleCreator.getCreatedAssetCount()
                + ", Existing:" + moduleCreator.getExistingAssetCount() + "]"
                + "\nOSGiServices........[Created:" + osgiServiceCreator.getCreatedAssetCount()
                + ", Existing:" + osgiServiceCreator.getExistingAssetCount() + "]"
                + "\nAssociations....[Added:" + gregDependencyHandler.getAddedAssocationCount()
                + ", Deleted:" + gregDependencyHandler.getRemovedAssocationCount() + "]");
    }

    private void processBundle(MavenProject project) throws MojoExecutionException, IOException {
        List<String> list = new ArrayList<String>();

        File file = project.getFile();
        File rootDirectory = file.getParentFile();
        File bundleFile = getBungleFile(rootDirectory);
        if (bundleFile != null){
            JarFile jarFile = new JarFile(bundleFile);

            System.out.println("Processing Jar file. [JarFile=" + jarFile.getName() + "]");

            Enumeration e = jarFile.entries();
            while (e.hasMoreElements()){
                JarEntry jarEntry = (JarEntry)e.nextElement();

                if (!jarEntry.isDirectory() && jarEntry.getName().contains("OSGI-INF") && jarEntry.getName().endsWith(".xml")){

                    System.out.println("Reading Service-Component xml. [File=" + jarEntry.getName() + "]");

                    InputStream inputStream = jarFile.getInputStream(jarEntry);

                    File tempFile = File.createTempFile("temp", ".tmp");
                    tempFile.deleteOnExit();

                    FileOutputStream outputStream = new FileOutputStream(tempFile);
                    IOUtils.copy(inputStream, outputStream);

                    List<Object> osgiServiceInfoList = null;
                    try {
                        osgiServiceInfoList = BundleXMLParser.parse(tempFile);
                    } catch (SAXException e1) {
                        e1.printStackTrace();
                    } catch (ParserConfigurationException e1) {
                        e1.printStackTrace();
                    }

                    for (int i = 0; i < osgiServiceInfoList.size(); i++){
                        Map<String, Object> osgiServiceInfo = (Map<String, Object>)osgiServiceInfoList.get(i);

                        String className = (String)osgiServiceInfo.get("className");
                        System.out.println("Creating OSGi service. [OSGiService=" + className + "]");

                        osgiServiceInfo.put("version", project.getVersion());
                        osgiServiceCreator.create(osgiServiceInfo);

                        createAssociations(osgiServiceInfo, project, file);
                    }

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

    public void createAssociations(Map<String, Object> parameters, MavenProject project, File currentPOM) throws MojoExecutionException {

        String moduleAbsolutPath = moduleCreator.
                getAbsoluteResourcePath(new String[]{project.getArtifactId(), project.getVersion()});

        String className = (String)parameters.get("className");
        String namespace = PathNameResolver.PackageToNamespace(className.substring(0, className.lastIndexOf(".")));

        String dependencyReosurcePath = osgiServiceCreator.
                getAbsoluteResourcePath(new String[]{className.substring(className.lastIndexOf(".") + 1), namespace});

        System.out.println("==========M:" + moduleAbsolutPath);
        System.out.println("==========R:" + dependencyReosurcePath);

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
