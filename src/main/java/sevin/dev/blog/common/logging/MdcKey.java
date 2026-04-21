package sevin.dev.blog.common.logging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MdcKey {

    REQUEST_ID("requestId"),
    METHOD("method"),
    PATH("path"),
    USERNAME("username");

    private final String key;
}
