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

package lwjake2.server;

import lwjake2.Defines;
import lwjake2.game.player_state_t;

public class client_frame_t {

    int areabytes;
    byte areabits[] = new byte[Defines.MAX_MAP_AREAS / 8];        // portalarea visibility bits
    player_state_t ps = new player_state_t();
    int num_entities;
    int first_entity;        // into the circular sv_packet_entities[]
    int senttime;            // for ping calculations
}
