package com.marceloaleixo.melvora.config;

import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.entity.Usuario;
import com.marceloaleixo.melvora.entity.enums.Role;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@Configuration
@EnableConfigurationProperties(BootstrapProperties.class)
public class BootstrapAdmin {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Bean
    CommandLineRunner bootstrapAdminRunner(
            EmpresaRepository empresas,
            UsuarioRepository usuarios,
            PasswordEncoder encoder,
            PlatformTransactionManager transactionManager,
            BootstrapProperties properties) {

        return args -> {
            if (!properties.isEnabled()) {
                log.info("Bootstrap inicial do Melvora desabilitado.");
                return;
            }

            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                String masterEmail = normalize(properties.getMasterEmail()).toLowerCase(Locale.ROOT);
                String adminEmail = normalize(properties.getAdminEmail()).toLowerCase(Locale.ROOT);
                String companyName = normalize(properties.getCompanyName());

                validateConfiguration(masterEmail, adminEmail, companyName);

                Empresa empresa = empresas.findAll().stream().findFirst().orElseGet(() -> {
                    Empresa novaEmpresa = new Empresa(companyName);
                    Empresa salva = empresas.save(novaEmpresa);
                    log.info("Empresa inicial criada: id={}, nome={}", salva.getId(), salva.getNomeFantasia());
                    return salva;
                });

                String generatedMasterPassword = null;
                String masterPassword = normalize(properties.getMasterPassword());
                if (masterPassword.isBlank()) {
                    generatedMasterPassword = generatePassword();
                    masterPassword = generatedMasterPassword;
                }

                String generatedAdminPassword = null;
                String adminPassword = normalize(properties.getAdminPassword());
                if (adminPassword.isBlank()) {
                    generatedAdminPassword = generatePassword();
                    adminPassword = generatedAdminPassword;
                }

                boolean masterCreated = false;
                if (!usuarios.existsByEmailIgnoreCase(masterEmail)) {
                    usuarios.save(new Usuario(
                            properties.getMasterName().trim(),
                            masterEmail,
                            encoder.encode(masterPassword),
                            Role.SUPER_ADMIN,
                            null
                    ));
                    masterCreated = true;
                }

                boolean adminCreated = false;
                var adminExistente = usuarios.findByEmailIgnoreCase(adminEmail);
                if (adminExistente.isEmpty()) {
                    usuarios.save(new Usuario(
                            properties.getAdminName().trim(),
                            adminEmail,
                            encoder.encode(adminPassword),
                            Role.ADMIN,
                            empresa
                    ));
                    adminCreated = true;
                } else if (adminExistente.get().getRole() == Role.ADMIN && !adminExistente.get().isAtivo()) {
                    adminExistente.get().setAtivo(true);
                    usuarios.save(adminExistente.get());
                    log.info("ADMIN inicial reativado: {}", adminEmail);
                }

                if (masterCreated || adminCreated) {
                    log.warn("============================================================");
                    log.warn("MELVORA - CREDENCIAIS INICIAIS");
                    log.warn("============================================================");
                    if (masterCreated) {
                        log.warn("SUPER_ADMIN: {}", masterEmail);
                        log.warn("Senha SUPER_ADMIN: {}", generatedMasterPassword != null
                                ? generatedMasterPassword
                                : "definida por MELVORA_BOOTSTRAP_MASTER_PASSWORD");
                    } else {
                        log.warn("SUPER_ADMIN já existente: {}", masterEmail);
                    }
                    if (adminCreated) {
                        log.warn("ADMIN: {}", adminEmail);
                        log.warn("Senha ADMIN: {}", generatedAdminPassword != null
                                ? generatedAdminPassword
                                : "definida por MELVORA_BOOTSTRAP_ADMIN_PASSWORD");
                    } else {
                        log.warn("ADMIN já existente: {}", adminEmail);
                    }
                    log.warn("Altere as credenciais após o primeiro acesso.");
                    log.warn("============================================================");
                } else {
                    log.info("Bootstrap já realizado. Nenhum usuário novo foi criado.");
                }
            });
        };
    }

    private static void validateConfiguration(String masterEmail, String adminEmail, String companyName) {
        if (masterEmail.isBlank() || !masterEmail.contains("@")) {
            throw new IllegalStateException("melvora.bootstrap.master-email inválido.");
        }
        if (adminEmail.isBlank() || !adminEmail.contains("@")) {
            throw new IllegalStateException("melvora.bootstrap.admin-email inválido.");
        }
        if (masterEmail.equalsIgnoreCase(adminEmail)) {
            throw new IllegalStateException("SUPER_ADMIN e ADMIN devem possuir e-mails diferentes.");
        }
        if (companyName.isBlank()) {
            throw new IllegalStateException("melvora.bootstrap.company-name não pode ser vazio.");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String generatePassword() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "A9!";
    }
}
