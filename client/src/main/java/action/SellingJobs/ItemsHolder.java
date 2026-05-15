package action.SellingJobs;

import javafx.scene.control.CheckBox;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

public class ItemsHolder {
    String itemId;
    String itemname;
    String iteminfomation;
    String iteminformation1;
    String iteminformation2;
    Double itemprice;
    LocalDate itemdate;
    LocalTime itemtime;
    Time itemduration;
    CheckBox checkbox;
    public ItemsHolder(String id,String name,  Double price, LocalTime time, Time duration) {
        this.itemId = id;
        this.itemname = name;
        this.itemprice = price;
        this.itemtime = time;
        this.itemduration = duration;
        this.checkbox=new CheckBox();


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
    public CheckBox getCheckBox(){return checkbox;}
    public String getItemId() {
        return itemId;
    }
}
