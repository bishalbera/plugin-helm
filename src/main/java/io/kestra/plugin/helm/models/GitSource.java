package io.kestra.plugin.helm.models;

import java.nio.file.Path;
import java.util.Optional;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.git.Clone;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
    title = "A chart or values file sourced from a Git repository",
    description = "The repository is cloned into the task's working directory before Helm runs. Cloning and authentication are delegated to the `io.kestra.plugin.git.Clone` task, so the same credentials work here."
)
public class GitSource {
    @Schema(
        title = "Repository URL",
        description = "HTTP(S) or SSH URL of the repository holding the chart or values file."
    )
    @NotNull
    @PluginProperty(group = "connection")
    private Property<String> url;

    @Schema(
        title = "Branch to check out",
        description = "Defaults to the repository's default branch. Mutually exclusive with `tag` and `commit`."
    )
    private Property<String> branch;

    @Schema(
        title = "Tag to check out",
        description = "Pins the clone to a tag. Mutually exclusive with `branch` and `commit`."
    )
    private Property<String> tag;

    @Schema(
        title = "Commit SHA to check out",
        description = "Pins the clone to an exact commit. Mutually exclusive with `branch` and `tag`."
    )
    private Property<String> commit;

    @Schema(
        title = "Path inside the repository",
        description = "For a chart, the directory holding `Chart.yaml` (e.g. `charts/nginx`). For a values file, the path to the file (e.g. `prod/nginx/values.yaml`). Defaults to the repository root."
    )
    private Property<String> path;

    @Schema(
        title = "Username or organization",
        description = "Used for HTTP basic authentication."
    )
    @PluginProperty(secret = true, group = "connection")
    @ToString.Exclude
    private Property<String> username;

    @Schema(
        title = "Password or personal access token",
        description = "Used for HTTP basic authentication. Supply through `{{ secret('...') }}` rather than inline."
    )
    @PluginProperty(secret = true, group = "connection")
    @ToString.Exclude
    private Property<String> password;

    @Schema(
        title = "PEM private key",
        description = "PEM-formatted private key whose public key is registered on the Git server, for SSH authentication."
    )
    @PluginProperty(secret = true, group = "connection")
    @ToString.Exclude
    private Property<String> privateKey;

    @Schema(
        title = "Passphrase for `privateKey`"
    )
    @PluginProperty(secret = true, group = "connection")
    @ToString.Exclude
    private Property<String> passphrase;

    public Path checkout(RunContext runContext, String directory) throws Exception {
        Clone clone = Clone.builder()
            .id("helm-git-checkout")
            .type(Clone.class.getName())
            .url(this.url)
            .branch(this.branch)
            .tag(this.tag)
            .commit(this.commit)
            .username(this.username)
            .password(this.password)
            .privateKey(this.privateKey)
            .passphrase(this.passphrase)
            .directory(Property.ofValue(directory))
            .build();

        runContext.logger().info(
            "Cloning {} into {}",
            runContext.render(this.url).as(String.class).orElseThrow(),
            directory
        );
        clone.run(runContext);

        Path root = runContext.workingDir().resolve(Path.of(directory));
        String subPath = runContext.render(this.path).as(String.class).orElse(null);

        return subPath == null || subPath.isBlank() ? root : root.resolve(subPath);
    }

    public String reference(RunContext runContext) throws Exception {
        String rUrl = runContext.render(this.url).as(String.class).orElseThrow();
        String host = rUrl
            .replaceFirst("^[a-zA-Z0-9+.-]+://", "")
            .replaceFirst("^[^/@]+@", "")
            .replaceFirst("\\.git$", "");

        Optional<String> rCommit = runContext.render(this.commit).as(String.class);
        Optional<String> rTag = runContext.render(this.tag).as(String.class);
        Optional<String> rBranch = runContext.render(this.branch).as(String.class);
        String revision = rCommit.or(() -> rTag).or(() -> rBranch).orElse("HEAD");

        String subPath = runContext.render(this.path).as(String.class).orElse(null);

        return host + "@" + revision + (subPath == null || subPath.isBlank() ? "" : ":" + subPath);
    }
}
