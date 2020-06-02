package org.envirocar.core.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "manufacturers",
primaryKeys = "id")
public class Manufacturers {
    @NonNull
    @ColumnInfo(name = "id")
    private java.lang.String id;

    @ColumnInfo(name = "name")
    private java.lang.String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
