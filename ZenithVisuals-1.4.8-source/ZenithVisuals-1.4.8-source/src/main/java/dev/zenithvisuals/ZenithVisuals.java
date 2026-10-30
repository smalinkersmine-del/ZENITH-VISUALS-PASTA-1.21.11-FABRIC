package dev.zenithvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.class_2960;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ZenithVisuals implements ClientModInitializer {
    public static final String MOD_ID = "zenithvisuals";
    public static final String VERSION = "1.4.8";
    public static final List<Feature> FEATURES = new ArrayList<>();
    public static final int[] THEMES = {0xFFB56CFF,0xFF68D8FF,0xFFFF70C7,0xFF92E86D,0xFFFFB45E,0xFF9B9BFF};
    public static final String[] THEME_NAMES = {"Фиолетовый","Лёд","Роза","Лайм","Янтарь","Барвинок"};
    public static final String[] FONT_NAMES = {"Стандартный", "Ровный", "Акцентный"};
    public static final String[] FONT_IDS = {"default", "uniform", "default"};
    public static final String[] GRADIENT_NAMES = {"Фиолетовый сон","Северное сияние","Закат","Океан","Карамель","Лаймовое свечение"};
    public static final int[][] GRADIENTS = {
        {0xFFB56CFF,0xFF68D8FF},{0xFF57E6C1,0xFF8878FF},{0xFFFF6B8A,0xFFFFC36A},
        {0xFF4DA8FF,0xFF65F0E0},{0xFFFF70C7,0xFF9B82FF},{0xFF92E86D,0xFF22C7A5}
    };
    public static final String[] ESP_MODES = {"Circle1","Circle2","Old","New","Souls"};
    public static int themeIndex=1, fontIndex=0, gradientIndex=0, gradientSpeed=1, activeStyle=1, targetEspMode=0;
    public static int targetEspSize=100, targetEspSpeed=1, targetEspOpacity=220, targetEspDisplayTime=5;
    public static boolean menuOpen=false;
    public static String discordAppId="";
    public static String discordState="In the main menu";
    private static boolean rightShiftWasDown=false;
    private static Path configPath;
    private static final AtomicBoolean hudInstalled=new AtomicBoolean(false);
    // Fullbright uses the vanilla gamma option. The original user value is kept
    // separately so disabling the feature restores the exact previous setting.
    private static volatile double fullbrightRestoreGamma=Double.NaN;
    private static volatile boolean fullbrightRestoreCaptured=false;
    private static volatile boolean fullbrightApplied=false;
    private static volatile long lastFullbrightErrorAt=0L;
    private static final Map<String,Boolean> SETTINGS=new LinkedHashMap<>();
    private static final Map<String,int[]> WIDGETS=new LinkedHashMap<>();
    public static final String[] EXTRA_FEATURE_IDS={"clock","coordinates","direction","biome_info","dimension_info","ping_display","potion_status","keystrokes","cps_counter","session_timer","memory_meter","fps_graph","item_counter","durability_alert","weather_display","world_time","light_level","compass","jump_counter","notification_feed","speed","cooldowns","staff_active"};
    public static final String[] WIDGET_IDS={"watermark","active_modules","armor_status","movement_info","keybinds","crosshair",
        "clock","coordinates","direction","biome_info","dimension_info","ping_display","potion_status","keystrokes","cps_counter","session_timer","memory_meter","fps_graph","item_counter","durability_alert","weather_display","world_time","light_level","compass","jump_counter","notification_feed","speed","cooldowns","staff_active"};
    public static final long SESSION_STARTED=System.currentTimeMillis();
    public static volatile int leftCps=0,rightCps=0,jumpCount=0;
    private static long cpsWindowStart=System.currentTimeMillis();
    private static int leftClicks=0,rightClicks=0;
    private static boolean lastLeft=false,lastRight=false,lastOnGround=true;
    private static final Deque<Toast> TOASTS=new ArrayDeque<>();

    static {
        FEATURES.add(new Feature("target_esp","Target ESP","Visuals",true,84));
        FEATURES.add(new Feature("fullbright","Fullbright","Visuals",false,0));
        FEATURES.add(new Feature("watermark","Logo","HUD",true,0));
        FEATURES.add(new Feature("active_modules","Active Modules","HUD",true,0));
        FEATURES.add(new Feature("armor_status","Armor","HUD",true,0));
        FEATURES.add(new Feature("movement_info","Movement Info","HUD",false,0));
        FEATURES.add(new Feature("keybinds","Keybinds","HUD",true,0));
        FEATURES.add(new Feature("crosshair","Custom Crosshair","HUD",true,0));
        FEATURES.add(new Feature("fps_optimizer","FPS Optimizer","Utility",true,0));
        FEATURES.add(new Feature("clock","Clock","HUD",false,0));
        FEATURES.add(new Feature("coordinates","Coordinates","HUD",false,0));
        FEATURES.add(new Feature("direction","Direction","HUD",false,0));
        FEATURES.add(new Feature("biome_info","Biome Info","Utility",false,0));
        FEATURES.add(new Feature("dimension_info","Dimension Info","Utility",false,0));
        FEATURES.add(new Feature("ping_display","Ping Display","Utility",false,0));
        FEATURES.add(new Feature("potion_status","Effects","HUD",true,0));
        FEATURES.add(new Feature("keystrokes","Keystrokes","HUD",true,0));
        FEATURES.add(new Feature("cps_counter","CPS Counter","HUD",false,0));
        FEATURES.add(new Feature("session_timer","Timer Indicator","HUD",true,0));
        FEATURES.add(new Feature("memory_meter","Memory Meter","Utility",false,0));
        FEATURES.add(new Feature("fps_graph","FPS Graph","Utility",true,0));
        FEATURES.add(new Feature("item_counter","Item Counter","Utility",false,0));
        FEATURES.add(new Feature("durability_alert","Durability Alert","Utility",false,0));
        FEATURES.add(new Feature("weather_display","Weather Display","Utility",false,0));
        FEATURES.add(new Feature("world_time","World Time","Utility",false,0));
        FEATURES.add(new Feature("light_level","Light Level","Utility",false,0));
        FEATURES.add(new Feature("compass","Compass","Utility",false,0));
        FEATURES.add(new Feature("jump_counter","Jump Counter","Utility",true,0));
        FEATURES.add(new Feature("notification_feed","Notification Feed","HUD",false,0));
        FEATURES.add(new Feature("speed","Speed","HUD",true,0));
        FEATURES.add(new Feature("cooldowns","Cooldown","HUD",true,0));
        FEATURES.add(new Feature("staff_active","Staff Active","HUD",true,0));
        FEATURES.add(new Feature("discord_presence","Discord Presence","Client",false,0));
        SETTINGS.put("watermark.fps",true); SETTINGS.put("active.showKeys",false); SETTINGS.put("active.background",true);
        SETTINGS.put("gradient.enabled",false); SETTINGS.put("armor.durability",true); SETTINGS.put("armor.background",true);
        SETTINGS.put("movement.coords",true); SETTINGS.put("movement.speed",true); SETTINGS.put("movement.facing",false);
        SETTINGS.put("target_esp.onlyVisible",false); SETTINGS.put("target_esp.hurtColor",true); SETTINGS.put("target_esp.throughWalls",false);
        SETTINGS.put("target_esp.box",true); SETTINGS.put("target_esp.ring",true);
        SETTINGS.put("keybinds.compact",false);
        SETTINGS.put("crosshair.outline",true); SETTINGS.put("crosshair.dot",false);
        SETTINGS.put("widget.background",true); SETTINGS.put("widget.gradient",false);
        SETTINGS.put("potion.showDuration",true); SETTINGS.put("item.showCount",true);
        SETTINGS.put("durability.onlyLow",true); SETTINGS.put("keystrokes.showSpace",true);
        SETTINGS.put("notification.sound",false); SETTINGS.put("coordinates.nether",false);
    }

    @Override public void onInitializeClient(){loadConfig();registerClientTick();registerHud();WorldTargetEsp.register();if(enabled("discord_presence"))DiscordRpc.startIfConfigured();System.out.println("[ZenithVisuals] Loaded "+VERSION+" for Minecraft 1.21.11");}

    public static final class Feature {
        public final String id,name,category; public boolean enabled; public int key;
        Feature(String id,String name,String category,boolean enabled,int key){this.id=id;this.name=name;this.category=category;this.enabled=enabled;this.key=key;}
    }
    public static Feature feature(String id){for(Feature f:FEATURES)if(f.id.equals(id))return f;return null;}
    public static int accent(){return THEMES[Math.floorMod(themeIndex,THEMES.length)];}
    public static int accentSoft(){return (accent()&0x00FFFFFF)|0x55000000;}
    public static boolean enabled(String id){Feature f=feature(id);return f!=null&&f.enabled;}
    public static boolean setting(String id){return SETTINGS.getOrDefault(id,false);}
    public static void toggleSetting(String id){SETTINGS.put(id,!setting(id));}
    public static Set<String> settingKeys(){return Collections.unmodifiableSet(SETTINGS.keySet());}
    public static int widgetValue(String id,int part,int fallback){int[] v=WIDGETS.get(id);return v==null?fallback:v[Math.max(0,Math.min(3,part))];}
    public static void setWidget(String id,int x,int y,int w,int h){WIDGETS.put(id,new int[]{x,y,w,h});}
    public static int defaultWidgetValue(String id,int part,int sw,int sh){
        int dx=10,dy=10,dw=158,dh=30;
        switch(id){
            case "watermark"->{dx=16;dy=10;dw=318;dh=32;}
            case "active_modules"->{dx=sw-156;dy=79;dw=156;dh=124;}
            case "armor_status"->{dx=16;dy=74;dw=166;dh=92;}
            case "movement_info"->{dx=16;dy=174;dw=166;dh=58;}
            case "keybinds"->{dx=16;dy=176;dw=145;dh=75;}
            case "crosshair"->{dx=sw/2-6;dy=sh/2-6;dw=12;dh=12;}
            default->{
                int index=-1;for(int i=0;i<EXTRA_FEATURE_IDS.length;i++)if(EXTRA_FEATURE_IDS[i].equals(id)){index=i;break;}
                if(index>=0){dx=12+(index%4)*166;dy=270+(index/4)*38;dw=154;dh=30;
                    if(id.equals("potion_status")){dx=sw-165;dy=sh-175;dw=165;dh=88;}
                    if(id.equals("keystrokes")){dx=2;dy=sh-330;dw=128;dh=88;}
                    if(id.equals("fps_graph")){dx=2;dy=sh-47;dw=155;dh=45;}
                    if(id.equals("jump_counter")){dx=2;dy=sh-89;dw=155;dh=34;}
                    if(id.equals("notification_feed"))dh=54;
                    if(id.equals("coordinates")){dx=12;dy=sh-38;dw=184;dh=28;}
                    if(id.equals("speed")){dx=12;dy=sh-72;dw=126;dh=28;}
                    if(id.equals("session_timer")){dx=sw/2-72;dy=sh-38;dw=144;dh=28;}
                    if(id.equals("cooldowns")){dx=sw-178;dy=158;dw=166;dh=82;}
                    if(id.equals("staff_active")){dx=sw-178;dy=58;dw=166;dh=88;}}
            }
        }
        return switch(part){case 0->dx;case 1->dy;case 2->dw;default->dh;};
    }
    public static void resetWidgets(){WIDGETS.clear();}
    public static synchronized void notify(String message){
        TOASTS.addFirst(new Toast(message,System.currentTimeMillis()));
        while(TOASTS.size()>5)TOASTS.removeLast();
    }
    public static synchronized List<Toast> notifications(){long now=System.currentTimeMillis();TOASTS.removeIf(t->now-t.createdAt>4500);return new ArrayList<>(TOASTS);}
    public static final class Toast{public final String message;public final long createdAt;Toast(String message,long createdAt){this.message=message;this.createdAt=createdAt;}}
    public static String sessionTime(){long sec=Math.max(0,(System.currentTimeMillis()-SESSION_STARTED)/1000);return String.format(Locale.ROOT,"%02d:%02d:%02d",sec/3600,(sec/60)%60,sec%60);}
    /** Static accent colour. HUD and ESP intentionally avoid animated RGB gradients. */
    public static int gradientColor(int index,int total){ return accent(); }
    public static int blend(int a,int b,double t){t=Math.max(0,Math.min(1,t));int aa=(a>>>24)&255,ar=(a>>>16)&255,ag=(a>>>8)&255,ab=a&255;int ba=(b>>>24)&255,br=(b>>>16)&255,bg=(b>>>8)&255,bb=b&255;return (mix(aa,ba,t)<<24)|(mix(ar,br,t)<<16)|(mix(ag,bg,t)<<8)|mix(ab,bb,t);}
    private static int mix(int a,int b,double t){return (int)Math.round(a+(b-a)*t);}

    private static void registerClientTick(){
        try{
            Class<?> events=Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");Object event=events.getField("END_CLIENT_TICK").get(null);
            Class<?> listener=Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents$EndTick");
            Object proxy=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(p,m,args)->{if(m.getName().equals("onEndTick")&&args!=null&&args.length>0)tick(args[0]);return null;});
            Class<?> eventType=Class.forName("net.fabricmc.fabric.api.event.Event");eventType.getMethod("register",Object.class).invoke(event,proxy);
        }catch(Throwable e){System.err.println("[ZenithVisuals] Could not register tick listener: "+e.getClass().getSimpleName());}
    }
    private static void tick(Object client){
        try{
            Object window=invoke(client,"method_22683");boolean rightShift=isKeyDown(window,344);
            if(rightShift&&!rightShiftWasDown){menuOpen=!menuOpen;invoke(client,"method_1507",new Class<?>[]{Class.forName("net.minecraft.class_437")},menuOpen?new ClickGui():null);saveConfig();}
            rightShiftWasDown=rightShift;
            for(Feature f:FEATURES){if(f.key<=0)continue;boolean down=isKeyDown(window,f.key),was=KEY_STATES.getOrDefault(f.id,false);if(down&&!was){f.enabled=!f.enabled;notify((f.enabled?"Включено: ":"Выключено: ")+f.name);saveConfig();}KEY_STATES.put(f.id,down);}
            updateLocalCounters(window,client);
            updateFullbright(client);
        }catch(Throwable ignored){}
    }
    /** Applies full gamma while enabled and restores the exact previous gamma when disabled. */
    private static void updateFullbright(Object client){
        try{
            Object options=readFieldDeep(client,"field_1690");
            Object gammaOption=options.getClass().getMethod("method_42473").invoke(options);
            Field valueField=fieldDeep(gammaOption.getClass(),"field_37868");
            Method setValue=gammaOption.getClass().getMethod("method_41748",Object.class);
            if(enabled("fullbright")){
                if(!fullbrightRestoreCaptured){
                    if(!Double.isFinite(fullbrightRestoreGamma)){
                        Object previous=valueField.get(gammaOption);
                        fullbrightRestoreGamma=previous instanceof Number?((Number)previous).doubleValue():1.0;
                    }
                    fullbrightRestoreCaptured=true;
                    saveConfig();
                }
                Object current=valueField.get(gammaOption);
                if(!(current instanceof Number)||Math.abs(((Number)current).doubleValue()-100.0)>0.001){
                    // Let Minecraft run the option callback, then bypass its normal 0..1 slider validator.
                    setValue.invoke(gammaOption,Double.valueOf(1.0));
                    valueField.set(gammaOption,Double.valueOf(100.0));
                }
                fullbrightApplied=true;
            }else if(fullbrightApplied||fullbrightRestoreCaptured){
                double restore=Double.isFinite(fullbrightRestoreGamma)?fullbrightRestoreGamma:1.0;
                setValue.invoke(gammaOption,Double.valueOf(restore));
                valueField.set(gammaOption,Double.valueOf(restore));
                fullbrightRestoreGamma=Double.NaN;
                fullbrightRestoreCaptured=false;
                fullbrightApplied=false;
                saveConfig();
            }
        }catch(Throwable error){
            long now=System.currentTimeMillis();
            if(now-lastFullbrightErrorAt>5000){lastFullbrightErrorAt=now;System.err.println("[ZenithVisuals] Fullbright could not update gamma: "+error.getClass().getSimpleName());}
        }
    }
    private static Field fieldDeep(Class<?> owner,String name)throws NoSuchFieldException{
        Class<?> type=owner;
        while(type!=null){try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}
        throw new NoSuchFieldException(owner.getName()+"."+name);
    }
    private static Object readFieldDeep(Object owner,String name)throws Exception{return fieldDeep(owner.getClass(),name).get(owner);}

    private static void updateLocalCounters(Object window,Object client){
        try{
            Object handle=invoke(window,"method_4490");long ptr=((Number)handle).longValue();
            Class<?> glfw=Class.forName("org.lwjgl.glfw.GLFW");Method mouse=glfw.getMethod("glfwGetMouseButton",long.class,int.class);
            boolean left=((Number)mouse.invoke(null,ptr,0)).intValue()==1, right=((Number)mouse.invoke(null,ptr,1)).intValue()==1;
            if(left&&!lastLeft)leftClicks++;if(right&&!lastRight)rightClicks++;lastLeft=left;lastRight=right;
            long now=System.currentTimeMillis();if(now-cpsWindowStart>=1000){leftCps=leftClicks;rightCps=rightClicks;leftClicks=rightClicks=0;cpsWindowStart=now;}
        }catch(Throwable ignored){}
        try{Object player=readPlayer(client);if(player!=null){boolean onGround=(Boolean)player.getClass().getMethod("method_24828").invoke(player);if(lastOnGround&&!onGround){double vy=0;try{Object velocity=player.getClass().getMethod("method_18798").invoke(player);vy=((Number)velocity.getClass().getField("field_1351").get(velocity)).doubleValue();}catch(Throwable ignored){}if(vy>0.05)jumpCount++;}lastOnGround=onGround;}}catch(Throwable ignored){}
    }
    private static Object readPlayer(Object client){try{Class<?> owner=Class.forName("net.minecraft.class_310");Field f=owner.getDeclaredField("field_1724");if(!f.canAccess(client))f.setAccessible(true);return f.get(client);}catch(Throwable ignored){return null;}}
    private static final Map<String,Boolean> KEY_STATES=new HashMap<>();
    private static boolean isKeyDown(Object window,int key){try{Class<?> u=Class.forName("net.minecraft.class_3675"),w=Class.forName("net.minecraft.class_1041");return(Boolean)u.getMethod("method_15987",w,int.class).invoke(null,window,key);}catch(Throwable ignored){return false;}}
    public static boolean keyDown(Object window,int key){return isKeyDown(window,key);}

    private static void registerHud(){
        if(!hudInstalled.compareAndSet(false,true))return;
        try{
            Class<?> registry=Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry"),elementType=Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement");
            class_2960 id=class_2960.method_60655(MOD_ID,"main_hud");Object element=Proxy.newProxyInstance(elementType.getClassLoader(),new Class<?>[]{elementType},(p,m,args)->{if(m.getName().equals("render")&&args!=null&&args.length>=1)HudRenderer.render(args[0]);return null;});
            registry.getMethod("addLast",class_2960.class,elementType).invoke(null,id,element);
        }catch(Throwable e){System.err.println("[ZenithVisuals] HUD layer unavailable: "+e.getClass().getSimpleName());}
    }
    private static Object invoke(Object o,String method)throws Exception{return o.getClass().getMethod(method).invoke(o);}
    private static Object invoke(Object o,String method,Class<?>[] types,Object...values)throws Exception{return o.getClass().getMethod(method,types).invoke(o,values);}

    private static void loadConfig(){
        Properties p=new Properties();try{
            Path dir=Paths.get(System.getProperty("user.dir","."),"config");Files.createDirectories(dir);configPath=dir.resolve("zenithvisuals.properties");if(Files.exists(configPath))try(InputStream in=Files.newInputStream(configPath)){p.load(in);}
            fullbrightRestoreCaptured=Boolean.parseBoolean(p.getProperty("fullbright.restoreCaptured","false"));
            try{fullbrightRestoreGamma=Double.parseDouble(p.getProperty("fullbright.restoreGamma","NaN"));}catch(NumberFormatException ignored){fullbrightRestoreGamma=Double.NaN;}
            themeIndex=Integer.parseInt(p.getProperty("theme","1"));fontIndex=Integer.parseInt(p.getProperty("font","0"));gradientIndex=Integer.parseInt(p.getProperty("gradient","0"));gradientSpeed=Integer.parseInt(p.getProperty("gradientSpeed","1"));activeStyle=Integer.parseInt(p.getProperty("activeStyle","0"));targetEspMode=Integer.parseInt(p.getProperty("targetEspMode","0"));targetEspSize=Integer.parseInt(p.getProperty("targetEspSize","100"));targetEspSpeed=Integer.parseInt(p.getProperty("targetEspSpeed","1"));targetEspOpacity=Integer.parseInt(p.getProperty("targetEspOpacity","220"));targetEspDisplayTime=Integer.parseInt(p.getProperty("targetEspDisplayTime","5"));discordAppId=p.getProperty("discordAppId","").trim();
            for(String key:new ArrayList<>(SETTINGS.keySet()))SETTINGS.put(key,Boolean.parseBoolean(p.getProperty("setting."+key,Boolean.toString(SETTINGS.get(key)))));
            for(String id:WIDGET_IDS){String raw=p.getProperty("widget."+id,"");if(!raw.isBlank()){String[] a=raw.split(",");if(a.length==4)try{WIDGETS.put(id,new int[]{Integer.parseInt(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2]),Integer.parseInt(a[3])});}catch(NumberFormatException ignored){}}}
            WIDGETS.remove("target_hud");
            for(Feature f:FEATURES){f.enabled=Boolean.parseBoolean(p.getProperty("feature."+f.id,Boolean.toString(f.enabled)));f.key=Integer.parseInt(p.getProperty("key."+f.id,Integer.toString(f.key)));}
            String configVersion=p.getProperty("configVersion","0");
            if(!"1.4.8".equals(configVersion)){
                // Apply the compact monochrome HUD once while preserving user toggles and positions.
                SETTINGS.put("gradient.enabled",false);
                SETTINGS.put("widget.gradient",false);
                SETTINGS.put("active.background",true);
                SETTINGS.put("active.showKeys",false);
                SETTINGS.put("widget.background",true);
                int[] oldWatermark=WIDGETS.get("watermark");
                if(oldWatermark==null || oldWatermark[2]>=420)WIDGETS.put("watermark",new int[]{16,10,318,32});
                for(Feature f:FEATURES){
                    if(f.id.equals("watermark")||f.id.equals("coordinates")||f.id.equals("speed")||f.id.equals("potion_status")||f.id.equals("keybinds")||f.id.equals("staff_active")||f.id.equals("armor_status")||f.id.equals("session_timer")||f.id.equals("cooldowns"))f.enabled=true;
                    if(f.id.equals("active_modules")||f.id.equals("movement_info"))f.enabled=false;
                }
            }
            if(!Files.exists(configPath) || !"1.4.8".equals(configVersion))saveConfig();
        }catch(Throwable e){System.err.println("[ZenithVisuals] Config load failed: "+e.getClass().getSimpleName());}
    }
    public static void saveConfig(){
        if(configPath==null)configPath=Paths.get(System.getProperty("user.dir","."),"config","zenithvisuals.properties");Properties p=new Properties();
        p.setProperty("configVersion","1.4.8");p.setProperty("fullbright.restoreCaptured",Boolean.toString(fullbrightRestoreCaptured));p.setProperty("fullbright.restoreGamma",Double.toString(fullbrightRestoreGamma));p.setProperty("theme",Integer.toString(themeIndex));p.setProperty("font",Integer.toString(fontIndex));p.setProperty("gradient",Integer.toString(gradientIndex));p.setProperty("gradientSpeed",Integer.toString(gradientSpeed));p.setProperty("activeStyle",Integer.toString(activeStyle));p.setProperty("targetEspMode",Integer.toString(targetEspMode));p.setProperty("targetEspSize",Integer.toString(targetEspSize));p.setProperty("targetEspSpeed",Integer.toString(targetEspSpeed));p.setProperty("targetEspOpacity",Integer.toString(targetEspOpacity));p.setProperty("targetEspDisplayTime",Integer.toString(targetEspDisplayTime));p.setProperty("discordAppId",discordAppId==null?"":discordAppId);
        for(Feature f:FEATURES){p.setProperty("feature."+f.id,Boolean.toString(f.enabled));p.setProperty("key."+f.id,Integer.toString(f.key));}
        for(Map.Entry<String,Boolean> e:SETTINGS.entrySet())p.setProperty("setting."+e.getKey(),Boolean.toString(e.getValue()));
        for(Map.Entry<String,int[]> e:WIDGETS.entrySet()){int[] v=e.getValue();p.setProperty("widget."+e.getKey(),v[0]+","+v[1]+","+v[2]+","+v[3]);}
        p.setProperty("# discord","Paste your Discord Developer Portal Application ID here to enable Rich Presence");
        try{Files.createDirectories(configPath.getParent());try(Writer out=Files.newBufferedWriter(configPath,StandardCharsets.ISO_8859_1)){p.store(out,"ZenithVisuals client settings");}}catch(Throwable e){System.err.println("[ZenithVisuals] Config save failed: "+e.getClass().getSimpleName());}
    }
    public static String keyName(int key){
        if(key<=0)return "NONE";if(key>=65&&key<=90)return Character.toString((char)key);if(key>=48&&key<=57)return Character.toString((char)key);
        return switch(key){case 32->"SPACE";case 256->"ESC";case 257->"ENTER";case 258->"TAB";case 259->"BACK";case 261->"DELETE";case 262->"RIGHT";case 263->"LEFT";case 264->"DOWN";case 265->"UP";case 340->"LSHIFT";case 344->"RSHIFT";case 341->"LCTRL";case 345->"RCTRL";case 342->"LALT";case 346->"RALT";default->"KEY"+key;};
    }
}
