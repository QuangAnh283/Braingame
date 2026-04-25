package org.example.quizizz.common.security;

import com.corundumstudio.socketio.HandshakeData;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClientIpResolver {

    private final Set<String> trustedProxies;

    public ClientIpResolver(@Value("${app.security.trusted-proxies:127.0.0.1,::1,0:0:0:0:0:0:0:1}") String trustedProxies) {
        this.trustedProxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddress)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            String forwardedAddress = firstForwardedAddress(forwardedFor);
            if (forwardedAddress != null) {
                return forwardedAddress;
            }
        }
        return remoteAddress != null ? remoteAddress : "unknown";
    }

    public String resolve(HandshakeData data) {
        String remoteAddress = remoteAddress(data);
        if (isTrustedProxy(remoteAddress) && data.getHttpHeaders() != null) {
            String forwardedFor = data.getHttpHeaders().get("X-Forwarded-For");
            String forwardedAddress = firstForwardedAddress(forwardedFor);
            if (forwardedAddress != null) {
                return forwardedAddress;
            }
        }
        return remoteAddress;
    }

    private boolean isTrustedProxy(String remoteAddress) {
        return remoteAddress != null && trustedProxies.contains(remoteAddress);
    }

    private String firstForwardedAddress(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        return Arrays.stream(forwardedFor.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String remoteAddress(HandshakeData data) {
        InetSocketAddress address = data.getAddress();
        if (address == null) {
            return "unknown";
        }
        if (address.getAddress() != null) {
            return address.getAddress().getHostAddress();
        }
        return address.getHostString();
    }
}
