package com.regsco.compras;
import android.content.Context;
import android.graphics.*;
import android.util.Base64;
import android.widget.ImageView;
import java.io.*;
import java.text.Normalizer;
final class ProductPhotos {
 static String key(String name){String n=Normalizer.normalize(name.toLowerCase(java.util.Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");if(n.contains("arroz"))return "rice";if(n.contains("macarrao")||n.contains("espaguete"))return "pasta";if(n.contains("oleo"))return "oil";if(n.contains("acucar"))return "sugar";if(n.matches(".*\\bsal\\b.*"))return "salt";return "";}
 static ImageView view(Context c,String name,String data,int pixels){ImageView image=new ImageView(c);image.setContentDescription(data.isEmpty()?"Imagem ilustrativa de "+name:"Foto de "+name);image.setScaleType(ImageView.ScaleType.CENTER_CROP);Bitmap b=decode(data,pixels);if(b!=null)image.setImageBitmap(b);else{int id=c.getResources().getIdentifier("food_"+key(name),"drawable",c.getPackageName());if(id!=0)image.setImageResource(id);else{image.setImageDrawable(new ThemeSkin.Icon(3,0xff567549));image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);image.setBackgroundColor(0xffedf0df);}}return image;}
 static Bitmap decode(String data,int size){try{if(data==null||data.isEmpty()||data.length()>1500000)return null;byte[] bytes=Base64.decode(data,Base64.DEFAULT);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);o.inJustDecodeBounds=false;o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>size*2)o.inSampleSize*=2;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);}catch(Exception e){return null;}}
 static String encode(Bitmap bitmap)throws IOException {if(bitmap==null)throw new IOException("Imagem inválida");float scale=Math.min(1f,512f/Math.max(bitmap.getWidth(),bitmap.getHeight()));Bitmap small=Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(bitmap.getWidth()*scale)),Math.max(1,Math.round(bitmap.getHeight()*scale)),true);ByteArrayOutputStream out=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,82,out);if(small!=bitmap)small.recycle();return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);}
 static String read(Context c,android.net.Uri uri)throws Exception{
  BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream in=c.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,o);}if(o.outWidth<1||o.outHeight<1)throw new IOException("Imagem não reconhecida");o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1024)o.inSampleSize*=2;o.inJustDecodeBounds=false;Bitmap b;try(InputStream in=c.getContentResolver().openInputStream(uri)){b=BitmapFactory.decodeStream(in,null,o);}if(b==null)throw new IOException("Imagem inválida");
  try(InputStream in=c.getContentResolver().openInputStream(uri)){androidx.exifinterface.media.ExifInterface e=new androidx.exifinterface.media.ExifInterface(in);Matrix m=new Matrix();if(e.isFlipped())m.postScale(-1,1);m.postRotate(e.getRotationDegrees());Bitmap r=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);if(r!=b){b.recycle();b=r;}}
  String encoded=encode(b);b.recycle();return encoded;
 }
}
