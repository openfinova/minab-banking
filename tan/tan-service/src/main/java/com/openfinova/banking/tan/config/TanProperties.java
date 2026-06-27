package com.openfinova.banking.tan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tan")
public class TanProperties {

    private String baseUrl = "https://tan.openfinova.com";
    private Device device = new Device();
    private Attestation attestation = new Attestation();
    private Authorization authorization = new Authorization();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Attestation getAttestation() {
        return attestation;
    }

    public void setAttestation(Attestation attestation) {
        this.attestation = attestation;
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    public static class Device {
        private int maxPerUser = 3;

        public int getMaxPerUser() {
            return maxPerUser;
        }

        public void setMaxPerUser(int maxPerUser) {
            this.maxPerUser = maxPerUser;
        }
    }

    public static class Attestation {
        private boolean enforce = false;

        public boolean isEnforce() {
            return enforce;
        }

        public void setEnforce(boolean enforce) {
            this.enforce = enforce;
        }
    }

    public static class Authorization {
        private int pendingTtlMinutes = 15;

        public int getPendingTtlMinutes() {
            return pendingTtlMinutes;
        }

        public void setPendingTtlMinutes(int pendingTtlMinutes) {
            this.pendingTtlMinutes = pendingTtlMinutes;
        }
    }
}
