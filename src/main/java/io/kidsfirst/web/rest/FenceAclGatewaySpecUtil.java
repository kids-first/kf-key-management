package io.kidsfirst.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kidsfirst.config.AllFences;
import io.kidsfirst.core.model.Acl;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Objects;

@Slf4j
public class FenceAclGatewaySpecUtil {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    public static final Acl EMPTY_ACL = new Acl(Collections.emptyIterator());

    public static GatewayFilterSpec filterAcl(GatewayFilterSpec f, AllFences.Fence fence) {
        if (fence.getName().equals("gen3") || fence.getName().equals("dcf")) {
            return f
                    .setPath("/user/user")
                    .modifyResponseBody(String.class, String.class, (serverWebExchange, s) -> {
                                if (Objects.requireNonNull(serverWebExchange.getResponse().getStatusCode()).is2xxSuccessful()) {
                                    try {
                                        JsonNode m = objectMapper.readTree(s);
                                        val acl = new Acl(m.path("project_access").fieldNames());
                                        val data = objectMapper.writeValueAsString(acl);
                                        return Mono.just(data);
                                    } catch (JsonProcessingException e) {
                                        throw new IllegalStateException("Impossible to parse json", e);
                                    }
                                } else {
                                    log.error("Error when retrieving ACLs for fence {} with status {}", fence.getName(), serverWebExchange.getResponse().getStatusCode());
                                }
                                return Mono.just(Objects.requireNonNullElse(s, ""));
                            }

                    );
        }
        return f.filters((exchange, chain) -> {
            try {
                val dataBufferFactory = exchange.getResponse().bufferFactory();
                val objMapper = new ObjectMapper();
                val obj = objMapper.writeValueAsBytes(EMPTY_ACL);
                exchange.getResponse().getHeaders().setContentLength(obj.length);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse().writeWith(Mono.just(obj).map(dataBufferFactory::wrap));

            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Impossible to parse json", e);
            }
        });
    }


}
