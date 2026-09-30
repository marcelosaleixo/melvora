# Melvora — Deploy no EasyPanel

## Configuração do serviço
- Build: **Dockerfile** (não Nixpacks)
- Porta interna: **8086** (deve corresponder a `SERVER_PORT=8086`)
- Health check: `GET /login` (se a rota de login estiver habilitada)
- Configure domínio e HTTPS no EasyPanel.

## PostgreSQL
Use o hostname interno real do serviço PostgreSQL dentro da mesma rede do EasyPanel. Não use nomes de exemplo como `seu-db-service`.

Exemplo de variáveis (substitua os valores pelos dados reais):

```env
SERVER_PORT=8086
DB_URL=jdbc:postgresql://NOME_REAL_DO_SERVICO_POSTGRES:5432/melvora
DB_USERNAME=melvora_user
DB_PASSWORD=COLOQUE_UMA_SENHA_FORTE
DB_POOL_SIZE=10
DB_POOL_MIN_IDLE=2
JPA_DDL_AUTO=update
FLYWAY_ENABLED=false
JPA_SHOW_SQL=false
THYMELEAF_CACHE=true
COOKIE_SECURE=true
MELVORA_BOOTSTRAP_ENABLED=true
MELVORA_BOOTSTRAP_MASTER_EMAIL=master@seudominio.com
MELVORA_BOOTSTRAP_MASTER_PASSWORD=DEFINA_UMA_SENHA_FORTE
MELVORA_BOOTSTRAP_MASTER_NAME=Administrador Master
MELVORA_BOOTSTRAP_COMPANY_NAME=Sua Empresa
MELVORA_BOOTSTRAP_ADMIN_EMAIL=admin@seudominio.com
MELVORA_BOOTSTRAP_ADMIN_PASSWORD=DEFINA_OUTRA_SENHA_FORTE
MELVORA_BOOTSTRAP_ADMIN_NAME=Administrador
MELVORA_SECRET_KEY=CHAVE_BASE64_ALEATORIA_DE_32_BYTES
MELVORA_WHATSAPP_WEBHOOK_VERIFY_TOKEN=TOKEN_FORTE
MELVORA_PUBLIC_BASE_URL=https://SEU_DOMINIO
```

Se o PostgreSQL estiver em outro host, utilize o hostname/IP alcançável pelo container e a porta liberada. Para conexão TLS, acrescente `?sslmode=require` à `DB_URL` quando suportado pelo servidor.

## Persistência
`spring.jpa.hibernate.ddl-auto=update` permite ao Hibernate criar tabelas ausentes e atualizar o schema sem remover tabelas/colunas existentes. Faça backup do banco antes de atualizar. `update` não substitui migrações versionadas para alterações complexas ou destrutivas.

O Flyway fica desativado nesta configuração para evitar que migrations históricas incompletas interrompam o primeiro start. Não ative `FLYWAY_ENABLED` sem preparar e validar baseline/migrations para a base atual.

## Bootstrap
No primeiro start, o Melvora cria a empresa inicial e usuários Master/Admin caso ainda não existam, usando os e-mails e senhas acima. Confira os logs após inicializar. Depois de confirmar o acesso, defina `MELVORA_BOOTSTRAP_ENABLED=false`.

## Diagnóstico do erro `UnknownHostException: seu-db-service`
Esse erro é DNS: o container não consegue resolver o hostname configurado em `DB_URL`. Copie o **Host Interno** exibido nos detalhes do serviço PostgreSQL no EasyPanel (não use o nome ilustrativo `seu-db-service`) e confirme que ambos os serviços estão na rede apropriada. O erro não é causado pela injeção de repositories nem pelo Hibernate.
