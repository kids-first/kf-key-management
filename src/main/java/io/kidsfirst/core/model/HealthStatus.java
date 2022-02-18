package io.kidsfirst.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class HealthStatus  implements Serializable {

    private String status;
}
