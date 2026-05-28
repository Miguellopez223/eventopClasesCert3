package edu.upb.eventop.integracion;

import edu.upb.eventop.service.exception.NotDataFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;



@Slf4j
@Service
public class SistemaA {

    @Value("${sistema1.url-base}")
    private String urlBase;
    @Value("${sistema1.connect-timeout}")
    private int connectTimeout = 10000;
    @Value("${sistema1.read-timeout}")
    private int readTimeout = 40000;


    public Sistema1AuthResponse auth(Sistema1AuthRequest request) throws Exception {
        RestClient restClient = create();

        ResponseEntity<Sistema1AuthResponse> response;
        try {
            response = restClient.post()
                    .uri(urlBase + "/api/v1/auth")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .body(request)
                    .retrieve()
                    .toEntity(Sistema1AuthResponse.class);
        } catch (NotDataFoundException e) {
            log.error("NotDataFoundException. {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Exception. ", e);
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Se genero error: {}", response.getStatusCode().value());
            throw new Exception("Se genero error");
        }

        return response.getBody();
    }



    private RestClient create() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        clientHttpRequestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder().requestFactory(clientHttpRequestFactory).build();
    }

}
