package action.model;

import javafx.scene.control.CheckBox;

public class ItemsHolder {
    private final String itemId;
    private final String itemname;
    private final String itemdescription;
    private final String iteminformation1;
    private final String iteminformation2;
    private final String itemprice;
    private final String itemdate;
    private final String itemtime;
    private final String itemduration;
    private final CheckBox checkBox;

    public ItemsHolder(
            String itemId,
            String itemname,
            String itemdescription,
            String iteminformation1,
            String iteminformation2,
            String itemprice,
            String itemdate,
            String itemtime,
            String itemduration
    ) {
        this.itemId = itemId;
        this.itemname = itemname;
        this.itemdescription = itemdescription;
        this.iteminformation1 = iteminformation1;
        this.iteminformation2 = iteminformation2;
        this.itemprice = itemprice;
        this.itemdate = itemdate;
        this.itemtime = itemtime;
        this.itemduration = itemduration;
        this.checkBox = new CheckBox();
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemname() {
        return itemname;
    }

    public String getItemdescription() {
        return itemdescription;
    }

    public String getIteminformation1() {
        return iteminformation1;
    }

    public String getIteminformation2() {
        return iteminformation2;
    }

    public String getItemprice() {
        return itemprice;
    }

    public String getItemdate() {
        return itemdate;
    }

    public String getItemtime() {
        return itemtime;
    }

    public String getItemduration() {
        return itemduration;
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }
}
