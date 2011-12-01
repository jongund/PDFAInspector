import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.SimpleBookmark;
import com.itextpdf.text.pdf.parser.*;
import com.itextpdf.text.xml.simpleparser.SimpleXMLParser;

/**
 * PdfExtractor
 * Extracts information and saves it to a separate xml file, determined by the filename parameter
 * Currently extracts: tags, form information, metadata, and bookmarks
 */
public class PdfExtractor {
	private String filename;
	private PdfReader reader;
	private PrintWriter out; //output xml file with all information
	private PrintWriter outline; //simplified xml file with only outline info
	private PdfName summaryName = new PdfName("Summary");
	private PdfName scopeName = new PdfName("Scope");
	private PdfName rowName = new PdfName("RowSpan");
	private PdfName colName = new PdfName("ColSpan");
	private PdfName headerName = new PdfName("Headers");
	
	private PdfArray pages = null;
	private int pageCount = 1;
	
	public PdfExtractor(String filename){
		this.filename = filename;
		try {
			reader = new PdfReader(this.filename);
			pages = reader.getCatalog().getAsDict(PdfName.PAGES).getAsArray(PdfName.KIDS);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    /**
     * Extract tags and save as XML
     * @return tags as an xml File
     * @throws IOException 
     */
    public File extractTags(String result, String result_ol) throws IOException{
		try {
			//TaggedPdfReaderTool tReader = new TaggedPdfReaderTool();
			FileOutputStream fop, fop_ol;
			File file = new File(result);
			File file_ol = new File(result_ol);
			
			fop = new FileOutputStream(file);
			fop_ol = new FileOutputStream(file_ol);
			
			try{
				convertToXmlWithAttr(fop, fop_ol);
			}catch(NullPointerException e){
			}
			//tReader.convertToXml(reader, fop);
			
		    fop.flush();
		    fop.close();
		    
		    return file;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
       

    }
    
    /**
     * Extract bookmarks and save as XML
     * @return bookmark information as a xml file
     */
    public File extractBookmarks(String result){
		try {
	        List<HashMap<String,Object>> list = SimpleBookmark.getBookmark(reader);
	        if (list == null){
	        	return null;
	        }
	        else{
	        	File file = new File(result);
				FileOutputStream fop = new FileOutputStream(file);
	        	SimpleBookmark.exportToXML(list, fop, "ISO8859-1", true);
	        	fop.flush();
	        	fop.close();
	        	return file;
	        }
	        
	        
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

    }
    
    /**
     * extract metadata
     * @return metadata information as xml file
     */
    public File extractMeta(String filename){
    	try{
    		File file = new File(filename);
    		file.setReadable(true, false);
    		PrintWriter writer = new PrintWriter(new FileOutputStream(file));
    	
        	//extract metadata
        	HashMap meta = reader.getInfo();
        	String title = (String)meta.get("Title");
        	String author = (String)meta.get("Author");
        	String creator = (String)meta.get("Creator");
        
        	//extract total page number
        	int pages = reader.getNumberOfPages();
        	
        	writer.println("<title>");
        	writer.println(title);
        	writer.println("</title>");
        	
        	writer.println("<author>");
        	writer.println(author);
        	writer.println("</author>");
        	
        	writer.println("<creator>");
        	writer.println(creator);
        	writer.println("</creator>");
        	
        	writer.println("<pageNum>");
        	writer.println(pages);
        	writer.println("</pageNum>");
        	
        	writer.flush();
        	writer.close();
        	
        	return file;
    	}
    	catch(Exception e){
    		e.printStackTrace();
    		return null;
    	}
    }
    
    /**
     * Extract Permissions
     * Currently unused; returns a boolean whether it is opened w/ full permissions
	 * @return isOpenedWithFullPermissions
     */
    public boolean extractPermissions(){
        return reader.isOpenedWithFullPermissions();
    }
    
    /**
     * Extract Form Information and writes into an xml file determined by the param result
     * @param result : output xml file
     * @return form information as a xml file
     */
    public File extractForm(String result){
        PrintWriter writer;
		try {
			File file = new File(result);
			writer = new PrintWriter(new FileOutputStream(file));
	    	AcroFields form = reader.getAcroFields();
	        
	    	writer.println("<form>");
	    	// Loop over the fields and get field names
	    	
	        Set<String> fields = form.getFields().keySet();
	        String tag;
	        // determines what form field it is
	        for (String key : fields){
	            switch (form.getFieldType(key)) {
	            case AcroFields.FIELD_TYPE_CHECKBOX:
	            	tag = "Checkbox";
	                break;
	            case AcroFields.FIELD_TYPE_COMBO:
	            	tag = "Combobox";
	                break;
	            case AcroFields.FIELD_TYPE_LIST:
	            	tag = "List";
	                break;
	            case AcroFields.FIELD_TYPE_NONE:
	            	tag = "None";
	                break;
	            case AcroFields.FIELD_TYPE_PUSHBUTTON:
	            	tag = "Pushbutton";
	                break;
	            case AcroFields.FIELD_TYPE_RADIOBUTTON:
	            	tag = "Radiobutton";
	                break;
	            case AcroFields.FIELD_TYPE_SIGNATURE:
	            	tag = "Signature";
	                break;
	            case AcroFields.FIELD_TYPE_TEXT:
	            	tag = "Text";
	                break;
	            default:
	                //if unknown
	            	tag = "unknown";
	            }
	            writer.println("<" + tag + ">");
	            writer.println('\t' + "<Name>" + key + "</Name>");
	            
	            int numWidgets = form.getFieldItem(key).size();
	            for (int j = 0; j < numWidgets; j++){
	            	PdfDictionary widget = form.getFieldItem(key).getWidget(j);
	            	if (widget.get(PdfName.TU) != null)
	            		writer.println('\t' + "<Tooltip>" + widget.get(PdfName.TU) + "</Tooltip>");
	            }
	            
	            writer.println("</" +tag+ ">");
	        }
	        writer.println("</form>");
	        writer.flush();
	        writer.close();
	        
	        return file;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

    }
    
    /**
     * Extracts tags with alt text attributes
     * @param result
     * @return
     */
    public void convertToXmlWithAttr(OutputStream os, OutputStream ol_os) throws IOException{
    	
    	OutputStreamWriter outs = new OutputStreamWriter(os, Charset.defaultCharset().name());
    	OutputStreamWriter outl = new OutputStreamWriter(ol_os, Charset.defaultCharset().name());
    	
    	out = new PrintWriter(outs);
    	outline = new PrintWriter(outl);
    	
    	
    	// get the StructTreeRoot from the root object
		PdfDictionary catalog = reader.getCatalog();
		PdfDictionary struct = catalog.getAsDict(PdfName.STRUCTTREEROOT);

		inspectStructChild(struct.getDirectObject(PdfName.K));

		out.flush();
		out.close();
		
		outline.flush();
		outline.close();
    }
    
    /**
     * Inspect children of structure tree root
     * @param k\
     * @param target
     * @return
     */
    private void inspectStructChild(PdfObject k) throws IOException {
		if (k == null)
			return;
		else if (k instanceof PdfArray)
			inspectChildArray((PdfArray) k);
		else if (k instanceof PdfDictionary)
			inspectChildDictionary((PdfDictionary) k);
    }
    
    /**
     * Inspect child array of a structure tree 
     * @param k
     * @param target
     * @return
     */
    private void inspectChildArray(PdfArray k) throws IOException {
		if (k == null)
			return;
		for (int i = 0; i < k.size(); i++) {
			inspectStructChild(k.getDirectObject(i));
		}
    }
    
    /**
     * 
     * @param dict
     * @param target
     * @return
     */
    private void inspectChildDictionary(PdfDictionary dict) throws IOException {
    	if (dict == null)
			return;
		else{	
			// if tag
			PdfName s = dict.getAsName(PdfName.S);
			if (s != null) {
				// Check to see if we have gone to a new page
				PdfDictionary temp = dict.getAsDict(PdfName.PG);
				if(temp != null){
					PdfArray pages = temp.getAsDict(PdfName.PARENT).getAsArray(PdfName.KIDS);
					if(temp != pages.getAsDict(pageCount - 1)){
						if(temp == pages.getAsDict(pageCount)){
							pageCount++;
						}
						else{
							for(int i = 0; i < pages.length(); i ++){
								if(temp == pages.getAsDict(i)){
									pageCount = i + 1;
									break;
								}
							}
						}
					}
				}
				
	            String tagN = PdfName.decodeName(s.toString());
				String tag = fixTagName(tagN);
				out.print("<");
				out.print(tag);
				
				outline.print("<");
				outline.print(tag);
				
				// if we know what page we're on, include it as an attribute
				//if(currentPage != null){
				if(temp != null){
					out.println(" Page=\"" + pageCount + "\"");
				}
				
				// if alt text exists, include in tag brackets
				if (dict.get(PdfName.ALT) != null){
					String alt = dict.getAsString(PdfName.ALT).toString();
					
					// alt text will contain null characters. we don't want these. remove them.
					String altText = "";
					for(int i = 0; i < alt.length(); i++){
						char c = alt.charAt(i);
						if(c != '\0'){
							altText = altText + c;
						}
					}					

					out.print(" Alt=\"" + altText + "\"");
				}
				
				// if id exists, include in tag brackets
				if(dict.get(PdfName.ID) != null){
					String id = dict.get(PdfName.ID).toString();
					out.print(" ID=\"" + id + "\"");
				}
				
				// if attribute exists, include in tag brackets
				PdfDictionary a = dict.getAsDict(PdfName.A);
				if (a != null) {
					extractAttr(a);
				}				
				
				out.print(">");
				outline.println(">");

				PdfDictionary dictPG = dict.getAsDict(PdfName.PG);
				if (dictPG != null)
					parseTag(tagN, dict.getDirectObject(PdfName.K), dictPG);
				inspectStructChild(dict.getDirectObject(PdfName.K));

				out.print("</");
				out.print(tag);
				out.println(">");

				outline.print("</");
				outline.print(tag);
				outline.println(">");
			}
			
			// if attribute dictionary
			else if (dict.get(PdfName.A)!= null) {
				//do nothing as of current desired features
			}
			else {
				inspectStructChild(dict.get(PdfName.K));
			}
			
			
		}
    }
    
    /**
     * Taken from TaggedPdfReaderTool in iText
     * @param tag
     * @return
     */
    private static String fixTagName(String tag) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < tag.length(); ++k) {
            char c = tag.charAt(k);
            boolean nameStart =
                c == ':'
                || (c >= 'A' && c <= 'Z')
                || c == '_'
                || (c >= 'a' && c <= 'z')
                || (c >= '\u00c0' && c <= '\u00d6')
                || (c >= '\u00d8' && c <= '\u00f6')
                || (c >= '\u00f8' && c <= '\u02ff')
                || (c >= '\u0370' && c <= '\u037d')
                || (c >= '\u037f' && c <= '\u1fff')
                || (c >= '\u200c' && c <= '\u200d')
                || (c >= '\u2070' && c <= '\u218f')
                || (c >= '\u2c00' && c <= '\u2fef')
                || (c >= '\u3001' && c <= '\ud7ff')
                || (c >= '\uf900' && c <= '\ufdcf')
                || (c >= '\ufdf0' && c <= '\ufffd');
            boolean nameMiddle =
                c == '-'
                || c == '.'
                || (c >= '0' && c <= '9')
                || c == '\u00b7'
                || (c >= '\u0300' && c <= '\u036f')
                || (c >= '\u203f' && c <= '\u2040')
                || nameStart;
            if (k == 0) {
                if (!nameStart)
                    c = '_';
            }
            else {
                if (!nameMiddle)
                    c = '-';
            }
            sb.append(c);
        }
        return sb.toString();
    }

	/**
	 * Taken from TaggedPdfReaderTool in iText: Searches for a tag in a page.
	 * 
	 * @param tag
	 *            the name of the tag
	 * @param object
	 *            an identifier to find the marked content
	 * @param page
	 *            a page dictionary
	 * @throws IOException
	 */
	public void parseTag(String tag, PdfObject object, PdfDictionary page) throws IOException {
		// if the identifier is a number, we can extract the content right away
		if (object instanceof PdfNumber) {
			PdfNumber mcid = (PdfNumber) object;
			RenderFilter filter = new MarkedContentRenderFilter(mcid.intValue());
			TextExtractionStrategy strategy = new SimpleTextExtractionStrategy();
			FilteredTextRenderListener listener = new FilteredTextRenderListener(
					strategy, filter);
			PdfContentStreamProcessor processor = new PdfContentStreamProcessor(listener);
			processor.processContent(PdfReader.getPageContent(page), page.getAsDict(PdfName.RESOURCES));
			out.print(SimpleXMLParser.escapeXML(listener.getResultantText(), true));
		}
		// if the identifier is an array, we call the parseTag method
		// recursively
		else if (object instanceof PdfArray) {
			PdfArray arr = (PdfArray) object;
			int n = arr.size();
			for (int i = 0; i < n; i++) {
				parseTag(tag, arr.getPdfObject(i), page);
				if (i < n - 1)
					out.println();
			}
		}
		// if the identifier is a dictionary, we get the resources from the
		// dictionary
		else if (object instanceof PdfDictionary) {
			PdfDictionary mcr = (PdfDictionary) object;
			parseTag(tag, mcr.getDirectObject(PdfName.MCID), mcr.getAsDict(PdfName.PG));
		}
	}

	/**
	 * Extracts attributes and prints them to output stream
	 * @param a
	 */
    public void extractAttr(PdfDictionary a){
    	if (a == null){
    		return;
    	}
    	else{
    		//System.out.println(PdfContentReaderTool.getDictionaryDetail(a));
    		
    		if (a.contains(summaryName)){
    			out.print(" Summary=\"" + a.get(summaryName) + "\"");
    		}
    		if (a.contains(scopeName)){
    			out.print(" Scope=\"" + a.get(scopeName) + "\"");
    		}
    		if (a.contains(headerName)){
    			out.print(" Headers=\"" + a.get(headerName) + "\"");
    		}
    		if (a.contains(rowName)){
    			out.print(" RowSpan=\"" + a.get(rowName) + "\"");
    		}
    		if (a.contains(colName)){
    			out.print(" ColSpan=\"" + a.get(colName) + "\"");
    		}
    		
    	}
    }

    public void extractText(String txt) throws IOException{
    	PdfReaderContentParser parser = new PdfReaderContentParser(reader);
    	PrintWriter out = new PrintWriter(new FileOutputStream(txt));
    	TextExtractionStrategy strategy;
    	for (int i = 1; i <= reader.getNumberOfPages(); i++) {
    	strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
    	out.println(strategy.getResultantText());
    	}
    	out.flush();
    	out.close();
    	/*PdfReaderContentParser parser = new PdfReaderContentParser(reader);
    	PrintWriter out = new PrintWriter(new FileOutputStream(txt));
    	TextExtractionStrategy strategy;
    	for (int i = 1; i <= reader.getNumberOfPages(); i++) {
    	strategy
    	= parser.processContent(i, new LocationTextExtractionStrategy());
    	out.println(strategy.getResultantText());
    	}
    	out.flush();
    	out.close();*/

    	
    }
    
}
