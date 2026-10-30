package dev.zenithvisuals;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side HUD renderer. Widgets are laid out and sized by HudEditor. */
final class HudRenderer {
    private static long lastHudErrorAt;
    private static final int HUD_TEXT = 0xFFF0F1F3;
    private static final int HUD_MUTED = 0xFF9A9EA6;
    private static final int HUD_PANEL = 0xD90A0C10;
    private static final int HUD_EDGE = 0x80545B66;
    private static final int HUD_ROW = 0x55212730;
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static long headerSnapshotAt;
    private static String cachedServer = "ОДИНОЧНАЯ ИГРА";
    private static String cachedPlayerName = "ИГРОК";
    private static String cachedClock = "00:00";
    private static int cachedFps;
    private static Object cachedPotionPlayer;
    private static List<?> cachedPotionEffects = Collections.emptyList();
    private static long cachedPotionAt;
    private static Object cachedArmorOwner;
    private static List<Object> cachedArmorStacks = Collections.emptyList();
    private static long cachedArmorAt;
    private static long lastFpsSampleAt;
    private static HudSnapshot cachedHudSnapshot;
    private HudRenderer() {}

    private record HudSnapshot(Object player, Object world) {}
    private record CooldownEntry(String name, float progress) {}
    private static Object cooldownCacheOwner;
    private static long cooldownCacheAt;
    private static List<CooldownEntry> cooldownCache = Collections.emptyList();
    private static long staffCacheAt;
    private static List<String> staffCache = Collections.emptyList();

    static void render(Object graphics) {
        if (ZenithVisuals.menuOpen) return;
        try {
            Class<?> mcType=Class.forName("net.minecraft.class_310");
            Object mc=cachedMethod(mcType,"method_1551").invoke(null);
            Object currentScreen=readField(mc,"field_1755");
            if(currentScreen!=null&&!currentScreen.getClass().getName().equals("net.minecraft.class_408"))return;
            Object font=readField(mc,"field_1772");
            int sw=intCall(graphics,"method_51421",854),sh=intCall(graphics,"method_51443",480);
            int accent=ZenithVisuals.accent();
            refreshHeaderSnapshot(mc);
            if(ZenithVisuals.enabled("watermark"))drawWatermark(graphics,font,mc,sw,sh,accent);
            if(ZenithVisuals.enabled("crosshair"))drawCrosshair(graphics,sw,sh);
            if(ZenithVisuals.enabled("keybinds"))drawKeybinds(graphics,font,sw,sh,accent);
            if(ZenithVisuals.enabled("active_modules"))drawActiveModules(graphics,font,sw,sh);
            if(ZenithVisuals.enabled("armor_status"))drawArmorStatus(graphics,font,mc,sw,sh);
            if(ZenithVisuals.enabled("movement_info"))drawMovementInfo(graphics,font,mc,sw,sh);
            drawExtraWidgets(graphics,font,mc,sw,sh);
        } catch(Throwable error) {
            long now=System.currentTimeMillis();
            if(now-lastHudErrorAt>2500){
                lastHudErrorAt=now;
                Throwable root=error;while(root instanceof InvocationTargetException && ((InvocationTargetException)root).getCause()!=null)root=((InvocationTargetException)root).getCause();
                System.err.println("[ZenithVisuals] HUD render failed: "+root.getClass().getSimpleName()+": "+root.getMessage());
                if(Boolean.getBoolean("zenithvisuals.debugHud"))root.printStackTrace();
            }
        }
    }

    private static void refreshHeaderSnapshot(Object mc) {
        long now = System.currentTimeMillis();
        if (now - headerSnapshotAt < 250) return;
        headerSnapshotAt = now;
        try {
            Object info = readField(mc, "field_1690");
            Object name = info == null ? null : invokeAny(info, "method_2956", "getName");
            cachedServer = name == null ? "ОДИНОЧНАЯ ИГРА" : String.valueOf(name);
        } catch (Throwable ignored) { cachedServer = "ОДИНОЧНАЯ ИГРА"; }
        try { cachedPlayerName = playerDisplayName(readField(mc, "field_1724")); }
        catch (Throwable ignored) { cachedPlayerName = "ИГРОК"; }
        try { cachedFps = ((Number) readField(mc, "field_1738")).intValue(); }
        catch (Throwable ignored) { cachedFps = 0; }
        cachedClock = java.time.LocalTime.now().withNano(0).toString();
    }

    private static void drawWatermark(Object g,Object font,Object mc,int sw,int sh,int accent)throws Exception{
        int x=val("watermark",0,16),y=val("watermark",1,10);
        int w=Math.max(230,Math.min(340,val("watermark",2,318)));
        int h=Math.max(28,Math.min(36,val("watermark",3,32)));
        panel(g,x,y,w,h,7,HUD_PANEL,HUD_EDGE);
        rounded(g,x+7,y+5,22,22,5,0xFF20242B);
        text(g,font,"ZV",x+12,y+12,HUD_TEXT,true);
        text(g,font,"ZENITH VISUALS",x+36,y+7,HUD_TEXT,true);
        text(g,font,"CLIENT  /  1.21.11",x+36,y+19,HUD_MUTED,false);
        fill(g,x+142,y+7,x+143,y+h-7,0x66545B66);
        String meta=cachedClock+"  ·  "+trim(cachedPlayerName,12);
        text(g,font,trim(meta,Math.max(8,(w-207)/6)),x+151,y+13,0xFFD0D2D6,false);
        if(ZenithVisuals.setting("watermark.fps")){
            rounded(g,x+w-48,y+7,40,18,5,0x88343A44);
            text(g,font,cachedFps+" FPS",x+w-44,y+13,HUD_TEXT,true);
        }
    }

    private static void drawCrosshair(Object g,int sw,int sh)throws Exception{
        int x=val("crosshair",0,sw/2-6),y=val("crosshair",1,sh/2-6),w=val("crosshair",2,12),h=val("crosshair",3,12),cx=x+w/2,cy=y+h/2,arm=Math.max(2,Math.min(w,h)/3);
        if(ZenithVisuals.setting("crosshair.outline")){fill(g,cx-arm,cy-1,cx-2,cy+1,0xCC0A0A0D);fill(g,cx+2,cy-1,cx+arm,cy+1,0xCC0A0A0D);fill(g,cx-1,cy-arm,cx+1,cy-2,0xCC0A0A0D);fill(g,cx-1,cy+2,cx+1,cy+arm,0xCC0A0A0D);}
        fill(g,cx-arm,cy,cx-2,cy+1,ZenithVisuals.accent());fill(g,cx+2,cy,cx+arm,cy+1,ZenithVisuals.accent());fill(g,cx,cy-arm,cx+1,cy-2,ZenithVisuals.accent());fill(g,cx,cy+2,cx+1,cy+arm,ZenithVisuals.accent());
        if(ZenithVisuals.setting("crosshair.dot"))fill(g,cx-1,cy-1,cx+2,cy+2,ZenithVisuals.accent());
    }

    private static void drawKeybinds(Object g,Object font,int sw,int sh,int accent)throws Exception{
        List<ZenithVisuals.Feature> active=new ArrayList<>();
        for(ZenithVisuals.Feature f:ZenithVisuals.FEATURES)if(f.enabled&&f.key>0)active.add(f);
        if(active.isEmpty())return;
        int x=val("keybinds",0,10),y=val("keybinds",1,48),w=val("keybinds",2,154),h=Math.max(35,21+active.size()*15);
        panel(g,x,y,w,h,7,HUD_PANEL,HUD_EDGE);
        text(g,font,"АКТИВНЫЕ КЛАВИШИ",x+9,y+8,HUD_TEXT,true);
        fill(g,x+8,y+19,x+w-8,y+20,0x55545B66);
        int iy=y+27;
        for(ZenithVisuals.Feature f:active){
            text(g,font,trim(f.name,Math.max(8,(w-55)/6)),x+9,iy,0xFFE4E5E8,false);
            String key=ZenithVisuals.keyName(f.key);
            int kw=Math.max(24,key.length()*6+10);
            rounded(g,x+w-kw-8,iy-3,kw,12,4,HUD_ROW);
            text(g,font,key,x+w-kw-3,iy,HUD_MUTED,true);
            iy+=15;
            if(iy>y+h-4)break;
        }
    }

    private static long activeModulesCacheAt;
    private static List<ZenithVisuals.Feature> activeModulesCache=Collections.emptyList();
    private static List<ZenithVisuals.Feature> activeModulesSnapshot(){
        long now=System.currentTimeMillis();
        if(now-activeModulesCacheAt<300)return activeModulesCache;
        List<ZenithVisuals.Feature> active=new ArrayList<>();
        for(ZenithVisuals.Feature f:ZenithVisuals.FEATURES){if(!f.enabled||f.id.equals("active_modules")||f.id.equals("discord_presence"))continue;active.add(f);}
        active.sort(Comparator.comparing(f->f.name.toLowerCase(Locale.ROOT)));
        activeModulesCache=active;activeModulesCacheAt=now;return activeModulesCache;
    }
    private static void drawActiveModules(Object g,Object font,int sw,int sh)throws Exception{
        List<ZenithVisuals.Feature> active=activeModulesSnapshot();
        if(active.isEmpty())return;
        int x=val("active_modules",0,sw-166),y=val("active_modules",1,52),w=val("active_modules",2,154),h=val("active_modules",3,Math.max(42,active.size()*16+28));
        if(ZenithVisuals.setting("active.background"))panel(g,x,y,w,h,7,HUD_PANEL,HUD_EDGE);
        text(g,font,"АКТИВНЫЕ МОДУЛИ",x+9,y+8,HUD_TEXT,true);
        fill(g,x+8,y+19,x+w-8,y+20,0x55545B66);
        int row=15;
        for(int i=0;i<active.size();i++){
            ZenithVisuals.Feature f=active.get(i);int yy=y+27+i*row;
            if(yy+row>y+h-2)break;
            fill(g,x+9,yy+4,x+11,yy+6,0xFFB7BDC6);
            text(g,font,trim(f.name,Math.max(8,(w-40)/6)),x+17,yy+2,0xFFE9EAED,false);
            if(ZenithVisuals.setting("active.showKeys")&&f.key>0){
                String key=ZenithVisuals.keyName(f.key);text(g,font,key,x+w-key.length()*6-10,yy+2,HUD_MUTED,true);
            }
        }
    }

    private static void drawArmorStatus(Object g,Object font,Object mc,int sw,int sh)throws Exception{
        Object player=readField(mc,"field_1724");if(player==null)return;List<Object> stacks=armorStacks(player);
        int x=val("armor_status",0,10),y=val("armor_status",1,91),w=val("armor_status",2,158),h=val("armor_status",3,87);
        if(ZenithVisuals.setting("armor.background"))panel(g,x,y,w,h,7,HUD_PANEL,HUD_EDGE);
        text(g,font,"БРОНЯ",x+9,y+8,HUD_TEXT,true);
        fill(g,x+8,y+19,x+w-8,y+20,0x55545B66);
        String[] labels={"ШЛЕМ","НАГРУДНИК","ПОНOЖИ","БОТИНКИ"};
        for(int i=0;i<Math.min(4,stacks.size());i++){
            Object stack=stacks.get(i);if(stack==null||isEmpty(stack))continue;
            int yy=y+26+i*14,pct=durability(stack);
            text(g,font,labels[Math.min(i,3)],x+9,yy,0xFFE5E6E9,false);
            if(ZenithVisuals.setting("armor.durability")){
                String value=pct<0?"--":pct+"%";text(g,font,value,x+w-31,yy,HUD_MUTED,true);
                fill(g,x+9,yy+9,x+w-9,yy+11,0xFF343941);
                if(pct>=0)fill(g,x+9,yy+9,x+9+(w-18)*pct/100,yy+11,pct>50?0xFFB9C0CA:pct>20?0xFF9A8D78:0xFF9B7478);
            } else text(g,font,trim(stackName(stack),Math.max(6,(w-18)/6)),x+9,yy,HUD_MUTED,false);
        }
    }

    private static void drawMovementInfo(Object g,Object font,Object mc,int sw,int sh)throws Exception{
        Object player=readField(mc,"field_1724");if(player==null)return;
        int x=val("movement_info",0,10),y=val("movement_info",1,190),w=val("movement_info",2,164),h=val("movement_info",3,62);
        panel(g,x,y,w,h,7,HUD_PANEL,HUD_EDGE);
        text(g,font,"ДВИЖЕНИЕ",x+9,y+8,HUD_TEXT,true);
        fill(g,x+8,y+19,x+w-8,y+20,0x55545B66);
        int yy=y+27;
        if(ZenithVisuals.setting("movement.speed")){double bps=horizontalSpeed(player)*20.0;text(g,font,String.format(Locale.ROOT,"%.2f BPS",bps),x+9,yy,0xFFE7E8EB,true);yy+=14;}
        if(ZenithVisuals.setting("movement.coords")){double px=numberCall(player,"method_23317",0),py=numberCall(player,"method_23318",0),pz=numberCall(player,"method_23321",0);text(g,font,String.format(Locale.ROOT,"XYZ  %.0f  %.0f  %.0f",px,py,pz),x+9,yy,HUD_MUTED,false);yy+=13;}
        if(ZenithVisuals.setting("movement.facing")){float yaw=(float)numberCall(player,"method_36454",0);text(g,font,"НАПРАВЛЕНИЕ  "+direction(yaw),x+9,yy,HUD_MUTED,false);}
    }


    // Additional client-side HUD widgets. They only read local client/world state;
    // none of them sends packets or automates combat/gameplay.
    private static final Deque<Integer> FPS_HISTORY=new ArrayDeque<>();
    private static void drawExtraWidgets(Object g,Object font,Object mc,int sw,int sh)throws Exception{
        Object player=readField(mc,"field_1724");
        Object world=readField(mc,"field_1687");
        for(String id:ZenithVisuals.EXTRA_FEATURE_IDS){
            if(!ZenithVisuals.enabled(id))continue;
            int x=ZenithVisuals.widgetValue(id,0,ZenithVisuals.defaultWidgetValue(id,0,sw,sh));
            int y=ZenithVisuals.widgetValue(id,1,ZenithVisuals.defaultWidgetValue(id,1,sw,sh));
            int w=Math.max(24,ZenithVisuals.widgetValue(id,2,ZenithVisuals.defaultWidgetValue(id,2,sw,sh)));
            int h=Math.max(16,ZenithVisuals.widgetValue(id,3,ZenithVisuals.defaultWidgetValue(id,3,sw,sh)));
            try{drawExtraWidget(g,font,mc,player,world,id,x,y,w,h);}catch(Throwable widgetError){
                long now=System.currentTimeMillis();if(now-lastHudErrorAt>2500){lastHudErrorAt=now;System.err.println("[ZenithVisuals] HUD widget "+id+" failed: "+widgetError.getClass().getSimpleName());}
            }
        }
    }
    private static void drawExtraWidget(Object g,Object font,Object mc,Object player,Object world,String id,int x,int y,int w,int h)throws Exception{
        if(ZenithVisuals.setting("widget.background"))panel(g,x,y,w,h,7,HUD_PANEL,HUD_EDGE);
        String title=id.replace('_',' ').toUpperCase(Locale.ROOT),value="—";
        switch(id){
            case "clock" -> value=java.time.LocalTime.now().withNano(0).toString();
            case "coordinates" -> {if(player!=null)value=String.format(Locale.ROOT,"X %.0f  Y %.0f  Z %.0f",numberCall(player,"method_23317",0),numberCall(player,"method_23318",0),numberCall(player,"method_23321",0));}
            case "direction","compass" -> {if(player!=null){float yaw=(float)numberCall(player,"method_36454",0);value=direction(yaw)+"  ·  "+Math.round((yaw%360+360)%360)+"°";}}
            case "biome_info" -> value=biomeName(player,world);
            case "dimension_info" -> value=dimensionName(world);
            case "ping_display" -> value=pingText(mc,player);
            case "potion_status" -> {drawPotionStatus(g,font,player,x,y,w,h);return;}
            case "keystrokes" -> {drawKeystrokes(g,font,mc,x,y,w,h);return;}
            case "cps_counter" -> value="ЛКМ  "+ZenithVisuals.leftCps+"     ПКМ  "+ZenithVisuals.rightCps;
            case "session_timer" -> {title="TIMER";value=ZenithVisuals.sessionTime();}
            case "speed" -> {if(player!=null)value=String.format(Locale.ROOT,"%.2f BPS",horizontalSpeed(player)*20.0);}
            case "cooldowns" -> {drawCooldowns(g,font,player,x,y,w,h);return;}
            case "staff_active" -> {drawStaffActive(g,font,mc,x,y,w,h);return;}
            case "memory_meter" -> {Runtime rt=Runtime.getRuntime();long used=(rt.totalMemory()-rt.freeMemory())/(1024*1024);long max=rt.maxMemory()/(1024*1024);value=used+" / "+max+" МБ";}
            case "fps_graph" -> {drawFpsGraph(g,font,mc,x,y,w,h);return;}
            case "item_counter" -> value=heldItemText(player);
            case "durability_alert" -> value=durabilityText(player);
            case "weather_display" -> value=weatherText(world);
            case "world_time" -> value=worldTime(world);
            case "light_level" -> value=lightText(player,world);
            case "jump_counter" -> value=Integer.toString(ZenithVisuals.jumpCount);
            case "notification_feed" -> {drawNotifications(g,font,x,y,w,h);return;}
        }
        text(g,font,title,x+9,y+8,HUD_TEXT,true);
        fill(g,x+8,y+19,x+w-8,y+20,0x55545B66);
        text(g,font,trim(value,Math.max(8,(w-18)/6)),x+9,y+25,0xFFD0D2D6,false);
    }
    private static void drawCooldowns(Object g,Object font,Object player,int x,int y,int w,int h)throws Exception{
        if(ZenithVisuals.setting("widget.background"))panel(g,x,y,w,h,7,HUD_PANEL,HUD_EDGE);
        text(g,font,"ПЕРЕЗАРЯДКИ",x+9,y+8,HUD_TEXT,true);
        fill(g,x+8,y+19,x+w-8,y+20,0x55545B66);
        List<CooldownEntry> entries=cooldownEntries(player);
        if(entries.isEmpty()){
            text(g,font,"Нет активных КД",x+9,y+27,HUD_MUTED,false);
            return;
        }
        int rows=Math.min(entries.size(),Math.max(1,Math.min(4,(h-28)/15)));
        for(int i=0;i<rows;i++){
            CooldownEntry entry=entries.get(i);
            int yy=y+23+i*15;
            text(g,font,trim(entry.name(),Math.max(7,(w-54)/6)),x+9,yy,0xFFE5E6E9,false);
            int pct=Math.max(0,Math.min(100,Math.round(entry.progress()*100.0f)));
            text(g,font,pct+"%",x+w-34,yy,HUD_MUTED,true);
            rounded(g,x+9,yy+9,w-18,3,1,0xFF343941);
            int bar=(w-18)*pct/100;
            if(bar>0)rounded(g,x+9,yy+9,bar,3,1,0xFFB8BEC8);
        }
    }

    private static List<CooldownEntry> cooldownEntries(Object player){
        long now=System.currentTimeMillis();
        if(player==cooldownCacheOwner && now-cooldownCacheAt<120)return cooldownCache;
        cooldownCacheOwner=player;cooldownCacheAt=now;
        List<CooldownEntry> result=new ArrayList<>();
        if(player==null){cooldownCache=result;return result;}
        try{
            Object manager=invokeAny(player,"method_7357","getItemCooldownManager");
            Object inventory=readField(player,"field_7514");
            Object main=readField(inventory,"field_7547");
            if(!(main instanceof Iterable<?> items)){cooldownCache=Collections.emptyList();return cooldownCache;}
            Class<?> stackType=Class.forName("net.minecraft.class_1799");
            Method active=manager.getClass().getMethod("method_7904",stackType);
            Method fraction=manager.getClass().getMethod("method_7905",stackType,float.class);
            Method group=null;
            try{group=manager.getClass().getMethod("method_62836",stackType);}catch(Throwable ignored){}
            Set<String> seen=new HashSet<>();
            for(Object stack:items){
                if(stack==null||isEmpty(stack)||!Boolean.TRUE.equals(active.invoke(manager,stack)))continue;
                String key;
                try{key=group==null?stackName(stack):String.valueOf(group.invoke(manager,stack));}catch(Throwable ignored){key=stackName(stack);}
                if(!seen.add(key))continue;
                float value=((Number)fraction.invoke(manager,stack,0.0f)).floatValue();
                value=Math.max(0.0f,Math.min(1.0f,value));
                result.add(new CooldownEntry(stackName(stack),value));
            }
        }catch(Throwable ignored){}
        result.sort(Comparator.comparing(CooldownEntry::name,String.CASE_INSENSITIVE_ORDER));
        cooldownCache=result;
        return result;
    }

    private static void drawStaffActive(Object g,Object font,Object mc,int x,int y,int w,int h)throws Exception{
        if(ZenithVisuals.setting("widget.background"))panel(g,x,y,w,h,7,HUD_PANEL,HUD_EDGE);
        text(g,font,"АКТИВНЫЙ ПЕРСОНАЛ",x+9,y+8,HUD_TEXT,true);
        fill(g,x+8,y+19,x+w-8,y+20,0x55545B66);
        List<String> names=staffNames(mc);
        if(names.isEmpty()){
            text(g,font,"Метки ролей не найдены",x+9,y+27,HUD_MUTED,false);
            return;
        }
        int rows=Math.min(names.size(),Math.max(1,Math.min(5,(h-26)/13)));
        for(int i=0;i<rows;i++){
            int yy=y+23+i*13;
            text(g,font,trim(names.get(i),Math.max(7,(w-50)/6)),x+9,yy,0xFFE5E6E9,false);
            text(g,font,"ONLINE",x+w-43,yy,HUD_MUTED,true);
        }
        if(names.size()>rows)text(g,font,"+"+(names.size()-rows),x+w-28,y+h-11,HUD_MUTED,false);
    }

    private static List<String> staffNames(Object mc){
        long now=System.currentTimeMillis();
        if(now-staffCacheAt<1000)return staffCache;
        staffCacheAt=now;
        List<String> found=new ArrayList<>();
        try{
            Object network=invokeAny(mc,"method_1562","getNetworkHandler");
            Object entries=invokeAny(network,"method_2880","getPlayerList");
            if(entries instanceof Iterable<?> iterable){
                for(Object entry:iterable){
                    Object team;
                    try{team=invokeAny(entry,"method_2955","getScoreboardTeam");}catch(Throwable ignored){continue;}
                    if(team==null)continue;
                    Object profile=invokeAny(entry,"method_2966","getProfile");
                    String name=String.valueOf(invokeAny(profile,"getName"));
                    String teamName=String.valueOf(invokeAny(team,"method_1197","getName"));
                    String prefix="",suffix="";
                    try{prefix=textValue(invokeAny(team,"method_1144","getPrefix"));}catch(Throwable ignored){}
                    try{suffix=textValue(invokeAny(team,"method_1136","getSuffix"));}catch(Throwable ignored){}
                    if(isStaffMarker(teamName+" "+prefix+" "+suffix))found.add(name);
                }
            }
        }catch(Throwable ignored){}
        found.sort(String.CASE_INSENSITIVE_ORDER);
        staffCache=found.size()>8?new ArrayList<>(found.subList(0,8)):found;
        return staffCache;
    }

    private static String textValue(Object text){
        if(text==null)return "";
        try{return String.valueOf(invokeAny(text,"method_10851","getString"));}
        catch(Throwable ignored){return String.valueOf(text);}
    }

    private static boolean isStaffMarker(String value){
        String s=value.toLowerCase(Locale.ROOT);
        String[] markers={"staff","admin","moder","helper","owner","operator","support","админ","модер","хелпер","владел","куратор","сотрудник","помощ"};
        for(String marker:markers)if(s.contains(marker))return true;
        return false;
    }

    private static void drawPotionStatus(Object g,Object font,Object player,int x,int y,int w,int h)throws Exception{
        if(ZenithVisuals.setting("widget.background"))panel(g,x,y,w,h,7,HUD_PANEL,HUD_EDGE);
        List<?> effects=cachedPotionEffects(player);
        text(g,font,"ЭФФЕКТЫ",x+9,y+8,HUD_TEXT,true);
        fill(g,x+8,y+19,x+w-8,y+20,0x55545B66);
        if(effects.isEmpty()){text(g,font,"Нет активных эффектов",x+9,y+27,HUD_MUTED,false);return;}
        int shown=Math.min(4,effects.size());
        for(int i=0;i<shown;i++){
            Object effect=effects.get(i);String name=potionEffectName(effect);String duration="";
            if(ZenithVisuals.setting("potion.showDuration")){try{int ticks=((Number)invokeAny(effect,"method_5584","getDuration")).intValue();int sec=Math.max(0,ticks/20);duration=String.format(Locale.ROOT,"%d:%02d",sec/60,sec%60);}catch(Throwable ignored){}}
            int yy=y+27+i*13;text(g,font,trim(name,Math.max(7,(w-55)/6)),x+9,yy,0xFFE5E6E9,false);
            if(!duration.isEmpty())text(g,font,duration,x+w-39,yy,HUD_MUTED,true);
        }
    }
    private static List<?> cachedPotionEffects(Object player){
        long now=System.currentTimeMillis();
        if(player==cachedPotionPlayer && now-cachedPotionAt<250)return cachedPotionEffects;
        cachedPotionPlayer=player;cachedPotionAt=now;
        try{Object active=player==null?null:invokeAny(player,"method_6026","getActiveStatusEffects");
            cachedPotionEffects=active instanceof Collection<?> c?new ArrayList<>(c):Collections.emptyList();
        }catch(Throwable ignored){cachedPotionEffects=Collections.emptyList();}
        return cachedPotionEffects;
    }

    private static String potionEffectName(Object instance){
        try{Object type=invokeAny(instance,"method_5579","getEffectType");Class<?> textType=Class.forName("net.minecraft.class_2561");
            for(Method m:type.getClass().getMethods())if(m.getParameterCount()==0&&textType.isAssignableFrom(m.getReturnType())){Object t=m.invoke(type);if(t!=null){String name=String.valueOf(textType.getMethod("method_10851").invoke(t));if(!name.isBlank())return name;}}
        }catch(Throwable ignored){}return "Эффект";
    }

    private static void drawKeystrokes(Object g,Object font,Object mc,int x,int y,int w,int h)throws Exception{
        Object window=mc.getClass().getMethod("method_22683").invoke(mc);int cell=Math.max(14,Math.min(22,(w-18)/3));int sx=x+(w-cell*3)/2,sy=y+12;
        int[] keys={87,65,83,68};String[] labels={"W","A","S","D"};int[][] pos={{1,0},{0,1},{1,1},{2,1}};
        for(int i=0;i<4;i++){boolean down=keyDown(window,keys[i]);int bx=sx+pos[i][0]*cell,by=sy+pos[i][1]*cell;rounded(g,bx,by,cell-2,cell-2,4,down?withAlpha(ZenithVisuals.accent(),190):0xAA303545);text(g,font,labels[i],bx+cell/2-3,by+4,down?0xFFFFFFFF:0xFFD4D0DD,true);}
        if(ZenithVisuals.setting("keystrokes.showSpace")){boolean down=keyDown(window,32);rounded(g,sx,sy+cell*2,cell*3-2,7,3,down?ZenithVisuals.accent():0xAA303545);}
    }
    private static void drawFpsGraph(Object g,Object font,Object mc,int x,int y,int w,int h)throws Exception{
        int fps=0;try{fps=((Number)readField(mc,"field_1738")).intValue();}catch(Throwable ignored){}
        long now=System.currentTimeMillis();
        long samplePeriod=ZenithVisuals.enabled("fps_optimizer")?300:200;
        if(now-lastFpsSampleAt>=samplePeriod){FPS_HISTORY.addLast(Math.max(0,fps));while(FPS_HISTORY.size()>16)FPS_HISTORY.removeFirst();lastFpsSampleAt=now;}
        rounded(g,x+1,y+2,w,h,8,0x55000000);if(ZenithVisuals.setting("widget.background"))rounded(g,x,y,w,h,8,0xC9111420);
        fill(g,x+7,y+3,x+w-7,y+5,ZenithVisuals.accent());text(g,font,"FPS GRAPH",x+8,y+8,ZenithVisuals.accent(),true);text(g,font,fps+" FPS",x+w-52,y+8,0xFFF3EFF9,true);
        int base=y+h-7,top=y+24,usable=Math.max(8,base-top),n=FPS_HISTORY.size(),i=0;for(int f:FPS_HISTORY){int bx=x+8+(w-16)*i/Math.max(1,n);int bh=Math.max(2,Math.min(usable,f*usable/Math.max(60,fps*2)));fill(g,bx,base-bh,bx+2,base-bh+2,ZenithVisuals.accent());i++;}
    }
    private static void drawNotifications(Object g,Object font,int x,int y,int w,int h)throws Exception{
        List<ZenithVisuals.Toast> notes=ZenithVisuals.notifications();if(notes.isEmpty())return;
        if(ZenithVisuals.setting("widget.background"))rounded(g,x,y,w,h,8,0xC9111420);fill(g,x+7,y+3,x+w-7,y+5,ZenithVisuals.accent());text(g,font,"УВЕДОМЛЕНИЯ",x+8,y+8,ZenithVisuals.accent(),true);
        int yy=y+22;for(int i=0;i<Math.min(2,notes.size());i++){ZenithVisuals.Toast n=notes.get(i);text(g,font,trim(n.message,Math.max(8,(w-16)/6)),x+8,yy,0xFFECE8F4,false);yy+=14;}
    }
    private static String biomeName(Object player,Object world){
        try{Object pos=invokeAny(player,"method_24515","getBlockPos");Object biome=invokeAny(world,"method_22347","getBiome",new Class<?>[]{pos.getClass()},new Object[]{pos});Object key=invokeAny(biome,"method_40230","getKey");Object id=invokeAny(key,"method_29177","getValue");return String.valueOf(id);}catch(Throwable ignored){return "Биом";}
    }
    private static String dimensionName(Object world){try{Object key=invokeAny(world,"method_27983","getRegistryKey");Object id=invokeAny(key,"method_29177","getValue");return String.valueOf(id);}catch(Throwable ignored){return "Измерение";}}
    private static String pingText(Object mc,Object player){
        try{Object network=invokeAny(mc,"method_1562","getNetworkHandler");Object uuid=invokeAny(player,"method_5667","getUuid");
            for(Method m:network.getClass().getMethods())if(m.getParameterCount()==1&&m.getParameterTypes()[0].isAssignableFrom(uuid.getClass())){Object entry=m.invoke(network,uuid);if(entry!=null){for(Method e:entry.getClass().getMethods())if(e.getParameterCount()==0&&(e.getName().equals("method_2959")||e.getName().equals("getLatency"))){return e.invoke(entry)+" мс";}}}
        }catch(Throwable ignored){}return "нет данных";
    }
    private static String potionText(Object player){try{Object effects=invokeAny(player,"method_6026","getActiveStatusEffects");if(effects instanceof Collection<?> c){if(c.isEmpty())return "Нет активных эффектов";return "Активно: "+c.size();}}catch(Throwable ignored){}return "Эффекты: —";}
    private static String heldItemText(Object player){try{Object stack=invokeAny(player,"method_6079","getMainHandStack");Object name=invokeAny(stack,"method_7964","getName");String label=String.valueOf(invokeAny(name,"getString"));int count=((Number)invokeAny(stack,"method_7947","getCount")).intValue();return trim(label,13)+" × "+count;}catch(Throwable ignored){return "Предмет в руке";}}
    private static String durabilityText(Object player){List<Object> stacks=armorStacks(player);int min=101;for(Object stack:stacks){if(stack==null||isEmpty(stack))continue;int d=durability(stack);if(d>=0)min=Math.min(min,d);}return min==101?"Броня не надета":"Минимум: "+min+"%";}
    private static String weatherText(Object world){try{Object rain=invokeAny(world,"method_8510","isRaining");return(Boolean)rain?"Дождь":"Ясно";}catch(Throwable ignored){return "Погода";}}
    private static String worldTime(Object world){try{long t=((Number)invokeAny(world,"method_8532","getTimeOfDay")).longValue();long day=(t+6000)%24000;if(day<0)day+=24000;long mins=day*60/1000;return String.format(Locale.ROOT,"%02d:%02d",mins/60,mins%60);}catch(Throwable ignored){return "Время мира";}}
    private static String lightText(Object player,Object world){try{Object pos=invokeAny(player,"method_24515","getBlockPos");for(Method m:world.getClass().getMethods())if(m.getParameterCount()==1&&m.getParameterTypes()[0].isAssignableFrom(pos.getClass())&&(m.getName().equals("method_8314")||m.getName().equals("getLightLevel")))return "Уровень: "+m.invoke(world,pos);}catch(Throwable ignored){}return "Освещение: —";}
    private static Object invokeAny(Object object,String...names)throws Exception{for(String n:names)try{return cachedMethod(object.getClass(),n).invoke(object);}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(Arrays.toString(names));}
    private static Object invokeAny(Object object,String first,String second,Class<?>[] types,Object[] args)throws Exception{for(String n:new String[]{first,second})try{return cachedMethod(object.getClass(),n,types).invoke(object,args);}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(first+"/"+second);}
    private static volatile Method keyPressedMethod;
    private static volatile Class<?> keyWindowType;
    private static boolean keyDown(Object window,int key){
        try{
            Method method=keyPressedMethod;
            if(method==null){synchronized(HudRenderer.class){method=keyPressedMethod;if(method==null){Class<?> u=Class.forName("net.minecraft.class_3675");keyWindowType=Class.forName("net.minecraft.class_1041");method=u.getMethod("method_15987",keyWindowType,int.class);method.setAccessible(true);keyPressedMethod=method;}}}
            return(Boolean)method.invoke(null,window,key);
        }catch(Throwable ignored){return false;}
    }

    private static List<Object> armorStacks(Object player){
        long now=System.currentTimeMillis();
        if(player==cachedArmorOwner && now-cachedArmorAt<250)return cachedArmorStacks;
        cachedArmorOwner=player;cachedArmorAt=now;
        try{
            Method getter=findNoArgMethod(player.getClass(),"method_5661","getArmorItems");
            Object value=getter==null?null:getter.invoke(player);
            if(value instanceof Iterable<?> it){List<Object> out=new ArrayList<>(4);for(Object o:it)out.add(o);Collections.reverse(out);cachedArmorStacks=out;return cachedArmorStacks;}
        }catch(Throwable ignored){}
        cachedArmorStacks=Collections.emptyList();return cachedArmorStacks;
    }
    private static boolean isEmpty(Object stack){try{return(Boolean)cachedMethod(stack.getClass(),"method_7960").invoke(stack);}catch(Throwable ignored){return false;}}
    private static int durability(Object stack){try{int max=((Number)cachedMethod(stack.getClass(),"method_7936").invoke(stack)).intValue();int damage=((Number)cachedMethod(stack.getClass(),"method_7919").invoke(stack)).intValue();if(max<=0)return -1;return Math.max(0,Math.min(100,(max-damage)*100/max));}catch(Throwable ignored){return -1;}}
    private static String stackName(Object stack){try{Object text=cachedMethod(stack.getClass(),"method_7964").invoke(stack);return String.valueOf(cachedMethod(text.getClass(),"getString").invoke(text));}catch(Throwable ignored){return String.valueOf(stack);}}
    private static String playerDisplayName(Object player){try{Object t=cachedMethod(player.getClass(),"method_5477").invoke(player);return String.valueOf(cachedMethod(t.getClass(),"getString").invoke(t));}catch(Throwable ignored){return "ИГРОК";}}
    private static double horizontalSpeed(Object player){try{Object v=player.getClass().getMethod("method_18798").invoke(player);double vx=numberField(v,"field_1352"),vz=numberField(v,"field_1350");return Math.sqrt(vx*vx+vz*vz);}catch(Throwable ignored){return 0;}}
    private static double numberField(Object obj,String name)throws Exception{return((Number)readField(obj,name)).doubleValue();}
    private static double numberCall(Object obj,String method,double fallback){try{return((Number)cachedMethod(obj.getClass(),method).invoke(obj)).doubleValue();}catch(Throwable ignored){return fallback;}}
    private static String direction(float yaw){float a=(yaw%360+360)%360;if(a<22.5||a>=337.5)return"S";if(a<67.5)return"SW";if(a<112.5)return"W";if(a<157.5)return"NW";if(a<202.5)return"N";if(a<247.5)return"NE";if(a<292.5)return"E";return"SE";}

    private static int val(String id,int part,int fallback){return ZenithVisuals.widgetValue(id,part,fallback);}
    private static void circle(Object g,int cx,int cy,int r,int color,int thickness)throws Exception{for(int i=0;i<72;i++){double a=Math.PI*2*i/72.0;int x=cx+(int)Math.round(Math.cos(a)*r),y=cy+(int)Math.round(Math.sin(a)*r);fill(g,x,y,x+thickness,y+thickness,color);}}
    private static void line(Object g,int x0,int y0,int x1,int y1,int color)throws Exception{int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,err=dx+dy;while(true){fill(g,x0,y0,x0+2,y0+2,color);if(x0==x1&&y0==y1)break;int e2=2*err;if(e2>=dy){err+=dy;x0+=sx;}if(e2<=dx){err+=dx;y0+=sy;}}}
    private static int withAlpha(int color,int a){return(color&0x00FFFFFF)|(Math.max(0,Math.min(255,a))<<24);}
    private static final Class<?> TEXT_TYPE=loadClass("net.minecraft.class_2561");
    private static final Class<?> DRAW_CONTEXT_TYPE=loadClass("net.minecraft.class_332");
    private static final Class<?> TEXT_RENDERER_TYPE=loadClass("net.minecraft.class_327");
    private static Class<?> loadClass(String name){try{return Class.forName(name);}catch(Throwable error){throw new ExceptionInInitializerError(error);}}
    private static void text(Object g,Object font,String value,int x,int y,int color,boolean bold)throws Exception{
        Object text=cachedMethod(TEXT_TYPE,"method_43470",String.class).invoke(null,value);
        text=TextFonts.apply(text,ZenithVisuals.fontIndex,bold);
        cachedMethod(DRAW_CONTEXT_TYPE,"method_27535",TEXT_RENDERER_TYPE,TEXT_TYPE,int.class,int.class,int.class).invoke(g,font,text,x,y,color);
    }
    private static void gradientStrip(Object g,int x,int y,int w,int h)throws Exception{
        // One draw call instead of up to 96 animated colour quads per strip.
        if(w>0&&h>0)fill(g,x,y,x+w,y+h,ZenithVisuals.accent());
    }
    private static void fill(Object g,int x1,int y1,int x2,int y2,int color)throws Exception{
        if(x2<=x1||y2<=y1)return;
        cachedMethod(DRAW_CONTEXT_TYPE,"method_25294",int.class,int.class,int.class,int.class,int.class).invoke(g,x1,y1,x2,y2,color);
    }
    private static void rounded(Object g,int x,int y,int w,int h,int r,int color)throws Exception{
        if(w<=0||h<=0)return;
        r=Math.max(0,Math.min(r,Math.min(w,h)/2));
        if(r<=0){fill(g,x,y,x+w,y+h,color);return;}
        int baseAlpha=(color>>>24)&255;
        for(int row=0;row<h;row++){
            int cornerRow=row<r?row:(row>=h-r?h-1-row:-1);
            double inset=0.0;
            if(cornerRow>=0){double dy=r-cornerRow-0.5;inset=r-Math.sqrt(Math.max(0.0,r*(double)r-dy*dy));}
            int leftFull=(int)Math.ceil(inset-1.0E-9);
            int rightFull=(int)Math.floor(w-inset+1.0E-9);
            double leftCoverage=Math.max(0.0,Math.min(1.0,leftFull-inset));
            double rightCoverage=Math.max(0.0,Math.min(1.0,(w-inset)-rightFull));
            if(leftCoverage>0.001&&leftFull>0)fill(g,x+leftFull-1,y+row,x+leftFull,y+row+1,(color&0x00FFFFFF)|((int)Math.round(baseAlpha*leftCoverage)<<24));
            if(rightFull>leftFull)fill(g,x+leftFull,y+row,x+rightFull,y+row+1,color);
            if(rightCoverage>0.001&&rightFull<w)fill(g,x+rightFull,y+row,x+rightFull+1,y+row+1,(color&0x00FFFFFF)|((int)Math.round(baseAlpha*rightCoverage)<<24));
        }
    }
    private static int roundedInset(int radius,int row){
        double dy=radius-row-0.5;
        return Math.max(0,Math.min(radius,(int)Math.round(radius-Math.sqrt(Math.max(0.0,radius*radius-dy*dy)))));
    }
    private static void panel(Object g,int x,int y,int w,int h,int radius,int background,int border)throws Exception{
        rounded(g,x,y,w,h,radius,border);
        if(w>2&&h>2)rounded(g,x+1,y+1,w-2,h-2,Math.max(0,radius-1),background);
    }
    private static int intCall(Object o,String m,int fallback){try{return((Number)cachedMethod(o.getClass(),m).invoke(o)).intValue();}catch(Throwable ignored){return fallback;}}
    private static Object readField(Object o,String n)throws Exception{return cachedField(o.getClass(),n).get(o);}
    private static boolean hasMethod(Object o,String n){if(o==null)return false;try{cachedMethod(o.getClass(),n);return true;}catch(Throwable ignored){return false;}}
    private static Method cachedMethod(Class<?> owner,String name,Class<?>...parameters)throws NoSuchMethodException{
        String key=owner.getName()+"#"+name+Arrays.toString(parameters);Method method=METHOD_CACHE.get(key);if(method!=null)return method;
        method=owner.getMethod(name,parameters);method.setAccessible(true);Method previous=METHOD_CACHE.putIfAbsent(key,method);return previous==null?method:previous;
    }
    private static Field cachedField(Class<?> owner,String name)throws NoSuchFieldException{
        String key=owner.getName()+"#"+name;Field field=FIELD_CACHE.get(key);if(field!=null)return field;
        Class<?> current=owner;while(current!=null){try{field=current.getDeclaredField(name);field.setAccessible(true);break;}catch(NoSuchFieldException ignored){current=current.getSuperclass();}}
        if(field==null)throw new NoSuchFieldException(owner.getName()+"."+name);
        Field previous=FIELD_CACHE.putIfAbsent(key,field);return previous==null?field:previous;
    }
    private static Method findNoArgMethod(Class<?> owner,String...names){
        for(String name:names)try{return cachedMethod(owner,name);}catch(NoSuchMethodException ignored){}
        return null;
    }
    private static List<Object> cachedArmorStacks(Object player){return armorStacks(player);}
    private static String trim(String s,int max){if(max<2)return s.length()>max?s.substring(0,max):s;return s.length()<=max?s:s.substring(0,max-1)+"…";}
}
