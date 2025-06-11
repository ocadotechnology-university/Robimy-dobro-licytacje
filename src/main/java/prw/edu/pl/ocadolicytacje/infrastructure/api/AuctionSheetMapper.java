package prw.edu.pl.ocadolicytacje.infrastructure.api;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuctionSheetMapper {

    public static final String MODERATOR_AUKCJI = "Moderator aukcji";
    public static final String ZAKOŃCZENIE_AUKCJI = "Zakończenie aukcji";
    public static final String PREFEROWANY_DZIEŃ_AUKCJI = "Preferowany dzień aukcji";
    public static final String DOSTAWCA_WŁAŚCICIEL_PRZEDMIOTU_USŁUGI_OFERTY = "Dostawca (właściciel przedmiotu/usługi/oferty)";
    public static final String PRZEDMIOT_TYTUŁ_AUKCJ = "Przedmiot / tytuł aukcji";
    public static final String OPIS_AUKCJI = "Opis aukcji";
    public static final String URL_ZDJĘCIA = "URL zdjęcia";
    public static final String MIASTO = "Miasto";
    public static final String CENA_WYWOŁAWCZA = "Cena wywoławcza";

    public List<AuctionCsvRowDto> map(List<List<Object>> rows) {
        if (Objects.isNull(rows) || rows.isEmpty()) {
            return List.of();
        }

        List<String> headers = rows.get(0).stream()
                .map(Objects::toString)
                .map(String::trim)
                .collect(Collectors.toList());

        List<AuctionCsvRowDto> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);

            Map<String, String> rowMap = new HashMap<>();
            for (int col = 0; col < headers.size() && col < row.size(); col++) {
                rowMap.put(headers.get(col), row.get(col).toString().trim());
            }
            AuctionCsvRowDto dto = AuctionCsvRowDto.builder()
                    .moderatorFullName(rowMap.get(MODERATOR_AUKCJI))
                    .auctionStartDateTime(parsePreferredDate(rowMap.get(PREFEROWANY_DZIEŃ_AUKCJI)))
                    .auctionEndDateTime(parsePreferredDate(rowMap.get(ZAKOŃCZENIE_AUKCJI)))
                    .supplierFullName(rowMap.get(DOSTAWCA_WŁAŚCICIEL_PRZEDMIOTU_USŁUGI_OFERTY))
                    .auctionTitle(rowMap.get(PRZEDMIOT_TYTUŁ_AUKCJ))
                    .auctionDescription(rowMap.get(OPIS_AUKCJI))
                    .photoUrl(rowMap.get(URL_ZDJĘCIA))
                    .city(rowMap.get(MIASTO))
                    .basePrice(parsePrice(rowMap.get(CENA_WYWOŁAWCZA)))
                    .build();
            System.out.println(

            );
            result.add(dto);
        }
        return result;
    }

    private static BigDecimal parsePrice(String priceStr) {
        try {
            return new BigDecimal(priceStr.replace(",", "."));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static LocalDateTime parsePreferredDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return LocalDateTime.parse(dateStr.trim(), formatter);
        } catch (Exception e) {
            throw new RuntimeException("Niepoprawny format daty i godziny: " + dateStr);
        }
    }
}
