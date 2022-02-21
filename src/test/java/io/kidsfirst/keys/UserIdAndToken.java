package io.kidsfirst.keys;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserIdAndToken {
    private String userId;
    private String accessToken;
}
