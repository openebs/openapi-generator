/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 // Based of the RustClientCodegen.java with a few modifications

package org.openapitools.codegen.languages;

import com.google.common.base.Strings;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.openapitools.codegen.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.openapitools.codegen.utils.StringUtils.camelize;
import static org.openapitools.codegen.utils.StringUtils.underscore;

public class RustMayastorCodegen extends DefaultCodegen implements CodegenConfig {
    private final Logger LOGGER = LoggerFactory.getLogger(RustMayastorCodegen.class);
    private boolean useSingleRequestParameter = false;
    private boolean supportMultipleResponses = false;
    private String actixWebVersion = "4.1.0";
    private String actixWebFeatures = "\"rustls\"";
    private String actixWebTelemetryVersion = "\"0.12.0\"";
    private boolean actixWebBeta = false;

    public static final String PACKAGE_NAME = "packageName";
    public static final String PACKAGE_VERSION = "packageVersion";
    public static final String SUPPORT_MULTIPLE_RESPONSES = "supportMultipleResponses";
    public static final String ACTIX_WEB_VERSION = "actixWebVersion";
    public static final String ACTIX_WEB4_BETA = "actixWeb4Beta";
    public static final String ACTIX_WEB_FEATURES = "actixWebFeatures";
    public static final String ACTIX_WEB_TELEMETRY_VERSION = "actixWebTelemetryVersion";
    private static final String NO_FORMAT = "%%NO_FORMAT";


    protected String packageName = "openapi";
    protected String packageVersion = "0.3.0";
    protected String apiDocPath = "docs/apis/";
    protected String modelDocPath = "docs/models/";
    protected String apiFolder = "src/apis";
    protected String modelFolder = "src/models";
    protected String enumSuffix = ""; // default to empty string for backward compatibility

    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    public String getName() {
        return "rust-mayastor";
    }

    public String getHelp() {
        return "Generates a Rust Bindings library (beta).";
    }

    public RustMayastorCodegen() {
        super();

        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .wireFormatFeatures(EnumSet.of(WireFormatFeature.JSON, WireFormatFeature.XML, WireFormatFeature.Custom))
                .securityFeatures(EnumSet.of(
                        SecurityFeature.BasicAuth,
                        SecurityFeature.ApiKey,
                        SecurityFeature.BearerToken,
                        SecurityFeature.OAuth2_Implicit
                ))
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling
                )
                .excludeSchemaSupportFeatures(
                        SchemaSupportFeature.Polymorphism
                )
                .excludeParameterFeatures(
                        ParameterFeature.Cookie
                )
                .includeClientModificationFeatures(
                        ClientModificationFeature.BasePath,
                        ClientModificationFeature.UserAgent
                )
        );

        outputFolder = "generated-code/rust-mayastor";
        modelTemplateFiles.put("model.mustache", ".rs");

        modelDocTemplateFiles.put("model_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");

        // default HIDE_GENERATION_TIMESTAMP to true
        hideGenerationTimestamp = Boolean.TRUE;

        embeddedTemplateDir = templateDir = "rust-mayastor";

        setReservedWordsLowerCase(
                Arrays.asList(
                        "abstract", "alignof", "as", "become", "box",
                        "break", "const", "continue", "crate", "do",
                        "else", "enum", "extern", "false", "final",
                        "fn", "for", "if", "impl", "in",
                        "let", "loop", "macro", "match", "mod",
                        "move", "mut", "offsetof", "override", "priv",
                        "proc", "pub", "pure", "ref", "return",
                        "Self", "self", "sizeof", "static", "struct",
                        "super", "trait", "true", "type", "typeof",
                        "unsafe", "unsized", "use", "virtual", "where",
                        "while", "yield", "async", "await", "dyn", "try"
                )
        );

        defaultIncludes = new HashSet<String>(
                Arrays.asList(
                        "map",
                        "array")
        );

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "i8", "i16", "i32", "i64",
                        "u8", "u16", "u32", "u64",
                        "f32", "f64", "isize", "usize",
                        "char", "bool", "str", "String")
        );

        instantiationTypes.clear();

        typeMapping.clear();
        typeMapping.put("integer", "i32");
        typeMapping.put("long", "i64");
        typeMapping.put("number", "f32");
        typeMapping.put("float", "f32");
        typeMapping.put("double", "f64");
        typeMapping.put("boolean", "bool");
        typeMapping.put("string", "String");
        typeMapping.put("UUID", "uuid::Uuid");
        typeMapping.put("URI", "url::Url");
        typeMapping.put("date", "string");
        typeMapping.put("DateTime", "String");
        typeMapping.put("password", "String");
        // TODO(bcourtine): review file mapping.
        // I tried to map as "std::io::File", but Reqwest multipart file requires a "AsRef<Path>" param.
        // Getting a file from a Path is simple, but the opposite is difficult. So I map as "std::path::Path".
        typeMapping.put("file", "std::path::PathBuf");
        typeMapping.put("binary", "crate::models::File");
        typeMapping.put("ByteArray", "String");
        typeMapping.put("object", "serde_json::Value");
        typeMapping.put("AnyType", "serde_json::Value");

        cliOptions.clear();
        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_NAME, "Rust package name (convention: lowercase).")
                .defaultValue("openapi"));
        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_VERSION, "Rust package version.")
                .defaultValue("1.1.0"));
        cliOptions.add(new CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP, CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC)
                .defaultValue(Boolean.TRUE.toString()));
        cliOptions.add(new CliOption(CodegenConstants.USE_SINGLE_REQUEST_PARAMETER, CodegenConstants.USE_SINGLE_REQUEST_PARAMETER_DESC, SchemaTypeUtil.BOOLEAN_TYPE)
                .defaultValue(Boolean.FALSE.toString()));
                
        cliOptions.add(new CliOption(ACTIX_WEB_VERSION, "Actix Web Dependency version used by Cargo.toml")
                .defaultValue(getActixWebVersion()));
        cliOptions.add(new CliOption(ACTIX_WEB_FEATURES, "Actix Web Dependency features used by Cargo.toml")
                .defaultValue(getActixWebFeatures()));
        cliOptions.add(new CliOption(ACTIX_WEB_TELEMETRY_VERSION, "Actix Web OpenTelemetry version used by Cargo.toml")
                .defaultValue(getActixWebFeatures()));
        cliOptions.add(new CliOption(ACTIX_WEB4_BETA, "Actix Web 4 Beta trait", SchemaTypeUtil.BOOLEAN_TYPE)
                .defaultValue(Boolean.FALSE.toString()));

        cliOptions.add(new CliOption(SUPPORT_MULTIPLE_RESPONSES, "If set, return type wraps an enum of all possible 2xx schemas. This option is for 'reqwest' library only", SchemaTypeUtil.BOOLEAN_TYPE)
            .defaultValue(Boolean.FALSE.toString()));
        cliOptions.add(new CliOption(CodegenConstants.ENUM_NAME_SUFFIX, CodegenConstants.ENUM_NAME_SUFFIX_DESC).defaultValue(this.enumSuffix));
    }

    @Override
    public ModelsMap postProcessModels(ModelsMap objs) {
        // process enum in models
        return postProcessModelsEnum(objs);
    }

    @SuppressWarnings({"static-method", "unchecked"})
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        // Index all CodegenModels by model name.
        Map<String, CodegenModel> allModels = new HashMap<>();

        for (Map.Entry<String, ModelsMap> entry : objs.entrySet()) {
            String modelName = toModelName(entry.getKey());
            List<ModelMap> models = entry.getValue().getModels();
            for (ModelMap mo : models) {
                CodegenModel cm = mo.getModel();
                allModels.put(modelName, cm);
            }
        }

        for (Map.Entry<String, ModelsMap> entry : objs.entrySet()) {
            List<ModelMap> models = entry.getValue().getModels();
            for (ModelMap mo : models) {
                CodegenModel cm = mo.getModel();
                if (cm.discriminator != null) {
                    List<Object> discriminatorVars = new ArrayList<>();
                    for (CodegenDiscriminator.MappedModel mappedModel : cm.discriminator.getMappedModels()) {
                        CodegenModel model = allModels.get(mappedModel.getModelName());
                        Map<String, Object> mas = new HashMap<>();
                        mas.put("modelName", camelize(mappedModel.getModelName()));
                        mas.put("mappingName", mappedModel.getMappingName());

                        // TODO: deleting the variable from the array was
                        // problematic; I don't know what this is supposed to do
                        // so I'm just cloning it for the moment
                        List<CodegenProperty> vars = new ArrayList<>(model.getVars());
                        vars.removeIf(p -> p.name.equals(cm.discriminator.getPropertyName()));
                        mas.put("vars", vars);
                        discriminatorVars.add(mas);
                    }
                    // TODO: figure out how to properly have the original property type that didn't go through toVarName
                    String vendorExtensionTagName = cm.discriminator.getPropertyName().replace("_", "");
                    cm.vendorExtensions.put("x-tag-name", vendorExtensionTagName);
                    cm.vendorExtensions.put("x-mapped-models", discriminatorVars);
                }
            }
        }
        return objs;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(CodegenConstants.ENUM_NAME_SUFFIX)) {
            enumSuffix = additionalProperties.get(CodegenConstants.ENUM_NAME_SUFFIX).toString();
        }

        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_NAME)) {
            setPackageName((String) additionalProperties.get(CodegenConstants.PACKAGE_NAME));
        } else {
            setPackageName("openapi");
        }

        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_VERSION)) {
            setPackageVersion((String) additionalProperties.get(CodegenConstants.PACKAGE_VERSION));
        } else {
            setPackageVersion("2.0.0");
        }

        if (additionalProperties.containsKey(ACTIX_WEB_VERSION)) {
            this.setActixWebVersion((String) additionalProperties.get(ACTIX_WEB_VERSION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(ACTIX_WEB_VERSION, getActixWebVersion());
        }
        if (additionalProperties.containsKey(ACTIX_WEB_FEATURES)) {
            this.setActixWebFeatures((String) additionalProperties.get(ACTIX_WEB_FEATURES));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(ACTIX_WEB_FEATURES, getActixWebFeatures());
        }
        if (additionalProperties.containsKey(ACTIX_WEB_TELEMETRY_VERSION)) {
            this.setActixWebTelemetryVersion((String) additionalProperties.get(ACTIX_WEB_TELEMETRY_VERSION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(ACTIX_WEB_TELEMETRY_VERSION, getActixWebTelemetryVersion());
        }

        if (additionalProperties.containsKey(ACTIX_WEB4_BETA)) {
            this.setActixWebBeta(convertPropertyToBoolean(ACTIX_WEB4_BETA));
        }
        writePropertyBack(ACTIX_WEB4_BETA, getActixWebBeta());

        if (additionalProperties.containsKey(CodegenConstants.USE_SINGLE_REQUEST_PARAMETER)) {
            this.setUseSingleRequestParameter(convertPropertyToBoolean(CodegenConstants.USE_SINGLE_REQUEST_PARAMETER));
        }
        writePropertyBack(CodegenConstants.USE_SINGLE_REQUEST_PARAMETER, getUseSingleRequestParameter());

        if (additionalProperties.containsKey(SUPPORT_MULTIPLE_RESPONSES)) {
            this.setSupportMultipleReturns(convertPropertyToBoolean(SUPPORT_MULTIPLE_RESPONSES));
        }
        writePropertyBack(SUPPORT_MULTIPLE_RESPONSES, getSupportMultipleReturns());

        // the option doesn't seem to be parsed correctly by the cmdline args - empty means default!??
        // so just set this to empty in the generator
        setApiNameSuffix("");

        additionalProperties.put(CodegenConstants.PACKAGE_NAME, packageName);
        additionalProperties.put(CodegenConstants.PACKAGE_VERSION, packageVersion);

        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);
        apiTemplateFiles.put("mod.mustache", "/mod.rs");
        supportingFiles.add(new SupportingFile("mod_clients.mustache", "src/clients", "mod.rs"));

        // Actix
        apiTemplateFiles.put("actix/server/api.mustache", "/actix/server/mod.rs");
        apiTemplateFiles.put("actix/server/handlers.mustache", "/actix/server/handlers.rs");
        apiTemplateFiles.put("actix/mod.mustache", "/actix/mod.rs");
        apiTemplateFiles.put("actix/client/api_clients.mustache", "/actix/client/mod.rs");
        supportingFiles.add(new SupportingFile("actix/server/api_mod.mustache", apiFolder, "actix_server.rs"));
        supportingFiles.add(new SupportingFile("actix/client/configuration.mustache", "src/clients/actix", "configuration.rs"));
        supportingFiles.add(new SupportingFile("actix/client/client.mustache", "src/clients/actix", "mod.rs"));

        // Tower - Hyper
        apiTemplateFiles.put("tower-hyper/mod.mustache", "/tower/mod.rs");
        apiTemplateFiles.put("tower-hyper/client/api_clients.mustache", "/tower/client/mod.rs");
        supportingFiles.add(new SupportingFile("tower-hyper/client/configuration.mustache", "src/clients/tower", "configuration.rs"));
        supportingFiles.add(new SupportingFile("tower-hyper/client/client.mustache", "src/clients/tower", "mod.rs"));
        supportingFiles.add(new SupportingFile("tower-hyper/client/body.mustache", "src/clients/tower", "body.rs"));
        supportingFiles.add(new SupportingFile("examples/tower-client-main.mustache", "examples/clients/tower", "main.rs"));
        

        modelPackage = packageName;
        apiPackage = packageName;

        supportingFiles.add(new SupportingFile("openapi.mustache", "api", "openapi.yaml"));
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
        supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("api_mod.mustache", apiFolder, "mod.rs"));
        supportingFiles.add(new SupportingFile("model_mod.mustache", modelFolder, "mod.rs"));
        supportingFiles.add(new SupportingFile("lib.mustache", "src", "lib.rs"));
        supportingFiles.add(new SupportingFile("Cargo.mustache", "", "Cargo.toml"));
    }

    public String getActixWebVersion() {
        return actixWebVersion;
    }

    public void setActixWebVersion(String actixWebVersion) {
        this.actixWebVersion = actixWebVersion;
    }

    public String getActixWebFeatures() {
        return actixWebFeatures;
    }

    public void setActixWebFeatures(String actixWebFeatures) {
        this.actixWebFeatures = actixWebFeatures;
    }

    public String getActixWebTelemetryVersion() {
        return actixWebTelemetryVersion;
    }

    public void setActixWebTelemetryVersion(String actixWebTelemetryVersion) {
        this.actixWebTelemetryVersion = actixWebTelemetryVersion;
    }

    public boolean getActixWebBeta() {
        return actixWebBeta;
    }

    public void setActixWebBeta(boolean actixWebBeta) {
        this.actixWebBeta = actixWebBeta;
    }

    public boolean getSupportMultipleReturns() {
        return supportMultipleResponses;
    }

    public void setSupportMultipleReturns(boolean supportMultipleResponses) {
        this.supportMultipleResponses = supportMultipleResponses;
    }

    private boolean getUseSingleRequestParameter() {
        return useSingleRequestParameter;
    }

    private void setUseSingleRequestParameter(boolean useSingleRequestParameter) {
        this.useSingleRequestParameter = useSingleRequestParameter;
    }

    @Override
    public String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return '_' + name;
    }

    @Override
    public String apiFileFolder() {
        return (outputFolder + File.separator + apiFolder).replace("/", File.separator);
    }

    public String modelFileFolder() {
        return (outputFolder + File.separator + modelFolder).replace("/", File.separator);
    }

    @Override
    public String toVarName(String name) {
        // replace - with _ e.g. created-at => created_at
        name = sanitizeName(name.replaceAll("-", "_"));

        // if it's all uppper case, do nothing
        if (name.matches("^[A-Z_]*$"))
            return name;

        // snake_case, e.g. PetId => pet_id
        name = underscore(name);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name))
            name = escapeReservedWord(name);

        // for reserved word or word starting with number, append _
        if (name.matches("^\\d.*"))
            name = "var_" + name;

        return name;
    }

    @Override
    public String toParamName(String name) {
        return toVarName(name);
    }

    @Override
    public String toModelName(String name) {
        // camelize the model name
        // phone_number => PhoneNumber
        return camelize(toModelFilename(name));
    }

    @Override
    public String toModelFilename(String name) {

        if (!Strings.isNullOrEmpty(modelNamePrefix)) {
            name = modelNamePrefix + "_" + name;
        }

        if (!Strings.isNullOrEmpty(modelNameSuffix)) {
            name = name + "_" + modelNameSuffix;
        }

        name = sanitizeName(name);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            LOGGER.warn("{} (reserved word) cannot be used as model name. Renamed to {}", name, "model_" + name);
            name = "model_" + name; // e.g. return => ModelReturn (after camelize)
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            LOGGER.warn("{} (model name starts with number) cannot be used as model name. Renamed to {}", name,
                    "model_" + name);
            name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
        }

        return underscore(name);
    }

    @Override
    public String toApiFilename(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // e.g. PetApi.rs => pet_api.rs
        return underscore(name) + "_api";
    }

    @Override
    public String apiDocFileFolder() {
        return (outputFolder + "/" + apiDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String modelDocFileFolder() {
        return (outputFolder + "/" + modelDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String toModelDocFilename(String name) {
        return toModelName(name);
    }

    @Override
    public String toApiDocFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        if (ModelUtils.isArraySchema(p)) {
            ArraySchema ap = (ArraySchema) p;
            Schema inner = ap.getItems();
            if (inner == null) {
                LOGGER.warn("{}(array property) does not have a proper inner type defined.Default to string",
                        ap.getName());
                inner = new StringSchema().description("TODO default missing array inner type to string");
            }
            return "Vec<" + getTypeDeclaration(inner) + ">";
        } else if (ModelUtils.isMapSchema(p)) {
            Schema inner = getAdditionalProperties(p);
            if (inner == null) {
                LOGGER.warn("{}(map property) does not have a proper inner type defined. Default to string", p.getName());
                inner = new StringSchema().description("TODO default missing map inner type to string");
            }
            return "::std::collections::HashMap<String, " + getTypeDeclaration(inner) + ">";
        }

        // Not using the supertype invocation, because we want to UpperCamelize
        // the type.
        String schemaType = getSchemaType(p);
        if (typeMapping.containsKey(schemaType)) {
            return typeMapping.get(schemaType);
        }

        if (typeMapping.containsValue(schemaType)) {
            return schemaType;
        }

        if (languageSpecificPrimitives.contains(schemaType)) {
            return schemaType;
        }

        // return fully-qualified model name
        // crate::models::{{classnameFile}}::{{classname}}
        return "crate::models::" + toModelName(schemaType);
    }

    @Override
    public String getSchemaType(Schema schema) {
        String schemaType = super.getSchemaType(schema);
        schemaType = matchingIntType(schemaType, schema);
        if (typeMapping.containsKey(schemaType)) {
            return typeMapping.get(schemaType);
        }
        return schemaType;
    }

    private long requiredBits(Long bound, boolean unsigned) {
        if (bound == null) return 0;

        if (unsigned) {
            if (bound < 0) {
                throw new RuntimeException("Unsigned bound is negative: " + bound);
            }
            return 65L - Long.numberOfLeadingZeros(bound >> 1);
        }

        return 65L - Long.numberOfLeadingZeros(
                // signed bounds go from (-n) to (n - 1), i.e. i8 goes from -128 to 127
                bound < 0 ? Math.abs(bound) - 1 : bound);
    }

    private String matchingIntType(String schemaType, Schema schema) {
        if ("integer".equals(schemaType) || "long".equals(schemaType)) {
            // match int type to schema constraints
            Long inclusiveMinimum = schema.getMinimum() != null ? schema.getMinimum().longValue() : null;
            boolean exclusiveMinimum = schema.getExclusiveMinimum() != null ? schema.getExclusiveMinimum() : false;
            if (inclusiveMinimum != null && exclusiveMinimum) {
                inclusiveMinimum++;
            }

            // a signed int is required unless a minimum greater than zero is set
            boolean unsigned = inclusiveMinimum != null && inclusiveMinimum >= 0;

            Long inclusiveMaximum = schema.getMaximum() != null ? schema.getMaximum().longValue() : null;
            boolean exclusiveMaximum = schema.getExclusiveMaximum() != null ? schema.getExclusiveMaximum() : false;
            if (inclusiveMaximum != null && exclusiveMaximum) {
                inclusiveMaximum--;
            }

            switch (schema.getFormat() == null ? NO_FORMAT : schema.getFormat()) {
                // standard swagger formats
                case "int32":
                    return unsigned ? "u32" : "i32";

                case "int64":
                    return unsigned ? "u64" : "i64";

                case NO_FORMAT:
                    return matchingNonStandardIntType(NO_FORMAT, unsigned, inclusiveMinimum, inclusiveMaximum);

                default:
                    // non-standard format, use ranges to figure out the type
                    return matchingNonStandardIntType(schema.getFormat(), unsigned, inclusiveMinimum, inclusiveMaximum);
            }
        } else {
            return schemaType;
        }
    }

    private String matchingNonStandardIntType(String format, boolean unsigned, Long inclusiveMin, Long inclusiveMax) {
        long requiredMinBits = requiredBits(inclusiveMin, unsigned);
        long requiredMaxBits = requiredBits(inclusiveMax, unsigned);
        long requiredBits = Math.max(requiredMinBits, requiredMaxBits);

        if (requiredMaxBits == 0) {
            switch (format) {
                case NO_FORMAT:
                    requiredMaxBits = 0;
                case "uint8":
                    requiredMaxBits = 8;
                case "int8":
                    requiredMaxBits = 7;
                case "uint16":
                    requiredMaxBits = 16;
                case "int16":
                    requiredMaxBits = 15;
                case "uint32":
                    requiredMaxBits = 32;
                case "int32":
                    requiredMaxBits = 31;
                case "uint64":
                    requiredMaxBits = 64;
                case "int64":
                    requiredMaxBits = 63;
                default:
                    requiredMaxBits = 0;
            };
        }

        if (requiredMaxBits == 0 && requiredMinBits <= 16) {
            // rust 'size' types are arch-specific and thus somewhat loose
            // so they are used when no format or maximum are specified
            // and as long as minimum stays within plausible smallest ptr size (16 bits)
            // this way all rust types are obtainable without defining custom formats
            // this behavior (default int size) could also follow a generator flag
            return unsigned ? "usize" : "isize";

        } else if (requiredBits <= 8) {
            return unsigned ? "u8" : "i8";

        } else if (requiredBits <= 16) {
            return unsigned ? "u16" : "i16";

        } else if (requiredBits <= 32) {
            return unsigned ? "u32" : "i32";
        }
        return unsigned ? "u64" : "i64";
    }

    @Override
    public String toOperationId(String operationId) {
        String sanitizedOperationId = sanitizeName(operationId);

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(sanitizedOperationId)) {
            LOGGER.warn("{} (reserved word) cannot be used as method name. Renamed to {}", operationId, StringUtils.underscore("call_" + operationId));
            sanitizedOperationId = "call_" + sanitizedOperationId;
        }

        return StringUtils.underscore(sanitizedOperationId);
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        OperationMap operations = objs.getOperations();
        List<CodegenOperation> operationList = operations.getOperation();
        for (CodegenOperation operation : operationList) {           
            operation.vendorExtensions.put("x-httpMethodLower", operation.httpMethod.toLowerCase(Locale.ROOT));
            operation.vendorExtensions.put("x-httpMethodUpper", operation.httpMethod.toUpperCase(Locale.ROOT));
            String path = operation.path;
            
            operation.httpMethod = StringUtils.camelize(operation.httpMethod.toLowerCase(Locale.ROOT));
            for (CodegenParameter param : operation.pathParams) {
                if ("String".equals(param.dataType) && "url".equals(param.dataFormat)) {
                    path = path.replaceAll(String.format(Locale.ROOT, "\\{%s\\}", param.baseName), String.format(Locale.ROOT, "\\{%s:.*\\}", param.baseName));
                    operation.vendorExtensions.put("x-actix-query-string", true);
                }
            }

            operation.vendorExtensions.put("x-actixPath", path);

            // add support for single request parameter using x-group-parameters
            if (!operation.vendorExtensions.containsKey("x-group-parameters") && useSingleRequestParameter) {
                operation.vendorExtensions.put("x-group-parameters", Boolean.TRUE);
            }
        }

        return objs;
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        generateYAMLSpecFile(objs);
        return super.postProcessSupportingFileData(objs);
    }

    @Override
    protected boolean needToImport(String type) {
        return !defaultIncludes.contains(type)
                && !languageSpecificPrimitives.contains(type);
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }


    @Override
    public String toEnumValue(String value, String datatype) {
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            return value;
        } else {
            return escapeText(value);
        }
    }

    @Override
    public String toEnumDefaultValue(String value, String datatype) {
        return datatype + "_" + value;
    }

    @Override
    public String toEnumVarName(String name, String datatype) {
        if (name.length() == 0) {
            return "Empty";
        }

        // number
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            String varName = name;
            varName = varName.replaceAll("-", "Minus");
            varName = varName.replaceAll("\\+", "Plus");
            varName = varName.replaceAll("\\.", "Dot");
            return varName;
        }

        // for symbol, e.g. $, #
        if (getSymbolName(name) != null) {
            return getSymbolName(name);
        }

        // string
        String enumName = camelize(sanitizeName(name));
        enumName = enumName.replaceFirst("^_", "");
        enumName = enumName.replaceFirst("_$", "");

        if (isReservedWord(enumName) || enumName.matches("\\d.*")) { // reserved word or starts with number
            return escapeReservedWord(enumName);
        } else {
            return enumName;
        }
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        String name = property.name;
        if (!org.apache.commons.lang3.StringUtils.isEmpty(enumSuffix)) {
            name = name + "_" + enumSuffix;
        }
        // camelize the enum name
        // phone_number => PhoneNumber
        String enumName = camelize(toModelName(name));

        // remove [] for array or map of enum
        enumName = enumName.replace("[]", "");

        if (enumName.matches("\\d.*")) { // starts with number
            return "_" + enumName;
        } else {
            return enumName;
        }
    }

    @Override
    public String toDefaultValue(Schema p) {
        if (p.getDefault() != null) {
            return p.getDefault().toString();
        } else {
            return null;
        }
    }

    @Override
    protected void updateRequestBodyForString(CodegenParameter codegenParameter, Schema schema, Set<String> imports, String bodyParameterName) {
        /**
         * we have a custom version of this function to set isString to false for isByteArray
         */
        updateRequestBodyForPrimitiveType(codegenParameter, schema, bodyParameterName, imports);
        if (ModelUtils.isByteArraySchema(schema)) {
            codegenParameter.isByteArray = true;
            // custom code
            codegenParameter.setIsString(false);
        } else if (ModelUtils.isBinarySchema(schema)) {
            codegenParameter.isBinary = true;
            codegenParameter.isFile = true; // file = binary in OAS3
        } else if (ModelUtils.isUUIDSchema(schema)) {
            codegenParameter.isUuid = true;
        } else if (ModelUtils.isURISchema(schema)) {
            codegenParameter.isUri = true;
        } else if (ModelUtils.isEmailSchema(schema)) {
            codegenParameter.isEmail = true;
        } else if (ModelUtils.isDateSchema(schema)) { // date format
            codegenParameter.setIsString(false); // for backward compatibility with 2.x
            codegenParameter.isDate = true;
        } else if (ModelUtils.isDateTimeSchema(schema)) { // date-time format
            codegenParameter.setIsString(false); // for backward compatibility with 2.x
            codegenParameter.isDateTime = true;
        } else if (ModelUtils.isDecimalSchema(schema)) { // type: string, format: number
            codegenParameter.isDecimal = true;
            codegenParameter.setIsString(false);
        }
        codegenParameter.pattern = toRegularExpression(schema.getPattern());
    }

    @Override
    protected void updateParameterForString(CodegenParameter codegenParameter, Schema parameterSchema){
        /**
         * we have a custom version of this function to set isString to false for uuid
         */
        if (ModelUtils.isEmailSchema(parameterSchema)) {
            codegenParameter.isEmail = true;
        } else if (ModelUtils.isUUIDSchema(parameterSchema)) {
            codegenParameter.setIsString(false);
            codegenParameter.isUuid = true;
        } else if (ModelUtils.isByteArraySchema(parameterSchema)) {
            codegenParameter.setIsString(false);
            codegenParameter.isByteArray = true;
            codegenParameter.isPrimitiveType = true;
        } else if (ModelUtils.isBinarySchema(parameterSchema)) {
            codegenParameter.isBinary = true;
            codegenParameter.isFile = true; // file = binary in OAS3
            codegenParameter.isPrimitiveType = true;
        } else if (ModelUtils.isDateSchema(parameterSchema)) {
            codegenParameter.setIsString(false); // for backward compatibility with 2.x
            codegenParameter.isDate = true;
            codegenParameter.isPrimitiveType = true;
        } else if (ModelUtils.isDateTimeSchema(parameterSchema)) {
            codegenParameter.setIsString(false); // for backward compatibility with 2.x
            codegenParameter.isDateTime = true;
            codegenParameter.isPrimitiveType = true;
        } else if (ModelUtils.isDecimalSchema(parameterSchema)) { // type: string, format: number
            codegenParameter.setIsString(false);
            codegenParameter.isDecimal = true;
            codegenParameter.isPrimitiveType = true;
        }
        if (Boolean.TRUE.equals(codegenParameter.isString)) {
            codegenParameter.isPrimitiveType = true;
        }
    }

    @Override
    protected void updatePropertyForAnyType(CodegenProperty property, Schema p) {
        /**
         * we have a custom version of this function to not set isNullable to true
         */
        // The 'null' value is allowed when the OAS schema is 'any type'.
        // See https://github.com/OAI/OpenAPI-Specification/issues/1389
        if (Boolean.FALSE.equals(p.getNullable())) {
            LOGGER.warn("Schema '{}' is any type, which includes the 'null' value. 'nullable' cannot be set to 'false'", p.getName());
        }
        if (languageSpecificPrimitives.contains(property.dataType)) {
            property.isPrimitiveType = true;
        }
        if (ModelUtils.isMapSchema(p)) {
            // an object or anyType composed schema that has additionalProperties set
            // some of our code assumes that any type schema with properties defined will be a map
            // even though it should allow in any type and have map constraints for properties
            updatePropertyForMap(property, p);
        }
    }

    @Override
    public GeneratorLanguage generatorLanguage() { return GeneratorLanguage.RUST; }
}
