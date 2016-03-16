package org.jahia.modules.external.elvis.communication;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import java.io.IOException;

/**
 * @author dgaillard
 */
public class ElvisSession {
    private static Logger logger = LoggerFactory.getLogger(ElvisSession.class);

    private String userName;
    private String password;
    private String baseUrl;
    private String fileLimit;
    private CloseableHttpClient httpClient;
    private HttpClientContext context;

    public ElvisSession(String baseUrl, String userName, String password, String fileLimit) {
        if (baseUrl.endsWith("/")) {
            baseUrl = StringUtils.substringBeforeLast(baseUrl, "/");
        }
        this.baseUrl = baseUrl;
        this.userName = userName;
        this.password = password;
        this.fileLimit = fileLimit;
    }

    public <X> X execute(ElvisSessionCallback<X> callback) throws PathNotFoundException {
        try {
            return callback.doInElvis();
        } catch (Exception e) {
            return callback.onError(e);
        }
    }

    public void initHttp() {
        context = HttpClientContext.create();
        CookieStore cookieStore = new BasicCookieStore();
        context.setCookieStore(cookieStore);
        httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
    }

    public void logout() {
        try {
            getDataFromApi("/logout");
            httpClient.close();
        } catch (IOException e) {
            logger.error("Could not logout from the ELVIS API !", e.getMessage());
        }
    }

    public void closeHttp() {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("Could not logout from the ELVIS API !", e.getMessage());
        }
    }

    public boolean isSessionAvailable() {
        if (this.context.getCookieStore().getCookies().isEmpty()) {
            try {
                // Execute get request to connect to the Elvis API
                connectToApi();
                return !context.getCookieStore().getCookies().isEmpty();
            } catch(IOException e) {
                logger.error("Could not login to the ELVIS API !", e.getMessage());
                return false;
            }
        } else {
            return true;
        }
    }

    public CloseableHttpResponse connectToApi() throws IOException {
        HttpGet get = new HttpGet(this.baseUrl + "/services/login?username=" + userName + "&password=" + password);
        get.setHeader("Accept", "Application/Json");
        return httpClient.execute(get, context);
    }

    public CloseableHttpResponse getDataFromApi(String endOfUri) throws IOException {
        HttpGet get = new HttpGet(this.baseUrl + "/services" + endOfUri);
        get.setHeader("Accept", "Application/Json");
        return httpClient.execute(get, context);
    }

    public CloseableHttpResponse getFileStream(String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", "*/*");
        return httpClient.execute(get, context);
    }

    public String getFileLimit() {
        return fileLimit;
    }
}
