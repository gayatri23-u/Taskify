package Model;

public class TaskModel {
    public String title;
    public String desc;
    public String priority;
    public boolean done;
    public String date;

    public TaskModel() {} // Firebase needs empty constructor

    public TaskModel(String title, String desc, String priority, boolean done, String date) {
        this.title = title;
        this.desc = desc;
        this.priority = priority;
        this.done = done;
        this.date = date;
    }
}