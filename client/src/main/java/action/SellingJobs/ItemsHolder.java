package action.SellingJobs;

import javafx.scene.control.CheckBox;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

public class ItemsHolder {
    String itemname;
    String iteminfomation;
    Double itemprice;
    LocalDate itemdate;
    LocalTime itemtime;
    Time itemduration;
    CheckBox checkbox;
    public ItemsHolder(String name, String infomation,  Double price, LocalDate date, LocalTime time, Time duration) {
        this.itemname = name;
        this.iteminfomation = infomation;
        this.itemprice = price;
        this.itemdate = date;
        this.itemtime = time;
        this.itemduration = duration;
        this.checkbox=new CheckBox();
    }
    public String getItemname() {
        return itemname;
    }
    public String getIteminfomation(){
        return iteminfomation;
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

}
