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
import gov.nist.drmf.interpreter.common.exceptions.MinimumRequirementNotFulfilledException;
import gov.nist.drmf.interpreter.common.interfaces.TranslationFeature;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.common.pojo.SemanticEnhancedAnnotationStatus;
import gov.nist.drmf.interpreter.generic.GenericLatexSemanticEnhancer;
import gov.nist.drmf.interpreter.generic.SemanticEnhancedDocumentBuilder;
import gov.nist.drmf.interpreter.generic.mediawiki.DefiningFormula;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.generic.GenericReplacementTool;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.beans.CASTranslators;
import org.citeplag.beans.MOIPresentationsExtension;
import org.citeplag.beans.SemanticEnhancedDocumentExtension;
import org.citeplag.beans.SimilarityResult;
import org.citeplag.config.CASTranslatorConfig;
import org.citeplag.config.GenericLatexTranslatorConfig;
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
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

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
    private static final Logger LOG = LogManager.getLogger(MathController.class.getName());

    @Autowired
    private MathoidConfig mathoidConfig;

    @Autowired
    private LaTeXMLRemoteConfig laTeXMLRemoteConfig;

    @Autowired
    private CASTranslatorConfig translatorConfig;

    @Autowired
    private GenericLatexTranslatorConfig genericTranslatorConfig;

    private GenericLatexSemanticEnhancer semanticEnhancer;

    @PostConstruct
    public void init() {
        try {
            LOG.info("Construct translators.");
            semanticEnhancer = new GenericLatexSemanticEnhancer(GenericLatexTranslatorConfig.getDefaultConfig());
            CASTranslators.init(translatorConfig);
        } catch (Exception e) {
            LOG.warn("Cannot construct translators and semantic enhancer.", e);
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
            LOG.info("latex conversion via mathoid from: " + request.getRemoteAddr());
            String eMathML = mathoidConverter.convertLatex(latex);
            // transform enriched MathML to well-formed MathML (pMML + cMML)
            return new EnrichedMathMLTransformer(eMathML).getFullMathML();
        } catch (ResourceAccessException e) {
            return "mathoid not available under: " + mathoidUrl;
        } catch (Exception e) {
            LOG.error("mathoid service error", e);
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
                LOG.info("similarity comparison from: " + request.getRemoteAddr());
                similarities = MathPlag.compareSimilarMathML(mathmlA, mathmlB);
            } else {
                LOG.info("identical comparison from: " + request.getRemoteAddr());
                similarities = MathPlag.compareIdenticalMathML(mathmlA, mathmlB);
            }

            // also compare the original similarity factors
            Map<String, Object> originals = MathPlag.compareOriginalFactors(mathmlA, mathmlB);

            return new SimilarityResult("Okay", "", similarities, originals);
        } catch (Exception e) {
            LOG.error("similarity error", e);
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
            @RequestParam(required = false) Boolean genericExperimentalFeatures,
            HttpServletRequest request
    ) {
        boolean experimental = genericExperimentalFeatures != null && genericExperimentalFeatures;

        LOG.info("Start translation process to " + cas + " from: " + request.getRemoteAddr());
        if (experimental) {
            LOG.debug("Experimental translation process activated.");
        }

        SemanticLatexTranslator translator = cas.getTranslator();
        try {
            TranslationFeature<PrintablePomTaggedExpression> feature = experimental ? new GenericReplacementTool() : null;
            TranslationInformation translationInf = translator.translateToObject(latex, label, feature);
            TranslationResponse tr = new TranslationResponse();
            tr.setResult(translationInf.getTranslatedExpression());
            tr.setLog(translationInf.getTranslationInformation().toString());
            return tr;
        } catch (NullPointerException npe) {
            TranslationResponse response = new TranslationResponse();
            response.setLog("res");
            return response;
        } catch (Exception e) {
            LOG.warn("Error due translation for " + latex, e);
            String errorMsg = "[ERROR] " + e.toString();
            TranslationResponse response = new TranslationResponse();
            response.setLog(errorMsg);
            return response;
        }
    }

    @PostMapping(
            value = "/generateAnnotatedDependencyGraph",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiOperation(
            value = "Generates an annotated dependency graph"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully analyzed", response = SemanticEnhancedDocument.class),
                    @ApiResponse(code = 500, message = "Unable generate semantic enhanced document")
            }
    )
    public SemanticEnhancedDocument getSemanticEnhancedDocument(
            @ApiParam(value = "The wikipage to analyze.", required = true)
            @RequestParam() String content
    ) {
        return semanticEnhancer.generateAnnotatedDocument(content);
    }

    // test case Jacobi polynomial: Q371631
    @PostMapping(
            value = "/generateAnnotatedDependencyGraphFromWikidataItem",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiOperation(
            value = "Generates an annotated dependency graph"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully loaded and analyzed", response = SemanticEnhancedDocument.class),
                    @ApiResponse(code = 500, message = "Unable generate semantic enhanced document by given Wikidata ID")
            }
    )
    public SemanticEnhancedDocument wikidataLoader(
            @RequestParam() String qid
    ) throws MediaWikiApiErrorException, IOException {
        SemanticEnhancedDocumentBuilder builder = SemanticEnhancedDocumentBuilder.getDefaultBuilder();
        return builder.getDocumentFromWikidataItem(qid);
    }

    @PostMapping(
            value = "/suggestWikidataItemDefiningFormula",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ApiOperation(
            value = "Suggests defining formula and its elements for a given Wikidata QID"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Scored Suggestions", response = DefiningFormula[].class),
                    @ApiResponse(code = 500, message = "Unable generate suggestions for given Wikidata QID")
            }
    )
    public List<DefiningFormula> wikidataSuggestor(
            @RequestParam() String qid
    ) throws MediaWikiApiErrorException, IOException {
        SemanticEnhancedDocumentBuilder builder = SemanticEnhancedDocumentBuilder.getDefaultBuilder();
        return builder.enhanceWikidataItem(qid);
    }

    @PostMapping("/appendTranslationsToDocument")
    @ApiOperation(
            value = "Adds translations to semantic LaTeX and CAS to each formulae in the provided document"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully translated entire document", response = SemanticEnhancedDocument.class),
                    @ApiResponse(code = 500, message = "Unable to append translations to document")
            }
    )
    public HttpEntity<?> appendTranslationsToDocument(
            @RequestBody SemanticEnhancedDocumentExtension semanticEnhancedDocumentExtension) {
        try {
            LOG.info("Start translating elements of document: " + semanticEnhancedDocumentExtension.getTitle());
            SemanticEnhancedDocument sed = semanticEnhancer.appendTranslationsToDocument(semanticEnhancedDocumentExtension);
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(sed, header);
        } catch (MinimumRequirementNotFulfilledException e) {
            LOG.error("Provided SED did not fulfill minimum requirement: " + e.getMessage());
            return new ResponseEntity<>("The given document did not fulfill the minimum requirements: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            LOG.error("Unable to append translations to document", e);
            return new ResponseEntity<>("An unknown error occurred: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/generateTranslatedComputedMoi")
    @ApiOperation(
            value = "Generates a translated and computed MOI for the given latex string and the given context (as dependency graph)"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully translated and computed formula", response = SemanticEnhancedDocument.class),
                    @ApiResponse(code = 500, message = "Unable to perform translation and computation")
            }
    )
    public HttpEntity<?> getTranslatedAndComputedMoi(
            @ApiParam(value = "The context as a dependency graph", required = true)
            @RequestBody SemanticEnhancedDocumentExtension documentDependencyGraph,
            @ApiParam(value = "The LaTeX string of the formula to analyze (might or might not be in the graph)", required = true)
            @RequestParam() String latex
    ) {
        try {
            MOIPresentations moi = semanticEnhancer.generateMOIPresentationFromDocument(documentDependencyGraph, latex);

            if (!SemanticEnhancedAnnotationStatus.TRANSLATED.hasPassed(moi.getRank())) {
                LOG.warn("Unable to perform translations for the given LaTeX string: " + latex);
                moi.setScore(-1.0);
            } else {
                moi = semanticEnhancer.computeMOI(moi);
            }

            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(moi, header);
        } catch (MinimumRequirementNotFulfilledException mrnfe) {
            LOG.error("The document did not provide sufficient information: " + mrnfe.getMessage());
            return new ResponseEntity<>("The given context (dependency graph) did not contain sufficient information: " + mrnfe.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (ParseException pe) {
            LOG.error("Unable to parse the given latex string. Reason: " + pe.getMessage());
            return new ResponseEntity<>("Unable to parse the given LaTeX formula because: " + pe.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            LOG.error("An unknown error occurred during computation/translation: " + e.getMessage(), e);
            return new ResponseEntity<>("An unknown error occurred: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/generateContextMoi")
    @ApiOperation(
            value = "Generates a single MOI object based on the given document context. The given latex expression can be included in the document or not."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully searched", response = SemanticEnhancedDocument.class),
                    @ApiResponse(code = 500, message = "Unable to generate MOI based on given context")
            }
    )
    public HttpEntity<?> generateMOIObject(
            @RequestBody SemanticEnhancedDocumentExtension semanticEnhancedDocumentExtension,
            @RequestParam() String latex
    ) {
        try {
            MOIPresentations moi = semanticEnhancer.generateMOIPresentationFromDocument(semanticEnhancedDocumentExtension, latex);
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(moi, header);
        } catch (MinimumRequirementNotFulfilledException e) {
            LOG.error("Provided SED did not fulfill minimum requirement: " + e.getMessage());
            return new ResponseEntity<>("The given document did not fulfill the minimum requirements: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            LOG.error("Unable to generate MOI object", e);
            return new ResponseEntity<>("An unknown error occurred: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/computeSingleMoi")
    @ApiOperation(
            value = "Computes a single MOI object. The object requires semantic latex and translations to CAS."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully searched", response = MOIPresentations.class),
                    @ApiResponse(code = 500, message = "Unable to compute MOI")
            }
    )
    public HttpEntity<?> computeSingleMoi(
            @RequestBody MOIPresentationsExtension moi
    ) {
        try {
            MOIPresentations result = semanticEnhancer.computeMOI(moi);
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(result, header);
        } catch (MinimumRequirementNotFulfilledException e) {
            LOG.error("Provided MOI did not fulfill minimum requirement: " + e.getMessage());
            return new ResponseEntity<>("The given MOI did not fulfill the minimum requirement. It requires semantic LaTeX and translations to CAS: "
                    + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            LOG.error("Unable to generate MOI object", e);
            return new ResponseEntity<>("An unknown error occurred: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/computeSingleMoiForCas")
    @ApiOperation(
            value = "Computes a single MOI object. The object requires semantic latex and translations to CAS."
    )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully searched", response = CASResult.class),
                    @ApiResponse(code = 500, message = "Unable to compute MOI in given CAS")
            }
    )
    public HttpEntity<?> computeSingleMoi(
            @RequestBody MOIPresentationsExtension moi,
            @RequestParam() CASTranslators cas
    ) {
        try {
            CASResult result = semanticEnhancer.computeMOI(moi, cas.toString());
            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(result, header);
        } catch (MinimumRequirementNotFulfilledException e) {
            LOG.error("Provided MOI did not fulfill minimum requirement: " + e.getMessage());
            return new ResponseEntity<>("The given MOI did not fulfill the minimum requirement. It requires semantic LaTeX and translations to CAS: "
                    + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            LOG.error("Unable to generate MOI object", e);
            return new ResponseEntity<>("An unknown error occurred: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
