package com.regsco.compras;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import java.util.Random;

/** Native decorative assets: scalable, offline and no photographic payload. */
final class ThemeSkin {
    static int background(int t){return t==0?0xfff7f4df:t==1?0xfffff1f8:0xff0e0a18;}
    static int text(int t){return t==0?0xff173c2b:t==1?0xff44224f:0xfff8effc;}
    static int secondary(int t){return t==0?0xff63715e:t==1?0xff785b7c:0xffc5b1cf;}
    static int accent(int t){return t==0?0xff2d6844:t==1?0xffad258b:0xfff18bdf;}
    static int bright(int t){return t==0?0xff8dcc73:t==1?0xffed60c4:0xffe948c4;}
    static int surface(int t){return t==0?0xedfffdf0:t==1?0xedfff8fc:0xed20162e;}
    static int stroke(int t){return t==0?0xffe4e5ce:t==1?0xffefd8e9:0xff3d294e;}
    static GradientDrawable card(int t,float radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{surface(t),t==2?0xed191222:surface(t)});d.setCornerRadius(radius);d.setStroke(1,stroke(t));return d;}
    static GradientDrawable primary(int t,float radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,t==0?new int[]{0xff397652,0xff234f36}:t==1?new int[]{0xffc54bb5,0xffa52f91}:new int[]{0xffffb3ed,0xffec62d5});d.setCornerRadius(radius);return d;}

    static final class Backdrop extends Drawable {
        final int theme;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final Path leaf=new Path();final Random random=new Random(82);final float[] stars=new float[180];
        Shader base,glowA,glowB;float width,height;
        Backdrop(int theme){this.theme=theme;for(int i=0;i<stars.length;i++)stars[i]=random.nextFloat();leaf.moveTo(0,0);leaf.cubicTo(-13,-14,-18,-36,-4,-51);leaf.cubicTo(13,-32,12,-12,0,0);leaf.close();}
        @Override protected void onBoundsChange(Rect b){width=b.width();height=b.height();if(width<=0||height<=0)return;base=new LinearGradient(0,0,width,height,new int[]{background(theme),theme==0?0xffedf0d7:theme==1?0xfffbe4f2:0xff080713},null,Shader.TileMode.CLAMP);glowA=new RadialGradient(width*.92f,height*.25f,width*.8f,theme==2?0x70552b83:theme==1?0x66e9a5e3:0x337eaa66,Color.TRANSPARENT,Shader.TileMode.CLAMP);glowB=new RadialGradient(width*.18f,height*.7f,width*.7f,theme==2?0x504b164c:theme==1?0x44f7b8d2:0x22abc381,Color.TRANSPARENT,Shader.TileMode.CLAMP);}
        @Override public void draw(Canvas c){if(base==null)return;paint.setStyle(Paint.Style.FILL);paint.setShader(base);c.drawRect(getBounds(),paint);paint.setShader(glowA);c.drawRect(getBounds(),paint);paint.setShader(glowB);c.drawRect(getBounds(),paint);paint.setShader(null);
            if(theme==0){branch(c,-width*.02f,height*.48f,width/360f,-12);branch(c,width*1.03f,height*.98f,width/360f,165);branch(c,width*.98f,height*.12f,width/480f,166);}
            else{for(int i=0;i<stars.length-2;i+=3){float x=stars[i]*width,y=stars[i+1]*height,r=(.4f+stars[i+2]*1.4f)*width/400f;paint.setColor(theme==2?0x65f2c5ff:0x40c862b4);c.drawCircle(x,y,r,paint);if(i%21==0){paint.setStrokeWidth(width/600f);c.drawLine(x-r*3,y,x+r*3,y,paint);c.drawLine(x,y-r*3,x,y+r*3,paint);}}
                if(theme==1){branch(c,width,height*.96f,width/390f,168);}
            }
        }
        void branch(Canvas c,float x,float y,float scale,float rotation){c.save();c.translate(x,y);c.scale(scale,scale);c.rotate(rotation);paint.setShader(null);paint.setColor(theme==0?0x65728b49:0x22ba73b0);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.2f);c.drawLine(0,15,18,-160,paint);paint.setStyle(Paint.Style.FILL);for(int i=0;i<6;i++){c.save();c.translate(i*3,-i*26);c.rotate(i%2==0?-43:60);paint.setColor(theme==0?(i%2==0?0x64729450:0x42779b4d):0x27c27ab6);c.drawPath(leaf,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.65f);paint.setColor(theme==0?0x88748a49:0x38b771a1);c.drawLine(0,0,-4,-44,paint);paint.setStyle(Paint.Style.FILL);c.restore();}c.restore();}
        @Override public void setAlpha(int a){}@Override public void setColorFilter(ColorFilter f){}@Override public int getOpacity(){return PixelFormat.OPAQUE;}
    }
    static final class Icon extends Drawable {
        final int kind,color;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final Path path=new Path();
        Icon(int kind,int color){this.kind=kind;this.color=color;}
        @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/32f,getBounds().height()/32f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.9f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);path.reset();
            switch(kind){
                case 0:path.moveTo(4,15);path.lineTo(16,5);path.lineTo(28,15);path.moveTo(8,13);path.lineTo(8,27);path.lineTo(13,27);path.lineTo(13,20);path.lineTo(19,20);path.lineTo(19,27);path.lineTo(24,27);path.lineTo(24,13);c.drawPath(path,p);break;
                case 1:path.moveTo(4,11);path.lineTo(28,11);path.lineTo(24,26);path.lineTo(8,26);path.close();c.drawPath(path,p);c.drawLine(10,11,14,5,p);c.drawLine(22,11,18,5,p);c.drawLine(12,16,13,22,p);c.drawLine(20,16,19,22,p);break;
                case 2:c.rotate(-40,16,16);c.drawRoundRect(9,3,23,29,7,7,p);c.drawLine(9,16,23,16,p);break;
                case 3:c.drawRoundRect(5,10,27,27,3,3,p);c.drawRoundRect(11,5,21,11,2,2,p);c.drawLine(5,17,27,17,p);c.drawLine(14,17,18,17,p);break;
                case 4:p.setStyle(Paint.Style.FILL);for(int x=7;x<28;x+=9)c.drawCircle(x,16,1.8f,p);break;
                case 5:c.drawCircle(16,16,11,p);c.drawLine(16,9,16,16,p);c.drawLine(16,16,22,19,p);break;
                case 6:c.drawCircle(16,16,5,p);for(int a=0;a<8;a++){c.save();c.rotate(a*45,16,16);c.drawLine(16,3,16,6,p);c.restore();}c.drawCircle(16,16,10,p);break;
                case 7:path.moveTo(16,2);path.lineTo(21,27);path.lineTo(3,12);path.lineTo(29,12);path.lineTo(10,29);path.close();c.drawPath(path,p);c.drawLine(16,2,10,29,p);break;
                case 8:path.moveTo(20,7);path.lineTo(11,16);path.lineTo(20,25);c.drawPath(path,p);break;
                default:c.drawCircle(16,16,11,p);c.drawLine(16,10,16,22,p);c.drawLine(10,16,22,16,p);
            }c.restore();}
        @Override public void setAlpha(int a){p.setAlpha(a);}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static final class Ring extends View {
        final Paint track=new Paint(Paint.ANTI_ALIAS_FLAG),arc=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF bounds=new RectF();final int theme;final float fraction;float shown;ValueAnimator animation;
        Ring(Context context,int theme,double fraction){super(context);this.theme=theme;this.fraction=(float)Math.max(0,Math.min(1,fraction));shown=this.fraction;track.setStyle(Paint.Style.STROKE);arc.setStyle(Paint.Style.STROKE);arc.setStrokeCap(Paint.Cap.ROUND);track.setColor(stroke(theme));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){float size=Math.min(w,h),line=size*.065f;track.setStrokeWidth(line);arc.setStrokeWidth(line);bounds.set((w-size)/2+line,(h-size)/2+line,(w+size)/2-line,(h+size)/2-line);arc.setShader(new LinearGradient(0,0,w,h,new int[]{accent(theme),bright(theme)},null,Shader.TileMode.CLAMP));}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawArc(bounds,0,360,false,track);canvas.drawArc(bounds,-90,360*shown,false,arc);}
    }
}
