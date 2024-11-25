package cz.inovatika.altoEditor.user;

import cz.inovatika.altoEditor.kramerius.K7UserInfo;
import cz.inovatika.altoEditor.utils.Config;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.FileUtils;
import cz.inovatika.utils.configuration.Configurator;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.javalin.http.Context;

public class UserUtils {

    private static final Logger LOGGER = LogManager.getLogger(UserUtils.class.getName());

    public static UserProfile getUserProfile(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        K7UserInfo k7UserInfo = new K7UserInfo();
        try {
            UserProfile user = k7UserInfo.getUser(token);
            return user;
        } catch (IOException e) {
            return null;
        }
    }

    public static String getToken(Context ctx) {
        String authHeader = ctx.header("Authorization");
        return authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
    }

    public static boolean isAltoEditorRolePath(String path, UserProfile user) {
        String requiredRoles = Config.getPermissionEditor();
        if (!user.getRoles().contains(requiredRoles)) {
            return false;
        } else {
            if (Const.EDITORS_PATH.contains(path)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdminRolePath(String path, UserProfile user) {
        String requiredRoles = Config.getPermissionCurator();
        if (!user.getRoles().contains(requiredRoles)) {
            return false;
        } else {
            if (Const.ADMINS_PATH.contains(path)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPublicPath(String path) {
        return Const.PUBLIC_PATH.contains(path);
    }
}
