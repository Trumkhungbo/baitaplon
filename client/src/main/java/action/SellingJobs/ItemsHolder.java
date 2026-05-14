package action.SellingJobs;

import javafx.scene.control.CheckBox;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

public class ItemsHolder {
    String itemname;
    String iteminfomation;
    String iteminformation1;
    String iteminformation2;
    Double itemprice;
    LocalDate itemdate;
    LocalTime itemtime;
    Time itemduration;
    CheckBox checkbox;
    public ItemsHolder(String name, String infomation, String information1,String information2,  Double price, LocalDate date, LocalTime time, Time duration) {
        this.itemname = name;
        this.iteminfomation = infomation;
        this.itemprice = price;
        this.itemdate = date;
        this.itemtime = time;
        this.itemduration = duration;
        this.checkbox=new CheckBox();
        this.iteminformation1 = information1;
        this.iteminformation2 = information2;

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
    public String getIteminformation1(){
        return iteminformation1;
    }
    public String getIteminformation2(){
        return iteminformation2;
    }

}
