package com.elingo.premium.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.premium")
@Getter
@Setter
public class PremiumProperties {
    private BankTransfer bankTransfer = new BankTransfer();
    private Sepay sepay = new Sepay();

    @Getter
    @Setter
    public static class BankTransfer {
        private String codePrefix = "ELGO";
        private String bankName = "MBBank";
        private String bankAccount = "";
        private String bankAccountName = "";
        private String qrTemplate = "compact";
    }

    @Getter
    @Setter
    public static class Sepay {
        private String secretKey = "";
    }
}
