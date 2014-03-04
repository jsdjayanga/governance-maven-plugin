package governance.plugin.util;

import governance.plugin.common.XmlParser;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jayanga on 3/4/14.
 */
public class MavenProjectScanner {

    private static List<MavenProject> projectList = new ArrayList<MavenProject>();

    public static List<MavenProject> getPOMTree(String rootPomPath, String buildProfileID) throws MojoExecutionException {
        projectList.clear();

        scanPOMTree(rootPomPath, buildProfileID);
        return projectList;
    }

    public static void scanPOMTree(String rootPomPath, String buildProfileID) throws MojoExecutionException {
        String filePath = rootPomPath.concat(File.separatorChar + "pom.xml");
        MavenProject project = createMavenProject(new File(filePath));
        projectList.add(project);

        List<String> modules = project.getModules();
        List<Profile> profiles = project.getModel().getProfiles();
        for (Profile profile : profiles){
            if (profile.getId().equals(buildProfileID)){
                //getLog().info("Adding modules of maven profile '"  + buildProfileID + "'");
                modules.addAll(profile.getModules());
            }
        }

        for (String module : modules){
            scanPOMTree(rootPomPath.concat(File.separatorChar + module.replace('/', File.separatorChar)), buildProfileID);
        }
    }

    public static MavenProject createMavenProject(File file) throws MojoExecutionException{
        MavenProject project = null;
        if (file.exists()){
            //getLog().debug("Processing " + file.getAbsoluteFile());

            Model model = XmlParser.parsePom(file);
            if (model == null){
                throw new MojoExecutionException("Error while processing  " + file.getAbsoluteFile());
            }
            project = new MavenProject(model);
            return project;
        }

        throw new MojoExecutionException("File does not exist.  " + file.getAbsoluteFile());
    }
}
