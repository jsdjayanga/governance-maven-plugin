package governance.plugin.util;

/**
 * Created by jayanga on 2/10/14.
 */
public class PackageToNamespace {
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
}
