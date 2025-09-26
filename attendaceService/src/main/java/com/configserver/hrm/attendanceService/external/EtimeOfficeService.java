package com.configserver.hrm.attendanceService.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EtimeOfficeService {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore = new BasicCookieStore();

    public EtimeOfficeService() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore) // keep cookies (JSESSIONID etc.)
                .build();
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    // ✅ Updated: now accepts date as parameter
    public byte[] downloadDailyReport(String reportDate) {
        try {
            // Step 1: Fetch login page -> extract token
            String loginPageUrl = "https://www.etimeoffice.com/Login/loginCheck";
            String loginPageHtml = restTemplate.getForObject(loginPageUrl, String.class);
            String token = extractToken(loginPageHtml);

            // Step 2: Prepare login request
            String loginUrl = "https://www.etimeoffice.com/Login/loginCheck";
            MultiValueMap<String, String> loginBody = new LinkedMultiValueMap<>();
            loginBody.add("loginModel.corporateId", "ConfigServer");
            loginBody.add("loginModel.userName", "ConfigServer");
            loginBody.add("loginModel.password", "ConfigServer@22");
            loginBody.add("pageTital", "Login Page");
            loginBody.add("__RequestVerificationToken", token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Step 3: Perform login
            ResponseEntity<String> loginResp = restTemplate.postForEntity(
                    loginUrl,
                    new HttpEntity<>(loginBody, headers),
                    String.class
            );

            // Step 4: Get cookies from login response
            List<String> cookies = loginResp.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null) {
                headers.put(HttpHeaders.COOKIE, cookies);
            }

            // Step 5: Call report API (GET) with dynamic reportDate
            String reportUrl = "https://www.etimeoffice.com/DailyReport/DetailsWeb1";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(reportUrl)
                    .queryParam("reportDate", reportDate)  // <-- dynamic date here
                    .queryParam("reportName", "DP")
                    .queryParam("reportType", "EXCEL")
                    .queryParam("shortType", "By Department Wise");

            HttpEntity<Void> reportEntity = new HttpEntity<>(headers);

            ResponseEntity<String> reportResp = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    reportEntity,
                    String.class
            );

            // Step 6: Parse JSON & decode Base64
            String bodyContent = reportResp.getBody();
            if (bodyContent == null) {
                throw new RuntimeException("Report API returned empty response");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(bodyContent);
            String base64Excel = root.get("_dataStr").asText();

            // Step 7: Decode Base64 -> raw Excel bytes
            return Base64.getDecoder().decode(base64Excel);

        } catch (Exception e) {
            throw new RuntimeException("Failed to download report: " + e.getMessage(), e);
        }
    }

    private String extractToken(String html) {
        Pattern pattern = Pattern.compile("name=\"__RequestVerificationToken\".*?value=\"(.*?)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("Verification token not found in login page");
    }
}
