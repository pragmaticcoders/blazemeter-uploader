package junit.converter.plugin;

import org.apache.maven.plugin.logging.Log;
import org.springframework.core.io.PathResource;
import org.springframework.http.*;
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

    public void uploadFile(File fileToUpload, Log log) {
        log.info("Uploading file: " + fileToUpload.getName());
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(prepareUploadRequest(fileToUpload), getHeaders());
        ResponseEntity<String> uploadResponse = new RestTemplate().exchange(getBlazemeterFilesUrl(), HttpMethod.POST, request, String.class);
        assert uploadResponse.getStatusCode().equals(HttpStatus.OK);
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

    private String getBlazemeterFilesUrl() {
        return UriComponentsBuilder.fromUriString(BLAZEMETER_URL).buildAndExpand(TEST_ID).toUriString();
    }
}