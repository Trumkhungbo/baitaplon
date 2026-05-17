package action.SellingJobs;

import javafx.scene.control.CheckBox;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

public class ItemsHolder {
    private String itemId;
    private String itemname;
    private Double itemprice;
    private LocalDate itemdate;
    private LocalTime itemtime;
    private Time itemduration;
    private CheckBox checkbox;

    public ItemsHolder(String id, String name, Double price, LocalTime time, Time duration) {
        this.itemId = id;
        this.itemname = name;
        this.itemprice = price;
        this.itemtime = time;
        this.itemduration = duration;
        this.checkbox = new CheckBox();
    }

    public String getItemname() {
        return itemname;
    }
    public Double getItemprice(){
        return itemprice;
    }
    public LocalDate getItemdate(){
        return itemdate;
    }
    public LocalTime getItemtime(){
        return itemtime;
    }
    public Time getItemduration(){
        return itemduration;
    }
    public CheckBox getCheckBox(){
        return checkbox; // Lấy cái hộp tick ra để Admin bấm Confirm
    }
    public String getItemId() {
        return itemId;
    }
}