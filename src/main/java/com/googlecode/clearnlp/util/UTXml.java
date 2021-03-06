/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package com.googlecode.clearnlp.util;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class UTXml
{
	static public Element getFirstElementByTagName(Element element, String name)
	{
		NodeList list = element.getElementsByTagName(name);
		return list.getLength() > 0 ? (Element)list.item(0) : null;		
	}
	
	static public String getTrimmedAttribute(Element element, String name)
	{
		return element.getAttribute(name).trim();
	}
	
	static public String getTrimmedTextContent(Element element)
	{
		return element.getTextContent().trim();
	}
	
	static public Element getDocumentElement(InputStream fin)
	{
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		
		try
		{
			DocumentBuilder builder = dFactory.newDocumentBuilder();
			Document        doc     = builder.parse(fin);
			
			return doc.getDocumentElement();
		}
		catch (Exception e) {System.exit(1);}
		
		return null;
	}
	
	static public String startsElement(boolean isClosed, String element, String... attributes)
	{
		StringBuilder build = new StringBuilder();
		int i, size = attributes.length;
		String key, val;
		
		build.append("<");
		build.append(element);
		
		for (i=0; i<size; i+=2)
		{
			key = attributes[i];
			val = attributes[i+1];
			
			build.append(" ");
			build.append(key);
			build.append("=\"");
			build.append(val);
			build.append("\"");
		}
		
		if (isClosed)	build.append("/>");
		else			build.append(">");
		
		return build.toString();
	}
	
	static public String endsElement(String element)
	{
		StringBuilder build = new StringBuilder();
		
		build.append("</");
		build.append(element);
		build.append(">");
		
		return build.toString();
	}
	
	static public String getTemplate(String element, String contents, String indent, String... attributes)
	{
		StringBuilder build = new StringBuilder();
		
		build.append(indent);
		build.append(UTXml.startsElement(false, element, attributes));
		build.append("\n");
		
		build.append(contents);
		build.append("\n");
		
		build.append(indent);
		build.append(UTXml.endsElement(element));
		
		return build.toString();
	}
}
