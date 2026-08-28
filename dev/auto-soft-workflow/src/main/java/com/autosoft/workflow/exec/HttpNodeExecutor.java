package com.autosoft.workflow.exec;

import com.autosoft.common.utils.AssertUtils;
import com.autosoft.workflow.config.WorkflowProperties;
import com.autosoft.workflow.graph.HttpHostGuard;
import com.autosoft.workflow.graph.NodeTypes;
import com.autosoft.workflow.graph.WfNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HttpNodeExecutor implements NodeExecutor {

    private final RestClient restClient;
    private final WorkflowProperties properties;

    public HttpNodeExecutor(@Qualifier("workflowRestClient") RestClient workflowRestClient, WorkflowProperties properties) {
        this.restClient = workflowRestClient;
        this.properties = properties;
    }

    @Override
    public String type() {
        return NodeTypes.HTTP;
    }

    @Override
    public Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context) {
        String url = str(renderedConfig.get("url"));
        AssertUtils.notBlank(url, "http 缺少 url");
        HttpHostGuard.assertAllowed(url, properties.getHttp().getAllowedHosts());
        String method = str(renderedConfig.get("method"));
        if (method == null || method.isBlank()) {
            method = "GET";
        }
        AssertUtils.isTrue("GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method), "http 仅允许 GET/POST");
        Map<String, String> headers = new LinkedHashMap<>();
        Object headerRaw = renderedConfig.get("headers");
        if (headerRaw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> headers.put(String.valueOf(k), v == null ? "" : String.valueOf(v)));
        }
        String body = str(renderedConfig.get("body"));
        String response;
        try {
            var spec = restClient.method(org.springframework.http.HttpMethod.valueOf(method.toUpperCase()))
                    .uri(url);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                spec = spec.header(header.getKey(), header.getValue());
            }
            if ("POST".equalsIgnoreCase(method) && body != null) {
                response = spec.contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);
            } else {
                response = spec.retrieve().body(String.class);
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("HTTP 调用失败: " + ex.getMessage());
        }
        if (response == null) {
            response = "";
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 4000) {
            response = new String(bytes, 0, 4000, StandardCharsets.UTF_8) + "...(truncated)";
        }
        return Map.of("status", 200, "body", response);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
