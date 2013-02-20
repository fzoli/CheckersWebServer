//a játszma létrehozása is ajax-szal működik
(function() {

	var success = false; //sikerült-e a játszma létrehozása
	var message = ''; //a szerver üzenete

	function initCreateButton() {
		$('div.create').html('<input id="create" type="button" class="button" value="Létrehoz" />');
	}

	function initCreateEvent() {
		$('input#create').click(function() {
			refreshUserInterface();
		});
		$("input#name, input#password").keypress(function (e) {
			if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
				refreshUserInterface();
			}
    	});
	}

	function refreshUserInterface() {
		getDatas();
		if (success) {
			disableInput();
		}
		setMessage(message);
	}

	function setMessage(msg) {
		$('div#message').html(msg);
	}

	function getValue(id) {
		return $('input#' + id).attr('value');
	}

	function getDatas() {
		$.ajax({
			type: "POST", 
			url: 'XmlRequest',
			data: "action=createGame&name=" + getValue('name') + "&password=" + getValue('password'),
			cache: false,
			async: false,
			dataType: "xml",
			success: function(response) {
				var resp = $(response).find('response');
				success = (resp.attr('success') == 'true');
				message = resp.attr('message');
			}
		});
	}

	function enableInput() {
		$('input#name, input#password, input#create').attr('disabled', null);
	}

	function disableInput() {
		$('input#name, input#password, input#create').attr('disabled', 'disabled');
	}

	$(document).ready(function() {
		initCreateButton();
		initCreateEvent();
		setMessage('Kérem, adja meg az adatokat.');
		enableInput();
	});

})();