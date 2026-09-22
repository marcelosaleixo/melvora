package com.marceloaleixo.melvora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "melvora.bootstrap")
public class BootstrapProperties {

    private boolean enabled = true;
    private String masterEmail = "master@melvora.local";
    private String masterPassword = "";
    private String masterName = "Administrador Master";
    private String companyName = "Salão Demonstração";
    private String adminEmail = "admin@salaodemo.local";
    private String adminPassword = "";
    private String adminName = "Administrador";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getMasterEmail() { return masterEmail; }
    public void setMasterEmail(String masterEmail) { this.masterEmail = masterEmail; }

    public String getMasterPassword() { return masterPassword; }
    public void setMasterPassword(String masterPassword) { this.masterPassword = masterPassword; }

    public String getMasterName() { return masterName; }
    public void setMasterName(String masterName) { this.masterName = masterName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
}
