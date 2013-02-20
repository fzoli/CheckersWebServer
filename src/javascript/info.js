//a bejelentkező ablak, játszma regisztráció és játszma jelszó megadás oldalakhoz használt input mezők maszkolására
(function() {

	$(document).ready(function() {
		
		$(".input[title]").tooltip(); //amelyik input mezőn van title, ahhoz alkalmazom a tooltip-et
		
		jQuery(function($){
			$.mask.definitions['E']='[qQwWeErRtTzZuUiIoOpPaAsSdDfFgGhHjJkKlLyYxXcCvVbBnNmM0123456789]';
			$.mask.definitions['H']='[qQwWeErRtTzZuUiIoOpPaAsSdDfFgGhHjJkKlLyYxXcCvVbBnNmM0123456789 öÖüÜóÓőŐúÚéÉáÁűŰíÍ]';
			$("input#id").mask("EEE?EEEEEEE",{placeholder:""}); //felhasználónév
			$("input#password").mask("E?EEEEEEEEEEEEEE",{placeholder:""}); //jelszó
			$("input#name").mask("HHHH?HHHHHHHHHHHHHHHH",{placeholder:""}); //játszma neve
		});
		
	});

})();