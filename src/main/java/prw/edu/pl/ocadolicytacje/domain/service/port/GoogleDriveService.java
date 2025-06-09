package prw.edu.pl.ocadolicytacje.domain.service.port;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveService {

    private static final String LIST_FILES_ENDPOINT =
            "https://www.googleapis.com/drive/v3/files"
                    + "?q=%s"                                  // ← w to miejsce wstawimy *zakodowane* zapytanie
                    + "&fields=files(id,name,mimeType)"
                    + "&orderBy=name"
                    + "&pageSize=1"
                    + "&key=%s";
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${gdrive.api-key}")
    private String apiKey;

    /**
     * Zwraca ID pierwszego pliku graficznego w podanym folderze.
     * Jeśli folder pusty lub niedostępny – zwraca {@code null}.
     */
    public String getFirstImageId(String folderId) {
        try {

            // q = '<FOLDER_ID>' in parents and mimeType contains 'image/' and trashed = false
            String rawQuery = "'%s' in parents and mimeType contains 'image/' and trashed=false".formatted(folderId);
            String encoded = URLEncoder.encode(rawQuery, StandardCharsets.UTF_8);

            String url = LIST_FILES_ENDPOINT.formatted(encoded, apiKey);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("Drive API zwróciło status {}", resp.statusCode());
                return null;
            }

            JsonNode root = mapper.readTree(resp.body());
            JsonNode files = root.path("files");
            if (files.isArray() && files.size() > 0) {
                return files.get(0).path("id").asText();
            }
        } catch (Exception e) {
            log.error("Błąd przy pobieraniu pliku z Drive API", e);
        }
        return null;
    }
}
