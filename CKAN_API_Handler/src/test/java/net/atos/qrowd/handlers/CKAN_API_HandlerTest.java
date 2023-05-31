package net.atos.qrowd.handlers;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CKAN_API_HandlerTest {

    private CKAN_API_Handler apiHandler;

    @Before
    public void setUp() throws Exception {
        String host = "https://ckan-cartagena-odala.hopu.eu";
        String apikey = "36c4c795-d9f0-490d-8046-c255a1019bf6";
        apiHandler = new CKAN_API_Handler(host, apikey);
    }

    @Test
    public void createOrganization() {
        try {
            apiHandler.createOrganization("odala2");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createPackage() {
        try {
            apiHandler.createPackage("odala2", "packageid2", "description", true, "tags1");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createOrUpdateResource() {
        try {
            apiHandler.createOrUpdateResource("packageid2", "resource", "yyyy-MM-dd'T'HH:mm:ssZ", "/home/hopu/Downloads/cartagena-odala.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
