package cz.inovatika.altoEditor.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigResolveOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.utils.Utils.getDefaultHome;


/**
 * Wrapper for Typesafe config library. For the configuration syntax see https://github.com/lightbend/config
 * <p>
 * Usage:  Configurator.get().getConfig().getString("blabla");
 * <p>
 * Application specific properties can be defined in the file "application.conf" in root level of application resources
 * and overriden in the external file defined in the property "application.conf" - by default it is "${user.home}/application.conf"
 * Configurator instance is lazy loaded upon first call of Configurator.get() and instantiates a WatchService monitoring the changes in the external configuration file
 * This WatchService should be properly removed by calling Configurator.get().shutdown() at application shutdown
 * <p>
 * <p>
 * to use custom config -Dapplication.conf=path/to/file
 */
public class Configurator {

    public static String HOME = "application.home";
    public static String CONFIG = "cz/inovatika/altoEditor/server/application.conf";
    public static File ADDITIONAL_CONFIG = null;

    private static final Logger LOG = LoggerFactory.getLogger(Configurator.class.getName());


    private static class LazyHolder {
        private static Configurator instance = instance();
    }

    public static Configurator get() {
        return LazyHolder.instance;
    }

    public static String getString(String s) {
        String value = LazyHolder.instance.getConfig().getString(s);
        try{
            if(value.startsWith("encoded:")){
               return value.substring(8);
            }
        }catch (Exception e){
            LOG.error("Try to decrypt value with encoded_ prefix, but it didnt work reason: {}", e.toString());
        }
        return LazyHolder.instance.getConfig().getString(s);
    }

    public static String getString(String s, String def) {
        try {
            return LazyHolder.instance.getConfig().getString(s);
        } catch (Exception e) {
            LOG.info("Using default value, reason {}", e.toString());
            return def;
        }

    }

    public static int getInt(String s, int def) {
        try {
            return LazyHolder.instance.getConfig().getInt(s);
        } catch (Exception e) {
            LOG.info("Using default value, reason {}", e.toString());
            return def;
        }
    }

    public static boolean getBoolean(String s, boolean def) {
        try {
            return LazyHolder.instance.getConfig().getBoolean(s);
        } catch (Exception e) {
            LOG.info("Using default value, reason {}", e.toString());
            return def;
        }
    }

    public static boolean hasPath(String s) {
        return LazyHolder.instance.getConfig().hasPath(s);
    }

    /**
     * Set custom config file - added as last one to configuration
     * this file is not watched for changes
     *
     * @param configFile
     */
    public static Configurator setAditionalConfigFile(File configFile) {
        ADDITIONAL_CONFIG = configFile;
        return LazyHolder.instance;
    }

    private static Configurator instance() {
        Configurator c = new Configurator();
        c.resolveConfig();
        c.watch();
        return c;
    }

    private void resolveConfig() {
        Config defaultConf = ConfigFactory.parseResourcesAnySyntax("application");
        Config overridesConf = ConfigFactory.defaultOverrides();
        Config referenceConf = ConfigFactory.parseResourcesAnySyntax("reference");
//        Config tempConf = overridesConf.withFallback(appConf).withFallback(referenceConf).resolve(ConfigResolveOptions.defaults().setAllowUnresolved(true));
//        Config userConf = ConfigFactory.parseFileAnySyntax(new File(tempConf.getString(CONFIG)));
        Config unresolvedConf = overridesConf.withFallback(defaultConf).withFallback(referenceConf);
        if (ADDITIONAL_CONFIG != null) {
            if (ADDITIONAL_CONFIG.exists()) {
                Config additionalConf = ConfigFactory.parseFileAnySyntax(ADDITIONAL_CONFIG);
                unresolvedConf = additionalConf.withFallback(unresolvedConf);
            }else {
                LOG.warn("Aditional config file {} doesnt exist!!", ADDITIONAL_CONFIG.getAbsoluteFile());
            }
        }
        config = unresolvedConf.resolve(ConfigResolveOptions.defaults().setAllowUnresolved(true));
    }

    private Config config;

    public Config getConfig() {
        return config;
    }

    private ExecutorService es = Executors.newSingleThreadExecutor();

    public void shutdown() {
        es.shutdownNow();
    }


    private void watch() {
        es.submit(() -> {
            final Path path;
            try {
                path = Path.of(getDefaultHome().getPath());
            } catch (Exception e) {
                LOG.warn("Config watcher service not started - {} ", e);
                return;
            }
            try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                LOG.info("Started watching user properties file:" + config.getString(CONFIG));
                while (true) {
                    final WatchKey wk = watchService.take();
                    for (WatchEvent<?> event : wk.pollEvents()) {
                        //we only register "ENTRY_MODIFY" so the context is always a Path.
                        final Path changed = (Path) event.context();
                        if (config.getString(CONFIG).endsWith(changed.toString())) {
                            LOG.info("Reloading user properties:" + config.getString(CONFIG));
                            resolveConfig();
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if (!valid) {
                        LOG.info("Stopped watching user properties file:" + config.getString(CONFIG));
                    }
                }
            } catch (IOException e) {
                LOG.error("Error in config watcher service {}", e.getMessage());
            } catch (InterruptedException e) {
                LOG.info("Config watcher service stopped");
            }
        });
    }

}
