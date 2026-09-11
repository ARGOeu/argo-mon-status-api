package org.grnet.status.api.endpoints;

import io.quarkus.security.Authenticated;
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
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.grnet.endpoint.scanner.runtime.ParamRef;
import org.grnet.endpoint.scanner.runtime.ParamType;
import org.grnet.endpoint.scanner.runtime.Scope;
import org.grnet.endpoint.scanner.runtime.SecuredEndpoint;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.GroupUserResponse;
import org.grnet.endpoint.scanner.runtime.context.RoleEndpointHolder;
import org.grnet.status.api.resolvers.CheckDateFormat;
import org.grnet.status.constraints.NotFoundEntity;
import org.grnet.status.constraints.ValidLatestDataStatusFilter;
import org.grnet.status.dtos.InformativeResponse;
import org.grnet.status.dtos.LatestDataResponse;
import org.grnet.status.dtos.Status;
import org.grnet.status.dtos.downtime.DowntimeRequest;
import org.grnet.status.dtos.downtime.DowntimeResponse;
import org.grnet.status.dtos.general.ExistResponseDto;
import org.grnet.status.dtos.incident.*;
import org.grnet.status.dtos.incident.IncidentRequestDto;
import org.grnet.status.dtos.incident.IncidentResponseDto;
import org.grnet.status.dtos.pagination.PageResource;
import org.grnet.status.dtos.profile.aggregation.AggregationProfileResponse;
import org.grnet.status.dtos.profile.metric.MetricProfileResponse;
import org.grnet.status.dtos.profile.operation.OperationProfileResponse;
import org.grnet.status.dtos.project.ProjectResponseDto;
import org.grnet.status.dtos.readiness.WebApiTenantReadiness;
import org.grnet.status.dtos.report.FullReportResponseDto;
import org.grnet.status.dtos.report.PartialReportResponseDto;
import org.grnet.status.dtos.status.*;
import org.grnet.status.dtos.statuspage.StatusPageRequestDto;
import org.grnet.status.dtos.statuspage.StatusPageResponseDto;
import org.grnet.status.dtos.statuspage.StatusPageUpdateRequestDto;
import org.grnet.status.dtos.tenant.TenantRequestDto;
import org.grnet.status.dtos.tenant.TenantResponseDto;
import org.grnet.status.dtos.tenant.alerts.AlertDefinitionRequest;
import org.grnet.status.dtos.tenant.invitations.TenantInvitationRequest;
import org.grnet.status.dtos.tenant.invitations.TenantInvitationResponse;
import org.grnet.status.dtos.tenant.node.*;
import org.grnet.status.dtos.tenant.status.TenantStatusDto;
import org.grnet.status.dtos.tenant.status.TenantStatusFullResponse;
import org.grnet.status.dtos.tenant.webapi.*;
import org.grnet.status.dtos.tenant.webapi.TenantWebApiGroupStatusResponse;
import org.grnet.status.dtos.tenant.webapi.TenantWebApiNodeRequest;
import org.grnet.status.dtos.tenant.webapi.TenantWebApiSupergroupsResponse;
import org.grnet.status.dtos.topology.EndpointTopologyDto;
import org.grnet.status.dtos.topology.FeedTopologyDto;
import org.grnet.status.dtos.topology.GroupTopologyDto;
import org.grnet.status.dtos.topology.ServiceTypeDto;
import org.grnet.status.enums.resources.*;
import org.grnet.status.repositories.*;
import org.grnet.status.services.*;
import org.grnet.status.util.Utility;

import java.io.IOException;
import java.util.List;

import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;

@Path("/v1/tenants")
@Authenticated
@Tag(name = "Tenant")
@SecurityScheme(
        securitySchemeName = "Authentication",
        description = "JWT token",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class TenantEndpoint {

    @Inject
    Utility utility;

    @Inject
    TenantService tenantService;

    @Inject
    TenantProjectService tenantProjectService;

    @Inject
    TenantInvitationService tenantInvitationService;

    @Inject
    ReportService reportService;

    @Inject
    StatusService statusService;

    @Inject
    StatusPageService statusPageService;

    @Inject
    ProfileService profileService;

    @Inject
    ContactService contactService;

    @Inject
    TopologyService topologyService;

    @Inject
    DowntimeService downtimeService;

    @Inject
    IncidentService incidentService;


    @Operation(
            summary = "List Tenants Available to the User",
            description = "Retrieves a paginated list of tenants the authenticated user is allowed to access."
    )
    @APIResponse(
            responseCode = "200",
            description = "Tenants list retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = PageableTenants.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @SecurityRequirement(name = "Authentication")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint
    public Response listTenants(
            @Parameter(name = "search", in = QUERY,
                    description = "Search tenants by name.")
            @QueryParam("search") String search,
            @Parameter(name = "sort", in = QUERY,
                    schema = @Schema(type = SchemaType.STRING, defaultValue = ""),
                    examples = {
                            @ExampleObject(name = "Tenant Name", value = "name"),
                            @ExampleObject(name = "Created At", value = "createdAt")},
                    description = "The field used to sort the results.")
            @DefaultValue("createdAt")
            @QueryParam("sort")
            String sort,
            @Parameter(name = "order", in = QUERY,
                    schema = @Schema(type = SchemaType.STRING),
                    examples = {
                            @ExampleObject(name = "Ascending", value = "ASC"),
                            @ExampleObject(name = "Descending", value = "DESC")},
                    description = "The order of the sorted results.")
            @DefaultValue("DESC")
            @QueryParam("order")
            String order,
            @Parameter(name = "page", in = QUERY,
                    description = "Page number. Must be >= 1.")
            @DefaultValue("1")
            @Min(value = 1, message = "Page number must be >= 1.")
            @QueryParam("page")
            int page,
            @Parameter(name = "size", in = QUERY,
                    description = "Page size.")
            @DefaultValue("10")
            @Min(value = 1, message = "Page size must be between 1 and 100.")
            @Max(value = 100, message = "Page size must be between 1 and 100.")
            @QueryParam("size")
            int size, @Context UriInfo uriInfo) {

        var result = tenantService.listAuthorizedTenants(page - 1, size, uriInfo, search, sort, order);

        return Response.ok().entity(result).build();
    }

    @Operation(
            summary = "Get Tenant By Id .",
            description = "Returns a specific tenant assessment.")
    @APIResponse(
            responseCode = "200",
            description = "The corresponding tenant.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantResponseDto.class)))
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
            description = "Entity Not Found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {


                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )

    public Response getTenant(
            @Parameter(description = "The ID of the tenant to retrieve.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id) {

        var tenant = tenantService.getTenantById(id);

        return Response.ok().entity(tenant).build();
    }

    @Operation(
            summary = "Update a tenant.",
            description = "Updates a specific tenant."
    )
    @APIResponse(
            responseCode = "200",
            description = "Tenant updated successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantResponseDto.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Page not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "409",
            description = "Tenant already exists.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response updateTenant(
            @Parameter(
                    description = "The ID of the tenant to retrieve.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Valid @NotNull(message = "The request body is empty.") TenantRequestDto request) throws IOException {

        var updated = tenantService.updateTenant(id, request);
        return Response.ok().entity(updated).build();
    }

    @Operation(summary = "List projects added to tenant",
            description = "Retrieves a list of projects that tenant belongs")
    @APIResponse(
            responseCode = "200",
            description = "Tenants list retrieved",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = ProjectResponseDto.class)))
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
            description = "Project does not exist.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/projects")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getProjectsByTenant(
            @Parameter(
                    description = "The ID of the project to retrieve.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Parameter(name = "search", in = QUERY,
                    description = "The \"search\" parameter is a query parameter that allows clients to specify a text string that will be used to search for matches in specific fields in Project entity. " +
                            "The search will be conducted in the following fields : projects' name. ")
            @QueryParam("search") String search,
            @Parameter(name = "sort", in = QUERY,
                    schema = @Schema(type = SchemaType.STRING, defaultValue = ""),
                    examples = {
                            @ExampleObject(name = "Project name", value = "name"),
                            @ExampleObject(name = "Created At", value = "createdAt")},
                    description = "The \"sort\" parameter allows clients to specify the field by which they want the results to be sorted.")
            @DefaultValue("createdAt")
            @QueryParam("sort")
            String sort,
            @Parameter(name = "order", in = QUERY,
                    schema = @Schema(type = SchemaType.STRING, defaultValue = ""),
                    examples = {@ExampleObject(name = "Ascending", value = "ASC"), @ExampleObject(name = "Descending", value = "DESC")},
                    description = "The \"order\" parameter specifies the order in which the sorted results should be returned.") @DefaultValue("DESC")
            @QueryParam("order")
            String order,
            @Parameter(name = "page", in = QUERY,
                    description = "Indicates the page number. Page number must be >= 1.") @DefaultValue("1") @Min(value = 1, message = "Page number must be >= 1.")
            @QueryParam("page")
            int page,
            @Parameter(name = "size", in = QUERY,
                    description = "The page size.")
            @DefaultValue("10") @Min(value = 1, message = "Page size must be between 1 and 100.") @Max(value = 100, message = "Page size must be between 1 and 100.")
            @QueryParam("size")
            int size,
            @Context UriInfo uriInfo) {

        var project = tenantProjectService.getProjectsByTenant(id, page - 1, size, uriInfo, search, sort, order);

        return Response.ok().entity(project).build();
    }

    @Operation(summary = "List tenant members.",
            description = "Retrieves a list of tenant members and related metadata.")
    @APIResponse(
            responseCode = "200",
            description = "Tenant members list retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = PageableTenantMembers.class)))
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
            description = "Tenant does not exist.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/members")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getMembersByTenant(
            @Parameter(
                    description = "The ID of the tenant to retrieve.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(name = "page", in = QUERY,
                    description = "Indicates the page number. Page number must be >= 1.") @DefaultValue("1") @Min(value = 1, message = "Page number must be >= 1.")
            @QueryParam("page")
            int page,
            @Parameter(name = "size", in = QUERY,
                    description = "The page size.")
            @DefaultValue("10") @Min(value = 1, message = "Page size must be between 1 and 100.") @Max(value = 100, message = "Page size must be between 1 and 100.")
            @QueryParam("size")
            int size,
            @Context UriInfo uriInfo) {

        var members = tenantService.getMembersByTenant(id, page - 1, size, uriInfo);

        return Response.ok().entity(members).build();
    }

    @Tag(name = "Tenant")
    @Operation(summary = "Invite a user to be a member of tenant.",
            description = "Invite a user to be a member of tenant.")
    @APIResponse(
            responseCode = "200",
            description = "Mail send",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantInvitationResponse.class)))
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
            description = "Invitation already exists.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @POST
    @Path("/{id}/invitation")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response createInvitation(
            @Parameter(
                    description = "The ID of the tenant to create an invitation.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Valid @NotNull(message = "The request body is empty.")
            TenantInvitationRequest request) {

        var status = tenantInvitationService.createInvitation(id, request, utility.getUserUniqueIdentifier());
        return Response.ok().entity(status).build();
    }

    @Tag(name = "Tenant")
    @Operation(
            summary = "Get all invitations of tenant.",
            description = "Returns all invitation of a Tenant. "
    )
    @APIResponse(
            responseCode = "200",
            description = "Invitation details.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = PageableTenantInvitations.class)))
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
            description = "Invitation not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "410",
            description = "Invitation expired.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/{id}/invitations")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getInvitations(
            @Parameter(
                    description = "The ID of the tenant under which the invitation was created.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            String id,
            @Parameter(name = "search", in = QUERY,
                    description = "Search invitations by role or email.")
            @QueryParam("search") String search,
            @Parameter(name = "sort", in = QUERY,
                    schema = @Schema(type = SchemaType.STRING, defaultValue = "createdAt"),
                    examples = {
                            @ExampleObject(name = "Created At", value = "createdAt"),
                            @ExampleObject(name = "Email", value = "email"),
                            @ExampleObject(name = "Status", value = "status")
                    },
                    description = "The field used to sort the results.")
            @DefaultValue("createdAt")
            @QueryParam("sort") String sort,
            @Parameter(
                    name = "order",
                    in = QUERY,
                    schema = @Schema(type = SchemaType.STRING, defaultValue = "DESC"),
                    examples = {
                            @ExampleObject(name = "Ascending", value = "ASC"),
                            @ExampleObject(name = "Descending", value = "DESC")
                    },
                    description = "The order of the sorted results.")
            @DefaultValue("DESC")
            @QueryParam("order") String order,
            @Parameter(name = "page", in = QUERY,
                    description = "Page number. Must be >= 1.")
            @DefaultValue("1")
            @Min(value = 1, message = "Page number must be >= 1.")
            @QueryParam("page")
            int page,
            @Parameter(name = "size", in = QUERY,
                    description = "Page size.")
            @DefaultValue("10")
            @Min(value = 1, message = "Page size must be between 1 and 100.")
            @Max(value = 100, message = "Page size must be between 1 and 100.")
            @QueryParam("size")
            int size, @Context UriInfo uriInfo) {
        var response = tenantInvitationService.getInvitationsByTenantByPageAndSize(search, sort, order, id, page - 1, size, uriInfo);

        return Response.ok(response).build();
    }

    @Tag(name = "Tenant")
    @Operation(
            summary = "Revoke an invitation.",
            description = "Revoke an invitation."
    )
    @APIResponse(
            responseCode = "200",
            description = "Invitation updated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantInvitationResponse.class)))
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
            description = "Invitation not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "409",
            description = "Invitation already responded.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "410",
            description = "Invitation expired.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @PATCH
    @Path("/{id}/invitations/{invitation_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response revoke(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @PathParam("invitation_id")
            @Valid @NotFoundEntity(repository = TenantInvitationRepository.class, message = "There is no Invitation with the following invitation_id: ")
            String invitationId) {

        var response = tenantInvitationService.revokeInvitation(id, invitationId, utility.getUserUniqueIdentifier());

        return Response.ok(response).build();
    }

    @Tag(name = "Profiles")
    @Operation(
            summary = "Get a specific aggregation profile.",
            description = "List one specific operations profile targeted by it's unique id.")
    @APIResponse(
            responseCode = "200",
            description = "Aggregation profile found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = AggregationProfileResponse.class)))
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
            description = "Aggregation profile not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/aggregation-profiles/{profile_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "profile_id",
                            type = ParamType.PATH,
                            referTo = AggregationProfileResource.class
                    )
            }
    )
    public Response listSpecificAggregationProfiles(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Parameter(description = "The aggregation profile id.",
                    required = true,
                    example = "profile-id",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("profile_id") String profileId,
            @Parameter(name = "date", in = QUERY, description = "Target date to retrieve a historic version of the profile.") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date) {

        var response = profileService.listSpecificAggregationProfiles(id, profileId, date);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Profiles")
    @Operation(
            summary = "List all aggregation profiles.",
            description = "List all aggregation profiles.")
    @APIResponse(
            responseCode = "200",
            description = "List of aggregation profiles.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = AggregationProfileResponse.class)))
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
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/aggregation-profiles")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response listAllAggregationProfiles(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Parameter(name = "date", in = QUERY, description = "Target date to retrieve a historic version of the profile.") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date) {

        var response = profileService.listAllAggregationProfiles(id, date);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Profiles")
    @Operation(
            summary = "Get a specific metric profile.",
            description = "List one specific metric profile targeted by it's unique id.")
    @APIResponse(
            responseCode = "200",
            description = "Metric profile found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = MetricProfileResponse.class)))
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
            description = "Metric profile not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/metric-profiles/{profile_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "profile_id",
                            type = ParamType.PATH,
                            referTo = MetricProfileResource.class
                    )

            }
    )
    public Response listSpecificMetricProfiles(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Parameter(description = "The metric profile id.",
                    required = true,
                    example = "profile-id",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("profile_id") String profileId,
            @Parameter(name = "date", in = QUERY, description = "Target date to retrieve a historic version of the profile.") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date) {

        var response = profileService.listSpecificMetricProfiles(id, profileId, date);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Profiles")
    @Operation(
            summary = "List all metric profiles.",
            description = "List all metric profiles.")
    @APIResponse(
            responseCode = "200",
            description = "List of metric profiles.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = MetricProfileResponse.class)))
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
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/metric-profiles")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response listAllMetricProfiles(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Parameter(name = "date", in = QUERY, description = "Target date to retrieve a historic version of the profile.") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date) {

        var response = profileService.listAllMetricProfiles(id, date);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Profiles")
    @Operation(
            summary = "Get a specific operations profile.",
            description = "List one specific operations profile targeted by it's unique id.")
    @APIResponse(
            responseCode = "200",
            description = "Operations profile found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = OperationProfileResponse.class)))
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
            description = "Operations profile not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/operations-profiles/{profile_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),

                    @ParamRef(
                            param = "profile_id",
                            type = ParamType.PATH,
                            referTo = OperationsProfileResource.class
                    )
            }
    )
    public Response listSpecificOperationsProfiles(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Parameter(description = "The operations profile id.",
                    required = true,
                    example = "profile-id",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("profile_id") String profileId,
            @Parameter(name = "date", in = QUERY, description = "Target date to retrieve a historic version of the profile.") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date) {

        var response = profileService.listSpecificOperationsProfiles(id, profileId, date);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Profiles")
    @Operation(
            summary = "List all operations profiles.",
            description = "List all operations profiles.")
    @APIResponse(
            responseCode = "200",
            description = "List of operations profiles.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = OperationProfileResponse.class)))
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
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/operations-profiles")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response listAllOperationsProfiles(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Parameter(name = "date", in = QUERY, description = "Target date to retrieve a historic version of the profile.") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date) {

        var response = profileService.listAllOperationsProfiles(id, date);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Reports")
    @Operation(summary = "Fetch ARGO reports",
            description = "Retrieves reports from the ARGO Web API.")
    @APIResponse(
            responseCode = "200",
            description = "List of available reports",
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/reports")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response fetchReports(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(name = "search", in = QUERY,
                    description = "Search report by name.")
            @QueryParam("search") String search,
            @Parameter(name = "public", in = QUERY,
                    description = "Retrieve only public reports.",
                    example = "true")
            @QueryParam("public")
            Boolean publicReports) {

        var reports = reportService.fetchReports(id, search, publicReports, false);

        return Response.ok(reports).build();
    }

    @Tag(name = "Reports")
    @Operation(summary = "Fetch Tenant' s report By Report ID",
            description = "Retrieves the report with the specific Report ID, for a tenant with specific Tenant ID,  from the ARGO Web API.")
    @APIResponse(
            responseCode = "200",
            description = "The report retrieved",
            content = @Content(schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = FullReportResponseDto.class)))
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/reports/{report-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "report-id",
                            type = ParamType.PATH,
                            referTo = ReportsResource.class
                    )
            }
    )
    public Response fetchReportByID(
            @Parameter(description = "The ID of the tenant to retrieve report.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(description = "The ID of the report to retrieve.",
                    required = true,
                    example = "cf010255-cda3-49d8-92d1-926c2c6cf9eb",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("report-id")
            @Valid String reportId) {

        var reports = reportService.fetchReportById(id, reportId);

        return Response.ok(reports).build();
    }

    @Tag(name = "Reports")
    @Operation(
            summary = "Set default node report",
            description = "Sets the specified report as the default node report for the given tenant."
    )
    @APIResponse(
            responseCode = "200",
            description = "Node report updated successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeReportResponse.class)))
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
            description = "Tenant or report not found.",
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
            responseCode = "502",
            description = "Connection error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/reports/{report-id}/set-node-report")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "report-id",
                            type = ParamType.PATH,
                            referTo = ReportsResource.class
                    )
            }
    )
    public Response setNodeReport(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The ID of the report.",
                    required = true,
                    example = "cf010255-cda3-49d8-92d1-926c2c6cf9eb",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("report-id") @Valid String reportId) {

        var response = reportService.setNodeReport(id, reportId);

        return Response.ok(response).build();
    }

    @Tag(name = "Reports")
    @Operation(summary = "Fetch status groups for a report",
            description = "Decrypts the provided secret key and retrieves report  groups from the ARGO Web API.")
    @APIResponse(
            responseCode = "200",
            description = "List of available reports",
            content = @Content(schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = StatusGroupResponseDto.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/{id}/reports/{report-id}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),

                    @ParamRef(
                            param = "report-id",
                            type = ParamType.PATH,
                            referTo = ReportsResource.class
                    )
            }
    )
    public Response fetchStatusGroups(
            @Parameter(
                    description = "The ID of the tenant to retrieve report.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The ID of the report to retrieve.",
                    required = true,
                    example = "cf010255-cda3-49d8-92d1-926c2c6cf9eb",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("report-id") @Valid String reportId) {

        var reports = statusService.getStatusGroups(id, reportId);

        return Response.ok(reports).build();
    }

    @Tag(name = "Status Pages")
    @Operation(
            summary = "Create a new status page.",
            description = "This endpoint allows an authenticated user to create a new ARGO Status Page."
    )
    @APIResponse(
            responseCode = "201",
            description = "Status Page created successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = StatusPageResponseDto.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request payload.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "409",
            description = "Slug already exists.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @POST
    @Path("/{id}/pages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response createStatusPage(
            @Parameter(
                    description = "The ID of the tenant to create pages under.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Valid @NotNull(message = "The request body is empty.")
            StatusPageRequestDto request,
            @Context UriInfo uriInfo) {

        var response = statusPageService.createStatusPage(id, request, utility.getUserUniqueIdentifier());

        return Response.created(uriInfo.getAbsolutePathBuilder().path(response.id).build()).entity(response).build();
    }

    @Tag(name = "Status Pages")
    @Operation(
            summary = "Get status page by page id.",
            description = "Returns a specific status page."
    )
    @APIResponse(
            responseCode = "200",
            description = "The corresponding status page.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = StatusPageResponseDto.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Page not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/{id}/pages/{page-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "page-id",
                            type = ParamType.PATH,
                            referTo = org.grnet.status.enums.resources.PageResource.class
                    )
            }
    )

    public Response getStatusPage(
            @Parameter(
                    description = "The ID of the tenant to retrieve report.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The ID of the status page to retrieve.",
                    required = true,
                    example = "e7ab046c-8544-47e6-bd8f-e8aa8b83acb0",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("page-id")
            @Valid @NotFoundEntity(repository = StatusPageRepository.class, message = "There is no Status Page with the following id:")
            String pageId) {

        var page = statusPageService.getStatusPageById(pageId);
        return Response.ok().entity(page).build();
    }

    @Tag(name = "Status Pages")
    @Operation(
            summary = "List status pages per tenant with pagination.",
            description = "Returns paginated list of status pages for the authenticated user."
    )
    @APIResponse(
            responseCode = "200",
            description = "List of status pages.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = PageableStatusPages.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @GET
    @Path("/{id}/pages")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }, scope = {Scope.ALL, Scope.MINE}
    )
    //@SecuredEndpoint
    public Response listStatusPages(
            @Parameter(
                    description = "The ID of the tenant to retrieve pages.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(name = "page", in = QUERY,
                    description = "Indicates the page number. Page number must be >= 1.")
            @DefaultValue("1") @Min(value = 1, message = "Page number must be >= 1.") @QueryParam("page") int page,
            @Parameter(name = "size", in = QUERY,
                    description = "The page size.")
            @DefaultValue("10") @Min(value = 1, message = "Page size must be between 1 and 100.")
            @Max(value = 100, message = "Page size must be between 1 and 100.") @QueryParam("size") int size,
            @Context UriInfo uriInfo) {


        var roles = RoleEndpointHolder.get();
        var pages = statusPageService.getStatusPageByUserAndPage(roles, page - 1, size, uriInfo, id, utility.getUserUniqueIdentifier());

        return Response.ok().entity(pages).build();
    }

    @Tag(name = "Status Pages")
    @Operation(
            summary = "Update a status page.",
            description = "Updates a specific status page."
    )
    @APIResponse(
            responseCode = "200",
            description = "Page updated successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = StatusPageResponseDto.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Page not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "409",
            description = "Slug already exists.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @PUT
    @Path("/{id}/pages/{page-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),

                    @ParamRef(
                            param = "page-id",
                            type = ParamType.PATH,
                            referTo = org.grnet.status.enums.resources.PageResource.class
                    )
            }
    )
    public Response updateStatusPage(
            @Parameter(
                    description = "The ID of the tenant to retrieve report.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(description = "The ID of the status page to update.",
                    required = true,
                    example = "e7ab046c-8544-47e6-bd8f-e8aa8b83acb0",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("page-id") String pageId,
            @Valid @NotNull(message = "The request body is empty.")
            StatusPageUpdateRequestDto request) {

        var updated = statusPageService.updateStatusPage(id, pageId, request);
        return Response.ok().entity(updated).build();
    }

    @Tag(name = "Status Pages")
    @Operation(
            summary = "Delete a status page.",
            description = "Deletes a specific status page."
    )
    @APIResponse(
            responseCode = "200",
            description = "Deletion completed.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Page not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @DELETE
    @Path("/{id}/pages/{page-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "page-id",
                            type = ParamType.PATH,
                            referTo = org.grnet.status.enums.resources.PageResource.class
                    )
            }
    )
    public Response deleteStatusPage(
            @Parameter(
                    description = "The ID of the tenant to retrieve report.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(
                    description = "The ID of the status page to delete.",
                    required = true,
                    example = "e7ab046c-8544-47e6-bd8f-e8aa8b83acb0",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("page-id") String pageId) {

        statusPageService.deleteStatusPage(pageId);

        var informativeResponse = new InformativeResponse();
        informativeResponse.code = 200;
        informativeResponse.message = "Status Page has been successfully deleted.";

        return Response.ok().entity(informativeResponse).build();
    }

    @Tag(name = "Status Pages")
    @Operation(summary = "Check if a status page slug exists",
            description = "Returns true if a status page with the given slug exists.")
    @APIResponse(
            responseCode = "200",
            description = "Slug existence response",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = ExistResponseDto.class))
    )
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class))
    )
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/pages/check-slug/{slug}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response checkSlugExists(
            @Parameter(
                    description = "The ID of the tenant to retrieve report.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @PathParam("slug") String slug) {

        var exists = statusPageService.slugExists(slug);

        return Response.ok(exists).build();
    }

    @Tag(name = "Tenant")
    @Operation(
            summary = "Check tenant's readiness.",
            description = "Returns true if tenant is ready or false if it is not. "
    )
    @APIResponse(
            responseCode = "200",
            description = "Tenant's readiness details.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiTenantReadiness.class)))
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
            description = "Tenant's Readiness not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @GET
    @Path("/{id}/check-readiness")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response checkReadiness(
            @Parameter(
                    description = "The ID of the tenant to check readiness.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            String id) {
        var response = tenantService.checkReadiness(id);

        return Response.ok(response).build();
    }

    @Tag(name = "Tenant")
    @Operation(
            summary = "Update Tenant's node information.",
            description = "Returns true if tenant is ready or false if it is not. "
    )
    @APIResponse(
            responseCode = "200",
            description = "Tenant's readiness details.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeResponse.class)))
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
            description = "Tenant's Readiness not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @PUT
    @Path("/{id}/set-node")
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTenantNode(
            @Parameter(
                    description = "The ID of the tenant to check readiness.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Valid @NotNull(message = "The request body is empty.")
            TenantWebApiNodeRequest request) {

        var response = tenantService.updateTenantNode(id, request);

        return Response.ok(response).build();
    }

    @Tag(name = "Tenant")
    @Operation(
            summary = "Update Tenant's feed topology information.",
            description = "Updates the tenant topology feed configuration."
    )
    @APIResponse(
            responseCode = "200",
            description = "Tenant feed topology updated successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
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
            description = "Tenant's Readiness not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @PUT
    @Path("/{id}/feeds/topology")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response updateTenantFeedTopology(
            @Parameter(
                    description = "The ID of the tenant to check readiness.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @RequestBody(
                    required = true,
                    description = "Feed topology configuration. The tenant database configuration must be completed before this request is executed.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = FeedTopologyDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "EOSC service catalog",
                                            value = """
                                                    {
                                                      "type": "eosc-service-catalog",
                                                      "feed_service_groups": "https://somewhere2.foo.bar/service_groups",
                                                      "feed_service_endpoints": "https://somewhere2.foo.bar/service_endpoints",
                                                      "feed_service_endpoints_extensions": "https://somewhere2.foo.bar/service_endpoints_extensions"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "CSV",
                                            value = """
                                                    {
                                                      "type": "CSV",
                                                      "feed_url": "https://docs.google.com/spreadsheets/d/1xiptZgYG2bn78hwBCEP7esTDyfMvvEXFLfJY2HblfI8/export?gid=0&format=csv",
                                                      "paginated": "false",
                                                      "fetch_type": [
                                                        "ServiceGroups"
                                                      ],
                                                      "uid_endpoints": ""
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Desy-Marketplace",
                                            value = """
                                                    {
                                                      "type": "desy-marketplace",
                                                      "feed_url": "https://desy-marketplace"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Node-Registry",
                                            value = """
                                                    {
                                                      "type": "node-registry"
                                                    }
                                                    """
                                    )

                            }
                    )
            )
            @Valid @NotNull(message = "The request body is empty.")
            FeedTopologyDto request) {

        var feedTopologyResponse = tenantService.updateFeedTopology(id, request);

        var response = new InformativeResponse();
        response.code = Integer.parseInt(feedTopologyResponse.status.getCode());
        response.message = feedTopologyResponse.status.getMessage();

        return Response.ok(response).build();
    }

    @Tag(name = "Tenant")
    @Operation(
            summary = "Retrieve Tenant's feed topology information.",
            description = "Returns the feed topology configuration for the specified tenant."
    )
    @APIResponse(
            responseCode = "200",
            description = "Tenant feed topology retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = FeedTopologyDto.class)))
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
    @Path("/{id}/feeds/topology")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getTenantFeedTopology(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id) {

        var response = tenantService.getFeedTopology(id);

        return Response.ok(response).build();
    }

    public static class PageableTenants extends PageResource<TenantResponseDto> {

        private List<TenantResponseDto> content;

        @Override
        public List<TenantResponseDto> getContent() {
            return content;
        }

        @Override
        public void setContent(List<TenantResponseDto> content) {
            this.content = content;
        }
    }

    public static class PageableTenantMembers extends PageResource<GroupUserResponse> {

        private List<GroupUserResponse> content;

        @Override
        public List<GroupUserResponse> getContent() {
            return content;
        }

        @Override
        public void setContent(List<GroupUserResponse> content) {
            this.content = content;
        }
    }

    public static class PageableTenantInvitations extends PageResource<TenantInvitationResponse> {

        private List<TenantInvitationResponse> content;

        @Override
        public List<TenantInvitationResponse> getContent() {
            return content;
        }

        @Override
        public void setContent(List<TenantInvitationResponse> content) {
            this.content = content;
        }
    }

    public static class PageableStatusPages extends PageResource<StatusPageResponseDto> {

        private List<StatusPageResponseDto> content;

        @Override
        public List<StatusPageResponseDto> getContent() {
            return content;
        }

        @Override
        public void setContent(List<StatusPageResponseDto> content) {
            this.content = content;
        }
    }


    @Tag(name = "Tenant")
    @Operation(summary = "Notify AMS to initialize the automation process of check readiness for a tenant",
            description = "Notify AMS to initialize automation process of check readiness.")
    @APIResponse(
            responseCode = "201",
            description = "Process initialized",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantStatusDto.class)))
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
            description = "Project already exists.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/notify-ams-check-readiness")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response notifyAms(
            @Parameter(
                    description = "The ID of the tenant to start automation process.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)) @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Valid @NotNull(message = "The request body is empty.")
            AlertDefinitionRequest request) {

        var status = tenantService.notifyAmsCheckReadiness(id, request);
        return Response.ok().entity(status).build();
    }


    @Tag(name = "Tenant")
    @Operation(
            summary = "Get Tenant's status By Id .",
            description = "Returns a specific tenant's status.")
    @APIResponse(
            responseCode = "200",
            description = "The corresponding tenant's status.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = TenantStatusFullResponse.class)))
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
            description = "Entity Not Found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )

    public Response getTenantStatus(@Parameter(
            description = "The ID of the tenant to retrieve status.",
            required = true,
            example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("id")
                                    @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id) {

        var status = tenantService.getTenantStatus(id);

        return Response.ok().entity(status).build();
    }

    @Tag(name = "Tenant")
    @Operation(
            summary = "Get list of contact types.",
            description = "This endpoint returns a list of contact types ")
    @APIResponse(
            responseCode = "200",
            description = "List of contact types existing.",
            content = @Content(schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = List.class)))
    @APIResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
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
            description = "Entity Not Found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/contact-types")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getContactTypes() {

        var contactTypes = contactService.getContactTypes();

        return Response.ok().entity(contactTypes).build();
    }

    @Tag(name = "Topologies")
    @Operation(summary = "Fetch ARGO group topologies",
            description = "Retrieves tenant's group topologies from the ARGO Web API.")
    @APIResponse(
            responseCode = "200",
            description = "List of available group topologies",
            content = @Content(schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = List.class)))
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
            description = "Topology already exists.",
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/topology/groups")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response fetchGroupTopologies(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(name = "date", in = QUERY, description = "Target date to retrieve a group topology ") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date) {

        var topologies = topologyService.fetchGroupTopologies(id, date);

        return Response.ok(topologies).build();
    }

    @Tag(name = "Topologies")
    @Operation(summary = "Fetch ARGO endpoint topologies",
            description = "Retrieves tenant's endpoint topologies from the ARGO Web API.")
    @APIResponse(
            responseCode = "200",
            description = "List of available topologies",
            content = @Content(schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = List.class)))
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
            description = "Topology already exists.",
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/topology/endpoints")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response fetchEndpointTopologies(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(name = "date", in = QUERY, description = "Target date to retrieve a endpoint topology ") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date
    ) {

        var topologies = topologyService.fetchEndpointTopologies(id, date);

        return Response.ok(topologies).build();
    }

    @Tag(name = "Topologies")
    @Operation(summary = "Fetch ARGO service types",
            description = "Retrieves tenant's service types from the ARGO Web API.")
    @APIResponse(
            responseCode = "200",
            description = "List of available service types",
            content = @Content(schema = @Schema(
                    type = SchemaType.ARRAY,
                    implementation = List.class)))
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
            description = "Topology already exists.",
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/topology/service-types")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response fetchServiceTypes(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(name = "date", in = QUERY, description = "Target date to retrieve service types ") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date
    ) {

        var topologies = topologyService.fetchServiceTypes(id, date);

        return Response.ok(topologies).build();
    }

    @Tag(name = "Topologies")
    @Operation(summary = "Create ARGO group topologies",
            description = "Retrieves tenant's group topologies to the ARGO Web API.")
    @APIResponse(
            responseCode = "201",
            description = "Succeeded",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Status.class)))
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
            description = "Topology already exists.",
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
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/topology/groups")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response createGroupTopologies(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(name = "date", in = QUERY, description = "Target date to create a group topology ") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.")
            String date,
            @Parameter(name = "force", in = QUERY,
                    description = "Overwrite existing topology entries.",
                    example = "true",
                    schema = @Schema(type = SchemaType.BOOLEAN))
            @QueryParam("force")
            Boolean force,
            @Valid @NotNull(message = "The request body is empty.") List<GroupTopologyDto> request) {

        var topologies = topologyService.createGroupTopology(id, date, force, request);

        return Response.ok().entity(topologies).build();
    }


    @Tag(name = "Topologies")
    @Operation(summary = "Create ARGO endpoint topologies",
            description = "Retrieves tenant's endpoint topologies to the ARGO Web API.")
    @APIResponse(
            responseCode = "201",
            description = "Succeeded",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Status.class)))
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
            description = "Topology already exists.",
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
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/topology/endpoints")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response createEndpointTopologies(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(name = "date", in = QUERY, description = "Target date to create an endpoint topology ")
            @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.")
            String date,
            @Parameter(name = "force", in = QUERY,
                    description = "Overwrite existing topology entries.",
                    example = "true",
                    schema = @Schema(type = SchemaType.BOOLEAN))
            @QueryParam("force")
            Boolean force,
            @Valid @NotNull(message = "The request body is empty.")
            List<EndpointTopologyDto> request) {

        var topologies = topologyService.createEndpointTopology(id, date, force, request);

        return Response.ok().entity(topologies).build();
    }

    @Tag(name = "Topologies")
    @Operation(summary = "Create ARGO endpoint topologies",
            description = "Retrieves tenant's endpoint topologies to the ARGO Web API.")
    @APIResponse(
            responseCode = "201",
            description = "Succeeded",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Status.class)))
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
            description = "Topology already exists.",
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
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/topology/service-types")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response createServiceTypes(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(name = "date", in = QUERY, description = "Target date to create service types ") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.")
            String date,
            @Parameter(name = "force", in = QUERY,
                    description = "Overwrite existing topology entries.",
                    example = "true",
                    schema = @Schema(type = SchemaType.BOOLEAN))
            @QueryParam("force")
            Boolean force,
            @Valid @NotNull(message = "The request body is empty.") List<ServiceTypeDto> request) {

        var topologies = topologyService.createServiceTypes(id, date, force, request);

        return Response.ok().entity(topologies).build();
    }


    @Tag(name = "Topologies")
    @Operation(
            summary = "Delete group topology  from a tenant.",
            description = "Delete group topology  from a tenant.")
    @APIResponse(
            responseCode = "200",
            description = "Group topology deleted successfully from a tenant.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
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
            description = "Entity Not Found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @DELETE
    @Path("/{id}/topology/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response deleteGroupTopologies(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,

            @Parameter(name = "date", in = QUERY, description = "Target date to delete a group topology ") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date
    ) {

        var topologies = topologyService.deleteGroupTopology(id, date);

        return Response.ok().entity(topologies).build();
    }

    @Tag(name = "Topologies")
    @Operation(
            summary = "Delete endpoint topology from a tenant.",
            description = "Delete endpoint topology from a tenant.")
    @APIResponse(
            responseCode = "200",
            description = "Endpoint topology  deleted successfully from a tenant.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
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
            description = "Entity Not Found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @DELETE
    @Path("/{id}/topology/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response deleteEndpointTopologies(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,

            @Parameter(name = "date", in = QUERY, description = "Target date to delete endpoint topology ") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date
    ) {

        var topologies = topologyService.deleteEndpointTopology(id, date);

        return Response.ok().entity(topologies).build();
    }

    @Tag(name = "Topologies")
    @Operation(
            summary = "Delete service types  from a tenant.",
            description = "Delete service types  from a tenant.")
    @APIResponse(
            responseCode = "200",
            description = "Service types deleted successfully from a tenant.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
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
            description = "Entity Not Found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @DELETE
    @Path("/{id}/topology/service-types")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response deleteServiceTypes(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,

            @Parameter(name = "date", in = QUERY, description = "Target date to delete service types") @QueryParam("date")
            @Valid @CheckDateFormat(pattern = "yyyy-mm-dd", message = "Valid date format is yyyy-mm-dd.") String date
    ) {

        var topologies = topologyService.deleteServiceTypes(id, date);

        return Response.ok().entity(topologies).build();
    }

    @Tag(name = "Capabilities")
    @Operation(
            summary = "Get monitoring metrics results for node services.",
            description = "Retrieve monitoring  metrics for a node’s services from Argo Web API.")
    @APIResponse(
            responseCode = "200",
            description = "Monitoring metrics retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeAvailabilityResponse.class)))
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/capabilities/monitoring/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getMonitoring(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(name = "item", in = QUERY,
                    description = "Metric name to target under the node.",
                    example = "ARCHIVE")
            @QueryParam("item")
            String item,
            @Parameter(name = "start-date", in = QUERY,
                    description = "Start date in W3C format.",
                      example = "2026-08-05"
             )
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("start-date")
            String startDate,
            @Parameter(name = "end-date", in = QUERY,
                    description = "End date in W3C format.",
                    example = "2026-08-05")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("end-date")
            String endDate,
            @Parameter(name = "granularity", in = QUERY,
                    description = "Granularity of results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {

        var availability = tenantService.getMonitoring(id, item,startDate, endDate, granularity);

        return Response.ok().entity(availability).build();
    }

    @Tag(name = "Capabilities")
    @Operation(
            summary = "Get monitoring metrics results for a tenant service.",
            description = "Retrieve monitoring metrics for a single service from Argo Web API.")
    @APIResponse(
            responseCode = "200",
            description = "Monitoring metrics retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeMonitoringMetricResponse.class)))
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
            description = "Tenant or service not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/capabilities/monitoring/metrics/{service-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getMonitoringByService(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "6b36d6d3-56a3-48a5-93af-aecf3e16a7c6",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(
                    description = "The ID of the service.",
                    required = true,
                    example = "CLOUD-B",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("service-id") String serviceId,
            @Parameter(
                    name = "start-date",
                    in = QUERY,
                    description = "Start date in UTC.",
                    example = "2026-08-05")
            @Valid @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("start-date") String startDate,
            @Parameter(
                    name = "end-date",
                    in = QUERY,
                    description = "End date in UTC.",
                    example = "2026-08-05")
            @Valid @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            @QueryParam("end-date") String endDate,
            @Parameter(
                    name = "granularity",
                    in = QUERY,
                    description = "Granularity of results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity") String granularity) {

        var metrics = tenantService.getMonitoringByService(id, serviceId, startDate, endDate, granularity);

        return Response.ok().entity(metrics).build();
    }

    @Tag(name = "Capabilities")
    @Operation(
            summary = "Get availability results for node services.",
            description = "Retrieve availability metrics for a node’s services from Argo Web API.")
    @APIResponse(
            responseCode = "200",
            description = "Availability retrieved successfully.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeAvailabilityResponse.class)))
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/capabilities/availability")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getAvailability(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(name = "item", in = QUERY,
                    description = "Service name to target under the node.",
                    example = "WIKI")
            @QueryParam("item")
            String item,
            @Parameter(name = "date", in = QUERY,
                    description = "Target date (YYYY-MM-DD).")
            @QueryParam("date")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            String date,
            @Parameter(name = "start_time", in = QUERY,
                    description = "Start time in W3C format.")
            @QueryParam("start_time")
            String startTime,
            @Parameter(name = "end_time", in = QUERY,
                    description = "End time in W3C format.")
            @QueryParam("end_time")
            String endTime,
            @Parameter(name = "start_date", in = QUERY,
                    description = "Start date (YYYY-MM-DD).")
            @QueryParam("start_date")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            String startDate,
            @Parameter(name = "end_date", in = QUERY,
                    description = "End date (YYYY-MM-DD).")
            @QueryParam("end_date")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            String endDate,
            @Parameter(name = "granularity", in = QUERY,
                    description = "Granularity of results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity) {

        var availability = tenantService.getAvailability(id, item, date, startTime, endTime, startDate, endDate, granularity);

        return Response.ok().entity(availability).build();
    }

    @Tag(name = "Capabilities")
    @Operation(
            summary = "Get status results for node services.",
            description = "Retrieve latest or historical status for a node’s services from Argo Web API.")
    @APIResponse(
            responseCode = "200",
            description = "Status retrieved successfully.",
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/capabilities/status")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatus(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(name = "item", in = QUERY,
                    description = "Service name to target under the node.",
                    example = "WIKI")
            @QueryParam("item")
            String item,
            @Parameter(name = "start_time", in = QUERY,
                    description = "Start time in W3C format.")
            @QueryParam("start_time")
            String startTime,

            @Parameter(name = "end_time", in = QUERY,
                    description = "End time in W3C format.")
            @QueryParam("end_time")
            String endTime,

            @Parameter(name = "history", in = QUERY,
                    description = "Show full history of status timelines.",
                    example = "true")
            @QueryParam("history")
            Boolean history
    ) {
        var status = tenantService.getStatus(id, item, startTime, endTime, history);

        return Response.ok().entity(status).build();
    }

    @Tag(name = "Capabilities")
    @Operation(
            summary = "Retrieve tenant node summary capability.",
            description = "Retrieves the daily availability and uptime summary for a specific service under the tenant node."
    )
    @APIResponse(
            responseCode = "200",
            description = "Tenant node summary details.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeSummaryResponse.class)))
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
    @Path("/{id}/capabilities/summary/{item}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getSummary(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The service name to examine.",
                    required = true,
                    example = "WIKI",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("item")
            String item,
            @Parameter(name = "start_date", in = QUERY,
                    description = "Start date (YYYY-MM-DD).")
            @QueryParam("start_date")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            String startDate,
            @Parameter(name = "end_date", in = QUERY,
                    description = "End date (YYYY-MM-DD).")
            @QueryParam("end_date")
            @Valid
            @CheckDateFormat(pattern = "yyyy-MM-dd", message = "Valid date format is yyyy-MM-dd.")
            String endDate,
            @Parameter(name = "granularity", in = QUERY,
                    description = "Granularity of results (daily, monthly).",
                    example = "daily")
            @QueryParam("granularity")
            String granularity
    ) {

        var summary = tenantService.getSummary(id, item, startDate, endDate, granularity);

        return Response.ok().entity(summary).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/results/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getGroupResults(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class,
                    message = "There is no Tenant with the following id: ")
            String id,
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

        var response = tenantService.getGroupResults(id, groupName, date, period, startTime, endTime, startDate, endDate, granularity, report);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Reports")
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
    @Path("/{id}/status/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getGroupStatusByGroup(
            @Parameter(
                    description = "The ID of the tenant.",
                    schema = @Schema(type = SchemaType.STRING),
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f")
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class,
                    message = "There is no Tenant with the following id: ")
            String id,
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

        var response = tenantService.getGroupStatus(id, groupName, startTime, endTime, history, report);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Reports")
    @Operation(
            summary = "Get report supergroup results.",
            description = "Retrieves availability and reliability results for the supergroups of a tenant's report.")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/results/{report-name}/supergroups")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getSupergroupsByReport(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
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

        var response = tenantService.getSupergroupsByReport(id, reportName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }

    @Tag(name = "Reports")
    @Operation(
            summary = "Get report supergroup results.",
            description = "Retrieves availability and reliability results for the supergroups of a tenant's report.")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/results/{report-name}/supergroups/{supergroup-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getSupergroupsByReport(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
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

        var response = tenantService.getSupergroupByNameByReport(id, reportName, supergroupName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }

    @Tag(name = "Reports")
    @Operation(
            summary = "Get report groups results.",
            description = "Retrieves availability and reliability results for the groups services of a tenant's report.")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/results/{report-name}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getGroupsResultsByReport(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
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

        var response = tenantService.retrieveGroupsResultsByReport(id, reportName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }


    @Tag(name = "Reports")
    @Operation(
            summary = "Get report group results.",
            description = "Retrieves availability and reliability results for the groups services of a tenant's report.")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("{id}/results/{report-name}/groups/{group-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getGroupByNameResultsByReport(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
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

        var response = tenantService.retrieveGroupByNameByReport(id, reportName, groupName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }

    @Tag(name = "Reports")
    @Operation(
            summary = "Get report endpoint results.",
            description = "Retrieves availability and reliability results for the endpoints of a tenant's report.")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("{id}/results/{report-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getEndpointsResultsByReport(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
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

        var response = tenantService.retrieveEndpointsResultsByReport(id, reportName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }


    @Tag(name = "Reports")
    @Operation(
            summary = "Get report endpoint results.",
            description = "Retrieves availability and reliability results for an endpoint of a tenant's report.")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("{id}/results/{report-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getEndpointByNameResultsByReport(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
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

        var response = tenantService.retrieveEndpointByNameResultsByReport(id, reportName, endpointName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }

    @Tag(name = "Reports")
    @Operation(
            summary = "Set tenant report public.",
            description = "Sets a specific tenant report as public."
    )
    @APIResponse(
            responseCode = "200",
            description = "Report is set as public.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeReportResponse.class)))
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
            description = "Tenant or report not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/reports/{report-id}/set-public")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "report-id",
                            type = ParamType.PATH,
                            referTo = ReportsResource.class
                    )
            }
    )
    public Response setReportPublic(
            @Parameter(
                    description = "The ID of the tenant.",
                    schema = @Schema(type = SchemaType.STRING),
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f")
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class,
                    message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The ID of the report.",
                    schema = @Schema(type = SchemaType.STRING),
                    required = true,
                    example = "cf010255-cda3-49d8-92d1-926c2c6cf9eb")
            @PathParam("report-id")
            String reportId) {

        var response = reportService.setReportPublic(id, reportId);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Reports")
    @Operation(
            summary = "Set tenant report private.",
            description = "Sets a specific tenant report as private."
    )
    @APIResponse(
            responseCode = "200",
            description = "Report is set as private.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = WebApiNodeReportResponse.class)))
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
            description = "Tenant or report not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class)))
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/reports/{report-id}/set-private")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "report-id",
                            type = ParamType.PATH,
                            referTo = ReportsResource.class
                    )
            }
    )
    public Response setReportPrivate(
            @Parameter(
                    description = "The ID of the tenant.",
                    schema = @Schema(type = SchemaType.STRING),
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f")
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The ID of the report.",
                    schema = @Schema(type = SchemaType.STRING),
                    required = true,
                    example = "cf010255-cda3-49d8-92d1-926c2c6cf9eb")
            @PathParam("report-id")
            String reportId) {
        var response = reportService.setReportPrivate(id, reportId);

        return Response.ok().entity(response).build();
    }

    @Tag(name = "Downtime")
    @Operation(
            summary = "Create a downtime for a tenant.",
            description = "Create a the tenant's downtime"
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
            description = "Downtime not found.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
    )
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/downtimes")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response createDowntime(@Parameter(
                                           description = "The ID of the tenant to create downtime.",
                                           required = true,
                                           example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                                           schema = @Schema(type = SchemaType.STRING)) @PathParam("id")
                                   @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
                                   @Valid DowntimeRequest request) {

        DowntimeResponse response = downtimeService.addDowntime(id, request);

        return Response.ok(response).build();
    }

    @Tag(name = "Downtime")
    @Operation(
            summary = "Fetch downtimes for a tenant.",
            description = "Returns the tenant's downtimes"
    )
    @APIResponse(
            responseCode = "200",
            description = "Downtimes fetched successfully.",
            content = @Content(schema = @Schema(implementation = PageableDowntimes.class))
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/downtimes")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )

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
                    description = "The ID of the tenant to fetch downtimes.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(
                    repository = TenantRepository.class,
                    message = "There is no Tenant with the following id: ")
            String id,

            @Context UriInfo uriInfo) {

        var response = downtimeService.fetchDowntimesByPageAndSize(
                page - 1,
                size,
                id,
                date,
                startDate,
                endDate,
                uriInfo
        );

        return Response.ok(response).build();
    }

    @Tag(name = "Downtime")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/downtimes/{downtime-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "downtime-id",
                            type = ParamType.PATH,
                            referTo = DowntimeResource.class
                    )
            }
    )

    public Response getDowntime(
            @Parameter(
                    description = "The ID of the tenant to fetch the downtime.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)) @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The ID of the downtime.",
                    required = true,
                    example = "13a1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("downtime-id")
            @Valid @NotFoundEntity(repository = DowntimeRepository.class, message = "There is no Downtime with the following id: ")
            String downtimeId,
            @Context UriInfo uriInfo) {

        var response = downtimeService.fetchDowntimes(id, downtimeId);

        return Response.ok(response).build();
    }

    @Tag(name = "Downtime")
    @Operation(
            summary = "Deletes a specific downtime for a tenant.",
            description = "Deletes a  tenant's specific downtime"
    )
    @APIResponse(
            responseCode = "200",
            description = "Downtimes deleted successfully.",
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
    @SecurityRequirement(name = "Authentication")
    @DELETE
    @Path("/{id}/downtimes/{downtime-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "downtime-id",
                            type = ParamType.PATH,
                            referTo = DowntimeResource.class
                    )
            }
    )

    public Response deleteDowntime(
            @Parameter(
                    description = "The ID of the tenant to fetch the downtime.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)) @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Parameter(
                    description = "The ID of the downtime.",
                    required = true,
                    example = "13a1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)) @PathParam("downtime-id")
            @Valid @NotFoundEntity(repository = DowntimeRepository.class, message = "There is no Downtime with the following id: ") String downtimeId,
            @Context UriInfo uriInfo) {

        downtimeService.deleteDowntime(id, downtimeId);
        var informativeResponse = new InformativeResponse();
        informativeResponse.code = 200;
        informativeResponse.message = "Downtime has been successfully deleted.";

        return Response.ok().entity(informativeResponse).build();
    }

    @Tag(name = "Downtime")
    @Operation(
            summary = "Update a specific downtime for a tenant.",
            description = "Returns the tenant's specific updated downtime"
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

    @SecurityRequirement(name = "Authentication")
    @PUT
    @Path("/{id}/downtimes/{downtime-id}")

    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "downtime-id",
                            type = ParamType.PATH,
                            referTo = DowntimeResource.class
                    )
            }
    )

    public DowntimeResponse updateDowntime(
            @Parameter(
                    description = "The ID of the tenant to fetch the downtime.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)) @PathParam("id")
            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
            @Parameter(
                    description = "The ID of the downtime.",
                    required = true,
                    example = "13a1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)) @PathParam("downtime-id")
            @Valid @NotFoundEntity(repository = DowntimeRepository.class, message = "There is no Downtime with the following id: ") String downtimeId,
            @Valid DowntimeRequest request) {

        return downtimeService.updateDowntime(id, downtimeId, request);
    }

    public static class PageableDowntimes extends PageResource<DowntimeResponse> {

        private List<DowntimeResponse> content;

        @Override
        public List<DowntimeResponse> getContent() {
            return content;
        }

        @Override
        public void setContent(List<DowntimeResponse> content) {
            this.content = content;
        }
    }

    @Tag(name = "Incident")
    @Operation(
            summary = "Create an incident.",
            description = "Creates a new incident for a service belonging to the specified tenant."
    )
    @APIResponse(
            responseCode = "201",
            description = "Incident created.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = IncidentResponseDto.class
            ))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid incident request.",
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
            description = "Tenant or service not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "500",
            description = "Incident could not be created.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/incidents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response createIncident(
            @Parameter(
                    description = "The ID of the tenant.",
                    schema = @Schema(type = SchemaType.STRING),
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f")
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(description = "The incident creation request.")
            @Valid IncidentRequestDto request) {

        var response = incidentService.createIncident(id, request, utility.getUid());

        return Response.status(Response.Status.CREATED).entity(response).build();
    }
    @Tag(name = "Incident")
    @Operation(
            summary = "Update an incident description.",
            description = "Updates the description of an existing incident."
    )
    @APIResponse(
            responseCode = "200",
            description = "Incident updated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = IncidentResponseDto.class
            ))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid incident update request.",
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
            description = "Tenant or incident not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "500",
            description = "Incident could not be updated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @SecurityRequirement(name = "Authentication")
    @PATCH
    @Path("/{id}/incidents/{incident-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "incident-id",
                            type = ParamType.PATH,
                            referTo = IncidentResource.class
                    )
            },
            scope = {Scope.ALL, Scope.MINE}
    )
    public Response updateIncidentDescription(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(description = "The ID of the incident.",
                    required = true,
                    example = "f347c170-7e62-4c72-98a7-9d5e7605c973",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("incident-id")
            @Valid
            @NotFoundEntity(repository = IncidentRepository.class, message = "There is no Incident with the following incident-id: ")
            String incidentId,
            @Valid IncidentDescriptionUpdateRequestDto request) {

        var roles = RoleEndpointHolder.get();

        var response = incidentService.updateIncidentDescriptionById(roles, id, incidentId, request, utility.getUid());

        return Response.ok(response).build();
    }

    @Tag(name = "Incident")
    @Operation(
            summary = "Update an incident status.",
            description = "Updates the status of an incident and, optionally, its status description."
    )
    @APIResponse(
            responseCode = "200",
            description = "Incident updated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = IncidentResponseDto.class
            ))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid incident update request.",
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
            description = "Tenant or incident not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "500",
            description = "Incident could not be updated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @SecurityRequirement(name = "Authentication")
    @PATCH
    @Path("/{id}/incidents/{incident-id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "incident-id",
                            type = ParamType.PATH,
                            referTo = IncidentResource.class
                    )
            },
            scope = {Scope.ALL, Scope.MINE}
    )
    public Response updateIncident(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(description = "The ID of the incident.",
                    required = true,
                    example = "f347c170-7e62-4c72-98a7-9d5e7605c973",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("incident-id")
            @Valid
            @NotFoundEntity(repository = IncidentRepository.class, message = "There is no Incident with the following incident-id: ")
            String incidentId,
            @Parameter(
                    description = "The incident update request.",
                    required = true
            )
            @NotNull(message = "Incident update request cannot be null.")
            @Valid IncidentStatusUpdateRequestDto request) {

        var roles = RoleEndpointHolder.get();

        var response = incidentService.updateIncidentStatus(roles, id, incidentId, request, utility.getUid());

        return Response.ok(response).build();
    }

    @Tag(name = "Incident")
    @Operation(
            summary = "Add an incident comment.",
            description = "Adds a new comment to an incident."
    )
    @APIResponse(
            responseCode = "201",
            description = "Comment added.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = IncidentCommentResponseDto.class
            ))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid comment request.",
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
            description = "Tenant or incident not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "500",
            description = "Comment could not be added.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @SecurityRequirement(name = "Authentication")
    @POST
    @Path("/{id}/incidents/{incident-id}/comments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "incident-id",
                            type = ParamType.PATH,
                            referTo = IncidentResource.class
                    )
            }
    )
    public Response addIncidentComment(

            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("id")
            @Valid
            @NotFoundEntity(
                    repository = TenantRepository.class,
                    message = "There is no Tenant with the following id: "
            )
            String id,

            @Parameter(
                    description = "The ID of the incident.",
                    required = true,
                    example = "62491b9b-7c95-4f66-bbd2-2eb407118afc",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("incident-id")
            @Valid
            @NotFoundEntity(
                    repository = IncidentRepository.class,
                    message = "There is no Incident with the following incident-id: "
            )
            String incidentId,

            @Parameter(
                    description = "The incident comment.",
                    required = true
            )
            @NotNull(message = "Incident comment request cannot be null.")
            @Valid
            IncidentCommentRequestDto request) {

        var response = incidentService.addComment(id, incidentId, request, utility.getUid());

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @Tag(name = "Incident")
    @Operation(
            summary = "Get all incidents.",
            description = "Retrieves a paginated list of incidents belonging to the specified tenant. Results can be filtered by incident title or service name."
    )
    @APIResponse(
            responseCode = "200",
            description = "Incidents retrieved.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = PageableIncidents.class
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
    @Path("/{id}/incidents")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getAllIncidents(
            @Parameter(
                    description = "The ID of the tenant.",
                    schema = @Schema(type = SchemaType.STRING),
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f")
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var response = incidentService.getIncidentsByPageAndSize(id, page - 1, size, search, date, uriInfo);

        return Response.ok(response).build();
    }

    @Tag(name = "Incident")
    @Operation(
            summary = "Get incident activity.",
            description = "Retrieves the status change history of a specific incident."
    )
    @APIResponse(
            responseCode = "200",
            description = "Incident activity retrieved.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = IncidentActivityResponseDto.class
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
            description = "Tenant or incident not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "500",
            description = "Incident activity could not be retrieved.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/incidents/{incident-id}/activity")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "incident-id",
                            type = ParamType.PATH,
                            referTo = IncidentResource.class
                    )
            }
    )
    public Response getIncidentActivity(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("id") @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The ID of the incident.",
                    required = true,
                    example = "62491b9b-7c95-4f66-bbd2-2eb407118afc",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("incident-id")
            @Valid
            @NotFoundEntity(repository = IncidentRepository.class, message = "There is no Incident with the following incident-id: ")
            String incidentId) {

        var response = incidentService.getIncidentActivity(id, incidentId);

        return Response.ok(response).build();
    }

    @Tag(name = "Incident")
    @Operation(
            summary = "Update an incident status description.",
            description = "Updates the description associated with a specific incident status activity."
    )
    @APIResponse(
            responseCode = "200",
            description = "Incident updated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = IncidentResponseDto.class
            ))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid incident update request.",
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
            description = "Tenant or incident not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @APIResponse(
            responseCode = "500",
            description = "Incident could not be updated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            ))
    )
    @SecurityRequirement(name = "Authentication")
    @PATCH
    @Path("/{id}/incidents/{incident-id}/activity/{activity-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    ),
                    @ParamRef(
                            param = "incident-id",
                            type = ParamType.PATH,
                            referTo = IncidentResource.class
                    )
            },
            scope = {Scope.ALL, Scope.MINE}
    )
    public Response updateIncidentStatusDescription(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(description = "The ID of the incident.",
                    required = true,
                    example = "f347c170-7e62-4c72-98a7-9d5e7605c973",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("incident-id")
            @Valid
            @NotFoundEntity(repository = IncidentRepository.class, message = "There is no Incident with the following incident-id: ")
            String incidentId,
            @Parameter(description = "The ID of the incident status.",
                    required = true,
                    example = "f347c170-7e62-4c72-98a7-9d5e7605c973",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("activity-id")
            @Valid
            @NotFoundEntity(repository = IncidentActivityRepository.class, message = "There is no Incident Status with the following incident-id: ")
            String activityId,
            @Parameter(
                    description = "The incident update request.",
                    required = true
            )
            @NotNull(message = "Incident update request cannot be null.")
            @Valid IncidentStatusDescriptionUpdateRequestDto request) {

        var roles = RoleEndpointHolder.get();

        var response = incidentService.updateIncidentStatusDescriptionById(roles, id, incidentId, activityId, request, utility.getUid());

        return Response.ok(response).build();
    }

    @Tag(name = "Incident")
    @Operation(
            summary = "Get an incident.",
            description = "Retrieves a specific incident belonging to the specified tenant."
    )
    @APIResponse(
            responseCode = "200",
            description = "Incident retrieved.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = IncidentResponseDto.class
            )))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            )))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            )))
    @APIResponse(
            responseCode = "404",
            description = "Tenant or incident not found.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            )))
    @APIResponse(
            responseCode = "500",
            description = "Incident could not be retrieved.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = InformativeResponse.class
            )))
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/incidents/{incident-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getIncident(
            @Parameter(
                    description = "The ID of the tenant.",
                    schema = @Schema(type = SchemaType.STRING),
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f")
            @PathParam("id")
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The ID of the incident.",
                    schema = @Schema(type = SchemaType.STRING),
                    required = true,
                    example = "8d8102e8-9476-4714-8101-ac76f934781f")
            @PathParam("incident-id")
            @NotFoundEntity(repository = IncidentRepository.class, message = "There is no Incident with the following id: ")
            String incidentId) {

        var response = incidentService.getIncident(id, incidentId);

        return Response.ok(response).build();
    }


    public static class PageableIncidents extends PageResource<IncidentResponseDto> {

        private List<IncidentResponseDto> content;

        @Override
        public List<IncidentResponseDto> getContent() {
            return content;
        }

        @Override
        public void setContent(List<IncidentResponseDto> content) {
            this.content = content;
        }
    }


    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("{id}/results/{report-name}/groups/{group-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getGroupsEndpointResultsByReport(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
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

        var response = tenantService.retrieveResultsEndpointByReportAndGroup(id, reportName, groupName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }


    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("{id}/results/{report-name}/groups/{group-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getGroupsEndpointResultsByReport(
            @Parameter(description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
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

        var response = tenantService.retrieveResultsEndpointByReportGroupAndEndpoint(id, reportName, groupName, endpointName, startTime, endTime, granularity);

        return Response.ok(response).build();
    }
//
//    @Tag(name = "Downtime")
//    @Operation(
//            summary = "Fetch a daily downtime for a tenant.",
//            description = "Returns the tenant's specific daily downtime"
//    )
//    @APIResponse(
//            responseCode = "200",
//            description = "Downtimes fetched successfully.",
//            content = @Content(schema = @Schema(implementation = DailyDowntimeResponse.class))
//    )
//    @APIResponse(
//            responseCode = "400",
//            description = "Invalid request payload.",
//            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
//    )
//    @APIResponse(
//            responseCode = "401",
//            description = "User not authenticated.",
//            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
//    )
//    @APIResponse(
//            responseCode = "403",
//            description = "Not permitted.",
//            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
//    )
//    @APIResponse(
//            responseCode = "404",
//            description = "Downtimes not found.",
//            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
//    )
//    @APIResponse(
//            responseCode = "500",
//            description = "Internal server error.",
//            content = @Content(schema = @Schema(implementation = InformativeResponse.class))
//    )
//    @SecurityRequirement(name = "Authentication")
//
//    @GET
//    @Path("/{id}/downtimes/daily")
//    @Produces(MediaType.APPLICATION_JSON)
//    @SecuredEndpoint(
//            params = {
//                    @ParamRef(
//                            param = "id",
//                            type = ParamType.PATH,
//                            referTo = TenantResource.class
//                    )
//            }
//    )
//    public Response getDailyDowntimes(
//            @Parameter(
//                    description = "The ID of the tenant to fetch downtimes.",
//                    required = true,
//                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
//                    schema = @Schema(type = SchemaType.STRING)) @PathParam("id")
//            @Valid @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ") String id,
//            @Parameter(
//                    name = "date",
//                    description = "UTC date in yyyy-MM-dd format",
//                    example = "2026-07-22",
//                    required = true
//            )
//            @CheckDateFormat(
//                    pattern = "yyyy-MM-dd",
//                    message = "Valid date format is yyyy-MM-dd."
//            )
//            @QueryParam("date")
//            @NotNull
//            String date
//    ) {
//
//        return Response.ok(
//                downtimeService.fetchDailyDowntimes(id, date)
//        ).build();
//
//        return Response.ok(downtimeService.fetchDailyDowntimes(id, date)).build();
//    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusGroupsByReport(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var groupTimelines = statusService.retrieveStatusTimelineGroupsByReport(id, reportName, startTime, endTime);

        return Response.ok(groupTimelines).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusGroupByNameByReport(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var groupTimelines = statusService.retrieveStatusTimelineGroupByNameByReport(id, reportName, groupName, startTime, endTime);

        return Response.ok(groupTimelines).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/service-types")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusServiceTypesByGroup(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var serviceTypeTimelines = statusService.retrieveStatusTimelineServiceTypesByGroup(id, reportName, groupName, startTime, endTime);

        return Response.ok(serviceTypeTimelines).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusServiceTypeByName(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var serviceTypeTimeline = statusService.retrieveStatusTimelineServiceTypeByName(id, reportName, groupName, serviceTypeName, startTime, endTime);

        return Response.ok(serviceTypeTimeline).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusEndpointsByGroup(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var endpointTimelines = statusService.retrieveStatusTimelineEndpointsByGroup(
                id, reportName, groupName, startTime, endTime);

        return Response.ok(endpointTimelines).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusEndpointByGroupAndName(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var endpointTimeline = statusService.retrieveStatusTimelineEndpointByGroupAndName(id, reportName, groupName, endpointName, startTime, endTime);

        return Response.ok(endpointTimeline).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusEndpointsByServiceType(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var endpointTimelines = statusService.retrieveStatusTimelineEndpointsByServiceType(id, reportName, groupName, serviceTypeName, startTime, endTime);

        return Response.ok(endpointTimelines).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusEndpointByServiceTypeAndName(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var endpointTimeline = statusService.retrieveStatusTimelineEndpointByServiceTypeAndName(
                id, reportName, groupName, serviceTypeName, endpointName, startTime, endTime);

        return Response.ok(endpointTimeline).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/endpoints/{endpoint-name}/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusMetricsByEndpoint(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var metricTimelines = statusService.retrieveStatusTimelineMetricsByEndpoint(id, reportName, groupName, endpointName, startTime, endTime);

        return Response.ok(metricTimelines).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/endpoints/{endpoint-name}/metrics/{metric-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusMetricByEndpointAndName(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var metricTimeline = statusService.retrieveStatusTimelineMetricByEndpointAndName(id, reportName, groupName, endpointName, metricName, startTime, endTime);

        return Response.ok(metricTimeline).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusMetricsByServiceTypeAndEndpoint(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var metricTimelines = statusService.retrieveStatusTimelineMetricsByServiceTypeAndEndpoint(id, reportName, groupName, serviceTypeName, endpointName, startTime, endTime);

        return Response.ok(metricTimelines).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}/metrics/{metric-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusMetricByServiceTypeEndpointAndName(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var metricTimeline = statusService.retrieveStatusTimelineMetricByServiceTypeEndpointAndName(id, reportName, groupName, serviceTypeName, endpointName, metricName, startTime, endTime);

        return Response.ok(metricTimeline).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/groups/{group-name}/service-types/{service-type-name}/endpoints/{endpoint-name}/metrics/{metric-name}/details")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusMetricDetailsByServiceTypeEndpointAndName(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var metricDetails = statusService.retrieveStatusMetricDetailsByServiceTypeEndpointAndName(
                id, reportName, groupName, serviceTypeName, endpointName, metricName, timestamp);

        return Response.ok(metricDetails).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusEndpointsByReport(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var endpointTimelines = statusService.retrieveStatusTimelineEndpointsByReport(id, reportName, startTime, endTime);

        return Response.ok(endpointTimelines).build();
    }

    @Tag(name = "Reports")
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
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/status/{report-name}/endpoints/{endpoint-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getStatusEndpointByNameByReport(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
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

        var groupTimelines = statusService.retrieveStatusTimelineEndpointByNameByReport(id, reportName, endpointName, startTime, endTime);

        return Response.ok(groupTimelines).build();
    }


    @Tag(name = "Reports")
    @Operation(
            summary = "Fetch the N latest data for the specified report, group type of the tenant",
            description = "Returns the the N latest data for the specified report, group type, group name of the tenant"
    )
    @APIResponse(
            responseCode = "200",
            description = "Latest data fetched successfully.",
            content = @Content(schema = @Schema(
                    implementation = LatestDataResponse.class))
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
            description = "Latest data not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/report/{report-id}/groups-type/{group-type}/latest-data")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getLatestData(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The id of the tenant's report.",
                    required = true,
                    example = "c7a6b0d4-4885-46da-9dd1-1f91d0e9142e",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("report-id")
            @Valid
            String reportId,
            @Parameter(
                    description = "The group type ",
                    required = true,
                    example = "SERVICEGROUPS",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("group-type")
            @Valid
            String groupType,
            @Parameter(name = "filter", in = QUERY,
                    description = "Filter errors by status.")
            @ValidLatestDataStatusFilter
            @DefaultValue("all")
            @QueryParam("filter") String filter,
            @Parameter(name = "strict", in = QUERY,
                    description = "the servce will return only the latest entry grouped by endpoint_group/host/service/metric",
                    example = "false")
            @QueryParam("strict")
            @DefaultValue("false")
            Boolean strict,
            @Parameter(name = "limit", in = QUERY,
                    description = "the limit",
                    example = "500")
            @QueryParam("limit")
            Integer limit) {

        var metricDetails = reportService.retrieveLatestData(
                id, reportId,groupType, filter, strict,limit);

        return Response.ok(metricDetails).build();
    }


    @Tag(name = "Reports")
    @Operation(
            summary = "Fetch the N latest data for the specified report, group type, group name of the tenant",
            description = "Returns the N latest data for the specified report, group type, group name of the tenant"
    )
    @APIResponse(
            responseCode = "200",
            description = "Latest data etched successfully.",
            content = @Content(schema = @Schema(
                    implementation = LatestDataResponse.class))
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
            description = "Latest data not found.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(
                    implementation = InformativeResponse.class))
    )
    @SecurityRequirement(name = "Authentication")
    @GET
    @Path("/{id}/report/{report-id}/groups-type/{group-type}/{group-name}/latest-data")
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint(
            params = {
                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = TenantResource.class
                    )
            }
    )
    public Response getLatestDataByGroupName(
            @Parameter(
                    description = "The ID of the tenant.",
                    required = true,
                    example = "42c1152d-e23c-4a19-b51a-b27f1eb7f37f",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("id")
            @Valid
            @NotFoundEntity(repository = TenantRepository.class, message = "There is no Tenant with the following id: ")
            String id,
            @Parameter(
                    description = "The id of the tenant's report.",
                    required = true,
                    example = "c7a6b0d4-4885-46da-9dd1-1f91d0e9142e",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("report-id")
            @Valid
            String reportId,
            @Parameter(
                    description = "The group type ",
                    required = true,
                    example = "SERVICEGROUPS",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("group-type")
            @Valid
            String groupType,
            @Parameter(
                    description = "The group name ",
                    required = true,
                    example = "HELPDESK",
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("group-name")
            @Valid
            String groupName,
            @Parameter(name = "filter", in = QUERY,
                    description = "Filter errors by status.")
            @ValidLatestDataStatusFilter
            @DefaultValue("all")
            @QueryParam("filter") String filter,
            @Parameter(name = "strict", in = QUERY,
                    description = "the servce will return only the latest entry grouped by endpoint_group/host/service/metric",
                    example = "false")
            @QueryParam("strict")
            @DefaultValue("false")
            Boolean strict,
            @Parameter(name = "limit", in = QUERY,
                    description = "the limit",
                    example = "500")
            @QueryParam("limit")
            Integer limit) {

        var metricDetails = reportService.retrieveLatestDataByGroupName(
                id, reportId,groupType,groupName, filter, strict,limit);

        return Response.ok(metricDetails).build();
    }
}
