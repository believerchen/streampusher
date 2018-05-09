package com.believer.mypublisher.coremedia.iso.boxes.apple;

import com.believer.mypublisher.googlecode.mp4parser.AbstractContainerBox;

/**
 * undocumented iTunes MetaData Box.
 */
public class AppleItemListBox extends AbstractContainerBox {
    public static final String TYPE = "ilst";

    public AppleItemListBox() {
        super(TYPE);
    }

}
