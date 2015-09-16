/*
 * 
 * Barcode generator for Reformational Study Centre
 * Written by: Rudolf Byker
 * 
 * Version 0, 2011
 * Version 1.0, 2013
 * 
 * Known bugs:
 * - Progress Monitor does not close on cancel. Process stops, but ProgresMonitor must be "cancelled twice".
 * 
 */

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

import org.krysalis.barcode4j.HumanReadablePlacement;
import org.krysalis.barcode4j.impl.code39.Code39Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class RSCBarcoder
{
	String ver = "v1";
	String title = "Reformational Study Centre - CODE-39 barcode generator "+ver;
	static String errorLogFile = "errors.log";
	static int padding = 5;
	static int fieldcols = 10;
	
	JFrame window = new JFrame(title);
	JTabbedPane jtbInput;
	BarcodesPanel barcodesPanel = new BarcodesPanel(window, fieldcols, padding);
	TextPanel textPanel = new TextPanel(window, padding, fieldcols);
	PDFPanel pdfPanel = new PDFPanel(fieldcols);
	JPanel pngPanel = new JPanel();
	
	ActionListener alActions = new ActionListener()
	{	
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			String ac = ae.getActionCommand();
			if (ac.equals("Quit"))
			{
				System.exit(0);
			}
			else if (ac.equals("Generate..."))
			{
				switch(jtbInput.getSelectedIndex())
				{
				case 0: // barcodes
					performGenerateBarcodes();
					break;
				case 1: // plain text
					composePlaintextPDF(new PDFSettings(pdfPanel.getAll()), new TextSettings(textPanel.getSettings()), textPanel.getTxts(), textPanel);
					break;
				}
			}
		}
	};
	
	RSCBarcoder()
	{
		makeWindow();
	} // END: RSCBarcoder
	
	private void makeWindow()
	{
		window.setLayout(new BorderLayout(padding, padding));
		JPanel sideBySide = new JPanel();
		sideBySide.setLayout(new BoxLayout(sideBySide, BoxLayout.X_AXIS));
		window.add(sideBySide, BorderLayout.CENTER);
		
		jtbInput = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		jtbInput.addTab("Barcodes", barcodesPanel);
		jtbInput.addTab("Plain text", textPanel);
		jtbInput.setBorder(new TitledBorder(null,"Input:",TitledBorder.CENTER,TitledBorder.TOP));
		JScrollPane jspInput = new JScrollPane(jtbInput);
		sideBySide.add(jspInput);

		pngPanel.add(new JLabel("No settings yet."));
		
		JTabbedPane jtbOutput = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		jtbOutput.addTab("PDF document", pdfPanel);
		jtbOutput.addTab("PNG images", pngPanel);
		jtbOutput.setBorder(new TitledBorder(null,"Output:",TitledBorder.CENTER,TitledBorder.TOP));
		JScrollPane jspOutput = new JScrollPane(jtbOutput);
		sideBySide.add(jspOutput);
		
		ButtonPanel actions = new ButtonPanel(alActions, new String[]{"Quit","Generate..."}, new String[]{"Leave the program.","Start the job after confirming settings."});
		window.add(actions,BorderLayout.SOUTH);
		
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.pack();
		window.setLocationRelativeTo(null);
		window.setVisible(true);
	}
	
	boolean composePlaintextPDF(PDFSettings pdfSettings, TextSettings txtSettings, String[] txt, JPanel gui)
	{
		boolean success = false;
		if (null != txt && txt.length > 0)
		{
			String filename = JOptionPane.showInputDialog(window,"Enter a filename for the new pdf:\n(omit the '.pdf' extention)");
			if (null != filename && filename.trim().length() > 0)
			{
				final ProgressMonitor mon = new ProgressMonitor(gui,"Generating plaintext pdf...","",0,txt.length);
				mon.setProgress(0);
				final PlaintextPDFComposer pdf = new PlaintextPDFComposer(pdfSettings,txtSettings,txt,mon,filename.trim());
				pdf.execute();
				
				try
				{
					success = pdf.get();
				}
				catch (CancellationException e)
				{
					// user cancelled
					success = false;
				}
				catch(InterruptedException e)
				{
					error("Could not generate pdf document.\n"+e.getMessage(),e);
					success = false;
				}
				catch (ExecutionException e)
				{
					error("Could not generate pdf document.\n"+e.getMessage(),e);
					success = false;
				}
			}
		}
		else
		{
			error("Nothing to generate.\nDid you open a file?");
			success = false;
		}
		
		return success;
	}
	
	class PlaintextPDFComposer extends SwingWorker<Boolean, Integer>
	{
		private ProgressMonitor mon;
		private PDFSettings pdfSet;
		private TextSettings txtSet;
		private String[] txts;
		private String filename;
		
		PlaintextPDFComposer(PDFSettings pdfSettings, TextSettings txtSettings, String[] txt, ProgressMonitor mon, String filename)
		{
			this.mon = mon;
			this.pdfSet = pdfSettings;
			this.txtSet = txtSettings;
			this.txts = txt;
			this.filename = filename;
		}
		
        public Boolean doInBackground()
        {
        	boolean success = true;
        	int progress = 0;
        	publish(progress);
            
            // in iText, each row needs to be filled in a table!
    		if (txts.length % pdfSet.cols != 0)
    		{
    			String[] new_msgs = new String[((txts.length/pdfSet.cols)+1)*pdfSet.cols];
    			for (int i=0;i<txts.length;i++)
    			{
    				new_msgs[i] = txts[i];
    			}
    			for (int i=txts.length;i<new_msgs.length;i++)
    			{
    				new_msgs[i] = "";
    			}
    			txts = new_msgs;
    		}
    		
    		Document document = null;
    		try
    		{
	    		document = new Document(new Rectangle(mmTpt(pdfSet.dim_pw),mmTpt(pdfSet.dim_ph)),mmTpt(pdfSet.mar_pl),0f,mmTpt(pdfSet.mar_pt),0f);
    			new File("docs").mkdirs(); // ensure that docs directory exists
    			/*PdfWriter writer =*/ PdfWriter.getInstance(document, new FileOutputStream("docs"+File.separatorChar+filename+".pdf"));
    			document.open();
    			
    			Font descriptionFont = FontFactory.getFont(pdfSet.font, pdfSet.fsize);
    			Font txtFont = FontFactory.getFont(txtSet.font, txtSet.fsize);
    			
    			PdfPTable table = new PdfPTable(pdfSet.cols);
    			table.setTotalWidth(mmTpt((pdfSet.dim_lw + pdfSet.colspace)*pdfSet.cols));	// tablewidth = (labelwidth + space between cols) * columns
    			table.setLockedWidth(true);
    			table.setHorizontalAlignment(Element.ALIGN_LEFT);
    			for (String txt : txts)
    			{
    				publish(++progress);
					if (isCancelled())
	    			{
	    				return false;
	    			}
    				
    				if (txt.equals(""))
    				{
    					// empty cell
    					PdfPCell c = new PdfPCell();
    					c.setHorizontalAlignment(Element.ALIGN_CENTER);
    					c.setFixedHeight(mmTpt(pdfSet.dim_lh + pdfSet.rowspace));		// label height + space between rows
    					c.setPaddingLeft(mmTpt(pdfSet.mar_ll));
    					c.setPaddingRight(mmTpt(pdfSet.mar_lr + pdfSet.colspace));
    					c.setPaddingTop(mmTpt(pdfSet.mar_lt));
    					c.setPaddingBottom(mmTpt(pdfSet.mar_lb + pdfSet.rowspace));	// bottom padding + space between rows
    					c.setBorderWidth(0f);
    					table.addCell(c);
    				}
    				else
    				{
    					try
    					{
    					if (txtSet.mcpl > 0)
    					{
    						char[] t0 = txt.toCharArray();
    						char[] t1 = new char[(t0.length/txtSet.mcpl) + t0.length];
    						for (int i=0,j=0;i<t0.length;i++,j++)
    						{
    							if (i!=0 && i%txtSet.mcpl == 0)
    							{
    								t1[j] = '\n';
    								j++;
    							}
   								t1[j] = t0[i];
    						}
    						txt = new String(t1);
    					}
    					}
    					catch(Exception e)
    					{
    						e.printStackTrace();
    					}
    					
    					PdfPTable innerTable = new PdfPTable(1);
    					innerTable.setWidthPercentage(100);
    					PdfPCell innerTxtCell = new PdfPCell(new Phrase(txt, txtFont));
    					innerTxtCell.setBorderWidth(0f);
    					innerTxtCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    					
    					if (pdfSet.pos.equals("Above barcode"))
    					{
    						PdfPCell innerDescrCell = new PdfPCell(new Phrase(pdfSet.txt,descriptionFont));
    						innerDescrCell.setBorderWidth(0f);
    						innerDescrCell.setFixedHeight(pdfSet.fsize*1.5f); // TODO: why *1.5f ??
    						innerTxtCell.setFixedHeight(mmTpt(pdfSet.dim_lh - (pdfSet.mar_lt + pdfSet.mar_lb)) - (pdfSet.fsize*1.5f)); //remaining space = labelheight - (bottompadding + toppadding + txtsize)
    						innerDescrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    						innerDescrCell.setPadding(0);
    						innerTable.addCell(innerDescrCell);
    						innerTable.addCell(innerTxtCell);
    					}
    					else if (pdfSet.pos.equals("Below barcode"))
    					{
    						PdfPCell innerDescrCell = new PdfPCell(new Phrase(pdfSet.txt,descriptionFont));
    						innerDescrCell.setBorderWidth(0f);
    						innerDescrCell.setFixedHeight(pdfSet.fsize*1.5f);
    						innerTxtCell.setFixedHeight(mmTpt(pdfSet.dim_lh - (pdfSet.mar_lt + pdfSet.mar_lb)) - (pdfSet.fsize*1.5f)); //remaining space = labelheight - (bottompadding + toppadding + txtsize)
    						innerDescrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    						innerDescrCell.setPadding(0);
    						innerTable.addCell(innerTxtCell);
    						innerTable.addCell(innerDescrCell);
    					}
    					else // None
    					{
    						innerTable.addCell(innerTxtCell);
    					}
    					
    					PdfPCell c = new PdfPCell();
    					c.addElement(innerTable);
    					c.setHorizontalAlignment(Element.ALIGN_CENTER);
    					c.setFixedHeight(mmTpt(pdfSet.dim_lh + pdfSet.rowspace));		// label height + space between rows
    					c.setPaddingLeft(mmTpt(pdfSet.mar_ll));
    					c.setPaddingRight(mmTpt(pdfSet.mar_lr + pdfSet.colspace));
    					c.setPaddingTop(mmTpt(pdfSet.mar_lt));
    					c.setPaddingBottom(mmTpt(pdfSet.mar_lb + pdfSet.rowspace));	// bottom padding + space between rows
    					c.setBorderWidth(0f);
    					table.addCell(c);
    				}
    			}
    			document.add(table);
    		}
    		catch(DocumentException e)
    		{
    			error("Error while creating pdf content.\n"+e.getMessage(),e);
    			success = false;
    		}
    		catch(FileNotFoundException e)
    		{
    			error("Missing file!\n"+e.getMessage(),e);
    			success = false;
    		}
    		finally
    		{
    			if (null != document)
    			{
    				document.close();
    			}
    		}
            
    		if (isCancelled())
			{
				return false;
			}
    		
            return success;
        }

        @Override
		protected void process(List<Integer> chunks)
        {
			super.process(chunks);
			if (mon.isCanceled())
			{
				mon.close();
				this.cancel(true);
			}
			else
			{
				mon.setProgress(chunks.get(chunks.size()-1));
			}
		}

		@Override
		public void done()
		{
			super.done();
			mon.close();
			
			try
			{
				if (get())
				{
					JOptionPane.showMessageDialog(null, "Images and PDF are now created.\nLook in 'images' and 'docs' folders in the program directory.");
				}
			}
			catch (HeadlessException e)
			{
				error("Error while creating Images or PDF\n"+e.getMessage(),e);
			}
			catch (InterruptedException e)
			{
				error("Error while creating Images or PDF\n"+e.getMessage(),e);
			}
			catch (ExecutionException e)
			{
				error("Error while creating Images or PDF\n"+e.getMessage(),e);
			}
		}
    }

	void performGenerateBarcodes()
	{
		BarcodeTaskPerformer task = new BarcodeTaskPerformer(barcodesPanel, new PDFSettings(pdfPanel.getAll()), pdfPanel);
		task.execute();
	}
	
	class BarcodeTaskPerformer extends SwingWorker<Boolean,Void>
	{
		BarcodesPanel bcPanel;
		JPanel pdfPanel;
		PDFSettings pdfSet;
		
		BarcodeTaskPerformer(BarcodesPanel bcPanel, PDFSettings pdfSet, JPanel pdfPanel)
		{
			this.bcPanel=bcPanel;
			this.pdfPanel=pdfPanel;
			this.pdfSet=pdfSet;
		}

		@Override
		protected Boolean doInBackground()
		{
			String[] barcodes = bcPanel.getBarcodes();
			
			if (null == barcodes)
			{
			}
			else if (barcodes.length < 1)
			{
				error("Nothing to generate!\nDid you add fields?");
			}
			else if (confirmBarcodes(barcodes))
			{
				// get settings (should already be safe for parsing)
				String[] barcodeSettings = bcPanel.getSettings();
				/* 
				 * 0 "Font",
				 * 1 "Font size",
				 * 2 "DPI",
				 * 3 "Bar height",
				 * 4 "Full height",
				 * 5 "Module width",
				 * 6 "Text placement",
				 * 7 "Text pattern",
				 * 8 "'Quiet zone' width",
				 * 9 "'Quiet zone' height"
				 */

				String fontname = barcodeSettings[0];
				double fontsize = Double.parseDouble(barcodeSettings[1]);
				int dpi = Integer.parseInt(barcodeSettings[2]);
				double barheight = Double.parseDouble(barcodeSettings[3]);
				double height = Double.parseDouble(barcodeSettings[4]);
				double modulewidth = Double.parseDouble(barcodeSettings[5]);
				String pattern = barcodeSettings[7];
				double quietzonewidth = Double.parseDouble(barcodeSettings[8]);
				double quietzoneheight = Double.parseDouble(barcodeSettings[9]);
				
				HumanReadablePlacement placement;
				switch (barcodeSettings[6])
				{
				case "Bottom":
					placement = HumanReadablePlacement.HRP_BOTTOM;
					break;
				case "Top":
					placement = HumanReadablePlacement.HRP_TOP;
					break;
				default:
					placement = HumanReadablePlacement.HRP_NONE;
					break;
				}
				
				Code39Bean bean = new Code39Bean();
				bean.setFontName(fontname);					// Sets the font name of the human-readable part.
				bean.setFontSize(fontsize);					// Sets the font size of the human-readable part.
				bean.setBarHeight(barheight);				// Sets the height of the bars.
				bean.setHeight(height);						// Sets the full height of the barcode.
				bean.setModuleWidth(modulewidth);			// Sets the width of the narrow module.
				bean.setMsgPosition(placement);				// Sets the placement of the human-readable part.
				bean.setPattern(pattern);					// Sets the pattern to be applied over the human readable message
				bean.setQuietZone(quietzonewidth);			// Sets the width of the quiet zone.
				bean.setVerticalQuietZone(quietzoneheight);	// Sets the height of the vertical quiet zone.

				if (generateIMGs(barcodes, bean, dpi, bcPanel))
				{
					// FIXME: how can we move this one to the EDT?? hmmm...
					String filename = JOptionPane.showInputDialog(null,"Enter a filename for the new pdf:\n(omit the '.pdf' extention)");
					if (null != filename && filename.trim().length() > 0)
					{
						if (composeBarcodePDF(pdfSet, barcodes, pdfPanel, filename.trim()))
						{
							return true;
						}
					}
				}
			}
			
			return false;
		}
				
		@Override
		public void done()
		{
			super.done();
			try
			{
				if (get())
				{
					JOptionPane.showMessageDialog(null, "Images and PDF are now created.\nLook in 'images' and 'docs' folders in the program directory.");
				}
			}
			catch (HeadlessException e)
			{
				error("Error while creating Images or PDF\n"+e.getMessage(),e);
			}
			catch (InterruptedException e)
			{
				error("Error while creating Images or PDF\n"+e.getMessage(),e);
			}
			catch (ExecutionException e)
			{
				error("Error while creating Images or PDF\n"+e.getMessage(),e);
			}
		}
	}
		
	boolean composeBarcodePDF(PDFSettings settings, String[] barcodes, JPanel gui, String filename)
	{
		final ProgressMonitor mon = new ProgressMonitor(gui,"Generating barcode pdf...","",0,barcodes.length);
		mon.setProgress(0);
		final BarcodePDFComposer pdf = new BarcodePDFComposer(settings,barcodes,mon,filename);
		pdf.execute();
		
		boolean success = false;
		try
		{
			success = pdf.get();
		}
		catch (CancellationException e)
		{
			// user cancelled
			success = false;
		}
		catch(InterruptedException e)
		{
			error("Could not generate pdf document.\n"+e.getMessage(),e);
			success = false;
		}
		catch (ExecutionException e)
		{
			error("Could not generate pdf document.\n"+e.getMessage(),e);
			success = false;
		}

		return success;
	}
	
	class BarcodePDFComposer extends SwingWorker<Boolean, Integer>
	{
		private String[] barcodes;
		private ProgressMonitor mon;
		private PDFSettings set;
		private String filename;
		
		BarcodePDFComposer(PDFSettings settings, String[] barcodes, ProgressMonitor mon, String filename)
		{
			this.barcodes = barcodes;
			this.mon = mon;
			this.set = settings;
			this.filename = filename;
		}
		
        public Boolean doInBackground()
        {
        	boolean success = true;
        	int progress = 0;
        	publish(progress);
            
            // in iText, each row needs to be filled in a table!
    		if (barcodes.length % set.cols != 0)
    		{
    			String[] new_msgs = new String[((barcodes.length/set.cols)+1)*set.cols];
    			for (int i=0;i<barcodes.length;i++)
    			{
    				new_msgs[i] = barcodes[i];
    			}
    			for (int i=barcodes.length;i<new_msgs.length;i++)
    			{
    				new_msgs[i] = "";
    			}
    			barcodes = new_msgs;
    		}
    		
    		Document document = null;
    		try
    		{
	    		document = new Document(new Rectangle(mmTpt(set.dim_pw),mmTpt(set.dim_ph)),mmTpt(set.mar_pl),0f,mmTpt(set.mar_pt),0f);
    			new File("docs").mkdirs(); // ensure that docs directory exists
    			/*PdfWriter writer =*/ PdfWriter.getInstance(document, new FileOutputStream("docs"+File.separatorChar+filename+".pdf"));
    			document.open();
    			
    			PdfPTable t = new PdfPTable(set.cols);
    			t.setTotalWidth(mmTpt((set.dim_lw + set.colspace)*set.cols));	// tablewidth = (labelwidth + space between cols) * columns
    			t.setLockedWidth(true);
    			t.setHorizontalAlignment(Element.ALIGN_LEFT);
    			for (String barcode : barcodes)
    			{
    				publish(++progress);
    				if (isCancelled())
    	            {
    	            	return false;
    	            }
    				
    				if (barcode.equals(""))
    				{
    					// empty cell
    					PdfPCell c = new PdfPCell();
    					c.setHorizontalAlignment(Element.ALIGN_CENTER);
    					c.setFixedHeight(mmTpt(set.dim_lh + set.rowspace));		// label height + space between rows
    					c.setPaddingLeft(mmTpt(set.mar_ll));
    					c.setPaddingRight(mmTpt(set.mar_lr + set.colspace));
    					c.setPaddingTop(mmTpt(set.mar_lt));
    					c.setPaddingBottom(mmTpt(set.mar_lb + set.rowspace));	// bottom padding + space between rows
    					c.setBorderWidth(0f);
    					t.addCell(c);
    				}
    				else
    				{
    					PdfPTable innerTable = new PdfPTable(1);
    					innerTable.setWidthPercentage(100);
    					PdfPCell innerImgCell = new PdfPCell(Image.getInstance("images"+File.separatorChar+barcode+".png"),true);
    					innerImgCell.setBorderWidth(0f);
    					innerImgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    					
    					if (set.pos.equals("Above barcode"))
    					{
    						PdfPCell innerTxtCell = new PdfPCell(new Phrase(set.txt,FontFactory.getFont(set.font, set.fsize)));
    						innerTxtCell.setBorderWidth(0f);
    						innerTxtCell.setFixedHeight(set.fsize*1.5f);
    						innerImgCell.setFixedHeight(mmTpt(set.dim_lh - (set.mar_lt + set.mar_lb)) - (set.fsize*1.5f)); //remaining space = labelheight - (bottompadding + toppadding + txtsize)
    						innerTxtCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    						innerTxtCell.setPadding(0);
    						innerTable.addCell(innerTxtCell);
    						innerTable.addCell(innerImgCell);
    					}
    					else if (set.pos.equals("Below barcode"))
    					{
    						PdfPCell innerTxtCell = new PdfPCell(new Phrase(set.txt,FontFactory.getFont(set.font, set.fsize)));
    						innerTxtCell.setBorderWidth(0f);
    						innerTxtCell.setFixedHeight(set.fsize*1.5f);
    						innerImgCell.setFixedHeight(mmTpt(set.dim_lh - (set.mar_lt + set.mar_lb)) - (set.fsize*1.5f)); //remaining space = labelheight - (bottompadding + toppadding + txtsize)
    						innerTxtCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    						innerTxtCell.setPadding(0);
    						innerTable.addCell(innerImgCell);
    						innerTable.addCell(innerTxtCell);
    					}
    					else // None
    					{
    						innerTable.addCell(innerImgCell);
    					}
    					
    					PdfPCell c = new PdfPCell();
    					c.addElement(innerTable);
    					c.setHorizontalAlignment(Element.ALIGN_CENTER);
    					c.setFixedHeight(mmTpt(set.dim_lh + set.rowspace));		// label height + space between rows
    					c.setPaddingLeft(mmTpt(set.mar_ll));
    					c.setPaddingRight(mmTpt(set.mar_lr + set.colspace));
    					c.setPaddingTop(mmTpt(set.mar_lt));
    					c.setPaddingBottom(mmTpt(set.mar_lb + set.rowspace));	// bottom padding + space between rows
    					c.setBorderWidth(0f);
    					t.addCell(c);
    				}
    			}
    			document.add(t);
    		}
    		catch(DocumentException e)
    		{
    			error("Error while creating pdf content.\n"+e.getMessage(),e);
    			success = false;
    		}
    		catch(FileNotFoundException e)
    		{
    			error("Missing file!\n"+e.getMessage(),e);
    			success = false;
    		}
    		catch(MalformedURLException e)
    		{
    			error("Missing barcode image file!\n"+e.getMessage(),e);
    			success = false;
    		}
    		catch(IOException e)
    		{
    			error("Error while reading/writing a file!\n"+e.getMessage(),e);
    			success = false;
    		}
    		finally
    		{
    			if (null != document)
    			{
    				document.close();
    			}
    		}
            
    		if (isCancelled())
            {
            	return false;
            }
    		
            return success;
        }

        @Override
		protected void process(List<Integer> chunks)
        {
			super.process(chunks);
			if (mon.isCanceled())
			{
				mon.close();
				this.cancel(true);
			}
			else
			{
				mon.setProgress(chunks.get(chunks.size()-1));
			}
		}
        
        @Override
        public void done()
        {
        	mon.close();
        }
    }
	
	boolean generateIMGs(String[] msgs, Code39Bean bean, int dpi, JPanel gui)
	{
		final ProgressMonitor mon = new ProgressMonitor(gui,"Generating barcode images...","",0,msgs.length);
		mon.setProgress(0);
		final ImgGenerator img = new ImgGenerator(msgs,bean,dpi,mon);
		img.execute();
		
		boolean success = false;
		try
		{
			success = img.get();
		}
		catch (CancellationException e)
		{
			// user cancelled
			success = false;
		}
		catch(InterruptedException e)
		{
			error("Could not generate all images.\n"+e.getMessage(),e);
			success = false;
		}
		catch (ExecutionException e)
		{
			error("Could not generate all images.\n"+e.getMessage(),e);
			success = false;
		}

		return success;
	}
	
	class ImgGenerator extends SwingWorker<Boolean, Integer>
	{
		private String[] barcodes;
		private Code39Bean bean;
		private int dpi;
		private ProgressMonitor mon;
		
		ImgGenerator(String[] barcodes, Code39Bean bean, int dpi, ProgressMonitor mon)
		{
			this.barcodes = barcodes;
			this.bean = bean;
			this.dpi = dpi;
			this.mon = mon;
		}
		
        public Boolean doInBackground()
        {
        	boolean success = true;
        	int progress = 0;
        	publish(progress);
            
            new File("images").mkdirs(); // ensure that images directory exists
            OutputStream out = null;
            
            try
			{
	            for (String msg : barcodes)
	    		{
	            	publish(++progress);
	            	if (isCancelled())
    	            {
    	            	return false;
    	            }
	            	
	    			File outputFile = new File("images"+File.separatorChar+msg+".png"); // TODO: make this optional
	    			
    				out = new FileOutputStream(outputFile);
    				BitmapCanvasProvider canvas = new BitmapCanvasProvider(out, "image/x-png", dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);
    				bean.generateBarcode(canvas, msg);
    				canvas.finish();
	    		}
			}
			catch (FileNotFoundException e)
			{
				error("File not found!\n"+e.getMessage(),e);
				success = false;
			}
			catch (IOException e)
			{
				error("Input/Output error!\n"+e.getMessage(),e);
				success = false;
			}
			finally
			{
				if (null != out)
				{
					try
					{
						out.close();
					}
					catch (IOException e)
					{
						error("Could not close file!",e);
						success = false;
					}
				}
			}
            
            if (isCancelled())
            {
            	return false;
            }
            
            return success;
        }

        @Override
		protected void process(List<Integer> chunks)
        {
			super.process(chunks);
			if (mon.isCanceled())
			{
				mon.close();
				this.cancel(true);
			}
			else
			{
				mon.setProgress(chunks.get(chunks.size()-1));
			}
		}
        
        @Override
        public void done()
        {
        	mon.close();
        }
    }
	
	protected static boolean confirmBarcodes(String[] barcodes)
	{
		final JDialog jd = new JDialog(); // TODO: parent
		jd.setTitle("Confirm barcodes...");
		jd.setLayout(new BorderLayout(padding,padding));
		jd.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
		
		int max = 10000;
		JList<String> jl;
		if (barcodes.length < max)
		{
			jd.add(new JLabel("Please confirm whether these barode numbers are correct!"),BorderLayout.NORTH);
			jl = new JList<String>(barcodes);
		}
		else
		{
			jd.add(new JLabel("<html>Please confirm whether these barode numbers are correct!<br/>There are "+barcodes.length+", but only the first "+max+" are shown:</html>"),BorderLayout.NORTH);
			String[] bc = new String[max];
			for (int i=0;i<max;i++)
			{
				bc[i] = barcodes[i];
			}
			jl = new JList<String>(bc);
		}
		JScrollPane jsp = new JScrollPane(jl);
		jd.add(jsp,BorderLayout.CENTER);
		
		class ConfActionListener implements ActionListener
		{
			private boolean confirmed = false;
			
			public boolean getConfirmed()
			{
				return confirmed;
			}
			
			public void actionPerformed(ActionEvent e)
			{
				String ac = e.getActionCommand();
				if (ac.equals("No, go back..."))
				{
					jd.dispose();
					confirmed = false;
				}
				else if (ac.equals("Yes, create barcodes!"))
				{
					jd.dispose();
					confirmed = true;
				}
			}
		};
		
		ConfActionListener caf = new ConfActionListener();
		
		ButtonPanel btns = new ButtonPanel(caf,new String[]{"No, go back...","Yes, create barcodes!"});
		jd.add(btns,BorderLayout.SOUTH);

		jd.pack();
		jd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		jd.setLocationRelativeTo(null);
		jd.setVisible(true);
		
		return caf.getConfirmed();
	}
	
	protected static void error(String msg)
	{
		JOptionPane.showMessageDialog(null,msg,"ERROR!",JOptionPane.ERROR_MESSAGE);
	}
	
	protected static void error(String msg, Exception e)
	{
		JOptionPane.showMessageDialog(null,msg,"ERROR!",JOptionPane.ERROR_MESSAGE);
		PrintStream p;
		try {
			p = new PrintStream(new FileOutputStream(errorLogFile,true));
			p.println("Error:"); //TODO: get current date and time
			p.println(msg);
			e.printStackTrace(p);
			p.close();
		} catch (FileNotFoundException e1) {
			error("Error while writing error message to file.\nThis is just sad...");
		}
	}
	
	// mm -> pt
	static float mmTpt(double mm)
	{
		return (float)mm * 2.8346456692913f;
	}
	
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new RSCBarcoder();
			}
		});
	}

	class PDFSettings
	{
		String txt;
		String font;
		float fsize;
		String pos;
		double dim_pw;
		double dim_ph;
		double mar_pl;
		double mar_pt;
		double dim_lw;
		double dim_lh;
		double mar_ll;
		double mar_lr;
		double mar_lt;
		double mar_lb;
		double rowspace;
		double colspace;
//		int rows;
		int cols;
		
		PDFSettings(String[] settings)
		{
			// settings should already be safe for parsing
			this.txt = settings[0];
			this.font = settings[1];
			this.fsize = Float.parseFloat(settings[2]);
			this.pos = settings[3];
			this.dim_pw = Double.parseDouble(settings[4]);
			this.dim_ph = Double.parseDouble(settings[5]);
			this.mar_pl = Double.parseDouble(settings[6]);
			this.mar_pt = Double.parseDouble(settings[7]);
			this.dim_lw = Double.parseDouble(settings[8]);
			this.dim_lh = Double.parseDouble(settings[9]);
			this.mar_ll = Double.parseDouble(settings[10]);
			this.mar_lr = Double.parseDouble(settings[11]);
			this.mar_lt = Double.parseDouble(settings[12]);
			this.mar_lb = Double.parseDouble(settings[13]);
			this.rowspace = Double.parseDouble(settings[14]);
			this.colspace = Double.parseDouble(settings[15]);
//			this.rows = Integer.parseInt(set[16]); // UNUSED!! :O lol
			this.cols = Integer.parseInt(settings[17]);
			/*
			 * 0 "Text",						0
			 * 1 "Font",						1
			 * 2 "Font size",					2
			 * 3 "Position"						3
			 * 
			 * 4 "Page width",					0
			 * 5 "Page height",					1
			 * 6 "Page left margin",			2
			 * 7 "Page top margin",				3
			 * 8 "Label width"					4
			 * 9 "Label height"					5
			 * 10 "Label left margin",			6
			 * 11 "Label right margin",			7
			 * 12 "Label top margin",			8
			 * 13 "Label bottom margin",		9
			 * 14 "Space between rows",			10
			 * 15 "Space between columns",		11
			 * 16 "Number of rows",				12
			 * 17 "Number of columns"			13
			 */
		}
	}
	
	class TextSettings
	{
		String font;
		float fsize;
		int mcpl; // maximum characters per line
		
		TextSettings(String[] settings)
		{
			// settings should already be safe for parsing
			// "Font","Font size","Max chars per line"
			this.font = settings[0];
			this.fsize = Float.parseFloat(settings[1]);
			this.mcpl = Integer.parseInt(settings[2]);
		}
	}
}