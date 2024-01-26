/**
 * OpenAPI Petstore
 * This is a sample server Petstore server. For this sample, you can use the api key `special-key` to test the authorization filters.
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.openapitools.client.api;

import org.openapitools.client.model.Order;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * OpenAPI Petstore Test
 *
 * API tests for StoreApi 
 */
public class StoreApiTest {

    private StoreApi client;
    private String baseUrl = "http://localhost:9080";
    
    @Before
    public void setup() throws MalformedURLException {
        client = RestClientBuilder.newBuilder()
                        .baseUrl(new URL(baseUrl))
                        .register(ApiException.class)
                        .build(StoreApi.class);
    }

    
    /**
     * Delete purchase order by ID
     *
     * For valid response try integer IDs with value &lt; 1000. Anything above 1000 or nonintegers will generate API errors
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteOrderTest() {
    	// TODO: test validations
        String orderId = null;
        //void response = api.deleteOrder(orderId);
        //assertNotNull(response);
        
        
    }
    
    /**
     * Returns pet inventories by status
     *
     * Returns a map of status codes to quantities
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getInventoryTest() {
    	// TODO: test validations
        //Map<String, Integer> response = api.getInventory();
        //assertNotNull(response);
        
        
    }
    
    /**
     * Find purchase order by ID
     *
     * For valid response try integer IDs with value &lt;&#x3D; 5 or &gt; 10. Other values will generate exceptions
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getOrderByIdTest() {
    	// TODO: test validations
        Long orderId = null;
        //Order response = api.getOrderById(orderId);
        //assertNotNull(response);
        
        
    }
    
    /**
     * Place an order for a pet
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void placeOrderTest() {
    	// TODO: test validations
        Order body = null;
        //Order response = api.placeOrder(body);
        //assertNotNull(response);
        
        
    }
    
}
