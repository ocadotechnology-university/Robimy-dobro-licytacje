package prw.edu.pl.ocadolicytacje.infrastructure.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class AuctionCsvRowDto {

    private final String moderatorFullName;
    private final LocalDateTime auctionStartDateTime;
    private final LocalDateTime auctionEndDateTime;
    private final String supplierFullName;
    private final String auctionTitle;
    private final String auctionDescription;
    private final String photoUrl;
    private final String city;
    private final BigDecimal basePrice;

    private AuctionCsvRowDto(Builder builder) {
        this.moderatorFullName = builder.moderatorFullName;
        this.auctionStartDateTime = builder.auctionStartDateTime;
        this.auctionEndDateTime = builder.auctionEndDateTime;
        this.supplierFullName = builder.supplierFullName;
        this.auctionTitle = builder.auctionTitle;
        this.auctionDescription = builder.auctionDescription;
        this.photoUrl = builder.photoUrl;
        this.city = builder.city;
        this.basePrice = builder.basePrice;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String moderatorFullName;
        private LocalDateTime auctionStartDateTime;
        private LocalDateTime auctionEndDateTime;
        private String supplierFullName;
        private String auctionTitle;
        private String auctionDescription;
        private String photoUrl;
        private String city;
        private BigDecimal basePrice;

        public Builder moderatorFullName(String name) {
            if (!isValidFullName(name)) {
                throw new IllegalArgumentException("Moderator musi mieć imię i nazwisko z dużych liter.");
            }
            this.moderatorFullName = name;
            return this;
        }

        public Builder auctionStartDateTime(LocalDateTime dateTime) {
            if (dateTime == null || dateTime.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Data aukcji musi być w przyszłości.");
            }
            this.auctionStartDateTime = dateTime;
            return this;
        }
        public Builder auctionEndDateTime(LocalDateTime dateTime){
            if (this.auctionStartDateTime == null) {
                throw new IllegalStateException("Data rozpoczęcia aukcji musi być ustawiona przed ustawieniem daty zakończenia.");
            }
            if (!dateTime.isAfter(this.auctionStartDateTime)) {
                throw new IllegalArgumentException("Data zakończenia aukcji musi być późniejsza niż data rozpoczęcia.");
            }
            this.auctionEndDateTime = dateTime;
            return this;
        }

        public Builder supplierFullName(String name) {
            if (!isValidFullName(name)) {
                throw new IllegalArgumentException("Dostawca musi mieć imię i nazwisko z dużych liter.");
            }
            this.supplierFullName = name;
            return this;
        }

        public Builder auctionTitle(String title) {
            if (title.length() > 256) {
                throw new IllegalArgumentException("Tytuł aukcji może mieć maksymalnie 256 znaków.");
            }
            this.auctionTitle = title;
            return this;
        }

        public Builder auctionDescription(String description) {
            if (description.length() > 4096) {
                throw new IllegalArgumentException("Opis aukcji może mieć maksymalnie 4096 znaków.");
            }
            this.auctionDescription = description;
            return this;
        }

        public Builder photoUrl(String url) {
            try {
                new URL(url); // weryfikacja poprawności URL
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Nieprawidłowy URL zdjęcia: " + url);
            }
            this.photoUrl = url;
            return this;
        }

        public Builder city(String city) {
            if (city.length() > 128) {
                throw new IllegalArgumentException("Miasto może mieć maksymalnie 128 znaków.");
            }
            this.city = city;
            return this;
        }

        public Builder basePrice(BigDecimal price) {
            if (price == null || price.scale() > 2 || price.precision() > 4) {
                throw new IllegalArgumentException("Cena musi być NUMERIC(4,2) – maks. 99.99.");
            }
            this.basePrice = price;
            return this;
        }

        public AuctionCsvRowDto build() {
            return new AuctionCsvRowDto(this);
        }

        private boolean isValidFullName(String name) {
            if (StringUtils.isBlank(name)) return false;

            String[] parts = StringUtils.split(name.trim());
            if (parts.length != 2) return false;

            return Character.isUpperCase(parts[0].charAt(0)) &&
                    Character.isUpperCase(parts[1].charAt(0));
        }
    }
}