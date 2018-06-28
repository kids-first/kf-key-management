package io.kidsfirst.fence;

import lombok.Data;
import lombok.Value;

@Data
public class KfTokens {
    String userid_in_ego;
    String userid_in_fence;
    String access_token;
    String refresh_token;
}
