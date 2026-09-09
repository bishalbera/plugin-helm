package io.kestra.plugin.helm;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.models.tasks.runners.TaskRunner;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
import io.kestra.plugin.scripts.runner.docker.Docker;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractHelm extends Task {
    private static final String DEFAULT_IMAGE = "alpine/helm:3.21.4";

    protected static final ObjectMapper JSON = JacksonMapper.ofJson();

    @Schema(
        title = "Task runner",
        description = "Runner used to launch the Helm CLI container; defaults to Docker with an empty entrypoint."
    )
    @Builder.Default
    @PluginProperty(group = "execution")
    @Valid
    protected TaskRunner<?> taskRunner = Docker.builder()
        .type(Docker.class.getName())
        .entryPoint(new ArrayList<>())
        .build();

    @Schema(
        title = "Container image",
        description = "Image providing the `helm` binary. Defaults to `" + DEFAULT_IMAGE + "`. Pin a Helm 3 image; Helm 4 changes CLI and output semantics this plugin does not yet support."
    )
    @Builder.Default
    @PluginProperty(group = "execution")
    protected Property<String> containerImage = Property.ofValue(DEFAULT_IMAGE);

    @Schema(
        title = "Additional environment variables",
        description = "Extra environment variables injected into the Helm container, for registry credentials or proxy settings."
    )
    @PluginProperty(group = "execution")
    protected Property<Map<String, String>> env;

    protected ScriptOutput execute(
        RunContext runContext,
        List<String> commands,
        List<String> outputFiles,
        Map<String, String> extraEnv
    ) throws Exception {
        Map<String, String> environment = new HashMap<>(
            runContext.render(this.env).asMap(String.class, String.class)
        );
        if (extraEnv != null) {
            environment.putAll(extraEnv);
        }

        return new CommandsWrapper(runContext)
            .withTaskRunner(this.taskRunner)
            .withContainerImage(runContext.render(this.containerImage).as(String.class).orElseThrow())
            .withEnv(environment)
            .withInterpreter(Property.ofValue(List.of("/bin/sh", "-c")))
            .withCommands(Property.ofValue(commands))
            .withOutputFiles(outputFiles)
            .withFailFast(true)
            .withBeforeCommandsWithOptions(true)
            .run();
    }

    protected String readOutputFile(RunContext runContext, ScriptOutput output, String name) throws Exception {
        URI uri = output.getOutputFiles().get(name);

        if (uri == null) {
            throw new IllegalStateException("Helm did not produce the expected output file '" + name + "'.");
        }

        try (InputStream stream = runContext.storage().getFile(uri)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
