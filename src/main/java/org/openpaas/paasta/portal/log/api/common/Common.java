package org.openpaas.paasta.portal.log.api.common;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.ClientCredentialsGrantTokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;
import org.cloudfoundry.uaa.tokens.GetTokenByClientCredentialsRequest;
import org.cloudfoundry.uaa.tokens.GetTokenByClientCredentialsResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.openpaas.paasta.portal.log.api.config.cloudfoundry.provider.TokenGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Common {

    private static final Logger LOGGER = LoggerFactory.getLogger(Common.class);

    @Value("${cloudfoundry.cc.api.url}")
    public String apiTarget;

    @Value("${cloudfoundry.cc.api.uaaUrl}")
    public String uaaTarget;

    @Value("${cloudfoundry.cc.api.sslSkipValidation}")
    public boolean cfskipSSLValidation;

    @Value("${cloudfoundry.user.admin.username}")
    public String adminUserName;

    @Value("${cloudfoundry.user.admin.password}")
    public String adminPassword;

    public static final String AUTHORIZATION_HEADER_KEY = "cf-Authorization";

    @Value("${cloudfoundry.user.uaaClient.clientId}")
    public String uaaClientId;

    @Value("${cloudfoundry.user.uaaClient.clientSecret}")
    public String uaaClientSecret;

    @Value("${cloudfoundry.user.uaaClient.adminClientId}")
    public String uaaAdminClientId;

    @Value("${cloudfoundry.user.uaaClient.adminClientSecret}")
    public String uaaAdminClientSecret;

    @Value("${cloudfoundry.user.uaaClient.skipSSLValidation}")
    public boolean skipSSLValidation;




    @Autowired
    DefaultConnectionContext connectionContext;

    @Autowired
    PasswordGrantTokenProvider tokenProvider;


    public ObjectMapper objectMapper = new ObjectMapper();



    public URL getTargetURL(String target) throws MalformedURLException, URISyntaxException {
        return getTargetURI(target).toURL();
    }

    private URI getTargetURI(String target) throws URISyntaxException {
        return new URI(target);
    }

    /**
     * get CloudFoundryClinet Object from token String
     *
     * @param token
     * @return CloudFoundryClinet
     */
    public CloudFoundryClient getCloudFoundryClient(String token) throws MalformedURLException, URISyntaxException {

        return new CloudFoundryClient(getCloudCredentials(token), getTargetURL(apiTarget), true);
    }


    /**
     * get CloudCredentials Object from token String
     *
     * @param token
     * @return CloudCredentials
     */
    public CloudCredentials getCloudCredentials(String token) {
        return new CloudCredentials(getOAuth2AccessToken(token), false);
    }

    /**
     * get CloudCredentials Object from id, password
     *
     * @param id
     * @param password
     * @return CloudCredentials
     */
    public CloudCredentials getCloudCredentials(String id, String password) {

        LOGGER.info("============getCloudCredentials==============");
        CloudCredentials test = new CloudCredentials(id, password);
        LOGGER.info("getToken       :" + test.getToken());
        LOGGER.info("getClientId    :" + test.getClientId());
        LOGGER.info("getClientSecret:" + test.getClientSecret());
        LOGGER.info("getEmail       :" + test.getEmail());
        LOGGER.info("getPassword    :" + test.getPassword());
        LOGGER.info("getProxyUser   :" + test.getProxyUser());
        return test;

    }

    /**
     * get DefailtOAuth2AccessToken Object from token String
     *
     * @param token
     * @return
     */
    private DefaultOAuth2AccessToken getOAuth2AccessToken(String token) {
        return new DefaultOAuth2AccessToken(token);
    }


    //credentials 세팅
    private ResourceOwnerPasswordResourceDetails getCredentials(String uaaClientId) {
        ResourceOwnerPasswordResourceDetails credentials = new ResourceOwnerPasswordResourceDetails();
        credentials.setAccessTokenUri(uaaTarget + "/oauth/token?grant_type=client_credentials&response_type=token");
        credentials.setClientAuthenticationScheme(AuthenticationScheme.header);

        credentials.setClientId(uaaClientId);

        if (uaaClientId.equals(uaaAdminClientId)) {
            credentials.setClientSecret(uaaAdminClientSecret);
        }
        return credentials;
    }


    /**
     * 요청 파라미터들의 빈값 또는 null값 확인을 하나의 메소드로 처리할 수 있도록 생성한 메소드
     * 요청 파라미터 중 빈값 또는 null값인 파라미터가 있는 경우, false를 리턴한다.
     *
     * @param params
     * @return
     */
    public boolean stringNullCheck(String... params) {
        return Arrays.stream(params).allMatch(param -> null != param && !param.equals(""));
    }

    //요청 문자열 파라미터 중, 공백을 포함하고 있는 파라미터가 있을 경우 false를 리턴
    public boolean stringContainsSpaceCheck(String... params) {
        return Arrays.stream(params).allMatch(param -> !param.contains(" "));
    }

    /**
     * Gets property value.
     *
     * @param key the key
     * @return property value
     * @throws Exception the exception
     */
    public static String getPropertyValue(String key) throws Exception {
        return getPropertyValue(key, "/config.properties");
    }


    /**
     * Gets process property value.
     *
     * @param key            the key
     * @param configFileName the config file name
     * @return property value
     * @throws Exception the exception
     */
    private static String getProcPropertyValue(String key, String configFileName) throws Exception {
        if (Constants.NONE_VALUE.equals(configFileName)) return "";

        Properties prop = new Properties();

        try (InputStream inputStream = ClassLoader.class.getResourceAsStream(configFileName)) {
            prop.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return prop.getProperty(key);
    }

    /**
     * Gets property value.
     *
     * @param key            the key
     * @param configFileName the config file name
     * @return property value
     * @throws Exception the exception
     */
    public static String getPropertyValue(String key, String configFileName) throws Exception {
        return getProcPropertyValue(key, Optional.ofNullable(configFileName).orElse(Constants.NONE_VALUE));
    }


    public static String convertApiUrl(String url) {
        return url.replace("https://", "").replace("http://", "");
    }


    /**
     * DefaultCloudFoundryOperations을 생성하여, 반환한다.
     *
     * @param connectionContext
     * @param tokenProvider
     * @return DefaultCloudFoundryOperations
     */
    public static DefaultCloudFoundryOperations cloudFoundryOperations(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return cloudFoundryOperations(cloudFoundryClient(connectionContext, tokenProvider), dopplerClient(connectionContext, tokenProvider), uaaClient(connectionContext, tokenProvider));
    }

    /**
     * DefaultCloudFoundryOperations을 생성하여, 반환한다.
     *
     * @param cloudFoundryClient
     * @param dopplerClient
     * @param uaaClient
     * @return DefaultCloudFoundryOperations
     */
    public static DefaultCloudFoundryOperations cloudFoundryOperations(org.cloudfoundry.client.CloudFoundryClient cloudFoundryClient, DopplerClient dopplerClient, UaaClient uaaClient) {
        return DefaultCloudFoundryOperations.builder().cloudFoundryClient(cloudFoundryClient).dopplerClient(dopplerClient).uaaClient(uaaClient).build();
    }

    /**
     * DefaultCloudFoundryOperations을 생성하여, 반환한다.
     *
     * @param connectionContext
     * @param tokenProvider
     * @param org
     * @param space
     * @return DefaultCloudFoundryOperations
     */
    public static DefaultCloudFoundryOperations cloudFoundryOperations(ConnectionContext connectionContext, TokenProvider tokenProvider, String org, String space) {
        return cloudFoundryOperations(cloudFoundryClient(connectionContext, tokenProvider), dopplerClient(connectionContext, tokenProvider), uaaClient(connectionContext, tokenProvider), org, space);
    }

    /**
     * DefaultCloudFoundryOperations을 생성하여, 반환한다.
     *
     * @param cloudFoundryClient
     * @param dopplerClient
     * @param uaaClient
     * @param org
     * @param space
     * @return DefaultCloudFoundryOperations
     */
    public static DefaultCloudFoundryOperations cloudFoundryOperations(org.cloudfoundry.client.CloudFoundryClient cloudFoundryClient, DopplerClient dopplerClient, UaaClient uaaClient, String org, String space) {
        return DefaultCloudFoundryOperations.builder().cloudFoundryClient(cloudFoundryClient).dopplerClient(dopplerClient).uaaClient(uaaClient).organization(org).space(space).build();
    }

    /**
     * ReactorCloudFoundryClient 생성하여, 반환한다.
     *
     * @param connectionContext
     * @param tokenProvider
     * @return DefaultCloudFoundryOperations
     */
    public static ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorCloudFoundryClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    }


    public static ReactorDopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorDopplerClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    }

    public static ReactorUaaClient uaaClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
        return ReactorUaaClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    }

    public static ReactorUaaClient uaaClient(ConnectionContext connectionContext, String clientId, String clientSecret) {
        return ReactorUaaClient.builder().connectionContext(connectionContext).tokenProvider(ClientCredentialsGrantTokenProvider.builder().clientId(clientId).clientSecret(clientSecret).build()).build();
    }

    // UAA Admin Client
    public static ReactorUaaClient uaaAdminClient(ConnectionContext connectionContext, String apiTarget, String token, String uaaAdminClientId, String uaaAdminClientSecret) {
        ReactorUaaClient reactorUaaClient = Common.uaaClient(connectionContext, tokenProvider(token));
        GetTokenByClientCredentialsResponse getTokenByClientCredentialsResponse = reactorUaaClient.tokens().getByClientCredentials(GetTokenByClientCredentialsRequest.builder().clientId(uaaAdminClientId).clientSecret(uaaAdminClientSecret).build()).block();
        return Common.uaaClient(connectionContext, tokenProvider(getTokenByClientCredentialsResponse.getAccessToken()));
    }

    private static final ThreadLocal<DefaultConnectionContext> connectionContextThreadLocal = new ThreadLocal<>();



    public DefaultConnectionContext connectionContext() {
        return connectionContext;
    }

    public static DefaultConnectionContext crateConnectionContext(String apiUrl, boolean skipSSLValidation) {
        DefaultConnectionContext connectionContext = peekConnectionContext();
        if (null != connectionContext) {
            boolean isEqual = connectionContext.getApiHost().equals(convertApiUrl(apiUrl)) && connectionContext.getSkipSslValidation().get() == skipSSLValidation;
            if (!isEqual) {
                removeConnectionContext();
                connectionContext = null;
            }
        }

        if (null == connectionContext) {
            connectionContext = DefaultConnectionContext.builder().apiHost(convertApiUrl(apiUrl)).skipSslValidation(skipSSLValidation).build();
            pushConnectionContext(connectionContext);
        }

        return connectionContext;
    }




    private static DefaultConnectionContext peekConnectionContext() {
        return connectionContextThreadLocal.get();
    }

    private static void pushConnectionContext(DefaultConnectionContext connectionContext) {
        connectionContextThreadLocal.set(connectionContext);
        LOGGER.info("Create connection context and push thread local : DefalutConnectionContext@{}", Integer.toHexString(connectionContext.hashCode()));
    }

    private static void removeConnectionContext() {
        disposeConnectionContext(connectionContextThreadLocal.get());
        connectionContextThreadLocal.remove();
    }



    private static void disposeConnectionContext(DefaultConnectionContext connectionContext) {
        try {
            if (null != connectionContext) connectionContext.dispose();
        } catch (Exception ignore) {
        }
    }

    public static TokenGrantTokenProvider tokenProvider(String token) {
        try {
            if (token.indexOf("bearer") < 0) {
                token = "bearer " + token;
            }
            return new TokenGrantTokenProvider(token);
        } catch (Exception e) {
            return null;
        }
    }

    public static TokenProvider tokenProviderWithDefault(String token, TokenProvider defaultTokenProvider) {
        if (null == token) return defaultTokenProvider;
        else if (token.trim().length() <= 0) return defaultTokenProvider;

        return tokenProvider(token);
    }

    /**
     * token을 제공하는 클레스 사용자 임의의 clientId를 사용하며,
     * user token, client token을 모두 얻을 수 있다.
     *
     * @param username
     * @param password
     * @return
     */
    public static PasswordGrantTokenProvider tokenProvider(String username, String password) {
        return PasswordGrantTokenProvider.builder().password(password).username(username).build();
    }

    public PasswordGrantTokenProvider tokenProvider() {
        return tokenProvider;
    }
}
