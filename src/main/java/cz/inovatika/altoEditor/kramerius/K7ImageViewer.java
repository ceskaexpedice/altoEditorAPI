package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.utils.Config;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;

public class K7ImageViewer {

    private static final Logger LOG = LoggerFactory.getLogger(K7ImageViewer.class.getName());

    public HttpResponse getResponse(String pid, String instanceId) throws IOException, AltoEditorException {

        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId);
        if (instance == null) {
            throw new DigitalObjectNotFoundException(instanceId, String.format("This instance \"%s\" is not configured.", instanceId));
        }

        K7Authenticator authenticator = new K7Authenticator(instance);
        String token = authenticator.authenticate();

        if (token == null || token.isEmpty()) {
            LOG.error("Kramerius token is null");
            throw new AltoEditorException(instanceId, "Kramerius token is null");
        }

        String imageUrl = Config.getKrameriusInstanceUrl(instanceId) +
                Config.getKrameriusInstanceUrlImage(instanceId) +
                pid + "/full/max/0/default.jpg";
        LOG.info("Trying to download image from " + imageUrl);

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(imageUrl);
        httpGet.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        if (token != null && !token.isEmpty()) {
            httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }
        httpGet.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        HttpResponse response = httpClient.execute(httpGet);
        return response;
    }
}
