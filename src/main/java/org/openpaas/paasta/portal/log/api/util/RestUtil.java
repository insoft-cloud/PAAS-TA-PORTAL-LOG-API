package org.openpaas.paasta.portal.log.api.util;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.rest.*;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.http.conn.ssl.SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;

/**
 * Some helper utilities for creating classes used for the REST support.
 *
 * @author Thomas Risberg
 */
public class RestUtil {

    public RestTemplate createRestTemplate(HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        RestTemplate restTemplate = new LoggingRestTemplate();
        restTemplate.setRequestFactory(createRequestFactory(httpProxyConfiguration, trustSelfSignedCerts));
        restTemplate.setErrorHandler(new CloudControllerResponseErrorHandler());
        restTemplate.setMessageConverters(getHttpMessageConverters());

        return restTemplate;
    }

    public ClientHttpRequestFactory createRequestFactory(HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties();

        if (trustSelfSignedCerts) {
            httpClientBuilder.setSslcontext(buildSslContext());
            httpClientBuilder.setHostnameVerifier(BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        }

        if (httpProxyConfiguration != null) {
            HttpHost proxy = new HttpHost(httpProxyConfiguration.getProxyHost(), httpProxyConfiguration.getProxyPort());
            httpClientBuilder.setProxy(proxy);

            if (httpProxyConfiguration.isAuthRequired()) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(httpProxyConfiguration.getProxyHost(), httpProxyConfiguration.getProxyPort()),
                        new UsernamePasswordCredentials(httpProxyConfiguration.getUsername(), httpProxyConfiguration.getPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            httpClientBuilder.setRoutePlanner(routePlanner);
        }

        HttpClient httpClient = httpClientBuilder.build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return requestFactory;
    }

//    public CustomOauthClient createOauthClient(URL authorizationUrl, HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
//        return new CustomOauthClient(authorizationUrl, createRestTemplate(httpProxyConfiguration, trustSelfSignedCerts));
//    }

    private javax.net.ssl.SSLContext buildSslContext()  {
        try {
            return new SSLContextBuilder().useSSL().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("An error occurred setting up the SSLContext", gse);
        }
    }

    private List<HttpMessageConverter<?>> getHttpMessageConverters() {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(new UploadApplicationPayloadHttpMessageConverter());
        messageConverters.add(getFormHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());
        messageConverters.add(new LoggregatorHttpMessageConverter());
        return messageConverters;
    }

    private FormHttpMessageConverter getFormHttpMessageConverter() {
        FormHttpMessageConverter formPartsMessageConverter = new CloudFoundryFormHttpMessageConverter();
        formPartsMessageConverter.setPartConverters(getFormPartsMessageConverters());
        return formPartsMessageConverter;
    }

    private List<HttpMessageConverter<?>> getFormPartsMessageConverters() {
        List<HttpMessageConverter<?>> partConverters = new ArrayList<HttpMessageConverter<?>>();
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        stringConverter.setSupportedMediaTypes(Collections.singletonList(JsonUtil.JSON_MEDIA_TYPE));
        stringConverter.setWriteAcceptCharset(false);
        partConverters.add(stringConverter);
        partConverters.add(new ResourceHttpMessageConverter());
        partConverters.add(new UploadApplicationPayloadHttpMessageConverter());
        return partConverters;
    }
}
