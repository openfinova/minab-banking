package com.openfinova.banking.exchangerate.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.api.entity.TradeDirection;

class ExchangeRateTest {

    @Test
    void getRateForDirection_buy_usesBidWhenSetOtherwiseMid() {
        BigDecimal mid = new BigDecimal("1.20000000");
        BigDecimal bid = new BigDecimal("1.19500000");
        ExchangeRate withBid = rate(mid);
        withBid.setBidRate(bid);
        ExchangeRate withoutBid = rate(mid);

        assertThat(withBid.getRateForDirection(TradeDirection.BUY)).isEqualByComparingTo(bid);
        assertThat(withoutBid.getRateForDirection(TradeDirection.BUY)).isEqualByComparingTo(mid);
    }

    @Test
    void getRateForDirection_sell_usesAskWhenSetOtherwiseMid() {
        BigDecimal mid = new BigDecimal("1.20000000");
        BigDecimal ask = new BigDecimal("1.20500000");
        ExchangeRate withAsk = rate(mid);
        withAsk.setAskRate(ask);
        ExchangeRate withoutAsk = rate(mid);

        assertThat(withAsk.getRateForDirection(TradeDirection.SELL)).isEqualByComparingTo(ask);
        assertThat(withoutAsk.getRateForDirection(TradeDirection.SELL)).isEqualByComparingTo(mid);
    }

    @Test
    void getRateForDirection_mid_isAlwaysMidRate() {
        BigDecimal mid = new BigDecimal("1.20000000");
        ExchangeRate rate = rate(mid);
        rate.setBidRate(new BigDecimal("1.19"));
        rate.setAskRate(new BigDecimal("1.21"));

        assertThat(rate.getRateForDirection(TradeDirection.MID)).isEqualByComparingTo(mid);
    }

    private static ExchangeRate rate(BigDecimal mid) {
        return new ExchangeRate("EUR", "USD", mid, LocalDate.now(), RateType.SPOT);
    }
}
