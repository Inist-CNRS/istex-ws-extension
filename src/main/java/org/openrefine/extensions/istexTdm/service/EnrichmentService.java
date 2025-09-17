package org.openrefine.extensions.istexTdm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openrefine.extensions.istexTdm.model.TdmRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnrichmentService {
    private static final Logger logger = LoggerFactory.getLogger("EnrichmentService");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public enum ResponseFormat {
        text,
        json_schema,
        json_object
    }

    // Use a single HttpClient instance for connection reuse
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2) // Use HTTP/2 if available
            .connectTimeout(Duration.ofSeconds(30)) // Set connection timeout
            .build();

    public static List<Serializable> invoke(String serviceUrl, List<TdmRequest> tdmRequests) throws Exception {
        String responseMessage;
        try {
            String payload = objectMapper.writeValueAsString(tdmRequests);
            logger.info("EnrichmentService - invoke - payload: {}", payload);

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl))
                    .timeout(Duration.ofSeconds(60)) // Set timeout for the request
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            // Send request and get response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("EnrichmentService raw response: {}", response.body());

            int responseCode = response.statusCode();
            if (responseCode == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                List<Serializable> responses = new ArrayList<>();
                if (responseJson.isArray()) {
                    for (JsonNode node : responseJson) {
                        // Assuming the response is an array of objects, and each object has a "value" field with the result.
                        // This might need adjustment based on the actual API response structure.
                        if (node.has("value")) {
                            responses.add(node.get("value").toString());
                        } else {
                            // Fallback or error handling if 'value' field is not found
                            responses.add("Error: 'value' field not found in response item.");
                        }
                    }
                }
                return responses;
            } else {
                logger.error("EnrichmentService request failure - {} {}", responseCode, response.body());
                responseMessage = MessageFormat.format(
                        "LLM request failed. Status Code : {0,number,integer}. Message : {1}", responseCode,
                        response.body());
            }
        } catch (InterruptedException e) {
            logger.error("EnrichmentService error InterruptedException - {}", e.getMessage());
            responseMessage = MessageFormat.format("LLM request failed. Message : InterruptedException {0}",
                    e.getMessage());
        } catch (SecurityException e) {
            logger.error("EnrichmentService error SecurityException - {}", e.getMessage());
            responseMessage = MessageFormat.format("LLM request failed. Message : SecurityException {0}",
                    e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("EnrichmentService error IllegalArgumentException - {}", e.getMessage());
            responseMessage = MessageFormat.format("LLM request failed. Message : IllegalArgumentException {0}",
                    e.getMessage());
        } catch (Exception e) {
            logger.error("EnrichmentService error Exception - {}", e.getMessage());
            responseMessage = MessageFormat.format("LLM request failed. Message : Exception {0}", e.getMessage());
        }

        throw new Exception(responseMessage);
    }

    public static String test() {
        String serviceUrl = "https://terms-extraction.services.istex.fr/v2/teeft/en";
        String userContent = "The diets of the most conspicuous reef‐fish species from northern Patagonia, the carnivorous species Pseudopercis semifasciata, Acanthistius patachonicus, Pinguipes brasilianus and Sebastes oculatus were studied. Pinguipes brasilianus had the narrowest diet and most specialized feeding strategy, preying mostly on reef‐dwelling organisms such as sea urchins, limpets, bivalves, crabs and polychaetes. The diet of A. patachonicus was characterized by the presence of reef and soft‐bottom benthic organisms, mainly polychaetes, crabs and fishes. Pseudopercis semifasciata showed the broadest spectrum of prey items, preying upon reef, soft‐bottom and transient organism (mainly fishes, cephalopods and crabs). All S. oculatus guts were empty, but stable‐isotope analyses suggested that this species consumed small fishes and crabs. In general, P. brasilianus depended on local prey populations and ate different reef‐dwelling prey than the other species. Pseudopercis semifasciata, A. patachonicus and probably S. oculatus, however, had overlapping trophic niches and consumed resources from adjacent environments. The latter probably reduces the importance of food as a limiting resource for these reef‐fish populations, facilitating their coexistence in spite of their high trophic overlap.";
        List<Serializable> tdmResponse;
        try {
            List<TdmRequest> requests = new ArrayList<>();
            requests.add(new TdmRequest(userContent));
            tdmResponse = invoke(serviceUrl, requests);
            return tdmResponse.get(0).toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

}
