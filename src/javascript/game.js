//Ez egy spéci JavaScript függvény. Névtelen, azonnal létrejön és lefut.
//Arra használják, hogy elkülönítsék a JavaScript fájlokat egymástól többek között azért,
//hogy a fájlokban szereplő azonos nevű globális változók ne írják felül egymást.
(function() {

	var xmlServer = 'XmlRequest'; //az xml válaszok elérési útvonala
	var eventServer = 'EventRequest'; //az esemény kiváltó elérési útvonala
	var isGameFinished = false; //vége van-e a játéknak
	var message = ''; //a játékosnak szánt üzenet
	var gamePending = ''; //a játék folyamat-állapota, lehet: Indít, Folytat, Megállít
	var isAbleStartStop = false; //képes-e a játékos a játék folyamat-állapotát módosítani
	var isPlayerInGame = false;
	var timeout = 5000;	//időtúllépés. alapértelmezetten 5 másodperc, de első használat előtt a szerveren lévőre átíródik
						//kivéve, ha már akkor disconnect van...
	var checkerboard;	//a játszma táblája
	
	//csinál egy üres kétdimenziós tömböt
	function initCheckerboardArray() {
		checkerboard = new Array(9);
		for (var i = 1; i <= 8; i++) {
			checkerboard[i] = new Array(9);
		}
	}
	
	//beállítja az üzenetet a message változó tartalmára
	function setMessage() {
		$('div#message').html(message);
	}

	//megmondja, hogy hézag-e (nem szabad rá figurát tenni) a kért pozíció
	function isGap(row, col) {
		if (row < 1 || row > 8 || col < 1 || col > 8) return true;
		return !((row % 2 != 0 && col % 2 == 0) || (row % 2 == 0 && col % 2 != 0));
	}

	//a figura html kódjának előállítása
	//player lehet: 1, 2
	//type lehet: basic, queen
	function getCheckerString(player, type) { 
		var p = 'P' + player;
		var t = (type == 'basic') ? 'B' : 'Q';
		return '<div class="checker' + p + t + '"></div>';
	}

	//új játéktábla létrehozása bábúkkal
	function createCheckerboard() {
		var className;
		var string = '<table>';
		for (var i = 1; i <= 8; i++) {
			string += '<tr>';
			for (var j = 1; j <= 8; j++) {
				className = isGap(i, j) ? 'class="gap"' : '';
				var checker = checkerboard[i][j] == null ? '' : checkerboard[i][j];
				string += '<td id="' + i + '_' + j + '" ' + className + '>' + checker + '</td>';
			}
			string += '</tr>';
		}
		string += '</table>';
		$("div#checkerboard").html(string);
	}

	//egy stringből boolean értéket ad vissza
	//a string lehet: true, false
	function strToBool(string) {
		return string == "true";
	}

	//játéktábla feltöltése figurákkal a szerver válasza alapján és ha már kérdezünk ...
	//akkor még öt adatot lekérünk:
	// - a játék folyamat-állapota
	// - léphet-e a játékos
	// - StartStop joga van-e a játékosnak
	// - mi a felhasználónak szánt üzenet
	// - vége van-e a játéknak
	//ezeket az adatokat ebben a fájlban mindenhol látható változóba eltárolni, hogy használhassa minden függvény
	function fillCheckerboardAndGetData() {
		//Mivel itt fordul elsőnek elő a kódban AJAX, megjegyzésbe tettem, hogyan működik jQuery-ben.
		$.ajax({
			type: "POST", //ezzel a metódussal megy a kérés
			url: xmlServer, //erre a címre megy a kérés.
			data: "action=getPlayData", //a kérésben lévő üzenet(ek)
			cache: false, //nem cache-eli az adatokat, mert naprakész infó kell minden kérés esetén
			async: false, //blokkolja a böngészőt a kérés idejére, hogy ne csinálhasson a felhasználó hülyeséget közben
			dataType: "xml",
			success: function(response) { //ha a szerver sikeresen válaszol, ez a függvény fut le
				fillCheckerboard($(response).find('checkerboard'));
				setDatas($(response).find('data')); //ez az XML-ben lévő data nevű teg/objektum, amit átadunk az adatbeállító eljárásnak
				redirectIfNeed();
			},
			error: function(xhr, status, error) {
				if(status == 'error');
					disconnect();
			}
		});
	}

	//Ez az eljárás hozza létre a figurákat (a tömbbe) a checkerboard objektum alapján, ami az XML-ből jött
	function fillCheckerboard(board) {
		initCheckerboardArray();
		//a checkboard teg minden gyerekelemén végigmegy, mint egy foreach
		$(board).children().each(function() {
			checkerboard[$(this).attr('row')][$(this).attr('col')] = getCheckerString($(this).attr('player'), $(this).attr('type'));
		});
	}

	//Ez az eljárás állítja be a globális változókat az adat objektum alapján, ami az XML-ből jött
	function setDatas(data) {
		isPlayerInGame = strToBool(data.attr('isPlayerInGame'));
		isAbleStartStop = strToBool(data.attr('isAbleStartStop'));	//a data teg isAbleStartStop attribútumának a tartalma
																	//és boolean értékké konvertálva
		isGameFinished = strToBool(data.attr('isGameFinished'));
		message = data.attr('message');
		timeout = (Number)(data.attr('timeout'));
		gamePending = data.attr('gamePending');
	}

	//átirányít, ha a felhasználó nincs játszmához csatlakozva
	function redirectIfNeed() {
		if (!isPlayerInGame) {
			redirect();
		}
	}

	function redirect() {
		$(location).attr('href', 'sign_in.jspx');
	}

	// 'px' levágása a string végéről, hogy számot kapjunk
	function getPixel(string) {
		if (string == 'auto') return 0;	//egyes böngészőkben a kezdőérték nem 0, hanem auto (pl. chrome)
										//és nem szeretem a NaN értéket, mert nem lehet vele számolni...
		return (Number)(string.substr(0, string.length - 2));
	}

	//a figura megjelenésben beállított top illetve left paramétereket adja vissza
	function getStylePosition(checker) {
		var position = Array();
		position['left'] = getPixel($(checker).css('left'));
		position['top'] = getPixel($(checker).css('top'));
		return position;
	}

	//a figura top illetve left paraméterének beállítása
	function setStylePosition(checker, position) {
		$(checker).css('top', position['top'] + 'px');
		$(checker).css('left', position['left'] + 'px');
	}

	//a figura relatív pozíciója a táblában elfoglalt helyhez képest
	//tehát, hogy hány sorral és oszloppal van eltolva megjelenésben
	function getMovedPosition(checker) {
		var stylePos = getStylePosition(checker);
		var left = stylePos['left'];
		var top = stylePos['top'];
		left = Math.round(left / 70); // { a CSS-ben beállított méret
		top = Math.round(top / 70);   // } 70 x 70 pixel, ezért osztom ennyivel
		var position = Array();
		position['row'] = top;
		position['col'] = left;
		return position;
	}

	//a figura pozíciója a táblában
	//tehát, hogy eredetileg melyik sorban és oszlopban van a html kódban
	function getBoardPosition(checker) {
		var position = Array();
		var p = $(checker).parent().attr('id');
		position['row'] = (Number)(p.substr(0, 1));
		position['col'] = (Number)(p.substr(2, 1));
		return position;
	}

	//a figura látszólagos pozíciója a táblán
	function getPosition(checker) {
		var posB = getBoardPosition($(checker));
		var posM = getMovedPosition($(checker));
		var position = Array();
		position['row'] = posB['row'] + posM['row'];
		position['col'] = posB['col'] + posM['col'];
		return position;
	}

	//figurák mozgathatóságának beállítása
	function setCheckersToDraggable() {
		var position;
		$("div.checkerP1B, div.checkerP2B, div.checkerP1Q, div.checkerP2Q").draggable(
			{
				//eseménykezelő: figura mozgatásának elkezdése
				//eltárolja azt, hogy hol van a figura a mozgás elkezdésekor
				start: function(event, ui) {
					position = getStylePosition($(this));
				}
			},
			{
				//eseménykezelő: figura mozgatásának befejezése
				//szól a szervernek, hogy honnan hova mozgott a játékos és frissíti a felületet
				stop: function(event, ui) {
					var checker = $(this);
					var posFrom = getBoardPosition(checker);
					var posTo = getPosition(checker);
					if (posTo['row'] == posFrom['row'] && posTo['col'] == posFrom['col'])
						return; //ha ugyan oda lépett a felhasználó, ahonnan elindult, akkor nincs mit tenni, kilép
					//csak akkor küld mozgatás kérést, ha olyan helyre lépett, ahová lehet figurát tenni
					if (!isGameFinished && !isGap(posTo['row'], posTo['col'])) {
						$.ajax({
							type: "POST",
							url: xmlServer,
							data: "action=move&fromRow=" + posFrom['row'] + "&fromCol=" + posFrom['col'] +
										"&toRow=" + posTo['row'] + "&toCol=" + posTo['col'],
							async: false //blokkolódik a böngésző míg nem jön meg a szerver válasza
						});
					}
					//ha biztosan hibás a lépés, mozgatás előtti pozícióba visszaállítás
					else {
						setStylePosition(checker, position);
					}
				}
			},
			{ grid: [70, 70] }, //70 x 70 pixelenként lehet mozgatni a figurákat (ez a CSS-ben beállított mérettel egyezik!)
			{ opacity: 0.3 } //mozgatás közben legyen átlátszó
		);
	}

	//a felület frissítése/újrarajzolása: játéktábla, 
	function refreshUserInterface() {
		// { fontos a sorrend, mert:
		fillCheckerboardAndGetData(); //az üres tömböt feltölti a szerver válasza alapján ÉS fentebb említett egyéb adatokat is kap
		createCheckerboard(); //az adatok alapján kirajzolja a táblát
		setCheckersToDraggable(); //a létrehozott figurákat mozgathatóvá teszi
		// }
		// { ezek után jöhet az egyéb adatok frissítése, mindegy a sorrend:
		setMessage();
		setStartStopButton();
		setGiveUpButton();
		// }
	}

	//AJAX long-polling technika...
	//lényege, hogy olyan ajax kérést indít a script, aminek nincs időtúllépése
	//a szerver oldalon while(true) ciklus van (meg egy sleep(1), hogy a processzor ne menjen tropára)
	//a while ciklusból akkor lépünk ki, amikor a kívánt esemény bekövetkezik
	//ennek következtében a szerver visszatér egy válasszal és a scriptben elindul egy függvény lefutás (mivel
	//sikerült az ajax kérés végrehajtása) Ezt a lefutó függvényt lehet eseménykezelőnek használni. :)
	//Ebben a függvényben természetesen rekurziót kell végezni, hogy újra megkapja a következő eseményt.
	//Ebből az következik, hogy nem lehet végtelen számú lépést így figyelni.
	//A Firefox kb. 9050 rekurziót bír, tehát minimum 8000 rekurziót bírnia kell egy böngészőnek.
	//A nyolcezredik esemény lefutásakor újratöltjük az oldalt, biztos, ami biztos...

	var eventCounter = 0; //esemény számláló
	function isNeedReloadPage() {
		return ++eventCounter == 8000;
	}

	//az oldal újratöltése, ha elértük a nyolcezredik eseményt
	function reloadPageIfNeed() {
		if (isNeedReloadPage()) location.reload();
	}

	//Nos! a függvény a következő:
	//a figyelés addig tart, míg nincs vége a játéknak
	//szól a szervernek, hogy adjon választ, ha esemény történt
	//ha esemény történik, frissül a felület majd újra kezdődik a figyelés
	//a szerver esemény jelzést ad, ha: lépett a másik játékos, csatlakozott/kilépett vagy StartStop gombra kattintott valamelyik játékos
	//FONTOS: a saját játékosunk lépésére nem dob a szerver eseményt.
	//Csak a másik játékos lépése esemény és a script egy NEM aszinkron mozgásküldést halyt végre.
	//Amikor sikeresen fogadta (és végrehajtotta) a szerver a mozgás kérését, a script ekkor frissíti a felületet.
	function startActionListener() {
		if (!isGameFinished) {
				$.ajax({
					type: "POST",
					url: eventServer,
					data: "action=waitGameEvent",
					async: true, //hogy ne blokkoljon a böngésző, míg nem lép a felhasználó
					success: function(response) { //ha megérkezik a válasz, akkor a másik játékos lépett
						reloadPageIfNeed(); //első utasítás, hogy ne fusson fölöslegesen a többi, ha újra kell tölteni az oldalt
						refreshUserInterface();
						startActionListener(); //rekurzió: figyelés újrakezdése, utolsó utasítás értelemszerűen
					}
				});
		}
	}

	//Ebből tudja a szerver, hogy nem szakadt meg a kapcsolat a játékosal.
	function startTellImHere() {
				$.ajax({
					type: "POST",
					url: eventServer,
					data: "action=ImHere",
					async: true,
					timeout: timeout + 5000, //ha a szerver nem válaszol azon belül, amit ígért (plusz 5 másodperc a biztonság kedvéért), akkor disconnect
					success: function(response) {
						startTellImHere();
					},
					error: function() { //ha hibába futunk, az azt jelenti, hogy disconnect történt
						refreshUserInterface();
					}
				});
	}

	//disconnect esetén be kell állítani a változókat
	function disconnect() {
		isAbleStartStop = false; //StartStop gomb tiltása, mert fölösleges
		$('input#btExit').attr('disabled', 'disabled'); //kilépés gomb tiltása
		message = 'Megszakadt a kapcsolat a szerverrel.'; //a játékosnak üzenni, hogy disconnect történt
	}

	//megjeleníti a gombot, beállítja a megfelelő szöveget: Start vagy Stop
	//eseménykezelőt rendel a gombhoz, hogy szóljon a kérésről a szervernek
	//csak akkor küld StartStop üzenetet, ha kapcsolódva van a másik játékos is
	function setStartStopButton() {
		var dis = isAbleStartStop ? '' : 'disabled="disabled"';
		$('span#start_stop').html('<input id="btStartStop" type="button" ' + dis + ' value="' + gamePending + '" />');
		$('input#btStartStop').click(function() {
			if (isAbleStartStop) { //fölösleges igazából, mert úgy is disabled lesz a gomb és nem fogad eseményt, de támadás ellen +1 védelem
				$.ajax({
					type: "POST",
					url: xmlServer,
					data: "action=start_stop",
					async: false //blokkolja a böngészőt a kérés idejére, hogy addig nehogy hülyeséget csináljon a felhasználó
				});
			}
		});
	}

	//eseménykezelőt rendel az oldal elhagyásához, hogy jelezze a script a szervernek, hogy kilépett a játékos a játszmából
	function setUnloadEvent() {
		$('input#btExit').click(function() {
			sendExit();
		});
		/*$(window).unload(function() {
			sendExit();
		});*/ //megint csak az Opera böngésző betett ennek...
	}

	function sendExit() {
			$.ajax({
				type: "POST",
				url: xmlServer,
				data: "action=exitGame",
				async: false,
				//fontos, hogy ne aszinkron legyen, hogy el lehessen küldeni az üzenetet még az oldal elhagyása előtt
				success: function(response) {
					redirect();
				}
			});
	}

	//eltávolítja a html kódban lévő javascript szükséges szöveget, ahova a tábla fog kerülni, miután lejön az adat a szerverről
	function removeJavascriptMessage() {
		$('div#checkerboard').html('Kérem, várjon...');
	}

	function setGiveUpButton() {
		$('span#give_up').html('<input type="button" id="btGiveUp" value="Felad" />');
		$('input#btGiveUp').click(function() {
			var accept = confirm('Biztos, hogy feladja a játékot?');
			if (accept) giveUpGame();
		});
		if (isGameFinished) $('input#btGiveUp').attr('disabled', 'disabled');
	}

	//játszma feladása
	function giveUpGame() {
		$.ajax({
				type: "POST",
				url: xmlServer,
				data: "action=give_up",
				async: false
		});
	}

	//a kilépés gomb linkjét eltávolítja
	function removeExitLink() {
		var button = $('a').html();
		var parent = $('a').parent();
		$('a').remove();
		parent.html(parent.html() + button);
	}

	//eseménykezelő: az oldal betöltődése. ITT KEZDŐDIK AZ ÉLET! :)
	$(document).ready(function() {
		removeExitLink();
		setGiveUpButton();
		setUnloadEvent();
		removeJavascriptMessage();
		// { fontos a sorrend, mert a játékfelület feltöltésekor beállítódnak változók, amiket használ a listener
		refreshUserInterface();
		startActionListener();
		startTellImHere();
		// }
	});

})();