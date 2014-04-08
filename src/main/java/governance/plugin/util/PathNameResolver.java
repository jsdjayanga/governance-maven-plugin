package governance.plugin.util;

/**
 * Created by jayanga on 2/10/14.
 */
public class PathNameResolver {

    /**
     * This method converts package name to a namespace
     * @param packageName Name of the package
     * @return Namespace
     */
    public static String PackageToNamespace(String packageName){
        String[] split = packageName.split("[.]");
        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        for (int i = split.length - 1; i >= 0; i--) {
            sb.append(split[i]);
            if (i > 0){
                sb.append(".");
            }
        }
        return sb.toString();
    }

    public static String getResourcePath(String name, String namespace, String resourcePath){
        name = name.trim();
        namespace = namespace.trim();

        String[] path = namespace.substring("http://".length()).split("[.]");
        StringBuilder sb = new StringBuilder();
        for (int i = path.length - 1; i >= 0 ; i--){
            if (path[i].trim().length() > 0) sb.append(path[i].trim() + "/");
        }

        return resourcePath  + sb.toString() + name;
    }

    public static String reverseNamespace(String namespace){
        String[] split = namespace.split("[.]");
        StringBuilder sb = new StringBuilder();
        for (int i = split.length - 1; i >= 0; i--) {
            sb.append(split[i]);
            if (i > 0){
                sb.append(".");
            }
        }
        return sb.toString();
    }
}
