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

package kuake2.game;

import kuake2.Defines;
import kuake2.qcommon.Com;
import kuake2.util.Lib;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

public class GameSVCmds {

    public static final int MAX_IPFILTERS = 1024;

    ;
    static GameSVCmds.ipfilter_t ipfilters[] = new GameSVCmds.ipfilter_t[MAX_IPFILTERS];
    static int numipfilters;

    static {
        for (int n = 0; n < GameSVCmds.MAX_IPFILTERS; n++)
            GameSVCmds.ipfilters[n] = new ipfilter_t();
    }

    public static void Svcmd_Test_f() {
        GameBase.gi.cprintf(null, Defines.PRINT_HIGH, "Svcmd_Test_f()\n");
    }

    /**
     * StringToFilter.
     */
    static boolean StringToFilter(String s, GameSVCmds.ipfilter_t f) {

        byte b[] = {0, 0, 0, 0};
        byte m[] = {0, 0, 0, 0};

        try {
            StringTokenizer tk = new StringTokenizer(s, ". ");

            for (int n = 0; n < 4; n++) {
                b[n] = (byte) Lib.atoi(tk.nextToken());
                if (b[n] != 0)
                    m[n] = -1;
            }

            f.mask = ByteBuffer.wrap(m).getInt();
            f.compare = ByteBuffer.wrap(b).getInt();
        } catch (Exception e) {
            GameBase.gi.cprintf(null, Defines.PRINT_HIGH,
                    "Bad filter address: " + s + "\n");
            return false;
        }

        return true;
    }

    /**
     * SV_FilterPacket.
     */
    static boolean SV_FilterPacket(String from) {
        int i;
        int in;
        int m[] = {0, 0, 0, 0};

        int p = 0;
        char c;

        i = 0;

        while (p < from.length() && i < 4) {
            m[i] = 0;

            c = from.charAt(p);
            while (c >= '0' && c <= '9') {
                m[i] = m[i] * 10 + (c - '0');
                c = from.charAt(p++);
            }
            if (p == from.length() || c == ':')
                break;

            i++;
            p++;
        }

        in = (m[0] & 0xff) | ((m[1] & 0xff) << 8) | ((m[2] & 0xff) << 16)
                | ((m[3] & 0xff) << 24);

        for (i = 0; i < numipfilters; i++)
            if ((in & ipfilters[i].mask) == ipfilters[i].compare)
                return ((int) GameBase.filterban.value) != 0;

        return ((int) 1 - GameBase.filterban.value) != 0;
    }

    /**
     * SV_AddIP_f.
     */
    static void SVCmd_AddIP_f() {
        int i;

        if (GameBase.gi.argc() < 3) {
            GameBase.gi.cprintf(null, Defines.PRINT_HIGH,
                    "Usage:  addip <ip-mask>\n");
            return;
        }

        for (i = 0; i < numipfilters; i++)
            if (ipfilters[i].compare == 0xffffffff)
                break; // free spot
        if (i == numipfilters) {
            if (numipfilters == MAX_IPFILTERS) {
                GameBase.gi.cprintf(null, Defines.PRINT_HIGH,
                        "IP filter list is full\n");
                return;
            }
            numipfilters++;
        }

        if (!StringToFilter(GameBase.gi.argv(2), ipfilters[i]))
            ipfilters[i].compare = 0xffffffff;
    }

    /**
     * SV_RemoveIP_f.
     */
    static void SVCmd_RemoveIP_f() {
        GameSVCmds.ipfilter_t f = new GameSVCmds.ipfilter_t();
        int i, j;

        if (GameBase.gi.argc() < 3) {
            GameBase.gi.cprintf(null, Defines.PRINT_HIGH,
                    "Usage:  sv removeip <ip-mask>\n");
            return;
        }

        if (!StringToFilter(GameBase.gi.argv(2), f))
            return;

        for (i = 0; i < numipfilters; i++)
            if (ipfilters[i].mask == f.mask
                    && ipfilters[i].compare == f.compare) {
                for (j = i + 1; j < numipfilters; j++)
                    ipfilters[j - 1] = ipfilters[j];
                numipfilters--;
                GameBase.gi.cprintf(null, Defines.PRINT_HIGH, "Removed.\n");
                return;
            }
        GameBase.gi.cprintf(null, Defines.PRINT_HIGH, "Didn't find "
                + GameBase.gi.argv(2) + ".\n");
    }

    /**
     * SV_ListIP_f.
     */
    static void SVCmd_ListIP_f() {
        int i;
        byte b[];

        GameBase.gi.cprintf(null, Defines.PRINT_HIGH, "Filter list:\n");
        for (i = 0; i < numipfilters; i++) {
            b = Lib.getIntBytes(ipfilters[i].compare);
            GameBase.gi
                    .cprintf(null, Defines.PRINT_HIGH, (b[0] & 0xff) + "."
                            + (b[1] & 0xff) + "." + (b[2] & 0xff) + "."
                            + (b[3] & 0xff));
        }
    }

    /**
     * SV_WriteIP_f.
     */
    static void SVCmd_WriteIP_f() {
        RandomAccessFile f;
        //char name[MAX_OSPATH];
        String name;
        byte b[];

        int i;
        cvar_t game;

        game = GameBase.gi.cvar("game", "", 0);

        if (game.string == null)
            name = Defines.GAMEVERSION + "/listip.cfg";
        else
            name = game.string + "/listip.cfg";

        GameBase.gi.cprintf(null, Defines.PRINT_HIGH, "Writing " + name + ".\n");

        f = Lib.fopen(name, "rw");
        if (f == null) {
            GameBase.gi.cprintf(null, Defines.PRINT_HIGH, "Couldn't open "
                    + name + "\n");
            return;
        }

        try {
            f.writeChars("set filterban " + (int) GameBase.filterban.value
                    + "\n");

            for (i = 0; i < numipfilters; i++) {
                b = Lib.getIntBytes(ipfilters[i].compare);
                f.writeChars("sv addip " + (b[0] & 0xff) + "." + (b[1] & 0xff)
                        + "." + (b[2] & 0xff) + "." + (b[3] & 0xff) + "\n");
            }

        } catch (IOException e) {
            Com.Printf("IOError in SVCmd_WriteIP_f:" + e);
        }

        Lib.fclose(f);
    }

    /**
     * ServerCommand
     * <p/>
     * ServerCommand will be called when an "sv" command is issued. The game can
     * issue gi.argc() / gi.argv() commands to get the rest of the parameters
     */
    public static void ServerCommand() {
        String cmd;

        cmd = GameBase.gi.argv(1);
        if (Lib.Q_stricmp(cmd, "test") == 0)
            Svcmd_Test_f();
        else if (Lib.Q_stricmp(cmd, "addip") == 0)
            SVCmd_AddIP_f();
        else if (Lib.Q_stricmp(cmd, "removeip") == 0)
            SVCmd_RemoveIP_f();
        else if (Lib.Q_stricmp(cmd, "listip") == 0)
            SVCmd_ListIP_f();
        else if (Lib.Q_stricmp(cmd, "writeip") == 0)
            SVCmd_WriteIP_f();
        else
            GameBase.gi.cprintf(null, Defines.PRINT_HIGH,
                    "Unknown server command \"" + cmd + "\"\n");
    }

    /**
     * PACKET FILTERING
     * <p/>
     * <p/>
     * You can add or remove addresses from the filter list with:
     * <p/>
     * addip <ip> removeip <ip>
     * <p/>
     * The ip address is specified in dot format, and any unspecified digits
     * will match any value, so you can specify an entire class C network with
     * "addip 192.246.40".
     * <p/>
     * Removeip will only remove an address specified exactly the same way. You
     * cannot addip a subnet, then removeip a single host.
     * <p/>
     * listip Prints the current list of filters.
     * <p/>
     * writeip Dumps "addip <ip>" commands to listip.cfg so it can be execed at
     * a later date. The filter lists are not saved and restored by default,
     * because I beleive it would cause too much confusion.
     * <p/>
     * filterban <0 or 1>
     * <p/>
     * If 1 (the default), then ip addresses matching the current list will be
     * prohibited from entering the game. This is the default setting.
     * <p/>
     * If 0, then only addresses matching the list will be allowed. This lets
     * you easily set up a private game, or a game that only allows players from
     * your local network.
     */

    public static class ipfilter_t {
        int mask;

        int compare;
    }
}