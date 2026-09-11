package org.grnet.status.api;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.grnet.endpoint.scanner.runtime.repositories.RoleEndpointRepository;
import org.grnet.status.dtos.Status;
import org.grnet.status.dtos.ams.PublishRequest;
import org.grnet.status.dtos.ams.PublishResponse;
import org.grnet.status.dtos.incident.IncidentRequestDto;
import org.grnet.status.dtos.incident.IncidentResponseDto;
import org.grnet.status.dtos.incident.ServiceDto;
import org.grnet.status.dtos.pagination.PageResource;
import org.grnet.status.dtos.report.FullReportResponseDto;
import org.grnet.status.dtos.report.MiniReportResponse;
import org.grnet.status.dtos.report.PartialReportResponseDto;
import org.grnet.status.dtos.report.WebApiReportResponse;
import org.grnet.status.dtos.tenant.ContactDto;
import org.grnet.status.dtos.tenant.TenantInfoDto;
import org.grnet.status.dtos.tenant.TenantRequestDto;
import org.grnet.status.dtos.tenant.TenantResponseDto;
import org.grnet.status.dtos.tenant.node.WebApiNodeMonitoringMetricResponse;
import org.grnet.status.dtos.tenant.webapi.TenantWebApiCreateResponse;
import org.grnet.status.dtos.tenant.webapi.TenantWebApiGetResponse;
import org.grnet.status.services.NodeService;
import org.grnet.status.services.clients.AmsClient;
import org.grnet.status.services.clients.AmsClientFactory;
import org.grnet.status.services.clients.ArgoWebApiClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(KeycloakComposeResource.class)
public class PublicEndpointTest extends KeycloakTest {
    @InjectMock
    AmsClientFactory amsClientFactory;
    private String currentMockId;
    @InjectMock
    @RestClient
    ArgoWebApiClient argoWebApiClient;
    @Inject
    TestEntitlementProvider entitlementProvider;

    @Inject
    RoleEndpointRepository roleEndpointRepository;

    @InjectMock
    NodeService nodeService;

    @BeforeEach
    public void mockArgoClient() throws Exception {

        when(argoWebApiClient.createTenant(any(), any())).thenAnswer(invocation -> {
            // Use the currentMockId set by the test
            return loadMockTenantResponse(currentMockId);
        });

//        when(argoWebApiClient.getTenant(any(), any())).thenAnswer(invocation -> {
//            // Use the currentMockId set by the test
//            return loadMockTenantGetResponse(currentMockId);
//        });
        when(argoWebApiClient.getTenant(any(), any())).thenAnswer(invocation -> {

            String tenantId = invocation.getArgument(1);

            return loadMockTenantGetResponse(tenantId);
        });

        when(argoWebApiClient.fetchReportsSuperAdmin(
                anyString(),
                anyString(),
                any(),
                any(),
                any()
        )).thenAnswer(invocation -> {

            String tenantId = invocation.getArgument(1);

            return loadMockReportsResponse(tenantId);
        });
    }

    @BeforeEach
    void mockAmsClient() {
        AmsClient mockClient = mock(AmsClient.class);

        when(amsClientFactory.buildClient(anyString()))
                .thenReturn(mockClient);

        var resp = new PublishResponse();
        resp.setMessageIds(List.of("mock-message-id-1"));

        when(mockClient.publish(anyString(), anyString(), anyString(), any(PublishRequest.class)))
                .thenReturn(resp);
    }
    @BeforeEach
    void setupRepo() {
        TestRoleEndpointRepository testRepo = new TestRoleEndpointRepository();
        QuarkusMock.installMockForType(testRepo, RoleEndpointRepository.class);
        this.roleEndpointRepository = testRepo;
    }

    // -------------------------------------------------------------------------
    // RESET STATE
    // -------------------------------------------------------------------------
    @BeforeEach
    void reset() {
        entitlementProvider.reset();
        ((TestRoleEndpointRepository) roleEndpointRepository).reset();
    }

    @BeforeEach
    public void cleanUp() {
        tenantService.deleteAll();
    }


    // -------------------------------------------------------------------------
    // ENTITLEMENTS
    // -------------------------------------------------------------------------
    private void mockSuperAdmin() {
        entitlementProvider.setSuperAdmin(true);
        entitlementProvider.setEntitlements(List.of());
    }

    private TenantWebApiGetResponse loadMockTenantGetResponse(String id) {

        var response = new TenantWebApiGetResponse();

        var data = new TenantWebApiGetResponse.Data();
        data.setId(id);

        var info = new TenantWebApiGetResponse.Info();
        info.setName("LOCALTENANT");
        data.setInfo(info);

        var dbConf = new TenantWebApiGetResponse.DbConf();
        dbConf.setStore("mongodb");
        dbConf.setServer("localhost");
        dbConf.setPort(27017);
        dbConf.setDatabase("test");
        dbConf.setUsername("user");
        dbConf.setPassword("password");

        data.setDb_conf(List.of(dbConf));

        response.setData(List.of(data));

        return response;
    }

    private WebApiReportResponse loadMockReportsResponse(String tenantId) {

        var response = new WebApiReportResponse();

        var status = new WebApiReportResponse.Status();
        status.code = "200";
        status.message = "Success";

        response.status = status;


        var report = new FullReportResponseDto();

        report.id = "report-1";
        report.tenant = tenantId;
        report.disabled = false;


        var info = new FullReportResponseDto.Info();
        info.name = "TEST_REPORT";
        info.description = "Test public report";
        info.created = "2026-01-01 00:00:00";
        info.updated = "2026-01-01 00:00:00";

        report.info = info;


        var computations = new FullReportResponseDto.Computations();
        computations.ar = true;
        computations.status = true;
        computations.trends = List.of("daily");

        report.computations = computations;


        var thresholds = new FullReportResponseDto.Thresholds();
        thresholds.availability = 80;
        thresholds.reliability = 90;
        thresholds.uptime = 0.8;
        thresholds.unknown = 0.1;
        thresholds.downtime = 0.1;

        report.thresholds = thresholds;


        response.data = List.of(report);

        return response;
    }
    private TenantWebApiCreateResponse loadMockTenantResponse(String id) {

        var tenantWebApiResponse = new TenantWebApiCreateResponse();
        var data = new TenantWebApiCreateResponse.Data();
        var link = new TenantWebApiCreateResponse.Links();
        var status = new Status();
        status.setCode("200");
        status.setMessage("Τenant was succesfully created");
        link.setSelf("https://https://test.api.grnet.gr/api/v2/admin/tenants/e1ab046c-8544-47e6-bd8f-e8aa8b83acb3");
        data.setId(id);
        data.setLinks(link);
        tenantWebApiResponse.setData(data);
        tenantWebApiResponse.setStatus(status);
        return tenantWebApiResponse;
    }


    private TenantResponseDto createTenant(String tenantName) {
        var request = new TenantRequestDto();
        var tenantInfo = new TenantInfoDto();
        var tenantContact = new ContactDto();
        tenantInfo.name = tenantName;
        tenantInfo.email = "test@gmail.com";
        tenantInfo.description = "this is test tenant description";
        tenantInfo.image = "https://example/image.png";
        tenantInfo.website = "https://test.tenant.org";

        tenantContact.email = "test@gmail.com";
        tenantContact.name = "Test user";
        tenantContact.type = "ADMIN";

        request.info = tenantInfo;
        request.contacts = Collections.singletonList(tenantContact);

        return given()
                .auth().oauth2(adminToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/admin/tenants")
                .then()
                .statusCode(200)
                .extract()
                .as(TenantResponseDto.class);
    }


    @Test
    public void fetchPublicReportsWithoutAuthentication() {

        currentMockId = "e1ab046c-8544-47e6-bd8f-e8aa8b83acb3";  // dynamically set here

        mockSuperAdmin();

        var tenant = createTenant("LOCALTENANT");

        // No authentication here because endpoint has @PermitAll
        var response =  given()
                .header("Origin", "https://frontend.com")
                .when()
                .get("/v1/public/tenants/{tenant-name}/reports/public", tenant.info.name)
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "*")
                .extract()
                .as(MiniReportResponse[].class);

        assertNotNull(response);
    }

    @Test
    public void fetchPrivateReportsWithAllowedCorsOrigin() {

        currentMockId = "e1ab046c-8544-47e6-bd8f-e8aa8b83acb3";

        mockSuperAdmin();

        var tenant = createTenant("LOCALTENANT");


        var response = given()
                .auth()
                .oauth2(adminToken)
                .header("Origin", "https://frontend.com")
                .basePath("/v1/tenants")//
                .contentType(ContentType.JSON)
                .when()
                .get("/{id}/reports", tenant.id)
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "https://frontend.com")
                .extract()
                .as(PartialReportResponseDto[].class);


        assertNotNull(response);
    }
    @Test
    public void fetchPrivateReportsWithForbiddenCorsOrigin() {

        currentMockId = "e1ab046c-8544-47e6-bd8f-e8aa8b83acb3";

        mockSuperAdmin();

        var tenant = createTenant("LOCALTENANT");
        given()
                .auth()
                .oauth2(adminToken)
                .header("Origin", "https://evil.com")
                .basePath("/v1/tenants")
                .contentType(ContentType.JSON)
                .when()
                .get("/{id}/reports", tenant.id)
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", org.hamcrest.Matchers.nullValue());
    }

    @Test
    public void fetchTenantPublicInformationWithoutAuthentication() {

        currentMockId = "e1ab046c-8544-47e6-bd8f-e8aa8b83acb3";

        mockSuperAdmin();

        var tenant = createTenant("LOCALTENANT");

        given()
                .header("Origin", "https://frontend.com")
                .when()
                .get("/v1/public/tenants/{tenant-name}/info", tenant.info.name)
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "*")
                .body("logo", org.hamcrest.Matchers.equalTo("https://example/image.png"));
    }

    @Test
    public void fetchTenantPublicInformationNotFound() {

        given()
                .when()
                .get("/v1/public/tenants/{tenant-name}/info", "UNKNOWN-TENANT")
                .then()
                .statusCode(404);
    }

    @Test
    public void getMonitoringMetricByServiceWithoutAuthentication() {

        when(nodeService.getMonitoringMetricByService(
                anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(monitoringMetricResponse());

        var response = given()
                .header("Origin", "https://frontend.com")
                .queryParam("granularity", "daily")
                .when()
                .get("/v1/public/nodes/{name}/capabilities/monitoring/metrics/{service-id}", "TENANTB", "CLOUD-B")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "*")
                .extract()
                .as(WebApiNodeMonitoringMetricResponse.class);

        assertNotNull(response);
        assertEquals(1, response.data.size());
        assertEquals("CLOUD-B", response.data.get(0).name);
        assertEquals(BigDecimal.valueOf(100), response.data.get(0).results.get(0).availability);
    }

    @Test
    public void getAllPublicIncidentsWithoutAuthentication() {

        currentMockId = UUID.randomUUID().toString();

        mockSuperAdmin();

        var tenant = createTenant("LOCALTENANT");

        currentMockId = tenant.id;

        createIncident(tenant.id);

        var response = given()
                .header("Origin", "https://frontend.com")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when()
                .get("/v1/public/tenants/{tenant-name}/incidents", tenant.info.name)
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "*")
                .extract()
                .as(PageResource.class);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    private IncidentResponseDto createIncident(String tenantId) {
        return given()
                .auth().oauth2(adminToken)
                .contentType(ContentType.JSON)
                .body(buildIncidentRequest())
                .post("/v1/tenants/{id}/incidents", tenantId)
                .then()
                .statusCode(201)
                .extract()
                .as(IncidentResponseDto.class);
    }

    private IncidentRequestDto buildIncidentRequest() {
        return buildIncidentRequest(
                "ESHOP unavailable",
                "Users cannot access the ESHOP service.",
                "6a6e8037-1e23-4b65-a75a-37d9e8d5bc44",
                "ESHOP"
        );
    }

    private IncidentRequestDto buildIncidentRequest(
            String title,
            String description,
            String serviceId,
            String serviceName) {

        var request = new IncidentRequestDto();

        request.title = title;
        request.description = description;

        var service = new ServiceDto();
        service.id = serviceId;
        service.name = serviceName;

        request.services = List.of(service);

        return request;
    }


    private WebApiNodeMonitoringMetricResponse monitoringMetricResponse() {

        var response = new WebApiNodeMonitoringMetricResponse();

        var data = new WebApiNodeMonitoringMetricResponse.Data();
        data.name = "CLOUD-B";

        var result = new WebApiNodeMonitoringMetricResponse.Result();
        result.date = "2026-08-14";
        result.availability = BigDecimal.valueOf(100);
        result.reliability = BigDecimal.valueOf(100);
        result.uptime = BigDecimal.ONE;

        data.results = List.of(result);
        response.data = List.of(data);

        return response;
    }
}
