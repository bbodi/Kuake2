/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package lwjake2.game;

import lwjake2.Defines;
import lwjake2.Globals;
import lwjake2.qcommon.Com;
import lwjake2.util.Lib;
import lwjake2.util.QuakeFile;

public class GameSave {

    public static void CreateEdicts() {
        GameBase.g_edicts = new edict_t[GameBase.game.maxentities];
        for (int i = 0; i < GameBase.game.maxentities; i++)
            GameBase.g_edicts[i] = new edict_t(i);
    }

    public static void CreateClients() {
        GameBase.game.clients = new gclient_t[GameBase.game.maxclients];
        for (int i = 0; i < GameBase.game.maxclients; i++)
            GameBase.game.clients[i] = new gclient_t(i);

    }

    private static String preloadclasslist [] = 
    {		
    	"jake2.game.PlayerWeapon",
    	"jake2.game.AIAdapter",
		"jake2.game.Cmd",
		"jake2.game.EdictFindFilter",
		"jake2.game.EdictIterator",
		"jake2.game.EndianHandler",
		"jake2.game.EntBlockedAdapter",
		"jake2.game.EntDieAdapter",
		"jake2.game.EntDodgeAdapter",
		"jake2.game.EntInteractAdapter",
		"jake2.game.EntPainAdapter",
		"jake2.game.EntThinkAdapter",
		"jake2.game.EntTouchAdapter",
		"jake2.game.EntUseAdapter",
		"jake2.game.GameAI",
		"jake2.game.GameBase",
		"jake2.game.GameChase",
		"jake2.game.GameCombat",
		"jake2.game.GameFunc",
		"jake2.game.GameMisc",
		"jake2.game.GameSVCmds",
		"jake2.game.GameSave",
		"jake2.game.GameSpawn",
		"jake2.game.GameTarget",
		"jake2.game.GameTrigger",
		"jake2.game.GameTurret",
		"jake2.game.GameUtil",
		"jake2.game.GameWeapon",
		"jake2.game.Info",
		"jake2.game.ItemDropAdapter",
		"jake2.game.ItemUseAdapter",
		"jake2.game.Monster",
		"jake2.game.PlayerClient",
		"jake2.game.PlayerHud",
		"jake2.game.PlayerTrail",
		"jake2.game.PlayerView",
		"jake2.game.SuperAdapter",
		"jake2.game.monsters.M_Actor",
		"jake2.game.monsters.M_Berserk",
		"jake2.game.monsters.M_Boss2",
		"jake2.game.monsters.M_Boss3",
		"jake2.game.monsters.M_Boss31",
		"jake2.game.monsters.M_Boss32",
		"jake2.game.monsters.M_Brain",
		"jake2.game.monsters.M_Chick",
		"jake2.game.monsters.M_Flash",
		"jake2.game.monsters.M_Flipper",
		"jake2.game.monsters.M_Float",
		"jake2.game.monsters.M_Flyer",
		"jake2.game.monsters.M_Gladiator",
		"jake2.game.monsters.M_Gunner",
		"jake2.game.monsters.M_Hover",
		"jake2.game.monsters.M_Infantry",
		"jake2.game.monsters.M_Insane",
		"jake2.game.monsters.M_Medic",
		"jake2.game.monsters.M_Mutant",
		"jake2.game.monsters.M_Parasite",
		"jake2.game.monsters.M_Player",
		"jake2.game.monsters.M_Soldier",
		"jake2.game.monsters.M_Supertank",
		"jake2.game.monsters.M_Tank",
		"jake2.game.GameItems",
		// DANGER! init as last, when all adatpers are != null
		"jake2.game.GameItemList"
    };
    
    /**
     * InitGame
     * 
     * This will be called when the dll is first loaded, which only happens when
     * a new game is started or a save game is loaded. 
     */
    public static void InitGame() {
        GameBase.gi.dprintf("==== InitGame ====\n");

        // preload all classes to register the adapters
        for ( int n=0; n < preloadclasslist.length; n++)
        {
        	try
			{
        		Class.forName(preloadclasslist[n]);
			}
        	catch(Exception e)
			{
        		Com.DPrintf("error loading class: " + e.getMessage());
			}
        }
        
        
        GameBase.gun_x = GameBase.gi.cvar("gun_x", "0", 0);
        GameBase.gun_y = GameBase.gi.cvar("gun_y", "0", 0);
        GameBase.gun_z = GameBase.gi.cvar("gun_z", "0", 0);

        //FIXME: sv_ prefix are wrong names for these variables 
        GameBase.sv_rollspeed = GameBase.gi.cvar("sv_rollspeed", "200", 0);
        GameBase.sv_rollangle = GameBase.gi.cvar("sv_rollangle", "2", 0);
        GameBase.sv_maxvelocity = GameBase.gi.cvar("sv_maxvelocity", "2000", 0);
        GameBase.sv_gravity = GameBase.gi.cvar("sv_gravity", "800", 0);

        // noset vars
        Globals.dedicated = GameBase.gi.cvar("dedicated", "0",
                Defines.CVAR_NOSET);

        // latched vars
        GameBase.sv_cheats = GameBase.gi.cvar("cheats", "0",
                Defines.CVAR_SERVERINFO | Defines.CVAR_LATCH);
        GameBase.gi.cvar("gamename", Defines.GAMEVERSION,
                Defines.CVAR_SERVERINFO | Defines.CVAR_LATCH);
        GameBase.gi.cvar("gamedate", Globals.__DATE__, Defines.CVAR_SERVERINFO
                | Defines.CVAR_LATCH);

        GameBase.maxclients = GameBase.gi.cvar("maxclients", "4",
                Defines.CVAR_SERVERINFO | Defines.CVAR_LATCH);
        GameBase.maxspectators = GameBase.gi.cvar("maxspectators", "4",
                Defines.CVAR_SERVERINFO);
        GameBase.deathmatch = GameBase.gi.cvar("deathmatch", "0",
                Defines.CVAR_LATCH);
        GameBase.coop = GameBase.gi.cvar("coop", "0", Defines.CVAR_LATCH);
        GameBase.skill = GameBase.gi.cvar("skill", "0", Defines.CVAR_LATCH);
        GameBase.maxentities = GameBase.gi.cvar("maxentities", "1024",
                Defines.CVAR_LATCH);

        // change anytime vars
        GameBase.dmflags = GameBase.gi.cvar("dmflags", "0",
                Defines.CVAR_SERVERINFO);
        GameBase.fraglimit = GameBase.gi.cvar("fraglimit", "0",
                Defines.CVAR_SERVERINFO);
        GameBase.timelimit = GameBase.gi.cvar("timelimit", "0",
                Defines.CVAR_SERVERINFO);
        GameBase.password = GameBase.gi.cvar("password", "",
                Defines.CVAR_USERINFO);
        GameBase.spectator_password = GameBase.gi.cvar("spectator_password",
                "", Defines.CVAR_USERINFO);
        GameBase.needpass = GameBase.gi.cvar("needpass", "0",
                Defines.CVAR_SERVERINFO);
        GameBase.filterban = GameBase.gi.cvar("filterban", "1", 0);

        GameBase.g_select_empty = GameBase.gi.cvar("g_select_empty", "0",
                Defines.CVAR_ARCHIVE);

        GameBase.run_pitch = GameBase.gi.cvar("run_pitch", "0.002", 0);
        GameBase.run_roll = GameBase.gi.cvar("run_roll", "0.005", 0);
        GameBase.bob_up = GameBase.gi.cvar("bob_up", "0.005", 0);
        GameBase.bob_pitch = GameBase.gi.cvar("bob_pitch", "0.002", 0);
        GameBase.bob_roll = GameBase.gi.cvar("bob_roll", "0.002", 0);

        // flood control
        GameBase.flood_msgs = GameBase.gi.cvar("flood_msgs", "4", 0);
        GameBase.flood_persecond = GameBase.gi.cvar("flood_persecond", "4", 0);
        GameBase.flood_waitdelay = GameBase.gi.cvar("flood_waitdelay", "10", 0);

        // dm map list
        GameBase.sv_maplist = GameBase.gi.cvar("sv_maplist", "", 0);

        // items
        GameItems.InitItems();

        GameBase.game.helpmessage1 = "";
        GameBase.game.helpmessage2 = "";

        // initialize all entities for this game
        GameBase.game.maxentities = (int) GameBase.maxentities.value;
        CreateEdicts();

        // initialize all clients for this game
        GameBase.game.maxclients = (int) GameBase.maxclients.value;

        CreateClients();

        GameBase.num_edicts = GameBase.game.maxclients + 1;
    }

    /**
     * WriteGame
     * 
     * This will be called whenever the game goes to a new level, and when the
     * user explicitly saves the game.
     * 
     * Game information include cross level data, like multi level triggers,
     * help computer info, and all client states.
     * 
     * A single player death will automatically restore from the last save
     * position.
     */
    public static void WriteGame(String filename, boolean autosave) {
        try {
            QuakeFile f;

            if (!autosave)
                PlayerClient.SaveClientData();

            f = new QuakeFile(filename, "rw");  

            GameBase.game.autosaved = autosave;
            GameBase.game.write(f);
            GameBase.game.autosaved = false;

            for (int i = 0; i < GameBase.game.maxclients; i++)
                GameBase.game.clients[i].write(f);

            Lib.fclose(f);
        } catch (Exception e) {
            e.printStackTrace();
            GameBase.gi.error("Couldn't write to " + filename);
        }
    }

    public static void ReadGame(String filename) {

        QuakeFile f = null;

        try {

            f = new QuakeFile(filename, "r");
            CreateEdicts();

            GameBase.game.load(f);

            for (int i = 0; i < GameBase.game.maxclients; i++) {
                GameBase.game.clients[i] = new gclient_t(i);
                GameBase.game.clients[i].read(f);
            }

            f.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * WriteLevel
     */
    public static void WriteLevel(String filename) {
        try {
            int i;
            edict_t ent;
            QuakeFile f;

            f = new QuakeFile(filename, "rw");
            
            // write out level_locals_t
            GameBase.level.write(f);

            // write out all the entities
            for (i = 0; i < GameBase.num_edicts; i++) {
                ent = GameBase.g_edicts[i];
                if (!ent.inuse)
                    continue;
                f.writeInt(i);
                ent.write(f);
            }

            i = -1;
            f.writeInt(-1);

            f.close();
        } catch (Exception e) {
            e.printStackTrace();
            GameBase.gi.error("Couldn't open for writing: " + filename);
        }
    }

    /**
     * ReadLevel
     * 
     * SpawnEntities will allready have been called on the level the same way it
     * was when the level was saved.
     * 
     * That is necessary to get the baselines set up identically.
     * 
     * The server will have cleared all of the world links before calling
     * ReadLevel.
     * 
     * No clients are connected yet.
     */
    public static void ReadLevel(String filename) {
        try {
            edict_t ent;

            QuakeFile f = new QuakeFile(filename, "r");

            // wipe all the entities
            CreateEdicts();

            GameBase.num_edicts = (int) GameBase.maxclients.value + 1;

            // load the level locals
            GameBase.level.read(f);

            // load all the entities
            while (true) {
                int entnum = f.readInt();
                if (entnum == -1)
                    break;

                if (entnum >= GameBase.num_edicts)
                    GameBase.num_edicts = entnum + 1;

                ent = GameBase.g_edicts[entnum];
                ent.read(f);
                ent.cleararealinks();
                GameBase.gi.linkentity(ent);
            }

            Lib.fclose(f);

            // mark all clients as unconnected
            for (int i = 0; i < GameBase.maxclients.value; i++) {
                ent = GameBase.g_edicts[i + 1];
                ent.client = GameBase.game.clients[i];
                ent.client.pers.connected = false;
            }

            // do any load time things at this point
            for (int i = 0; i < GameBase.num_edicts; i++) {
                ent = GameBase.g_edicts[i];

                if (!ent.inuse)
                    continue;

                // fire any cross-level triggers
                if (ent.classname != null)
                    if (Lib.strcmp(ent.classname, "target_crosslevel_target") == 0)
                        ent.nextthink = GameBase.level.time + ent.delay;
            }
        } catch (Exception e) {
            e.printStackTrace();
            GameBase.gi.error("Couldn't read level file " + filename);
        }
    }
}