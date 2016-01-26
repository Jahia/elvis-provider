package org.jahia.modules.external.elvis;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Damien GAILLARD
 */
public class ElvisDataSource extends FilesDataSource {
    private static final Logger logger = LoggerFactory.getLogger(ElvisDataSource.class);

    private String ELVIS_BASE_PATH = "/services";
    private CookieStore cookieStore = new BasicCookieStore();
    private CloseableHttpClient httpClient;
    private ExternalContentStoreProvider externalContentStoreProvider;
    private HttpClientContext context;
    private String userName;
    private String password;
    private String url;

    @Override
    public ExternalFile getExternalFile(String path) throws PathNotFoundException {
        return new ExternalFile(ExternalFile.FileType.FOLDER, path, new Date(), new Date());
    }

    @Override
    public List<ExternalFile> getChildrenFiles(String path) throws RepositoryException {
        List<ExternalFile> childrenList = new ArrayList<>();
        try {
            HttpResponse browseResponse = get("/browse?path=" + path);
            if (browseResponse.getStatusLine().getStatusCode() == 200) {
                JSONArray jsonArray = new JSONArray(EntityUtils.toString(browseResponse.getEntity()));
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject element = jsonArray.getJSONObject(i);
                    childrenList.add(new ExternalFile(ExternalFile.FileType.FOLDER, element.get("assetPath").toString(), new Date(), new Date()));
                }
            }

//            HttpResponse searchResponse = get("/search?q=folderPath:" + path);
//            if (searchResponse.getStatusLine().getStatusCode() == 200) {
//                logger.info("HERE HERE HERE");
//                JSONObject jsonObject = new JSONObject(searchResponse.getEntity().getContent());
//                if (jsonObject.has("hits")) {
//                    JSONArray searchJsonArray = new JSONArray(jsonObject.getJSONArray("hits"));
//                    for (int i = 0 ; i < searchJsonArray.length() ; i++) {
//                        JSONObject element = searchJsonArray.getJSONObject(i);
//                        String elPath = element.get("assetPath").toString();
//                        long created = element.getJSONObject("assetCreated").getLong("value");
//                        long modified = element.getJSONObject("assetModified").getLong("value");
//                        childrenList.add(new ExternalFile(ExternalFile.FileType.FILE, elPath, new Date(created), new Date(modified)));
//                    }
//                }
//            }

            return childrenList;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Binary getFileBinary(String path) throws PathNotFoundException {
        return null;
    }

    @Override
    public boolean isAvailable() throws RepositoryException {
        if (this.context.getCookieStore().getCookies().size() == 0) {
            try {
                //Execute the post to Dalim API
                get("/login?username=" + this.userName + "&password=" + this.password);
                return this.context.getCookieStore().getCookies().size() > 0;
            } catch(IOException e) {
                logger.error("Could not login to the ELVIS API !", e.getMessage());
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void start() {
        this.context = HttpClientContext.create();
        this.context.setCookieStore(cookieStore);
        this.httpClient = HttpClientBuilder.create().setDefaultCookieStore(this.cookieStore).build();
    }

    @Override
    public void stop() {
        //Logout
        try {
            HttpPost post = new HttpPost(this.url + ELVIS_BASE_PATH + "/logout");
            this.httpClient.execute(post);
            this.httpClient.close();
        } catch (Exception e) {
            logger.error("Could not logout from the ELVIS API !", e.getMessage());
        }
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setExternalContentStoreProvider(ExternalContentStoreProvider externalContentStoreProvider) {
        this.externalContentStoreProvider = externalContentStoreProvider;
    }

    private HttpResponse get(String endOfUri) throws IOException {
        HttpGet get = new HttpGet(this.url + ELVIS_BASE_PATH + endOfUri);
        get.addHeader("Authorization", "Basic " + getAuthentication());
        get.setHeader("Accept", "Application/Json");
        get.setHeader("Content-Type", "Application/Json;charset=utf-8");
        return this.httpClient.execute(get, context);
    }

    private String getAuthentication() {
        return Base64.encodeBase64String((this.userName + ":" + this.password).getBytes());
    }
}
