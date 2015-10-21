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

package kuake2.sys

import kuake2.Defines
import kuake2.Globals
import kuake2.client.CL_input
import kuake2.client.Key
import kuake2.game.Cmd
import kuake2.game.usercmd_t
import kuake2.qcommon.Cvar
import kuake2.qcommon.xcommand_t
import kuake2.util.Math3D

/**
 * IN
 */
class IN : Globals() {
    companion object {

        private var mouse_avail = true

        var mouse_active = false

        private var ignorefirst = false

        private val mouse_buttonstate: Int = 0

        private var mouse_oldbuttonstate: Int = 0

        private var old_mouse_x: Int = 0

        private var old_mouse_y: Int = 0

        private var mlooking: Boolean = false

        fun ActivateMouse() {
            if (!mouse_avail)
                return
            if (!mouse_active) {
                KBD.mx = 0
                KBD.my = 0
                install_grabs()
                mouse_active = true
            }
        }

        fun DeactivateMouse() {
            // if (!mouse_avail || c == null) return;
            if (mouse_active) {
                uninstall_grabs()
                mouse_active = false
            }
        }

        private fun install_grabs() {
            Globals.re.keyboardHandler.installGrabs()
            ignorefirst = true
        }

        private fun uninstall_grabs() {
            Globals.re.keyboardHandler.uninstallGrabs()
        }

        fun toggleMouse() {
            if (mouse_avail) {
                mouse_avail = false
                DeactivateMouse()
            } else {
                mouse_avail = true
                ActivateMouse()
            }
        }

        fun Init() {
            Globals.in_mouse = Cvar.Get("in_mouse", "1", Defines.CVAR_ARCHIVE)
            Globals.in_joystick = Cvar.Get("in_joystick", "0", Defines.CVAR_ARCHIVE)
        }

        fun Shutdown() {
            mouse_avail = false
        }

        fun Real_IN_Init() {
            // mouse variables
            Globals.m_filter = Cvar.Get("m_filter", "0", 0)
            Globals.in_mouse = Cvar.Get("in_mouse", "1", Defines.CVAR_ARCHIVE)
            Globals.freelook = Cvar.Get("freelook", "1", 0)
            Globals.lookstrafe = Cvar.Get("lookstrafe", "0", 0)
            Globals.sensitivity = Cvar.Get("sensitivity", "3", 0)
            Globals.m_pitch = Cvar.Get("m_pitch", "0.022", 0)
            Globals.m_yaw = Cvar.Get("m_yaw", "0.022", 0)
            Globals.m_forward = Cvar.Get("m_forward", "1", 0)
            Globals.m_side = Cvar.Get("m_side", "0.8", 0)

            Cmd.AddCommand("+mlook", object : xcommand_t() {
                override fun execute() {
                    MLookDown()
                }
            })
            Cmd.AddCommand("-mlook", object : xcommand_t() {
                override fun execute() {
                    MLookUp()
                }
            })

            Cmd.AddCommand("force_centerview", object : xcommand_t() {
                override fun execute() {
                    Force_CenterView_f()
                }
            })

            Cmd.AddCommand("togglemouse", object : xcommand_t() {
                override fun execute() {
                    toggleMouse()
                }
            })

            IN.mouse_avail = true
        }

        fun Commands() {
            if (!IN.mouse_avail) {
                return
            }

            val kbd = Globals.re.keyboardHandler
            var i = 0
            while (i < 3) {
                if ((IN.mouse_buttonstate and (1 shl i)) != 0 && (IN.mouse_oldbuttonstate and (1 shl i)) == 0)
                    kbd.Do_Key_Event(Key.K_MOUSE1 + i, true)

                if ((IN.mouse_buttonstate and (1 shl i)) == 0 && (IN.mouse_oldbuttonstate and (1 shl i)) != 0)
                    kbd.Do_Key_Event(Key.K_MOUSE1 + i, false)
                i++
            }
            IN.mouse_oldbuttonstate = IN.mouse_buttonstate
        }

        fun Frame() {
            if (!Globals.cl.refresh_prepped || Globals.cls.key_dest == Defines.key_console || Globals.cls.key_dest == Defines.key_menu)
                DeactivateMouse()
            else
                ActivateMouse()
        }

        fun CenterView() {
            Globals.cl.viewangles[Defines.PITCH] = -Math3D.SHORT2ANGLE(Globals.cl.frame.playerstate.pmove.delta_angles[Defines.PITCH].toInt())
        }

        fun Move(cmd: usercmd_t) {
            if (!IN.mouse_avail)
                return

            if (Globals.m_filter.value != 0.0f) {
                KBD.mx = (KBD.mx + IN.old_mouse_x) / 2
                KBD.my = (KBD.my + IN.old_mouse_y) / 2
            }

            IN.old_mouse_x = KBD.mx
            IN.old_mouse_y = KBD.my

            KBD.mx = (KBD.mx * Globals.sensitivity.value).toInt()
            KBD.my = (KBD.my * Globals.sensitivity.value).toInt()

            // add mouse X/Y movement to cmd
            if ((CL_input.in_strafe.state and 1) != 0 || ((Globals.lookstrafe.value.toInt() != 0) && IN.mlooking)) {
                cmd.sidemove = (cmd.sidemove + (Globals.m_side.value * KBD.mx)).toShort()
            } else {
                Globals.cl.viewangles[Defines.YAW] -= Globals.m_yaw.value * KBD.mx
            }

            if ((IN.mlooking || Globals.freelook.value != 0.0f) && (CL_input.in_strafe.state and 1) == 0) {
                Globals.cl.viewangles[Defines.PITCH] += Globals.m_pitch.value * KBD.my
            } else {
                cmd.forwardmove = (cmd.forwardmove - (Globals.m_forward.value * KBD.my)).toShort()
            }
            KBD.mx = 0
            KBD.my = 0
        }

        internal fun MLookDown() {
            mlooking = true
        }

        internal fun MLookUp() {
            mlooking = false
            CenterView()
        }

        internal fun Force_CenterView_f() {
            Globals.cl.viewangles[Defines.PITCH] = 0f
        }
    }

}