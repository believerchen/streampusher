/*  
 * Copyright 2008 CoreMedia AG, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an AS IS BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

package com.believer.mypublisher.coremedia.iso.boxes;


import com.believer.mypublisher.coremedia.iso.IsoTypeReader;
import com.believer.mypublisher.coremedia.iso.IsoTypeWriter;
import com.believer.mypublisher.coremedia.iso.Utf8;
import com.believer.mypublisher.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * The copyright box contains a copyright declaration which applies to the entire presentation, when contained
 * within the MovieBox, or, when contained in a track, to that entire track. There may be multple boxes using
 * different language codes.
 *
 * @see MovieBox
 * @see TrackBox
 */
public class CopyrightBox extends AbstractFullBox {
    public static final String TYPE = "cprt";

    private String language;
    private String copyright;

    public CopyrightBox() {
        super(TYPE);
    }

    public String getLanguage() {
        return language;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    protected long getContentSize() {
        return 7 + Utf8.utf8StringLengthInBytes(copyright);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        language = IsoTypeReader.readIso639(content);
        copyright = IsoTypeReader.readString(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, language);
        byteBuffer.put(Utf8.convert(copyright));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        return "CopyrightBox[language=" + getLanguage() + ";copyright=" + getCopyright() + "]";
    }



}
