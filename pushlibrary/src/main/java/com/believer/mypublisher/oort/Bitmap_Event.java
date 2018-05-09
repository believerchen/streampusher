package com.believer.mypublisher.oort;

import android.graphics.Bitmap;


/**
 * Created by believer on 2018/4/9.
 */

public class Bitmap_Event{
    private Bitmap mesg;
    private int  type;//类型
    Bitmap_Event(int type, Bitmap mesg) {
        this.mesg = mesg;
        this.type = type;
    }

    public static Bitmap_Event I(int type,Bitmap mesg) {
        return new Bitmap_Event(type, mesg);
    }

    public int getType() {
        return type;
    }

    public Bitmap getMsg() {
        return mesg;
    }
}
