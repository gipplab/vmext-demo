package org.citeplag.controller;

import com.formulasearchengine.mathmltools.converters.MathoidConverter;
import com.formulasearchengine.mathmltools.converters.cas.TranslationResponse;
import com.formulasearchengine.mathmltools.converters.mathoid.EnrichedMathMLTransformer;
import com.formulasearchengine.mathmltools.converters.services.LaTeXMLServiceResponse;
import com.formulasearchengine.mathmltools.similarity.MathPlag;
import com.formulasearchengine.mathmltools.similarity.result.Match;
import com.google.common.collect.Maps;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.GenericLatexSemanticEnhancer;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import mlp.ParseException;
import org.apache.log4j.Logger;
import org.citeplag.beans.CASTranslators;
import org.citeplag.beans.SimilarityResult;
import org.citeplag.config.CASTranslatorConfig;
import org.citeplag.config.LaTeXMLRemoteConfig;
import org.citeplag.config.MathoidConfig;
import org.citeplag.util.CASTranslatorsBinder;
import org.citeplag.util.Example;
import org.citeplag.util.ExampleLoader;
import org.citeplag.util.LaTeXMLInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for our little MathML Pipeline.
 * Here we have in total:
 * <p>
 * 1. two POST methods for our latex to mathml conversion via latexml and mathoid
 * 2. one POST method for the similarity comparison
 * 3. one GET method to load a predefined example
 *
 * @author Vincent Stange
 */
@RestController
@RequestMapping("/math")
public class MathController {
    private static Logger logger = Logger.getLogger(MathController.class);

    @Autowired
    private MathoidConfig mathoidConfig;

    @Autowired
    private LaTeXMLRemoteConfig laTeXMLRemoteConfig;

    @Autowired
    private CASTranslatorConfig translatorConfig;

    @PostConstruct
    public void init() {
        try {
            logger.info("Construct translators.");
            CASTranslators.init(translatorConfig);
        } catch (Exception e) {
            logger.warn("Cannot construct translators.", e);
        }
    }

    /**
     * POST method for calling the LaTeXML service / installation.
     *
     * @param config   optional configuration, if null, system default will be used
     * @param rawLatex the original (generic) input tex format, needed if the latex parameter is semantic
     * @param latex    latex to be converted
     * @param request  http request for logging
     * @return service response
     * @throws Exception anything that could go wrong
     */
    @PostMapping
    @ApiOperation(value = "Converts a Latex String via LaTeXML to MathML semantics.")
    public LaTeXMLServiceResponse convertLatexml(
            @RequestParam(required = false) String config,
            @RequestParam(required = false) String rawLatex,
            @RequestParam String latex,
            HttpServletRequest request) throws Exception {
        return LaTeXMLInterface.convertLaTeX(
                laTeXMLRemoteConfig, config, rawLatex, latex, request
        );
    }

    @InitBinder("cas")
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(CASTranslators.class, new CASTranslatorsBinder());
    }

    @PostMapping("/translation")
    @ApiOperation(value = "Translates a semantic LaTeX string to a given CAS.")
    public TranslationResponse translation(
            @RequestParam() CASTranslators cas,
            @RequestParam() String latex,
            @RequestParam(required = false) String label,
            HttpServletRequest request
    ) {
        logger.info("Start translation process to " + cas + " from: " + request.getRemoteAddr());

        SemanticLatexTranslator translator = cas.getTranslator();
        try {
            TranslationInformation translationInf = translator.translateToObject(latex, label);
            TranslationResponse tr = new TranslationResponse();
            tr.setResult(translationInf.getTranslatedExpression());
            tr.setLog(translationInf.getTranslationInformation().toString());
            return tr;
        } catch (NullPointerException npe) {
            TranslationResponse response = new TranslationResponse();
            response.setLog("res");
            return response;
        } catch (Exception e) {
            logger.warn("Error due translation for " + latex, e);
            String errorMsg = "[ERROR] " + e.toString();
            TranslationResponse response = new TranslationResponse();
            response.setLog(errorMsg);
            return response;
        }
    }

    /**
     * POST method for calling the Mathoid service.
     *
     * @param mathoidUrl optional url configuration, if null, system default will be used
     * @param latex      latex to be converted
     * @param request    http request for logging
     * @return mathml as string
     * @throws Exception anything that could go wrong
     */
    @PostMapping("/mathoid")
    @ApiOperation(value = "Converts a Latex String via Mathoid to MathML semantics.")
    public String convertMathoid(
            @RequestParam(required = false) String mathoidUrl,
            @RequestParam() String latex,
            HttpServletRequest request) throws Exception {

        // If local configuration is given, use it.
        mathoidUrl = mathoidUrl != null ? mathoidUrl : mathoidConfig.getUrl();

        MathoidConverter mathoidConverter = new MathoidConverter(new MathoidConfig().setUrl(mathoidUrl));
        try {
            logger.info("latex conversion via mathoid from: " + request.getRemoteAddr());
            String eMathML = mathoidConverter.convertLatex(latex);
            // transform enriched MathML to well-formed MathML (pMML + cMML)
            return new EnrichedMathMLTransformer(eMathML).getFullMathML();
        } catch (ResourceAccessException e) {
            return "mathoid not available under: " + mathoidUrl;
        } catch (Exception e) {
            logger.error("mathoid service error", e);
            return e.getMessage();
        }
    }

    @PostMapping(path = "similarity")
    @ApiOperation(value = "Get a list of similarities between two MathML semantics.")
    public SimilarityResult getSimilarities(
            @RequestParam(value = "mathml1") String mathmlA,
            @RequestParam(value = "mathml2") String mathmlB,
            @RequestParam(value = "type") String type,
            HttpServletRequest request) {

        try {
            List<Match> similarities;
            if (type.equals("similar")) {
                logger.info("similarity comparison from: " + request.getRemoteAddr());
                similarities = MathPlag.compareSimilarMathML(mathmlA, mathmlB);
            } else {
                logger.info("identical comparison from: " + request.getRemoteAddr());
                similarities = MathPlag.compareIdenticalMathML(mathmlA, mathmlB);
            }

            // also compare the original similarity factors
            Map<String, Object> originals = MathPlag.compareOriginalFactors(mathmlA, mathmlB);

            return new SimilarityResult("Okay", "", similarities, originals);
        } catch (Exception e) {
            logger.error("similarity error", e);
            return new SimilarityResult("Error", e.getMessage(), Collections.emptyList(), Maps.newTreeMap());
        }
    }

    /**
     * GET method to load an example and print the object out as a JSON.
     * (JSON transformation is done by spring)
     *
     * @return current example
     * @throws IOException requested example does not exist
     */
    @GetMapping(path = "example")
    @ApiOperation(value = "Get a full example for the demo.")
    public Example getExample() throws IOException {
        // this could easily be extended for more examples
        return new ExampleLoader().load("euler");
    }

    @PostMapping("/analyzeDocument")
    @ApiOperation(
            value = "Analyzes the math in the given wikitext content"
    )
    public List<SemanticEnhancedDocument> getSemanticEnhancedDocuments(
            @ApiParam(value = "The text search query", required = true)
            @RequestParam() String content) {
        GenericLatexSemanticEnhancer semanticEnhancer = new GenericLatexSemanticEnhancer();
        return semanticEnhancer.getSemanticEnhancedDocumentsFromWikitext(content);
    }

    @PostMapping("/analyzeFormula")
    @ApiOperation(
            value = "Analyzes the math in the given wikitext content"
    )
    public HttpEntity getSemanticEnhancedMath(
            @ApiParam(value = "The text search query", required = true)
            @RequestParam() String content,
            @ApiParam(value = "The formula", required = true)
            @RequestParam()String latex
    ) {
        GenericLatexSemanticEnhancer semanticEnhancer = new GenericLatexSemanticEnhancer();
        try {
            MOIPresentations result = semanticEnhancer.enhanceGenericLaTeX(content, latex, null);
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(result, header);
        } catch (ParseException e) {
            logger.error("Unable to annotate generic latex: " + latex, e);
            return new ResponseEntity<>("Unknown Format", HttpStatus.BAD_REQUEST);
        }
    }
}
