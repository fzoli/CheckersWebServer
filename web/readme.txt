Az JavaScript kódok a Netbeans projekt src könyvtárában vannak.

A szoftver használata:

- belépés kötelezõ. az elsõ belépés egyben regisztráció is, tehát elsõ alkalommal meg kell adni a pontos felhasználónevet.
  a többi bejelentkezés esetén már írható az egész felhasználónév kisbetûvel.
- bejelentkezve lehetõség van játszmát létrehozni, ha még nem tette meg a felhasználó.
  mindenkinek 1 játszma tulajdonosa lehet.
- a tulajdonosnak joga van a játszmát törölni és újraindítani
  (adatbázisban be lehet állítani a felhasználót adminná, ez esetben bármelyik játszmát kezelheti)
- csatlakozni lehet egy játszmához, ha nincs megtelve (2 játékos van benne)
- ha egy játékos elhagyja menet közben a játékot, átveheti a helyét más, ennek kivédésére jelszóvédett játszmát lehet létrehozni
  a tulajdonostól nem kér a rendszer jelszót
- játszmához csatlakozás után mindkét játékosnak el kell indítani a játszmát
- bármikor feladhatja a játékos a játszmát, ha akarja
- szüneteltetheti a játékos a folyamatban lévõ játszmát
  csak az indíthatja el a játszmát, aki a szüneteltetést kezdte
- ha egy befejezett játszmához csatlakozik egy játékos, akkor újraindul a játszma magától