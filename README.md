# Squares

Java okenni hra pro dva hrace inspirovana hrou tecky a ctverce.

Aktualni verze: **4.1**.

## Funkce verze 4.1

- lokalni uzivatelske profily s vyberem, prejmenovanim a archivaci
- obrazovka statistik dostupna pres `Hra / Statistiky` ve vsech hernich rezimech
- mistni zebricek aktivnich i archivovanych lokalnich profilu s poctem her, vyhrami, remizami, prohrami, celkovym skore a uspesnosti vyher
- SQLite historie dokoncene hry a transakcni outbox pripraveny pro budouci serverovou synchronizaci
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

Projekt je kompatibilni s Java 8.

```powershell
mvn package
```

Vysledny samostatne spustitelny soubor je `target\squares.jar` a obsahuje i SQLite ovladac.
Licencni texty pouzitych knihoven jsou ulozene v `src\main\resources\META-INF\licenses`
a pri sestaveni se automaticky vlozi do JARu.
Lokalni data se ve Windows ukladaji do `%LOCALAPPDATA%\Squares\squares.db`.

## Struktura zdrojovych kodu

Java kod pouziva jmenny prostor `cz.humblej.squares` a je rozdelen podle
odpovednosti:

- `app` - spusteni a koordinace aplikace
- `model` - profily a vysledky her
- `game` - strategie pocitace
- `ui` - herni panel, dialogy, zpravy a zvuky
- `network` - sitova hra
- `codec` - serializace vysledku
- `persistence` - SQLite databaze a uloziste

Rozsahlejsi casti jsou dale rozdelene na male spolupracujici komponenty. Herni
panel pouziva samostatny renderer, kodek stavu, spravce tahu pocitace, geometrii
hran a generator nahodneho rozehrani. Aplikacni vrstva oddeluje dialog nastaveni,
praci se sitovymi adresami a spravu okna. Sitova vrstva oddeluje dratovy protokol,
klientskou relaci a bezpecne predavani prace na Swing vlakno.

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
