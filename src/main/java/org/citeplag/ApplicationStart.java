package org.citeplag;

import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Predicate;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import org.citeplag.beans.SearchResultResponse;
import org.citeplag.converter.MoiPresentationsConverter;
import org.citeplag.converter.SearchResultResponseConverter;
import org.citeplag.converter.SemanticEnhancedDocumentConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

/**
 * Main application start.
 *
 * @author Vincent Stange
 */
@ComponentScan
@Configuration
@EnableSwagger2
@EnableAutoConfiguration
public class ApplicationStart {

    public static void main(String[] args) throws Exception {
        // start the full spring environment
        SpringApplication.run(ApplicationStart.class, args);
    }

    @Autowired
    private TypeResolver typeResolver;

    /**
     * Pretty print for every json output.
     *
     * @return override the jackson builder.
     */
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.indentOutput(true);
        return builder;
    }

    @Bean("SearchResultResponseConverter")
    public HttpMessageConverter<SearchResultResponse> stringHttpMessageConverter() {
        return new SearchResultResponseConverter();
    }

    @Bean("SemanticEnhancedDocumentConverter")
    public HttpMessageConverter<SemanticEnhancedDocument> stringHttpSedMessageConverter() {
        return new SemanticEnhancedDocumentConverter();
    }

    @Bean("MoiPresentationsConverter")
    public HttpMessageConverter<MOIPresentations> stringHttpMoiMessageConverter() {
        return new MoiPresentationsConverter();
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }

    /**
     * SpringFox / Swagger configuration.
     * @return Docket Object from SpringFox / Swagger.
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                // general informations
                .apiInfo(getApiInfo())
                .pathMapping("/")
                // exposed endpoints
                .select()
                .paths(getDocumentedApiPaths())
                .build()
                // Convenience rule builder that substitutes a generic type with one type parameter
                // with the type parameter. In this case ResponseEntity<T>
                .genericModelSubstitutes(ResponseEntity.class)
                // default response code should not be used
                .useDefaultResponseMessages(false);
    }

    /**
     * Every REST Service we want to document with Swagger
     *
     * @return Predicate conditions
     */
    private Predicate<String> getDocumentedApiPaths() {
        return or(
                regex("/math.*"),
                regex("/moi.*"),
                regex("/tests.*"),
                regex("/config.*"),
                regex("/basex.*"),
                regex("/v1/media.*")
        );
    }

    /**
     * General information about our project's API.
     * (Information for the Swagger UI)
     *
     * @return see ApiInfo
     */
    private ApiInfo getApiInfo() {
        return new ApiInfoBuilder()
                .title("MathML Demo (VMEXT) - An Endpoint for LaTeX and MathML Computations & Visualizations")
                .description("A SciPlore & LaCASt Project")
                .termsOfServiceUrl("http://springfox.io")
                .contact(new Contact("Vincent Stange", null, "vinc.sohn at gmail.com"))
                .license("Apache License Version 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
                .version("2.0")
                .build();
    }
}