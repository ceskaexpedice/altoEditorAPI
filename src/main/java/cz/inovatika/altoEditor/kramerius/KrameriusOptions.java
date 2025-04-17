package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.utils.Config;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KrameriusOptions {

    private static final Logger LOGGER = LogManager.getLogger(KrameriusOptions.class.getName());

    private List<KrameriusInstance> krameriusInstances;

    public static KrameriusOptions get() {
        KrameriusOptions options = new KrameriusOptions();

        List<KrameriusInstance> krameriusInstances = new ArrayList<>();
        List<String> krameriusIds = Config.getKrameriusInstances();
        for (String krameriusId : krameriusIds) {
            KrameriusInstance krameriusConf = new KrameriusInstance(krameriusId.trim());
            if (valid(krameriusConf)) {
                krameriusInstances.add(krameriusConf);
            }
        }

        options.setKrameriusInstances(krameriusInstances);
        return options;

    }

    public List<KrameriusInstance> getKrameriusInstances() {
        return krameriusInstances;
    }

    public void setKrameriusInstances(List<KrameriusInstance> krameriusInstances) {
        this.krameriusInstances = krameriusInstances;
    }

    public static KrameriusInstance findKrameriusInstance(List<KrameriusInstance> listOfInstances, String id) {
        if (id != null) {
            for (KrameriusInstance krameriusInstance : listOfInstances) {
                if (id.equals(krameriusInstance.getId())) {
                    return krameriusInstance;
                }
            }
        }
        return null;
    }

    public static File getExportFolder(String instanceId, URI exportUri, String type) {
        if (instanceId == null || instanceId.isEmpty()) {
            return new File(exportUri);
        } else {
            File exportFile = new File(Config.getKrameriusInstanceExportFoxmlFolder(instanceId));
            if (!exportFile.exists() || !exportFile.isDirectory() || !exportFile.canRead() || !exportFile.canWrite()) {
                throw new IllegalArgumentException("Error s nakonfigurovanou cestou: " + Config.getKrameriusInstanceExportFoxmlFolder(instanceId) + " (zkontrolujte, ze cesta existuje a mate do ni prava na cteni a zapis.");
            } else {
                return exportFile;
            }
        }
    }

    private static boolean valid(KrameriusInstance instance) {
        Config.getKrameriusInstanceTitle(instance.getId());
        Config.getKrameriusInstanceVersion(instance.getId());
        Config.getKrameriusInstanceType(instance.getId());
        Config.getKrameriusInstanceUrl(instance.getId());
        Config.getKrameriusInstanceUrlParametrizedImportQuery(instance.getId());
        Config.getKrameriusInstanceUrlStateQuery(instance.getId());
//        Config.getKrameriusInstancePassword(instance.getId());
//        Config.getKrameriusInstanceUsername(instance.getId());
//        Config.getKrameriusInstanceExportFoxmlFolder(instance.getId());
//        Config.getKrameriusInstanceKrameriusImportFoxmlFolder(instance.getId());
        Config.getKrameriusInstanceUrlLogin(instance.getId());
        Config.getKrameriusInstanceUrlDownloadFoxml(instance.getId());
        Config.getKrameriusInstanceUrlUploadStream(instance.getId());
        Config.getKrameriusInstanceUrlImage(instance.getId());
//        Config.getKrameriusInstanceClientId(instance.getId());
//        Config.getKrameriusInstanceClientSecret(instance.getId());
//        Config.getKrameriusInstanceGrantType(instance.getId());
        return true;
    }

    public static class KrameriusInstance {

        private String instanceId;

        public KrameriusInstance(String instanceId) {
            this.instanceId = instanceId;
        }

        public String getId() {
            return instanceId;
        }
    }
}
