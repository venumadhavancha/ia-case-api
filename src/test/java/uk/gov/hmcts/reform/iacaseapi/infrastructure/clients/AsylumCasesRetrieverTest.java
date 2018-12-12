package uk.gov.hmcts.reform.iacaseapi.infrastructure.clients;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.State.APPEAL_STARTED;

import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.iacaseapi.domain.entities.ccd.State;
import uk.gov.hmcts.reform.iacaseapi.domain.exceptions.AsylumCaseRetrievalException;
import uk.gov.hmcts.reform.iacaseapi.infrastructure.security.AccessTokenDecoder;
import uk.gov.hmcts.reform.iacaseapi.infrastructure.security.IdamAuthorizor;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class AsylumCasesRetrieverTest {

    private final AuthTokenGenerator serviceAuthorizationTokenGenerator = mock(AuthTokenGenerator.class);
    private final AccessTokenDecoder accessTokenDecoder = mock(AccessTokenDecoder.class);
    private final ResponseEntity responseEntity = mock(ResponseEntity.class);
    private final IdamAuthorizor idamAuthorizor = mock(IdamAuthorizor.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final String someServiceAuthorizationToken = "some-service-authorization";
    private final String someIdamAccessToken = "some-access-token";
    private final String testSystemPassword = "some-test-user-password";
    private final String testSystemUser = "some-test-user";
    private final String ccdBaseUrl = "some-url";
    private final String userId = "10";

    @Captor ArgumentCaptor<ParameterizedTypeReference> parameterizedTypeReference;
    @Captor ArgumentCaptor<Map<String, String>> urlVariables;
    @Captor ArgumentCaptor<HttpMethod> httpMethod;
    @Captor ArgumentCaptor<HttpEntity> httpEntity;
    @Captor ArgumentCaptor<String> urlCaptor;

    private final AsylumCasesRetriever underTest = new AsylumCasesRetriever(
            ccdBaseUrl,
            testSystemUser,
            testSystemPassword,
            restTemplate,
            idamAuthorizor,
            serviceAuthorizationTokenGenerator,
            accessTokenDecoder);

    @Before
    public void setUp() {

        when(idamAuthorizor.exchangeForAccessToken(testSystemUser, testSystemPassword))
                .thenReturn(someIdamAccessToken);
        when(serviceAuthorizationTokenGenerator.generate())
                .thenReturn(someServiceAuthorizationToken);
        when(accessTokenDecoder.decode(someIdamAccessToken))
                .thenReturn(singletonMap("id", userId));
    }

    @Test
    public void builds_get_asylum_cases_http_request_correctly() {

        when(restTemplate.exchange(
                urlCaptor.capture(),
                httpMethod.capture(),
                httpEntity.capture(),
                parameterizedTypeReference.capture(),
                urlVariables.capture())).thenReturn(responseEntity);

        underTest.getAsylumCasesPage("1");

        verify(idamAuthorizor).exchangeForAccessToken(testSystemUser, testSystemPassword);
        verify(serviceAuthorizationTokenGenerator).generate();
        verify(accessTokenDecoder).decode(someIdamAccessToken);

        assertTrue(urlCaptor.getValue()
                .startsWith(ccdBaseUrl));
        assertThat(urlCaptor.getValue()
                .endsWith(queryStringForAllAppealStatesExcept(APPEAL_STARTED) + "&page=1"));
        assertTrue(httpMethod.getValue()
                .equals(GET));

        assertThat(httpEntity.getValue().getHeaders().get(AUTHORIZATION))
                .containsExactly(someIdamAccessToken);
        assertThat(httpEntity.getValue().getHeaders().get("ServiceAuthorization")
                .equals(someServiceAuthorizationToken));

        assertThat(urlVariables.getValue().get("uid"))
                .isEqualToIgnoringCase(userId);
        assertThat(urlVariables.getValue().get("jid"))
                .isEqualToIgnoringCase("IA");
        assertThat(urlVariables.getValue().get("ctid"))
                .isEqualToIgnoringCase("Asylum");
    }

    @Test
    public void builds_get_asylum_cases_pagination_metadata_http_request_correctly() {

        when(restTemplate.exchange(
                urlCaptor.capture(),
                httpMethod.capture(),
                httpEntity.capture(),
                parameterizedTypeReference.capture(),
                urlVariables.capture())).thenReturn(responseEntity);

        when(responseEntity.getBody()).thenReturn(singletonMap("total_pages_count", "1"));

        int numberOfPages = underTest.getNumberOfPages();

        verify(idamAuthorizor).exchangeForAccessToken(testSystemUser, testSystemPassword);
        verify(serviceAuthorizationTokenGenerator).generate();
        verify(accessTokenDecoder).decode(someIdamAccessToken);

        Assertions.assertThat(numberOfPages).isEqualTo(1);

        assertTrue(urlCaptor.getValue()
                .startsWith(ccdBaseUrl));
        assertThat(urlCaptor.getValue()
                .endsWith(queryStringForAllAppealStatesExcept(APPEAL_STARTED)));
        assertThat(urlCaptor.getValue()
                .contains("pagination_metadata"));
        assertTrue(httpMethod.getValue()
                .equals(GET));

        assertThat(httpEntity.getValue().getHeaders().get(AUTHORIZATION))
                .containsExactly(someIdamAccessToken);
        assertThat(httpEntity.getValue().getHeaders().get("ServiceAuthorization")
                .equals(someServiceAuthorizationToken));

        assertThat(urlVariables.getValue().get("uid"))
                .isEqualToIgnoringCase(userId);
        assertThat(urlVariables.getValue().get("jid"))
                .isEqualToIgnoringCase("IA");
        assertThat(urlVariables.getValue().get("ctid"))
                .isEqualToIgnoringCase("Asylum");
    }

    @Test
    public void throws_correct_exception_when_failed_to_retrieve_metadata_from_ccd() {

        RestClientException underlyingException = mock(RestClientException.class);

        when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(HttpEntity.class),
                Mockito.any(ParameterizedTypeReference.class),
                Mockito.any(Map.class)))
                    .thenThrow(underlyingException);

        try {

            underTest.getNumberOfPages();

        } catch (AsylumCaseRetrievalException e) {
            assertThat(e).hasCause(underlyingException);
            assertThat(e).hasMessage("Couldn't retrieve metadata from CCD");
        }
    }

    @Test
    public void throws_correct_exception_when_failed_to_retrieve_asylum_cases_from_ccd() {

        RestClientException underlyingException = mock(RestClientException.class);

        when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(HttpMethod.class),
                Mockito.any(HttpEntity.class),
                Mockito.any(ParameterizedTypeReference.class),
                Mockito.any(Map.class)))
                .thenThrow(underlyingException);

        try {

            underTest.getAsylumCasesPage("1");

        } catch (AsylumCaseRetrievalException e) {
            assertThat(e).hasCause(underlyingException);
            assertThat(e).hasMessage("Couldn't retrieve asylum cases from CCD");
        }
    }

    private String queryStringForAllAppealStatesExcept(State state) {
        return "?" + stream(State.values())
                .filter(s -> !s.equals(state))
                .map(s -> "state=" + s)
                .collect(joining("&"));
    }
}