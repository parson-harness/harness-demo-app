package io.harness.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.demo.config.AppConfig;
import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.client.SplitFactoryBuilder;
import io.split.client.api.SplitResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
public class RuntimeConfigService {
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final boolean fmeEnabled;
    private final String fmeSdkKey;
    private final String fmeFlagName;

    private volatile SplitFactory splitFactory;
    private volatile SplitClient splitClient;
    private volatile String initializationError = "";

    public RuntimeConfigService(
        AppConfig appConfig,
        ObjectMapper objectMapper,
        @Value("${fme.enabled:false}") boolean fmeEnabled,
        @Value("${fme.sdk-key:}") String fmeSdkKey,
        @Value("${fme.flag-name:runtime_experience_config}") String fmeFlagName
    ) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.fmeEnabled = fmeEnabled;
        this.fmeSdkKey = fmeSdkKey;
        this.fmeFlagName = fmeFlagName;
    }

    @PostConstruct
    public void initialize() {
        if (!fmeEnabled) {
            return;
        }
        if (fmeSdkKey == null || fmeSdkKey.isBlank()) {
            initializationError = "FME is enabled but no SDK key is configured";
            return;
        }

        try {
            SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(2000)
                .build();
            splitFactory = SplitFactoryBuilder.build(fmeSdkKey, config);
            splitClient = splitFactory.client();
            splitClient.blockUntilReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            initializationError = "FME SDK initialization was interrupted";
        } catch (TimeoutException e) {
            initializationError = "FME SDK timed out while waiting for initialization";
        } catch (Exception e) {
            initializationError = e.getMessage() == null ? "FME SDK initialization failed" : e.getMessage();
        }
    }

    @PreDestroy
    public void destroy() {
        if (splitFactory != null && !splitFactory.isDestroyed()) {
            splitFactory.destroy();
        }
    }

    public AppConfig resolveConfig() {
        AppConfig resolved = appConfig.copy();
        populateBaseRuntimeMetadata(resolved);

        if (!fmeEnabled) {
            resolved.setRuntimeConfigStatus("deployment-only");
            resolved.setRuntimeConfigResolvedBy("Harness Pipeline");
            resolved.setRuntimeConfigLayering("application defaults -> deployment config");
            return resolved;
        }

        resolved.setRuntimeConfigEnabled(true);
        resolved.setRuntimeConfigFlag(fmeFlagName);
        resolved.setRuntimeConfigTargetKey(buildTargetKey(resolved));
        resolved.setRuntimeConfigLayering("application defaults -> deployment config -> Harness FME runtime overrides");
        resolved.setRuntimeConfigRestartRequirement("FME changes apply without rebuild or restart; deployment config changes require rollout");
        resolved.setRuntimeConfigRollbackStrategy("FME treatment rollback or kill switch for runtime config; pipeline rollback for deployment config");
        resolved.setRuntimeConfigSecretHandling("FME for non-sensitive config only; secrets stay in Kubernetes Secret");
        resolved.setRuntimeConfigAuditTrail("Harness FME change history + Harness pipeline execution history");

        if (splitClient == null) {
            resolved.setRuntimeConfigStatus("sdk-unavailable");
            resolved.setRuntimeConfigResolvedBy("Harness Pipeline");
            resolved.setRuntimeConfigAvailable(false);
            resolved.setRuntimeConfigError(initializationError);
            return resolved;
        }

        try {
            Map<String, Object> attributes = buildAttributes(resolved);
            SplitResult result = splitClient.getTreatmentWithConfig(
                resolved.getRuntimeConfigTargetKey(),
                fmeFlagName,
                attributes,
                null
            );

            resolved.setRuntimeConfigAvailable(true);
            resolved.setRuntimeConfigResolvedBy("Harness FME Java SDK");

            if (result == null) {
                resolved.setRuntimeConfigStatus("no-result");
                resolved.setRuntimeConfigTreatment("control");
                return resolved;
            }

            resolved.setRuntimeConfigTreatment(result.treatment());

            if (result.treatment() == null || result.treatment().isBlank() || "control".equalsIgnoreCase(result.treatment())) {
                resolved.setRuntimeConfigStatus("control");
                resolved.setRuntimeConfigError("FME returned the control treatment");
                return resolved;
            }

            if (result.config() == null || result.config().isBlank()) {
                resolved.setRuntimeConfigStatus("runtime-connected");
                return resolved;
            }

            RuntimeConfigPayload payload = objectMapper.readValue(result.config(), RuntimeConfigPayload.class);
            applyOverrides(resolved, payload);
            resolved.setRuntimeConfigOverridesActive(true);
            resolved.setRuntimeConfigStatus("runtime-overrides-active");
            return resolved;
        } catch (Exception e) {
            resolved.setRuntimeConfigStatus("runtime-error");
            resolved.setRuntimeConfigError(e.getMessage() == null ? "Runtime config evaluation failed" : e.getMessage());
            return resolved;
        }
    }

    private void populateBaseRuntimeMetadata(AppConfig resolved) {
        resolved.setRuntimeConfigEnabled(false);
        resolved.setRuntimeConfigAvailable(false);
        resolved.setRuntimeConfigOverridesActive(false);
        resolved.setRuntimeConfigStatus("deployment-only");
        resolved.setRuntimeConfigTreatment("not-evaluated");
        resolved.setRuntimeConfigFlag(fmeFlagName);
        resolved.setRuntimeConfigTargetKey(buildTargetKey(resolved));
        resolved.setRuntimeConfigResolvedBy("Harness Pipeline");
        resolved.setRuntimeConfigLayering("application defaults -> deployment config");
        resolved.setRuntimeConfigRestartRequirement("Deployment-time config changes require rollout");
        resolved.setRuntimeConfigRollbackStrategy("Pipeline rollback");
        resolved.setRuntimeConfigSecretHandling("Secrets stay in Kubernetes Secret");
        resolved.setRuntimeConfigAuditTrail("Harness Pipeline execution history");
        resolved.setRuntimeConfigError("");
    }

    private Map<String, Object> buildAttributes(AppConfig resolved) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("environment", safeValue(resolved.getEnvironment()));
        attributes.put("region", safeValue(resolved.getRegion()));
        attributes.put("tenant", safeValue(resolved.getConfigTenant()));
        attributes.put("deploymentTarget", safeValue(resolved.getDeploymentTarget()));
        attributes.put("releaseRing", safeValue(resolved.getConfigReleaseRing()));
        attributes.put("profile", safeValue(resolved.getConfigProfile()));
        attributes.put("namespace", safeValue(resolved.getNamespace()));
        return attributes;
    }

    private String buildTargetKey(AppConfig resolved) {
        return String.join("_",
            sanitizeIdentifier(resolved.getEnvironment()),
            sanitizeIdentifier(resolved.getConfigTenant()),
            sanitizeIdentifier(resolved.getRegion()),
            sanitizeIdentifier(resolved.getDeploymentTarget())
        );
    }

    private void applyOverrides(AppConfig resolved, RuntimeConfigPayload payload) {
        if (payload == null) {
            return;
        }
        if (hasText(payload.getProfile())) {
            resolved.setConfigProfile(payload.getProfile().trim());
        }
        if (hasText(payload.getBanner())) {
            resolved.setConfigBanner(payload.getBanner().trim());
        }
        if (hasText(payload.getTenant())) {
            resolved.setConfigTenant(payload.getTenant().trim());
        }
        if (hasText(payload.getReleaseRing())) {
            resolved.setConfigReleaseRing(payload.getReleaseRing().trim());
        }
        if (hasText(payload.getTarget())) {
            resolved.setConfigTarget(payload.getTarget().trim());
        }
        if (hasText(payload.getSupportContact())) {
            resolved.setConfigSupportContact(payload.getSupportContact().trim());
        }
        if (hasText(payload.getVersion())) {
            resolved.setConfigVersion(payload.getVersion().trim());
        }
        if (hasText(payload.getSource())) {
            resolved.setConfigSource(payload.getSource().trim());
        } else {
            resolved.setConfigSource("Harness FME Runtime Configuration");
        }
    }

    private String sanitizeIdentifier(String value) {
        String normalized = safeValue(value).replaceAll("[^A-Za-z0-9.@_-]", "_");
        return normalized.isBlank() ? "default" : normalized;
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "default" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static class RuntimeConfigPayload {
        private String profile;
        private String banner;
        private String tenant;
        private String releaseRing;
        private String target;
        private String supportContact;
        private String version;
        private String source;

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getBanner() {
            return banner;
        }

        public void setBanner(String banner) {
            this.banner = banner;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public String getReleaseRing() {
            return releaseRing;
        }

        public void setReleaseRing(String releaseRing) {
            this.releaseRing = releaseRing;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getSupportContact() {
            return supportContact;
        }

        public void setSupportContact(String supportContact) {
            this.supportContact = supportContact;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
