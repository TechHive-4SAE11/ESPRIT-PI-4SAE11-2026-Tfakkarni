package org.techhive.medicamentvalidationservice.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.techhive.medicamentvalidationservice.dto.OpenFdaDrugResponse;

/**
 * Spring Batch ItemReader that paginates through the OpenFDA Drug API.
 * Reads one page (DrugResult list) at a time.
 */
@Slf4j
@Component
public class OpenFdaItemReader implements ItemReader<OpenFdaDrugResponse.DrugResult> {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final int pageSize;
    private final int maxPages;

    private int currentPage = 0;
    private int currentIndex = 0;
    private OpenFdaDrugResponse currentResponse;
    private boolean finished = false;

    public OpenFdaItemReader(
            @Value("${openfda.api.base-url}") String baseUrl,
            @Value("${openfda.api.page-size}") int pageSize,
            @Value("${openfda.api.max-pages}") int maxPages) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.pageSize = pageSize;
        this.maxPages = maxPages;
    }

    @Override
    public OpenFdaDrugResponse.DrugResult read() {
        if (finished) {
            return null;
        }

        // Fetch next page if needed
        if (currentResponse == null || currentIndex >= currentResponse.getResults().size()) {
            if (currentPage >= maxPages) {
                finished = true;
                log.info("Reached maximum pages ({}). Total pages read: {}", maxPages, currentPage);
                return null;
            }
            fetchNextPage();
            if (finished || currentResponse == null || currentResponse.getResults() == null || currentResponse.getResults().isEmpty()) {
                finished = true;
                return null;
            }
            currentIndex = 0;
        }

        return currentResponse.getResults().get(currentIndex++);
    }

    private void fetchNextPage() {
        int skip = currentPage * pageSize;
        String url = String.format("%s?limit=%d&skip=%d", baseUrl, pageSize, skip);

        try {
            log.info("Fetching OpenFDA page {} (skip={}, limit={})", currentPage + 1, skip, pageSize);
            currentResponse = restTemplate.getForObject(url, OpenFdaDrugResponse.class);

            if (currentResponse != null && currentResponse.getResults() != null) {
                log.info("Fetched {} drug records from page {}", currentResponse.getResults().size(), currentPage + 1);
                currentPage++;
            } else {
                log.info("No more results from OpenFDA API");
                finished = true;
            }
        } catch (RestClientException e) {
            log.error("Error fetching from OpenFDA API at page {}: {}", currentPage + 1, e.getMessage());
            // If we've already loaded some data, continue with what we have
            if (currentPage > 0) {
                log.warn("Stopping pagination due to API error. {} pages loaded successfully.", currentPage);
            }
            finished = true;
        }
    }

    /**
     * Reset reader state for re-execution (e.g., scheduled refresh).
     */
    public void reset() {
        currentPage = 0;
        currentIndex = 0;
        currentResponse = null;
        finished = false;
    }
}
