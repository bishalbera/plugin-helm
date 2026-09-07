package io.kestra.plugin.helm.services;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.client.Config;

public final class KubeConfigService {
    private static final String CLUSTER = "kestra";
    private static final String USER = "kestra";
    private static final String CONTEXT = "kestra";

    private KubeConfigService() {
    }

    public static Map<String, Object> toKubeConfig(Config config, String namespace) {
        Map<String, Object> cluster = new LinkedHashMap<>();
        cluster.put("server", config.getMasterUrl());

        String caCertData = normalize(config.getCaCertData());
        if (caCertData != null) {
            cluster.put("certificate-authority-data", caCertData);
        } else if (config.isTrustCerts()) {
            cluster.put("insecure-skip-tls-verify", true);
        }

        Map<String, Object> user = new LinkedHashMap<>();
        String token = config.getOauthToken() != null ? config.getOauthToken() : config.getAutoOAuthToken();
        if (token == null && config.getOauthTokenProvider() != null) {
            token = config.getOauthTokenProvider().getToken();
        }
        if (token != null && !token.isBlank()) {
            user.put("token", token);
        }

        String clientCertData = normalize(config.getClientCertData());
        if (clientCertData != null) {
            user.put("client-certificate-data", clientCertData);
        }

        String clientKeyData = normalize(config.getClientKeyData());
        if (clientKeyData != null) {
            user.put("client-key-data", clientKeyData);
        }

        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            user.put("username", config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().isBlank()) {
            user.put("password", config.getPassword());
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("cluster", CLUSTER);
        context.put("user", USER);
        String resolvedNamespace = namespace != null ? namespace : config.getNamespace();
        if (resolvedNamespace != null && !resolvedNamespace.isBlank()) {
            context.put("namespace", resolvedNamespace);
        }

        Map<String, Object> kubeConfig = new LinkedHashMap<>();
        kubeConfig.put("apiVersion", "v1");
        kubeConfig.put("kind", "Config");
        kubeConfig.put("clusters", List.of(Map.of("name", CLUSTER, "cluster", cluster)));
        kubeConfig.put("users", List.of(Map.of("name", USER, "user", user)));
        kubeConfig.put("contexts", List.of(Map.of("name", CONTEXT, "context", context)));
        kubeConfig.put("current-context", CONTEXT);

        return kubeConfig;
    }

    public static String clusterName(Config config) {
        String masterUrl = config.getMasterUrl();
        if (masterUrl == null || masterUrl.isBlank()) {
            return null;
        }

        return masterUrl
            .replaceFirst("^[a-zA-Z0-9+.-]+://", "")
            .replaceFirst("/.*$", "")
            .replaceFirst(":\\d+$", "");
    }

    private static String normalize(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }

        return data.startsWith("-----BEGIN")
            ? Base64.getEncoder().encodeToString(data.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            : data;
    }
}
