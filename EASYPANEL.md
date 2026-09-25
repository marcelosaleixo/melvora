# Melvora — Deploy no EasyPanel

## 1. Aplicação

- Tipo: Dockerfile
- Porta: `8080`
- Health check sugerido: `GET /login`
- Domínio: apontar para a aplicação e habilitar HTTPS.

## 2. Variáveis obrigatórias

```env
SERVER_PORT=8080
DB_URL=jdbc:postgresql://SEU_POSTGRES:5432/melvora?sslmode=disable
DB_USERNAME=postgres
DB_PASSWORD=SENHA_FORTE
COOKIE_SECURE=true
THYMELEAF_CACHE=true
MELVORA_SECRET_KEY=CHAVE_BASE64_32_BYTES
MELVORA_WHATSAPP_WEBHOOK_VERIFY_TOKEN=TOKEN_FORTE
MELVORA_PUBLIC_BASE_URL=https://SEU-DOMINIO
```

Para PostgreSQL gerenciado com SSL, use `sslmode=require` na `DB_URL`.

## 3. Bootstrap inicial

Defina uma vez:

```env
MELVORA_BOOTSTRAP_ENABLED=true
MELVORA_BOOTSTRAP_MASTER_EMAIL=master@seudominio.com
MELVORA_BOOTSTRAP_MASTER_PASSWORD=SENHA_FORTE
MELVORA_BOOTSTRAP_MASTER_NAME=Administrador Master
MELVORA_BOOTSTRAP_COMPANY_NAME=Sua Empresa
MELVORA_BOOTSTRAP_ADMIN_EMAIL=admin@seudominio.com
MELVORA_BOOTSTRAP_ADMIN_PASSWORD=SENHA_FORTE
MELVORA_BOOTSTRAP_ADMIN_NAME=Administrador
```

Depois do primeiro bootstrap e validação dos acessos, recomenda-se definir:

```env
MELVORA_BOOTSTRAP_ENABLED=false
```

## 4. PostgreSQL

Não use `ddl-auto=update`. O projeto utiliza Flyway e `ddl-auto=validate`.

No primeiro start, as migrations `V1...V40` devem ser executadas automaticamente. A V40 é uma migration de reparo idempotente para bancos legados em que a V30 aparece como aplicada no histórico, mas a tabela `agenda_horarios_funcionamento` não existe fisicamente.

## 5. Build

O Dockerfile executa:

```bash
mvn -B clean package -DskipTests
```

O container de runtime utiliza somente o JAR gerado e Java 21.

## 6. Ordem recomendada de teste

1. PostgreSQL
2. Melvora
3. Flyway
4. Login
5. SUPER_ADMIN
6. Empresa
7. ADMIN
8. Licenciamento
9. Clientes
10. Serviços
11. Agenda
12. Atendimento
13. Financeiro
14. Retenção
15. Campanhas
16. WhatsApp/WuzAPI/n8n
