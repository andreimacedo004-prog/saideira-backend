# Saideira — Backend

App de check-in de rolê entre amigos, estilo GymRats: a galera cria um desafio com começo e fim, cada rolê vira um check-in com foto, e o ranking do grupo se monta sozinho.

Backend em **Java 17 + Spring Boot 3.3 + PostgreSQL**, com a mesma estrutura do EloFit.

## Estrutura de pastas

```
src/main/java/com/saideira/backend/
├── model/        → Entidades JPA (tabelas do banco)
├── repository/   → Acesso a dados (Spring Data JPA)
├── service/      → Regras de negócio — o ScoreService é o placar
├── controller/   → Endpoints REST
├── dto/          → Entrada/saída da API (nunca expõe entidade)
├── security/     → JWT
├── config/       → Segurança/CORS, relógio no fuso de Brasília, Jackson
├── exception/    → Tratamento centralizado de erros
└── util/         → Normalizador de texto

src/main/resources/db/migration/ → Migrations Flyway (V1 esquema, V2 catálogo de cervejas, V3 formato e quantidade)
src/test/java/                   → Testes unitários
testar_saideira.ps1              → Passeio completo pela API, em PowerShell
```

**Regra de dependência:** Controller → Service → Repository.

**Diferença para o EloFit:** aqui os services devolvem DTO, e não entidade. Com `open-in-view=false`, a sessão do Hibernate fecha quando o service termina; montar o DTO no controller e ler uma lista lazy (como os membros de um grupo) estouraria `LazyInitializationException`.

## Entidades

- `User`: perfil e credenciais
- `FriendGroup`: a galera, com código de convite (igual ao EloFit)
- `Challenge`: desafio com data de início e fim dentro de um grupo; cada desafio tem seu ranking
- `CheckIn`: o rolê (tipo, local, foto, legenda, horário, amigos marcados, cervejas)
- `Beer`: catálogo de cervejas compartilhado; já vem com ~27 das mais comuns de bar
- `Reaction` / `Comment`: interações no feed

## Pontuação

Calculada **na hora**, a partir dos check-ins. Não existe coluna de pontos no banco: se alguém apaga um check-in ou uma regra muda, o ranking se ajusta sozinho.

| O quê | Pontos |
|---|---|
| Check-in | +10 |
| Cerveja que a pessoa ainda não tinha registrado no desafio | +5 cada (a quantidade não conta) |
| Amigo marcado | +3 cada |
| Lugar onde a pessoa ainda não tinha feito check-in no desafio | +5 |

- "Ainda não tinha" segue a ordem em que os rolês **aconteceram**, não a ordem de registro.
- Lugar é comparado normalizado: "Bar do Zé" e "bar do ze" são o mesmo lugar.
- A novidade é por pessoa: a cerveja que a amiga já provou continua nova para você.

Pontua rolê, variedade e galera, nunca quantidade de bebida.

### Formato e quantidade ("soma escondida")

Cada cerveja do check-in vai com **formato** (`LATA` 350 ml, `LATAO` 473, `LONG_NECK` 330, `GARRAFA` 600, `LITRAO` 1000, `CHOPP` 300) e **quantidade** (1 a 20). Esses dados:

- **não valem ponto**: 6 latões da mesma cerveja continuam sendo 1 cerveja nova;
- **não aparecem no feed**;
- alimentam só a **retrospectiva** (`GET /api/desafios/{id}/retrospectiva`). Ali cada pessoa vê o próprio volume e o **total coletivo** da galera. Não existe ranking de quem bebeu mais, de propósito.

### Anti-farm

- Intervalo mínimo de **2h** entre check-ins da mesma pessoa no mesmo desafio (`CHECKIN_INTERVALO_MINUTOS`)
- Check-in retroativo de até **24h** (para quem esqueceu na hora), nunca no futuro
- No máximo **5 cervejas** por check-in
- Só dá para marcar quem é do grupo, e ninguém se marca
- Catálogo não aceita cerveja duplicada (ignora maiúsculas, acentos e espaços)

As regras ficam expostas em `GET /api/regras`, para a tela "como pontuar" do app nunca discordar do backend.

## Endpoints

Tudo exige `Authorization: Bearer <token>`, exceto `/api/auth/**`, `/api/health` e `/api/regras`.

| Método | Rota | O que faz |
|---|---|---|
| POST | `/api/auth/cadastro` | Cria conta (exige `maiorDeIdade: true`) e devolve token |
| POST | `/api/auth/login` | Devolve token (vale 30 dias) |
| GET | `/api/regras` | Pontuação, limites do check-in e os formatos com o volume de cada um |
| GET | `/api/usuarios/eu` | Meu perfil |
| PUT | `/api/usuarios/eu/perfil` | Nome, bio e foto |
| POST | `/api/grupos` | Cria grupo |
| GET | `/api/grupos` | Meus grupos |
| GET | `/api/grupos/{id}` | Detalhe (só membros) |
| GET | `/api/grupos/{id}/convite` | Link para compartilhar |
| POST | `/api/grupos/entrar` | Entra pelo código do convite |
| POST | `/api/grupos/{id}/desafios` | Cria desafio (`nome`, `dataInicio`, `dataFim`) |
| GET | `/api/grupos/{id}/desafios` | Desafios do grupo |
| GET | `/api/desafios` | Desafios de todos os meus grupos, ativos primeiro (tela inicial) |
| GET | `/api/desafios/{id}` | Detalhe, com status `EM_BREVE` / `ATIVO` / `ENCERRADO` e `criadoPorId` |
| PATCH | `/api/desafios/{id}` | Muda o nome (`nome`). Só quem criou; as datas não mudam |
| DELETE | `/api/desafios/{id}` | Apaga o desafio. Só quem criou; leva junto check-ins, reações e comentários (cascade no banco) |
| GET | `/api/desafios/{id}/ranking` | Ranking com o detalhe dos pontos |
| GET | `/api/desafios/{id}/retrospectiva` | Meus litros e cervejas + total da galera (para o Wrapped) |
| POST | `/api/desafios/{id}/checkins` | Faz check-in |
| GET | `/api/desafios/{id}/checkins` | Feed, já com pontos, reações e nº de comentários |
| GET | `/api/checkins/{id}` | Um check-in |
| GET | `/api/checkins/{id}/edicao` | Dados para a tela de edição, com formato e quantidade (só o autor) |
| PUT | `/api/checkins/{id}` | Edita tudo menos o horário (só o autor, enquanto o desafio não acabou). Devolve o card com os pontos recalculados |
| DELETE | `/api/checkins/{id}` | Apaga (só o autor) |
| PUT | `/api/checkins/{id}/reacoes/{tipo}` | Reage: `BRINDE`, `FOGO`, `RISADA`, `LENDA` |
| DELETE | `/api/checkins/{id}/reacoes/{tipo}` | Desfaz a reação |
| POST | `/api/checkins/{id}/comentarios` | Comenta |
| GET | `/api/checkins/{id}/comentarios` | Lista comentários |
| DELETE | `/api/comentarios/{id}` | Apaga (quem comentou ou o dono do check-in) |
| GET | `/api/cervejas?busca=` | Autocomplete de cervejas |
| POST | `/api/cervejas` | Cadastra cerveja (201 se nova, 200 se já existia) |

Exemplo de check-in:

```json
POST /api/desafios/1/checkins
{
  "tipo": "BAR",
  "local": "Bar do Zé",
  "fotoUrl": "https://res.cloudinary.com/.../foto.jpg",
  "legenda": "Só mais uma",
  "amigosIds": [2, 3],
  "cervejas": [
    { "cervejaId": 15, "formato": "GARRAFA", "quantidade": 2 }
  ],
  "feitoEm": "2026-11-06T23:30:00"
}
```

`tipo`: `BAR`, `FESTA`, `CHURRASCO`, `SHOW`, `VISITA` ou `OUTRO`. `feitoEm` é opcional (sem ele, vale a hora do servidor).

Padrão de erro: `{ "erro": "mensagem" }` com 400 (regra), 401 (token), 403 (não é do grupo), 404 e 409. Validação de campo devolve `{ "campo": "mensagem" }`.

## Rodando localmente

1. Crie o banco (no mesmo Postgres do EloFit):
   ```sql
   CREATE DATABASE saideira;
   CREATE USER saideira_user WITH PASSWORD 'sua_senha';
   ALTER DATABASE saideira OWNER TO saideira_user;
   ```
   Com o usuário como dono do banco, não precisa do `GRANT ON SCHEMA public` do Postgres 15+.

2. Copie `application-local.properties.example` para `src/main/resources/application-local.properties` e preencha a senha e o segredo do JWT. O arquivo está no `.gitignore` e é carregado automaticamente, sem precisar ativar profile.

3. `mvn spring-boot:run` (ou o Run do IntelliJ). O Flyway cria as tabelas e o catálogo de cervejas no primeiro boot.

4. API em `http://localhost:8280`. É uma porta diferente da do EloFit (8180), então os dois sobem juntos.

5. Em outro terminal: `powershell -ExecutionPolicy Bypass -File .\testar_saideira.ps1`

Testes: `mvn test`. São unitários com Mockito e não precisam de banco.

## Fotos

O backend recebe só a URL (`fotoUrl`). O caminho mais simples para o PWA:

1. Crie uma conta grátis no Cloudinary e um *upload preset* **unsigned**
2. O PWA manda a foto direto para `https://api.cloudinary.com/v1_1/<cloud>/image/upload`
3. A URL que volta vai no check-in

O backend só aceita links `http(s)://`.

## Fuso horário

Todo "agora" sai do `Clock` configurado em `app.fuso-horario` (padrão `America/Sao_Paulo`), e o fuso da JVM é fixado no `main`. Sem isso, no servidor em UTC um desafio que termina em 31/12 fecharia às 21h de Brasília.

## Publicando no Railway

Mesmo esquema do EloFit: um serviço a partir do repositório e um Postgres no mesmo projeto. Variáveis do serviço:

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_USER` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `JWT_SECRET` | um segredo novo (não reaproveite o de dev nem o do EloFit) |
| `APP_CORS_ORIGENS` | endereço do PWA, ex.: `https://saideira.vercel.app` |
| `CONVITE_BASE_URL` | `https://<endereco-do-pwa>/convite/` |

O Railway injeta `PORT` sozinho.

## O que ainda falta

- [ ] PWA (React + TypeScript, igual ao elofit-web)
- [ ] Saideira Wrapped no fim do desafio (rolê mais épico, bar favorito, quem arrastou mais gente)
- [ ] Badges
- [ ] Editar check-in (hoje é apagar e refazer)
- [ ] Sair do grupo / remover membro; editar e apagar desafio
- [ ] Paginar o feed quando um desafio passar de algumas centenas de check-ins (o ranking continua precisando de todos)
- [ ] Limite de tentativas no login
- [ ] Aceitar foto só do domínio do Cloudinary
