package com.openfinova.banking.exchangerate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class EcbExchangeRateProviderTest {

    private static final String SAMPLE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01"
                             xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
              <gesmes:subject>Reference rates</gesmes:subject>
              <gesmes:Sender>
                <gesmes:name>European Central Bank</gesmes:name>
              </gesmes:Sender>
              <Cube>
                <Cube time="2024-03-15">
                  <Cube currency="USD" rate="1.0858"/>
                  <Cube currency="JPY" rate="162.34"/>
                  <Cube currency="GBP" rate="0.85420"/>
                  <Cube currency="CHF" rate="0.9620"/>
                </Cube>
              </Cube>
            </gesmes:Envelope>
            """;

    private final EcbExchangeRateProvider provider = new EcbExchangeRateProvider();

    @Test
    void parse_extractsRatesAndDate() {
        ExchangeRateProvider.ProviderRates result = provider
                .parse(SAMPLE_XML.getBytes(StandardCharsets.UTF_8), Set.of("USD", "JPY", "GBP", "CHF"));

        assertThat(result.baseCurrency()).isEqualTo("EUR");
        assertThat(result.publicationDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(result.rates()).containsEntry("USD", new BigDecimal("1.0858"))
                .containsEntry("JPY", new BigDecimal("162.34")).containsEntry("GBP", new BigDecimal("0.85420"))
                .containsEntry("CHF", new BigDecimal("0.9620"));
    }

    @Test
    void parse_filtersToRequestedCurrencies() {
        ExchangeRateProvider.ProviderRates result = provider
                .parse(SAMPLE_XML.getBytes(StandardCharsets.UTF_8), Set.of("USD", "GBP"));

        assertThat(result.rates()).hasSize(2).containsOnlyKeys("USD", "GBP");
    }

    @Test
    void parse_emptyRequestedSet_returnsAll() {
        ExchangeRateProvider.ProviderRates result = provider
                .parse(SAMPLE_XML.getBytes(StandardCharsets.UTF_8), Set.of());

        assertThat(result.rates()).hasSize(4);
    }

    @Test
    void parse_throwsWhenNoRatesMatch() {
        assertThatThrownBy(() -> provider.parse(SAMPLE_XML.getBytes(StandardCharsets.UTF_8), Set.of("AED")))
                .isInstanceOf(ExchangeRateProviderException.class).hasMessageContaining("no usable rates");
    }

    @Test
    void parse_rejectsMalformedXml() {
        assertThatThrownBy(() -> provider.parse("<not really xml".getBytes(StandardCharsets.UTF_8), Set.of("USD")))
                .isInstanceOf(ExchangeRateProviderException.class).hasMessageContaining("parse");
    }

    @Test
    void fetchLatestRates_rejectsNonEurBase() {
        assertThatThrownBy(() -> provider.fetchLatestRates("USD", Set.of("EUR")))
                .isInstanceOf(ExchangeRateProviderException.class).hasMessageContaining("EUR");
    }
}
