package org.citeplag.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.beans.SearchResultResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class SearchResultResponseConverter extends AbstractHttpMessageConverter<SearchResultResponse> {
    private static final Logger LOG = LogManager.getLogger(SearchResultResponseConverter.class.getName());

    private final ObjectMapper mapper;

    public SearchResultResponseConverter() {
        super();
        LinkedList<MediaType> supportedMediaTypes = new LinkedList<>();
        supportedMediaTypes.add(MediaType.TEXT_PLAIN);
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        super.setSupportedMediaTypes(supportedMediaTypes);
        super.setDefaultCharset(StandardCharsets.UTF_8);
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.isInstance(new SearchResultResponse());
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return MediaType.APPLICATION_JSON.includes(mediaType);
    }

    @Override
    protected @NotNull
    SearchResultResponse readInternal(@NotNull Class<? extends SearchResultResponse> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        String arg = IOUtils.toString(inputMessage.getBody(), StandardCharsets.UTF_8.toString());
        LOG.warn("Read Internal, but if is the argument?: " + arg);
        return mapper.readValue(inputMessage.getBody(), clazz);
    }

    @Override
    protected void writeInternal(@NotNull SearchResultResponse resultResponse, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        MediaType requestedContentType = outputMessage.getHeaders().getContentType();
        if (MediaType.TEXT_PLAIN.includes(requestedContentType)) {
            outputMessage.getBody().write(resultResponse.toString().getBytes());
        } else if (MediaType.APPLICATION_JSON.includes(requestedContentType)) {
            byte[] bytes = mapper.writeValueAsString(resultResponse).getBytes();
            outputMessage.getBody().write(bytes);
        } else {
            LOG.info("Unsupported media requested for House. Switch to default (JSON)");
            outputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            writeInternal(resultResponse, outputMessage);
        }
    }
}
