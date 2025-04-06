package com.vicmatskiv.pointblank.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class SoundRegistry {
   public static final DeferredRegister<SoundEvent> SOUNDS;
   private static final Map<String, Supplier<SoundEvent>> registeredSoundEvents;
   public static Supplier<SoundEvent> HIT_HEADSHOT;
   public static Supplier<SoundEvent> HIT_LIGHT;
   public static Supplier<SoundEvent> HIT_HEAVY;
   public static Supplier<SoundEvent> CRAFTING_COMPLETED;
   public static Supplier<SoundEvent> CRAFTING_IN_PROGRESS;
   public static Supplier<SoundEvent> CRAFTING_ITEM_SELECTED;
   public static Supplier<SoundEvent> ATTACHMENT_ADDED;
   public static Supplier<SoundEvent> ATTACHMENT_REMOVED;
   public static Supplier<SoundEvent> RELOAD;
   public static Supplier<SoundEvent> M1911A1;
   public static Supplier<SoundEvent> GLOCK17;
   public static Supplier<SoundEvent> LAMG;
   public static Supplier<SoundEvent> AA12;
   public static Supplier<SoundEvent> M4A1;
   public static Supplier<SoundEvent> STAR15;
   public static Supplier<SoundEvent> G36C;
   public static Supplier<SoundEvent> G36K;
   public static Supplier<SoundEvent> M4SOPMODII;
   public static Supplier<SoundEvent> M16A1;
   public static Supplier<SoundEvent> AUG;
   public static Supplier<SoundEvent> AK47;
   public static Supplier<SoundEvent> AK74;
   public static Supplier<SoundEvent> AK12;
   public static Supplier<SoundEvent> UMP45;
   public static Supplier<SoundEvent> MP5;
   public static Supplier<SoundEvent> MP7;
   public static Supplier<SoundEvent> MP7A2DEVGRU;
   public static Supplier<SoundEvent> TMP;
   public static Supplier<SoundEvent> M950;
   public static Supplier<SoundEvent> G41;
   public static Supplier<SoundEvent> WA2000;
   public static Supplier<SoundEvent> SL8;
   public static Supplier<SoundEvent> MK14EBR;
   public static Supplier<SoundEvent> WA2000_ACTIONBACK;
   public static Supplier<SoundEvent> WA2000_ACTIONFORWARD;
   public static Supplier<SoundEvent> M590;
   public static Supplier<SoundEvent> PUMP1;
   public static Supplier<SoundEvent> PUMP2;
   public static Supplier<SoundEvent> FOLEY_START;
   public static Supplier<SoundEvent> FOLEY_END;
   public static Supplier<SoundEvent> SHELL;
   public static Supplier<SoundEvent> SHELL_CHAMBER;
   public static Supplier<SoundEvent> M4_ACTIONBACK;
   public static Supplier<SoundEvent> M4_ACTIONFORWARD;
   public static Supplier<SoundEvent> M4_MAGHIT;
   public static Supplier<SoundEvent> M4_MAGIN;
   public static Supplier<SoundEvent> M4_MAGOUT;
   public static Supplier<SoundEvent> M4_MAGOUT_THROW;
   public static Supplier<SoundEvent> M4_MAGOUT_THROW2;
   public static Supplier<SoundEvent> M4_PRESS;
   public static Supplier<SoundEvent> M4_HIT;
   public static Supplier<SoundEvent> XM3;
   public static Supplier<SoundEvent> L96A1;
   public static Supplier<SoundEvent> BOLTBACK;
   public static Supplier<SoundEvent> BOLTFORWARD;
   public static Supplier<SoundEvent> BULLETINSERT;
   public static Supplier<SoundEvent> MP5_ACTIONSLAP;
   public static Supplier<SoundEvent> MP5_MAGOUT;
   public static Supplier<SoundEvent> MP5_MAGIN;
   public static Supplier<SoundEvent> MP5_ACTIONBACK;
   public static Supplier<SoundEvent> MP5_ACTIONUP;
   public static Supplier<SoundEvent> MP5_BOLTCLOSE;
   public static Supplier<SoundEvent> RIFLE_MAGIN;
   public static Supplier<SoundEvent> RIFLE_MAGOUT;
   public static Supplier<SoundEvent> C14;
   public static Supplier<SoundEvent> GM6LYNX;
   public static Supplier<SoundEvent> GM6LYNX_ACTIONBACK;
   public static Supplier<SoundEvent> GM6LYNX_ACTIONFORWARD;
   public static Supplier<SoundEvent> GM6LYNX_MAGIN;
   public static Supplier<SoundEvent> GM6LYNX_MAGOUT;
   public static Supplier<SoundEvent> M870;
   public static Supplier<SoundEvent> SPAS12;
   public static Supplier<SoundEvent> AK_MAGIN;
   public static Supplier<SoundEvent> AK_MAGOUT;
   public static Supplier<SoundEvent> AK_ACTIONBACK;
   public static Supplier<SoundEvent> AK_ACTIONFORWARD;
   public static Supplier<SoundEvent> SCARL;
   public static Supplier<SoundEvent> XM7;
   public static Supplier<SoundEvent> LMG_BULLETCHAIN;
   public static Supplier<SoundEvent> LMG_COVERUP;
   public static Supplier<SoundEvent> LMG_COVERDOWN;
   public static Supplier<SoundEvent> LMG_MAGOUT;
   public static Supplier<SoundEvent> LMG_MAGIN;
   public static Supplier<SoundEvent> PISTOL_HIT;
   public static Supplier<SoundEvent> PISTOL_MAGIN;
   public static Supplier<SoundEvent> PISTOL_MAGOUT;
   public static Supplier<SoundEvent> PISTOL_SLIDE;
   public static Supplier<SoundEvent> DBSHOTGUN_OPEN;
   public static Supplier<SoundEvent> DBSHOTGUN_CLOSE;
   public static Supplier<SoundEvent> CITORICXS;
   public static Supplier<SoundEvent> HS12;
   public static Supplier<SoundEvent> MINIGUN_FOLEY;
   public static Supplier<SoundEvent> MINIGUN_UNLOAD;
   public static Supplier<SoundEvent> MINIGUN_LOAD;
   public static Supplier<SoundEvent> MINIGUNCHARGEUP;
   public static Supplier<SoundEvent> MINIGUNWINDDOWN;
   public static Supplier<SoundEvent> M134MINIGUN;
   public static Supplier<SoundEvent> LOCKING;
   public static Supplier<SoundEvent> LOCKED;
   public static Supplier<SoundEvent> IGNITE;
   public static Supplier<SoundEvent> LAUNCHER;
   public static Supplier<SoundEvent> AT4;
   public static Supplier<SoundEvent> MGL_SHOOT;
   public static Supplier<SoundEvent> MGL_OPEN;
   public static Supplier<SoundEvent> MGL_UNLOAD;
   public static Supplier<SoundEvent> MGL_LOAD;
   public static Supplier<SoundEvent> MGL_CLOSE;
   public static Supplier<SoundEvent> UAR10;
   public static Supplier<SoundEvent> DRAW;
   public static Supplier<SoundEvent> INSPECT_UP;
   public static Supplier<SoundEvent> INSPECT_FOLEY;
   public static Supplier<SoundEvent> INSPECT_TURN;
   public static Supplier<SoundEvent> INSPECT_DOWN;
   public static Supplier<SoundEvent> PISTOL_DRAW;
   public static Supplier<SoundEvent> PISTOL_INSPECTUP;
   public static Supplier<SoundEvent> PISTOL_INSPECTFOLEY;
   public static Supplier<SoundEvent> PISTOL_INSPECTTURN;
   public static Supplier<SoundEvent> PISTOL_INSPECTDOWN;
   public static Supplier<SoundEvent> LAUNCHER_DRAW;
   public static Supplier<SoundEvent> LAUNCHER_RELOADEND;
   public static Supplier<SoundEvent> LAUNCHER_RELOADSTART;
   public static Supplier<SoundEvent> SMAW_LOAD;
   public static Supplier<SoundEvent> SMAW_TURN;
   public static Supplier<SoundEvent> SMAW_UNLOAD;
   public static Supplier<SoundEvent> AT4_COVER;
   public static Supplier<SoundEvent> AT4_LEVER;
   public static Supplier<SoundEvent> AK_SAFETY;
   public static Supplier<SoundEvent> AK_DRAW;
   public static Supplier<SoundEvent> M9;
   public static Supplier<SoundEvent> PISTOL_MAGOUT_FAST;
   public static Supplier<SoundEvent> REVOLVER_OPEN;
   public static Supplier<SoundEvent> REVOLVER_CLOSE;
   public static Supplier<SoundEvent> REVOLVER_SHELLSOUT;
   public static Supplier<SoundEvent> REVOLVER_SPEEDLOADER;
   public static Supplier<SoundEvent> CLICK;
   public static Supplier<SoundEvent> RHINO;
   public static Supplier<SoundEvent> MK48;
   public static Supplier<SoundEvent> M249;
   public static Supplier<SoundEvent> P30L;
   public static Supplier<SoundEvent> RO635;
   public static Supplier<SoundEvent> SCARL_PRESS;
   public static Supplier<SoundEvent> DEAGLE_MAGOUT;
   public static Supplier<SoundEvent> DEAGLE_MAGIN;
   public static Supplier<SoundEvent> DEAGLE_SLIDE;
   public static Supplier<SoundEvent> DESERTEAGLE;
   public static Supplier<SoundEvent> PRINTER_ON;
   public static Supplier<SoundEvent> PRINTER_OFF;
   public static Supplier<SoundEvent> VECTOR_MAGIN;
   public static Supplier<SoundEvent> VECTOR_PRESS;
   public static Supplier<SoundEvent> VECTOR;
   public static Supplier<SoundEvent> P90_MAGOUT;
   public static Supplier<SoundEvent> P90_MAGIN;
   public static Supplier<SoundEvent> P90;
   public static Supplier<SoundEvent> BALLISTA;
   public static Supplier<SoundEvent> BALLISTA_MAGOUT;
   public static Supplier<SoundEvent> BALLISTA_MAGIN;
   public static Supplier<SoundEvent> BALLISTA_BOLTBACK;
   public static Supplier<SoundEvent> BALLISTA_BOLTFORWARD;
   public static Supplier<SoundEvent> XM7_MAGOUT;
   public static Supplier<SoundEvent> XM7_MAGIN;
   public static Supplier<SoundEvent> XM7_MAGOUTFAST;
   public static Supplier<SoundEvent> XM7_CHARGEBACK;
   public static Supplier<SoundEvent> XM7_CHARGEFORWARD;
   public static Supplier<SoundEvent> M4A1_SILENCED;
   public static Supplier<SoundEvent> M4SOPMODII_SILENCED;
   public static Supplier<SoundEvent> M16A1_SILENCED;
   public static Supplier<SoundEvent> HK416;
   public static Supplier<SoundEvent> HK416_SILENCED;
   public static Supplier<SoundEvent> SCARL_UNSILENCED;
   public static Supplier<SoundEvent> XM7_UNSILENCED;
   public static Supplier<SoundEvent> AUG_SILENCED;
   public static Supplier<SoundEvent> G41_SILENCED;
   public static Supplier<SoundEvent> AK47_SILENCED;
   public static Supplier<SoundEvent> AK12_SILENCED;
   public static Supplier<SoundEvent> AK74_SILENCED;
   public static Supplier<SoundEvent> MP5_SILENCED;
   public static Supplier<SoundEvent> RO635_SILENCED;
   public static Supplier<SoundEvent> UMP45_UNSILENCED;
   public static Supplier<SoundEvent> VECTOR_SILENCED;
   public static Supplier<SoundEvent> P90_SILENCED;
   public static Supplier<SoundEvent> M950_SILENCED;
   public static Supplier<SoundEvent> TMP_SILENCED;
   public static Supplier<SoundEvent> LAUNCHER_OPEN;
   public static Supplier<SoundEvent> LAUNCHER_CLOSE;
   public static Supplier<SoundEvent> GLOCK_SUPPRESSED;
   public static Supplier<SoundEvent> M1911_SILENCED;
   public static Supplier<SoundEvent> P30L_SILENCED;
   public static Supplier<SoundEvent> M9_SILENCED;
   public static Supplier<SoundEvent> DESERTEAGLE_SILENCED;
   public static Supplier<SoundEvent> RHINO_SILENCED;
   public static Supplier<SoundEvent> L96A1_SILENCED;
   public static Supplier<SoundEvent> GM6LYNX_SILENCED;
   public static Supplier<SoundEvent> BALLISTA_SILENCED;
   public static Supplier<SoundEvent> UAR10_SILENCED;
   public static Supplier<SoundEvent> MK14EBR_SILENCED;
   public static Supplier<SoundEvent> SL8_SILENCED;
   public static Supplier<SoundEvent> SPAS12_SILENCED;
   public static Supplier<SoundEvent> XM29;
   public static Supplier<SoundEvent> XM29_GRENADE;
   public static Supplier<SoundEvent> GRENADE20MM;
   public static Supplier<SoundEvent> AN94;
   public static Supplier<SoundEvent> LASER;
   public static Supplier<SoundEvent> LASER_CHARGE;
   public static Supplier<SoundEvent> LASER_OPEN;
   public static Supplier<SoundEvent> LASER_MAGOUT;
   public static Supplier<SoundEvent> LASER_MAGIN;
   public static Supplier<SoundEvent> LASER_CLOSE;
   public static Supplier<SoundEvent> G3_MAGOUT;
   public static Supplier<SoundEvent> G3_MAGIN;
   public static Supplier<SoundEvent> G3;
   public static Supplier<SoundEvent> G3_SILENCED;
   public static Supplier<SoundEvent> SHOTGUN_MAGOUT;
   public static Supplier<SoundEvent> SHOTGUN_MAGIN;
   public static Supplier<SoundEvent> AR57;
   public static Supplier<SoundEvent> AA12_MAGOUT;
   public static Supplier<SoundEvent> AA12_MAGIN;
   public static Supplier<SoundEvent> PIN;
   public static Supplier<SoundEvent> THROW;

   public static Supplier<SoundEvent> register(String sound) {
      Supplier<SoundEvent> registeredSound = SOUNDS.register(sound, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("pointblank", sound)));
      registeredSoundEvents.putIfAbsent(sound, registeredSound);
      return registeredSound;
   }

   public static SoundEvent getSoundEvent(String sound) {
      Supplier<SoundEvent> registeredSoundEvent = registeredSoundEvents.get(sound);
      return registeredSoundEvent != null ? registeredSoundEvent.get() : null;
   }

   static {
      SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "pointblank");
      registeredSoundEvents = new HashMap<>();
      HIT_HEADSHOT = register("hit_headshot");
      HIT_LIGHT = register("hit_light");
      HIT_HEAVY = register("hit_heavy");
      CRAFTING_COMPLETED = register("crafting_completed");
      CRAFTING_IN_PROGRESS = register("crafting_in_progress");
      CRAFTING_ITEM_SELECTED = register("crafting_item_selected");
      ATTACHMENT_ADDED = register("attachment_added");
      ATTACHMENT_REMOVED = register("attachment_removed");
      RELOAD = register("reload");
      M1911A1 = register("m1911a1");
      GLOCK17 = register("glock17");
      LAMG = register("lamg");
      AA12 = register("aa12");
      M4A1 = register("m4a1");
      STAR15 = register("star15");
      G36C = register("g36c");
      G36K = register("g36k");
      M4SOPMODII = register("m4sopmodii");
      M16A1 = register("m16a1");
      AUG = register("aug");
      AK47 = register("ak47");
      AK74 = register("ak74");
      AK12 = register("ak12");
      UMP45 = register("ump45");
      MP5 = register("mp5");
      MP7 = register("mp7");
      MP7A2DEVGRU = register("mp7a2devgru");
      TMP = register("tmp");
      M950 = register("m950");
      G41 = register("g41");
      WA2000 = register("wa2000");
      SL8 = register("sl8");
      MK14EBR = register("mk14ebr");
      WA2000_ACTIONBACK = register("wa2000_actionback");
      WA2000_ACTIONFORWARD = register("wa2000_actionforward");
      M590 = register("m590");
      PUMP1 = register("pump1");
      PUMP2 = register("pump2");
      FOLEY_START = register("foley_start");
      FOLEY_END = register("foley_end");
      SHELL = register("shell");
      SHELL_CHAMBER = register("shell_chamber");
      M4_ACTIONBACK = register("m4_actionback");
      M4_ACTIONFORWARD = register("m4_actionforward");
      M4_MAGHIT = register("m4_maghit");
      M4_MAGIN = register("m4_magin");
      M4_MAGOUT = register("m4_magout");
      M4_MAGOUT_THROW = register("m4_magout_throw");
      M4_MAGOUT_THROW2 = register("m4_magout_throw2");
      M4_PRESS = register("m4_press");
      M4_HIT = register("m4_hit");
      XM3 = register("xm3");
      L96A1 = register("l96a1");
      BOLTBACK = register("boltback");
      BOLTFORWARD = register("boltforward");
      BULLETINSERT = register("bulletinsert");
      MP5_ACTIONSLAP = register("mp5_actionslap");
      MP5_MAGOUT = register("mp5_magout");
      MP5_MAGIN = register("mp5_magin");
      MP5_ACTIONBACK = register("mp5_actionback");
      MP5_ACTIONUP = register("mp5_actionup");
      MP5_BOLTCLOSE = register("mp5_boltclose");
      RIFLE_MAGIN = register("rifle_magin");
      RIFLE_MAGOUT = register("rifle_magout");
      C14 = register("c14");
      GM6LYNX = register("gm6lynx");
      GM6LYNX_ACTIONBACK = register("gm6lynx_actionback");
      GM6LYNX_ACTIONFORWARD = register("gm6lynx_actionforward");
      GM6LYNX_MAGIN = register("gm6lynx_magin");
      GM6LYNX_MAGOUT = register("gm6lynx_magout");
      M870 = register("m870");
      SPAS12 = register("spas12");
      AK_MAGIN = register("ak_magin");
      AK_MAGOUT = register("ak_magout");
      AK_ACTIONBACK = register("ak_actionback");
      AK_ACTIONFORWARD = register("ak_actionforward");
      SCARL = register("scarl");
      XM7 = register("xm7");
      LMG_BULLETCHAIN = register("lmg_bulletchain");
      LMG_COVERUP = register("lmg_coverup");
      LMG_COVERDOWN = register("lmg_coverdown");
      LMG_MAGOUT = register("lmg_magout");
      LMG_MAGIN = register("lmg_magin");
      PISTOL_HIT = register("pistol_hit");
      PISTOL_MAGIN = register("pistol_magin");
      PISTOL_MAGOUT = register("pistol_magout");
      PISTOL_SLIDE = register("pistol_slide");
      DBSHOTGUN_OPEN = register("dbshotgun_open");
      DBSHOTGUN_CLOSE = register("dbshotgun_close");
      CITORICXS = register("citoricxs");
      HS12 = register("hs12");
      MINIGUN_FOLEY = register("minigun_foley");
      MINIGUN_UNLOAD = register("minigun_unload");
      MINIGUN_LOAD = register("minigun_load");
      MINIGUNCHARGEUP = register("minigunchargeup");
      MINIGUNWINDDOWN = register("minigunwinddown");
      M134MINIGUN = register("m134minigun");
      LOCKING = register("locking");
      LOCKED = register("locked");
      IGNITE = register("ignite");
      LAUNCHER = register("launcher");
      AT4 = register("at4");
      MGL_SHOOT = register("mgl_shoot");
      MGL_OPEN = register("mgl_open");
      MGL_UNLOAD = register("mgl_unload");
      MGL_LOAD = register("mgl_load");
      MGL_CLOSE = register("mgl_close");
      UAR10 = register("uar10");
      DRAW = register("draw");
      INSPECT_UP = register("inspect_up");
      INSPECT_FOLEY = register("inspect_foley");
      INSPECT_TURN = register("inspect_turn");
      INSPECT_DOWN = register("inspect_down");
      PISTOL_DRAW = register("pistol_draw");
      PISTOL_INSPECTUP = register("pistol_inspectup");
      PISTOL_INSPECTFOLEY = register("pistol_inspectfoley");
      PISTOL_INSPECTTURN = register("pistol_inspectturn");
      PISTOL_INSPECTDOWN = register("pistol_inspectdown");
      LAUNCHER_DRAW = register("launcher_draw");
      LAUNCHER_RELOADEND = register("launcher_reloadend");
      LAUNCHER_RELOADSTART = register("launcher_reloadstart");
      SMAW_LOAD = register("smaw_load");
      SMAW_TURN = register("smaw_turn");
      SMAW_UNLOAD = register("smaw_unload");
      AT4_COVER = register("at4_cover");
      AT4_LEVER = register("at4_lever");
      AK_SAFETY = register("ak_safety");
      AK_DRAW = register("ak_draw");
      M9 = register("m9");
      PISTOL_MAGOUT_FAST = register("pistol_magout_fast");
      REVOLVER_OPEN = register("revolver_open");
      REVOLVER_CLOSE = register("revolver_close");
      REVOLVER_SHELLSOUT = register("revolver_shellsout");
      REVOLVER_SPEEDLOADER = register("revolver_speedloader");
      CLICK = register("click");
      RHINO = register("rhino");
      MK48 = register("mk48");
      M249 = register("m249");
      P30L = register("p30l");
      RO635 = register("ro635");
      SCARL_PRESS = register("scarl_press");
      DEAGLE_MAGOUT = register("deagle_magout");
      DEAGLE_MAGIN = register("deagle_magin");
      DEAGLE_SLIDE = register("deagle_slide");
      DESERTEAGLE = register("deserteagle");
      PRINTER_ON = register("printer_on");
      PRINTER_OFF = register("printer_off");
      VECTOR_MAGIN = register("vector_magin");
      VECTOR_PRESS = register("vector_press");
      VECTOR = register("vector");
      P90_MAGOUT = register("p90_magout");
      P90_MAGIN = register("p90_magin");
      P90 = register("p90");
      BALLISTA = register("ballista");
      BALLISTA_MAGOUT = register("ballista_magout");
      BALLISTA_MAGIN = register("ballista_magin");
      BALLISTA_BOLTBACK = register("ballista_boltback");
      BALLISTA_BOLTFORWARD = register("ballista_boltforward");
      XM7_MAGOUT = register("xm7_magout");
      XM7_MAGIN = register("xm7_magin");
      XM7_MAGOUTFAST = register("xm7_magoutfast");
      XM7_CHARGEBACK = register("xm7_chargeback");
      XM7_CHARGEFORWARD = register("xm7_chargeforward");
      M4A1_SILENCED = register("m4a1_silenced");
      M4SOPMODII_SILENCED = register("m4sopmodii_silenced");
      M16A1_SILENCED = register("m16a1_silenced");
      HK416 = register("hk416");
      HK416_SILENCED = register("hk416_silenced");
      SCARL_UNSILENCED = register("scarl_unsilenced");
      XM7_UNSILENCED = register("xm7_unsilenced");
      AUG_SILENCED = register("aug_silenced");
      G41_SILENCED = register("g41_silenced");
      AK47_SILENCED = register("ak47_silenced");
      AK12_SILENCED = register("ak12_silenced");
      AK74_SILENCED = register("ak74_silenced");
      MP5_SILENCED = register("mp5_silenced");
      RO635_SILENCED = register("ro635_silenced");
      UMP45_UNSILENCED = register("ump45_unsilenced");
      VECTOR_SILENCED = register("vector_silenced");
      P90_SILENCED = register("p90_silenced");
      M950_SILENCED = register("m950_silenced");
      TMP_SILENCED = register("tmp_silenced");
      LAUNCHER_OPEN = register("launcher_open");
      LAUNCHER_CLOSE = register("launcher_close");
      GLOCK_SUPPRESSED = register("glock_suppressed");
      M1911_SILENCED = register("m1911_silenced");
      P30L_SILENCED = register("p30l_silenced");
      M9_SILENCED = register("m9_silenced");
      DESERTEAGLE_SILENCED = register("deserteagle_silenced");
      RHINO_SILENCED = register("rhino_silenced");
      L96A1_SILENCED = register("l96a1_silenced");
      GM6LYNX_SILENCED = register("gm6lynx_silenced");
      BALLISTA_SILENCED = register("ballista_silenced");
      UAR10_SILENCED = register("uar10_silenced");
      MK14EBR_SILENCED = register("mk14ebr_silenced");
      SL8_SILENCED = register("sl8_silenced");
      SPAS12_SILENCED = register("spas12_silenced");
      XM29 = register("xm29");
      XM29_GRENADE = register("xm29_grenade");
      GRENADE20MM = register("grenade20mm");
      AN94 = register("an94");
      LASER = register("laser");
      LASER_CHARGE = register("laser_charge");
      LASER_OPEN = register("laser_open");
      LASER_MAGOUT = register("laser_magout");
      LASER_MAGIN = register("laser_magin");
      LASER_CLOSE = register("laser_close");
      G3_MAGOUT = register("g3_magout");
      G3_MAGIN = register("g3_magin");
      G3 = register("g3");
      G3_SILENCED = register("g3_silenced");
      SHOTGUN_MAGOUT = register("shotgun_magout");
      SHOTGUN_MAGIN = register("shotgun_magin");
      AR57 = register("ar57");
      AA12_MAGOUT = register("aa12_magout");
      AA12_MAGIN = register("aa12_magin");
      PIN = register("pin");
      THROW = register("throw");
   }
}
