package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.exception.RegraNegocioException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecretCryptoService {
    private static final int IV_SIZE = 12;
    private static final int TAG_BITS = 128;
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    public SecretCryptoService(@Value("${melvora.security.secret-key:}") String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            this.key = null;
        } else {
            try {
                byte[] decoded = Base64.getDecoder().decode(configuredKey);
                if (decoded.length != 32) throw new IllegalArgumentException("A chave precisa ter 32 bytes em Base64.");
                this.key = decoded;
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("melvora.security.secret-key deve ser uma chave AES-256 em Base64.", ex);
            }
        }
    }

    public String encrypt(String plainText) {
        if (key == null) throw new RegraNegocioException("A chave de segurança do Melvora não está configurada.");
        if (plainText == null || plainText.isBlank()) throw new RegraNegocioException("Token de acesso é obrigatório.");
        try {
            byte[] iv = new byte[IV_SIZE];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível proteger o token do WhatsApp.", ex);
        }
    }

    public String decrypt(String cipherText) {
        if (key == null) throw new RegraNegocioException("A chave de segurança do Melvora não está configurada.");
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[IV_SIZE];
            byte[] encrypted = new byte[payload.length - IV_SIZE];
            System.arraycopy(payload, 0, iv, 0, IV_SIZE);
            System.arraycopy(payload, IV_SIZE, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RegraNegocioException("Não foi possível descriptografar o token do WhatsApp.");
        }
    }
}
