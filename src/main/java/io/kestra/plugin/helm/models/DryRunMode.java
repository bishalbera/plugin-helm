package io.kestra.plugin.helm.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "Whether the upgrade is simulated rather than applied",
    description = "Maps to Helm 4's `--dry-run` flag. A dry run changes nothing on the cluster and emits no Assets."
)
public enum DryRunMode {
    @Schema(title = "Apply the upgrade for real")
    NONE(null),

    @Schema(title = "Simulate without contacting the cluster")
    CLIENT("client"),

    @Schema(title = "Simulate against the API server, validating the rendered manifests")
    SERVER("server");

    private final String flagValue;

    DryRunMode(String flagValue) {
        this.flagValue = flagValue;
    }

    public String flagValue() {
        return flagValue;
    }

    public boolean enabled() {
        return flagValue != null;
    }
}
