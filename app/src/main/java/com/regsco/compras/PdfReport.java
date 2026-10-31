package com.regsco.compras;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/** Small standards-based PDF writer for the locally generated text summary. */
final class PdfReport {
    private static final Charset ENCODING=Charset.forName("windows-1252");
    static void write(String report,OutputStream destination) throws IOException {
        if(destination==null)throw new IOException("Não foi possível abrir o arquivo");
        List<String> lines=new ArrayList<>();
        for(String line:report.split("\n",-1)){
            while(line.length()>76){int cut=line.lastIndexOf(' ',76);if(cut<1)cut=76;lines.add(line.substring(0,cut));line=line.substring(cut).trim();}lines.add(line);
        }
        int pages=Math.max(1,(lines.size()+37)/38),objects=3+pages*2;long[] offsets=new long[objects+1];
        try(Counting out=new Counting(destination)){
            text(out,"%PDF-1.4\n%âãÏÓ\n");
            object(out,offsets,1,"<< /Type /Catalog /Pages 2 0 R >>");
            StringBuilder kids=new StringBuilder();for(int i=0;i<pages;i++)kids.append(4+i*2).append(" 0 R ");
            object(out,offsets,2,"<< /Type /Pages /Count "+pages+" /Kids ["+kids+"] >>");
            object(out,offsets,3,"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
            for(int page=0;page<pages;page++){
                int id=4+page*2;object(out,offsets,id,"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents "+(id+1)+" 0 R >>");
                StringBuilder content=new StringBuilder("BT /F1 12 Tf 42 795 Td 19 TL\n");
                for(int i=page*38;i<Math.min(lines.size(),(page+1)*38);i++)content.append('(').append(escape(lines.get(i))).append(") Tj T*\n");
                content.append("ET\nBT /F1 9 Tf 42 30 Td (RegsCo - ").append(page+1).append(" / ").append(pages).append(") Tj ET\n");
                byte[] bytes=content.toString().getBytes(ENCODING);offsets[id+1]=out.count;text(out,(id+1)+" 0 obj\n<< /Length "+bytes.length+" >>\nstream\n");out.write(bytes);text(out,"\nendstream\nendobj\n");
            }
            long start=out.count;text(out,"xref\n0 "+(objects+1)+"\n0000000000 65535 f \n");for(int i=1;i<=objects;i++)text(out,String.format(Locale.ROOT,"%010d 00000 n \n",offsets[i]));text(out,"trailer\n<< /Size "+(objects+1)+" /Root 1 0 R >>\nstartxref\n"+start+"\n%%EOF\n");
        }
    }
    static String escape(String s){return s.replace("\\","\\\\").replace("(","\\(").replace(")","\\)").replace('\r',' ');}
    static void text(Counting out,String s)throws IOException{out.write(s.getBytes(ENCODING));}
    static void object(Counting out,long[] offsets,int id,String body)throws IOException{offsets[id]=out.count;text(out,id+" 0 obj\n"+body+"\nendobj\n");}
    static final class Counting extends FilterOutputStream {long count;Counting(OutputStream out){super(out);}@Override public void write(int b)throws IOException{out.write(b);count++;}@Override public void write(byte[] bytes,int off,int len)throws IOException{out.write(bytes,off,len);count+=len;}}
}
