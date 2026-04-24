package io.harness.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private static final Pattern ASG_BUILD_VERSION_PATTERN = Pattern.compile("^(?:.*-)?(\\d+)(?:-[A-Za-z0-9]+)?$");
    
    private String appName = "My Application";
    private String version = "1.0.0";
    private String buildId = "";
    private String environment = "development";
    private String deploymentVariant = "";
    private String deploymentTrack = "";
    private String deploymentStrategy = "";
    private String customerName = "Harness Customer";
    private String customerLogo = "";
    private String deploymentTarget = "kubernetes";
    private String publicUrl = "";
    private String stageUrl = "";
    private String podName = "local";
    private String namespace = "default";
    private String region = "us-east-1";
    private String configProfile = "shared-base";
    private String configBanner = "";
    private String configTenant = "default";
    private String configReleaseRing = "default";
    private String configTarget = "default-target";
    private String configSupportContact = "support@harness.io";
    private String configVersion = "";
    private String configSource = "Harness Pipeline";
    private String secretProvider = "Harness Secret Variable";
    private String dynamicSecret = "";
    private boolean chaosEnabled = true;
    private int chaosLatencyMs = 0;
    private double chaosErrorRate = 0.0;
    private transient volatile boolean asgDeploymentTrackResolved = false;
    private transient String inferredAsgDeploymentTrack = "";
    
    /**
     * Returns the effective deployment variant for display.
     * Blue/Green deployments use harness.io/color label (blue/green).
     * Canary deployments use harness.io/track label (canary/stable).
     * This method returns the appropriate value based on what's set.
     */
    public String getEffectiveVariant() {
        // If color is set (blue/green deployment), use it
        if (deploymentVariant != null && !deploymentVariant.isEmpty()) {
            return deploymentVariant;
        }
        // Otherwise use track (canary deployment)
        String effectiveTrack = getDeploymentTrack();
        if (effectiveTrack != null && !effectiveTrack.isEmpty()) {
            return effectiveTrack;
        }
        return "stable";
    }

    public String getDeploymentTrack() {
        if (deploymentTrack != null && !deploymentTrack.isEmpty()) {
            return deploymentTrack;
        }
        if (!"asg".equalsIgnoreCase(deploymentTarget)) {
            return deploymentTrack;
        }
        return resolveAsgDeploymentTrack();
    }

    public String getDisplayVersion() {
        if (version == null || version.isBlank()) {
            return "1.0.0";
        }
        String trimmed = version.trim();
        if (SEMVER_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }
        Matcher matcher = ASG_BUILD_VERSION_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return "1.0." + matcher.group(1);
        }
        return trimmed;
    }
    
    public String getVariantColor() {
        String variant = getEffectiveVariant();
        if (variant == null) {
            return "#8B5CF6"; // Purple for stable/unknown
        }
        switch (variant.toLowerCase()) {
            case "blue":
                return "#3B82F6"; // Blue
            case "green":
                return "#22C55E"; // Green
            case "canary":
                return "#EAB308"; // Yellow/Amber
            case "stable":
            default:
                return "#8B5CF6"; // Purple for stable
        }
    }

    public String getDisplayCustomerName() {
        if (customerName == null || customerName.isBlank()) {
            return "Harness Customer";
        }
        return customerName.trim();
    }

    public String getDisplayBuildId() {
        if (buildId == null || buildId.isBlank()) {
            return "n/a";
        }
        return buildId.trim();
    }

    public String getDisplayConfigBanner() {
        if (configBanner == null || configBanner.isBlank()) {
            return "No banner configured";
        }
        return configBanner.trim();
    }

    public String getDisplayConfigVersion() {
        if (configVersion == null || configVersion.isBlank()) {
            return "not-set";
        }
        return configVersion.trim();
    }

    public String getDisplayConfigTarget() {
        if (configTarget == null || configTarget.isBlank()) {
            return "default-target";
        }
        return configTarget.trim();
    }

    public String getDisplayConfigSource() {
        if (configSource == null || configSource.isBlank()) {
            return "Harness Pipeline";
        }
        return configSource.trim();
    }

    public String getDisplaySecretProvider() {
        if (secretProvider == null || secretProvider.isBlank()) {
            return "Harness Secret Variable";
        }
        return secretProvider.trim();
    }

    public boolean isDynamicSecretConfigured() {
        return dynamicSecret != null && !dynamicSecret.isBlank();
    }

    public String getDisplayDeploymentTarget() {
        if (deploymentTarget == null || deploymentTarget.isBlank()) {
            return "Unknown Target";
        }

        switch (deploymentTarget.trim().toLowerCase()) {
            case "k8s":
            case "kubernetes":
            case "eks":
                return "Amazon EKS";
            case "ecs":
                return "Amazon ECS";
            case "lambda":
                return "AWS Lambda";
            case "asg":
                return "EC2 Auto Scaling";
            default:
                return humanize(deploymentTarget);
        }
    }

    public String getDisplayDeploymentStrategy() {
        return humanize(resolveEffectiveDeploymentStrategyKey());
    }

    public String getEffectiveDeploymentStrategy() {
        return resolveEffectiveDeploymentStrategyKey();
    }

    public String getDeploymentNarrative() {
        String variant = getEffectiveVariant();
        String strategyKey = resolveEffectiveDeploymentStrategyKey();

        if ("asg".equalsIgnoreCase(deploymentTarget) && ("blue".equalsIgnoreCase(variant) || "green".equalsIgnoreCase(variant))) {
            return "You are viewing the " + variant + " fleet";
        }

        if ("blue".equalsIgnoreCase(variant) || "green".equalsIgnoreCase(variant)) {
            return "You are viewing the " + variant + " release";
        }

        if ("canary".equalsIgnoreCase(variant)) {
            return "You are viewing canary traffic";
        }

        if ("stable".equalsIgnoreCase(variant) && "canary".equalsIgnoreCase(strategyKey)) {
            return "You are viewing the stable production track";
        }

        return "You are viewing the live production release";
    }

    private String resolveEffectiveDeploymentStrategyKey() {
        if (deploymentStrategy != null && !deploymentStrategy.isBlank()) {
            return deploymentStrategy.trim();
        }

        String variant = getEffectiveVariant();
        if ("blue".equalsIgnoreCase(variant) || "green".equalsIgnoreCase(variant)) {
            return "blue-green";
        }
        if ("canary".equalsIgnoreCase(variant) || "stable".equalsIgnoreCase(variant)) {
            return "canary";
        }
        return "standard";
    }

    private String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }

        String[] parts = value.trim().replace('-', ' ').replace('_', ' ').split("\\s+");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase());
            }
        }

        return builder.length() > 0 ? builder.toString() : "Unknown";
    }

    private String resolveAsgDeploymentTrack() {
        if (asgDeploymentTrackResolved) {
            return inferredAsgDeploymentTrack;
        }
        synchronized (this) {
            if (asgDeploymentTrackResolved) {
                return inferredAsgDeploymentTrack;
            }
            inferredAsgDeploymentTrack = inferDeploymentTrackFromAsgName(resolveAutoScalingGroupName());
            asgDeploymentTrackResolved = true;
            return inferredAsgDeploymentTrack;
        }
    }

    protected String inferDeploymentTrackFromAsgName(String autoScalingGroupName) {
        if (autoScalingGroupName == null || autoScalingGroupName.isBlank()) {
            return "";
        }
        return autoScalingGroupName.endsWith("__Canary") ? "canary" : "stable";
    }

    protected String resolveAutoScalingGroupName() {
        String metadataToken = fetchMetadataToken();
        String instanceId = readMetadata("/latest/meta-data/instance-id", metadataToken);
        String metadataRegion = readMetadata("/latest/meta-data/placement/region", metadataToken);
        String resolvedRegion = metadataRegion != null && !metadataRegion.isBlank() ? metadataRegion.trim() : region;
        if (instanceId == null || instanceId.isBlank() || resolvedRegion == null || resolvedRegion.isBlank()) {
            return "";
        }

        try {
            Process process = new ProcessBuilder(
                "aws",
                "autoscaling",
                "describe-auto-scaling-instances",
                "--instance-ids",
                instanceId.trim(),
                "--region",
                resolvedRegion,
                "--query",
                "AutoScalingInstances[0].AutoScalingGroupName",
                "--output",
                "text"
            ).start();

            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }

            if (process.exitValue() != 0) {
                return "";
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (output.isEmpty() || "None".equalsIgnoreCase(output) || "null".equalsIgnoreCase(output)) {
                return "";
            }
            return output;
        } catch (Exception e) {
            return "";
        }
    }

    private String fetchMetadataToken() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://169.254.169.254/latest/api/token"))
                .timeout(Duration.ofSeconds(2))
                .header("X-aws-ec2-metadata-token-ttl-seconds", "21600")
                .method("PUT", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private String readMetadata(String path, String metadataToken) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://169.254.169.254" + path))
                .timeout(Duration.ofSeconds(2))
                .GET();

            if (metadataToken != null && !metadataToken.isBlank()) {
                builder.header("X-aws-ec2-metadata-token", metadataToken);
            }

            HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }
}
