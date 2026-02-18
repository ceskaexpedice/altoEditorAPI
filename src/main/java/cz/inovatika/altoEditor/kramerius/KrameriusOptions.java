package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.utils.Config;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KrameriusOptions {

    private static final Logger LOGGER = LogManager.getLogger(KrameriusOptions.class.getName());

    public static final String KRAMERIUS_PROCESS_FINISHED = "FINISHED";
    public static final String KRAMERIUS_PROCESS_WARNING = "WARNING";
    public static final String KRAMERIUS_PROCESS_FAILED = "FAILED";
    public static final String KRAMERIUS_PROCESS_ERROR = "ERROR";
    public static final String KRAMERIUS_PROCESS_PLANNED = "PLANNED";
    public static final String KRAMERIUS_PROCESS_RUNNING = "RUNNING";


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

    private static boolean valid(KrameriusInstance instance) {
        Config.getKrameriusInstanceTitle(instance.getId());
        Config.getKrameriusInstanceVersion(instance.getId());
        Config.getKrameriusInstanceType(instance.getId());
        Config.getKrameriusInstanceUrl(instance.getId());
        Config.getKrameriusInstanceUrlParametrizedImportQuery(instance.getId());
        Config.getKrameriusInstanceUrlStateQuery(instance.getId());
        Config.getKrameriusInstanceUrlDownloadFoxml(instance.getId());
        Config.getKrameriusInstanceUrlUploadStream(instance.getId());
        Config.getKrameriusInstanceUrlImage(instance.getId());
        Config.getKrameriusInstanceUrlModelInfo(instance.getId());
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
