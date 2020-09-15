package edu.asu.cse535.team18;

public enum Gestures {
    SELECT_ONE(-1, R.string.select_one , ""),
    BUY(1, R.string.buy, "/6/6442.mp4"),
    HOUSE(2, R.string.house, "/23/23234.mp4"),
    FUN(3, R.string.fun, "/22/22976.mp4"),
    HOPE(4, R.string.hope, "/22/22197.mp4"),
    ARRIVE(5, R.string.arrive, "/26/26971.mp4"),
    REALLY(6, R.string.really, "24/24977.mp4"),
    READ(7, R.string.read, "/7/7042.mp4"),
    LIP(8, R.string.lip, "/26/26085.mp4"),
    MOUTH(9, R.string.mouth, "/22/22188.mp4"),
    SOME(10, R.string.some, "/23/23931.mp4"),
    COMMUNICATE(11, R.string.communicate, "/22/22897.mp4"),
    WRITE(12, R.string.write, "/27/27923.mp4"),
    CREATE(13, R.string.create, "/22/22337.mp4"),
    PRETEND(14, R.string.pretend, "/25/25901.mp4"),
    SISTER(15, R.string.sister, "/21/21587.mp4"),
    MAN(16, R.string.man, "/21/21568.mp4"),
    ONE(17, R.string.one, "/26/26492.mp4"),
    DRIVE(18, R.string.drive, "/23/23918.mp4"),
    PERFECT(19, R.string.perfect, "/24/24791.mp4"),
    MOTHER(20, R.string.mother, "/21/21571.mp4");

    final int id;
    final int displayResId;
    final String urlPath;

    Gestures(int id, int displayResId, String urlPath) {
        this.id = id;
        this.displayResId = displayResId;
        this.urlPath = urlPath;
    }

    public static Gestures getGestureById(final int inId) {
        for (Gestures gesture : Gestures.values()) {
            if (gesture.id == inId) {
                return gesture;
            }
        }
        return null;
    }
}
