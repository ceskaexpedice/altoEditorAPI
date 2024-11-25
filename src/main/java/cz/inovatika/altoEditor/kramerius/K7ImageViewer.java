package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Config;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class K7ImageViewer {

    private static final Logger LOGGER = LogManager.getLogger(K7ImageViewer.class.getName());

    public HttpResponse getResponse(String pid, String instanceId, UserProfile userProfile) throws IOException {

        String imageUrl = Config.getKrameriusInstanceUrl(instanceId) +
                Config.getKrameriusInstanceUrlImage(instanceId) +
                pid + "/full/max/0/default.jpg";
        LOGGER.info("Trying to download image from " + imageUrl);

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(imageUrl);
        httpGet.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + userProfile.getToken()));
        httpGet.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        HttpResponse response = httpClient.execute(httpGet);
        return response;
    }
}
