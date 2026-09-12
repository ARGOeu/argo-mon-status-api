# Changelog

---

All notable changes to this project will be documented in this file.

According to [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) , the `Unreleased` section serves the following purposes:

-   People can see what changes they might expect in upcoming releases.
-   At release time, you can move the `Unreleased` section changes into a new release version section.

## Types of changes

---

-   `Added` for new features.
-   `Changed` for changes in existing functionality.
-   `Removed` for now removed features.
-   `Fixed` for any bug fixes.
-   `Security` in case of vulnerabilities.
-   `Deprecated` for soon-to-be removed features.


## Unreleased

### Added
- [#182](https://github.com/ARGOeu/argo-mon-api/pull/182) ARGO-5692 Trigger readiness validation on first feed configuration
- [#183](https://github.com/ARGOeu/argo-mon-status-api/pull/183) ARGO-5698 Upgrade quarkus auth version in pom.xml
- [#184](https://github.com/ARGOeu/argo-mon-api/pull/184) ARGO-5696 Update tenant resource group deletion to use Quarkus Auth role hierarchy
- [#185](https://github.com/ARGOeu/argo-mon-api/pull/186) ARGO-5706 [API] Fetch monthly and daily A/R supergroup views for all topology levels for tenant
- [#187](https://github.com/ARGOeu/argo-mon-api/pull/187) ARGO-5711 [API] Fetch monthly and daily A/R groups views for all topology levels for tenant
- [#190](https://github.com/ARGOeu/argo-mon-api/pull/190) ARGO-5715: [API] Fetch monthly and daily A/R endpoints views for all topology levels for tenant
- [#191](https://github.com/ARGOeu/argo-mon-status-api/pull/191) ARGO-5274 [API] - Configuration Settings Page
- [#192](https://github.com/ARGOeu/argo-mon-status-api/pull/192) ARGO-5748 Expose Performance Settings Through Public API
- [#193](https://github.com/ARGOeu/argo-mon-status-api/pull/193) ARGO-5740 [API] Add downtime
- [#194](https://github.com/ARGOeu/argo-mon-api/pull/194) ARGO-5732 [API] Create Incident
- [#195](https://github.com/ARGOeu/argo-mon-status-api/pull/195) ARGO-5744 [API] Get Downtimes
- [#196](https://github.com/ARGOeu/argo-mon-status-api/pull/19) ARGO-5745 [API] Get Downtime
- [#197](https://github.com/ARGOeu/argo-mon-status-api/pull/197) ARGO-5741 [API] Delete downtime
- [#198](https://github.com/ARGOeu/argo-mon-status-api/pull/198) ARGO-5743 [API] Update downtime
- [#200](https://github.com/ARGOeu/argo-mon-api/pull/200) ARGO-5733 [API] Change incident status / add comment
- [#201](https://github.com/ARGOeu/argo-mon-status-api/pull/201) ARGO-5762 Restrict downtime management to supported topology feeds
- [#202](https://github.com/ARGOeu/argo-mon-api/pull/202) ARGO-5734 [API] Get all incidents
- [#203](https://github.com/ARGOeu/argo-mon-status-api/pull/203) ARGO-578 Validate downtime service endpoints against tenant topology
- [#204](https://github.com/ARGOeu/argo-mon-api/pull/204) ARGO-5735 [API] Get incident
- [#205](https://github.com/ARGOeu/argo-mon-status-api/pull/205) ARGO-5771 Support node-registry feed#205
- [#206](https://github.com/ARGOeu/argo-mon-api/pull/206) ARGO-5775 Add public API endpoints for A/R results
- [#207](https://github.com/ARGOeu/argo-mon-api/pull/207) ARGO-5736 [API] Send email when reported
- [#208](https://github.com/ARGOeu/argo-mon-api/pull/208) ARGO-5781 Fetch Endpoint A/R Results per Report and Group via Status API#208
- [#209](https://github.com/ARGOeu/argo-mon-api/pull/209) ARGO-5737 [API] Activity log / history of status changes
- [#211](https://github.com/ARGOeu/argo-mon-api/pull/211) ARGO-5738 [API] Get daily incidents
- [#213](https://github.com/ARGOeu/argo-mon-api/pull/213) ARGO-5738 [API] ARGO-5746 [API] Get tenants daily downtimes
- [#214](https://github.com/ARGOeu/argo-mon-api/pull/214) ARGO-5806 [API] Add incident_admin role
- [#218](https://github.com/ARGOeu/argo-mon-api/pull/218) ARGO-5802 [API] Group status timeline
- [#219](https://github.com/ARGOeu/argo-mon-api/pull/219) ARGO-5803 [API] Service-type status timeline
- [#220](https://github.com/ARGOeu/argo-mon-api/pull/220) ARGO-5804 [API] Endpoint status timeline
- [#222](https://github.com/ARGOeu/argo-mon-api/pull/222) ARGO-5796 Create event for initialising archiver component
- [#224](https://github.com/ARGOeu/argo-mon-api/pull/224) ARGO-5805 [API] Metric status timeline
- [#225](https://github.com/ARGOeu/argo-mon-api/pull/225) ARGO-5811 [API] - add a new true/false field at tenant
- [#226](https://github.com/ARGOeu/argo-mon-api/pull/226) ARGO-5758 Add public endpoint for tenant basic info
- [#228](https://github.com/ARGOeu/argo-mon-api/pull/228) ARGO-5839 [API] Run Local Keycloak as a Quarkus Dev Service
- [#229](https://github.com/ARGOeu/argo-mon-api/pull/229) ARGO-5847 Expose performance field in GET /v1/public/tenants/{tenant-name}/info
- [#230](https://github.com/ARGOeu/argo-mon-api/pull/230) ARGO-5843 argo-mon-api get tenant downtimes for public tenants
- [#231](https://github.com/ARGOeu/argo-mon-api/pull/231)  ARGO-5845 Conflict when declaring new downtime
- [#232](https://github.com/ARGOeu/argo-mon-api/pull/232)  ARGO-5850 Add endpoint to expose whether a tenant uses an external feed topology
- [#233](https://github.com/ARGOeu/argo-mon-api/pull/233) ARGO-5883 Add Endpoints to support Monitoring Capability for Metrics to Argo Monitoring Status API
- [#234](https://github.com/ARGOeu/argo-mon-api/pull/234) ARGO-5859 Implement Endpoint Status Timeline API Endpoints
- [#235](https://github.com/ARGOeu/argo-mon-api/pull/235) ARGO-5888 argo-mon-api: when a new tenant is created please in INIT_COMPUTE_ENGINE event specify if performance:true/false (according to what the user has defined in the tenant)- #235
- [#236](https://github.com/ARGOeu/argo-mon-api/pull/236) ARGO-5862 Publicly Expose Status Timelines endpoints for tenant's reports
- [#238](https://github.com/ARGOeu/argo-mon-api/pull/238) ARGO-5870 Add Service-Specific Monitoring Metrics Endpoints
- [#241](https://github.com/ARGOeu/argo-mon-api/pull/241) ARGO-5880 create a INIT_PERFORMANCE_DATA event
- [#242](https://github.com/ARGOeu/argo-mon-api/pull/242) ARGO-5883 [API] Support multiple affected services per incident
- [#244](https://github.com/ARGOeu/argo-mon-api/pull/244) ARGO-5905: Support a description field for incident status changes
- [#254](https://github.com/ARGOeu/argo-mon-api/pull/254) Add a public API endpoint for the incidents list
 
### Fix 

- [#210](https://github.com/ARGOeu/argo-mon-api/pull/210) ARGO-5792 Enforce Non-Null completed_at and Request Validation for Downtime Dates
- [#212](https://github.com/ARGOeu/argo-mon-api/pull/212) ARGO-5797 Update public REST endpoint paths to use tenant name instead of tenant ID
- [#221](https://github.com/ARGOeu/argo-mon-api/pull/221) ARGO-5820 [API] Use Configured Super Admin Role in AGM Initialization
- [#227](https://github.com/ARGOeu/argo-mon-api/pull/227) ARGO-5831 Update the downtime management validation to allow downtime operations for all feed topologies except External.
- [#237](https://github.com/ARGOeu/argo-mon-api/pull/237) ARGO-5864 start-date, end-date for monitoring capability should be optional- #237
- [#239](https://github.com/ARGOeu/argo-mon-api/pull/239) ARGO-5876 [API] Prevent inactive reports from being made public
- [#242](https://github.com/ARGOeu/argo-mon-api/pull/242) ARGO-5881 Retrieve metric status result details
- [#243](https://github.com/ARGOeu/argo-mon-api/pull/243) ARGO-5906 Make date in automation daily downtimes optional
- [#245](https://github.com/ARGOeu/argo-mon-api/pull/245) ARGO-5909 EOSC-BEYOND-FEDERATION duplicate INIT_TOPOLOGY_INTEGRATOR job in status job list
- [#246](https://github.com/ARGOeu/argo-mon-api/pull/246) ARGO-5907 When topology feed changes clean up INIT_TOPO_CONNECTOR or INIT_TOPO_INTEGRATION jobs if not relevant
- [#247](https://github.com/ARGOeu/argo-mon-api/pull/247) ARGO-5910 [API] Use a human-readable username for incident actions
- [#248](https://github.com/ARGOeu/argo-mon-api/pull/248) ARGO-5913 [API] Preserve incident activity when updating status description
- [#249](https://github.com/ARGOeu/argo-mon-api/pull/249) ARGO-5914 [API] Support editing incident and status descriptions
- [#250](https://github.com/ARGOeu/argo-mon-api/pull/250) ARGO-5919 Fix transaction locking when enabling/disabling Performance Data for a tenant- #250
- [#252](https://github.com/ARGOeu/argo-mon-api/pull/252) ARGO-5921 fix secured endpoint path

## 1.0.0 - 2026-06-26
---
### Added

- [#1](https://github.com/ARGOeu/argo-mon-status-api/pull/1) ARGO-5136 Add Status Pages API and Implement /v1/encrypt & /v1/reports Endpoints
- [#3](https://github.com/ARGOeu/argo-mon-status-api/pull/3) ARGO-5145 Implement Endpoints for Status Page API (Status, Status Pages & CRUD Operations)
- [#5](https://github.com/ARGOeu/argo-mon-status-api/pull/5) ARGO-5147 argo-mon-status-api pom issue preventing jenkins deployment
- [#9](https://github.com/ARGOeu/argo-mon-status-api/pull/9) ARGO-5158 Implement UserEndpoint for user registration and profile management
- [#10](https://github.com/ARGOeu/argo-mon-status-api/pull/10) ARGO-5162 Implement Logo Upload Support for Status Pages
- [#11](https://github.com/ARGOeu/argo-mon-status-api/pull/11) ARGO-5166 Implement entitlement-based access control for Status Pages API
- [#13](https://github.com/ARGOeu/argo-mon-status-api/pull/13) ARGO-51176 Delete tenant
- [#14](https://github.com/ARGOeu/argo-mon-status-api/pull/14) ARGO-5182 Implement Project Metadata Management (CRUD) in Status Pages API
- [#24](https://github.com/ARGOeu/argo-mon-status-api/pull/24) ARGO-5178 ARGO-5179 ARGO-5177 Create/Read/Update Tenant
- [#13](https://github.com/ARGOeu/argo-mon-status-api/pull/13) ARGO-51176 Delete tenant
- [#23](https://github.com/ARGOeu/argo-mon-status-api/pull/23) ARGO-5188 Get List of Tenants
- [#31](https://github.com/ARGOeu/argo-mon-status-api/pull/31) ARGO-5199 Add search,sort and order and unique name validation for Projects
- [#33](https://github.com/ARGOeu/argo-mon-status-api/pull/33) ARGO-5174 Add Contacts to tenant
- [#34](https://github.com/ARGOeu/argo-mon-status-api/pull/34) ARGO-5206 Assign Multiple Projects to a Tenant & Tenant–Project Managment
- [#35](https://github.com/ARGOeu/argo-mon-status-api/pull/35) ARGO-5173 Add infrastracture related metadata to tenant
- [#36](https://github.com/ARGOeu/argo-mon-status-api/pull/36) ARGO-5211: Add role and subgroup handling to authorization
- [#39](https://github.com/ARGOeu/argo-mon-status-api/pull/39) ARGO-5225 Implement Keycloak AGM Group Management Integration for Tenants
- [#40](https://github.com/ARGOeu/argo-mon-status-api/pull/40) ARGO-5218 ARGO-5219 Add Tenant Status Field to Store Background Job Execution State/Implement PUT /v1/tenant/{tenantId}/status Endpoint for Updating Tenant Status #40
- [#41](https://github.com/ARGOeu/argo-mon-status-api/pull/41) ARGO-5236 Prevent Project deletion when project belongs to tenant
- [#42](https://github.com/ARGOeu/argo-mon-status-api/pull/42/) ARGO-5223 Implement tenants list endpoint with role-based filtering for admin and viewer roles
- [#43](https://github.com/ARGOeu/argo-mon-status-api/pull/43) ARGO-5228: Prevent Tenant and Project name changes on update and add Project description
- [#44](https://github.com/ARGOeu/argo-mon-status-api/pull/44) ARGO-5229 Expose user group memberships and roles in user profile
- [#46](https://github.com/ARGOeu/argo-mon-status-api/pull/46) ARGO-5243 Design & Implement Group Membership Endpoints Using AGM
- [#48](https://github.com/ARGOeu/argo-mon-status-api/pull/48) ARGO-5245 Add Admin Endpoint to Create Tenant Group and Expose Group Status
- [#49](https://github.com/ARGOeu/argo-mon-status-api/pull/49) ARGO-5248 Protect automation endpoints
- [#50](https://github.com/ARGOeu/argo-mon-status-api/pull/50) ARGO-5263 Add execution mode for tenant jobs
- [#53](https://github.com/ARGOeu/argo-mon-status-api/pull/53) ARGO-5279 Introduce job properties for tenant status events
- [#55](https://github.com/ARGOeu/argo-mon-status-api/pull/55) ARGO-5296 Mock AMS in Dev profile mode
- [#56](https://github.com/ARGOeu/argo-mon-status-api/pull/56) ARGO-5280 Add execution mode for tenant jobs
- [#57](https://github.com/ARGOeu/argo-mon-status-api/pull/57) ARGO-5281 Tenant Invitation Management
- [#58](https://github.com/ARGOeu/argo-mon-status-api/pull/58) ARGO-5298 Include User Tenants in Users Response
- [#59](https://github.com/ARGOeu/argo-mon-status-api/pull/59) ARGO-5300 Initialize manual jobs in events for jobs in tenant's status 
- [#61](https://github.com/ARGOeu/argo-mon-status-api/pull/61) ARGO-5304 Add init_compute_engine as a job in the status of tenant
- [#62](https://github.com/ARGOeu/argo-mon-status-api/pull/62) ARGO-5303 Allow Super Admins to add members to tenant groups directly
- [#63](https://github.com/ARGOeu/argo-mon-status-api/pull/63) ARGO-5302 Add search and pagination support to admin members endpoint
- [#65](https://github.com/ARGOeu/argo-mon-status-api/pull/65) ARGO-5308 Allow super admin and admin to revoke invitations
- [#69](https://github.com/ARGOeu/argo-mon-status-api/pull/69) ARGO-5301 Mock WebApi in Dev profile
- [#76](https://github.com/ARGOeu/argo-mon-status-api/pull/76) ARGO-5338 Retrieve tenant's report by id
- [#81](https://github.com/ARGOeu/argo-mon-status-api/pull/81) ARGO-5342 Create GET endpoints for Profiles
- [#82](https://github.com/ARGOeu/argo-mon-status-api/pull/82) ARGO-5344 Status-api support the ability to create status pages by tenant name
- [#83](https://github.com/ARGOeu/argo-mon-status-api/pull/83) ARGO-5351 Support Multiple Roles in Authorization Entitlement Checks
- [#84](https://github.com/ARGOeu/argo-mon-status-api/pull/84) ARGO-5336 check tenant readiness - quarkus api
- [#86](https://github.com/ARGOeu/argo-mon-status-api/pull/86) ARGO-5365 Add conversion from page/size parameters to first/max for pagination
- [#85](https://github.com/ARGOeu/argo-mon-status-api/pull/85) ARGO-5361 Fetch Tenant's Reports
- [#91](https://github.com/ARGOeu/argo-mon-status-api/pull/91) ARGO-5369 Exclude CHECK_READINESS from jobs list in status
- [#93](https://github.com/ARGOeu/argo-mon-status-api/pull/93) ARGO-5373 Reports order by active first
- [#94](https://github.com/ARGOeu/argo-mon-status-api/pull/94/) ARGO-5374 List tenant status pages based on tenant role
- [#95](https://github.com/ARGOeu/argo-mon-status-api/pull/95) ARGO-5378 Create seperate endpoint to notify ams about CHECK READINESS event
- [#96](https://github.com/ARGOeu/argo-mon-status-api/pull/96) ARGO-5382 Implement System Health Check Endpoint
- [#97](https://github.com/ARGOeu/argo-mon-status-api/pull/97) ARGO-5384 Re-Appear CHECK_READINESS in job list
- [#98](https://github.com/ARGOeu/argo-mon-status-api/pull/98) ARGO-5386 Change tenant admin access to endpoints
- [#102](https://github.com/ARGOeu/argo-mon-status-api/pull/102) ARGO-5396 Improve error messages in Argo Monitoring Status
- [#103](https://github.com/ARGOeu/argo-mon-status-api/pull/103) ARGO-5398 Expose uid in User Profile and Status Members responses
- [#104](https://github.com/ARGOeu/argo-mon-status-api/pull/104) ARGO-5401 Remove endpoints from Admin Resource that are common with Tenant Resource
- [#106](https://github.com/ARGOeu/argo-mon-status-api/pull/106) ARGO-5419 ARGO-5432 ARGO-5433 CRUD topology
- [#108](https://github.com/ARGOeu/argo-mon-status-api/pull/108) ARGO-5443 Support node info in tenants and set reports as default
- [#112](https://github.com/ARGOeu/argo-mon-status-api/pull/112) ARGO-5461 Sync Web API Tenants to Local DB (Dev Environment Only)
- [#115](https://github.com/ARGOeu/argo-mon-status-api/pull/115) ARGO-5477 Support force parameter when creating new topology items
- [#118](https://github.com/ARGOeu/argo-mon-status-api/pull/118) ARGO-5491 Status-api generate an INIT_CONNECTOR event
- [#120](https://github.com/ARGOeu/argo-mon-status-api/pull/120) ARGO-5496 Add option for status page theming configuration
- [#126](https://github.com/ARGOeu/argo-mon-status-api/pull/126) ARGO-5528 Implement topology feed management for tenant connectors
- [#128](https://github.com/ARGOeu/argo-mon-status-api/pull/128) ARGO-5538 Add support for ARGO Web API summary capability endpoint in Status ARGO
- [#130](https://github.com/ARGOeu/argo-mon-status-api/pull/130) ARGO-5545 Trigger INIT_POEM AMS notification after INIT_COMPUTE_ENGINE completion
- [#131](https://github.com/ARGOeu/argo-mon-status-api/pull/131) ARGO-5547 argo-status api nodes capabilities should also be accessible using the node name
- [#132](https://github.com/ARGOeu/argo-mon-status-api/pull/132) ARGO-5549 Proxy group results and status calls from web-api
- [#139](https://github.com/ARGOeu/argo-mon-status-api/pull/139) ARGO-5552 Extend theming option field values for status page creation
- [#148](https://github.com/ARGOeu/argo-mon-status-api/pull/148) ARGO-5589 Make INIT_MONITORING_BOX event automatic
- [#149](https://github.com/ARGOeu/argo-mon-status-api/pull/149) ARGO-5593: Status api: support setting reports as public (and back to private)
- [#154](https://github.com/ARGOeu/argo-mon-status-api/pull/154) ARGO-5619 Support desy-marketplace topology feed
- [#155](https://github.com/ARGOeu/argo-mon-status-api/pull/155) ARGO-5620: Status api should support the ability to serve publicly available results
- [#156](https://github.com/ARGOeu/argo-mon-status-api/pull/156) ARGO-5627 Include tenant info in public status page response
- [#159](https://github.com/ARGOeu/argo-mon-status-api/pull/159) ARGO-5624 Generate a DELETE_TENANT event when a tenant is deleted
- [#164](https://github.com/ARGOeu/argo-mon-status-api/pull/164/) ARGO-5638 Consume memberships from Quarkus Auth and resolve tenant names
- [#165](https://github.com/ARGOeu/argo-mon-status-api/pull/165) ARGO-5640 Status-api: Create a public call that displays a list of public reports available for the tenant
- [#166](https://github.com/ARGOeu/argo-mon-status-api/pull/166) ARGO-5645: Automatically add missing tenant status jobs during status updates
- [#167](https://github.com/ARGOeu/argo-mon-status-api/pull/167) ARGO-5658 Expose Recommended Role and Assignment Metadata in Status API
  [#168](https://github.com/ARGOeu/argo-mon-status-api/pull/168) ARGO-5654 Extend Resource Authorization to Support the Resources in the status-pages
- [#168](https://github.com/ARGOeu/argo-mon-status-api/pull/168) ARGO-5654 Extend Resource Authorization to Support the Resources in the status-pages
- [#169](https://github.com/ARGOeu/argo-mon-status-api/pull/169) ARGO-5662 Enable Access-Control-Allow-Origin: * only for public GET endpoints 
- [#175](https://github.com/ARGOeu/argo-mon-status-api/pull/175) ARGO-5667: Status api: for public reports call provide also info if a report is node report
- [#171](https://github.com/ARGOeu/argo-mon-status-api/pull/171) ARGO-5671 - Integrate RCIAM Keycloak authentication
- [#145](https://github.com/ARGOeu/argo-mon-status-api/pull/145) ARGO-5573 Add all/mine Scope Support to Secured Endpoints and Role Assignments
- [#141](https://github.com/ARGOeu/argo-mon-status-api/pull/141) ARGO-5685 Super admin view all tenant pages
- [#140](https://github.com/ARGOeu/argo-mon-status-api/pull/140) ARGO-5686 Add scope to list all tenant pages
- [#134](https://github.com/ARGOeu/argo-mon-status-api/pull/134) ARGO-5525 Assign Secured Endpoints to Roles
- [#129](https://github.com/ARGOeu/argo-mon-status-api/pull/129) ARGO-5541 Use quarkus-auth deps in project
- [#122](https://github.com/ARGOeu/argo-mon-status-api/pull/122) ARGO-5689 Add group roles to group response
- [#116](https://github.com/ARGOeu/argo-mon-status-api/pull/122) ARGO-5483: Make endpoint topology tags field accept any key-value pairs

### Fix

- [#7](https://github.com/ARGOeu/argo-mon-status-api/pull/7) ARGO-5152: Update Keycloak Redirect URIs and Enable CORS for Local UI Development
- [#8](https://github.com/ARGOeu/argo-mon-status-api/pull/8) ARGO-5156 ARGO-MON-STATUS-API: Downgrade project postgres version to 11
- [#16](https://github.com/ARGOeu/argo-mon-status-api/pull/16) ARGO-5186 Rename tenant creation script to avoid conflict in flyway
- [#25](https://github.com/ARGOeu/argo-mon-status-api/pull/25) ARGO-5190 Fix website in tenant to be empty #25
- [#26](https://github.com/ARGOeu/argo-mon-status-api/pull/26) ARGO-5194 Fix the update of the tenant to not contain checks
- [#28](https://github.com/ARGOeu/argo-mon-status-api/pull/28) ARGO-5200 FIx Upload Image
- [#29](https://github.com/ARGOeu/argo-mon-status-api/pull/29) ARGO-5202 Fix leftover logo files when updating StatusPage logos and Tenant images
- [#32](https://github.com/ARGOeu/argo-mon-status-api/pull/32) ARGO-5203 ARGO-5204 Handle Database Failures/ Apply one search param to tenant and also sort, order to be defined by user
- [#37](https://github.com/ARGOeu/argo-mon-status-api/pull/37) ARGO-5210 [Status] - api Get contacts add tenant id and name
- [#41](https://github.com/ARGOeu/argo-mon-status-api/pull/41) ARGO-5236 Prevent Project deletion when project belongs to tenant
- [#45](https://github.com/ARGOeu/argo-mon-status-api/pull/45) ARGO-5217 PUBLISH events to AMS to inform about tenant's creation and triggering the python script jobs
- [#47](https://github.com/ARGOeu/argo-mon-status-api/pull/47) ARGO-5244 Fix WebApi application.properties configuration
- [#51](https://github.com/ARGOeu/argo-mon-status-api/pull/51) ARGO-5276 Restrict super_admin bypass on automation endpoints
- [#52](https://github.com/ARGOeu/argo-mon-status-api/pull/52) ARGO-5278 Improve tenant tests: consistent mock tenant ID and AMS isolation
- [#54](https://github.com/ARGOeu/argo-mon-status-api/pull/54) ARGO-5282 Harmonize EventStatus, EventName enum values to always be UPPERCASE
- [#60](https://github.com/ARGOeu/argo-mon-status-api/pull/60) ARGO-5281 Tenant Invitation Management (fix)
- [#64](https://github.com/ARGOeu/argo-mon-status-api/pull/64) ARGO-5309 Permit Tenant Name with Nums & add missing manual init_monitoring_box job
- [#66](https://github.com/ARGOeu/argo-mon-status-api/pull/66) ARGO-5310 Allow Super Admins to search members by user ID
- [#67](https://github.com/ARGOeu/argo-mon-status-api/pull/67) ARGO-5314 Tenant users wiped out in web-api envrihub and aquainfra
- [#68](https://github.com/ARGOeu/argo-mon-status-api/pull/68) ARGO-5313: View invitation in email is not working
- [#70](https://github.com/ARGOeu/argo-mon-status-api/pull/70) ARGO-5324 Enforce tenant name immutability and validate contacts list
- [#71](https://github.com/ARGOeu/argo-mon-status-api/pull/71) ARGO-5327 API should default missing optional fields to empty collections or empty objects instead of null
- [#72](https://github.com/ARGOeu/argo-mon-status-api/pull/72) ARGO-5333 Prevent db_conf.database Override on Tenant Update & Consolidate Web API Update Call
- [#75](https://github.com/ARGOeu/argo-mon-status-api/pull/75) ARGO-5337 Use voperson_id as username
- [#73](https://github.com/ARGOeu/argo-mon-status-api/pull/70) ARGO-5336 Enable Trust-All TLS Configuration for argo-web-api REST Client in Dev Profile
- [#77](https://github.com/ARGOeu/argo-mon-status-api/pull/77) ARGO-5319 Make the web-api error better for users
- [#78](https://github.com/ARGOeu/argo-mon-status-api/pull/78) ARGO-5340 Improve invitation links and async group assignment on accept
- [#79](https://github.com/ARGOeu/argo-mon-status-api/pull/79) ARGO-5348 Fix report id to fetch report
- [#80](https://github.com/ARGOeu/argo-mon-status-api/pull/80) ARGO-5349 Fix add user to tenant group via invitation
- [#87](https://github.com/ARGOeu/argo-mon-status-api/pull/87) ARGO-5366 Fix @POST to @GET to fetch reports
- [#88](https://github.com/ARGOeu/argo-mon-status-api/pull/88) ARGO-5367: Resend existing pending tenant invitation instead of returing 409
- [#89](https://github.com/ARGOeu/argo-mon-status-api/pull/89) ARGO-5690 Fix members pagination
- [#90](https://github.com/ARGOeu/argo-mon-status-api/pull/90) ARGO-5368 Fix Filter Tags to tenant's report
- [#92](https://github.com/ARGOeu/argo-mon-status-api/pull/92) ARGO-5372 List tenant status pages based on tenant role
- [#99](https://github.com/ARGOeu/argo-mon-status-api/pull/99) ARGO-5387 Improve error handling (Group Management + Invitations)
- [#100](https://github.com/ARGOeu/argo-mon-status-api/pull/100) ARGO-5391 FIX "id to load is required for loading" error when accessing tenant admin the v1/tenants/contact-types
- [#101](https://github.com/ARGOeu/argo-mon-status-api/pull/101) ARGO-5393 Improve API error message when required path parameter is missing
- [#105](https://github.com/ARGOeu/argo-mon-status-api/pull/105) ARGO-5414 Add JavaDoc descriptions to service and repository methods
- [#107](https://github.com/ARGOeu/argo-mon-status-api/pull/107) ARGO-5431 Status-api returns 502 in reports when a tenant is not fully ready
- [#109](https://github.com/ARGOeu/argo-mon-status-api/pull/109) ARGO-5446 Validate tenant initialization when x-tenant-id header is used
- [#111](https://github.com/ARGOeu/argo-mon-status-api/pull/111) ARGO-5470 DELETE topology if already exists while trying to recreate
- [#119](https://github.com/ARGOeu/argo-mon-status-api/pull/119) ARGO-5495 INIT_COMPUTE_ENGINE status not set to completed
- [#121](https://github.com/ARGOeu/argo-mon-status-api/pull/121) ARGO-5504 Trigger INIT_TOPOLOGY_CONNECTOR after compute engine completion (fix)
- [#123](https://github.com/ARGOeu/argo-mon-status-api/pull/123) ARGO-5514 INIT_COMPUTE_ENGINE remains IN_PROGRESS after completion and event trigger
- [#127](https://github.com/ARGOeu/argo-mon-status-api/pull/127) ARGO-5537 Improve Argo Web API error handling and logging in Status API
- [#133](https://github.com/ARGOeu/argo-mon-status-api/pull/133) ARGO-5551 INIT_POEM cannot be changed to completed
- [#142](https://github.com/ARGOeu/argo-mon-status-api/pull/142) ARGO-5582 Implement entitlement-based tenant member resolution
- [#143](https://github.com/ARGOeu/argo-mon-status-api/pull/143) ARGO-5583 Update user profile entitlement mapping to support resource-based tenant roles
- [#146](https://github.com/ARGOeu/argo-mon-status-api/pull/146) ARGO-5585 Status-api: when a tenant has no data send an appropriate message
- [#147](https://github.com/ARGOeu/argo-mon-status-api/pull/147) ARGO-5602 Refactor status-api tests and adapt imports after quarkus-auth package changes
- [#150](https://github.com/ARGOeu/argo-mon-status-api/pull/150) ARGO-5606: Continue tenant listing when tenant data retrieval fails
- [#152](https://github.com/ARGOeu/argo-mon-status-api/pull/152) ARGO-5622 Prevent lost updates during concurrent tenant status modifications
- [#157](https://github.com/ARGOeu/argo-mon-status-api/pull/157) ARGO-5629 Update feed topology regression
- [#160](https://github.com/ARGOeu/argo-mon-status-api/pull/160) ARGO-5631 Fix topology integration automation event handling
- [#161](https://github.com/ARGOeu/argo-mon-status-api/pull/161) ARGO-5632 Replace free-text topology feed types with enum values
- [#162](https://github.com/ARGOeu/argo-mon-status-api/pull/162) ARGO-5635 Remove tenant_name and tenant_image from StatusPageConfigDto
- [#176](https://github.com/ARGOeu/argo-mon-status-api/pull/176) ARGO-5675 Reports are not fetched for tenant
- [#177](https://github.com/ARGOeu/argo-mon-api/pull/177) ARGO-5676 Topology Connector and Integrator Topo jobs should appear after topology is configured
- [#179](https://github.com/ARGOeu/argo-mon-api/pull/179) ARGO-5605 Message when pushing check readiness
- [#178](https://github.com/ARGOeu/argo-mon-api/pull/178) ARGO-5681 to trigger deploy
- [#174](https://github.com/ARGOeu/argo-mon-status-api/pull/174) ARGO-5682 add PATCH to cors
- [#173](https://github.com/ARGOeu/argo-mon-status-api/pull/173) ARGO-5683 minor fix in cors
- [#172](https://github.com/ARGOeu/argo-mon-status-api/pull/172) ARGO-5684 Add missing prod properties
- [#151](https://github.com/ARGOeu/argo-mon-status-api/pull/151) ARGO-5618 Tenants not retrieved for other roles than admin
- [#138](https://github.com/ARGOeu/argo-mon-status-api/pull/138) ARGO-5687 Fix entitlement searching for list tenants
- [#135](https://github.com/ARGOeu/argo-mon-status-api/pull/135) ARGO-5688 Enhance tenant invitation with quarkus auth role management
- [#114](https://github.com/ARGOeu/argo-mon-status-api/pull/114) ARGO-5471: Add node capabilities endpoints for availability and status
- [#199](https://github.com/ARGOeu/argo-mon-status-api/pull/199) ARGO-5670 Fix Quarkus Authorization CDI integration after Quarkus Auth API changed

### Removed

-[#15](https://github.com/ARGOeu/argo-mon-status-api/pull/15) ARGO-5183 Remove User DB & Update Keycloak Scope
-[#170](https://github.com/ARGOeu/argo-mon-status-api/pull/170) ARGO-5666 [MON-STATUS-API] remove endpoints : assign secured endpoints to role