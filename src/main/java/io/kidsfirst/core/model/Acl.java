package io.kidsfirst.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Iterator;

@Data
@AllArgsConstructor
public class Acl {
    private Iterator<String> acl;

}