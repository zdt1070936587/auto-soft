package com.autosoft.workflow.graph;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

/**
 * HTTP 节点 SSRF 防护：必须在白名单，且拒绝环回/私网/元数据地址。
 */
public final class HttpHostGuard {

    private HttpHostGuard() {
    }

    public static void assertAllowed(String url, List<String> allowedHosts) {
        AssertUtils.notBlank(url, "http 需要 url");
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ex) {
            throw new BizException(ResultCode.BAD_REQUEST, "http url 不合法");
        }
        String scheme = uri.getScheme();
        AssertUtils.isTrue("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme), "仅允许 http/https");
        String host = uri.getHost();
        AssertUtils.notBlank(host, "url 缺少 host");
        String lower = host.toLowerCase(Locale.ROOT);
        AssertUtils.isTrue(!"localhost".equals(lower) && !lower.endsWith(".localhost"), "禁止访问 localhost");
        List<String> allowed = allowedHosts == null ? List.of() : allowedHosts;
        boolean listed = allowed.stream().anyMatch(item -> item != null && host.equalsIgnoreCase(item.trim()));
        AssertUtils.isTrue(listed, "HTTP host 不在白名单: " + host);
        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) {
                assertPublic(addr);
            }
        } catch (UnknownHostException ex) {
            throw new BizException(ResultCode.BAD_REQUEST, "无法解析 host: " + host);
        }
    }

    private static void assertPublic(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            throw new BizException(ResultCode.BAD_REQUEST, "禁止访问内网或本机地址");
        }
        String ip = addr.getHostAddress();
        if ("169.254.169.254".equals(ip) || ip.startsWith("169.254.") || ip.startsWith("fd") || ip.startsWith("fc")) {
            throw new BizException(ResultCode.BAD_REQUEST, "禁止访问元数据或私有地址");
        }
    }
}
