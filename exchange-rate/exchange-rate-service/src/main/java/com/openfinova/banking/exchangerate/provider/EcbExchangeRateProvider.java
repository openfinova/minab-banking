package com.openfinova.banking.exchangerate.provider;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * European Central Bank reference rates provider.
 *
 * <p>Source: <a href="https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml">
 * eurofxref-daily.xml</a> — published each TARGET business day around 16:00 CET, EUR-base mid rates.
 * Free, no key, no attribution required, ~30 currencies.
 *
 * <p>The endpoint only publishes rates with EUR as the base, so this provider refuses any other base
 * currency rather than silently producing wrong rates.
 */
@Component
@ConditionalOnProperty(name = "app.exchange-rate.provider", havingValue = "ecb", matchIfMissing = true)
public class EcbExchangeRateProvider implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(EcbExchangeRateProvider.class);

    private static final String PROVIDER_ID = "ecb";
    private static final String ENDPOINT = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
    private static final String EUR = "EUR";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;

    public EcbExchangeRateProvider() {
        this(HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build());
    }

    /** Test-only constructor allowing a custom HTTP client. */
    EcbExchangeRateProvider(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ProviderRates fetchLatestRates(String baseCurrency, Set<String> targetCurrencies) {
        if (!EUR.equalsIgnoreCase(baseCurrency)) {
            throw new ExchangeRateProviderException(
                    "ECB provider only supports EUR as base currency, got: " + baseCurrency);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT)).timeout(HTTP_TIMEOUT)
                .header("Accept", "application/xml").GET().build();

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (java.io.IOException e) {
            throw new ExchangeRateProviderException("ECB request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateProviderException("ECB request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new ExchangeRateProviderException("ECB returned HTTP " + response.statusCode());
        }

        return parse(response.body(), targetCurrencies);
    }

    /**
     * Parses the ECB eurofxref-daily.xml payload. Public for testing.
     */
    ProviderRates parse(byte[] xml, Set<String> targetCurrencies) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Harden against XXE — we never expect external entities here.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc;
            try (ByteArrayInputStream in = new ByteArrayInputStream(xml)) {
                doc = builder.parse(in);
            }

            NodeList timeCubes = doc.getElementsByTagNameNS("*", "Cube");
            Element dateCube = null;
            for (int i = 0; i < timeCubes.getLength(); i++) {
                Element el = (Element) timeCubes.item(i);
                if (el.hasAttribute("time")) {
                    dateCube = el;
                    break;
                }
            }
            if (dateCube == null) {
                throw new ExchangeRateProviderException("ECB response missing <Cube time=...> element");
            }

            LocalDate publicationDate;
            try {
                publicationDate = LocalDate.parse(dateCube.getAttribute("time"));
            } catch (DateTimeParseException e) {
                throw new ExchangeRateProviderException(
                        "ECB response has unparseable date: " + dateCube.getAttribute("time"),
                        e);
            }

            Map<String, BigDecimal> rates = new HashMap<>();
            NodeList currencyCubes = dateCube.getElementsByTagNameNS("*", "Cube");
            for (int i = 0; i < currencyCubes.getLength(); i++) {
                Element el = (Element) currencyCubes.item(i);
                String currency = el.getAttribute("currency");
                String rateStr = el.getAttribute("rate");
                if (currency.isEmpty() || rateStr.isEmpty()) {
                    continue;
                }
                if (targetCurrencies != null && !targetCurrencies.isEmpty() && !targetCurrencies.contains(currency)) {
                    continue;
                }
                try {
                    rates.put(currency, new BigDecimal(rateStr));
                } catch (NumberFormatException e) {
                    log.warn("Skipping ECB rate for {} with unparseable value '{}'", currency, rateStr);
                }
            }

            if (rates.isEmpty()) {
                throw new ExchangeRateProviderException(
                        "ECB response contained no usable rates for requested currencies");
            }

            return new ProviderRates(EUR, publicationDate, rates);
        } catch (ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException e) {
            throw new ExchangeRateProviderException("Failed to parse ECB XML response", e);
        }
    }
}
