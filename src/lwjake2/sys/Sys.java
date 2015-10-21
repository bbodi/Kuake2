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

package lwjake2.sys;

import lwjake2.Defines;
import lwjake2.Globals;
import lwjake2.client.CL;
import lwjake2.qcommon.Com;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Sys
 */
public final class Sys extends Defines {

    public static void Error(String error) {

        CL.Shutdown();
        //StackTrace();
        new Exception(error).printStackTrace();
        System.exit(1);
    }

    public static void Quit() {
        CL.Shutdown();

        System.exit(0);
    }

    //ok!
    public static File[] FindAll(String path, int musthave, int canthave) {

        int index = path.lastIndexOf('/');

        if (index != -1) {
            findbase = path.substring(0, index);
            findpattern = path.substring(index + 1, path.length());
        } else {
            findbase = path;
            findpattern = "*";
        }

        if (findpattern.equals("*.*")) {
            findpattern = "*";
        }

        File fdir = new File(findbase);

        if (!fdir.exists())
            return null;

        FilenameFilter filter = new FileFilter(findpattern, musthave, canthave);

        return fdir.listFiles(filter);
    }

    /**
     * Match the pattern findpattern against the filename.
     * 
     * In the pattern string, `*' matches any sequence of characters, `?'
     * matches any character, [SET] matches any character in the specified set,
     * [!SET] matches any character not in the specified set. A set is composed
     * of characters or ranges; a range looks like character hyphen character
     * (as in 0-9 or A-Z). [0-9a-zA-Z_] is the set of characters allowed in C
     * identifiers. Any other character in the pattern must be matched exactly.
     * To suppress the special syntactic significance of any of `[]*?!-\', and
     * match the character exactly, precede it with a `\'.
     */
    static class FileFilter implements FilenameFilter {

        String regexpr;

        int musthave, canthave;

        FileFilter(String findpattern, int musthave, int canthave) {
            this.regexpr = convert2regexpr(findpattern);
            this.musthave = musthave;
            this.canthave = canthave;

        }

        /*
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File dir, String name) {
            if (name.matches(regexpr)) {
                return CompareAttributes(dir, musthave, canthave);
            }
            return false;
        }

        String convert2regexpr(String pattern) {

            StringBuffer sb = new StringBuffer();

            char c;
            boolean escape = false;

            String subst;

            // convert pattern
            for (int i = 0; i < pattern.length(); i++) {
                c = pattern.charAt(i);
                subst = null;
                switch (c) {
                case '*':
                    subst = (!escape) ? ".*" : "*";
                    break;
                case '.':
                    subst = (!escape) ? "\\." : ".";
                    break;
                case '!':
                    subst = (!escape) ? "^" : "!";
                    break;
                case '?':
                    subst = (!escape) ? "." : "?";
                    break;
                case '\\':
                    escape = !escape;
                    break;
                default:
                    escape = false;
                }
                if (subst != null) {
                    sb.append(subst);
                    escape = false;
                } else
                    sb.append(c);
            }

            // the converted pattern
            String regexpr = sb.toString();

            //Com.DPrintf("pattern: " + pattern + " regexpr: " + regexpr +
            // '\n');
            try {
                Pattern.compile(regexpr);
            } catch (PatternSyntaxException e) {
                Com.Printf("invalid file pattern ( *.* is used instead )\n");
                return ".*"; // the default
            }
            return regexpr;
        }

        boolean CompareAttributes(File dir, int musthave, int canthave) {
            // . and .. never match
            String name = dir.getName();

            if (name.equals(".") || name.equals(".."))
                return false;

            return true;
        }

    }


    //============================================

    static File[] fdir;

    static int fileindex;

    static String findbase;

    static String findpattern;

    // ok.
    public static File FindFirst(String path, int musthave, int canthave) {

        if (fdir != null)
            Sys.Error("Sys_BeginFind without close");

        //	COM_FilePath (path, findbase);

        fdir = FindAll(path, canthave, musthave);
        fileindex = 0;

        if (fdir == null)
            return null;

        return FindNext();
    }

    public static File FindNext() {

        if (fileindex >= fdir.length)
            return null;

        return fdir[fileindex++];
    }

    public static void FindClose() {
        fdir = null;
    }

    public static void SendKeyEvents() {
		Globals.re.getKeyboardHandler().Update();

        // grab frame time
        Globals.sys_frame_time = Timer.Milliseconds();
    }

    public static String GetClipboardData() {
        // TODO: implement GetClipboardData
        return null;
    }

    public static void ConsoleOutput(String msg) {
        if (Globals.nostdout != null && Globals.nostdout.value != 0)
            return;

        System.out.print(msg);
    }

}