# Squares server API v1 a serverove identity

Stav: navrh pro implementaci

Tento dokument urcuje hranici mezi Windows klientem, budoucim Android klientem
a serverem. Stavajici lokalni profily zustavaji plne funkcni bez internetu.
Napojeni lokalniho profilu na serverovy ucet je dobrovolne.

Strojove citelny kontrakt je v
[`docs/openapi/squares-api-v1.yaml`](openapi/squares-api-v1.yaml).

## 1. Hlavni rozhodnuti

1. API pouziva HTTPS, JSON a zakladni cestu `/api/v1`.
2. Squares server neuklada hesla. Prihlaseni zajistuje OpenID Connect provider.
3. Windows i Android pouziji Authorization Code flow s PKCE v externim
   systemovem prohlizeci. Windows pouzije loopback redirect, Android overeny
   app-link redirect.
4. Trvalou prihlasovaci identitou je dvojice OIDC `issuer` + `subject` (`iss` +
   `sub`). E-mail, zobrazovane jmeno ani handle nejsou identita.
5. Jeden ucet ma ve verzi v1 prave jednu verejnou hracskou identitu.
6. Lokalni `PlayerProfile.id` se nikdy nepovazuje za globalni identitu.
7. Klientem odeslane vysledky mohou tvorit pouze nezavazny casual zebricek.
   Hodnoceny ranked zebricek prijima jen hry, jejichz tahy a vysledek overil
   server.
8. Odeslani vysledku je idempotentni pres
   `PUT /me/game-submissions/{gameId}`. Opakovani stejneho pozadavku je bezpecne;
   jiny obsah pro stejneho hrace a `gameId` vrati `409 Conflict`.
9. Chyby pouzivaji `application/problem+json` podle RFC 9457.
10. Casove udaje jsou UTC timestampy ve formatu RFC 3339.

Relevantni standardy:

- OAuth pro nativni aplikace: https://www.rfc-editor.org/rfc/rfc8252.html
- Aktualni OAuth security BCP: https://www.rfc-editor.org/rfc/rfc9700.html
- OpenID Connect Core: https://openid.net/specs/openid-connect-core-1_0.html
- HTTP Problem Details: https://www.rfc-editor.org/rfc/rfc9457.html
- HTTP idempotence: https://www.rfc-editor.org/rfc/rfc9110.html#name-idempotent-methods
- OpenAPI 3.1.1: https://spec.openapis.org/oas/v3.1.1.html

## 2. Identity

### 2.1 Urovne identity

| Identita | Vlastnik | Viditelnost | Ucel |
| --- | --- | --- | --- |
| `account_id` | server | soukroma | ucet, blokace, audit, vlastnictvi dat |
| OIDC `issuer` + `subject` | identity provider | soukroma | stabilni vazba prihlaseni na ucet |
| `player_id` | server | verejna | hrac ve hre, statistiky a zebricek |
| `local_profile_id` | klient | pouze zarizeni | stavajici offline profil v SQLite |
| `installation_id` | klient | ucet a server | rozliseni instalace a diagnostika; nikdy autentizace |

`account_id` a `player_id` jsou serverem generovana UUID. Jejich oddeleni brani
tomu, aby verejne herni API odhalovalo interni identitu uctu, a umoznuje budouci
rozsireni na vice hernich profilu pod jednim uctem.

### 2.2 Verejny profil

Verejny hrac ma:

- `playerId`: nemenne UUID,
- `handle`: globalne unikatni ASCII jmeno o delce 3 az 24 znaku,
- `displayName`: zobrazovane Unicode jmeno o delce 1 az 40 znaku,
- `createdAt` a `revision`.

Handle se normalizuje na mala pismena pomoci locale-independent pravidel a
server rezervuje nevhodna nebo systemova jmena. `displayName` neni identita a
nemusi byt unikatni.

### 2.3 Vznik a prihlaseni uctu

1. Klient otevre systemovy prohlizec a provede OIDC Authorization Code + PKCE.
2. Klient posle access token jako `Authorization: Bearer ...`.
3. Server overi podpis, `iss`, `aud`, `exp` a typ tokenu.
4. Server vyhleda `account_id` podle unikatni dvojice (`issuer`, `subject`).
5. Pri prvnim platnem tokenu server vytvori ucet bez verejneho profilu.
6. `GET /me` vrati `onboardingRequired: true`.
7. Klient vytvori profil pres `PUT /me/profile` a zaregistruje instalaci.

Pristupove a obnovovaci tokeny se neukladaji do databaze `squares.db` v
otevrenem tvaru. Android pouzije zabezpecene uloziste vazane na Keystore;
Windows uloziste vazane na uzivatelsky ucet systemu.

### 2.4 Vazba na lokalni profil

Lokalni databaze pozdeji dostane samostatnou tabulku vazeb, nikoli zmenu
vyznamu stavajiciho `profiles.id`:

```text
profile_server_links
  local_profile_id  -> profiles.id
  server_player_id
  oidc_issuer
  linked_at
  last_synced_at
```

Jeden lokalni profil muze byt propojen nejvyse s jednim serverovym hracem.
Jeden serverovy hrac muze byt prihlasen na vice instalacich. Na sdilenem Windows
pocitaci ma kazdy propojeny lokalni profil vlastni prihlasovaci relaci.

## 3. Instalace

Klient pri prvnim startu vygeneruje nahodne `installation_id` a po prihlaseni ho
zaregistruje pomoci `PUT /me/installations/{installationId}`. Posila pouze:

- platformu `WINDOWS` nebo `ANDROID`,
- verzi aplikace,
- verzi `squares-core`,
- locale a cas posledniho kontaktu.

ID instalace je mozne zkopirovat nebo podvrhnout, proto nesmi rozhodovat o
opravneni. Autorizaci vzdy urcuje access token.

## 4. API v1

### Verejne operace

- `GET /meta` - verze API, aktualni verze pravidel a minimalni verze klienta.
- `GET /players/{playerId}` - verejny profil.
- `GET /players/{playerId}/statistics` - casual a ranked statistiky.
- `GET /leaderboards/{board}` - `casual` nebo `ranked`, kurzorove strankovani.

### Autentizovane operace

- `GET /me` - stav uctu a vlastni hracsky profil.
- `PUT /me/profile` - idempotentni vytvoreni nebo uplna aktualizace profilu.
- `PUT /me/installations/{installationId}` - registrace/obnova instalace.
- `PUT /me/game-submissions/{gameId}` - idempotentni odeslani vysledku.
- `GET /me/game-submissions/{gameId}` - stav synchronizace vysledku.

API neprijima `account_id` od klienta. Vlastnika vzdy odvozuje z access tokenu.

## 5. Odeslani hry a duveryhodnost

### 5.1 Kategorie

| Typ hry | Overeni | Casual | Ranked |
| --- | --- | --- | --- |
| lokalni dva hraci | `UNVERIFIED` | ano | ne |
| hrac proti CPU | `UNVERIFIED` | ano, samostatna kategorie | ne |
| soucasna LAN hra, jedna strana | `UNVERIFIED` | cekajici | ne |
| soucasna LAN hra, shodne obe strany | `PEER_CONFIRMED` | ano | ne |
| budouci serverova online hra | `SERVER_VERIFIED` | ano | ano |

`PEER_CONFIRMED` znamena jen to, ze oba klienti poslali stejny vysledek. Neni to
dukaz, ze klienti nebyli upraveni.

### 5.2 Pravidla prijeti

- URL `gameId` musi byt UUID a je globalnim ID hry z `GameResult.gameId`.
- Server znovu overi vsechny invarianty, ktere dnes overuje `GameResult`.
- Sedadla musi byt prave `RED` a `BLUE`.
- Skore, outcomes, duvod konce a CPU konfigurace musi byt konzistentni.
- `submittedBySeat` musi oznacovat prihlaseneho hrace. Server nepripise hru
  jinemu `playerId` pouze podle tela pozadavku.
- U LAN hry server ceka na podani druheho prihlaseneho hrace. Shodu urcuje
  kanonicky hash hernich dat bez serverovych metadat.
- Dve rozdilna podani stejne LAN hry dostanou stav `CONFLICTED` a nezapoctou se.
- `LOCAL`, `COMPUTER` a soucasny `NETWORK` vstup nikdy nevytvori ranked vysledek.
- Serverovy cas prijeti je autoritativni pro audit; klientsky `finishedAt` se
  pouziva jen jako udaj o hre.

### 5.3 Idempotence a outbox

Soucasny lokalni outbox muze pouzit `aggregate_id` jako `{gameId}` v URL. Stavovy
automat klienta:

```text
PENDING -> SENDING -> SENT
              |         ^
              +-> RETRY-+
              +-> DEAD (trvala 4xx chyba)
```

Stejny `PUT` se stejnym obsahem vrati existujici vysledek. Jiny obsah pro stejne
`gameId` a prihlaseneho hrace vrati `409` s problem code
`submission-payload-conflict`.

## 6. Navrh serverove databaze

Backend ma pouzit PostgreSQL. Minimalni tabulky:

### `accounts`

- `account_id uuid primary key`
- `status` (`ACTIVE`, `BLOCKED`, `DELETED`)
- `created_at`, `updated_at`

### `account_identities`

- `issuer text`
- `subject text`
- `account_id uuid references accounts`
- primary key (`issuer`, `subject`)

Jeden ucet muze byt v budoucnu propojen s vice identity providery, ale propojeni
vyzaduje znovuovereni obou identit.

### `players`

- `player_id uuid primary key`
- `account_id uuid unique references accounts`
- `handle`, `normalized_handle unique`
- `display_name`
- `revision`, `created_at`, `updated_at`

### `installations`

- `account_id uuid references accounts`
- `installation_id uuid`
- `platform`, `app_version`, `core_version`, `locale`
- `created_at`, `last_seen_at`, `revoked_at`
- primary key (`account_id`, `installation_id`)

### `game_submissions`

- `submission_id uuid primary key`
- `game_id uuid`
- `submitted_by_player_id uuid references players`
- `installation_id uuid`
- `payload jsonb`, `payload_hash bytea`
- `status`, `received_at`, `updated_at`
- unique (`game_id`, `submitted_by_player_id`)

### `games` a `game_players`

Normalizovana autoritativni projekce prijatych podani. Uchovava take snapshot
jmen, aby pozdejsi prejmenovani nezmenilo historii. `games` obsahuje
`verification_status` a `ranked`; tyto hodnoty vypocita pouze server.

### Zebricek

Zebricek je odvozena projekce, ne zdroj pravdy. Verze v1 zachova metriku
celkoveho skore pro casual board. Ranked rating a sezony se doplni az se
serverove rizenou online hrou.

## 7. Chyby a HTTP semantika

- `400` - syntakticky nebo obecne neplatny pozadavek.
- `401` - chybejici/neplatny access token.
- `403` - platny ucet nema opravneni nebo je blokovan.
- `404` - zdroj neexistuje nebo k nemu uzivatel nema pristup.
- `409` - obsazeny handle nebo konflikt idempotentniho podani.
- `422` - JSON odpovida schematu, ale porusuje herni invariant.
- `429` - omezeni frekvence; odpoved obsahuje `Retry-After`.

Problem Details obsahuje stabilni strojovy `code`; klient nesmi rozhodovat podle
lokalizovaneho `detail`.

## 8. Bezpecnostni pravidla

- Pouze TLS; zadne tokeny v URL nebo logu.
- OIDC discovery a povoleny issuer/audience jsou explicitni serverova
  konfigurace.
- JWT algoritmus se bere z povolene konfigurace, ne slepe z hlavicky tokenu.
- `email`, `preferred_username` a `displayName` se nepouzivaji jako klic uctu.
- Refresh tokeny rotovat, pokud to provider podporuje.
- Rate limit oddelene pro IP, ucet a zapis vysledku.
- Verejne odpovedi neobsahuji e-mail, issuer, subject, installation ID ani
  account ID.
- Audit uchovava duvod zmeny verification/ranked stavu.
- Smazany ucet se ve starych hrach anonymizuje bez poruseni integrity vysledku.

## 9. Implementacni poradi

1. Zalozit modul `squares-server` (Java 21, Spring Boot, PostgreSQL).
2. Implementovat OIDC validaci, `accounts`, `account_identities`, `/me` a profil.
3. Implementovat registraci instalace.
4. Implementovat idempotentni game submissions a serverovou validaci
   `GameResult`.
5. Pridat migraci lokalni SQLite pro vazbu profilu a obsluhu outboxu.
6. Napojit Windows klienta a otestovat retry/offline scenare.
7. Implementovat casual zebricek.
8. Teprve nad stabilnim API vytvorit Android klienta.

Mimo rozsah v1 jsou hesla spravovana Squares serverem, WebSocket matchmaking,
serverove rizena online partie, ranked rating, sezony, moderacni UI a automaticke
propojovani uctu podle e-mailu.
