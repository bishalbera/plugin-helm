package io.kestra.plugin.helm.models;

import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonCreator;

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
    description = "Either a plain path string relative to the working directory, or an object with a `git` block."
)
public class ValuesSource {
    @Schema(
        title = "Path to a values file",
        description = "Relative to the task's working directory."
    )
    private Property<String> path;

    @Schema(
        title = "Values file sourced from Git"
    )
    private GitSource git;

    @JsonCreator
    public static ValuesSource of(String path) {
        return ValuesSource.builder().path(Property.ofValue(path)).build();
    }

    public ResolvedValues resolve(RunContext runContext, int index) throws Exception {
        String rPath = runContext.render(this.path).as(String.class).orElse(null);

        if ((rPath == null) == (this.git == null)) {
            throw new IllegalArgumentException("Each `valuesFrom` entry must set exactly one of `path` or `git`.");
        }

        if (this.git != null) {
            Path checkout = this.git.checkout(runContext, "values-" + index);
            return new ResolvedValues(
                runContext.workingDir().path().relativize(checkout).toString(),
                this.git.reference(runContext)
            );
        }

        return new ResolvedValues(rPath, rPath);
    }

    public record ResolvedValues(String path, String reference) {
    }
}
