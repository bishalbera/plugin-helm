package io.kestra.plugin.helm.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "How Helm waits for resources to become ready",
    description = "Maps to Helm 4's `--wait` flag. When unset, Helm applies its own default of `HOOK_ONLY`."
)
public enum WaitStrategy {
    @Schema(title = "Wait until all resources are ready, up to `timeout`")
    WATCHER("watcher"),

    @Schema(title = "Wait only for hooks, Helm's default when `--wait` is omitted")
    HOOK_ONLY("hookOnly"),

    @Schema(title = "Use Helm's legacy readiness polling")
    LEGACY("legacy");

    private final String flagValue;

    WaitStrategy(String flagValue) {
        this.flagValue = flagValue;
    }

    public String flagValue() {
        return flagValue;
    }
}
