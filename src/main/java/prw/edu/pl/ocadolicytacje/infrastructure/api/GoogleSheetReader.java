package prw.edu.pl.ocadolicytacje.infrastructure.api;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleSheetReader {
    public static final String APPLICATION_NAME = "Parser";
    private static final String SPREADSHEET_ID = "1fsnPVw5kaNxST75Sh-LW46G6Y2ZCZMBMZVPN12QEytQ";
    private static final String RANGE = "Arkusz1";

    public List<List<Object>> getCsvData() throws GeneralSecurityException, IOException {
            Sheets sheets = createSheetsService();
            ValueRange response = sheets.spreadsheets().values()
                    .get(SPREADSHEET_ID, RANGE)
                    .execute();
            return response.getValues();
    }

    private static Sheets createSheetsService() throws IOException, GeneralSecurityException {
        FileInputStream serviceAccountStream = new FileInputStream("src/main/resources/credentials.json");

        var credentials = ServiceAccountCredentials
                .fromStream(serviceAccountStream)
                .createScoped(List.of(SheetsScopes.SPREADSHEETS_READONLY));

        return new Sheets.Builder(
                com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport(),
                com.google.api.client.json.jackson2.JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        )
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
