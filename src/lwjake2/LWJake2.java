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

package lwjake2;

import lwjake2.qcommon.Com;
import lwjake2.qcommon.Cvar;
import lwjake2.qcommon.Qcommon;
import lwjake2.sys.Timer;

/**
 * Jake2 is the main class of Quake2 for Java.
 */
public final class LWJake2 {

    /**
     * main is used to start the game. Quake2 for Java supports the following
     * command line arguments:
     *
     * @param args
     */
    public static void main(String[] args) {

        boolean dedicated = false;

        // check if we are in dedicated mode to hide the java dialog.
        for (int n = 0; n < args.length; n++) {
            if (args[n].equals("+set")) {
                if (n++ >= args.length)
                    break;

                if (!args[n].equals("dedicated"))
                    continue;

                if (n++ >= args.length)
                    break;

                if (args[n].equals("1") || args[n].equals("\"1\"")) {
                    Com.Printf("Starting in dedicated mode.\n");
                    dedicated = true;
                }
            }
        }

        // TODO: check if dedicated is set in config file

        Globals.dedicated = Cvar.Get("dedicated", "0", Qcommon.CVAR_NOSET);

        if (dedicated)
            Globals.dedicated.value = 1.0f;

        // in C the first arg is the filename
        int argc = (args == null) ? 1 : args.length + 1;
        String[] c_args = new String[argc];
        c_args[0] = "LWJake2";
        if (argc > 1) {
            System.arraycopy(args, 0, c_args, 1, argc - 1);
        }
        Qcommon.Init(c_args);

        Globals.nostdout = Cvar.Get("nostdout", "0", 0);

        int oldtime = Timer.Milliseconds();
        int newtime;
        int time;
        while (true) {
            // find time spending rendering last frame
            newtime = Timer.Milliseconds();
            time = newtime - oldtime;

            if (time > 0)
                Qcommon.Frame(time);
            oldtime = newtime;
        }
    }
}