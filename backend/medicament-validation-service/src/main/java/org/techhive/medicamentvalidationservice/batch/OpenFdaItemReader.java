package org.techhive.medicamentvalidationservice.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.techhive.medicamentvalidationservice.dto.OpenFdaDrugResponse;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Reads drug data from the OpenFDA API in paginated chunks.
 */
@Slf4j
@Component
public class OpenFdaItemReader implements ItemReader<OpenFdaDrugResponse.DrugResult> {

    @Value("${openfda.api.base-url}")
    private String baseUrl;

    @Value("${openfda.api.page-size}")
    private int pageSize;

    @Value("${openfda.api.max-pages}")
    private int maxPages;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Queue<OpenFdaDrugResponse.DrugResult> buffer = new LinkedList<>();
    private int currentPage = 0;
    private boolean finished = false;

    @Override
    public OpenFdaDrugResponse.DrugResult read() {
        if (!buffer.isEmpty()) {
            return buffer.poll();
        }

        if (finished) {
            return null;
        }

        // Fetch next page
        try {
            int skip = currentPage * pageSize;
            String url = baseUrl + "?limit=" + pageSize + "&skip=" + skip;
            log.debug("Fetching OpenFDA page {}: {}", currentPage, url);

            OpenFdaDrugResponse response = restTemplate.getForObject(url, OpenFdaDrugResponse.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                finished = true;
                return null;
            }

            buffer.addAll(response.getResults());
            currentPage++;

            if (currentPage >= maxPages) {
                log.info("Reached max pages limit ({})", maxPages);
                finished = true;
            }

            return buffer.poll();
        } catch (Exception e) {
            log.error("Error fetching OpenFDA data at page {}: {}", currentPage, e.getMessage());
            finished = true;
            return null;
        }
    }

    public void reset() {
        buffer.clear();
        currentPage = 0;
        finished = false;
    }
}
