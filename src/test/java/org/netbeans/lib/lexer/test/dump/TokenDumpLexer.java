/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.lexer.test.dump;

import org.netbeans.api.lexer.PartType;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;
import org.netbeans.spi.lexer.TokenPropertyProvider;
import org.netbeans.spi.lexer.TokenPropertyProvider;

/**
 * Simple implementation a lexer.
 *
 * @author mmetelka
 */
final class TokenDumpLexer implements Lexer<TokenDumpTokenId> {

    // Copy of LexerInput.EOF
    private static final int EOF = LexerInput.EOF;

    private LexerInput input;

    private TokenFactory<TokenDumpTokenId> tokenFactory;

    TokenDumpLexer(LexerRestartInfo<TokenDumpTokenId> info) {
        this.input = info.input();
        this.tokenFactory = info.tokenFactory();
    }

    public Object state() {
        return null;
    }

    public Token<TokenDumpTokenId> nextToken() {
        int c = input.read();
        switch (c) {
            case '\r':
                input.consumeNewline(); // continue to '\n' handling
            case '\n': // newline
                return token(TokenDumpTokenId.NEWLINE);

            case EOF:
                return null;

            case '.':
                switch (c = input.read()) {
                    case '\\': // ".\"
                        if ((c = input.read()) == '.') { // ".\."
                            switch (c = input.read()) {
                                case 'b':
                                    return finishCharLiteralOrText(TokenDumpTokenId.BACKSPACE_CHAR, '\b');
                                case 'f':
                                    return finishCharLiteralOrText(TokenDumpTokenId.FORM_FEED_CHAR, '\f');
                                case 't':
                                    return finishCharLiteralOrText(TokenDumpTokenId.TAB_CHAR, '\t');
                                case 'r':
                                    return finishCharLiteralOrText(TokenDumpTokenId.CR_CHAR, '\r');
                                case 'n':
                                    return finishCharLiteralOrText(TokenDumpTokenId.NEWLINE_CHAR, '\n');
                                case 'u':
                                    if ((c = input.read()) == '.')
                                        return finishUnicodeOrText();
                            }
                        }
                        input.backup(1);
                        return finishText();

                    case 'e': // ".e"
                        if ((c = input.read()) == '.'
                         && (c = input.read()) == 'o'
                         && (c = input.read()) == '.'
                         && (c = input.read()) == 'f'
                         && (c = input.read()) == '.'
                        )
                            return finishNewlineOrText(TokenDumpTokenId.EOF_VIRTUAL);
                        input.backup(1);
                        return finishText();

                    case 't': // ".t"
                        if ((c = input.read()) == '.'
                         && (c = input.read()) == 'e'
                         && (c = input.read()) == '.'
                         && (c = input.read()) == 's'
                         && (c = input.read()) == '.'
                         && (c = input.read()) == 't'
                         && (c = input.read()) == '.'
                        ) { // ".t.e.s.t."
                            return finishTillNewline(TokenDumpTokenId.TEST_NAME);
                        }
                        input.backup(1);
                        return finishText();

                    case EOF:
                        return token(TokenDumpTokenId.TEXT);
                }

            default:
                return finishText();
        }
    }

    private Token<TokenDumpTokenId> finishText() {
        return finishTillNewline(TokenDumpTokenId.TEXT);
    }

    private Token<TokenDumpTokenId> finishTillNewline(TokenDumpTokenId id) {
        while (true) {
            switch (input.read()) {
                case '\r':
                case '\n':
                case EOF:
                    input.backup(1);
                    return token(id);
            }
        }
    }

    private Token<TokenDumpTokenId> finishNewlineOrText(TokenDumpTokenId id) {
        // If newline follows then return the given id otherwise return text
        switch (input.read()) {
            case '\r':
            case '\n':
            case EOF: // EOF is also valid ending
                input.backup(1);
                return token(id);
        }
        return finishText();
    }

    private Token<TokenDumpTokenId> finishUnicodeOrText() {
        // If 4 unicode hex numbers followed by '.' return UNICODE_CHAR otherwise TEXT
        int c;
        int number = 0;
        int hexDigit = 0;
        for (int i = 4; i > 0; i--) { // read 4 unicode digits
            switch (c = input.read()) {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    hexDigit = c - '0';
                    break;
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                    hexDigit = c - 'a' + 10;
                    break;
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                    hexDigit = c - 'A' + 10;
                    break;
                default:
                    input.backup(1);
                    return finishText();

            }
            number = (number << 4) | hexDigit;
        }
        return finishCharLiteralOrText(TokenDumpTokenId.UNICODE_CHAR, (char)number);
    }

    private Token<TokenDumpTokenId> finishCharLiteralOrText(TokenDumpTokenId id, char ch) {
        int c;
        if ((c = input.read()) == '.') {
            switch (c = input.read()) {
                case '\r':
                case '\n':
                case EOF:
                    input.backup(1);
                    return tokenFactory.createPropertyToken(id, input.readLength(),
                        new UnicodeCharValueProvider<TokenDumpTokenId>(new Character(ch)), PartType.COMPLETE);
            }
        }
        input.backup(1);
        return finishText();
    }

    private Token<TokenDumpTokenId> token(TokenDumpTokenId id) {
        return tokenFactory.createToken(id);
    }

    public void release() {
    }

    private static final class UnicodeCharValueProvider<T extends TokenId> implements TokenPropertyProvider<T> {

        private Character ch;

        UnicodeCharValueProvider(Character ch) {
            this.ch = ch;
        }

        public Object getValue(Token<T> token, Object key) {
            if (TokenDumpTokenId.UNICODE_CHAR_TOKEN_PROPERTY.equals(key))
                return ch;
            return null; // no non-tokenStore value
        }

    }

}
