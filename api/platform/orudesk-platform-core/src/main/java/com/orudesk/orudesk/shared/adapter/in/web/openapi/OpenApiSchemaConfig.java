package com.orudesk.orudesk.shared.adapter.in.web.openapi;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Only in apps that publish an OpenAPI contract, i.e. that have springdoc (and with it, swagger-core). */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "io.swagger.v3.core.converter.ModelConverter")
class OpenApiSchemaConfig {

    @Bean
    RecordNullabilityModelConverter recordNullabilityModelConverter() {
        return new RecordNullabilityModelConverter();
    }
}
