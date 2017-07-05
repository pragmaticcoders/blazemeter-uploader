package junit.converter.plugin;

import org.springframework.core.io.PathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;

public class FileUploader {
    private String BLAZEMETER_URL = "https://a.blazemeter.com/api/v4/tests/{test-id}/files";
    private String API_KEY;
    private String TEST_ID;

    public FileUploader apiKey(String apiKey) {
        this.API_KEY = apiKey;
        return this;
    }

    public FileUploader testId(String testId) {
        this.TEST_ID = testId;
        return this;
    }

    public void uploadFile(File fileToUpload) {
        System.out.println("Uploading file: " + fileToUpload.getName());
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(prepareUploadRequest(fileToUpload), getHeaders());
        String result = new RestTemplate().postForObject(getBlazemeterFilesUrl(), request, String.class);

    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", API_KEY);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    private MultiValueMap<String, Object> prepareUploadRequest(File filesToUpload) {
        MultiValueMap<String, Object> uploadParameters = new LinkedMultiValueMap<String, Object>();
        uploadParameters.add("name", filesToUpload.getName());
        uploadParameters.add("filename", filesToUpload.getName());
        uploadParameters.add("file", new PathResource(filesToUpload.getPath()));
        return uploadParameters;
    }

    private static UriComponentsBuilder getUriBuilder() {
        return UriComponentsBuilder.newInstance();
    }

    private String getBlazemeterFilesUrl() {
        UriComponentsBuilder builder = getUriBuilder().path(BLAZEMETER_URL);
        return UriComponentsBuilder.fromUriString(BLAZEMETER_URL).buildAndExpand(TEST_ID).toUriString();
    }
}