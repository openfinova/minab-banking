package com.openfinova.banking.identity.entity;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Objects;

import com.openfinova.banking.identity.api.permission.BankingPermission;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Size;

/**
 * Optional constraints on a permission granted through a {@link BankingRole}. When all constraint
 * fields are null, the grant behaves like an unconditional permission for the given
 * {@link BankingPermission}.
 *
 * Time windows use the JVM default timezone and apply to the current local time each day (e.g.
 * business hours). If {@code timeWindowStart} is after {@code timeWindowEnd}, the window is treated
 * as crossing midnight.
 */
@Embeddable
public class ScopedPermissionGrant implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 80)
    private BankingPermission permission;

    @Column(name = "time_window_start")
    private LocalTime timeWindowStart;

    @Column(name = "time_window_end")
    private LocalTime timeWindowEnd;

    @Size(max = 20)
    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Size(max = 40)
    @Column(name = "department_code", length = 40)
    private String departmentCode;

    protected ScopedPermissionGrant() {
    }

    public BankingPermission getPermission() {
        return permission;
    }

    public void setPermission(BankingPermission permission) {
        this.permission = permission;
    }

    public LocalTime getTimeWindowStart() {
        return timeWindowStart;
    }

    public void setTimeWindowStart(LocalTime timeWindowStart) {
        this.timeWindowStart = timeWindowStart;
    }

    public LocalTime getTimeWindowEnd() {
        return timeWindowEnd;
    }

    public void setTimeWindowEnd(LocalTime timeWindowEnd) {
        this.timeWindowEnd = timeWindowEnd;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScopedPermissionGrant that = (ScopedPermissionGrant) o;
        return permission == that.permission && Objects.equals(timeWindowStart, that.timeWindowStart)
                && Objects.equals(timeWindowEnd, that.timeWindowEnd) && Objects.equals(branchCode, that.branchCode)
                && Objects.equals(departmentCode, that.departmentCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permission, timeWindowStart, timeWindowEnd, branchCode, departmentCode);
    }
}
