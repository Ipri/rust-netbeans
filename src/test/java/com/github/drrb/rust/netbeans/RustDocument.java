/*
 * Copyright (C) 2013 drrb
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.drrb.rust.netbeans;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Document for testing
 */
public class RustDocument extends PlainDocument {

    public RustDocument() {
        putProperty("mimeType", "text/x-rust-source");
    }
    
    public static RustDocument containing(CharSequence contents) {
        RustDocument document = new RustDocument();
        try {
            document.insertString(0, contents.toString(), null);
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
        return document;
    }   
}
