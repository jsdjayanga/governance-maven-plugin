package governance.plugin.handler;

import governance.plugin.rxt.GRegDependencyHandler;
import governance.plugin.rxt.module.ModuleCreator;
import governance.plugin.rxt.osgiservice.BundleXMLParser;
import governance.plugin.rxt.osgiservice.OSGiServiceCreator;
import governance.plugin.rxt.service.ServicesXMLParser;
import governance.plugin.util.Configurations;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

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
                //File rootFile = project.getFile().getParentFile();
                //scanDirectory(project, rootFile);

                System.out.println("Bundle found: " + project.getName());

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

        File rootDirectory = project.getFile().getParentFile();
        File bundleFile = getBungleFile(rootDirectory);
        if (bundleFile != null){
            JarFile jarFile = new JarFile(bundleFile);

            System.out.println("Processing Jar file=" + jarFile.getName());

            Enumeration e = jarFile.entries();
            while (e.hasMoreElements()){
                JarEntry jarEntry = (JarEntry)e.nextElement();

                if (!jarEntry.isDirectory() && jarEntry.getName().contains("OSGI-INF") && jarEntry.getName().endsWith(".xml")){

                    System.out.println("======XML Files=======++:" + jarEntry.getName());

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

                        System.out.println("Creating osgi services ");

                        osgiServiceInfo.put("version", project.getVersion());
                        osgiServiceCreator.create(osgiServiceInfo);

                        // TODO - create associations.
                    }

                    /*
                    BufferedReader br = null;
                    StringBuilder sb = new StringBuilder();

                    String line;

                    br = new BufferedReader(new InputStreamReader(inputStream));
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    */

                    //System.out.println("========file===========+:" + sb.toString());
                }
            }
            // Get descriptor FILES List and process one by one.
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


    /*
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

                osgiServiceCreator.create(serviceInfo);
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

                osgiServiceCreator.create(serviceInfo);
                createAssociations(serviceInfo, project, file);
            }
        }
    }

    public void createAssociations(Map<String, String> parameters, MavenProject project, File currentPOM) throws MojoExecutionException {

        String moduleAbsolutPath = moduleCreator.
                getAbsoluteResourcePath(new String[]{project.getArtifactId(), project.getVersion()});

        String dependencyReosurcePath = osgiServiceCreator.
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

    */
}
