/**
 * Copyright 2018 Atos
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.atos.qrowd.handlers;

import com.google.gson.Gson;
import net.atos.qrowd.pojos.*;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CKAN_API_Handler {
    private final Logger log = Logger.getLogger(CKAN_API_Handler.class);

    private String HOST;
    private String api_key;
    private CloseableHttpClient httpclient;

    public CKAN_API_Handler(String HOST, String api_key) {
        this.HOST = HOST;
        this.api_key = api_key;
        this.httpclient = HttpClients.createDefault();
    }

    /**
     * Call the CKAN API to check if the dataset with the name passed as argument exists in the CKAN instance
     *
     * @param package_id The name of the package to check the existence of
     * @return boolean -> true if found, false in other case
     * @throws IOException Exception parsing the result message or closing the connection
     */
    public boolean packageExists(String package_id) throws IOException {

        String line;
        StringBuilder sb = new StringBuilder();
        HttpPost postRequest;
        Gson gson = new Gson();

        postRequest = new HttpPost(HOST + "/api/3/action/package_search?q=name:" + package_id);
        postRequest.setHeader("X-CKAN-API-Key", api_key);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        httpclient.close();
        response.close();
        // Parse the response into a POJO to be able to get results from it.
        // ToDo: If no result is returned, raise an error (when converting to POJO fails or return code !=200?)
        if (statusCode == 200) {
            CkanFullList CkanFullList = gson.fromJson(sb.toString(), CkanFullList.class);
            //by default we get the first package_ of the list of packages
            if (CkanFullList.getPackage().getPackages().size() == 1) {
                log.info("Package: " + package_id + " was found in CKAN.");
                return true;
            } else {
                log.warn("Package: " + package_id + " not found");
                //ToDo: Null, really?
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Method to get a complete dataset with all its resources from the CKAN API
     *
     * @param name The name of the package to check the existence of
     * @return Package_ class with the requested data if it exists, null if not found
     * @throws IOException Exception parsing the result message or closing the connection
     */
    public Package_ getPackageByName(String name) throws IOException {
        HttpPost postRequest;
        StringBuilder sb = new StringBuilder();
        String line;

        Gson gson = new Gson();

        //query the API to get the resources with that file name
        postRequest = new HttpPost(HOST + "/api/3/action/package_search?q=name:" + name);
        postRequest.setHeader("X-CKAN-API-Key", api_key);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        httpclient.close();
        response.close();
        // Parse the response into a POJO to be able to get results from it.
        // ToDo: If no result is returned, raise an error (when converting to POJO fails or return code !=200?)
        if (statusCode == 200) {
            CkanFullList CkanFullList = gson.fromJson(sb.toString(), CkanFullList.class);
            //by default we get the first package_ of the list of packages
            if (CkanFullList.getPackage().getPackages().size() == 1) {
                log.info("Package: " + name + " was found in CKAN.");
                return CkanFullList.getPackage().getPackages().get(0);
            } else {
                log.warn("Package: " + name + " not found");
                //ToDo: Null, really?
                return null;
            }
        } else {
            return null; //........
        }

    }

    /**
     * Method to create an empty dataset  using the CKAN API
     *
     * @param organization_id
     * @param package_id          Name of the package to be created
     * @param package_description
     * @param package_private
     * @param tags                Comma-separated String of tags to add to the dataset
     * @throws IOException Exception parsing the result message or closing the connection
     */
    public void createPackage(String organization_id, String package_id, String package_description, boolean package_private, String tags) throws IOException {

        HttpPost postRequest;
        StringBuilder sb = new StringBuilder();
        String line;

        //Split <tags> by "," and for each element in the list generate a tag
        if (tags == null) {
            tags = "";
        }
        String[] tagList = tags.split(",");
        List<Tag> list = new ArrayList<>();
        for (String tag : tagList) {
            Tag t = new Tag();
            //Since CKAN only allows alphanumeric and _ we need to deal with illegal characters/spaces...
            t.setName(tag.replaceAll("[^\\.a-zA-Z0-9]+", "_"));
            list.add(t);
        }

        Package_ pack = new Package_();
        pack.setName(package_id);
        pack.setOwnerOrg(organization_id);
        pack.setNotes(package_description);
        pack.setPrivate(package_private);
        //Set the new list of tags for the dataset

        if (list.size() == 0 || tags.trim().isEmpty()) {
            pack.setTags(null);
            pack.setNumTags(0);
        } else {
            pack.setTags(list);
            pack.setNumTags(list.size());
        }
        Gson gson = new Gson();

        StringEntity reqEntity = new StringEntity(gson.toJson(pack));

        postRequest = new HttpPost(HOST + "/api/3/action/package_create?use_default_schema=true");
        postRequest.setEntity(reqEntity);
        postRequest.setHeader("X-CKAN-API-Key", api_key);
        postRequest.setHeader("Accept", "application/json");
        postRequest.setHeader("Content-type", "application/json");

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();

        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        sb.append(statusCode);
        sb.append("\n");
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        httpclient.close();
        response.close();

        if (statusCode != 200) {
            log.error("statusCode =!=" + statusCode);
            log.error("Error creating the package via CKAN API. Package id: " + package_id);
            log.error(sb);
        } else {
            log.info("Request returns statusCode 200: OK");
            log.info(sb);
        }
    }

    public void createPackagePojoNoResources(Package_ dataset, String name, String tags) throws IOException {

        //Split <tags> by "," and for each element in the list generate a tag
        if (tags == null) {
            tags = "";
        }
        String[] tagList = tags.split(",");
        List<Tag> list = new ArrayList<>();
        for (String tag : tagList) {
            Tag t = new Tag();
            //Since CKAN only allows alphanumeric and _ we need to deal with illegal characters/spaces...
            t.setName(tag.replaceAll("[^\\.a-zA-Z0-9]+", "_"));
            list.add(t);
        }

        HttpPost postRequest;
        StringBuilder sb = new StringBuilder();
        String line;
        //Set the new dataset name and title
        dataset.setName(name);
        dataset.setTitle(name);

        //Remove identifiers of the old dataset and its resources
        dataset.setId(null);
        dataset.setRevisionId(null);
        dataset.setResources(null);
        dataset.setNumResources(null);

        //Set the new list of tags for the dataset

        if (list.size() == 0 || tags.trim().isEmpty()) {
            dataset.setTags(null);
            dataset.setNumTags(0);
        } else {
            dataset.setTags(list);
            dataset.setNumTags(list.size());
        }
        Gson gson = new Gson();

        System.out.println(gson.toJson(dataset));
        StringEntity reqEntity = new StringEntity(gson.toJson(dataset));

        postRequest = new HttpPost(HOST + "/api/action/package_create");
        postRequest.setEntity(reqEntity);
        postRequest.setHeader("X-CKAN-API-Key", api_key);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();

        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        sb.append(statusCode);
        sb.append("\n");
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        httpclient.close();
        response.close();

        if (statusCode != 200) {
            log.error("statusCode =!=" + statusCode);

            log.error(sb);
        } else {
            log.info("Request returns statusCode 200: OK");
            log.info(sb);
        }
    }

    /**
     * Method that checks if the organization with organization_id stored in the object variable exists or not
     *
     * @return boolean-> true if exists, false otherwise
     * @throws IOException Exception parsing the result message or closing the connection
     */
    public boolean organizationExists(String organization_id) throws IOException {
        String line;
        StringBuilder sb = new StringBuilder();
        HttpPost postRequest;

        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("id", new StringBody(organization_id, ContentType.TEXT_PLAIN))
                .build();

        postRequest = new HttpPost(HOST + "/api/action/organization_show");
        postRequest.setEntity(reqEntity);
        postRequest.setHeader("X-CKAN-API-Key", api_key);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();

        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        httpclient.close();
        response.close();

        if (statusCode == 200) {
            log.info("Organization with id " + organization_id + " exists");
            log.info(sb);
            return true;
        } else {
            log.warn("Organization with id " + organization_id + " not found");
            log.warn(sb);
            return false;
        }
    }

    /**
     * Method to create a new organization with the organization_id stored in the object
     *
     * @throws IOException Exception parsing the result message or closing the connection
     */
    public void createOrganization(String organization_id) throws IOException {

        HttpPost postRequest;
        StringBuilder sb = new StringBuilder();
        String line;

        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("name", new StringBody(organization_id, ContentType.TEXT_PLAIN))
                .addPart("id", new StringBody(organization_id, ContentType.TEXT_PLAIN))
                .addPart("title", new StringBody(organization_id, ContentType.TEXT_PLAIN))
                .build();

        postRequest = new HttpPost(HOST + "/api/action/organization_create");
        postRequest.setEntity(reqEntity);
        postRequest.setHeader("X-CKAN-API-Key", api_key);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();

        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        sb.append(statusCode);
        sb.append("\n");
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        httpclient.close();
        response.close();

        if (statusCode != 200) {
            log.error("statusCode =!=" + statusCode);
            log.error(sb);
        } else {
            log.info("Request returns statusCode 200: OK");
            log.info(sb);
        }
    }

    /**
     * Method to upload a resource previously created with the new id specified in resourceFileName, to a dataset with id dataset_name
     *
     * @param resource         Resource previously created or gotten from the API
     * @param dataset_name     Id of the dataset to upload the resource to
     * @param resourceFileName New name of the resource
     * @throws IOException Exception parsing the result message, getting the file in the resource or closing the connection
     */
    public void uploadFilePojo(Resource resource, String dataset_name, String resourceFileName) throws IOException {

        URL url = new URL(resource.getUrl());
        String tDir = System.getProperty("java.io.tmpdir");
        String path = tDir + "/" + resourceFileName;
        File file = new File(path);
        file.deleteOnExit();
        FileUtils.copyURLToFile(url, file);

        ContentBody cbFile = new FileBody(file, ContentType.TEXT_HTML);
        HttpPost postRequest;

        MultipartEntityBuilder multipart = MultipartEntityBuilder.create()
                .addPart("file", cbFile)
                .addPart("key", new StringBody(resourceFileName.split("\\.")[0], ContentType.TEXT_PLAIN))
                .addPart("name", new StringBody(resourceFileName, ContentType.TEXT_PLAIN))
                .addPart("package_id", new StringBody(dataset_name, ContentType.TEXT_PLAIN))
                .addPart("upload", cbFile);
        if (resource.getUrl() != null) {
            multipart.addPart("url", new StringBody(resource.getUrl(), ContentType.TEXT_PLAIN));
        }
        if (resource.getFormat() != null) {
            multipart.addPart("format", new StringBody(resource.getFormat(), ContentType.TEXT_PLAIN));
        }
        if (resource.getDescription() != null) {
            multipart.addPart("description", new StringBody(resource.getDescription(), ContentType.TEXT_PLAIN));
        }
        if (resource.getMimetype() != null) {
            multipart.addPart("mimetype", new StringBody(resource.getMimetype().toString(), ContentType.TEXT_PLAIN));
        }
        HttpEntity reqEntity = multipart.build();

        postRequest = new HttpPost(HOST + "/api/3/action/resource_create");
        postRequest.setEntity(reqEntity);
        postRequest.setHeader("X-CKAN-API-Key", api_key);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();

        String line;
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        if (statusCode != 200) {
            log.error("statusCode =!=" + statusCode);
            log.error(sb.toString());
        } else log.info("Request returns statusCode 200: OK");
        response.close();
        httpclient.close();
    }

    public Boolean createOrUpdateResource(String package_id, String resource_name, String resource_suffix_regex, String path, String resource_format) throws IOException {
        File file = new File(path);
        String filename = file.getName().replaceAll("[^\\.a-zA-Z0-9]+", "_");
        HttpPost postRequest;
        StringBuilder sb = new StringBuilder();
        String line;

        Gson gson = new Gson();

        //query the API to get the resources with that file name
        postRequest = new HttpPost(HOST + "/api/action/resource_search?query=name:" + filename);
        postRequest.setHeader("X-CKAN-API-Key", api_key);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        httpclient.close();
        response.close();

        //Parse the response into a POJO to be able to get results from it.
        ResourceResponse resResponse = gson.fromJson(sb.toString(), ResourceResponse.class);
        System.out.println(resResponse);

        String resource_packageId;
        String id;
        //This is needed to check that the resource belongs to the current package
        Package_ foundPackage = getPackageByName(package_id);
        String foundPackageId = "Not_found";
        if (foundPackage != null) {
            foundPackageId = foundPackage.getId();
        }

        //Now we need to check if the count of results is 1 (otherwise error)
        //if the count is 0, call uploadFile to create the file
        if (resResponse.getResult().getCount() == 0) {
            log.info("No resource found under that name, creating it...");
            uploadFile(package_id, resource_name, resource_suffix_regex, path, resource_format);
            return true;
            //if the count is 1, get all the needed data to update the resource
        } else if (resResponse.getResult().getCount() == 1) {
            resource_packageId = resResponse.getResult().getResults().get(0).getPackageId();
            id = resResponse.getResult().getResults().get(0).getId();
            //If the resource's package_id is the same as the current package id (search for package by name and get the id)
            if (foundPackage != null && resource_packageId.equals(foundPackageId)) {
                log.info("Resource found in the current package, updating it");
                updateFile(path, id);
                return true;
            } else {
                //If no package is found(cannot happen because the resource must belong to a package) or the package is different than the current one
                log.warn("The found resource does not belong to the current package");
                log.warn("Current package id found:" + foundPackageId + ". Package expected:" + resource_packageId);
                log.warn("Creating the resource in the current package");
                uploadFile(package_id, resource_name, resource_suffix_regex, path, resource_format);
                return true;
            }
        } else {
            // Iterate over all the resources, checking if any of them belongs to the current package
            // If any belongs, update it
            // If none belongs, create the resource in the current package

            boolean isPackageFound = false;
            for (Result_ result : resResponse.getResult().getResults()) {
                resource_packageId = result.getPackageId();
                id = result.getId();
                //This is needed to check that the resource belongs to the current package
                foundPackage = getPackageByName(package_id);
                foundPackageId = "Not_found";
                if (foundPackage != null) {
                    foundPackageId = foundPackage.getId();
                }
                if (foundPackage != null && resource_packageId.equals(foundPackageId)) {
                    log.info("Resource found in the current package, updating it");
                    updateFile(path, id);
                    isPackageFound = true;
                }
            }
            if (!isPackageFound) {
                log.warn("None of the found resources belongs to the current package");
                log.warn("Creating the resource in the current package");
                uploadFile(package_id, resource_name, resource_suffix_regex, path, resource_format);
            }
            return true;
        }
    }

    /**
     * Update the file stored in the resource with id resourceId
     *
     * @param path       Local path of the file to upload to the resource
     * @param resourceId Id of the resource to upload the file to
     * @throws IOException Exception parsing the result message or closing the connection
     */
    private void updateFile(String path, String resourceId) throws IOException {
        File file = new File(path);

        HttpPost postRequest;
        ContentBody cbFile = new FileBody(file, ContentType.TEXT_HTML);
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("id", new StringBody(resourceId, ContentType.TEXT_PLAIN))
                .addPart("file", cbFile)
                .addPart("upload", cbFile)
                .build();

        postRequest = new HttpPost(HOST + "/api/action/resource_patch");
        postRequest.setEntity(reqEntity);
        postRequest.setHeader("X-CKAN-API-Key", api_key);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();
        httpclient.close();
        response.close();

        if (statusCode != 200) {
            log.error("statusCode =!=" + statusCode);
        } else log.info("Request returns statusCode 200: OK");
    }

    /**
     * Function that uploads a file to CKAN through it's API
     *
     * @param path       Local filesystem path of the file to upload
     * @throws IOException Exception parsing the result message or closing the connection
     */
    private void uploadFile(String package_id, String resource_name, String resource_suffix_regex, String path, String resource_format) throws IOException {
        File file = new File(path);
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat(resource_suffix_regex);
        String date = dateFormatGmt.format(new Date());
        StringBuilder sb = new StringBuilder();
        String line;

        HttpPost postRequest;
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addTextBody("name", String.format("%s-%s", resource_name, date), ContentType.DEFAULT_TEXT)
                .addTextBody("package_id", package_id, ContentType.DEFAULT_TEXT)
                .addTextBody("format", resource_format, ContentType.DEFAULT_TEXT)
                .addBinaryBody("upload", file)
                .build();

        postRequest = new HttpPost(HOST + "/api/3/action/resource_create");
        postRequest.setEntity(reqEntity);
        postRequest.setHeader("X-CKAN-API-Key", api_key);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = httpclient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();

        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        sb.append(statusCode);
        sb.append("\n");
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        httpclient.close();
        response.close();

        if (statusCode != 200) {
            log.error("Error creating a resource: " + file.getName().split("\\.")[0] + "in package:" + package_id);
            log.error("statusCode =!=" + statusCode);
            log.error(sb);
        } else log.info("Request returns statusCode 200: OK");
    }

    public void close() {
        try {
            httpclient.close();
        } catch (IOException e) {
            log.error(e);
        }
    }
}
