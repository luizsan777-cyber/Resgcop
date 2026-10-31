package com.regsco.compras;
import android.app.Activity;
import android.graphics.*;
import android.util.LruCache;
import android.widget.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

/** Small HTTPS catalogue thumbnails only; never uploads a camera image. */
final class RemotePhotos {
    static final ExecutorService WORK=Executors.newFixedThreadPool(2);
    static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(8*1024*1024){protected int sizeOf(String key,Bitmap b){return b.getByteCount();}};
    static String safe(String value){try{URI u=new URI(value);String host=u.getHost();if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1&&u.getPort()!=443||host==null)return "";
        for(String domain:new String[]{"ik.imagekit.io","vteximg.com.br","vtexassets.com"})if(host.equals(domain)||host.endsWith("."+domain))return value;
    }catch(Exception ignored){}return "";}
    static void load(Activity a,ImageView view,TextView state,String url){view.setTag(url);if(safe(url).isEmpty()){state.setText("Foto não disponibilizada pelo site");return;}Bitmap cached=CACHE.get(url);if(cached!=null){view.setImageBitmap(cached);state.setText("Foto do site");return;}
        WORK.execute(()->{Bitmap bitmap=null;try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);try{if(c.getResponseCode()!=200)throw new IOException();try(InputStream in=c.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>3000000)throw new IOException();bytes.write(buffer,0,n);}byte[] data=bytes.toByteArray();BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>512)o.inSampleSize*=2;o.inJustDecodeBounds=false;bitmap=BitmapFactory.decodeByteArray(data,0,data.length,o);}}finally{c.disconnect();}}catch(Exception ignored){}
            final Bitmap ready=bitmap;if(ready!=null)CACHE.put(url,ready);a.runOnUiThread(()->{if(a.isDestroyed()||!url.equals(view.getTag()))return;if(ready!=null){view.setImageBitmap(ready);state.setText("Foto do site");}else state.setText("Foto indisponível • toque em Ver produto para conferir");});
        });
    }
}
