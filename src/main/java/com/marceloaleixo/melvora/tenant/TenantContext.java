package com.marceloaleixo.melvora.tenant;

import com.marceloaleixo.melvora.exception.TenantContextException;

public final class TenantContext {

    private static final ThreadLocal<Long> EMPRESA_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long empresaId) {
        if (empresaId == null || empresaId <= 0) {
            throw new IllegalArgumentException("Empresa inválida.");
        }
        EMPRESA_ID.set(empresaId);
    }

    public static Long getRequired() {
        Long id = EMPRESA_ID.get();

        if (id == null) {
            throw new TenantContextException(
                "Tenant não definido para a requisição."
            );
        }

        return id;
    }

    public static Long get() {
        return EMPRESA_ID.get();
    }

    public static void clear() {
        EMPRESA_ID.remove();
    }
}
