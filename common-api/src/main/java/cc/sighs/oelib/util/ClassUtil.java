package cc.sighs.oelib.util;

public class ClassUtil {
    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, ClassUtil.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
