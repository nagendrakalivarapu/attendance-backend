package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String employeeId;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String qualification;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}