package Model;

public class Badge {

    private String title;
    private String desc;
    private boolean unlocked;

    public Badge() {
        // Required empty constructor for Firebase
    }

    public Badge(String title, String desc, boolean unlocked) {
        this.title = title;
        this.desc = desc;
        this.unlocked = unlocked;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isUnlocked() {
        return unlocked;
    }
}