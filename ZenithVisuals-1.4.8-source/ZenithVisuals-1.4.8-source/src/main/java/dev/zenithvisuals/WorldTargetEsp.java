package dev.zenithvisuals;

import java.lang.reflect.*;

/** Reliable 3D target ESP for Minecraft 1.21.11 / Fabric API 0.141.x. */
final class WorldTargetEsp {
    private static boolean registered;
    private static long lastErrorLog;
    private static Object lastTarget;
    private static long lastTargetSeenAt;
    private static volatile Method vertexMethod, colorMethod, normalMethod, lineWidthMethod;
    private static volatile Class<?> vertexEntryType;
    private WorldTargetEsp() {}

    static void register() {
        if (registered) return;
        try {
            Class<?> eventsType = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents");
            Object afterEntities = eventsType.getField("AFTER_ENTITIES").get(null);
            Class<?> listenerType = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents$AfterEntities");
            Object listener = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[]{listenerType},
                    (proxy, method, args) -> {
                        if (method.getName().equals("afterEntities") && args != null && args.length == 1) render(args[0]);
                        return null;
                    });

            // Register through Fabric's public Event interface, never through its private implementation class.
            Class<?> eventType = Class.forName("net.fabricmc.fabric.api.event.Event");
            eventType.getMethod("register", Object.class).invoke(afterEntities, listener);
            registered = true;
            System.out.println("[ZenithVisuals] 3D Target ESP render event registered");
        } catch (Throwable error) {
            logError("register", error);
        }
    }

    private static void render(Object context) {
        if (!ZenithVisuals.enabled("target_esp") || ZenithVisuals.menuOpen) return;
        try {
            Class<?> mcType = Class.forName("net.minecraft.class_310");
            Object mc = mcType.getMethod("method_1551").invoke(null);
            Object player = publicField(mc, "field_1724");
            if (player == null) return;

            long now = System.currentTimeMillis();
            Object target = findCrosshairTarget(mc, player);
            if (isLiving(target) && target != player && alive(target)) {
                lastTarget = target;
                lastTargetSeenAt = now;
            }
            long lifetime = Math.max(1, Math.min(10, ZenithVisuals.targetEspDisplayTime)) * 1000L;
            if (lastTarget == null || now - lastTargetSeenAt > lifetime || !alive(lastTarget)) {
                lastTarget = null;
                return;
            }
            target = lastTarget;
            if (ZenithVisuals.setting("target_esp.onlyVisible") && !canSee(player, target)) return;

            // Use the public Fabric WorldRenderContext interface. Implementations may be package-private.
            Class<?> contextType = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext");
            Object matrices = contextType.getMethod("matrices").invoke(context);
            Object consumers = contextType.getMethod("consumers").invoke(context);
            Object gameRenderer = contextType.getMethod("gameRenderer").invoke(context);
            if (matrices == null || consumers == null || gameRenderer == null) return;

            // Resolve calls through public Minecraft API classes. Calling getMethod on a private runtime
            // implementation class can throw IllegalAccessException even when the API method is public.
            Class<?> gameRendererType = Class.forName("net.minecraft.class_757");
            Object camera = gameRendererType.getMethod("method_19418").invoke(gameRenderer);
            Class<?> cameraType = Class.forName("net.minecraft.class_4184");
            Object cameraPos = declaredOrPublicField(cameraType, camera, "field_18712");
            double camX = number(cameraPos, "field_1352");
            double camY = number(cameraPos, "field_1351");
            double camZ = number(cameraPos, "field_1350");

            Class<?> entityType = Class.forName("net.minecraft.class_1297");
            Object bounds = entityType.getMethod("method_5829").invoke(target);
            double minX = number(bounds, "field_1323"), minY = number(bounds, "field_1322"), minZ = number(bounds, "field_1321");
            double maxX = number(bounds, "field_1320"), maxY = number(bounds, "field_1325"), maxZ = number(bounds, "field_1324");
            double cx = (minX + maxX) * 0.5, cz = (minZ + maxZ) * 0.5;
            double width = Math.max(maxX - minX, maxZ - minZ);
            double height = Math.max(0.5, maxY - minY);

            Class<?> renderLayers = Class.forName("net.minecraft.class_12249");
            Object linesLayer = renderLayers.getField("field_64042").get(null); // RenderLayers.LINES
            Class<?> providerType = Class.forName("net.minecraft.class_4597");
            Object consumer = providerType.getMethod("method_73477", Class.forName("net.minecraft.class_1921"))
                    .invoke(consumers, linesLayer);
            if (consumer == null) return;

            Class<?> matrixStackType = Class.forName("net.minecraft.class_4587");
            matrixStackType.getMethod("method_22903").invoke(matrices); // push
            try {
                Object entry = matrixStackType.getMethod("method_23760").invoke(matrices); // peek
                Class<?> entryType = Class.forName("net.minecraft.class_4587$class_4665");
                double x1=minX-camX, x2=maxX-camX, y1=minY-camY, y2=maxY-camY, z1=minZ-camZ, z2=maxZ-camZ;
                double scale = Math.max(0.5, Math.min(2.0, ZenithVisuals.targetEspSize / 100.0));
                double radius = Math.max(0.38, width * 0.72) * scale;
                double speed = 0.45 + Math.max(0, Math.min(4, ZenithVisuals.targetEspSpeed)) * 0.4;
                double phase = now * 0.0018 * speed;
                int mode = Math.floorMod(ZenithVisuals.targetEspMode, ZenithVisuals.ESP_MODES.length);
                int base = ZenithVisuals.setting("target_esp.hurtColor") && hurtTime(target) > 0
                        ? 0xFFFF456B : ZenithVisuals.gradientColor(0, 1);

                if (ZenithVisuals.setting("target_esp.box")) {
                    drawBox(consumer, entry, entryType, x1,y1,z1,x2,y2,z2,base);
                }
                if (ZenithVisuals.setting("target_esp.ring")) {
                    switch (mode) {
                        case 0 -> {
                            int lower = ZenithVisuals.enabled("fps_optimizer") ? 40 : 52;
                            int upper = ZenithVisuals.enabled("fps_optimizer") ? 28 : 36;
                            ring(consumer,entry,entryType,cx-camX,y1+0.035,cz-camZ,radius,phase,lower,base);
                            ring(consumer,entry,entryType,cx-camX,y1+0.055,cz-camZ,radius*0.72,-phase*0.72,upper,base);
                        }
                        case 1 -> {
                            int lower = ZenithVisuals.enabled("fps_optimizer") ? 40 : 50;
                            int upper = ZenithVisuals.enabled("fps_optimizer") ? 32 : 40;
                            ring(consumer,entry,entryType,cx-camX,y1+0.04,cz-camZ,radius,-phase,lower,base);
                            ring(consumer,entry,entryType,cx-camX,y1+height*0.72,cz-camZ,radius*0.82,phase*1.18,upper,base);
                        }
                        case 2 -> square(consumer,entry,entryType,cx-camX,y1+height*0.50,cz-camZ,radius*1.12,phase,base);
                        case 3 -> {
                            square(consumer,entry,entryType,cx-camX,y1+height*0.50,cz-camZ,radius*1.12,Math.PI/4,base);
                            square(consumer,entry,entryType,cx-camX,y1+height*0.18,cz-camZ,radius*0.92,-phase,base);
                        }
                        default -> souls(consumer,entry,entryType,cx-camX,y1,cz-camZ,height,radius,phase);
                    }
                }
            } finally {
                matrixStackType.getMethod("method_22909").invoke(matrices); // pop
            }
        } catch (Throwable error) {
            logError("render", error);
        }
    }

    private static Object findCrosshairTarget(Object mc, Object player) {
        try {
            Object hit = publicField(mc, "field_1765");
            if (hit != null && hit.getClass().getName().equals("net.minecraft.class_3966")) {
                Class<?> hitType = Class.forName("net.minecraft.class_3966");
                Object entity = hitType.getMethod("method_17782").invoke(hit);
                if (isLiving(entity) && entity != player) return entity;
            }
        } catch (Throwable ignored) {}
        try {
            Object entity = publicField(mc, "field_1692");
            if (isLiving(entity) && entity != player) return entity;
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean isLiving(Object entity) {
        return entity != null && hasMethod(entity, "method_6032") && hasMethod(entity, "method_5829");
    }
    private static boolean alive(Object entity) {
        if (!isLiving(entity)) return false;
        try {
            Class<?> livingType=Class.forName("net.minecraft.class_1309");
            return ((Number)livingType.getMethod("method_6032").invoke(entity)).floatValue() > 0.0f;
        } catch (Throwable error) { return false; }
    }
    private static boolean canSee(Object player,Object target) {
        try {
            Class<?> entityType=Class.forName("net.minecraft.class_1297");
            return (Boolean)entityType.getMethod("method_6057",entityType).invoke(player,target);
        } catch (Throwable ignored) { return true; }
    }
    private static int hurtTime(Object entity) {
        try { return ((Number)declaredOrPublicField(entity,"field_6235")).intValue(); }
        catch (Throwable ignored) { return 0; }
    }

    private static void drawBox(Object vc,Object entry,Class<?> entryType,double x1,double y1,double z1,double x2,double y2,double z2,int color)throws Exception {
        double[][] p={{x1,y1,z1},{x2,y1,z1},{x2,y1,z2},{x1,y1,z2},{x1,y2,z1},{x2,y2,z1},{x2,y2,z2},{x1,y2,z2}};
        int[][] e={{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for(int i=0;i<e.length;i++) line(vc,entry,entryType,p[e[i][0]],p[e[i][1]],color);
    }
    private static void ring(Object vc,Object entry,Class<?> entryType,double cx,double y,double cz,double r,double phase,int count,int color)throws Exception {
        double[] aPoint=new double[3],bPoint=new double[3];
        for(int i=0;i<count;i++) {
            double a=phase+i*Math.PI*2.0/count,b=phase+(i+1)*Math.PI*2.0/count;
            aPoint[0]=cx+Math.cos(a)*r; aPoint[1]=y; aPoint[2]=cz+Math.sin(a)*r;
            bPoint[0]=cx+Math.cos(b)*r; bPoint[1]=y; bPoint[2]=cz+Math.sin(b)*r;
            line(vc,entry,entryType,aPoint,bPoint,color);
        }
    }
    private static void square(Object vc,Object entry,Class<?> entryType,double cx,double y,double cz,double r,double rotation,int color)throws Exception {
        double[][] pts=new double[4][3];
        for(int i=0;i<4;i++){double a=rotation+Math.PI/4+i*Math.PI/2;pts[i]=new double[]{cx+Math.cos(a)*r,y,cz+Math.sin(a)*r};}
        for(int i=0;i<4;i++)line(vc,entry,entryType,pts[i],pts[(i+1)%4],color);
    }
    private static void souls(Object vc,Object entry,Class<?> entryType,double cx,double minY,double cz,double h,double r,double phase)throws Exception {
        int samples=ZenithVisuals.enabled("fps_optimizer")?14:20;
        double[] aPoint=new double[3],bPoint=new double[3];
        for(int arm=0;arm<3;arm++) {
            double offset=arm*Math.PI*2.0/3.0;
            for(int i=0;i<samples;i++) {
                double t=i/(double)(samples-1),a=phase+offset+t*Math.PI*3.2,rr=r*(0.30+0.70*t);
                double y=minY+h*(0.08+0.84*t)+Math.sin(a*1.5)*0.10;
                double x=cx+Math.cos(a)*rr,z=cz+Math.sin(a)*rr;
                int color=ZenithVisuals.accent();double s=0.024+0.022*t;
                aPoint[0]=x-s;aPoint[1]=y;aPoint[2]=z;bPoint[0]=x+s;bPoint[1]=y;bPoint[2]=z;
                line(vc,entry,entryType,aPoint,bPoint,color);
                aPoint[0]=x;aPoint[1]=y-s;aPoint[2]=z;bPoint[0]=x;bPoint[1]=y+s;bPoint[2]=z;
                line(vc,entry,entryType,aPoint,bPoint,color);
            }
        }
    }

    private static void line(Object consumer,Object entry,Class<?> entryType,double[] a,double[] b,int color)throws Exception {
        double dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2],len=Math.sqrt(dx*dx+dy*dy+dz*dz);
        float nx=len>1.0E-6?(float)(dx/len):0f,ny=len>1.0E-6?(float)(dy/len):1f,nz=len>1.0E-6?(float)(dz/len):0f;
        ensureLineMethods(entryType);
        // The interface methods are resolved once, not hundreds of times per frame.
        float width=0.82f;
        int argb=(ZenithVisuals.targetEspOpacity&255)<<24 | (color&0x00FFFFFF);
        Object v=vertexMethod.invoke(consumer,entry,(float)a[0],(float)a[1],(float)a[2]);
        colorMethod.invoke(v,argb); normalMethod.invoke(v,entry,nx,ny,nz); lineWidthMethod.invoke(v,width);
        Object v2=vertexMethod.invoke(consumer,entry,(float)b[0],(float)b[1],(float)b[2]);
        colorMethod.invoke(v2,argb); normalMethod.invoke(v2,entry,nx,ny,nz); lineWidthMethod.invoke(v2,width);
    }

    private static void ensureLineMethods(Class<?> entryType)throws Exception {
        if(vertexMethod!=null && vertexEntryType==entryType)return;
        synchronized(WorldTargetEsp.class){
            if(vertexMethod!=null && vertexEntryType==entryType)return;
            Class<?> vertexType=Class.forName("net.minecraft.class_4588");
            vertexMethod=vertexType.getMethod("method_56824",entryType,float.class,float.class,float.class);
            colorMethod=vertexType.getMethod("method_39415",int.class);
            normalMethod=vertexType.getMethod("method_60831",entryType,float.class,float.class,float.class);
            // Required by Minecraft 1.21.11's RenderLayers.LINES vertex format.
            lineWidthMethod=vertexType.getMethod("method_75298",float.class);
            vertexEntryType=entryType;
        }
    }

    private static Object call(Object o,String name)throws Exception {
        Method method=o.getClass().getMethod(name);
        if(!method.canAccess(o))method.setAccessible(true);
        return method.invoke(o);
    }
    private static Object publicField(Object o,String name)throws Exception {
        Class<?> owner = Class.forName("net.minecraft.class_310");
        try {
            return owner.getField(name).get(o);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // MinecraftClient.player is private in 1.21.11; getField() alone throws and
            // used to abort the entire ESP render callback before it could find a target.
            Field field = owner.getDeclaredField(name);
            if (!field.canAccess(o)) field.setAccessible(true);
            return field.get(o);
        }
    }
    private static Object declaredOrPublicField(Object o,String name)throws Exception {
        return declaredOrPublicField(o.getClass(),o,name);
    }
    private static Object declaredOrPublicField(Class<?> owner,Object o,String name)throws Exception {
        try { return owner.getField(name).get(o); }
        catch (NoSuchFieldException ignored) { Field f=owner.getDeclaredField(name); if(!f.canAccess(o))f.setAccessible(true); return f.get(o); }
    }
    private static double number(Object o,String name)throws Exception { return ((Number)declaredOrPublicField(o,name)).doubleValue(); }
    private static boolean hasMethod(Object o,String name) { if(o==null)return false;for(Method m:o.getClass().getMethods())if(m.getName().equals(name))return true;return false; }
    private static void logError(String where,Throwable error) {
        long now=System.currentTimeMillis(); if(now-lastErrorLog<2000)return;lastErrorLog=now;
        Throwable root=error;while(root instanceof InvocationTargetException && ((InvocationTargetException)root).getCause()!=null)root=((InvocationTargetException)root).getCause();
        System.err.println("[ZenithVisuals] Target ESP "+where+" failed: "+root.getClass().getName()+": "+root.getMessage());
        if(Boolean.getBoolean("zenithvisuals.debugEsp"))root.printStackTrace();
    }
}
