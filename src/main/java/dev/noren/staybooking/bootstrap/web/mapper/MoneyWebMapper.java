package dev.noren.staybooking.bootstrap.web.mapper;

import dev.noren.staybooking.bootstrap.domain.FieldChange;
import dev.noren.staybooking.bootstrap.domain.Money;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MoneyWebMapper {
    public Money toMoney(BigDecimal amount) {
        return amount == null ? null : Money.of(amount);
    }

    public BigDecimal toAmount(Money money) {
        return money == null ? null : money.amount();
    }

    public FieldChange<Money> toMoneyChange(JsonNullable<BigDecimal> field) {
        if (field == null || !field.isPresent()) {
            return new FieldChange<>(false, null);
        }
        return new FieldChange<>(true, toMoney(field.orElse(null)));
    }
}
