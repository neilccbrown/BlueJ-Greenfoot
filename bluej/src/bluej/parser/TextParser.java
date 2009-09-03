/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser;

import java.io.Reader;
import java.io.StringReader;
import java.util.Stack;

import antlr.TokenStreamException;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.entity.JavaEntity;

public class TextParser extends NewParser
{
    private Stack<JavaEntity> valueStack = new Stack<JavaEntity>();
    private Stack<LocatableToken> operatorStack = new Stack<LocatableToken>();
    
    public TextParser(Reader r)
    {
        super(r);
    }
    
    public TextParser(String s)
    {
        this(new StringReader(s));
    }
    
    public boolean atEnd()
    {
        try {
            return tokenStream.LA(1).getType() == JavaTokenTypes.EOF;
        } catch (TokenStreamException e) {
            return true;
        }
    }
    
    @Override
    protected void gotLiteral(LocatableToken token)
    {
        // TODO Auto-generated method stub
        super.gotLiteral(token);
    }
    
    @Override
    protected void gotBinaryOperator(LocatableToken token)
    {
        // TODO Auto-generated method stub
        super.gotBinaryOperator(token);
    }
}
