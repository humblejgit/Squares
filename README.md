# Squares

Java okenni hra pro dva hrace inspirovana hrou tecky a ctverce.

Aktualni verze: **4.3.0**.

## Funkce verze 4.3.0

- lokalni uzivatelske profily s vyberem, prejmenovanim a archivaci
- obrazovka statistik dostupna pres `Hra / Statistiky` ve vsech hernich rezimech
- mistni zebricek aktivnich i archivovanych lokalnich profilu s poctem her, vyhrami, remizami, prohrami, celkovym skore a uspesnosti vyher
- SQLite historie dokoncene hry a transakcni outbox pripraveny pro budouci serverovou synchronizaci
- prihlaseni online uctu pres systemovy prohlizec pomoci OIDC Authorization Code + PKCE
- bezpecne ulozeni obnovovaci relace pomoci Windows DPAPI, automaticka obnova tokenu a odhlaseni
- nacteni online uctu a vytvoreni nebo uprava verejneho profilu primo z menu hry
- platformne nezavisle herni jadro oddelene od Swingu jako zaklad budouci Android verze
- strukturovane vysledky her se shodnym ID na obou pocitacich v sitove hre
- hra clovek vs. CPU se tremi urovnemi obtiznosti a strategii nad retezci ctvercu
- promenliva doba premysleni CPU podle slozitosti pozice bez blokovani okna, zapocitana do limitu tahu
- hra clovek vs. clovek na jednom PC
- sitova hra v rezimu server/klient
- prehledny uvodni dialog s volbami rezimu hry pod sebou
- automaticky sitovy port 1080
- automaticky vyber jedineho vhodneho sitoveho adapteru
- filtrovani virtualnich sitovych adapteru pri hostovani
- zmena sitoveho adapteru a portu v nastaveni hostitelske hry
- volitelna velikost pole 5x5 az 10x10
- nastavitelna obtiznost CPU: lehka, stredni a tezka s minimaxovym dohledem
- skore, celkovy cas hry a cas premysleni kazdeho hrace
- volitelny casovy limit tahu s prohrou padem na cas
- nastaveni hry pred startem a z menu
- hostitel urcuje nastaveni sitove hry, klient ho automaticky prevezme
- nahodne pocatecni hrany bez okamzitych hotovych ctvercu
- vestaveny chat pro sitovou hru vcetne emotikonu a zvuku prichozi zpravy
- prehledne sitove info v samostatnych radcich
- restart hry vcetne potvrzeni druhe strany v sitove hre
- volitelne zvuky
- kontrola stejneho buildu pri pripojeni klienta
- distribuce aktualniho JARu uzivatelum pres GitHub Releases
- startovaci skript overuje verzi i SHA-256 stazeneho JARu

## Spusteni

Pro testery:

```bat
start.bat
```

Pro lokalni spusteni ze zdrojoveho projektu:

```bat
run.bat
```

## Sestaveni

Desktop a herni jadro jsou kompatibilni s Java 8. Server vyzaduje Java 21;
pro sestaveni celeho Maven reactoru je proto potreba JDK 21.

```powershell
mvn package
```

Vysledny samostatne spustitelny soubor je `target\squares.jar` a obsahuje i SQLite ovladac.
Samostatne herni jadro lze sestavit a otestovat prikazem `mvn -pl squares-core package`;
jeho JAR vznikne v `squares-core\target`.
Licencni texty pouzitych knihoven jsou ulozene v `squares-desktop\src\main\resources\META-INF\licenses`
a pri sestaveni se automaticky vlozi do JARu.
Lokalni data se ve Windows ukladaji do `%LOCALAPPDATA%\Squares\squares.db`.
OIDC tokeny nejsou soucasti SQLite databaze. Desktop je uklada do souboru
`%LOCALAPPDATA%\Squares\oidc-session.dat`, zasifrovane pomoci Windows DPAPI a
navazane na prihlaseny Windows ucet.

## Struktura zdrojovych kodu

Projekt je rozdelen do tri Maven modulu:

- `squares-core` - platformne nezavisle modely, herni pravidla, snapshoty, vysledky a CPU strategie
- `squares-desktop` - Windows/Swing aplikace, SQLite persistence a soucasna sitova vrstva
- `squares-server` - Spring Boot API, PostgreSQL schema a budouci serverova synchronizace

Java kod pouziva jmenny prostor `cz.humblej.squares` a uvnitr modulu je dale
rozdelen podle odpovednosti:

- `app` - spusteni a koordinace aplikace
- `model` - profily a vysledky her
- `game` - platformne nezavisle herni jadro, stav hry a strategie pocitace
- `ui` - herni panel, dialogy, zpravy a zvuky
- `network` - sitova hra
- `codec` - serializace vysledku
- `persistence` - SQLite databaze a uloziste

Rozsahlejsi casti jsou dale rozdelene na male spolupracujici komponenty. Herni
panel pouziva samostatny renderer, kodek stavu, spravce tahu pocitace, geometrii
hran a generator nahodneho rozehrani. Aplikacni vrstva oddeluje dialog nastaveni,
praci se sitovymi adresami a spravu okna. Sitova vrstva oddeluje dratovy protokol,
klientskou relaci a bezpecne predavani prace na Swing vlakno.

## Serverove API

Navrh identity, synchronizace vysledku a globalnich zebricku je popsany v
[`docs/server-api-v1.md`](docs/server-api-v1.md). Strojove citelny kontrakt pro
budouci Windows, Android a serverovou implementaci je v
[`docs/openapi/squares-api-v1.yaml`](docs/openapi/squares-api-v1.yaml).

API v1 pocita s externim OpenID Connect prihlasenim, dobrovolnym propojenim
lokalniho profilu a serveroveho hrace a idempotentnim odesilanim vysledku z
offline outboxu. Klientem oznamene hry se nezapocitavaji do ranked zebricku;
ten bude vyzadovat budouci serverem overovane partie.

### Lokalni spusteni serveru

Server, PostgreSQL a lokalni OIDC provider Keycloak lze spustit pres Docker
Compose:

```powershell
docker compose up --build -d
```

Po nabehnuti je metadata endpoint dostupny na
`http://localhost:8080/api/v1/meta` a health check na
`http://localhost:8080/actuator/health`. Flyway migrace se aplikuji automaticky
pri startu serveru. Keycloak bezi na `http://localhost:9090`; jeho lokalni
administrace pouziva `admin` / `squares-admin`.

Zivy Authorization Code + PKCE test otevre systemovy prohlizec, prijme
loopback callback a zavola `GET /api/v1/me`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\test-local-oidc-login.ps1
```

Predpripraveny uzivatel je `tester` s heslem `squares-test`. Realm, testovaci
uzivatel, admin heslo i klient `squares-smoke` jsou urcene vyhradne pro lokalni
vyvoj a nesmi se pouzit v produkci.

Heslo se zadava pouze na strance Keycloaku v systemovem prohlizeci. Desktopova
aplikace ani Squares API heslo neobdrzi. Po uspesnem prihlaseni prohlizec vrati
jednorazovy autorizacni kod na nahodny loopback port `127.0.0.1`; desktop jej
vymeni pomoci PKCE za access a refresh token. Squares API nasledne odvozuje ucet
ze stabilni dvojice OIDC `issuer` + `subject`.

Autentizovane endpointy overuji bearer JWT proti OIDC provideru. Konfigurace se
predava promennymi `SQUARES_OIDC_ISSUER`, `SQUARES_OIDC_AUDIENCE` a
`SQUARES_OIDC_JWK_SET_URI`. Vyvojove vychozi hodnoty pocitaji s realm
`squares` dostupnym na `http://localhost:9090`. Kontejner API pouziva pro
stazeni podpisovych klicu interni adresu Keycloaku, ale issuer tokenu zustava
verejna loopback adresa. Produkcni hodnoty musi smerovat na duveryhodny HTTPS
issuer.

Desktop klient nabizi prihlaseni v `Hra / Online ucet`. Ve vychozim lokalnim
prostredi pouziva stejny Keycloak a API jako Docker Compose. Adresy lze zmenit
systemovymi vlastnostmi `squares.oidc.issuer`, `squares.api.base-uri` a
`squares.oidc.client-id`, nebo promennymi prostredi
`SQUARES_DESKTOP_OIDC_ISSUER`, `SQUARES_DESKTOP_API_BASE_URI` a
`SQUARES_DESKTOP_OIDC_CLIENT_ID`. Produkcni OIDC a API adresy musi pouzivat
HTTPS; HTTP je povoleno pouze pro `localhost` a `127.0.0.1`.

### Rucni overeni online uctu ve hre

1. Spustit lokalni infrastrukturu a overit jeji stav:

   ```powershell
   docker compose up --build -d
   docker compose ps
   Invoke-RestMethod http://localhost:8080/actuator/health
   ```

   Health endpoint musi vratit stav `UP`.

2. Spustit aktualni klient ze zdrojoveho projektu pomoci `run.bat`, zvolit
   herni rezim a otevrit `Hra / Online ucet`.
3. Kliknout na `Prihlasit` a v Keycloaku pouzit `tester` / `squares-test`.
4. Po navratu do hry nastavit verejne `Uzivatelske jmeno` a `Zobrazovane
   jmeno`. Uzivatelske jmeno je globalne jedinecne, ma 3 az 24 znaku a pouziva
   pouze mala pismena bez diakritiky, cisla, `_` a `-`. Zobrazovane jmeno muze
   obsahovat Unicode a nemusi byt jedinecne.
5. Dialog znovu otevrit a overit nacteni profilu pres `GET /api/v1/me`.
6. Hru ukoncit a znovu spustit. Relace se musi obnovit bez noveho zadani hesla.
7. Tlacitkem `Odhlasit` se refresh token revokuje a lokalni zasifrovana relace
   odstrani.

Lokalni herni profil a online ucet jsou dve rozdilne identity. Lokalni profil
zustava pouzitelny bez internetu; verejne uzivatelske jmeno patri online uctu na
serveru.

### Produkcni prihlaseni

Lokalni ucet `tester` nahrazuje pouze budouci produkcni prihlaseni. Pro produkci
se pocita s Keycloakem na verejne HTTPS domene a s temito moznostmi na jeho
prihlasovaci strance:

- prihlaseni pres Google,
- prihlaseni pres Microsoft,
- registrace noveho uctu e-mailem vcetne overeni adresy a obnovy hesla.

Tyto poskytovatele a samoobsluzna registrace zatim nejsou nakonfigurovane.
Desktop bude nadale pouzivat stejny OIDC Authorization Code + PKCE tok a nebude
zpracovavat uzivatelska hesla.

Serverove integracni testy pouzivaji docasny PostgreSQL pres Testcontainers a
vyzaduji spusteny Docker:

```powershell
mvn -pl squares-server -am test
```

## Licence

Puvodni kod Squares je proprietarni software. Je dovoleno stahnout, nainstalovat
a spustit nezmeneny JAR pro osobni nekomercni pouziti. Prevzeti zdrojoveho kodu,
jeho upravy, odvozena dila a dalsi distribuce nejsou bez predchoziho pisemneho
svoleni autora dovoleny. Uplne podminky jsou v souboru `LICENSE.txt`.

Knihovny tretich stran zustavaji pod vlastnimi open-source licencemi uvedenymi
v `THIRD-PARTY-NOTICES.txt`.

## Publikace release

Release se publikuje na GitHub pomoci GitHub CLI:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\publish-github-release.ps1
```

Skript sestavi `dist\squares.jar`, vytvori nebo aktualizuje release podle verze v `pom.xml` a nahraje `squares.jar`, `start.bat` a interni `squares-launcher.ps1`.
Pred nahranim overi pritomnost i obsah vsech povinnych licencnich souboru v JARu.
