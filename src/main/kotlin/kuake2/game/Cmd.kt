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

package kuake2.game

import kuake2.Defines
import kuake2.Globals
import kuake2.game.monsters.M_Player
import kuake2.qcommon.*
import kuake2.server.SV_GAME
import kuake2.util.Lib
import kuake2.util.Math3D

import java.util.Arrays
import java.util.Comparator

/**
 * Cmd
 */
object Cmd {
    val ALIAS_LOOP_COUNT = 16
    var Wait_f: xcommand_t = object : xcommand_t() {
        public override fun execute() {
            Globals.cmd_wait = true
        }
    }
    //var cmd_functions: cmd_function_t? = null
    private var cmd_functions = hashMapOf<String, cmd_function_t>()
    var cmd_argc: Int = 0
    var cmd_argv = arrayOfNulls<String>(Defines.MAX_STRING_TOKENS)
    var cmd_args: String = ""
    var PlayerSort: Comparator<Int> = Comparator { o1: Int, o2: Int ->
            val anum1 = GameBase.game.clients[o1].ps.stats[Defines.STAT_FRAGS].toInt()
            val bnum1 = GameBase.game.clients[o2].ps.stats[Defines.STAT_FRAGS].toInt()
            if (anum1 < bnum1) -1 else if (anum1 > bnum1) 1 else 0
        }

    internal var List_f: xcommand_t = object : xcommand_t() {
        public override fun execute() {
            cmd_functions.values.forEach { cmd ->
                Com.Printf(cmd.name!! + '\n')
            }
            Com.Printf("${cmd_functions.size} commands\n")
        }
    }
    internal var Exec_f: xcommand_t = object : xcommand_t() {
        public override fun execute() {
            if (Cmd.Argc() != 2) {
                Com.Printf("exec <filename> : execute a script file\n")
                return
            }

            val f = FS.LoadFile(Cmd.Argv(1))
            if (f == null) {
                Com.Printf("couldn't exec " + Cmd.Argv(1) + "\n")
                return
            }
            Com.Printf("execing " + Cmd.Argv(1) + "\n")

            Cbuf.InsertText(String(f))

            FS.FreeFile(f)
        }
    }
    internal var Echo_f: xcommand_t = object : xcommand_t() {
        public override fun execute() {
            for (i in 1..Cmd.Argc() - 1) {
                Com.Printf(Cmd.Argv(i) + " ")
            }
            Com.Printf("'\n")
        }
    }
    internal var Alias_f: xcommand_t = object : xcommand_t() {
        public override fun execute() {
            if (Cmd.Argc() == 1) {
                Com.Printf("Current alias commands:\n")
                var a = Globals.cmd_alias
                while (a != null) {
                    Com.Printf(a.name + " : " + a.value)
                    a = a.next
                }
                return
            }

            val s = Cmd.Argv(1)
            if (s.length > Defines.MAX_ALIAS_NAME) {
                Com.Printf("Alias name is too long\n")
                return
            }

            // if the alias already exists, reuse it
            var a = Globals.cmd_alias
            while (a != null) {
                if (s.equals(a.name, ignoreCase = true)) {
                    a.value = null
                    break
                }
                a = a.next
            }

            if (a == null) {
                a = cmdalias_t()
                a.next = Globals.cmd_alias
                Globals.cmd_alias = a
            }
            a.name = s

            // copy the rest of the command line
            var cmd = ""
            val c = Cmd.Argc()
            for (i in 2..c - 1) {
                cmd = cmd + Cmd.Argv(i)
                if (i != (c - 1))
                    cmd = cmd + " "
            }
            cmd = cmd + "\n"

            a.value = cmd
        }
    }
    private val expanded = CharArray(Defines.MAX_STRING_CHARS)

    private val temporary = CharArray(Defines.MAX_STRING_CHARS)

    /**
     * Register our commands.
     */
    @JvmStatic fun Init() {

        Cmd.AddCommand("exec", Exec_f)
        Cmd.AddCommand("echo", Echo_f)
        Cmd.AddCommand("cmdlist", List_f)
        Cmd.AddCommand("alias", Alias_f)
        Cmd.AddCommand("wait", Wait_f)
    }

    /**
     * Cmd_MacroExpandString.
     */
    fun MacroExpandString(text: CharArray, len: Int): CharArray? {
        var len = len

        var inquote = false

        var scan = text

        if (len >= Defines.MAX_STRING_CHARS) {
            Com.Printf("Line exceeded " + Defines.MAX_STRING_CHARS + " chars, discarded.\n")
            return null
        }

        var count = 0
        var i = 0
        while (i < len) {
            if (scan[i] == '"')
                inquote = !inquote

            if (inquote) {
                i++
                continue
            } // don't expand inside quotes

            if (scan[i] != '$') {
                i++
                continue
            }

            // scan out the complete macro, without $
            val ph = Com.ParseHelp(text, i + 1)
            var token = Com.Parse(ph)

            if (ph.data == null) {
                i++
                continue
            }

            token = Cvar.VariableString(token)

            var j = token.length

            len += j

            if (len >= Defines.MAX_STRING_CHARS) {
                Com.Printf("Expanded line exceeded " + Defines.MAX_STRING_CHARS + " chars, discarded.\n")
                return null
            }

            System.arraycopy(scan, 0, temporary, 0, i)
            System.arraycopy(token.toCharArray(), 0, temporary, i, token.length)
            System.arraycopy(ph.data, ph.index, temporary, i + j, len - ph.index - j)

            System.arraycopy(temporary, 0, expanded, 0, 0)
            scan = expanded
            i--
            if (++count == 100) {
                Com.Printf("Macro expansion loop, discarded.\n")
                return null
            }
            i++
        }

        if (inquote) {
            Com.Printf("Line has unmatched quote, discarded.\n")
            return null
        }

        return scan
    }

    /**
     * Cmd_TokenizeString
     *
     *
     * Parses the given string into command line tokens. $Cvars will be expanded
     * unless they are in a quoted token.
     */
    @JvmStatic fun TokenizeString(input: CharArray, macroExpand: Boolean) {
        cmd_argc = 0
        cmd_args = ""

        var len = Lib.strlen(input)
        // macro expand the text
        var text = if (macroExpand) {
            MacroExpandString(input, len)
        } else input;
        if (text == null)
            return

        len = Lib.strlen(text)

        val ph = Com.ParseHelp(text)

        while (true) {

            // skip whitespace up to a /n
            var c = ph.skipwhitestoeol()

            if (c == '\n') {
                // a newline seperates commands in the buffer
                ph.nextchar()
                break
            }

            if (c.toInt() == 0)
                return

            // set cmd_args to everything after the first arg
            if (cmd_argc == 1) {
                cmd_args = String(text, ph.index, len - ph.index)
                cmd_args.trim()
            }

            val com_token = Com.Parse(ph)

            if (ph.data == null)
                return

            if (cmd_argc < Defines.MAX_STRING_TOKENS) {
                cmd_argv[cmd_argc] = com_token
                cmd_argc++
            }
        }
    }

    @JvmStatic fun AddCommand(cmd_name: String, function: xcommand_t?) {
        //Com.DPrintf("Cmd_AddCommand: " + cmd_name + "\n");
        // fail if the command is a variable name
        if ((Cvar.VariableString(cmd_name)).length > 0) {
            Com.Printf("Cmd_AddCommand: $cmd_name already defined as a var\n")
            return
        }

        // fail if the command already exists
        if (cmd_name in cmd_functions) {
            Com.Printf("Cmd_AddCommand: $cmd_name already defined\n")
            return
        }
        val cmd = cmd_function_t()
        cmd.name = cmd_name
        cmd.function = function
        cmd_functions.put(cmd_name, cmd)
    }

    /**
     * Cmd_RemoveCommand
     */
    @JvmStatic fun RemoveCommand(cmd_name: String) {
        cmd_functions.remove(cmd_name)
    }

    /**
     * Cmd_Exists
     */
    fun Exists(cmd_name: String): Boolean {
        return cmd_name in cmd_functions
    }

    @JvmStatic fun Argc(): Int {
        return cmd_argc
    }

    @JvmStatic fun Argv(i: Int): String {
        if (i < 0 || i >= cmd_argc)
            return ""
        return cmd_argv[i]!!
    }

    @JvmStatic fun Args(): String {
        return cmd_args
    }

    /**
     * Cmd_ExecuteString
     *
     *
     * A complete command line has been parsed, so try to execute it
     * FIXME: lookupnoadd the token to speed search?
     */
    @JvmStatic fun ExecuteString(text: String) {
        TokenizeString(text.toCharArray(), true)

        // execute the command line
        if (Argc() == 0)
            return  // no tokens

        // check functions
        val cmd = cmd_functions[cmd_argv[0]!!]
        if (cmd != null) {
            if (cmd.function != null) {
                cmd.function.execute()
            } else {
                // forward to server command
                Cmd.ExecuteString("cmd " + text)
            }
        } else {
            // check alias
            var a = Globals.cmd_alias
            while (a != null) {
                if (cmd_argv[0]!!.equals(a.name, ignoreCase = true)) {
                    if (++Globals.alias_count == ALIAS_LOOP_COUNT) {
                        Com.Printf("ALIAS_LOOP_COUNT\n")
                        return
                    }
                    Cbuf.InsertText(a.value)
                    return
                }
                a = a.next
            }

            // check cvars
            if (Cvar.Command())
                return

            // send it as a server command if we are connected
            Cmd.ForwardToServer()
        }
    }

    fun Spawn_f(ent: edict_t) {
        val spawned_ent = GameUtil.G_Spawn()
        val forward = floatArrayOf(0f, 0f, 0f)
        Math3D.AngleVectors(ent.s.angles, forward, null, null)
        Math3D.VectorCopy(ent.s.origin, spawned_ent.s.origin)
        Math3D.VectorScale(forward, 100f, forward)
        Math3D.VectorAdd(spawned_ent.s.origin, forward, spawned_ent.s.origin)
        spawned_ent.classname = Cmd.Argv(1)
        GameSpawn.ED_CallSpawn(spawned_ent)
    }

    /**
     * Cmd_Give_f
     *
     *
     * Give items to a client.
     */
    fun Give_f(ent: edict_t) {
        val index: Int
        val give_all: Boolean
        if (GameBase.deathmatch.value.toInt() != 0 && GameBase.sv_cheats.value.toInt() == 0) {
            SV_GAME.PF_cprintfhigh(ent,
                    "You must run the server with '+set cheats 1' to enable this command.\n")
            return
        }

        val name = Cmd.Args()

        if (0 == Lib.Q_stricmp(name, "all"))
            give_all = true
        else
            give_all = false

        if (give_all || 0 == Lib.Q_stricmp(Cmd.Argv(1), "health")) {
            if (Cmd.Argc() == 3)
                ent.health = Lib.atoi(Cmd.Argv(2))
            else
                ent.health = ent.max_health
            if (!give_all)
                return
        }

        if (give_all || 0 == Lib.Q_stricmp(name, "weapons")) {
            var i = 1
            while (i < GameBase.game.num_items) {
                val it = GameItemList.itemlist[i]
                if (null == it!!.pickup) {
                    i++
                    continue
                }
                if (0 == (it.flags and Defines.IT_WEAPON)) {
                    i++
                    continue
                }
                ent.client.pers.inventory[i] += 1
                i++
            }
            if (!give_all)
                return
        }

        if (give_all || 0 == Lib.Q_stricmp(name, "ammo")) {
            var i = 1
            while (i < GameBase.game.num_items) {
                var it = GameItemList.itemlist[i]
                if (null == it!!.pickup) {
                    i++
                    continue
                }
                if (0 == (it.flags and Defines.IT_AMMO)) {
                    i++
                    continue
                }
                GameItems.Add_Ammo(ent, it, 1000)
                i++
            }
            if (!give_all)
                return
        }

        if (give_all || Lib.Q_stricmp(name, "armor") == 0) {
            val info: gitem_armor_t

            var it = GameItems.FindItem("Jacket Armor")
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = 0

            it = GameItems.FindItem("Combat Armor")
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = 0

            it = GameItems.FindItem("Body Armor")
            info = it!!.info as gitem_armor_t
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = info.max_count

            if (!give_all)
                return
        }

        if (give_all || Lib.Q_stricmp(name, "Power Shield") == 0) {
            val it = GameItems.FindItem("Power Shield")
            val it_ent = GameUtil.G_Spawn()
            it_ent.classname = it!!.classname
            GameItems.SpawnItem(it_ent, it)
            GameItems.Touch_Item(it_ent, ent, GameBase.dummyplane, null)
            if (it_ent.inuse)
                GameUtil.G_FreeEdict(it_ent)

            if (!give_all)
                return
        }

        if (give_all) {
            var i = 1
            while (i < GameBase.game.num_items) {
                var it = GameItemList.itemlist[i]
                if (it!!.pickup != null) {
                    i++
                    continue
                }
                if ((it.flags and (Defines.IT_ARMOR or Defines.IT_WEAPON or Defines.IT_AMMO)) != 0) {
                    i++
                    continue
                }
                ent.client.pers.inventory[i] = 1
                i++
            }
            return
        }

        var it = GameItems.FindItem(name)
        if (it == null) {
            val item_name = Cmd.Argv(1)
            it = GameItems.FindItem(item_name)
            if (it == null) {
                SV_GAME.PF_cprintf(ent, Defines.PRINT_HIGH, "unknown item\n")
                return
            }
        }

        if (it.pickup == null) {
            SV_GAME.PF_cprintf(ent, Defines.PRINT_HIGH, "non-pickup item\n")
            return
        }

        index = GameItems.ITEM_INDEX(it)

        if ((it.flags and Defines.IT_AMMO) != 0) {
            if (Cmd.Argc() == 3)
                ent.client.pers.inventory[index] = Lib.atoi(Cmd.Argv(2))
            else
                ent.client.pers.inventory[index] += it.quantity
        } else {
            val it_ent = GameUtil.G_Spawn()
            it_ent.classname = it.classname
            GameItems.SpawnItem(it_ent, it)
            GameItems.Touch_Item(it_ent, ent, GameBase.dummyplane, null)
            if (it_ent.inuse)
                GameUtil.G_FreeEdict(it_ent)
        }
    }

    /**
     * Cmd_God_f
     *
     *
     * Sets client to godmode
     *
     *
     * argv(0) god
     */
    fun God_f(ent: edict_t) {
        val msg: String

        if (GameBase.deathmatch.value.toInt() != 0 && GameBase.sv_cheats.value.toInt() == 0) {
            SV_GAME.PF_cprintfhigh(ent,
                    "You must run the server with '+set cheats 1' to enable this command.\n")
            return
        }

        ent.flags = ent.flags xor Defines.FL_GODMODE
        if (0 == (ent.flags and Defines.FL_GODMODE))
            msg = "godmode OFF\n"
        else
            msg = "godmode ON\n"

        SV_GAME.PF_cprintf(ent, Defines.PRINT_HIGH, msg)
    }

    /**
     * Cmd_Notarget_f
     *
     *
     * Sets client to notarget
     *
     *
     * argv(0) notarget.
     */
    fun Notarget_f(ent: edict_t) {
        val msg: String

        if (GameBase.deathmatch.value.toInt() != 0 && GameBase.sv_cheats.value.toInt() == 0) {
            SV_GAME.PF_cprintfhigh(ent,
                    "You must run the server with '+set cheats 1' to enable this command.\n")
            return
        }

        ent.flags = ent.flags xor Defines.FL_NOTARGET
        if (0 == (ent.flags and Defines.FL_NOTARGET))
            msg = "notarget OFF\n"
        else
            msg = "notarget ON\n"

        SV_GAME.PF_cprintfhigh(ent, msg)
    }

    /**
     * Cmd_Noclip_f
     *
     *
     * argv(0) noclip.
     */
    fun Noclip_f(ent: edict_t) {
        val msg: String

        if (GameBase.deathmatch.value.toInt() != 0 && GameBase.sv_cheats.value.toInt() == 0) {
            SV_GAME.PF_cprintfhigh(ent,
                    "You must run the server with '+set cheats 1' to enable this command.\n")
            return
        }

        if (ent.movetype == Defines.MOVETYPE_NOCLIP) {
            ent.movetype = Defines.MOVETYPE_WALK
            msg = "noclip OFF\n"
        } else {
            ent.movetype = Defines.MOVETYPE_NOCLIP
            msg = "noclip ON\n"
        }

        SV_GAME.PF_cprintfhigh(ent, msg)
    }

    /**
     * Cmd_Use_f
     *
     *
     * Use an inventory item.
     */
    fun Use_f(ent: edict_t) {
        val index: Int
        val it: gitem_t?
        val s: String

        s = Cmd.Args()

        it = GameItems.FindItem(s)
        Com.dprintln("using:" + s)
        if (it == null) {
            SV_GAME.PF_cprintfhigh(ent, "unknown item: " + s + "\n")
            return
        }
        if (it.use == null) {
            SV_GAME.PF_cprintfhigh(ent, "Item is not usable.\n")
            return
        }
        index = GameItems.ITEM_INDEX(it)
        if (0 == ent.client.pers.inventory[index]) {
            SV_GAME.PF_cprintfhigh(ent, "Out of item: " + s + "\n")
            return
        }

        it.use.use(ent, it)
    }

    /**
     * Cmd_Drop_f
     *
     *
     * Drop an inventory item.
     */
    fun Drop_f(ent: edict_t) {
        val index: Int
        val it: gitem_t?
        val s: String

        s = Cmd.Args()
        it = GameItems.FindItem(s)
        if (it == null) {
            SV_GAME.PF_cprintfhigh(ent, "unknown item: " + s + "\n")
            return
        }
        if (it.drop == null) {
            SV_GAME.PF_cprintf(ent, Defines.PRINT_HIGH,
                    "Item is not dropable.\n")
            return
        }
        index = GameItems.ITEM_INDEX(it)
        if (0 == ent.client.pers.inventory[index]) {
            SV_GAME.PF_cprintfhigh(ent, "Out of item: " + s + "\n")
            return
        }

        it.drop.drop(ent, it)
    }

    /**
     * Cmd_Inven_f.
     */
    fun Inven_f(ent: edict_t) {
        val cl = ent.client
        cl.showscores = false
        cl.showhelp = false

        if (cl.showinventory) {
            cl.showinventory = false
            return
        }

        cl.showinventory = true

        GameBase.gi.WriteByte(Defines.svc_inventory)
        var i = 0
        while (i < Defines.MAX_ITEMS) {
            GameBase.gi.WriteShort(cl.pers.inventory[i])
            i++
        }
        GameBase.gi.unicast(ent, true)
    }

    /**
     * Cmd_InvUse_f.
     */
    fun InvUse_f(ent: edict_t) {
        val it: gitem_t

        Cmd.ValidateSelectedItem(ent)

        if (ent.client.pers.selected_item == -1) {
            SV_GAME.PF_cprintfhigh(ent, "No item to use.\n")
            return
        }

        it = GameItemList.itemlist[ent.client.pers.selected_item]
        if (it.use == null) {
            SV_GAME.PF_cprintfhigh(ent, "Item is not usable.\n")
            return
        }
        it.use.use(ent, it)
    }

    /**
     * Cmd_WeapPrev_f.
     */
    fun WeapPrev_f(ent: edict_t) {

        val cl = ent.client
        if (cl.pers.weapon == null)
            return

        val selected_weapon = GameItems.ITEM_INDEX(cl.pers.weapon)

        // scan for the next valid one
        var i = 1
        while (i <= Defines.MAX_ITEMS) {
            val index = (selected_weapon + i) % Defines.MAX_ITEMS
            if (0 == cl.pers.inventory[index]) {
                i++
                continue
            }

            var it = GameItemList.itemlist[index]
            if (it.use == null) {
                i++
                continue
            }

            if (0 == (it.flags and Defines.IT_WEAPON)) {
                i++
                continue
            }
            it.use.use(ent, it)
            if (cl.pers.weapon === it)
                return  // successful
            i++
        }
    }

    /**
     * Cmd_WeapNext_f.
     */
    fun WeapNext_f(ent: edict_t) {
        val cl = ent.client

        if (null == cl.pers.weapon)
            return

        val selected_weapon = GameItems.ITEM_INDEX(cl.pers.weapon)

        // scan for the next valid one
        var i = 1
        while (i <= Defines.MAX_ITEMS) {
            var index = (selected_weapon + Defines.MAX_ITEMS - i) % Defines.MAX_ITEMS
            //bugfix rst
            if (index == 0)
                index++
            if (0 == cl.pers.inventory[index]) {
                i++
                continue
            }
            val it = GameItemList.itemlist[index]
            if (null == it.use) {
                i++
                continue
            }
            if (0 == (it.flags and Defines.IT_WEAPON)) {
                i++
                continue
            }
            it.use.use(ent, it)
            if (cl.pers.weapon === it)
                return  // successful
            i++
        }
    }

    /**
     * Cmd_WeapLast_f.
     */
    fun WeapLast_f(ent: edict_t) {
        val cl: gclient_t
        val index: Int
        val it: gitem_t

        cl = ent.client

        if (null == cl.pers.weapon || null == cl.pers.lastweapon)
            return

        index = GameItems.ITEM_INDEX(cl.pers.lastweapon)
        if (0 == cl.pers.inventory[index])
            return
        it = GameItemList.itemlist[index]
        if (null == it.use)
            return
        if (0 == (it.flags and Defines.IT_WEAPON))
            return
        it.use.use(ent, it)
    }

    /**
     * Cmd_InvDrop_f
     */
    fun InvDrop_f(ent: edict_t) {
        val it: gitem_t

        Cmd.ValidateSelectedItem(ent)

        if (ent.client.pers.selected_item == -1) {
            SV_GAME.PF_cprintfhigh(ent, "No item to drop.\n")
            return
        }

        it = GameItemList.itemlist[ent.client.pers.selected_item]
        if (it.drop == null) {
            SV_GAME.PF_cprintfhigh(ent, "Item is not dropable.\n")
            return
        }
        it.drop.drop(ent, it)
    }

    /**
     * Cmd_Score_f
     *
     *
     * Display the scoreboard.
     */
    fun Score_f(ent: edict_t) {
        ent.client.showinventory = false
        ent.client.showhelp = false

        if (0 == GameBase.deathmatch.value.toInt() && 0 == GameBase.coop.value.toInt())
            return

        if (ent.client.showscores) {
            ent.client.showscores = false
            return
        }

        ent.client.showscores = true
        PlayerHud.DeathmatchScoreboard(ent)
    }

    /**
     * Cmd_Help_f
     *
     *
     * Display the current help message.
     */
    @JvmStatic fun Help_f(ent: edict_t) {
        // this is for backwards compatability
        if (GameBase.deathmatch.value.toInt() != 0) {
            Score_f(ent)
            return
        }

        ent.client.showinventory = false
        ent.client.showscores = false

        if (ent.client.showhelp && (ent.client.pers.game_helpchanged == GameBase.game.helpchanged)) {
            ent.client.showhelp = false
            return
        }

        ent.client.showhelp = true
        ent.client.pers.helpchanged = 0
        PlayerHud.HelpComputer(ent)
    }

    /**
     * Cmd_Kill_f
     */
    fun Kill_f(ent: edict_t) {
        if ((GameBase.level.time - ent.client.respawn_time) < 5)
            return
        ent.flags = ent.flags and Defines.FL_GODMODE.inv()
        ent.health = 0
        GameBase.meansOfDeath = Defines.MOD_SUICIDE
        PlayerClient.player_die.die(ent, ent, ent, 100000, Globals.vec3_origin)
    }

    /**
     * Cmd_PutAway_f
     */
    fun PutAway_f(ent: edict_t) {
        ent.client.showscores = false
        ent.client.showhelp = false
        ent.client.showinventory = false
    }

    /**
     * Cmd_Players_f
     */
    fun Players_f(ent: edict_t) {
        val index = arrayOfNulls<Int>(256)

        var count = 0
        var i = 0
        while (i < GameBase.maxclients.value) {
            if (GameBase.game.clients[i].pers.connected) {
                index[count] = i
                count++
            }
            i++
        }

        // sort by frags
        Arrays.sort<Int>(index, 0, count - 1, Cmd.PlayerSort)

        // print information
        var large = ""

        i = 0
        while (i < count) {
            val stats = GameBase.game.clients[index[i]!!.toInt()].ps.stats[Defines.STAT_FRAGS]
            val netname = GameBase.game.clients[index[i]!!.toInt()].pers.netname
            var small = "$stats $netname\n"

            if (small.length + large.length > 1024 - 100) {
                // can't print all of them in one packet
                large += "...\n"
                break
            }
            large += small
            i++
        }

        SV_GAME.PF_cprintfhigh(ent, "$large\n$count players\n")
    }

    /**
     * Cmd_Wave_f
     */
    fun Wave_f(ent: edict_t) {
        val i: Int

        i = Lib.atoi(Cmd.Argv(1))

        // can't wave when ducked
        if ((ent.client.ps.pmove.pm_flags.toInt() and pmove_t.PMF_DUCKED) != 0)
            return

        if (ent.client.anim_priority > Defines.ANIM_WAVE)
            return

        ent.client.anim_priority = Defines.ANIM_WAVE

        when (i) {
            0 -> {
                SV_GAME.PF_cprintfhigh(ent, "flipoff\n")
                ent.s.frame = M_Player.FRAME_flip01 - 1
                ent.client.anim_end = M_Player.FRAME_flip12
            }
            1 -> {
                SV_GAME.PF_cprintfhigh(ent, "salute\n")
                ent.s.frame = M_Player.FRAME_salute01 - 1
                ent.client.anim_end = M_Player.FRAME_salute11
            }
            2 -> {
                SV_GAME.PF_cprintfhigh(ent, "taunt\n")
                ent.s.frame = M_Player.FRAME_taunt01 - 1
                ent.client.anim_end = M_Player.FRAME_taunt17
            }
            3 -> {
                SV_GAME.PF_cprintfhigh(ent, "wave\n")
                ent.s.frame = M_Player.FRAME_wave01 - 1
                ent.client.anim_end = M_Player.FRAME_wave11
            }
            else -> {
                SV_GAME.PF_cprintfhigh(ent, "point\n")
                ent.s.frame = M_Player.FRAME_point01 - 1
                ent.client.anim_end = M_Player.FRAME_point12
            }
        }
    }

    /**
     * Command to print the players own position.
     */
    fun ShowPosition_f(ent: edict_t) {
        SV_GAME.PF_cprintfhigh(ent, "pos=" + Lib.vtofsbeaty(ent.s.origin) + "\n")
    }

    /**
     * Cmd_Say_f
     */
    fun Say_f(ent: edict_t, team: Boolean, arg0: Boolean) {
        var team = team
        if (Cmd.Argc() < 2 && !arg0)
            return

        if (0 == ((GameBase.dmflags.value).toInt() and (Defines.DF_MODELTEAMS or Defines.DF_SKINTEAMS)))
            team = false

        var text = if (team) {
            "(${ent.client.pers.netname}): "
        } else {
            "${ent.client.pers.netname}: "
        }

        if (arg0) {
            text += Cmd.Argv(0)
            text += " "
            text += Cmd.Args()
        } else {
            if (Cmd.Args().startsWith("\""))
                text += Cmd.Args().substring(1, Cmd.Args().length - 1)
            else
                text += Cmd.Args()
        }

        // don't let text be too long for malicious reasons
        if (text.length > 150)
        //text[150] = 0;
            text = text.substring(0, 150)

        text += "\n"

        if (GameBase.flood_msgs.value.toInt() != 0) {
            val cl = ent.client

            if (GameBase.level.time < cl.flood_locktill) {
                SV_GAME.PF_cprintfhigh(ent, "You can't talk for " + (cl.flood_locktill - GameBase.level.time).toInt() + " more seconds\n")
                return
            }
            var i = (cl.flood_whenhead - GameBase.flood_msgs.value + 1).toInt()
            if (i < 0)
                i += (10)
            if (cl.flood_when[i].toInt() != 0 && GameBase.level.time - cl.flood_when[i] < GameBase.flood_persecond.value) {
                cl.flood_locktill = GameBase.level.time + GameBase.flood_waitdelay.value
                SV_GAME.PF_cprintf(ent, Defines.PRINT_CHAT,
                        "Flood protection:  You can't talk for " + GameBase.flood_waitdelay.value.toInt() + " seconds.\n")
                return
            }

            cl.flood_whenhead = (cl.flood_whenhead + 1) % 10
            cl.flood_when[cl.flood_whenhead] = GameBase.level.time
        }

        if (Globals.dedicated.value.toInt() != 0)
            SV_GAME.PF_cprintf(null, Defines.PRINT_CHAT, "" + text + "")

        var j = 1
        while (j <= GameBase.game.maxclients) {
            val other = GameBase.g_edicts[j]
            if (!other.inuse) {
                j++
                continue
            }
            if (other.client == null) {
                j++
                continue
            }
            if (team) {
                if (!GameUtil.OnSameTeam(ent, other)) {
                    j++
                    continue
                }
            }
            SV_GAME.PF_cprintf(other, Defines.PRINT_CHAT, "" + text + "")
            j++
        }

    }

    /**
     * Returns the playerlist. TODO: The list is badly formatted at the moment.
     */
    fun PlayerList_f(ent: edict_t) {
        // connect time, ping, score, name
        var text = ""

        var i = 0
        while (i < GameBase.maxclients.value) {
            val e2 = GameBase.g_edicts[1 + i]
            if (!e2.inuse) {
                i++
                continue
            }

            val st = "" + (GameBase.level.framenum - e2.client.resp.enterframe) / 600 + ":" + ((GameBase.level.framenum - e2.client.resp.enterframe) % 600) / 10 + " " + e2.client.ping + " " + e2.client.resp.score + " " + e2.client.pers.netname + " " + (if (e2.client.resp.spectator) " (spectator)" else "") + "\n"

            if (text.length + st.length > 1024 - 50) {
                text += "And more...\n"
                SV_GAME.PF_cprintfhigh(ent, "" + text + "")
                return
            }
            text += st
            i++
        }
        SV_GAME.PF_cprintfhigh(ent, text)
    }

    /**
     * Adds the current command line as a clc_stringcmd to the client message.
     * things like godmode, noclip, etc, are commands directed to the server, so
     * when they are typed in at the console, they will need to be forwarded.
     */
    fun ForwardToServer() {
        val cmd: String

        cmd = Cmd.Argv(0)
        if (Globals.cls.state <= Defines.ca_connected || cmd[0] == '-' || cmd[0] == '+') {
            Com.Printf("Unknown command \"$cmd\"\n")
            return
        }

        MSG.WriteByte(Globals.cls.netchan.message, Defines.clc_stringcmd)
        SZ.Print(Globals.cls.netchan.message, cmd)
        if (Cmd.Argc() > 1) {
            SZ.Print(Globals.cls.netchan.message, " ")
            SZ.Print(Globals.cls.netchan.message, Cmd.Args())
        }
    }

    /**
     * Cmd_CompleteCommand.
     */
    @JvmStatic fun CompleteCommand(partial: String): List<String> {
        val cmds = cmd_functions.filter {
            it.key.startsWith(partial)
        }.keys.toArrayList()
        var a: cmdalias_t? = Globals.cmd_alias
        while (a != null) {
            if (a.name.startsWith(partial))
                cmds.add(a.name)
            a = a.next
        }
        return cmds
    }

    /**
     * Processes the commands the player enters in the quake console.
     */
    @JvmStatic fun ClientCommand(ent: edict_t) {
        val cmd: String

        if (ent.client == null)
            return  // not fully in game yet

        cmd = GameBase.gi.argv(0).toLowerCase()

        if (cmd == "players") {
            Players_f(ent)
            return
        }
        if (cmd == "say") {
            Say_f(ent, false, false)
            return
        }
        if (cmd == "say_team") {
            Say_f(ent, true, false)
            return
        }
        if (cmd == "score") {
            Score_f(ent)
            return
        }
        if (cmd == "help") {
            Help_f(ent)
            return
        }

        if (GameBase.level.intermissiontime.toInt() != 0)
            return

        if (cmd == "use")
            Use_f(ent)
        else if (cmd == "drop")
            Drop_f(ent)
        else if (cmd == "give")
            Give_f(ent)
        else if (cmd == "spawn")
            Spawn_f(ent)
        else if (cmd == "god")
            God_f(ent)
        else if (cmd == "notarget")
            Notarget_f(ent)
        else if (cmd == "noclip")
            Noclip_f(ent)
        else if (cmd == "inven")
            Inven_f(ent)
        else if (cmd == "invnext")
            GameItems.SelectNextItem(ent, -1)
        else if (cmd == "invprev")
            GameItems.SelectPrevItem(ent, -1)
        else if (cmd == "invnextw")
            GameItems.SelectNextItem(ent, Defines.IT_WEAPON)
        else if (cmd == "invprevw")
            GameItems.SelectPrevItem(ent, Defines.IT_WEAPON)
        else if (cmd == "invnextp")
            GameItems.SelectNextItem(ent, Defines.IT_POWERUP)
        else if (cmd == "invprevp")
            GameItems.SelectPrevItem(ent, Defines.IT_POWERUP)
        else if (cmd == "invuse")
            InvUse_f(ent)
        else if (cmd == "invdrop")
            InvDrop_f(ent)
        else if (cmd == "weapprev")
            WeapPrev_f(ent)
        else if (cmd == "weapnext")
            WeapNext_f(ent)
        else if (cmd == "weaplast")
            WeapLast_f(ent)
        else if (cmd == "kill")
            Kill_f(ent)
        else if (cmd == "putaway")
            PutAway_f(ent)
        else if (cmd == "wave")
            Wave_f(ent)
        else if (cmd == "playerlist")
            PlayerList_f(ent)
        else if (cmd == "showposition")
            ShowPosition_f(ent)
        else
        // anything that doesn't match a command will be a chat
            Say_f(ent, false, true)
    }

    @JvmStatic fun ValidateSelectedItem(ent: edict_t) {
        val cl = ent.client

        if (cl.pers.inventory[cl.pers.selected_item] != 0)
            return  // valid

        GameItems.SelectNextItem(ent, -1)
    }
}