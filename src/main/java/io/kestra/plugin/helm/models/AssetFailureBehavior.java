package io.kestra.plugin.helm.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    title = "What to do when Asset emission fails"
)
public enum AssetFailureBehavior {
    @Schema(title = "Log a warning and let the task succeed")
    WARN,

    @Schema(title = "Fail the task")
    FAIL
}
