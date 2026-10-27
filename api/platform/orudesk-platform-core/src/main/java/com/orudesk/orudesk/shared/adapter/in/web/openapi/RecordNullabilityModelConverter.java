package com.orudesk.orudesk.shared.adapter.in.web.openapi;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.RecordComponent;
import java.util.Iterator;
import java.util.Map;

/**
 * Makes the OpenAPI contract say which fields can be missing, using the same rule as the Java code: a record
 * component is always there, unless it's marked {@code @Nullable}.
 * <p>
 * Without this, springdoc marks a field required only when it has {@code @NotNull} or {@code @NotBlank}, so every
 * field of every response is optional, and the generated TypeScript makes the web app check {@code person.id}
 * for undefined everywhere. With it, {@code PersonDetails.id} is {@code string} and {@code PersonDetails.phone} is
 * {@code string | null}, which is the truth.
 * <p>
 * springdoc picks up every {@link ModelConverter} bean and asks it, in a chain, to describe each Java type.
 */
public class RecordNullabilityModelConverter implements ModelConverter {

    @Override
    public @Nullable Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (schema == null) {
            return null;
        }
        JavaType javaType = Json.mapper().constructType(type.getType());
        Class<?> recordClass = javaType.getRawClass();
        if (!recordClass.isRecord()) {
            return schema;
        }
        Schema<?> model = definition(schema, context);
        if (model == null || model.getProperties() == null) {
            return schema;
        }
        for (RecordComponent component : recordClass.getRecordComponents()) {
            Schema<?> property = model.getProperties().get(component.getName());
            if (property == null) {
                continue;
            }
            if (isNullable(component)) {
                if (property.get$ref() == null && property.getTypes() != null) {
                    property.addType("null");   // OpenAPI 3.1: "type": ["string", "null"]
                }
            } else if (model.getRequired() == null || !model.getRequired().contains(component.getName())) {
                model.addRequiredItem(component.getName());
            }
        }
        return schema;
    }

    /** The chain returns either the model itself, or a {@code $ref} to it that the context has already defined. */
    private static @Nullable Schema<?> definition(Schema<?> schema, ModelConverterContext context) {
        if (schema.get$ref() == null) {
            return schema;
        }
        String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
        Map<String, Schema> defined = context.getDefinedModels();
        return defined.get(name);
    }

    private static boolean isNullable(RecordComponent component) {
        // JSpecify's @Nullable annotates the type (TYPE_USE), so it sits on the component's annotated type
        return component.getAnnotatedType().isAnnotationPresent(Nullable.class);
    }
}
