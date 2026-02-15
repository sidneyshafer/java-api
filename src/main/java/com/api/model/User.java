package com.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * User domain model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String status;
    private String role;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
