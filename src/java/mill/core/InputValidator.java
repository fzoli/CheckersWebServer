//ellenőrzi a beviteli mezőket illetve megmondja, hogy mi legyen a title az input mezőkhöz

package mill.core;

public class InputValidator {
    
    private final static int MIN_USER_LENGTH = 3;
    private final static int MAX_USER_LENGTH = 10;
    private final static int MIN_GAME_LENGTH = 4;
    private final static int MAX_GAME_LENGTH = 20;
    private final static int MIN_PASSWORD_LENGTH = 6;
    private final static int MAX_PASSWORD_LENGTH = 15;
    private final static String ACCENTUATED = "szóközt, a magyar";
    private final static String NOT_ACCENTUATED = "az angol";
    private final static String CONSTRAIN_BEGIN = " karakter hosszú. Csak ";
    private final static String CONSTRAIN_END = " ABC betűit és a számokat tartalmazhatja.";
    
    public static boolean isUserIdValid(String value) {
        return value.matches(createRule(MIN_USER_LENGTH, MAX_USER_LENGTH));
    }
    
    public static boolean isGameNameValid(String value) {
        return value.matches("^[a-zá-űA-ZÁ-Ű0-9]{1}[a-zá-űA-ZÁ-Ű 0-9]{" + (MIN_GAME_LENGTH - 1) + "," + (MAX_GAME_LENGTH - 1) + "}$");
    }
    
    public static boolean isPasswordValid(String value) {
        return value.matches(createRule(MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH));
    }
    
    private static String createRule(int min, int max) {
        return "^[a-zA-Z0-9]{" + min + "," + max + "}$";
    }
    
    public static boolean isGamePasswordValid(String value) {
        if (value.length() == 0) return true;
        else return isPasswordValid(value);
    }
    
    private static String createConstrain(boolean accentuated) {
        String s = accentuated ? ACCENTUATED : NOT_ACCENTUATED;
        return CONSTRAIN_BEGIN + s + CONSTRAIN_END;
    }
    
    public static String getUserIdConstrain() {
        return MIN_USER_LENGTH + "-" + MAX_USER_LENGTH + createConstrain(false);
    }

    public static String getGameNameConstrain() {
        return "Első karakter nem lehet szóköz. " + MIN_GAME_LENGTH + "-" + MAX_GAME_LENGTH + createConstrain(true);
    }
    
    public static String getPasswordConstrain() {
        return MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + createConstrain(false);
    }
    
    public static String getGamePasswordConstrain() {
        return "Kitöltése nem kötelező, de kitöltve " + getPasswordConstrain();
    }
    
}