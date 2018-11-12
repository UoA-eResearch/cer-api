package nz.ac.auckland.cer.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nz.ac.auckland.cer.model.RequestConfig;
import okhttp3.*;
import okhttp3.ResponseBody;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@RestController
public class RequestController {

    private static final Logger logger = LoggerFactory.getLogger(RequestController.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient.Builder builder;
    private OkHttpClient client;

    @Value("${service-now.base-url}")
    private String baseUrl;

    @Value("${service-now.api-key}")
    private String apiKey;

    @Value("${service-now.requests-config-file}")
    private String requestsConfigFile;

    @Value("${ok-http.proxy}")
    private String proxy;

    private HashMap<String, RequestConfig> requestConfigHashMap = new HashMap<>();

    RequestController() {

    }

    private void buildClient() throws MalformedURLException {
        if (builder == null || client == null) {
            builder = new OkHttpClient.Builder();

            // If proxy env variables are set then use proxy
            if (!proxy.equals("")) {
                URL proxyUrl = new URL(proxy);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort()));
                builder.proxy(proxy);
            }

            client = builder.build();
        }
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    private ResponseBody post(String url, String json) throws IOException {
        okhttp3.RequestBody body = okhttp3.RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("apikey", apiKey)
                .post(body)
                .build();
        logger.info("post() local variable apikey = {}", apiKey);
        logger.info("post() local variable request = {}", request);
        Response response = client.newCall(request).execute();
        return response.body();
    }

    private String getPrimaryEmail(String mail) {
        String[] addresses = mail.split(";");

        if (addresses.length > 0) {
            return addresses[0];
        }

        return "";
    }

    private void loadRequestConfigurations() throws IOException {
        if (requestConfigHashMap.size() == 0) {
            ObjectMapper mapper = new ObjectMapper();
            RequestConfig[] requestConfigList = mapper.readValue(new File(requestsConfigFile), RequestConfig[].class);

            for (RequestConfig requestConfig : requestConfigList) {
                requestConfigHashMap.put(requestConfig.getId(), requestConfig);
            }
        }
    }

    private RequestConfig getRequestConfig(String key) throws IOException {
        this.loadRequestConfigurations();

        return requestConfigHashMap.get(key);
    }

    /*
     * Create ServiceNow VM Consultation ticket
     */

    @RequestMapping(method = RequestMethod.POST, value = "/serviceRequest/vm")
    ResponseEntity<Object> createServiceRequest(@RequestAttribute(value = "uid") String requestorUpi,
                                                @RequestAttribute(value = "displayName") String displayName,
                                                @RequestAttribute(value = "mail") String mail,
                                                @RequestBody String body) throws IOException {

        logger.info("createServiceRequest() called with arguments requestorUPi: {}, displayName: {}, mail: {}, body: {}", requestorUpi, displayName, mail, body);

        return this.createRequest("vm", requestorUpi, displayName, mail, body);
    }

    /*
     * Create ServiceNow Data Consultation ticket
     */

    @RequestMapping(method = RequestMethod.POST, value = "/serviceRequest/storage")
    ResponseEntity<Object> createRequestStorage(@RequestAttribute(value = "uid") String requestorUpi,
                                                @RequestAttribute(value = "displayName") String displayName,
                                                @RequestAttribute(value = "mail") String mail,
                                                @RequestBody String body) throws IOException {

        return this.createRequest("storage", requestorUpi, displayName, mail, body);
    }

    private ResponseEntity<Object> createRequest(String requestConfigKey, String requestorUpi, String displayName,
                                                 String mail, String body) throws IOException {

        logger.info("createRequest() called with arguments requestConfigKey: {}, requestorUpid: {}, displayName: {}, mail: {}, body: {}", requestConfigKey, requestorUpi, displayName, mail, body);

        RequestConfig requestConfig = this.getRequestConfig(requestConfigKey);

        // Generate comments based on template
        StringTemplate template = this.getTemplate("service_request_templates/" + requestConfigKey + ".tpl", body);
        template.setAttribute("requestorUpi", requestorUpi);
        template.setAttribute("displayName", displayName);
        template.setAttribute("mail", this.getPrimaryEmail(mail));
        String shortDescription = requestConfig.getShortDescription() + ": " + displayName + ", " + requestorUpi;
        String output = template.toString();

        return this.sendServiceNowRequest(requestorUpi, requestConfig.getCategory(), requestConfig.getSubcategory(),
                requestConfig.getCmdbCiId(), requestConfig.getAssignmentGroupId(), requestConfig.getBusinessServiceId(),
                shortDescription, output, requestConfig.getWatchList(), requestConfig.getCorrelationDisplay());
    }

    private StringTemplate getTemplate(String templateName, String body) throws IOException {
        ClassPathResource res = new ClassPathResource(templateName);
        String templateFile = new String(FileCopyUtils.copyToByteArray(res.getInputStream()), StandardCharsets.UTF_8);
        StringTemplate template = new StringTemplate(templateFile, DefaultTemplateLexer.class);

        Map<String, Object> objectMap = new ObjectMapper().readValue(body, new TypeReference<Map<String, Object>>() {
        });
        template.setAttributes(objectMap);

        return template;
    }

    private ResponseEntity<Object> sendServiceNowRequest(String requestorUpi, String category, String subcategory,
                                                         String cmdbCiId, String assignmentGroup, String businessServiceId,
                                                         String shortDescription, String comments, String watchList, String correlationDisplay) throws IOException {

        logger.info("sendServiceNowRequest() called with arguments requestorUpi: {}, category: {}, subcategory: {}, cmdbCiId: {}, assignmentGroup: {}, businessServiceId: {}, shortDescription: {}, comments: {}, watchList: {}, correlationDisplay: {}", requestorUpi, category, subcategory, cmdbCiId, assignmentGroup, businessServiceId, shortDescription, comments, watchList, correlationDisplay);

        this.buildClient();

        String url = baseUrl + "/service/servicenow-readwrite/import/u_rest_u_request";
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        JSONObject response = new JSONObject();
        response.put("status", httpStatus.value());
        response.put("statusText", httpStatus.getReasonPhrase());

        // Create ticket body
        JSONObject body = new JSONObject()
                .put("u_requestor", requestorUpi)
                .put("u_assignment_group", assignmentGroup)
                .put("u_category", category)
                .put("u_subcategory", subcategory)
                .put("u_cmdb_ci", cmdbCiId)
                .put("u_business_service", businessServiceId)
                .put("u_short_description", shortDescription)
                .put("u_comments", comments)
                .put("u_correlation_id", "skav012too1337forguids") // TODO: Update with java.util.UUID.randomUUID();
                .put("u_watch_list", watchList)
                .put("u_correlation_display", correlationDisplay);

        try {
            // Submit ticket
            ResponseBody responseBody = post(url, body.toString());
            JSONObject serviceNowResponse = new JSONObject(responseBody.string());

            logger.info("serviceNowRespose = {}", serviceNowResponse);

            if (!serviceNowResponse.isNull("result")) {
                JSONObject result = serviceNowResponse.getJSONArray("result").getJSONObject(0);
                httpStatus = HttpStatus.OK;
                String ticketUrl = String.format("https://uoa%s.service-now.com/nav_to.do?uri=u_request.do?sys_id=%s",
                        baseUrl == "https://api.auckland.ac.nz" ? "prod" : baseUrl.split("\\.")[1],
                        result.getString("sys_id"));
                response.put("status", httpStatus.value());
                response.put("statusText", httpStatus.getReasonPhrase());
                response.put("ticketNumber", result.getString("display_value"));
                response.put("ticketUrl", ticketUrl);
            } else if (!serviceNowResponse.isNull("error")) {
                JSONObject error = serviceNowResponse.getJSONObject("error");
                logger.error("status: " + httpStatus.value() + ", statusText: " + httpStatus.getReasonPhrase());
                logger.error("ServiceNow internal error: " + error.toString());
            }
        } catch (IOException e) {
            logger.error("status: " + httpStatus.value() + ", statusText: " + httpStatus.getReasonPhrase());
            logger.error("Error communicating with ServiceNow: " + e.toString());
        } catch (JSONException e) {
            logger.error("status: " + httpStatus.value() + ", statusText: " + httpStatus.getReasonPhrase());
            logger.error("Error reading ServiceNow response: " + e.toString());
        }

        return new ResponseEntity<>(response.toString(), httpStatus);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Object> handleMissingParams(ServletRequestBindingException e) {
        HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;
        JSONObject response = new JSONObject();
        response.put("status", httpStatus.value());
        response.put("statusText", httpStatus.getReasonPhrase());
        response.put("error", "User is not authenticated with UoA Single Sign On");

        logger.error("status: " + httpStatus.value() + ", statusText: " + httpStatus.getReasonPhrase());
        logger.error("User is not authenticated with UoA Single Sign On: " + e.toString());

        return new ResponseEntity<>(response.toString(), httpStatus);
    }
}
