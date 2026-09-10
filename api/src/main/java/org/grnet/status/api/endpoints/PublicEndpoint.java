package org.grnet.status.api.endpoints;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.grnet.status.api.resolvers.CheckDateFormat;
import org.grnet.status.constraints.NotFoundEntity;
import org.grnet.status.dtos.InformativeResponse;
import org.grnet.status.dtos.downtime.DowntimeResponse;
import org.grnet.status.dtos.report.PartialReportResponseDto;
import org.grnet.status.dtos.setting.SettingResponseDto;
import org.grnet.status.dtos.status.*;
import org.grnet.status.dtos.statuspage.StatusPageConfigDto;
import org.grnet.status.dtos.tenant.PublicTenantInformationResponseDto;
import org.grnet.status.dtos.tenant.node.WebApiNodeMonitoringMetricResponse;
import org.grnet.status.dtos.tenant.node.WebApiNodeStatusResponse;
import org.grnet.status.dtos.tenant.webapi.*;
import org.grnet.status.repositories.DowntimeRepository;
import org.grnet.status.services.*;
import org.grnet.status.services.clients.WebApiService;

import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;

@Tag(name = "Public")
@Path("/v1/public")
public class PublicEndpoint {

    @Inject
    StatusService statusService;

    @Inject
    TenantService tenantService;

    @Inject
    ReportService reportService;

    @Inject
    SettingService settingService;
    @Inject
    WebApiService webApiService;
    @Inject
    DowntimeService downtimeService;

    @Inject
    NodeService nodeService;

    @Inject
    IncidentService incidentService;

    @Operation(
            summary = "Get status page configuration by slug",
            description = "Returns only the public configuration (config field) for the given slug."
    )
    @APIResponse(
            responseCode = "200",
            description = "Configuration found",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = StatusPageConfigDto.class,
                    description = "The stored public configuration object"
            ))
    )
    @APIResponse(
            responseCode = "404",
            description = "Status page not found",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/pages/{slug}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusPageConfig(@PathParam("slug") String slug) {

        var statusPage = statusService.getConfigBySlug(slug);

        return Response.ok(statusPage).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Retrieve tenant group results.",
            description = "Retrieves latest availability and uptime results for all tenant groups."
    )
    @APIResponse(
            responseCode = "200",
            description = "Tenant group results.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiGroupResultsResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Tenant not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/results/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getPublicGroupResults(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-GRNET",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "group", in = QUERY,
                    description = "Optional group name.",
                    example = "WIKI")
            @QueryParam("group")
            String groupName,
            @Parameter(name = "date", in = QUERY,
                    description = "UTC date in YYYY-MM-DD format.",
                    example = "2026-05-21")
            @QueryParam("date")
            @CheckDateFormat(pattern = "yyyy-MM-dd",
                    message = "Valid date format is yyyy-MM-dd.")
            String date,
            @Parameter(name = "period", in = QUERY,
                    description = "Specify the lookback window in days or weeks (e.g. 7d or 2w).",
                    example = "7d")
            @QueryParam("period")
            String period,
            @Parameter(name = "start_time", in = QUERY,
                    description = "UTC time in W3C format.",
                    example = "2026-05-21T12:00:00Z")
            @QueryParam("start_time")
            String startTime,
            @Parameter(name = "end_time", in = QUERY,
                    description = "UTC time in W3C format.",
                    example = "2026-05-22T12:00:00Z")
            @QueryParam("end_time")
            String endTime,
            @Parameter(name = "start_date", in = QUERY,
                    description = "UTC date in YYYY-MM-DD format.",
                    example = "2026-05-20")
            @QueryParam("start_date")
            @CheckDateFormat(pattern = "yyyy-MM-dd",
                    message = "Valid date format is yyyy-MM-dd.")
            String startDate,
            @Parameter(name = "end_date", in = QUERY,
                    description = "UTC date in YYYY-MM-DD format.",
                    example = "2026-05-22")
            @QueryParam("end_date")
            @CheckDateFormat(pattern = "yyyy-MM-dd",
                    message = "Valid date format is yyyy-MM-dd.")
            String endDate,
            @Parameter(name = "granularity", in = QUERY,
                    description = "Granularity of time that will be used to present data. Possible values are monthly, daily.",
                    example = "daily")
            @QueryParam("granularity")
            String granularity,
            @Parameter(name = "report", in = QUERY,
                    description = "Target report name. Optional when the tenant has only one report. " +
                            "Required when the tenant has multiple reports.",
                    example = "BASIC")
            @QueryParam("report")
            String report) {

        var tenant = tenantService.getTenantByName(tenantName);

        var response = tenantService.getGroupResults(tenant.id, groupName, date, period, startTime, endTime, startDate, endDate, granularity, report);

        return Response.ok().entity(response).build();
    }


    @Tag(name = "Public")
    @Operation(
            summary = "Retrieve tenant group status.",
            description = "Retrieves latest status results for a specific tenant group."
    )
    @APIResponse(
            responseCode = "200",
            description = "Tenant group status results.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiGroupStatusResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Tenant not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/tenants/{tenant-name}/status/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getGroupStatusByGroup(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-GRNET",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "group", in = QUERY,
                    description = "Optional group name.",
                    example = "WIKI")
            @QueryParam("group")
            String groupName,
            @Parameter(name = "start_time", in = QUERY,
                    description = "UTC time in W3C format.",
                    example = "2026-05-21T12:00:00Z")
            @QueryParam("start_time")
            String startTime,
            @Parameter(name = "end_time", in = QUERY,
                    description = "UTC time in W3C format.",
                    example = "2026-05-22T12:00:00Z")
            @QueryParam("end_time")
            String endTime,
            @Parameter(name = "history", in = QUERY,
                    description = "Show full history of status timelines.",
                    example = "true")
            @QueryParam("history")
            Boolean history,
            @Parameter(name = "report", in = QUERY,
                    description = "Target report name. Optional when the tenant has only one report. " +
                            "Required when the tenant has multiple reports.",
                    example = "BASIC")
            @QueryParam("report")
            String report) {

        var tenant = tenantService.getTenantByName(tenantName);

        var response = tenantService.getGroupStatus(tenant.id, groupName, startTime, endTime, history, report);

        return Response.ok().entity(response).build();
    }


    @Tag(name = "Public")
    @Operation(summary = "Fetch Public ARGO reports",
            description = "Retrieves public reports from the ARGO Web API.")
    @APIResponse(
            responseCode = "200",
            description = "List of available public reports",
            content = @Content(schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = PartialReportResponseDto.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "409",
            description = "Assessment already exists.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "501",
            description = "Not Implemented.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "502",
            description = "Connection error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/reports/public")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response fetchPublicReports(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-GRNET",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "search", in = QUERY,
                    description = "Search report by name.")
            @QueryParam("search") String search,
            @Parameter(name = "node", in = QUERY,
                    description = "Get node reports.")
            @QueryParam("node") Boolean node) {

        var tenant = tenantService.getTenantByName(tenantName);
        var reports = reportService.fetchReportsByStatus(tenant.id, search, Boolean.TRUE, node);

        return Response.ok(reports).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Retrieve the Performance Monitoring configuration.",
            description = "Exposes performance monitoring base url"
    )
    @APIResponse(
            responseCode = "200",
            description = "The performance monitoring setting was found and returned successfully.",
            content = @Content(schema = @Schema(implementation = SettingResponseDto.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Setting not found.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @GET
    @Path("/settings/performance")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPerformanceSetting() {
        var setting = settingService.getPerformanceSetting();
        return Response.ok().entity(setting).build();
    }


    @Tag(name = "Public")
    @Operation(
            summary = "Get report supergroup results.",
            description = "Exposes availability and reliability results for the supergroups of a tenant's report.")
    @APIResponse(
            responseCode = "200",
            description = "Report supergroup results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiSupergroupsResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/results/{report-name}/supergroups")
    @Produces(MediaType.APPLICATION_JSON)

    public Response getSupergroupsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "reportName",
                    required = true,
                    description = "The name of the report.",
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(name = "start-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T00:00:00Z")
            @QueryParam("start-time")
            String startTime,
            @Parameter(name = "end-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T12:00:00Z")
            @QueryParam("end-time")
            String endTime,
            @Parameter(name = "granularity", in = QUERY,
                    description = "The aggregation granularity of the results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {
        var tenant = tenantService.getTenantByName(tenantName);

        checkPublicReport(tenant.id, reportName);
        var response = tenantService.getSupergroupsByReport(tenant.id, reportName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Get report supergroup results.",
            description = "Exposes availability and reliability results for the supergroups of a tenant's report.")
    @APIResponse(
            responseCode = "200",
            description = "Report supergroup results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiSupergroupsResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/results/{report-name}/supergroups/{supergroup-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSupergroupsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "reportName",
                    required = true,
                    description = "The name of the report.",
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(name = "supergroupName",
                    required = true,
                    description = "The name of the supergroup.",
                    example = "PROJECTA")
            @PathParam("supergroup-name")
            String supergroupName,
            @Parameter(name = "start-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T00:00:00Z")
            @QueryParam("start-time")
            String startTime,
            @Parameter(name = "end-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T12:00:00Z")
            @QueryParam("end-time")
            String endTime,
            @Parameter(name = "granularity", in = QUERY,
                    description = "The aggregation granularity of the results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {

        var tenant = tenantService.getTenantByName(tenantName);

        checkPublicReport(tenant.id, reportName);
        var response = tenantService.getSupergroupByNameByReport(tenant.id, reportName, supergroupName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Get report groups results.",
            description = "Exposes availability and reliability results for the groups services of a tenant's report.")
    @APIResponse(
            responseCode = "200",
            description = "Report groups services results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiGroupResultsByReportResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/results/{report-name}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupsResultsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "reportName",
                    required = true,
                    description = "The name of the report.",
                    example = "BASIC")
            @PathParam("report-name")
            String reportName,
            @Parameter(name = "start-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T00:00:00Z")
            @QueryParam("start-time")
            String startTime,
            @Parameter(name = "end-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T12:00:00Z")
            @QueryParam("end-time")
            String endTime,
            @Parameter(name = "granularity", in = QUERY,
                    description = "The aggregation granularity of the results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {

        var tenant = tenantService.getTenantByName(tenantName);

        checkPublicReport(tenant.id, reportName);
        var response = tenantService.retrieveGroupsResultsByReport(tenant.id, reportName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }


    @Tag(name = "Public")
    @Operation(
            summary = "Get report group results.",
            description = "Exposes availability and reliability results for the groups services of a tenant's report.")
    @APIResponse(
            responseCode = "200",
            description = "Report endpointgroups services results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiGroupResultsByReportResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/results/{report-name}/groups/{group-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupByNameResultsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "reportName",
                    required = true,
                    description = "The name of the report.",
                    example = "BASIC")
            @PathParam("report-name")
            String reportName,
            @Parameter(name = "groupName",
                    required = true,
                    description = "The name of the group.",
                    example = "ARCHIVE")
            @PathParam("group-name")
            String groupName,
            @Parameter(name = "start-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T00:00:00Z")
            @QueryParam("start-time")
            String startTime,
            @Parameter(name = "end-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T12:00:00Z")
            @QueryParam("end-time")
            String endTime,
            @Parameter(name = "granularity", in = QUERY,
                    description = "The aggregation granularity of the results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {

        var tenant = tenantService.getTenantByName(tenantName);

        checkPublicReport(tenant.id, reportName);
        var response = tenantService.retrieveGroupByNameByReport(tenant.id, reportName, groupName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Get report endpoint results.",
            description = "Exposes availability and reliability results for the endpoints of a tenant's report.")
    @APIResponse(
            responseCode = "200",
            description = "Report endpoints results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiEndpointResultsByReportResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/results/{report-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEndpointsResultsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "reportName",
                    required = true,
                    description = "The name of the report.",
                    example = "BASIC")
            @PathParam("report-name")
            String reportName,
            @Parameter(name = "start-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T00:00:00Z")
            @QueryParam("start-time")
            String startTime,
            @Parameter(name = "end-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T12:00:00Z")
            @QueryParam("end-time")
            String endTime,
            @Parameter(name = "granularity", in = QUERY,
                    description = "The aggregation granularity of the results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {
        var tenant = tenantService.getTenantByName(tenantName);

        checkPublicReport(tenant.id, reportName);

        var response = tenantService.retrieveEndpointsResultsByReport(tenant.id, reportName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }


    @Tag(name = "Public")
    @Operation(
            summary = "Get report endpoint results.",
            description = "Exposes availability and reliability results for an endpoint of a tenant's report.")
    @APIResponse(
            responseCode = "200",
            description = "Report endpoint results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiEndpointResultsByReportResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/results/{report-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEndpointByNameResultsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "reportName",
                    required = true,
                    description = "The name of the report.",
                    example = "BASIC")
            @PathParam("report-name")
            String reportName,
            @Parameter(name = "endpointName",
                    required = true,
                    description = "The name of the endpoint.",
                    example = "hostname1.archive.foo")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(name = "start-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T00:00:00Z")
            @QueryParam("start-time")
            String startTime,
            @Parameter(name = "end-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T12:00:00Z")
            @QueryParam("end-time")
            String endTime,
            @Parameter(name = "granularity", in = QUERY,
                    description = "The aggregation granularity of the results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {
        var tenant = tenantService.getTenantByName(tenantName);

        checkPublicReport(tenant.id, reportName);
        var response = tenantService.retrieveEndpointByNameResultsByReport(tenant.id, reportName, endpointName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }

    private void checkPublicReport(String id, String reportName) {
        var reports = webApiService.retrieveReportsWebApi(id, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);
        if (reports.data.isEmpty()) {

            throw new NotFoundException("At least one public report should exist to be able to fetch results");
        }
        reports.data.stream()
                .filter(r -> r.info.name.equals(reportName))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        String.format("Report %s not found in public reports", reportName)
                ));
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Get report group endpoints results.",
            description = "Retrieves availability and reliability results for the group endpoints of a tenant's report and specific group.")
    @APIResponse(
            responseCode = "200",
            description = "Report groups endpoints results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiGroupEndpointResultsByReportResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/results/{report-name}/groups/{group-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupsEndpointResultsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "reportName",
                    required = true,
                    description = "The name of the report.",
                    example = "BASIC")
            @PathParam("report-name")
            String reportName,
            @Parameter(name = "groupName",
                    required = true,
                    description = "The name of the group.",
                    example = "ARCHIVE")
            @PathParam("group-name")
            String groupName,

            @Parameter(name = "start-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T00:00:00Z")
            @QueryParam("start-time")
            String startTime,
            @Parameter(name = "end-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T12:00:00Z")
            @QueryParam("end-time")
            String endTime,
            @Parameter(name = "granularity", in = QUERY,
                    description = "The aggregation granularity of the results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {

        var tenant = tenantService.getTenantByName(tenantName);

        checkPublicReport(tenant.id, reportName);
        var response = tenantService.retrieveResultsEndpointByReportAndGroup(tenant.id, reportName, groupName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }


    @Tag(name = "Public")
    @Operation(
            summary = "Get report group endpoints results by endpoint.",
            description = "Retrieves availability and reliability results for the group endpoints of a tenant's report and specific group and a specific endpoint.")
    @APIResponse(
            responseCode = "200",
            description = "Report groups endpoint results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantWebApiGroupEndpointResultsByReportResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/results/{report-name}/groups/{group-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupsEndpointResultsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "reportName",
                    required = true,
                    description = "The name of the report.",
                    example = "BASIC")
            @PathParam("report-name")
            String reportName,
            @Parameter(name = "groupName",
                    required = true,
                    description = "The name of the group.",
                    example = "ARCHIVE")
            @PathParam("group-name")
            String groupName,

            @Parameter(name = "endpointName",
                    required = true,
                    description = "The name of the endpoint.",
                    example = "hostname1.project-a.foo")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(name = "start-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T00:00:00Z")
            @QueryParam("start-time")
            String startTime,
            @Parameter(name = "end-time", in = QUERY,
                    required = true,
                    description = "UTC time in W3C format.",
                    example = "2026-07-02T12:00:00Z")
            @QueryParam("end-time")
            String endTime,
            @Parameter(name = "granularity", in = QUERY,
                    description = "The aggregation granularity of the results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {

        var tenant = tenantService.getTenantByName(tenantName);

        checkPublicReport(tenant.id, reportName);

        var response = tenantService.retrieveResultsEndpointByReportGroupAndEndpoint(tenant.id, reportName, groupName, endpointName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch tenant public information.",
            description = "Retrieves public information about a tenant.")
    @APIResponse(
            responseCode = "200",
            description = "Tenant public information retrieved.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = PublicTenantInformationResponseDto.class)))
    @APIResponse(
            responseCode = "404",
            description = "Tenant not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/tenants/{tenant-name}/info")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response fetchTenantPublicInformation(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-GRNET",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName) {

        var tenant = tenantService.getTenantPublicInformation(tenantName);

        return Response.ok(tenant).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch downtimes for a tenant.",
            description = "Returns the tenant's downtimes"
    )
    @APIResponse(
            responseCode = "200",
            description = "Downtimes fetched successfully.",
            content = @Content(schema = @Schema(implementation = TenantEndpoint.PageableDowntimes.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request payload.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Downtimes not found.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/downtimes")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll

    public Response getDowntimes(
            @Parameter(
                    name = "page",
                    in = ParameterIn.QUERY,
                    description = "Indicates the page number. Page number must be >= 1.")
            @DefaultValue("1")
            @Min(value = 1, message = "Page number must be >= 1.")
            @QueryParam("page")
            int page,

            @Parameter(
                    name = "size",
                    in = ParameterIn.QUERY,
                    description = "The page size.")
            @DefaultValue("10")
            @Min(value = 1, message = "Page size must be between 1 and 100.")
            @Max(value = 100, message = "Page size must be between 1 and 100.")
            @QueryParam("size")
            int size,

            @Parameter(
                    name = "date",
                    in = ParameterIn.QUERY,
                    required = false,
                    description = "Filter downtimes active on this date (UTC).",
                    example = "2026-07-06")
            @CheckDateFormat(
                    pattern = "yyyy-MM-dd",
                    message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("date")
            String date,

            @Parameter(
                    name = "start_date",
                    in = ParameterIn.QUERY,
                    required = false,
                    description = "Start date of the filtering period (UTC).",
                    example = "2026-07-01")
            @CheckDateFormat(
                    pattern = "yyyy-MM-dd",
                    message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("start_date")
            String startDate,

            @Parameter(
                    name = "end_date",
                    in = ParameterIn.QUERY,
                    required = false,
                    description = "End date of the filtering period (UTC).",
                    example = "2026-07-10")
            @CheckDateFormat(
                    pattern = "yyyy-MM-dd",
                    message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("end_date")
            String endDate,
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Context UriInfo uriInfo) {

        var tenant = tenantService.getTenantByName(tenantName);

        if (!Boolean.TRUE.equals(tenant.publicDowntime)) {
            throw new ForbiddenException(
                    "Public downtime information is disabled for tenant '" + tenantName + "'."
            );
        }
        var response = downtimeService.fetchDowntimesByPageAndSize(
                page - 1,
                size,
                tenant.id,
                date,
                startDate,
                endDate,
                uriInfo
        );

        return Response.ok(response).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch a specific downtime for a tenant.",
            description = "Returns the tenant's specific downtime"
    )
    @APIResponse(
            responseCode = "200",
            description = "Downtimes fetched successfully.",
            content = @Content(schema = @Schema(implementation = DowntimeResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request payload.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Downtimes not found.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/downtimes/{downtime-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll

    public Response getDowntime(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    description = "The ID of the downtime.",
                    required = true,
                    example = "13a1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("downtime-id")
            @Valid @NotFoundEntity(repository = DowntimeRepository.class, message = "There is no Downtime with the following id: ")
            String downtimeId,
            @Context UriInfo uriInfo) {
        var tenant = tenantService.getTenantByName(tenantName);

        if (!Boolean.TRUE.equals(tenant.publicDowntime)) {
            throw new ForbiddenException(
                    "Public downtime information is disabled for tenant '" + tenantName + "'."
            );
        }
        var response = downtimeService.fetchDowntimes(tenant.id, downtimeId);
        return Response.ok(response).build();
    }
    @Tag(name = "Public")
    @Operation(
            summary = "Get monitoring metric results for node services.",
            description = "Retrieve monitoring metric results for a node’s services from Argo Web API.")
    @APIResponse(
            responseCode = "200",
            description = "Monitoring metrics results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeStatusResponse.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/nodes/{name}/capabilities/monitoring/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getMonitoringMetric(
            @Parameter(description = "The name of the Node.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("name")
            String nodeName,
            @Parameter(name = "start-date", in = QUERY,
                    description = "Start date in W3C format.")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("start-date")
            String startDate,

            @Parameter(name = "end-date", in = QUERY,
                    description = "End date in W3C format.")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("end-date")
            String endDate,
            @Parameter(name = "granularity", in = QUERY,
                    description = "Granularity of results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {


        var status = nodeService.getMonitoringMetricNodeName(nodeName,  startDate, endDate,granularity);

        return Response.ok().entity(status).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Get monitoring metric results for a node service.",
            description = "Retrieve monitoring metric results for a single node service from Argo Web API.")
    @APIResponse(
            responseCode = "200",
            description = "Monitoring metric results retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeMonitoringMetricResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Node or service not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/nodes/{name}/capabilities/monitoring/metrics/{service-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getMonitoringMetricByService(
            @Parameter(
                    description = "The name of the Node.",
                    required = true,
                    example = "TENANTB",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("name")
            String nodeName,
            @Parameter(
                    description = "The ID of the service.",
                    required = true,
                    example = "CLOUD-B",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("service-id")
            String serviceId,
            @Parameter(
                    name = "start-date",
                    in = QUERY,
                    description = "Start date in W3C format.")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("start-date")
            String startDate,
            @Parameter(
                    name = "end-date",
                    in = QUERY,
                    description = "End date in W3C format.")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("end-date")
            String endDate,
            @Parameter(
                    name = "granularity",
                    in = QUERY,
                    description = "Granularity of results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {

        var metrics = nodeService.getMonitoringMetricByService(nodeName, serviceId, startDate, endDate, granularity);

        return Response.ok().entity(metrics).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch status timelines for all groups.",
            description = "Returns the status timelines for all groups of the specified report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Group status timelines fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiGroupStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Status groups not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusGroupsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);


        var groupTimelines = statusService.retrieveStatusTimelineGroupsByReport(tenant.id, reportName, startTime, endTime);

        return Response.ok(groupTimelines).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timeline for a group.",
            description = "Returns the status timeline for a specific group of the specified report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Group status timeline fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiGroupStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Status group not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusGroupByNameByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);

        var groupTimelines = statusService.retrieveStatusTimelineGroupByNameByReport(tenant.id, reportName, groupName, startTime, endTime);

        return Response.ok(groupTimelines).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timelines for service types.",
            description = "Returns the status timelines for all service types of a specific group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Service type status timelines fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiServiceTypeStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Service types not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/service-types")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusServiceTypesByGroup(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);
        var serviceTypeTimelines = statusService.retrieveStatusTimelineServiceTypesByGroup(tenant.id, reportName, groupName, startTime, endTime);

        return Response.ok(serviceTypeTimelines).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timeline for a service type.",
            description = "Returns the status timeline for a specific service type of the specified group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Service type status timeline fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiServiceTypeStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Service type not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusServiceTypeByName(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "service-type-name",
                    description = "The name of the service type.",
                    required = true,
                    example = "webportal")
            @PathParam("service-type-name")
            String serviceTypeName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);
        var serviceTypeTimeline = statusService.retrieveStatusTimelineServiceTypeByName(tenant.id, reportName, groupName, serviceTypeName, startTime, endTime);

        return Response.ok(serviceTypeTimeline).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timelines for endpoints.",
            description = "Returns the status timelines for all endpoints of a specific group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Endpoint status timelines fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiEndpointStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Endpoints not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusEndpointsByGroup(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);


        var endpointTimelines = statusService.retrieveStatusTimelineEndpointsByGroup(
                tenant.id, reportName, groupName, startTime, endTime);

        return Response.ok(endpointTimelines).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timeline for an endpoint.",
            description = "Returns the status timeline for a specific endpoint of the specified group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Endpoint status timeline fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiEndpointStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Endpoint not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusEndpointByGroupAndName(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "endpoint-name",
                    description = "The name of the endpoint.",
                    required = true,
                    example = "host1.example_ID1")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);


        var endpointTimeline = statusService.retrieveStatusTimelineEndpointByGroupAndName(tenant.id, reportName, groupName, endpointName, startTime, endTime);

        return Response.ok(endpointTimeline).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timelines for endpoints under a service type.",
            description = "Returns the status timelines for all endpoints of a specific service type, group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Endpoint status timelines fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiEndpointStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Endpoints not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusEndpointsByServiceType(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "service-type-name",
                    description = "The name of the service type.",
                    required = true,
                    example = "webportal")
            @PathParam("service-type-name")
            String serviceTypeName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);
        var endpointTimelines = statusService.retrieveStatusTimelineEndpointsByServiceType(tenant.id, reportName, groupName, serviceTypeName, startTime, endTime);

        return Response.ok(endpointTimelines).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timeline for an endpoint under a service type.",
            description = "Returns the status timeline for a specific endpoint of the specified service type, group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Endpoint status timeline fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiEndpointStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Endpoint not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusEndpointByServiceTypeAndName(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "service-type-name",
                    description = "The name of the service type.",
                    required = true,
                    example = "webportal")
            @PathParam("service-type-name")
            String serviceTypeName,
            @Parameter(
                    name = "endpoint-name",
                    description = "The name of the endpoint.",
                    required = true,
                    example = "wiki.example.foo_ID3")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);
        var endpointTimeline = statusService.retrieveStatusTimelineEndpointByServiceTypeAndName(
                tenant.id, reportName, groupName, serviceTypeName, endpointName, startTime, endTime);

        return Response.ok(endpointTimeline).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timelines for metrics.",
            description = "Returns the status timelines for all metrics of a specific endpoint, group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Metric status timelines fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiMetricStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Metrics not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/endpoints/{endpoint-name}/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusMetricsByEndpoint(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "endpoint-name",
                    description = "The name of the endpoint.",
                    required = true,
                    example = "wiki.example.foo_ID3")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);

        var metricTimelines = statusService.retrieveStatusTimelineMetricsByEndpoint(tenant.id, reportName, groupName, endpointName, startTime, endTime);

        return Response.ok(metricTimelines).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timeline for a metric.",
            description = "Returns the status timeline for a specific metric of the specified endpoint, group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Metric status timeline fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiMetricStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Metric not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/endpoints/{endpoint-name}/metrics/{metric-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusMetricByEndpointAndName(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "endpoint-name",
                    description = "The name of the endpoint.",
                    required = true,
                    example = "wiki.example.foo_ID3")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(
                    name = "metric-name",
                    description = "The name of the metric.",
                    required = true,
                    example = "generic.http.connect")
            @PathParam("metric-name")
            String metricName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);

        var metricTimeline = statusService.retrieveStatusTimelineMetricByEndpointAndName(tenant.id, reportName, groupName, endpointName, metricName, startTime, endTime);

        return Response.ok(metricTimeline).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timelines for metrics under a service type.",
            description = "Returns the status timelines for all metrics of a specific endpoint, service type, group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Metric status timelines fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiMetricStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Metrics not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusMetricsByServiceTypeAndEndpoint(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "service-type-name",
                    description = "The name of the service type.",
                    required = true,
                    example = "webportal")
            @PathParam("service-type-name")
            String serviceTypeName,
            @Parameter(
                    name = "endpoint-name",
                    description = "The name of the endpoint.",
                    required = true,
                    example = "wiki.example.foo_ID3")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);


        var metricTimelines = statusService.retrieveStatusTimelineMetricsByServiceTypeAndEndpoint(tenant.id, reportName, groupName, serviceTypeName, endpointName, startTime, endTime);

        return Response.ok(metricTimelines).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timeline for a metric under a service type.",
            description = "Returns the status timeline for a specific metric of the specified endpoint, service type, group and report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Metric status timeline fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiMetricStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Metric not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}/metrics/{metric-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusMetricByServiceTypeEndpointAndName(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "service-type-name",
                    description = "The name of the service type.",
                    required = true,
                    example = "webportal")
            @PathParam("service-type-name")
            String serviceTypeName,
            @Parameter(
                    name = "endpoint-name",
                    description = "The name of the endpoint.",
                    required = true,
                    example = "wiki.example.foo_ID3")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(
                    name = "metric-name",
                    description = "The name of the metric.",
                    required = true,
                    example = "generic.http.connect")
            @PathParam("metric-name")
            String metricName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);

        var metricTimeline = statusService.retrieveStatusTimelineMetricByServiceTypeEndpointAndName(tenant.id, reportName, groupName, serviceTypeName, endpointName, metricName, startTime, endTime);

        return Response.ok(metricTimeline).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch status result details for a metric under a service type.",
            description = "Returns the details of a specific metric status result for the specified endpoint, service type, group and report."
    )
    @APIResponse(
            responseCode = "200",
            description = "Metric status result details fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiMetricStatusDetailsResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Metric status result not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}/metrics/{metric-name}/details")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusMetricDetailsByServiceTypeEndpointAndName(
            @Parameter(
                    name = "tenant-name",
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            @Valid
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "group-name",
                    description = "The name of the group.",
                    required = true,
                    example = "WIKI")
            @PathParam("group-name")
            String groupName,
            @Parameter(
                    name = "service-type-name",
                    description = "The name of the service type.",
                    required = true,
                    example = "webportal")
            @PathParam("service-type-name")
            String serviceTypeName,
            @Parameter(
                    name = "endpoint-name",
                    description = "The name of the endpoint.",
                    required = true,
                    example = "wiki.example.foo_e17eb908-15be-403c-86ec-2fdb8bcfb523")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(
                    name = "metric-name",
                    description = "The name of the metric.",
                    required = true,
                    example = "generic.http.connect")
            @PathParam("metric-name")
            String metricName,
            @Parameter(
                    name = "timestamp",
                    description = "Timestamp of the metric status result in UTC.",
                    example = "2026-08-26T00:00:00Z",
                    required = true)
            @QueryParam("timestamp")
            @NotNull String timestamp) {

        var tenant = tenantService.getTenantByName(tenantName);

        var metricDetails = statusService.retrieveStatusMetricDetailsByServiceTypeEndpointAndName(
                tenant.id, reportName, groupName, serviceTypeName, endpointName, metricName, timestamp);

        return Response.ok(metricDetails).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch status timelines for all endpoints.",
            description = "Returns the status timelines for all endpoints of the specified report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Endpoint status timelines fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiEndpointStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Status groups not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getStatusEndpointsByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);

        var endpointTimelines = statusService.retrieveStatusTimelineEndpointsByReport(tenant.id, reportName, startTime, endTime);

        return Response.ok(endpointTimelines).build();
    }

    @Tag(name = "Public")
    @Operation(
            summary = "Fetch the status timeline for an endpoint.",
            description = "Returns the status timeline for a specific endpoint of the specified report within the requested time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "Endpoint status timeline fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = TenantWebApiEndpointStatusTimelineResponse.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User not authenticated.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Status group not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/tenants/{tenant-name}/status/{report-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusEndpointByNameByReport(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(
                    name = "report-name",
                    description = "The name of the report.",
                    required = true,
                    example = "CORE")
            @PathParam("report-name")
            String reportName,
            @Parameter(
                    name = "endpoint-name",
                    description = "The name of the endpoint.",
                    required = true,
                    example = "archive.example.foo_service-6wzv3n7")
            @PathParam("endpoint-name")
            String endpointName,
            @Parameter(
                    name = "start-time",
                    description = "Start of the requested time range in UTC.",
                    example = "2026-07-27T00:59:59Z",
                    required = true)
            @QueryParam("start-time")
            @NotNull String startTime,
            @Parameter(
                    name = "end-time",
                    description = "End of the requested time range in UTC.",
                    example = "2026-07-27T23:59:59Z",
                    required = true)
            @QueryParam("end-time")
            @NotNull String endTime) {
        var tenant = tenantService.getTenantByName(tenantName);

        var groupTimelines = statusService.retrieveStatusTimelineEndpointByNameByReport(tenant.id, reportName, endpointName, startTime, endTime);

        return Response.ok(groupTimelines).build();
    }

    @Operation(
            summary = "Get all incidents.",
            description = "Retrieves a paginated list of incidents belonging to the specified tenant. Results can be filtered by incident title or service name."
    )
    @APIResponse(
            responseCode = "200",
            description = "Incidents retrieved.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantEndpoint.PageableIncidents.class
            ))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid pagination parameters.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "404",
            description = "Tenant not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "500",
            description = "Incidents could not be retrieved.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/tenants/{tenant-name}/incidents")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getAllIncidents(
            @Parameter(
                    description = "The name of the tenant.",
                    required = true,
                    example = "TENANT-TEST",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("tenant-name")
            String tenantName,
            @Parameter(name = "page", in = QUERY,
                    description = "Indicates the page number. Page number must be >= 1.")
            @DefaultValue("1") @Min(value = 1, message = "Page number must be >= 1.")
            @QueryParam("page")
            int page,
            @Parameter(name = "size", in = QUERY,
                    description = "The page size.")
            @DefaultValue("10") @Min(value = 1, message = "Page size must be between 1 and 100.") @Max(value = 100, message = "Page size must be between 1 and 100.")
            @QueryParam("size")
            int size,
            @Parameter(name = "search", in = QUERY,
                    description = "Optional text used to search incidents by title or service name.")
            @QueryParam("search")
            String search,
            @Parameter(name = "date", in = QUERY,
                    description = "Optional creation date used to filter incidents. Format: YYYY-MM-DD.",
                    example = "2026-07-21")
            @QueryParam("date")
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            String date,
            @Context UriInfo uriInfo) {

        var tenant = tenantService.getTenantByName(tenantName);

        var response = incidentService.getIncidentsByPageAndSize(tenant.id, page - 1, size, search, date, uriInfo);

        return Response.ok(response).build();
    }
}
