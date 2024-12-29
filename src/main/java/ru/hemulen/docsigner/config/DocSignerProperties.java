package ru.hemulen.docsigner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="docsigner")
@Data
public class DocSignerProperties {
    private String isMnemonic;
    private String containerAlias;
    private String containerPassword;
    private String adapterOutPath;
    private String attachmentOutPath;
    private String attachmentInPath;
    private String esiaRoutingCode;
    private String esiaMnemonic; // PROD - MNSV61, UAT - MNSV61, MNSV62
    private String backLink;
    private String dbURL;
    private String dbUsename;
    private String dbPassword;

}
