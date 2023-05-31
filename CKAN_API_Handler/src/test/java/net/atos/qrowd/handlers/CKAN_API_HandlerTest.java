package net.atos.qrowd.handlers;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class CKAN_API_HandlerTest {

    private CKAN_API_Handler apiHandler;

    @Before
    public void setUp() throws Exception {
        String host = "https://demo.eu";
        String apikey = "36c4c795";
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
            apiHandler.createOrUpdateResource("packageid2", "resource", "yyyy-MM-dd'T'HH:mm:ssZ", "/home/hopu/Downloads/cartagena-odala", "json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
