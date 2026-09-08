package io.kestra.plugin.helm.models;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@Schema(
    title = "A values file to pass to Helm",
    description = "A path relative to the task's working directory. To use a values file held in Git, clone it with `io.kestra.plugin.git.Clone` inside a `WorkingDirectory` and point this at the checkout."
)
public class ValuesSource {
    @Schema(
        title = "Path to a values file",
        description = "Relative to the task's working directory."
    )
    @PluginProperty(group = "source")
    private Property<String> path;

    @JsonCreator
    public static ValuesSource of(String path) {
        return ValuesSource.builder().path(Property.ofValue(path)).build();
    }

    public String resolve(RunContext runContext) throws Exception {
        return runContext.render(this.path).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Each `valuesFrom` entry requires a `path`."));
    }
}
