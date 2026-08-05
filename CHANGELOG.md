# Changelog

## 4.6.0 - 2026-08-05

- Pridan verejny casual zebricek podle celkoveho skore s kurzorovym strankovanim a samostatnym endpointem pro vlastni umisteni.
- Dialog statistik obsahuje mistni, globalni casual a budouci ranked kartu; sitova data nacita asynchronne a osetruje nacitani, odhlaseni, prazdny zebricek a nedostupny server.
- Opravena soubezna synchronizace vysledku, aby pozadavek vznikly behem probihajiciho odesilani nezustal bez obsluhy.
- Server odmita podvrzene `playerId` u sedadla soupere a prijima identitu pouze pro autentizovaneho odesilatele.
- Verze projektu a kontrola sitoveho buildu byly sjednoceny na 4.6.0.

## 4.5.0 - 2026-08-03

- Implementovany idempotentni endpointy `PUT/GET /api/v1/me/game-submissions/{gameId}`.
- Server overuje registrovanou instalaci, identitu odesilatele a domenove invarianty `GameResult`.
- Shodna podani obou stran LAN hry se paruji jako `PEER_CONFIRMED`; rozdilna podani konci jako `CONFLICTED`.
- Lokalni SQLite outbox byl rozsiren o stavy `SENDING`, `RETRY`, `SENT` a `DEAD`, obnovu preruseneho odesilani a exponencialni odklad.
- Vysledky se synchronizuji automaticky po hre a rucne z dialogu online uctu.
- Dialog online uctu zobrazuje pocty cekajicich, odesilanych, odeslanych a chybnych vysledku.
- Hlaseni desktopove hry i identity byla presunuta do externich ceskych a anglickych katalogu.
- V menu hry lze zvolit cestinu nebo anglictinu; volba se pouzije pri pristim spusteni.
- Klient posila serveru zvoleny jazyk v hlavicce `Accept-Language`.
- Serverove schema pro cisty start odkazuje herni data primo na centralni Player Identity.
- Verze projektu a kontrola sitoveho buildu byly sjednoceny na 4.5.0.

## 4.4.0 - 2026-08-02

- Zavedena centralni serverova `Player Identity`, ktera je oddelena od
  autentizacniho uctu i Squares-specifickeho verejneho profilu.
- Sdilena identita byla fyzicky vyclenena do modulu `humblej-identity-model`,
  `humblej-identity-client`, `humblej-identity-desktop` a `humblej-identity-server`.
- OIDC token lifecycle a HTTP relace jsou znovupouzitelne bez Squares UI;
  DPAPI, systemovy prohlizec a loopback PKCE zustavaji Windows adapterem.
- Spolecne `GET /me` vraci pouze ucet a centralni `playerId`; Squares profil ma
  samostatne `GET/PUT /me/profile`.
- Existujici serverova `playerId` zachovava migrace beze zmeny a pouziva ji jako
  ID centralni identity.
- Implementovan idempotentni endpoint
  `PUT /api/v1/me/installations/{installationId}` pro registraci a obnovu instalace.
- Desktop generuje jedno trvale `installationId`, uklada je do SQLite a po
  prihlaseni je registruje s platformou, verzi aplikace/jadra a locale.
- Do SQLite byla pridana samostatna vazba lokalniho profilu na serverove
  `playerId`, vcetne ochrany pred prepsanim jinym online uctem.
- Dialog `Hra / Online ucet` zobrazuje aktualni lokalni profil, Player ID,
  Installation ID a umoznuje profil propojit nebo vedome odpojit.
- Doplneny migrace, integracni a desktopove testy i dokumentace API.

## 4.3 - 2026-08-01

- Pridan zaklad serveroveho modulu se Spring Boot, PostgreSQL migracemi a Docker Compose.
- Pridan verejny endpoint `GET /api/v1/meta` a health check serveru.
- Pridana validace OIDC JWT tokenu vcetne issueru, audience a subjectu.
- Pridano automaticke zakladani serverovych uctu a identit, `GET /api/v1/me`
  a idempotentni `PUT /api/v1/me/profile`.
- Pridany integracni testy identity API proti PostgreSQL vcetne soubezneho
  prvniho prihlaseni a kolize verejneho handle.
- Pridan lokalni Keycloak realm s PKCE klientem, testovacim uzivatelem a
  PowerShell testem ziveho prihlaseni proti `GET /api/v1/me`.
- Desktop klient se prihlasuje pres systemovy prohlizec pomoci OIDC
  Authorization Code + PKCE a zachytava callback na nahodnem loopback portu.
- Access, refresh a ID token se ukladaji sifrovane pomoci Windows DPAPI;
  klient obnovuje access token pred vyprsenim i po `401` a neplatnou relaci
  bezpecne zahodi.
- V menu `Hra / Online ucet` lze nacist `GET /api/v1/me`, dokoncit nebo upravit
  profil pres `PUT /api/v1/me/profile` a odhlasit se vcetne revokace refresh
  tokenu.

## 4.2 - 2026-07-20

- Stav desky, pravidla tahu, skore, herni hodiny a zivotni cyklus hry byly oddeleny od Swingu do platformne nezavisleho `GameEngine`.
- Pridany sdilene typy tahu a snapshotu hry a samostatne sestaveni strukturovaneho vysledku bez zavislosti na uzivatelskem rozhrani.
- Swingovy panel nyni funguje jako adapter nad hernim jadrem; sitovy format stavu zustal zpetne kompatibilni.
- Nahodne rozehrani a datovy typ tahu CPU byly sjednoceny s hernim jadrem.
- Pridany prime testy pravidel, casoveho limitu, snapshotu a nahodneho rozehrani bez spousteni Swingu.
- Herni jadro bylo fyzicky vycleneno do samostatneho Maven modulu `squares-core`; Swing aplikace a jeji infrastruktura jsou v modulu `squares-desktop`.
- Verze projektu a kontrola sitoveho buildu byly sjednoceny na 4.2.0.
- Navrzeno verzovane REST API v1 a serverove identity pro spolecny Windows a Android ucet, OIDC prihlaseni, idempotentni synchronizaci vysledku a oddelene casual/ranked zebriky.

## 4.1 - 2026-07-20

- Puvodni kod Squares je chranen proprietarni licenci pro osobni nekomercni pouziti nezmeneneho JARu.
- Do JARu se vklada vlastni licence i licence knihoven tretich stran a publikacni skript jejich obsah povinne overuje.
- Java jmenny prostor byl zmenen z `cz.codex.squares` na `cz.humblej.squares` a tridy byly rozdeleny do logickych balicku.
- Tridy `SquaresPanel`, `SquaresApp` a `NetworkGame` byly rozdeleny na mensi komponenty pro vykreslovani, kodovani stavu, tahy CPU, konfiguracni dialogy, sitovy protokol a klientskou relaci bez zmeny jejich verejneho chovani.
- Verze projektu a kontrola sitoveho buildu byly sjednoceny na 4.1.0.

## 4.0 - 2026-07-18

- Zavedeny domenove modely `PlayerProfile`, `PlayerResult` a `GameResult` jako zaklad profilu, historie her a budoucich zebricku.
- Konec hry se uvnitr aplikace predava jako strukturovany vysledek vcetne ID hry, rezimu, duvodu ukonceni, skore, casu a parametru hry; text pro dialog a sitovy protokol se vytvari az ve vrstve zprav.
- Pridana lokalni SQLite databaze v uzivatelske slozce aplikace, verzovane schema a transakcni outbox pro budouci synchronizaci se serverem.
- Pridana sprava uzivatelskych profilu: vytvoreni, vyber, prejmenovani a archivace; lokalni hra umoznuje priradit profil i druhemu hraci.
- Vysledky lokalnich, CPU a sitovych her se ukladaji vcetne snapshotu jmen a ID profilu; opakovany zapis stejneho `gameId` nevytvori duplicitu.
- Pridana polozka `Hra -> Statistiky` pro lokalni hru, hru proti CPU, sitoveho hostitele i klienta se souhrnem aktualniho profilu a mistnim zebrickem.
- Zebricek agreguje pocet her, vyhry, remizy, prohry, celkove skore a procento vyher ze zaznamu lokalnich profilu; zahrnuje i archivovane profily a vynechava CPU, hosty a pouze vzdalene profily.
- Poradi zebricku urcuje celkove skore, dale pocet vyher, pocet her a nakonec jmeno profilu.
- Sitovi hraci si pri spojeni vymeni identity profilu a oba ulozi stejny strukturovany vysledek se shodnym ID hry.
- Distribucni JAR se sestavuje pres Maven Shade a obsahuje SQLite ovladac i potrebne nativni knihovny.
- Verze projektu a kontrola sitoveho buildu byly sjednoceny na 4.0.0.

## 3.1 - 2026-07-15

- Nadpis uvodniho dialogu pro vyber herniho rezimu byl zmenen na `REZIM HRY` a vycentrovan.
- CPU ma promennou prodlevu tahu podle slozitosti pozice a vypocet probiha na pozadi; prodleva se zapocitava i pri casovem limitu, takze CPU muze prohrat na cas.
- CPU rozpoznava bezpecne tahy a vyhodnocuje cele nucene retezce, takze pri nutne obeti vybira levnejsi variantu.
- Tezka obtiznost pouziva casove omezeny minimax s alfa-beta prorezavanim a transpozicni tabulkou; vypocet tahu ma limit, aby zustalo okno plynule.
- Rozhodovani CPU je oddeleno od Swing panelu do samostatne strategie, kterou lze overovat simulovanymi partiemi.

## 3.0 - 2026-07-13

- Pridan novy rezim hry clovek vs. CPU na jednom PC.
- CPU hraje za modreho hrace a pouziva heuristiku nad dostupnymi tahy: bere body, vyhyba se zbytecnemu darovani ctvercu a podle obtiznosti pridava miru nahody.
- Pridany tri urovne obtiznosti CPU: lehka, stredni a tezka.
- Obtiznost CPU je rizena jednim parametrem `skill`, ktery ovlivnuje pravdepodobnost nejlepsiho tahu, tresty za rizikove tahy a miru nahody.
- Uvodni dialog vyberu rezimu byl zprehlednen do svislych tlacitek: clovek vs. CPU, clovek vs. clovek, sitova hra - server a sitova hra - klient.
- Odstraneno zbytecne tlacitko OK z uvodniho dialogu vyberu rezimu.

## 2.2 - 2026-07-12

- V nastaveni sitove hry lze zmenit sitovy adapter a port; start hostitele zustava jednoduchy s automatickym nevirtualnim adapterem a portem 1080.
- Pridana distribuce uzivatelum pres GitHub Releases.
- Pridan uzivatelsky `start.bat` a interni `squares-launcher.ps1`, ktery overi posledni release, stahne aktualni `squares.jar` a spusti hru.
- Launcher porovnava verzi i SHA-256 digest GitHub assetu, takze zachyti i prepsany JAR ve stejnem releasu.
- Pridan `publish-github-release.ps1` pro sestaveni JAR a vytvoreni nebo aktualizaci GitHub releasu.

## 2.1 - 2026-07-11

- Ghost hrana se v sitove hre zobrazuje jen pri tahu lokalniho hrace.
- Cas hostitele se pri cekani na klienta drzi na nule a spousti se az po pripojeni klienta.
- Sitova hra automaticky pouziva port 1080 bez dotazu.
- Vyber sitoveho adapteru nenabizi virtualni adaptery; pri jedinem vhodnem adapteru se vyber preskoci.
- Pokud neni nalezen zadny vhodny sitovy adapter, hostitelska hra se nespusti a zobrazi jasnou hlasku.
- Spodni sitove info se zobrazuje ve trech radcich: IP:port, plocha a stav klienta.
- Startovni informacni dialog hostitele byl odstraneny.
- Chat dostal zvuk prichozi zpravy, jednodussi nadpis a upravene rozlozeni emotikonu.
- Chatove zpravy a vstup pouzivaji bezny font kvuli cestine; emoji tlacitka zustavaji ve fontu Segoe UI Emoji.

## 2.0 - 2026-07-06

- Pridan vestaveny chat pro sitovou hru vcetne emotikonu, rychlych textu a casu odeslani zpravy.
- Chat zobrazuje zpravy bez textovych prefixu; radky jsou podbarvene barvou hrace a dlouhe zpravy se zalamuji.
- Pridan dialog nastaveni hry s volbou velikosti pole, maximalniho casu na premysleni a nahodneho vygenerovani hran.
- Zapojeny casove limity tahu: pri vyprseni casu hrac prohrava padem na cas.
- Nahodne vygenerovane pocatecni hrany nevytvari uzavrene ctverce ani ctverce se tremi stranami.
- V sitove hre urcuje nastaveni hostitel a klient prebere velikost pole, casove limity i pocatecni stav.
- Pridana kontrola buildu pri pripojeni klienta; sitova hra vyzaduje stejny build na obou pocitacich.
- Upraveno chovani restartu a nastaveni v sitove hre, aby se neprekryvaly modalni dialogy.
- Cas v lokalni hre se pozastavi pri dialogu nastaveni, napovede nebo neaktivnim okne; v sitove hre zustava spolecny cas synchronizovany hostitelem.

## 1.1 - 2026-07-05

- Opraven restart v sitove hre: restart vyvolany hostitelem musi potvrdit i klient.
- Pridana polozka menu `Hra -> O hre` s informaci o buildu a jednoduchou napovedou.
- Opraveno prizpusobeni velikosti okna pri zmene hraci plochy z vetsi na mensi.

## 1.0 - 2026-07-04

- Prvni stabilni verze hry Squares.
- Hra na jednom PC i sitova hra v rezimu hostitel/klient.
- Volitelna velikost hraci plochy 5x5 az 10x10.
- Skore, cas hry a cas premysleni kazdeho hrace.
- Restart hry, zmena velikosti pole a volitelne zvuky.


