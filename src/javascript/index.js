//FIGYELEM! a game.js-ben részletes magyarázat van a működési elvről és, hogy mi mit csinál!
(function() {

	var xmlServer = 'XmlRequest';
	var eventServer = 'EventRequest';
	var selected = ''; //annak a játszmának a neve, ami ki van jelölve
	var first = ''; //az első sora a listának
	var user = ''; //a belépett felhasználó neve
	var isAdmin = false; //módosíthatja-e mások játszmáit
	var owners = new Array(); //a tulajdonosok nevének a listája, hogy tudni lehessen az aktuális user hozhat-e létre játszmát
	var playerNumbers = new Array(); //játszmánként a csatlakozott játékosok száma
	var isPlayerInGame = false;
	var timeout = 5000; //mennyi idő után adja fel a script, alapértelmezetten 5 mp, de ez még az első kérés előtt módosul a szerveren lévőre
	var disconnected = false;

	function strToBool(value) {
		return value == 'true';
	}

	//ajax kérést küld, hogy kéri a játszmalistát és frissíti a táblát az alapján
	function refreshUserInterface() {
		$.ajax({
			type: "POST",
			url: xmlServer,
			data: "action=getPlayList",
			cache: false,
			async: false, //fontos, hogy ne aszinkron legyen, hogy a timeout változó be tudjon állítódni
			dataType: "xml",
			success: function(response) {
				var resp = $(response).find('response');
				setMessage($(resp).attr('message'));
				user = $(resp).attr('user');
				timeout = (Number)($(resp).attr('timeout'));
				isAdmin = strToBool($(resp).attr('isAdmin'));
				isPlayerInGame = strToBool($(resp).attr('isPlayerInGame'));
				redirectIfNeed();
				setLastActionMessage($(resp).attr('lastAction'));
				var string = '<tr><td class="radio"></td><td>Név</td><td>Tulajdonos</td><td>Jelszó</td><td>Státusz</td></tr>';
				var checked;
				first = $(response).find('play').attr('name');
				var counter = 0;
				owners = new Array();
				playerNumbers = new Array();
				$(response).find('play').each(function() {
					if (strToBool($(this).attr('protected'))) {
						checked = 'checked="checked"';
					}
					else {
						checked = '';
					}
					string += '<tr>';
					string += '<td class="radio">' +
				              '<input class="radio" type="radio" name="game_name" ' +
							         'value="' + $(this).attr('name') + '" />' +
							  '</td>';
					string += '<td>' + $(this).attr('name') + '</td>';
					string += '<td>' + $(this).attr('owner') + '</td>';
					string += '<td><input class="pw" type="checkbox" ' + checked + ' /></td>';
					string += '<td>' + $(this).attr('state') + '</td>';
					string += '</tr>';
					owners[counter] = $(this).attr('owner');
					playerNumbers[$(this).attr('name')] = $(this).attr('playerNumber');
					counter++;
				});
				if (counter == 0) {
					$("table#list").html('<tr><td>Kérem, hozzon létre játszmát.</td></tr>');
					disableButtons();
				}
				else {
					$("table#list").html(string);
					setEvent();
				}
				setRadioCheck();
				setCreateButton();
			},
			error: function() {
				//disconnect detektálva, disconnect megjelenítése
				disconnect();
			}
		});
	}

	function disconnect() {
		disconnected = true;
		setMessage('Megszakadt a kapcsolat a szerverrel.');
		$('input.radio').attr('disabled', 'disabled'); //minden rádiógomb letiltása
		$('input.button').attr('disabled', 'disabled'); //minden gomb letiltása
	}

	function setLastActionMessage(lastAction) {
		$('div#lastAction').html('Utolsó bejelentkező: ' + lastAction);
	}

	function redirectIfNeed() {
		if (user == '') {
			$(location).attr('href', 'index.jspx');
		}
		if (isPlayerInGame) {
			$(location).attr('href', 'game.jspx');
		}
	}

	//ha a kijelölt játszma létezik, engedélyezi a gombokat és kijelöli a játszmát
	//különben tiltja a gombokat
	function setRadioCheck() {
		var selRad = findSelected();
		if (selRad != null) {
			$(selRad).attr('checked', 'checked');
			onSelectRadio();
		}
		else {
			selected = '';
			disableButtons();
		}
	}

	function findSelected() {
		var selRad = null;
		$("input.radio").each(function() {
			if (selected == $(this).attr('value')) {
				selRad = $(this);
			}
		});
		return selRad;
	}

	function onSelectRadio() {
		var numPlayer = findNumberPlayer();
		if (isOwner() || isAdmin) {
			enableButtons();
			if (numPlayer == 2) {
				disableConnectButton();
			}
		}
		else {
			disableButtons();
			if (numPlayer != 2) {
				enableConnectButton();
			}
		}
	}

	function findNumberPlayer() {
		return playerNumbers[selected];
	}

	//ha egy játszmát kijelölnek, engedélyezi a gombokat
	//és a checkboxok váltóztatását letiltja
	function setEvent() {
		$("input.radio").change(function() {
			selected = $(this).attr('value');
			onSelectRadio();
		});
		$("input.pw").click(function() {
			$(this).attr('checked', !$(this).attr('checked'));
		});
	}

	var eventCounter = 0;
	function isNeedReloadPage() {
		return ++eventCounter == 8000;
	}

	function reloadPageIfNeed() {
		if (isNeedReloadPage()) location.reload();
	}

	function startActionListener() {
		$.ajax({
			type: "POST",
			url: eventServer,
			data: "action=waitListEvent",
			async: true,
			timeout: timeout + 5000,
			success: function(response) {
				reloadPageIfNeed();
				refreshUserInterface();
				startActionListener();
			},
			error: function() {
				//disconnect történt, felület frissítése
				refreshUserInterface();
			}
		});
	}

	function setMessage(message) {
		$('div#message').html(message);
	}

	function enableButtons() {
			$("input.button").attr('disabled', null);
			setCreateButton();
	}

	function disableButtons() {
			$("input.button").attr('disabled', 'disabled');
			$("input#exit").attr('disabled', null);
			$("input#rules").attr('disabled', null);
			setCreateButton();
	}

	function enableCreateButton() {
			$("input#create").attr('disabled', null);
	}

	function disableCreateButton() {
		$("input#create").attr('disabled', 'disabled');
	}

	function enableConnectButton() {
			$("input#connect").attr('disabled', null);
	}

	function disableConnectButton() {
			$("input#connect").attr('disabled', 'disabled');
	}

	function removeJavaScriptMessage() {
		$("table#list").html('<tr><td>Kérem, várjon...</td></tr>');
	}

	function initFrame() {
		//ezt azért nem tettem bele a html kimenetbe, mert nem szabványos attribútum a REV
		$("a.frame").attr('rev', 'width: 600px; height: 350px;');
	}

	function initRemoveButton() {
		$("input#remove").click(function() {
			if (!isOwner() && !isAdmin) return true;
			return confirm('Biztos, hogy törölni akarja a játszmát?');
		});
	}

	function initClearButton() {
		$("input#clear").click(function() {
			return confirm('Ha újraindítja a játszmát, a játékosok ki lesznek léptetve a játékból és a bábúk alaphelyzetbe kerülnek.\n\nBiztos, hogy ezt akarja?');
		});
	}

	function isOwner() {
		var tr = 0;
		var exists = false;
		$('table#list tr').each(function() {
			if (tr != 0) {
				var td = 0;
				var pName, pOwner;
				$(this).children().each(function() {
					if (td == 1) {
						pName = $(this).html();
					}
					else if (td == 2) {
						pOwner = $(this).html();
						if (user == pOwner && selected == pName) {
							exists = true;
						}
					}
					td++;
				});
			}
			tr++;
		});
		return exists;
	}

	function setCreateButton() {
		if (isOwnerAll()) disableCreateButton();
		else enableCreateButton();
	}

	function isOwnerAll() {
		for (i = 0; i < owners.length; i++) {
			if (owners[i] == user) return true;
		}
		return false;
	}

	$(document).ready(function() {
		initFrame();
		initRemoveButton();
		initClearButton();
		removeJavaScriptMessage();
		refreshUserInterface();
		startActionListener();
	});

})();

//debuggoláskor keletkezett kód, ami lehet, még jól jöhet
/*
console.log(response.documentElement);
for(i in response) {
	try {
		console.log(i + ' => ' + response[i]);	
	}
	catch(e) {
		console.log(i + ' => HIBA: ' + e);  
	}
}
return;
*/