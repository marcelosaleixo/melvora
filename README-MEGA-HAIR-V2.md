# Melvora — Mega Hair v2

Evolução da base multitenant para rastreabilidade de Mega Hair.

## Fluxo

Produto Mega Hair → Lote → Estoque → Aplicação na cliente → Manutenção → Histórico.

## API

### Clientes
- `POST /api/clientes`
- `GET /api/clientes?page=0&size=20`

### Lotes
- `POST /api/estoque/lotes`
- `GET /api/estoque/lotes?page=0&size=20`

### Aplicação
`POST /api/mega-hair/aplicacoes`

Exemplo:
```json
{
  "clienteId": 10,
  "profissionalId": 4,
  "dataAplicacao": "2026-09-22",
  "observacoes": "Aplicação inicial",
  "lotes": [
    { "loteId": 3, "quantidade": 1 }
  ]
}
```

### Manutenção
`POST /api/mega-hair/aplicacoes/{id}/manutencoes?profissionalId=4`

### Histórico
`GET /api/mega-hair/clientes/{clienteId}/historico`

## Segurança

- O tenant é obtido da sessão autenticada; `empresaId` não é aceito como autoridade do frontend.
- Todas as operações de domínio usam `empresa_id`.
- O consumo do lote usa `PESSIMISTIC_WRITE` para evitar dupla baixa concorrente.
- O saldo do lote é protegido por `CHECK CONSTRAINT`.
- DTOs usam Bean Validation.
- Erros de validação e regras de negócio são tratados por `@RestControllerAdvice`.
- Não há exposição de stack trace ou SQL para o cliente.

## Banco

A migration `V2__mega_hair_rastreabilidade.sql` cria as tabelas de clientes, lotes, movimentações, aplicações e manutenções.

## Observação de build

O ambiente desta sessão não possui Maven instalado e o Maven Wrapper não conseguiu baixar o Maven devido à indisponibilidade de acesso ao repositório externo. Portanto, o projeto deve ser validado localmente com `./mvnw test` ou `mvn test` após restaurar acesso ao Maven Central.
