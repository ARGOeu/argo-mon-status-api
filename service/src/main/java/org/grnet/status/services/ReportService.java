package org.grnet.status.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.grnet.status.dtos.LatestDataResponse;
import org.grnet.status.dtos.encrypt.EncryptRequestDto;
import org.grnet.status.dtos.encrypt.EncryptResponseDto;
import org.grnet.status.dtos.report.MiniReportResponse;
import org.grnet.status.dtos.report.PartialReportResponseDto;
import org.grnet.status.dtos.report.FullReportResponseDto;
import org.grnet.status.dtos.report.WebApiReportResponse;
import org.grnet.status.dtos.tenant.node.WebApiNodeReportResponse;
import org.grnet.status.mappers.GeneralMapper;
import org.grnet.status.mappers.ReportMapper;
import org.grnet.status.repositories.TenantRepository;
import org.grnet.status.services.clients.ArgoWebApiClient;
import org.grnet.status.services.clients.WebApiService;
import org.grnet.status.services.utils.EncryptUtil;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static io.netty.util.AsciiString.contains;

/**
 * Service responsible for retrieving reports and related data from Argo Web API.
 */
@ApplicationScoped
public class ReportService {

    private static final Logger LOG = Logger.getLogger(ReportService.class);

    @ConfigProperty(name = "web.api.access.token")
    String accessToken;
    @Inject
    EncryptUtil encryptUtil;

    @Inject
    WebApiService webApiService;

    @Inject
    @RestClient
    ArgoWebApiClient argoWebApiClient;

    @Inject
    TenantRepository tenantRepository;


    /**
     * Retrieves a list of reports for the given tenant with optional search filtering.
     *
     * @param tenantId tenant identifier
     * @param search   search filter
     * @return list of reports
     */
    public List<MiniReportResponse> fetchReportsByStatus(
            String tenantId,
            String search,
            Boolean publicReports,
            Boolean nodeReports) {

        var reports = fetchWebApiReports(tenantId, publicReports, nodeReports);

        var miniReports = reports.data.stream()
                .filter(r -> r != null && r.info != null)
                .map(ReportMapper.INSTANCE::fullToMiniReport)
                .toList();

        if (StringUtils.isNotBlank(search)) {
            var lowerSearch = search.toLowerCase();

            miniReports = miniReports.stream()
                    .filter(r ->
                            contains(StringUtils.defaultString(r.id).toLowerCase(), lowerSearch) ||
                                    contains(StringUtils.defaultString(r.name).toLowerCase(), lowerSearch)
                    )
                    .toList();
        }

        return miniReports.stream()
                .sorted(Comparator.comparing(r -> r.name))
                .collect(Collectors.toList());
    }


    /**
     * Retrieves a list of reports for the given tenant with optional search filtering.
     *
     * @param tenantId tenant identifier
     * @param search   search filter
     * @return list of reports
     */
    public List<PartialReportResponseDto> fetchReports(String tenantId, String search, Boolean publicReports, Boolean nodeReports) {

        var reports = fetchWebApiReports(tenantId, publicReports, nodeReports);
        var partialReports = reports.data.stream()
                .filter(r -> r != null && r.info != null)
                .map(ReportMapper.INSTANCE::fullToPartialReport)
                .toList();

        if (StringUtils.isNotBlank(search)) {
            var lowerSearch = search.toLowerCase();

            partialReports = partialReports.stream()
                    .filter(r ->
                            contains(r.id.toLowerCase(), lowerSearch) ||
                                    contains(r.name.toLowerCase(), lowerSearch) ||
                                    contains(r.description.toLowerCase(), lowerSearch) ||
                                    contains(r.tenantName.toLowerCase(), lowerSearch)
                    )
                    .toList();

            partialReports = new ArrayList<>(partialReports);
        }

        return partialReports.stream()
                .sorted(Comparator.comparing(r -> r.disabled))
                .collect(Collectors.toList());

    }

    /**
     * Encrypts a plain text secret.
     *
     * @param request encryption request
     * @return encryption response
     */
    public EncryptResponseDto encrypt(EncryptRequestDto request) {

        var encrypted = encryptUtil.encrypt(request.secret);
        return GeneralMapper.INSTANCE.toEncryptResponse(encrypted);
    }

    /**
     * Retrieves a report by its identifier for the given tenant.
     *
     * @param id       tenant identifier
     * @param reportId report identifier
     * @return report response
     */
    public FullReportResponseDto fetchReportById(String id, String reportId) {

        webApiService.validateTenantInitialized(id, "Reports");

        var reports = argoWebApiClient.fetchReportsSuperAdmin(accessToken, id, null, null, null);
        var found = reports.data.stream()
                .anyMatch(r -> reportId.equals(r.id));

        if (!found) {
            throw new NotFoundException("Fetching Report... Not found report with id: " + reportId + " for tenant with id: " + id);
        }

        try {
            return argoWebApiClient.fetchReportByIdSuperAdmin(reportId, accessToken, id).data.get(0);
        } catch (ClientWebApplicationException e) {
            throw new ClientWebApplicationException("Fetching Report... Report not found in Argo Web Api with id: " + reportId);
        }
    }

    /**
     * Sets the default node report by tenant and report identifier.
     *
     * @param tenantId tenant identifier
     * @param reportId report identifier
     * @return status response
     */
    public WebApiNodeReportResponse setNodeReport(String tenantId, String reportId) {

        webApiService.validateTenantInitialized(tenantId, "Reports");
        var reports = argoWebApiClient.fetchReportsSuperAdmin(accessToken, tenantId, null, null, null);
        var found = reports.data.stream()
                .anyMatch(r -> reportId.equals(r.id));

        if (!found) {
            throw new NotFoundException("Fetching Report... Not found report with id: " + reportId + " for tenant with id: " + tenantId);
        }

        try {
            return webApiService.setNodeReportWebApi(reportId, tenantId);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(
                    "Updating Report... Failed to set node report with id: " + reportId,
                    502
            );
        }
    }


    /**
     * Sets the specified report as public for the given tenant.
     *
     * @param tenantId tenant identifier
     * @param reportId report identifier
     * @return status response
     */
    @Transactional
    public WebApiNodeReportResponse setReportPublic(String tenantId, String reportId) {

        var tenant = tenantRepository.findById(tenantId);

        return webApiService.setReportPublicWebApi(reportId, tenant.id);
    }

    /**
     * Sets the specified report as private for the given tenant.
     *
     * @param tenantId tenant identifier
     * @param reportId report identifier
     * @return status response
     */
    @Transactional
    public WebApiNodeReportResponse setReportPrivate(String tenantId, String reportId) {

        var tenant = tenantRepository.findById(tenantId);

        return webApiService.setReportPrivateWebApi(reportId, tenant.id);
    }

    private WebApiReportResponse fetchWebApiReports(String tenantId, Boolean publicReports, Boolean nodeReports) {

        webApiService.validateTenantInitialized(tenantId, "Reports");

        LOG.info("Fetching reports from ARGO Web API...");

        return webApiService.retrieveReportsWebApi(tenantId, publicReports, Boolean.FALSE.equals(publicReports), nodeReports);
    }

    /**
     * Returns the latest -limit- errors based on the status as received from filter  (ok, non-ok, critical, warning, ok, unknown, missing, all)
     * @param id
     * @param reportId
     * @param groupType
     * @param filter
     * @param strict
     * @param limit
     * @return
     */
    public LatestDataResponse retrieveLatestData(String id, String reportId, String groupType, String filter, boolean strict, int limit) {

        // Latest data can only be retrieved for tenants whose Reports component
        // has been successfully initialized.
        webApiService.validateTenantInitialized(id, "Reports");

        // Retrieve the reports configured for the tenant in order to resolve the
        // internal report name required by the Argo Web API.
        var reports = argoWebApiClient.fetchReportsSuperAdmin(
                accessToken, id, null, null, null);

        // Find the requested report using the report ID provided by the client.
        // The Argo Web API expects the report name rather than the report ID.
        var report = reports.data.stream()
                .filter(r -> reportId.equals(r.id))
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException( "Fetching Report... Not found report with id: " + reportId + " for tenant with id: " + id));

        try {
            // Retrieve the latest data for the specified report and endpoint
            // group type. The report name is used because this is what the
            // Argo Web API expects.
            return argoWebApiClient.listLatestData(report.info.name, groupType, filter, limit, strict, accessToken, id);

        } catch (ClientWebApplicationException e) {

            // A 404 from the Argo Web API indicates that the requested endpoint
            // group type could not be found for the specified report.
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException("Fetching Latest Metric Data... No endpoint group of type '" + groupType + "' was found for report '" + reportId + "' in tenant with id: " + id);}

            // Propagate other data returned by the Argo Web API while adding
            // context about the operation that failed.
            throw new ClientWebApplicationException("Fetching Latest Metric Data... Failed to retrieve from Argo Web API.", e);
        }
    }


    public LatestDataResponse retrieveLatestDataByGroupName(String id, String reportId, String groupType, String groupName, String filter, boolean strict, int limit) {

        // Latest data can only be retrieved for tenants whose Reports component
        // has been successfully initialized.
        webApiService.validateTenantInitialized(id, "Reports");

        // Retrieve the reports configured for the tenant in order to resolve the
        // internal report name required by the Argo Web API.
        var reports = argoWebApiClient.fetchReportsSuperAdmin(
                accessToken, id, null, null, null);

        // Find the requested report using the report ID provided by the client.
        // The Argo Web API expects the report name rather than the report ID.
        var report = reports.data.stream()
                .filter(r -> reportId.equals(r.id))
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException("Fetching Report... Not found report with id: " + reportId + " for tenant with id: " + id));

        try {
            // Retrieve the latest data for the specified endpoint group type
            // and group name. The report name is used because this is what the
            // Argo Web API expects.
            return argoWebApiClient.listLatestDataByGroupName(report.info.name, groupType, groupName, filter, limit, strict, accessToken, id);

        } catch (ClientWebApplicationException e) {

            // A 404 from the Argo Web API indicates that the requested endpoint
            // group could not be found for the specified report.
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new NotFoundException("Fetching Latest Metric Data... No endpoint group of type '" + groupType + "' was found for report '" + reportId + "' in tenant with id: " + id);
            }

            // Propagate other data returned by the Argo Web API while adding
            // context about the operation that failed.
            throw new ClientWebApplicationException("Fetching Latest Metric Data... Failed to retrieve from Argo Web API.", e);
        }
    }
}
