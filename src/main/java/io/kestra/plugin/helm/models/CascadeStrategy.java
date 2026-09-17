package io.kestra.plugin.helm.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "How dependent resources are deleted",
    description = "Maps to Helm's `--cascade` flag."
)
public enum CascadeStrategy {
    @Schema(title = "Delete dependents in the background, Helm's default")
    BACKGROUND("background"),

    @Schema(title = "Leave dependents in place")
    ORPHAN("orphan"),

    @Schema(title = "Delete dependents first; combine with `wait` to ensure finalizers complete before the task returns")
    FOREGROUND("foreground");

    private final String flagValue;

    CascadeStrategy(String flagValue) {
        this.flagValue = flagValue;
    }

    public String flagValue() {
        return flagValue;
    }
}
