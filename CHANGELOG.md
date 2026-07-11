# Changelog

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
