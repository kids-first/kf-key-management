package io.kidsfirst.fence.model;

import lombok.Data;

@Data
public class KfTokens {
    public String userid_in_ego;
    public  String userid_in_fence;
    public String access_token;
    public String refresh_token;
}
