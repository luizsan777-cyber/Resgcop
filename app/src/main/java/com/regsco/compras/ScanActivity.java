package com.regsco.compras;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.common.Barcode;
import java.io.File;

/** On-device capture and recognition. Photographs stay in private cache. */
public class ScanActivity extends ComponentActivity {
    int theme;
    LinearLayout root;FrameLayout area;PreviewView preview;ImageCapture capture;
    androidx.camera.core.Camera camera;ProcessCameraProvider provider;
    Bitmap photo;CropView crop;File temporary;boolean torch=false,working=false;
    TextView hint;Button shoot,light,recognize,again;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);theme=getIntent().getIntExtra("theme",0);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(new ThemeSkin.Backdrop(theme));root.setPadding(16,16,16,16);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(16,i.getSystemWindowInsetTop()+16,16,i.getSystemWindowInsetBottom()+16);return i;});setContentView(root);
        hint=new TextView(this);hint.setTextColor(ThemeSkin.text(theme));hint.setTextSize(17);hint.setPadding(10,12,10,12);hint.setText("Enquadre a etiqueta. Toque para focar.");root.addView(hint);
        area=new FrameLayout(this);root.addView(area,new LinearLayout.LayoutParams(-1,0,1));
        preview=new PreviewView(this);area.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        shoot=button("Fotografar",()->take());light=button("Lanterna",()->{if(camera!=null&&camera.getCameraInfo().hasFlashUnit()){torch=!torch;camera.getCameraControl().enableTorch(torch);}else toast("Esta câmera não possui flash");});
        recognize=button("Reconhecer área selecionada",()->read());again=button("Fotografar novamente",()->reset());button("Cancelar",()->finish());
        recognize.setVisibility(View.GONE);again.setVisibility(View.GONE);
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)start();else requestPermissions(new String[]{Manifest.permission.CAMERA},10);
    }
    Button button(String text,Runnable action){Button b=new Button(this);b.setText(text);b.setTextColor(theme==2?0xff321337:Color.WHITE);b.setBackground(ThemeSkin.primary(theme,18));b.setMinHeight((int)(52*getResources().getDisplayMetrics().density));root.addView(b);b.setOnClickListener(v->{if(!working)action.run();});return b;}
    void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==10&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)start();else{toast("Sem permissão de câmera. O cadastro manual continua disponível.");finish();}}
    void start(){
        var future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{try{
            provider=future.get();Preview p=new Preview.Builder().build();p.setSurfaceProvider(preview.getSurfaceProvider());
            capture=new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build();
            provider.unbindAll();camera=provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,p,capture);
            preview.setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_UP){var point=preview.getMeteringPointFactory().createPoint(event.getX(),event.getY());camera.getCameraControl().startFocusAndMetering(new FocusMeteringAction.Builder(point).build());v.performClick();}return true;});
        }catch(Exception e){toast("Não foi possível iniciar a câmera");finish();}},ContextCompat.getMainExecutor(this));
    }
    void take(){
        if(capture==null)return;working=true;shoot.setEnabled(false);
        temporary=new File(getCacheDir(),"regsco-scan-"+System.nanoTime()+".jpg");
        capture.setTargetRotation(preview.getDisplay().getRotation());
        capture.takePicture(new ImageCapture.OutputFileOptions.Builder(temporary).build(),ContextCompat.getMainExecutor(this),new ImageCapture.OnImageSavedCallback(){
            @Override public void onError(ImageCaptureException e){working=false;shoot.setEnabled(true);toast("Falha ao fotografar. Tente novamente.");cleanup();}
            @Override public void onImageSaved(ImageCapture.OutputFileResults result){
                new Thread(()->{
                    Bitmap decoded=null;try{
                        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeFile(temporary.getPath(),opts);opts.inJustDecodeBounds=false;opts.inSampleSize=1;
                        while(Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>2600)opts.inSampleSize*=2;
                        decoded=BitmapFactory.decodeFile(temporary.getPath(),opts);
                        ExifInterface exif=new ExifInterface(temporary);Matrix matrix=new Matrix();if(exif.isFlipped())matrix.postScale(-1,1);matrix.postRotate(exif.getRotationDegrees());
                        if(decoded!=null){Bitmap rotated=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),matrix,true);if(rotated!=decoded)decoded.recycle();decoded=rotated;}
                    }catch(Exception ignored){}finally{cleanup();}
                    final Bitmap ready=decoded;runOnUiThread(()->{working=false;shoot.setEnabled(true);if(isDestroyed()){if(ready!=null)ready.recycle();return;}if(ready==null){toast("Imagem inválida. Fotografe novamente.");return;}
                        photo=ready;crop=new CropView();area.removeAllViews();area.addView(crop,new FrameLayout.LayoutParams(-1,-1));camera.getCameraControl().enableTorch(false);torch=false;
                        shoot.setVisibility(View.GONE);light.setVisibility(View.GONE);recognize.setVisibility(View.VISIBLE);again.setVisibility(View.VISIBLE);hint.setText("Arraste um retângulo ao redor da etiqueta. Sem seleção, será usada a imagem inteira.");
                    });
                },"regsco-photo").start();
            }
        });
    }
    void read(){
        if(photo==null)return;working=true;hint.setText("Reconhecendo no aparelho…");
        Bitmap selected=crop.selected();InputImage image=InputImage.fromBitmap(selected,0);
        var text=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        var scanner=BarcodeScanning.getClient(new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13,Barcode.FORMAT_EAN_8,Barcode.FORMAT_UPC_A,Barcode.FORMAT_UPC_E).build());
        var textTask=text.process(image);var codeTask=scanner.process(image);
        com.google.android.gms.tasks.Tasks.whenAllComplete(textTask,codeTask).addOnCompleteListener(done->{
            text.close();scanner.close();if(selected!=photo)selected.recycle();working=false;if(isDestroyed())return;
            String detected=textTask.isSuccessful()?textTask.getResult().getText():"";
            String barcode="";if(codeTask.isSuccessful()&&!codeTask.getResult().isEmpty()){String raw=codeTask.getResult().get(0).getRawValue();if(raw!=null)barcode=raw;}
            if(detected.isEmpty()&&barcode.isEmpty()){hint.setText("Não foi possível reconhecer. Ajuste o recorte ou fotografe novamente.");return;}
            setResult(RESULT_OK,new Intent().putExtra("text",detected).putExtra("barcode",barcode));finish();
        });
    }
    void reset(){area.removeAllViews();area.addView(preview,new FrameLayout.LayoutParams(-1,-1));if(photo!=null){photo.recycle();photo=null;}shoot.setVisibility(View.VISIBLE);light.setVisibility(View.VISIBLE);recognize.setVisibility(View.GONE);again.setVisibility(View.GONE);hint.setText("Enquadre a etiqueta. Toque para focar.");}
    void cleanup(){if(temporary!=null)temporary.delete();}
    @Override protected void onDestroy(){cleanup();if(provider!=null)provider.unbindAll();super.onDestroy();}
    final class CropView extends View {
        Paint imagePaint=new Paint(Paint.FILTER_BITMAP_FLAG),borderPaint=new Paint();
        RectF displayed=new RectF(),selection=new RectF();float x,y;boolean chosen=false;
        CropView(){super(ScanActivity.this);setContentDescription("Arraste para selecionar a área da etiqueta");}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(photo==null)return;float scale=Math.min((float)getWidth()/photo.getWidth(),(float)getHeight()/photo.getHeight());float w=photo.getWidth()*scale,h=photo.getHeight()*scale;displayed.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);canvas.drawBitmap(photo,null,displayed,imagePaint);if(chosen){Paint p=borderPaint;p.setColor(0xff80ffb0);p.setStrokeWidth(4);p.setStyle(Paint.Style.STROKE);canvas.drawRect(selection,p);}}
        @Override public boolean onTouchEvent(MotionEvent e){float px=Math.max(displayed.left,Math.min(displayed.right,e.getX())),py=Math.max(displayed.top,Math.min(displayed.bottom,e.getY()));if(e.getAction()==MotionEvent.ACTION_DOWN){x=px;y=py;chosen=true;}selection.set(Math.min(x,px),Math.min(y,py),Math.max(x,px),Math.max(y,py));invalidate();if(e.getAction()==MotionEvent.ACTION_UP)performClick();return true;}
        @Override public boolean performClick(){super.performClick();return true;}
        Bitmap selected(){if(!chosen||selection.width()<12||selection.height()<12||displayed.width()==0)return photo;float scale=photo.getWidth()/displayed.width();int l=Math.max(0,(int)((selection.left-displayed.left)*scale)),t=Math.max(0,(int)((selection.top-displayed.top)*scale));int w=Math.min(photo.getWidth()-l,Math.max(1,(int)(selection.width()*scale))),h=Math.min(photo.getHeight()-t,Math.max(1,(int)(selection.height()*scale)));return Bitmap.createBitmap(photo,l,t,w,h);}
    }
}
