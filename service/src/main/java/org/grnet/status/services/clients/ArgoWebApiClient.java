package org.grnet.status.services.clients;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.grnet.status.dtos.LatestDataResponse;
import org.grnet.status.dtos.Status;
import org.grnet.status.dtos.argo.ArgoStatusGroupsResponse;
import org.grnet.status.dtos.profile.aggregation.AggregationProfileResponse;
import org.grnet.status.dtos.profile.metric.MetricProfileResponse;
import org.grnet.status.dtos.profile.operation.OperationProfileResponse;
import org.grnet.status.dtos.readiness.WebApiTenantReadiness;
import org.grnet.status.dtos.report.WebApiReportResponse;
import org.grnet.status.dtos.status.*;
import org.grnet.status.dtos.tenant.node.*;
import org.grnet.status.dtos.tenant.webapi.*;
import org.grnet.status.dtos.topology.*;

import java.util.List;

@RegisterRestClient(configKey = "argo-web-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ArgoWebApiClient {

    @GET
    @Path("/api/v2/reports")
    WebApiReportResponse fetchReports(
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/reports")
    WebApiReportResponse fetchReportsSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("public") String publicReports,
            @QueryParam("private") String privateReports,
            @QueryParam("node") String node
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/reports/{id}")
    WebApiReportResponse fetchReportById(
            @PathParam("id") String id,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/reports/{id}")
    WebApiReportResponse fetchReportByIdSuperAdmin(
            @PathParam("id") String id,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;

    @POST
    @Path("/api/v2/reports/{id}/set-public")
    WebApiNodeReportResponse setReportPublicSuperAdmin(
            @PathParam("id") String reportId,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;

    @POST
    @Path("/api/v2/reports/{id}/set-private")
    WebApiNodeReportResponse setReportPrivateSuperAdmin(
            @PathParam("id") String reportId,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v3/status/{report}")
    ArgoStatusGroupsResponse fetchStatusGroups(
            @HeaderParam("x-api-key") String apiKey,
            @PathParam("report") String report
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v3/status/{report}")
    ArgoStatusGroupsResponse fetchStatusGroupsSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report") String report
    ) throws WebApplicationException, ProcessingException;


    @POST
    @Path("/api/v2/admin/tenants")
    TenantWebApiCreateResponse createTenant(
            @HeaderParam("x-api-key") String apiKey,
            TenantWebApiRequest request
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/admin/tenants/{id}")
    TenantWebApiGetResponse getTenant(
            @HeaderParam("x-api-key") String apiKey,
            @PathParam("id") String id
    ) throws WebApplicationException, ProcessingException;

    @PUT
    @Path("/api/v2/admin/tenants/{id}")
    Status updateTenant(@PathParam("id") String id,
                        @HeaderParam("x-api-key") String apiKey,
                        TenantWebApiRequest request
    ) throws WebApplicationException, ProcessingException;

    @DELETE
    @Path("/api/v2/admin/tenants/{id}")
    Status deleteTenant(
            @PathParam("id") String id,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/admin/tenants")
    TenantWebApiGetResponse getTenants(
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;


    @PUT
    @Path("/api/v2/admin/tenants/{id}/info")
    Status updateTenantInfo(
            @PathParam("id") String id,
            @HeaderParam("x-api-key") String apiKey,
            TenantWebApiRequest request
    ) throws WebApplicationException, ProcessingException;


    @PUT
    @Path("/api/v2/admin/tenants/{id}/topology")
    Status updateTenantTopology(
            @PathParam("id") String id,
            @HeaderParam("x-api-key") String apiKey,
            TenantWebApiRequest request
    ) throws WebApplicationException, ProcessingException;

    @PUT
    @Path("/api/v2/admin/tenants/{id}/db-conf")
    Status updateTenantDBConf(
            @PathParam("id") String id,
            @HeaderParam("x-api-key") String apiKey, TenantWebApiRequest request
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/operations_profiles/{id}")
    OperationProfileResponse listSpecificOperationsProfiles(
            @PathParam("id") String id,
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/operations_profiles/{id}")
    OperationProfileResponse listSpecificOperationsProfilesSuperAdmin(
            @PathParam("id") String id,
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;


    @GET
    @Path("/api/v2/operations_profiles")
    OperationProfileResponse listAllOperationsProfiles(
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/operations_profiles")
    OperationProfileResponse listAllOperationsProfilesSuperAdmin(
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;


    @GET
    @Path("/api/v2/aggregation_profiles/{id}")
    AggregationProfileResponse listSpecificAggregationProfiles(
            @PathParam("id") String id,
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/aggregation_profiles/{id}")
    AggregationProfileResponse listSpecificAggregationProfilesSuperAdmin(
            @PathParam("id") String id,
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/aggregation_profiles")
    AggregationProfileResponse listAllAggregationProfiles(
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/aggregation_profiles")
    AggregationProfileResponse listAllAggregationProfilesSuperAdmin(
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/metric_profiles/{id}")
    MetricProfileResponse listSpecificMetricProfiles(
            @PathParam("id") String id,
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/metric_profiles/{id}")
    MetricProfileResponse listSpecificMetricProfilesSuperAdmin(
            @PathParam("id") String id,
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;


    @GET
    @Path("/api/v2/metric_profiles")
    MetricProfileResponse listAllMetricProfiles(
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/metric_profiles")
    MetricProfileResponse listAllMetricProfilesSuperAdmin(
            @QueryParam("date") String date,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/admin/tenants/{id}/ready")
    WebApiTenantReadiness getTenantReadiness(
            @PathParam("id") String id,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/version")
    @Produces(MediaType.APPLICATION_JSON)
    Response version();

    @GET
    @Path("/api/v2/topology/groups")
    WebApiGroupTopologyResponse fetchTopologyGroupsSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date
    ) throws WebApplicationException, ProcessingException;


    @GET
    @Path("/api/v2/topology/endpoints")
    WebApiEndpointTopologyResponse fetchTopologyEndpointsSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v2/topology/service-types")
    WebApiServiceTypeResponse fetchServiceTypesSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date
    ) throws WebApplicationException, ProcessingException;

    @POST
    @Path("/api/v2/topology/groups")
    Status createTopologyGroupsSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date,
            @QueryParam("force") Boolean force,
            List<GroupTopologyDto> request
    ) throws WebApplicationException, ProcessingException;

    @POST
    @Path("/api/v2/topology/endpoints")
    Status createTopologyEndpointsSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date,
            @QueryParam("force") Boolean force,
            List<EndpointTopologyDto> request
    ) throws WebApplicationException, ProcessingException;

    @DELETE
    @Path("/api/v2/topology/groups")
    Status deleteTopologyGroupsSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date
    ) throws WebApplicationException, ProcessingException;

    @DELETE
    @Path("/api/v2/topology/endpoints")
    Status deleteTopologyEndpointsSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date
    ) throws WebApplicationException, ProcessingException;

    @DELETE
    @Path("/api/v2/topology/service-types")
    Status deleteServiceTypesSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date
    ) throws WebApplicationException, ProcessingException;

    @POST
    @Path("/api/v2/topology/service-types")
    Status createServiceTypesSuperAdmin(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date,
            @QueryParam("force") Boolean force,
            List<ServiceTypeDto> request) throws WebApplicationException, ProcessingException;

    @POST
    @Path("/api/v2/admin/tenants/{id}/node-set")
    WebApiNodeResponse setTenantNode(
            @PathParam("id") String id,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;

    @POST
    @Path("/api/v2/admin/tenants/{id}/node-unset")
    WebApiNodeResponse unsetTenantNode(
            @PathParam("id") String id,
            @HeaderParam("x-api-key") String apiKey
    ) throws WebApplicationException, ProcessingException;


    @POST
    @Path("/api/v2/reports/{id}/set-node-report")
    WebApiNodeReportResponse setNodeReport(
            @PathParam("id") String reportId,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;


    @GET
    @Path("/api/v4/nodes/{nodeName}/capabilities/availability")
    WebApiNodeAvailabilityResponse getNodeAvailabilityCapability(
            @HeaderParam("x-api-key") String accessToken,
            @PathParam("nodeName") String nodeName,
            @QueryParam("date") String date,
            @QueryParam("start_time") String startTime,
            @QueryParam("end_time") String endTime,
            @QueryParam("start_date") String startDate,
            @QueryParam("end_date") String endDate,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v4/nodes/{nodeName}/capabilities/availability/{item}")
    WebApiNodeAvailabilityResponse getNodeAvailabilityCapabilityByService(
            @HeaderParam("x-api-key") String accessToken,
            @PathParam("nodeName") String nodeName,
            @PathParam("item") String item,
            @QueryParam("date") String date,
            @QueryParam("start_time") String startTime,
            @QueryParam("end_time") String endTime,
            @QueryParam("start_date") String startDate,
            @QueryParam("end_date") String endDate,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v4/nodes/{nodeName}/capabilities/status")
    WebApiNodeStatusResponse getNodeStatus(
            @HeaderParam("x-api-key") String accessToken,
            @PathParam("nodeName") String nodeName,
            @QueryParam("start_time") String startTime,
            @QueryParam("end_time") String endTime,
            @QueryParam("history") Boolean history
    );
    @GET
    @Path("/api/v4/nodes/{nodeName}/capabilities/monitoring/metrics")
    WebApiNodeMonitoringMetricResponse getNodeMonitoringMetrics(
            @HeaderParam("x-api-key") String accessToken,
            @PathParam("nodeName") String nodeName,
            @QueryParam("start-date") String startDate,
            @QueryParam("end-date") String endDate,
            @QueryParam("granularity") String granularity
    );
    @GET
    @Path("/api/v4/nodes/{nodeName}/capabilities/monitoring/metrics/{item}")
    WebApiNodeMonitoringMetricResponse getNodeMonitoringMetricsByMetric(
            @HeaderParam("x-api-key") String accessToken,
            @PathParam("nodeName") String nodeName,
            @PathParam("item") String item,
            @QueryParam("start-date") String startDate,
            @QueryParam("end-date") String endDate,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v4/nodes/{nodeName}/capabilities/monitoring/metrics/{service-id}")
    WebApiNodeMonitoringMetricResponse getNodeMonitoringMetricsByService(
            @HeaderParam("x-api-key") String accessToken,
            @PathParam("nodeName") String nodeName,
            @PathParam("service-id") String serviceId,
            @QueryParam("start-date") String startDate,
            @QueryParam("end-date") String endDate,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v4/nodes/{nodeName}/capabilities/status/{item}")
    WebApiNodeStatusResponse getNodeStatusByService(
            @HeaderParam("x-api-key") String accessToken,
            @PathParam("nodeName") String nodeName,
            @PathParam("item") String item,
            @QueryParam("start_time") String startTime,
            @QueryParam("end_time") String endTime,
            @QueryParam("history") Boolean history
    );

    @GET
    @Path("/api/v2/feeds/topology")
    WebApiFeedsTopologyResponse getFeedTopology(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;

    @PUT
    @Path("/api/v2/feeds/topology")
    WebApiFeedsTopologyResponse updateFeedTopology(
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId,
            FeedTopologyDto request
    ) throws WebApplicationException, ProcessingException;

    @GET
    @Path("/api/v4/nodes/{nodeName}/capabilities/summary/{item}")
    WebApiNodeSummaryResponse getNodeSummaryCapability(
            @HeaderParam("x-api-key") String apiKey,
            @PathParam("nodeName") String nodeName,
            @PathParam("item") String item,
            @QueryParam("start_date") String startDate,
            @QueryParam("end_date") String endDate,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v4/results/groups")
    TenantWebApiGroupResultsResponse getGroupResultsSuperAdmin(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("date") String date,
            @QueryParam("period") String period,
            @QueryParam("start_time") String startTime,
            @QueryParam("end_time") String endTime,
            @QueryParam("start_date") String startDate,
            @QueryParam("end_date") String endDate,
            @QueryParam("granularity") String granularity,
            @QueryParam("report") String report
    );

    @GET
    @Path("/api/v4/results/groups/{groupName}")
    TenantWebApiGroupResultsResponse getGroupResultsByGroupSuperAdmin(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("groupName") String groupName,
            @QueryParam("date") String date,
            @QueryParam("period") String period,
            @QueryParam("start_time") String startTime,
            @QueryParam("end_time") String endTime,
            @QueryParam("start_date") String startDate,
            @QueryParam("end_date") String endDate,
            @QueryParam("granularity") String granularity,
            @QueryParam("report") String report
    );

    @GET
    @Path("/api/v4/status/groups")
    TenantWebApiGroupStatusResponse getGroupStatusSuperAdmin(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @QueryParam("start_time") String startTime,
            @QueryParam("end_time") String endTime,
            @QueryParam("history") Boolean history,
            @QueryParam("report") String report
    );

    @GET
    @Path("/api/v4/status/groups/{groupName}")
    TenantWebApiGroupStatusResponse getGroupStatusByGroupSuperAdmin(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("groupName") String groupName,
            @QueryParam("start_time") String startTime,
            @QueryParam("end_time") String endTime,
            @QueryParam("history") Boolean history,
            @QueryParam("report") String report
    );

    @GET
    @Path("/api/v5/results/{report-name}/supergroups")
    TenantWebApiSupergroupsResponse getSupergroupsResultsByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v5/results/{report-name}/supergroups/{supergroup-name}")
    TenantWebApiSupergroupsResponse getSupergroupByNameResultsByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("supergroup-name") String supergroupName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime,
            @QueryParam("granularity") String granularity
    );


    @GET
    @Path("/api/v5/results/{report-name}/groups")
    TenantWebApiGroupResultsByReportResponse getGroupsResultsByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v5/results/{report-name}/groups/{group-name}")
    TenantWebApiGroupResultsByReportResponse getGroupByNameResultsByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v5/results/{report-name}/endpoints")
    TenantWebApiEndpointResultsByReportResponse getEndpointsResultsByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v5/results/{report-name}/endpoints/{endpoint-name}")
    TenantWebApiEndpointResultsByReportResponse getEndpointByNameResultsByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("endpoint-name") String groupName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime,
            @QueryParam("granularity") String granularity
    );


    @GET
    @Path("/api/v5/results/{report-name}/groups/{group-name}/endpoints")
    TenantWebApiGroupEndpointResultsByReportResponse getResultsEndpointsByReportAndGroup(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime,
            @QueryParam("granularity") String granularity
    );
    @GET
    @Path("/api/v5/results/{report-name}/groups/{group-name}/endpoints/{endpoint-name}")
    TenantWebApiGroupEndpointResultsByReportResponse geResultsEndpointsByReportGroupAndEndpoint(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("endpoint-name") String endpointName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime,
            @QueryParam("granularity") String granularity
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups")
    TenantWebApiGroupStatusTimelineResponse getGroupsStatusTimelineByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}")
    TenantWebApiGroupStatusTimelineResponse getGroupStatusTimelineByGroupNameByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );


    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/endpoints")
    TenantWebApiEndpointStatusTimelineResponse getEndpointsStatusTimelineByGroup(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/endpoints/{endpoint-name}")
    TenantWebApiEndpointStatusTimelineResponse getEndpointStatusTimelineByGroupAndName(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("endpoint-name") String endpointName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints")
    TenantWebApiEndpointStatusTimelineResponse getEndpointsStatusTimelineByServiceType(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("service-type-name") String serviceTypeName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}")
    TenantWebApiEndpointStatusTimelineResponse getEndpointStatusTimelineByServiceTypeAndName(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("service-type-name") String serviceTypeName,
            @PathParam("endpoint-name") String endpointName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/service-types")
    TenantWebApiServiceTypeStatusTimelineResponse getServiceTypesStatusTimelineByGroup(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/service-types/{service-type-name}")
    TenantWebApiServiceTypeStatusTimelineResponse getServiceTypeStatusTimelineByName(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("service-type-name") String serviceTypeName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/endpoints/{endpoint-name}/metrics")
    TenantWebApiMetricStatusTimelineResponse getMetricsStatusTimelineByEndpoint(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("endpoint-name") String endpointName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/endpoints/{endpoint-name}/metrics/{metric-name}")
    TenantWebApiMetricStatusTimelineResponse getMetricStatusTimelineByEndpointAndName(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("endpoint-name") String endpointName,
            @PathParam("metric-name") String metricName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}/metrics")
    TenantWebApiMetricStatusTimelineResponse getMetricsStatusTimelineByServiceTypeAndEndpoint(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("service-type-name") String serviceTypeName,
            @PathParam("endpoint-name") String endpointName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}/metrics/{metric-name}")
    TenantWebApiMetricStatusTimelineResponse getMetricStatusTimelineByServiceTypeEndpointAndName(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("service-type-name") String serviceTypeName,
            @PathParam("endpoint-name") String endpointName,
            @PathParam("metric-name") String metricName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}/metrics/{metric-name}/details")
    TenantWebApiMetricStatusDetailsResponse getMetricStatusDetailsByServiceTypeEndpointAndName(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("group-name") String groupName,
            @PathParam("service-type-name") String serviceTypeName,
            @PathParam("endpoint-name") String endpointName,
            @PathParam("metric-name") String metricName,
            @QueryParam("timestamp") String timestamp
    );

    @GET
    @Path("/api/v5/status/{report-name}/endpoints")
    TenantWebApiEndpointStatusTimelineResponse getEndpointsStatusTimelineByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v5/status/{report-name}/endpoints/{endpoint-name}")
    TenantWebApiEndpointStatusTimelineResponse getEndpointsStatusTimelineByEndpointNameByReport(
            @HeaderParam("x-api-key") String accessToken,
            @HeaderParam("x-tenant-id") String tenantId,
            @PathParam("report-name") String reportName,
            @PathParam("endpoint-name") String endpointName,
            @QueryParam("start-time") String startTime,
            @QueryParam("end-time") String endTime
    );

    @GET
    @Path("/api/v2/latest/{report-name}/{group-type}")
    LatestDataResponse listLatestData(
            @PathParam("report-name") String reportName,
            @PathParam("group-type") String groupType,
            @QueryParam("filter") String filter,
            @QueryParam("limit") Integer limit,
            @QueryParam("strict") Boolean strict,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;


    @GET
    @Path("/api/v2/latest/{report-name}/{group-type}/{group-name}")
    LatestDataResponse listLatestDataByGroupName(
            @PathParam("report-name") String reportName,
            @PathParam("group-type") String groupType,
            @PathParam("group-name") String groupName,
            @QueryParam("filter") String filter,
            @QueryParam("limit") Integer limit,
            @QueryParam("strict") Boolean strict,
            @HeaderParam("x-api-key") String apiKey,
            @HeaderParam("x-tenant-id") String tenantId
    ) throws WebApplicationException, ProcessingException;
}
