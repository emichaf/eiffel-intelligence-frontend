package com.ericsson.ei.frontend;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.ei.frontend.utils.BackEndInstanceFileUtils;
import com.ericsson.ei.frontend.utils.BackEndInstancesUtils;
import com.google.gson.JsonParser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TestSubscriptionCRUD {
    private static final String SUBSCRIPTION_ENDPOINT = "/subscriptions";
    private static final String SUBSCRIPTION_DELETE_ENDPOINT = "/subscriptions/Subscription_1";
    private static final String SUBSCRIPTION_FILE_PATH = "src/functionaltest/resources/responses/subscription.json";
    private static final String ADMIN = "admin";
    private static final String NOT_FOUND = "[]";
    private String subscriptionRequestBody;
    private String responseBodyPost;
    private String responseBodyPut;
    private String responseBodyDelete;
    private String encodedAuth;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BackEndInstancesUtils backEndInstancesUtils;

    @Autowired
    private BackEndInstanceFileUtils backEndInstanceFileUtils;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    private MockServerClient mockServerClient;

    @Before
    public void init() throws Exception {
    	File tempFile = File.createTempFile("tempfile", ".json");
        tempFile.deleteOnExit();

        String filePath = tempFile.getAbsolutePath().toString();
        Files.write(Paths.get(filePath), "[]".getBytes());
        backEndInstanceFileUtils.setEiInstancesPath(filePath);
        
        backEndInstancesUtils.setDefaultBackEndInstanceToNull();
        backEndInstancesUtils.setDefaultBackEndInstance("test", "localhost", mockServerRule.getPort(), "", false);

        subscriptionRequestBody = FileUtils.readFileToString(new File(SUBSCRIPTION_FILE_PATH), "UTF-8");
        String auth = ADMIN + ":" + ADMIN;
        encodedAuth = StringUtils.newStringUtf8(Base64.encodeBase64(auth.getBytes()));
        responseBodyPost = new JsonParser().parse("{\"msg\": \"Inserted Successfully\"," + "\"statusCode\": 200}")
                .toString();
        responseBodyPut = new JsonParser().parse("{\"msg\": \"Updated Successfully\"," + "\"statusCode\": 200}")
                .toString();
        responseBodyDelete = new JsonParser().parse("{\"msg\": \"Deleted Successfully\"," + "\"statusCode\": 200}")
                .toString();
    }

    @Test
    public void testCreateSubscriptionSuccess() throws Exception {
        mockSubscriptionEndpointForPostAndPut("POST", responseBodyPost);
        mockMvc.perform(post(SUBSCRIPTION_ENDPOINT).servletPath(SUBSCRIPTION_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth).content(subscriptionRequestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
                .andExpect(content().string(responseBodyPost)).andReturn();
    }

    @Test
    public void testCreateSubscriptionNotFound() throws Exception {
        mockSubscriptionEndpointForPostAndPut("POST", responseBodyPost);
        mockMvc.perform(post(SUBSCRIPTION_ENDPOINT).servletPath(SUBSCRIPTION_ENDPOINT).content(subscriptionRequestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isNotFound())
                .andExpect(content().string(NOT_FOUND)).andReturn();
    }

    @Test
    public void testUpdateSubscriptionSuccess() throws Exception {
        mockSubscriptionEndpointForPostAndPut("PUT", responseBodyPut);
        mockMvc.perform(put(SUBSCRIPTION_ENDPOINT).servletPath(SUBSCRIPTION_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth).content(subscriptionRequestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
                .andExpect(content().string(responseBodyPut)).andReturn();
    }

    @Test
    public void testUpdateSubscriptionNotFound() throws Exception {
        mockSubscriptionEndpointForPostAndPut("PUT", responseBodyPut);
        mockMvc.perform(put(SUBSCRIPTION_ENDPOINT).servletPath(SUBSCRIPTION_ENDPOINT).content(subscriptionRequestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isNotFound())
                .andExpect(content().string(NOT_FOUND)).andReturn();
    }

    @Test
    public void testDeleteSubscriptionSuccess() throws Exception {
        mockSubscriptionEndpointForDelete();
        mockMvc.perform(delete(SUBSCRIPTION_DELETE_ENDPOINT).servletPath(SUBSCRIPTION_DELETE_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(content().string(responseBodyDelete)).andReturn();
    }

    @Test
    public void testDeleteSubscriptionNotFound() throws Exception {
        mockSubscriptionEndpointForDelete();
        mockMvc.perform(delete(SUBSCRIPTION_DELETE_ENDPOINT).servletPath(SUBSCRIPTION_DELETE_ENDPOINT)
                .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isNotFound())
                .andExpect(content().string(NOT_FOUND)).andReturn();
    }

    @Test
    public void testGetSubscriptionSuccess() throws Exception {
        mockSubscriptionEndpointForGet();
        mockMvc.perform(MockMvcRequestBuilders.get(SUBSCRIPTION_DELETE_ENDPOINT)
                .servletPath(SUBSCRIPTION_DELETE_ENDPOINT).header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
                .andExpect(content().string(subscriptionRequestBody)).andReturn();
    }

    @Test
    public void testGetSubscriptionNotFound() throws Exception {
        mockSubscriptionEndpointForGet();
        mockMvc.perform(MockMvcRequestBuilders.get(SUBSCRIPTION_DELETE_ENDPOINT)
                .servletPath(SUBSCRIPTION_DELETE_ENDPOINT).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound()).andExpect(content().string(NOT_FOUND)).andReturn();
    }

    private void mockSubscriptionEndpointForPostAndPut(String method, String responseBody) {
        mockServerClient
                .when(request().withMethod(method).withPath(SUBSCRIPTION_ENDPOINT).withBody(subscriptionRequestBody)
                        .withHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth))
                .respond(response().withBody(responseBody).withStatusCode(200).withHeader(HttpHeaders.AUTHORIZATION,
                        "Basic " + encodedAuth));
    }

    private void mockSubscriptionEndpointForDelete() {
        mockServerClient
                .when(request().withMethod("DELETE").withPath(SUBSCRIPTION_DELETE_ENDPOINT)
                        .withHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth))
                .respond(response().withBody(responseBodyDelete).withStatusCode(200)
                        .withHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth));
    }

    private void mockSubscriptionEndpointForGet() {
        mockServerClient
                .when(request().withMethod("GET").withPath(SUBSCRIPTION_DELETE_ENDPOINT)
                        .withHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth))
                .respond(response().withBody(subscriptionRequestBody).withStatusCode(200)
                        .withHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth));
    }

    @After
    public void after() throws IOException {
    	backEndInstancesUtils.setDefaultBackEndInstanceToNull();
    }
}